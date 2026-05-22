package info.kgeorgiy.ja.davydov.hello;

import java.util.function.Supplier;

import info.kgeorgiy.ja.davydov.base.AbstractMain;
import info.kgeorgiy.java.advanced.hello.NewHelloServer;

class HelloServerMain extends AbstractMain {
    private final Supplier<NewHelloServer> factory;
    private final String usageText;

    HelloServerMain(Supplier<NewHelloServer> factory, String name) {
        this.factory = factory;
        this.usageText = "Usage: " + name + " port threads";
    }

    @Override
    protected void run(String[] args) {
        final int port = parseRequiredInt(args, 0, "port");
        final int threads = parseRequiredInt(args, 1, "threads");
        final NewHelloServer server = factory.get();
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
        server.start(port, threads);
    }

    @Override
    protected String usage() {
        return usageText;
    }

    @Override
    protected int minArgs() {
        return 2;
    }

    @Override
    protected int maxArgs() {
        return 2;
    }
}
