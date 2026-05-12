package info.kgeorgiy.ja.davydov.hello;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import info.kgeorgiy.ja.davydov.base.AbstractMain;
import info.kgeorgiy.ja.davydov.base.RequestExecutor;
import info.kgeorgiy.java.advanced.hello.NewHelloServer;

/**
 * UDP server implementation of the course interface {@link NewHelloServer}.
 *
 * @see NewHelloServer
 */
public class HelloUDPServer implements NewHelloServer {

    private static final int REQUEST_LIMIT = 10000;

    private List<DatagramSocket> sockets;
    private RequestExecutor requestExecutor;
    private RequestExecutor receiverExecutor;
    private final int BUFFER_SIZE = 8192;

    private void handlePacket(final DatagramSocket serverSocket, final DatagramPacket packet, final String format) {
        final String request = new String(packet.getData(), packet.getOffset(), packet.getLength(),
                StandardCharsets.UTF_8);
        final byte[] responseData = format.replaceAll("%%", request).getBytes(StandardCharsets.UTF_8);
        final DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length,
                packet.getSocketAddress());
        try {
            serverSocket.send(responsePacket);
        } catch (final IOException ignored) {
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void close() {
        if (sockets != null) {
            for (final DatagramSocket socket : sockets) {
                try {
                    socket.close();
                } catch (final Exception ignored) {
                }
            }
            sockets = null;
        }
        if (receiverExecutor != null) {
            receiverExecutor.close();
            receiverExecutor = null;
        }
        if (requestExecutor != null) {
            requestExecutor.close();
            requestExecutor = null;
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * See the required semantics in {@link NewHelloServer#start(int, Map)}.
     */
    @Override
    public synchronized void start(final int threads, final Map<Integer, String> portToStr) {
        close();
        requestExecutor = new RequestExecutor(threads, REQUEST_LIMIT);
        sockets = new ArrayList<>();
        receiverExecutor = portToStr.isEmpty() ? null : new RequestExecutor(portToStr.size(), portToStr.size());
        try {
            for (Map.Entry<Integer, String> portEntry : portToStr.entrySet()) {
                final DatagramSocket serverSocket = new DatagramSocket(new InetSocketAddress(portEntry.getKey()));
                sockets.add(serverSocket);
                final String format = portEntry.getValue();
                receiverExecutor.run(() -> {
                    while (!serverSocket.isClosed()) {
                        final byte[] buffer = new byte[BUFFER_SIZE];
                        final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        try {
                            serverSocket.receive(packet);
                            requestExecutor.run(() -> handlePacket(serverSocket, packet, format));
                        } catch (final IOException e) {
                            if (!serverSocket.isClosed()) {
                                System.err.println(e.getMessage());
                            }
                        }
                    }
                });
            }
        } catch (final IOException e) {
            close();
            throw new IllegalArgumentException("Failed to start server", e);
        }
    }

    private static final class Main extends AbstractMain {
        private static final String USAGE = "Usage: HelloUDPServer port threads";

        /**
         * Parses command-line arguments and starts the server.
         *
         * @param args command-line arguments
         */
        @Override
        protected void run(final String[] args) {
            final int port = parseRequiredInt(args, 0, "port");
            final int threads = parseRequiredInt(args, 1, "threads");
            final HelloUDPServer server = new HelloUDPServer();
            Runtime.getRuntime().addShutdownHook(new Thread(server::close));
            server.start(port, threads);
        }

        /**
         * Returns command usage text.
         *
         * @return usage string
         */
        @Override
        protected String usage() {
            return USAGE;
        }

        /**
         * Returns minimal supported argument count.
         *
         * @return minimal number of arguments
         */
        @Override
        protected int minArgs() {
            return 2;
        }

        /**
         * Returns maximal supported argument count.
         *
         * @return maximal number of arguments
         */
        @Override
        protected int maxArgs() {
            return 2;
        }
    }

    /**
     * Starts the command-line server.
     *
     * @param args command-line arguments
     */
    public static void main(final String[] args) {
        new Main().main(args);
    }

}
