package sha;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Server
{
    private static final Logger log = LogManager.getLogger();
    private final AtomicInteger connId = new AtomicInteger();
    private final ConcurrentHashMap<Integer, EndPoint> endpointMap = new ConcurrentHashMap<>();

    public Server(int port) throws IOException {
        Thread.currentThread().setName("sharpc-serverEndpoint-thread");
        ServerSocketChannel serverSocket = ServerSocketChannel.open();
        serverSocket.bind(new InetSocketAddress(port));
        log.info("started listening on port {}", port);
        while(true) {
            SocketChannel sc = serverSocket.accept();
            log.debug("serverEndpoint accepted a connection");
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    int c = connId.getAndIncrement();
                    if(c > (1L<<16)) {
                        throw new RuntimeException("currently supports max " + (1L << 16) + " connections");
                    }
                    try {
                        MyEndpoint endpoint = new MyEndpoint(sc, c);
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            t.start();
        }
    }

    public abstract void onMessage(long id, byte[] body);

    public void sendResponse(long id, byte[] response) throws IOException, InterruptedException {
        int myId = (int)(id >> 48);
        EndPoint endPoint = endpointMap.get(myId);
        long mask = 0xFFFFFFFFFFFFL;
        id &= mask;
        endPoint.send(id, response, null);
    }

    class MyEndpoint extends EndPoint {
        private final int myId;
        long mask = 0xFFFFFFFFFFFFL;

        public MyEndpoint(SocketChannel socketChannel, int myId) throws IOException, InterruptedException {
            super(socketChannel);
            this.myId = myId;
            endpointMap.put(myId, this);
        }

        @Override
        void handle(long id, byte[] data) {
            id &= mask;
            id |= ((long)myId << 48);
            onMessage(id, data);
        }
    }
}
