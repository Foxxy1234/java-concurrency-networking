package info.kgeorgiy.ja.Davydov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.tools.JarImpler;

import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class Implementor implements JarImpler {

    private static final String JAVA_FILE = ".java";

    private static void printUsage() {
        System.err.println("Usage:\n" + "  Implementor <class-name>\n" + "  Implementor -jar <class-name> <jar-file>");
    }

    public static void main(final String[] args) {
        if (args == null) {
            printUsage();
            return;
        }
        try {
            if (args.length == 1) {
                final Class<?> token = Class.forName(args[0]);
                final Path root = Path.of(".");
                new Implementor().implement(token, root);
            } else if (args.length == 3 && "-jar".equals(args[0])) {
                final Class<?> token = Class.forName(args[1]);
                final Path jarFile = Path.of(args[2]);
                new Implementor().implementJar(token, jarFile);
            } else {
                printUsage();
            }
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
        final Path outputFile = JarCreationTools.getPathWithImpl(token, root, JAVA_FILE);
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

    @Override
    public void implementJar(final Class<?> token, final Path jarFile) throws ImplerException {
        if (token == null || jarFile == null) {
            throw new IllegalArgumentException("Token and jarFile must not be null");
        }
        final Path root = JarCreationTools.createTmpRoot(jarFile);
        try {
            implement(token, root);
            JarCreationTools.compiler(token, root, List.of(token, Implementor.class, ImplerException.class));
            JarCreationTools.writeJar(token, root, jarFile);
        } finally {
            JarCreationTools.deleteRecursively(root);
        }
    }
}
