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

    /**
     * Utility class constructor.
     */
    private JarCreationTools() {
    }

    /**
     * Creates temporary directory near target jar file.
     *
     * @param path target jar file path.
     * @return temporary directory path.
     * @throws ImplerException if directory cannot be created.
     */
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

    /**
     * Compiles generated implementation source file.
     *
     * @param token        source token.
     * @param root         root directory where generated files are located.
     * @param dependencies classes to include in compilation classpath.
     * @throws ImplerException if compiler is unavailable or compilation fails.
     */
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

    /**
     * Packs compiled implementation class into jar archive.
     *
     * @param token   source token.
     * @param root    root directory with compiled class.
     * @param jarFile target jar file.
     * @throws ImplerException if jar cannot be written.
     */
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

    /**
     * Splits package name into path segments.
     *
     * @param token source token.
     * @return array of package path segments.
     */
    private static String[] getPackageParts(final Class<?> token) {
        return token.getPackageName().split("\\.");
    }

    /**
     * Builds full path to generated implementation file.
     *
     * @param token     source token.
     * @param root      target root directory.
     * @param extension file extension, for example {@code .java} or {@code .class}.
     * @return path to generated implementation file.
     */
    static Path getPathWithImpl(final Class<?> token, final Path root, final String extension) {
        return Arrays.stream(getPackageParts(token)).reduce(root, Path::resolve, (a, b) -> b)
                .resolve(token.getSimpleName() + IMPL_SUFFIX + extension);
    }

    /**
     * Ensures current module can read {@code java.compiler} module.
     *
     * @throws ImplerException if {@code java.compiler} module is unavailable.
     */
    private static void checkJavaCompilerAccessible() throws ImplerException {
        final Module thisModule = JarCreationTools.class.getModule();
        final Module compilerModule = ModuleLayer.boot().findModule("java.compiler")
                .orElseThrow(() -> new ImplerException("Module java.compiler is not present"));
        thisModule.addReads(compilerModule);
    }

    /**
     * Resolves classpath entries for required dependencies.
     *
     * @param dependencies classes whose code sources form the classpath.
     * @return list of classpath paths.
     */
    private static List<Path> getClassPath(final List<Class<?>> dependencies) {
        return dependencies.stream().map(dependency -> {
            try {
                return Path.of(dependency.getProtectionDomain().getCodeSource().getLocation().toURI());
            } catch (final URISyntaxException e) {
                throw new AssertionError(e);
            }
        }).toList();
    }

    /**
     * Converts implementation class path to jar entry name.
     *
     * @param token source token.
     * @return jar entry name for compiled implementation class.
     */
    private static String toJarEntryName(final Class<?> token) {
        return String.join("/", getPackageParts(token)) + "/" + token.getSimpleName() + IMPL_SUFFIX + CLASS_FILE;
    }

    private static final SimpleFileVisitor<Path> DELETE_VISITOR = new SimpleFileVisitor<>() {
        /**
         * Deletes visited file.
         *
         * @param file file to delete.
         * @param atrs file attributes.
         * @return continue file tree traversal.
         * @throws IOException if deletion fails.
         */
        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes atrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        /**
         * Deletes directory after all nested entries are processed.
         *
         * @param dir directory to delete.
         * @param e   I/O exception that terminated directory iteration, if any.
         * @return continue file tree traversal.
         * @throws IOException if deletion fails.
         */
        @Override
        public FileVisitResult postVisitDirectory(final Path dir, final IOException e) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }
    };

    /**
     * Deletes directory recursively.
     *
     * @param root directory to delete.
     * @throws ImplerException if recursive deletion fails.
     */
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
