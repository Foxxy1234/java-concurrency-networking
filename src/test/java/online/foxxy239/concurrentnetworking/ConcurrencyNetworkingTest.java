package online.foxxy239.concurrentnetworking;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import online.foxxy239.concurrentnetworking.bank.RemoteBank;
import online.foxxy239.concurrentnetworking.parallel.ParallelMapper;
import online.foxxy239.concurrentnetworking.udp.NonBlockingUdpEchoServer;

class ConcurrencyNetworkingTest {
    @Test
    void bankPreservesBalanceUnderConcurrentWithdrawals() throws Exception {
        try (RemoteBank bank = new RemoteBank()) {
            bank.createAccount("wallet");
            bank.changeAmount("wallet", 80);
            try (var workers = Executors.newFixedThreadPool(16)) {
                for (int index = 0; index < 100; index++) {
                    workers.submit(() -> bank.changeAmount("wallet", -1));
                }
                workers.shutdown();
                workers.awaitTermination(5, TimeUnit.SECONDS);
            }
            assertEquals(0, bank.amount("wallet"));
        }
    }

    @Test
    void parallelMapperKeepsInputOrder() throws Exception {
        try (ParallelMapper mapper = new ParallelMapper(2)) {
            assertEquals(List.of(1, 4, 9, 16), mapper.map(value -> value * value, List.of(1, 2, 3, 4)));
        }
    }

    @Test
    void nonBlockingUdpServerRepliesToDatagram() throws Exception {
        try (NonBlockingUdpEchoServer server = new NonBlockingUdpEchoServer()) {
            server.start(0);
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setSoTimeout(2_000);
                byte[] request = "hello".getBytes(StandardCharsets.UTF_8);
                socket.send(new DatagramPacket(request, request.length, InetAddress.getLoopbackAddress(), server.port()));
                byte[] response = new byte[32];
                DatagramPacket packet = new DatagramPacket(response, response.length);
                socket.receive(packet);
                assertEquals("Hello, hello", new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8));
            }
        }
    }
}
