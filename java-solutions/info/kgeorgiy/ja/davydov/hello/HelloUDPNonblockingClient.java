package info.kgeorgiy.ja.davydov.hello;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

import info.kgeorgiy.java.advanced.hello.HelloClient;

/**
 * Non-blocking UDP client implementation of {@link HelloClient}.
 */
public class HelloUDPNonblockingClient implements HelloClient {

    /**
     * Creates a client instance.
     */
    public HelloUDPNonblockingClient() {
    }

    private record Deadline(long at, SelectionKey key) {
    }

    private static final class ChannelState {
        final int threadIndex;
        final String prefix;
        final int totalRequests;
        int currentRequest;
        final ByteBuffer sendBuffer;
        final ByteBuffer receiveBuffer;

        private ChannelState(int threadIndex, String prefix, int totalRequests) {
            this.threadIndex = threadIndex;
            this.prefix = prefix;
            this.totalRequests = totalRequests;
            this.currentRequest = 1;
            this.sendBuffer = ByteBuffer.allocate(HelloHelper.BUFFER_SIZE);
            this.receiveBuffer = ByteBuffer.allocate(HelloHelper.BUFFER_SIZE);
            fillSendBuffer();
        }

        private void fillSendBuffer() {
            sendBuffer.clear();
            sendBuffer.put(HelloHelper.checkMessage(
                    HelloHelper.formatRequest(prefix, currentRequest, threadIndex + 1)));
            sendBuffer.flip();
        }

        private void handleWritable(SelectionKey key, PriorityQueue<Deadline> timeouts, long currentTime) throws IOException {
            final DatagramChannel dc = (DatagramChannel) key.channel();
            dc.write(sendBuffer);
            sendBuffer.rewind();
            receiveBuffer.clear();
            key.interestOps(SelectionKey.OP_READ);
            timeouts.add(new Deadline(currentTime + HelloHelper.TIMEOUT_MS, key));
        }

        private boolean handleReadable(SelectionKey key) throws IOException {
            final DatagramChannel dc = (DatagramChannel) key.channel();
            receiveBuffer.clear();
            dc.read(receiveBuffer);
            receiveBuffer.flip();
            final String response = StandardCharsets.UTF_8.decode(receiveBuffer).toString();
            final int thread = threadIndex + 1;
            if (!HelloHelper.responseChecker(prefix, currentRequest, thread).test(response)) {
                return false;
            }
            System.out.println("Message: " + HelloHelper.formatRequest(prefix, currentRequest, thread));
            System.out.println("Answer: " + response);
            currentRequest++;
            if (currentRequest > totalRequests) {
                key.cancel();
                dc.close();
                return true;
            }
            fillSendBuffer();
            key.interestOps(SelectionKey.OP_WRITE);
            return false;
        }
    }

    private static void checkTimeouts(PriorityQueue<Deadline> timeouts, long currentTime) {
        while (!timeouts.isEmpty() && timeouts.peek().at() <= currentTime) {
            final SelectionKey key = timeouts.poll().key();
            if (!key.isValid() || (key.interestOps() & SelectionKey.OP_READ) == 0) {
                continue;
            }
            final ChannelState state = (ChannelState) key.attachment();
            state.sendBuffer.rewind();
            key.interestOps(SelectionKey.OP_WRITE);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(String host, int port, String prefix, int requests, int threads) {
        final InetSocketAddress address = new InetSocketAddress(host, port);
        try (Selector selector = Selector.open()) {
            int activeChannels = threads;
            final PriorityQueue<Deadline> timeouts = new PriorityQueue<>(threads,
                    Comparator.comparingLong(Deadline::at));
            for (int i = 0; i < threads; i++) {
                final DatagramChannel ch = DatagramChannel.open();
                ch.configureBlocking(false);
                ch.connect(address);
                ch.register(selector, SelectionKey.OP_WRITE, new ChannelState(i, prefix, requests));
            }
            while (activeChannels > 0) {
                selector.select(HelloHelper.TIMEOUT_MS);
                final long currentTime = System.currentTimeMillis();
                final Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    final SelectionKey key = it.next();
                    it.remove();
                    final ChannelState state = (ChannelState) key.attachment();
                    if (key.isWritable()) {
                        state.handleWritable(key, timeouts, currentTime);
                    } else if (key.isReadable() && state.handleReadable(key)) {
                        activeChannels--;
                    }
                }
                checkTimeouts(timeouts, currentTime);
            }
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }

    /**
     * Starts the command-line client.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        new HelloClientMain(HelloUDPNonblockingClient::new, "HelloUDPNonblockingClient").main(args);
    }
}
