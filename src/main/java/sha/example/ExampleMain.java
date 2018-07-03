package sha.example;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class ExampleMain
{
    private static final Logger log = LogManager.getLogger();

    public static void main( String[] args ) {
        try {
            ExampleMain obj = new ExampleMain();
            obj.go(args);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * All teh code from here:
     */
    private void go(String[] args ) throws Exception {
        String host = "127.0.0.1";
        int port = 19838;
        startServer(port);
        Thread.sleep(500);

        ExampleClient client = new ExampleClient(host, port);
    }
    private void startServer(int port) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    new ExampleServer(port);
                } catch (IOException e) {
                    log.error("boo", e);
                }
            }
        }).start();

    }

}
