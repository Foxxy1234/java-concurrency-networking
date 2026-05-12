package info.kgeorgiy.ja.davydov.hello;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import info.kgeorgiy.ja.davydov.base.*;
import info.kgeorgiy.java.advanced.hello.HelloClient;

/**
 * UDP client implementation of the course interface {@link HelloClient}.
 *
 * @see HelloClient
 */
public class HelloUDPClient implements HelloClient {

    private final int CHARSET_BYTE_SIZE = 8, PREFIX_MAX_RESPONSE_SIZE = 1000;

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
        private final int port, TIMEOUT_MS = 100;

        /**
         * Creates UDP client bound to target host and port.
         *
         * @param host server host
         * @param port server port
         * @throws IOException if socket cannot be opened or host cannot be resolved
         */
        public UDPClient(String host, int port) throws IOException {
            ds = new DatagramSocket();
            address = InetAddress.getByName(host);
            this.port = port;
            ds.setSoTimeout(TIMEOUT_MS);
        }

        /**
         * Sends requests produced by the supplier until all are acknowledged.
         *
         * @param messages request supplier
         * @throws IOException if packet send/receive fails with non-timeout I/O error
         */
        public void send(Supplier<RequestTask> messages) throws IOException {
            RequestTask requestTask;
            while (!Thread.currentThread().isInterrupted() && (requestTask = messages.get()) != null) {
                final String message = requestTask.message();
                final byte[] responseBuffer = new byte[(message.length() + PREFIX_MAX_RESPONSE_SIZE)
                        * CHARSET_BYTE_SIZE];
                final DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
                final byte[] requestData = message.getBytes(StandardCharsets.UTF_8);
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

        /**
         * Closes the underlying socket.
         */
        @Override
        public void close() {
            ds.close();
        }

    }

    private static Predicate<String> responseChecker(final String prefix, final int request, final int thread) {
        final Pattern responsePattern = Pattern.compile(".*\\Q" + prefix + "\\E([\\p{Nd}]+)_([\\p{Nd}]+).*");
        return response -> {
            final Matcher matcher = responsePattern.matcher(response);
            return matcher.matches() && Integer.parseInt(matcher.group(1)) == request
                    && Integer.parseInt(matcher.group(2)) == thread;
        };
    }

    private Supplier<RequestTask> createMessagesSupplier(String prefix, int messagesCount, int numThread) {
        return new Supplier<>() {
            int current = 0;

            @Override
            public RequestTask get() {
                if (current < messagesCount) {
                    final int request = ++current;
                    final int thread = numThread + 1;
                    final String message = prefix + request + "_" + thread;
                    return new RequestTask(message, responseChecker(prefix, request, thread));
                }
                return null;
            }
        };
    }

    /**
     * {@inheritDoc}
     * <p>
     * Behavior follows the contract from
     * {@link HelloClient#run(String, int, String, int, int)}.
     */
    @Override
    public void run(String host, int port, String prefix, int requests, int threads) {
        try (RequestExecutor requestExecutor = new RequestExecutor(threads, threads)) {
            final List<Future<?>> tasks = new ArrayList<>();
            IntStream.range(0, threads).forEach(i -> {
                tasks.add(requestExecutor.run(() -> {
                    try (UDPClient client = new UDPClient(host, port)) {
                        final Supplier<RequestTask> messages = createMessagesSupplier(prefix, requests, i);
                        client.send(messages);
                    } catch (IOException e) {
                        System.err.println("Failed on thread " + i + ": " + e.getMessage());
                    }
                }));
            });
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

    private static final class Main extends AbstractMain {
        private static final String USAGE = "Usage: HelloUDPClient ip port prefix threads requests";

        /**
         * Parses command-line arguments and runs the UDP client.
         *
         * @param args command-line arguments
         * @throws IOException if argument parsing helpers throw I/O related errors
         */
        @Override
        protected void run(final String[] args) throws IOException {
            final String host = args[0];
            final int port = parseRequiredInt(args, 1, "port");
            final String prefix = args[2];
            final int threads = parseRequiredInt(args, 3, "threads");
            final int requests = parseRequiredInt(args, 4, "requests");
            HelloUDPClient hudpc = new HelloUDPClient();
            hudpc.run(host, port, prefix, requests, threads);
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
            return 5;
        }

        /**
         * Returns maximal supported argument count.
         *
         * @return maximal number of arguments
         */
        @Override
        protected int maxArgs() {
            return 5;
        }
    }

    /**
     * Starts the command-line client.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        new Main().main(args);
    }
}
