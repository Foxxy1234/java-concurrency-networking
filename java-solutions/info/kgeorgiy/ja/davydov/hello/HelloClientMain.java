package info.kgeorgiy.ja.davydov.hello;

import java.util.function.Supplier;

import info.kgeorgiy.ja.davydov.base.AbstractMain;
import info.kgeorgiy.java.advanced.hello.HelloClient;

class HelloClientMain extends AbstractMain {
    private final Supplier<HelloClient> factory;
    private final String usageText;

    HelloClientMain(Supplier<HelloClient> factory, String className) {
        this.factory = factory;
        this.usageText = "Usage: " + className + " ip port prefix threads requests";
    }

    @Override
    protected void run(String[] args) {
        final String host = args[0];
        final int port = parseRequiredInt(args, 1, "port");
        final String prefix = args[2];
        final int threads = parseRequiredInt(args, 3, "threads");
        final int requests = parseRequiredInt(args, 4, "requests");
        factory.get().run(host, port, prefix, requests, threads);
    }

    @Override
    protected String usage() {
        return usageText;
    }

    @Override
    protected int minArgs() {
        return 5;
    }

    @Override
    protected int maxArgs() {
        return 5;
    }
}
