package sha.example;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sha.Server;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * A simple RPC which accepts two longs and returns the product of them
 */
public class ExampleServer extends Server
{
    private static final Logger log = LogManager.getLogger();
    private final int port;

    public static void main( String[] args ) {
        String usage = "Usage: java sha.example.ExampleServer port";
        if(args.length != 1) {
            System.out.println(usage);
            return;
        }
        try {
            ExampleServer obj = new ExampleServer(Integer.parseInt(args[0]));
        } catch (Exception e) {
            log.error("", e);
            System.out.println(usage);
        }
    }


    public ExampleServer(int port) throws IOException {
        super(port);
        this.port = port;
    }


    @Override
    public void onMessage(long id, byte[] body) {
        ByteBuffer myResponse = ByteBuffer.allocate(8);

        ByteBuffer bf = ByteBuffer.wrap(body);
        long a = bf.getLong();
        long b = bf.getLong();
        // we don't care about overflow
        long c = a * b;
        myResponse.putLong(0, c);
        try {
            sendResponse(id, myResponse.array());
        } catch (IOException | InterruptedException e) {
            log.error("boo", e);
        }
    }
}
