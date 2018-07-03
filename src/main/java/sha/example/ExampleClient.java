package sha.example;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sha.Client;
import sha.Utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

public class ExampleClient
{
    private static final Logger log = LogManager.getLogger();
    private final String host;
    private final int port;

    public ExampleClient(String host, int port) throws IOException, InterruptedException {

        this.host = host;
        this.port = port;
        go();
    }

    public static void main( String[] args ) {
        String usage = "Usage: java -cp ... sha.example.ExampleClient host port";
        if(args.length != 2) {
            System.out.println(usage);
            return;
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);

        try {
            ExampleClient obj = new ExampleClient(host, port);
            obj.go();
        } catch (Exception e) {
            log.error("", e);
            System.out.println(usage);
        }
    }

    /**
     * All teh code from here:
     */
    private void go() throws IOException, InterruptedException {

        Client client = new Client(host, port);
        Utils.Timer timer = new Utils.Timer("");
        ByteBuffer bf = ByteBuffer.allocate(16);
        long a = 0;
        while(true) {
            long b = Long.MAX_VALUE - a;
            bf.clear();
            bf.putLong(a);
            bf.putLong(b);
            long finalA = a;
//            log.error("client side {} {} {}", finalA, b);

            client.send(bf.array(), new CompletionHandler<byte[], Void>() {
                @Override
                public void completed(byte[] result, Void attachment) {
                    ByteBuffer bf = ByteBuffer.wrap(result);
                    long c = bf.getLong();
                    if(c != finalA * b) {
//                        log.error("{} {} {}", finalA, b, c);
                        throw new RuntimeException("RPC failed!");
                    }
                }
                @Override
                public void failed(Throwable exc, Void attachment) {
                    log.error("boo", exc);
                }
            });
            a++;
            timer.count();
        }
    }

}
