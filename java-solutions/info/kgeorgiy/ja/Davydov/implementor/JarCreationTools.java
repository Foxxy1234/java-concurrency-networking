package info.kgeorgiy.ja.Davydov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.jar.*;
import java.util.stream.*;

final class JarCreationTools {
    private static final String IMPL_SUFFIX = "Impl";
    private static final String JAVA_FILE = ".java";
    private static final String CLASS_FILE = ".class";

    private JarCreationTools() {
    }

    static Path createTmpRoot(final Path path) throws ImplerException {
        final Path parent = path.toAbsolutePath().getParent();
        if (parent == null) {
            throw new ImplerException("Jar file parent directory is undefined: " + path);
        }
        try {
            Files.createDirectories(parent);
            return Files.createTempDirectory(parent, "implementor-");
        } catch (final IOException e) {
            throw new ImplerException("Failed to create temporary directory", e);
        }
    }

    static void compiler(final Class<?> token, final Path root, final List<Class<?>> dependencies)
            throws ImplerException {
        checkJavaCompilerAccessible();
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new ImplerException("Could not find java compiler, include tools.jar to classpath");
        }

        final Path sourceFile = getPathWithImpl(token, root, JAVA_FILE);
        final String classpath = getClassPath(dependencies).stream().map(Path::toString)
                .collect(Collectors.joining(File.pathSeparator));

        final String[] args = Stream.of("-cp", classpath, "-encoding", StandardCharsets.UTF_8.name(), "-d",
                root.toString(), sourceFile.toString()).toArray(String[]::new);
        final int exitCode = compiler.run(null, null, null, args);
        if (exitCode != 0) {
            throw new ImplerException("Compilation failed with exit code " + exitCode);
        }
    }

    static void writeJar(final Class<?> token, final Path root, final Path jarFile) throws ImplerException {
        final String entryName = toJarEntryName(token);
        final Path classFile = getPathWithImpl(token, root, CLASS_FILE);
        if (!Files.isRegularFile(classFile)) {
            throw new ImplerException("Compiled class not found: " + classFile);
        }
        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try (final JarOutputStream jos = new JarOutputStream(
                Files.newOutputStream(jarFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING),
                manifest)) {
            jos.putNextEntry(new JarEntry(entryName));
            Files.copy(classFile, jos);
            jos.closeEntry();
        } catch (final IOException e) {
            throw new ImplerException("Failed to write jar: " + jarFile, e);
        }
    }

    private static String[] getPackageParts(final Class<?> token) {
        return token.getPackageName().split("\\.");
    }

    static Path getPathWithImpl(final Class<?> token, final Path root, final String extension) {
        return Arrays.stream(getPackageParts(token)).reduce(root, Path::resolve, (a, b) -> b)
                .resolve(token.getSimpleName() + IMPL_SUFFIX + extension);
    }

    private static void checkJavaCompilerAccessible() throws ImplerException {
        final Module thisModule = JarCreationTools.class.getModule();
        final Module compilerModule = ModuleLayer.boot().findModule("java.compiler")
                .orElseThrow(() -> new ImplerException("Module java.compiler is not present"));
        thisModule.addReads(compilerModule);
    }

    private static List<Path> getClassPath(final List<Class<?>> dependencies) {
        return dependencies.stream().map(dependency -> {
            try {
                return Path.of(dependency.getProtectionDomain().getCodeSource().getLocation().toURI());
            } catch (final URISyntaxException e) {
                throw new AssertionError(e);
            }
        }).toList();
    }

    private static String toJarEntryName(final Class<?> token) {
        return String.join("/", getPackageParts(token)) + "/" + token.getSimpleName() + IMPL_SUFFIX + CLASS_FILE;
    }

    private static final SimpleFileVisitor<Path> DELETE_VISITOR = new SimpleFileVisitor<>() {
        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes atrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(final Path dir, final IOException e) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }
    };

    static void deleteRecursively(final Path root) throws ImplerException {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try {
            Files.walkFileTree(root, DELETE_VISITOR);
        } catch (final IOException | UncheckedIOException e) {
            throw new ImplerException("Failed to delete temporary directory", e);
        }
    }
}
