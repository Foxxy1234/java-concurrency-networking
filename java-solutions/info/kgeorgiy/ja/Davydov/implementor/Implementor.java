package info.kgeorgiy.ja.Davydov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class Implementor implements Impler {

    public static void main(final String[] args) {
        if (args == null || args.length != 1) {
            System.err.println("Usage: Implementor <class-name>");
            return;
        }
        try {
            final Class<?> token = Class.forName(args[0]);
            final Path root = Path.of(".");
            new Implementor().implement(token, root);
        } catch (final ClassNotFoundException e) {
            System.err.println("Class not found: " + e.getMessage());
        } catch (final ImplerException e) {
            System.err.println("Implementation failed: " + e.getMessage());
        }
    }

    @Override
    public void implement(final Class<?> token, final Path root) throws ImplerException {
        if (token == null || root == null) {
            throw new IllegalArgumentException("Token and root path must not be null");
        }
        final Path outputFile = getPathByToken(token, root);
        try {
            Files.createDirectories(outputFile.getParent());
        } catch (IOException e) {
            throw new ImplerException("Failed to create directories for .java file: " + e.getMessage(), e);
        }
        try (final Writer writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
            BaseImplementor implementor = token.isInterface() ? new InterfaceImplementor() : new ClassImplementor();
            writer.write(implementor.generateImplementation(token));
        } catch (final IOException e) {
            throw new ImplerException("Failed to write implementation file: " + e.getMessage(), e);
        }
    }

    private Path getPathByToken(final Class<?> token, final Path root) {
        final String path = token.getPackageName().replace('.', File.separatorChar);
        final String name = token.getSimpleName() + "Impl.java";
        return root.resolve(path).resolve(name);
    }
}