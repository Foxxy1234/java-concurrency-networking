package online.foxxy239.concurrentnetworking.udp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;

public final class NonBlockingUdpEchoServer implements AutoCloseable {
    private Selector selector;
    private DatagramChannel channel;
    private Thread worker;

    public synchronized void start(final int port) throws IOException {
        if (channel != null) {
            throw new IllegalStateException("Server is already running");
        }
        selector = Selector.open();
        channel = DatagramChannel.open();
        channel.configureBlocking(false);
        channel.bind(new InetSocketAddress(port));
        worker = Thread.ofPlatform().start(this::serve);
    }

    public synchronized int port() throws IOException {
        return ((InetSocketAddress) channel.getLocalAddress()).getPort();
    }

    private void serve() {
        final ByteBuffer input = ByteBuffer.allocate(8_192);
        try {
            while (!Thread.currentThread().isInterrupted() && channel.isOpen()) {
                selector.select(100);
                input.clear();
                final SocketAddress client = channel.receive(input);
                if (client == null) {
                    continue;
                }
                input.flip();
                final String request = StandardCharsets.UTF_8.decode(input).toString();
                final ByteBuffer response = StandardCharsets.UTF_8.encode("Hello, " + request);
                channel.send(response, client);
            }
        } catch (final IOException | ClosedSelectorException ignored) {
            // Closing the selector or channel terminates the serving loop.
        }
    }

    @Override
    public synchronized void close() throws IOException {
        if (selector != null) {
            selector.wakeup();
            selector.close();
            selector = null;
        }
        if (channel != null) {
            channel.close();
            channel = null;
        }
        if (worker != null) {
            worker.interrupt();
            worker = null;
        }
    }
}
