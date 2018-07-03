package sha;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import static sha.EndPoint.State.BODY;
import static sha.EndPoint.State.HEADER;

public abstract class EndPoint {
    private static final Logger log = LogManager.getLogger();

    public static final int sz = 2*1024_0;
    public static final int header_sz = 20;
    protected final ConcurrentHashMap<Long, WrapCallback> map = new ConcurrentHashMap<>();
    private final BlockingQueue<ByteBuffer> bq = new ArrayBlockingQueue<>(1024);
    private final long magic = 271828;
    private ByteBuffer current;

    long id = 0L;
    private final Selector selector;
    private final Recv recv;
    private SocketChannel socketChannel;

    public static void main(String[] args) throws IOException, InterruptedException {
        ServerSocketChannel serverSocket = ServerSocketChannel.open();
        serverSocket.bind(new InetSocketAddress("127.0.0.1", 19838));

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.currentThread().setName("sharpc-serverEndpoint-thread");
                    SocketChannel sc = serverSocket.accept();
                    log.debug("serverEndpoint accepted a connection");
                    ServerEndpoint serverEndpoint = new ServerEndpoint(sc);
                } catch (IOException | InterruptedException e) {
                    log.error("boo", e);
                }
            }
        });


        Thread.currentThread().setName("sharpc-clientEndpoint-thread");
        t.start();
        SocketChannel sc = SocketChannel.open();
        sc.connect(new InetSocketAddress("127.0.0.1", 19838));
        log.debug("finished connect");
        ClientEndpoint clientEndpoint = new ClientEndpoint(sc);

        Utils.Timer timer = new Utils.Timer("");
        Utils.LatencyTimer lat = new Utils.LatencyTimer("");
        int i=0;
        byte[] data = new byte[30_000];
        Arrays.fill(data, (byte) i);
        while(true) {
            i++;

            int finalI = i;
            clientEndpoint.send(-1, data, new CompletionHandler<byte[], Void>() {
                long created = System.nanoTime();
                @Override
                public void completed(byte[] result, Void attachment) {
                    timer.count();
                    lat.count(System.nanoTime()-created);
//                    log.info("the/ result for id:{} is {}, size:{}", finalI, Arrays.toString(result), result.length);
                }

                @Override
                public void failed(Throwable exc, Void attachment) {

                }
            });
        }
    }

    abstract void handle(long id, byte[] data);

    public EndPoint(SocketChannel socketChannel) throws IOException, InterruptedException {
        this.socketChannel = socketChannel;
        selector = Selector.open();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_WRITE);
        ensureFree();
        recv = new Recv();
        String name = Thread.currentThread().getName();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.currentThread().setName(name+"-recv");
                    recv.recv();
                } catch (IOException | InterruptedException e) {
                    log.error("boo", e);
                }
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.currentThread().setName(name+"-periodic-sender");
                    while (true) {
                        doSend();
                        Thread.sleep(10);
                    }
                } catch (IOException | InterruptedException e) {
                    log.error("boo", e);
                }

            }
        }).start();
    }

    public synchronized void send(long myId, byte[] data, CompletionHandler callback) throws InterruptedException, IOException {
        long localId = myId < 0?id:myId;
        if(callback != null) {
            map.put(localId, new WrapCallback(callback));
        }
        if(current.remaining() < header_sz) {
            ensureFree();
        }
        current.putLong(magic);
        current.putLong(localId);
        current.putInt(data.length);

        int start = 0;
        while(start < data.length) {
            if(current.remaining() == 0) {
                ensureFree();
            }
            int length = Math.min(current.remaining(), data.length-start);
            current.put(data, start, length);
            start += length;
        }
        if(bq.size() > 1) {
            doSend();
        }

        if(myId < 0) {
            id++;
        }
    }

    public  synchronized void doSend() throws IOException {
        List<ByteBuffer> localList = new ArrayList<>();

        bq.drainTo(localList);
        for (ByteBuffer bf : localList) {
            bf.flip();
            while (bf.remaining() > 0) {
                int written = socketChannel.write(bf);
                if (written == 0) {
                    // wait for socket to become writable again.
                    selector.select();
                }
            }
        }
    }

    private void ensureFree() throws InterruptedException {
        current = ByteBuffer.allocate(sz);
        bq.put(current);
    }

    static class WrapCallback implements CompletionHandler {
        CompletionHandler callback;
        boolean fired;

        public WrapCallback(CompletionHandler callback) {
            this.callback = callback;
        }

        @Override
        public synchronized void completed(Object result, Object attachment) {
            if(!fired) {
                callback.completed(result, attachment);
                fired = true;
            }
        }

        @Override
        public void failed(Throwable exc, Object attachment) {
            if(!fired) {
                callback.failed(exc, attachment);
                fired = true;
            }
        }
    }

    /**
     * ALL THE RECIEVE CODE
     */

    class Recv {
        ByteBuffer current;
        private final Selector selector;
        LinkedList<ByteBuffer> list = new LinkedList<>();
        Message msg = new Message();
        long read = 0;
        long dispatched = 0;

        long debug_messages = 0;
        long debug_batchCount = 0;

        public Recv() throws IOException, InterruptedException {
            selector = Selector.open();
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_READ);
            addBuffer();
        }

        /**
         * because I dont know the spelling of recieve
         */
        public void recv() throws IOException, InterruptedException {
            while(true) {
                selector.select();
                while(true) {
                    int readNow = socketChannel.read(current);
                    if (readNow == -1) {
                        return;
                    }
                    if(readNow == 0) {
                        break;
                    }
                    read += readNow;
                }
                dispatch();
            }
        }

        private void dispatch() throws InterruptedException {
            while(true) {
                if(msg.state == HEADER) {
                    if(read - dispatched >= header_sz) {
                        parseHeader();
                    } else {
                        break;
                    }
                }
                if(msg.state==BODY ) {
                    if(read  - dispatched >= msg.length + header_sz) {
                        finishMessage();
                    } else {
                        if(current.remaining() == 0) {
                            addBuffer();
                        }
                        break;
                    }
                }
            }
            debug_batchCount = 0;
        }

        private void parseHeader() {
            // It is guaranteed that the header will fit into a single bytebuffer
            ByteBuffer sbuf = list.getFirst();
            if(sbuf.getLong(msg.startOffset) != magic) {
                throw new RuntimeException("expected magic header");
            }
            try {
                msg.id = sbuf.getLong(msg.startOffset + 8);
            } catch (Exception e) {
                log.error("boo", e);
            }

            msg.length = sbuf.getInt(msg.startOffset+16);
            msg.state = BODY;
//            log.debug("server parsed header: id:{} length:{}", msg.id, msg.length);
        }

        private void finishMessage() {
            byte body[] = new byte[msg.length];
            int start = 0;
            Iterator<ByteBuffer> it = list.iterator();
            boolean first = true;
            long s = 0, e = 0;
            ByteBuffer old = null;
            while(it.hasNext()) {
                ByteBuffer bf = it.next();
                bf.flip();
                if (first) {
                    bf.position(msg.startOffset+header_sz);
                    s = msg.startOffset;
                    first = false;
                }
                int l = Math.min(body.length - start, bf.limit() - bf.position());
                bf.get(body, start, l);
                start += l;


                if(bf.remaining() == 0 || (start >= body.length && bf.remaining()<header_sz)) {
                    it.remove();
                    old = bf;
                }

                if(start >= body.length) {
                    e = bf.position();
                    break;
                }
            }

            //setup for the new message
            dispatched += body.length + header_sz;
            debug_messages++;
            handle(msg.id, body);
            msg = new Message();
            msg.state = HEADER;
            if(list.size() > 0) {
                // save the offset of the next message
                msg.startOffset = current.position();
                // unflip it.
                current.position(current.limit());
                current.limit(current.capacity());
            } else {
                addBuffer(old);
                msg.startOffset = 0;
            }
//            if(Thread.currentThread().getName().equals("sharpc-server-thread-recv")) {
//                log.debug("debug_batchCount:{} s:{}, e:{}, pos:{} limit:{}", debug_batchCount++, s, e, current.position(), current.limit());
//            }

        }

        private void addBuffer(ByteBuffer old) {
            current = ByteBuffer.allocate(sz);
            current.put(old);
            list.add(current);
        }

        private void addBuffer() {
            current = ByteBuffer.allocate(sz);
            list.add(current);
        }
    }

    static class Message {
        int startOffset;
        State state = HEADER;

        // part of the header
        int length;
        long id;
    }
    static enum State {HEADER, BODY}

    static interface Handler {
        void handle(long id, byte[] body);
    }

    static class ClientEndpoint extends EndPoint {

        public ClientEndpoint(SocketChannel socketChannel) throws IOException, InterruptedException {
            super(socketChannel);
        }

        @Override
        public void handle(long id, byte[] body) {
            WrapCallback callback = map.remove(id);
//            log.info("firing callback for {}", id);
            if (callback == null) {
                throw new RuntimeException("No callback for id " + id);
            }
            try {
                callback.callback.completed(body, null);
            } finally {
                callback.fired = true;
            }
        }

    }
    static class ServerEndpoint extends EndPoint {
        public ServerEndpoint(SocketChannel socketChannel) throws IOException, InterruptedException {
            super(socketChannel);
        }

        @Override
        public void handle(long id, byte[] body) {
            try {
                send(id, body, null);
            } catch (InterruptedException | IOException e) {
                log.error("boo", e);
            }
        }
    }
}
