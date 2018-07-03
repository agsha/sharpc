package sha;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.CompletionHandler;
import java.nio.channels.SocketChannel;

public class Client
{
    private static final Logger log = LogManager.getLogger();
    private final EndPoint.ClientEndpoint clientEndpoint;

    public Client(String host, int port) throws IOException, InterruptedException {
        Thread.currentThread().setName("sharpc-clientEndpoint-thread");
        SocketChannel sc = SocketChannel.open();
        sc.connect(new InetSocketAddress(host, port));
        log.debug("client finished connect");
        clientEndpoint = new EndPoint.ClientEndpoint(sc);
    }

    public void send(byte[] data, CompletionHandler callback) throws IOException, InterruptedException {
        clientEndpoint.send(-1, data, callback);
    }
}
