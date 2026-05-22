package info.kgeorgiy.ja.davydov.hello;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

import info.kgeorgiy.ja.davydov.base.RequestExecutor;
import info.kgeorgiy.java.advanced.hello.NewHelloServer;

/**
 * Non-blocking UDP server implementation of {@link NewHelloServer}.
 */
public class HelloUDPNonblockingServer implements NewHelloServer {

    private Selector selector;
    private List<DatagramChannel> channels;
    private RequestExecutor workerExecutor;
    private Thread selectorThread;
    private volatile boolean closed = true;

    /**
     * Creates a server instance.
     */
    public HelloUDPNonblockingServer() {
    }

    private record Response(ByteBuffer buffer, SocketAddress address) {
    }

    private static final class PortState {
        final String format;
        final Queue<Response> outQueue = new ConcurrentLinkedQueue<>();
        final ByteBuffer receiveBuffer = ByteBuffer.allocate(HelloHelper.BUFFER_SIZE);
        final ArrayBlockingQueue<ByteBuffer> buffers;

        private PortState(String format, int queueSize) {
            this.format = format;
            this.buffers = new ArrayBlockingQueue<>(queueSize);
            for (int i = 0; i < queueSize; i++) {
                buffers.add(ByteBuffer.allocate(HelloHelper.BUFFER_SIZE));
            }
        }

        private void handleReadable(SelectionKey key, RequestExecutor workerExecutor, Selector selector)
                throws IOException {
            final DatagramChannel dc = (DatagramChannel) key.channel();
            receiveBuffer.clear();
            final SocketAddress clientAddress = dc.receive(receiveBuffer);
            if (clientAddress == null) {
                return;
            }
            receiveBuffer.flip();
            final String request = StandardCharsets.UTF_8.decode(receiveBuffer).toString();
            workerExecutor.run(() -> {
                final ByteBuffer buffer;
                try {
                    buffer = buffers.take();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                buffer.clear();
                buffer.put(HelloHelper.checkMessage(HelloHelper.formatResponse(format, request)));
                buffer.flip();
                outQueue.add(new Response(buffer, clientAddress));
                selector.wakeup();
            });
        }

        private void handleWritable(SelectionKey key) {
            final DatagramChannel dc = (DatagramChannel) key.channel();
            Response response;
            while ((response = outQueue.poll()) != null) {
                try {
                    dc.send(response.buffer(), response.address());
                } catch (IOException ignored) {
                }
                buffers.add(response.buffer());
            }
            if (outQueue.isEmpty()) {
                key.interestOps(SelectionKey.OP_READ);
            }
        }
    }

    private static void checkPendingWrites(Selector selector) {
        for (final SelectionKey key : selector.keys()) {
            if (!key.isValid()) {
                continue;
            }
            final PortState state = (PortState) key.attachment();
            if (!state.outQueue.isEmpty() && (key.interestOps() & SelectionKey.OP_WRITE) == 0) {
                key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void start(int threads, Map<Integer, String> ports) {
        closeResources();
        if (ports.isEmpty()) {
            return;
        }
        try {
            selector = Selector.open();
            channels = new ArrayList<>();
            workerExecutor = new RequestExecutor(threads, 10_000);
            for (final Map.Entry<Integer, String> port : ports.entrySet()) {
                final DatagramChannel dc = DatagramChannel.open();
                channels.add(dc);
                dc.setOption(StandardSocketOptions.SO_REUSEADDR, true);
                dc.configureBlocking(false);
                dc.bind(new InetSocketAddress(port.getKey()));
                dc.register(selector, SelectionKey.OP_READ, new PortState(port.getValue(), threads));
            }
        } catch (IOException e) {
            closeResources();
            throw new IllegalArgumentException("Failed to start server", e);
        }
        closed = false;
        selectorThread = new Thread(this::selectorLoop);
        selectorThread.start();
    }

    private void selectorLoop() {
        try {
            while (!closed) {
                checkPendingWrites(selector);
                selector.select();
                if (closed) {
                    break;
                }
                final Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    final SelectionKey key = it.next();
                    it.remove();
                    final PortState state = (PortState) key.attachment();
                    if (key.isReadable()) {
                        state.handleReadable(key, workerExecutor, selector);
                    }
                    if (key.isValid() && key.isWritable()) {
                        state.handleWritable(key);
                    }
                }
            }
        } catch (IOException e) {
            if (!closed) {
                System.err.println("Selector error: " + e.getMessage());
            }
        }
    }

    private void closeResources() {
        if (selector != null) {
            selector.wakeup();
            try {
                selector.close();
            } catch (IOException ignored) {
            }
            selector = null;
        }
        if (channels != null) {
            for (final DatagramChannel dc : channels) {
                try {
                    dc.close();
                } catch (IOException ignored) {
                }
            }
            channels = null;
        }
        if (selectorThread != null) {
            selectorThread.interrupt();
            try {
                selectorThread.join(HelloHelper.TIMEOUT_MS * 10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            selectorThread = null;
        }
        if (workerExecutor != null) {
            workerExecutor.close();
            workerExecutor = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        closeResources();
    }

    /**
     * Starts the command-line server.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        new HelloServerMain(HelloUDPNonblockingServer::new, "HelloUDPNonblockingServer").main(args);
    }
}
