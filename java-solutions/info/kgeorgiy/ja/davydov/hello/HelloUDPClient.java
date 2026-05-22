package info.kgeorgiy.ja.davydov.hello;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import info.kgeorgiy.ja.davydov.base.RequestExecutor;
import info.kgeorgiy.java.advanced.hello.HelloClient;

/**
 * UDP client implementation of the course interface {@link HelloClient}.
 *
 * @see HelloClient
 */
public class HelloUDPClient implements HelloClient {

    private record RequestTask(String message, Predicate<String> responseChecker) {
    }

    /**
     * Creates a client instance.
     */
    public HelloUDPClient() {
    }

    private class UDPClient implements AutoCloseable {
        private final DatagramSocket ds;
        private final InetAddress address;
        private final int port;

        UDPClient(String host, int port) throws IOException {
            ds = new DatagramSocket();
            address = InetAddress.getByName(host);
            this.port = port;
            ds.setSoTimeout(HelloHelper.TIMEOUT_MS);
        }

        void send(Supplier<RequestTask> messages) throws IOException {
            RequestTask requestTask;
            while (!Thread.currentThread().isInterrupted() && (requestTask = messages.get()) != null) {
                final String message = requestTask.message();
                final byte[] responseBuffer = new byte[HelloHelper.BUFFER_SIZE];
                final DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
                final byte[] requestData = HelloHelper.checkMessage(message);
                final DatagramPacket requestPacket = new DatagramPacket(requestData, requestData.length, address, port);
                while (!ds.isClosed() && !Thread.currentThread().isInterrupted()) {
                    try {
                        ds.send(requestPacket);
                        responsePacket.setData(responseBuffer);
                        responsePacket.setLength(responseBuffer.length);
                        ds.receive(responsePacket);
                        final String response = new String(responsePacket.getData(), responsePacket.getOffset(),
                                responsePacket.getLength(), StandardCharsets.UTF_8);
                        if (requestTask.responseChecker().test(response)) {
                            System.out.println("Message: " + message);
                            System.out.println("Answer: " + response);
                            break;
                        }
                    } catch (SocketTimeoutException ignored) {
                    }
                }
            }
        }

        @Override
        public void close() {
            ds.close();
        }
    }

    private Supplier<RequestTask> createMessagesSupplier(String prefix, int messagesCount, int numThread) {
        return new Supplier<>() {
            int current = 0;

            @Override
            public RequestTask get() {
                if (current < messagesCount) {
                    final int request = ++current;
                    final int thread = numThread + 1;
                    final String message = HelloHelper.formatRequest(prefix, request, thread);
                    return new RequestTask(message, HelloHelper.responseChecker(prefix, request, thread));
                }
                return null;
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(String host, int port, String prefix, int requests, int threads) {
        try (RequestExecutor requestExecutor = new RequestExecutor(threads)) {
            final List<Future<?>> tasks = IntStream.range(0, threads)
                    .mapToObj(i -> requestExecutor.run(() -> {
                        try (UDPClient client = new UDPClient(host, port)) {
                            client.send(createMessagesSupplier(prefix, requests, i));
                        } catch (IOException e) {
                            System.err.println("Failed on thread " + i + ": " + e.getMessage());
                        }
                    }))
                    .collect(Collectors.toList());
            try {
                for (final Future<?> task : tasks) {
                    requestExecutor.await(task);
                }
            } catch (InterruptedException e) {
                requestExecutor.close();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Starts the command-line client.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        new HelloClientMain(HelloUDPClient::new, "HelloUDPClient").main(args);
    }
}
