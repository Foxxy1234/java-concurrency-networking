package info.kgeorgiy.ja.davydov.hello;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    private void handlePacket(final DatagramSocket serverSocket, final DatagramPacket packet, final String format) {
        final String request = new String(packet.getData(), packet.getOffset(), packet.getLength(),
                StandardCharsets.UTF_8);
        final byte[] responseData = HelloHelper.checkMessage(HelloHelper.formatResponse(format, request));
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
     */
    @Override
    public synchronized void start(final int threads, final Map<Integer, String> portToStr) {
        close();
        requestExecutor = new RequestExecutor(threads, REQUEST_LIMIT);
        sockets = new ArrayList<>();
        receiverExecutor = portToStr.isEmpty() ? null : new RequestExecutor(portToStr.size());
        try {
            for (Map.Entry<Integer, String> portEntry : portToStr.entrySet()) {
                final DatagramSocket serverSocket = new DatagramSocket(new InetSocketAddress(portEntry.getKey()));
                sockets.add(serverSocket);
                final String format = portEntry.getValue();
                receiverExecutor.run(() -> {
                    while (!serverSocket.isClosed()) {
                        final byte[] buffer = new byte[HelloHelper.BUFFER_SIZE];
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

    /**
     * Starts the command-line server.
     *
     * @param args command-line arguments
     */
    public static void main(final String[] args) {
        new HelloServerMain(HelloUDPServer::new, "HelloUDPServer").main(args);
    }
}
