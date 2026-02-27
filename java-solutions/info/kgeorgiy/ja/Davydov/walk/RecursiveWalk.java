package info.kgeorgiy.ja.Davydov.walk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

class PrintHashForFiles<T extends PrefixHashFunction> implements FileVisitor<Path> {

	private final T hashFunc;
	private final BufferedWriter writer;

	public PrintHashForFiles(T hashFunc, BufferedWriter writer) {
		this.hashFunc = hashFunc;
		this.writer = writer;
	}

	private void writeHash(Path path) throws IOException {
		byte[] h = hashFunc.getHash();
		writer.write(String.format("%02x%02x%02x%02x %s%n", h[0], h[1], h[2], h[3], path.toString()));
	}

	private void writeZero(Path path) throws IOException {
		writer.write(String.format("00000000 %s%n", path.toString()));
	}

	@Override
	public FileVisitResult postVisitDirectory(Path path, IOException e) throws IOException {
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attr) throws IOException {
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(Path path, BasicFileAttributes attr) throws IOException {
		hashFunc.reset();
		try (InputStream is = Files.newInputStream(path)) {
			byte[] buf = new byte[4096];
			int bytesRead;
			while ((bytesRead = is.read(buf)) != -1) {
				hashFunc.update(buf, 0, bytesRead);
			}
		} catch (IOException e) {
			writeZero(path);
			return FileVisitResult.CONTINUE;
		}
		writeHash(path);
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFileFailed(Path path, IOException e) throws IOException {
		writeZero(path);
		return FileVisitResult.CONTINUE;
	}
}

public class RecursiveWalk {
	public static void main(String[] args) {
		if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
			System.err.println("Invalid usage");
			return;
		}
		Path input, output;
		try {
			input = Path.of(args[0]);
			output = Path.of(args[1]);
		} catch (InvalidPathException e) {
			System.err.println("Invalid path: " + e.getMessage());
			return;
		}
		if (output.getParent() != null) {
			try {
				Files.createDirectories(output.getParent());
			} catch (IOException e) {
				System.err.println("Cannot create directories for output: " + e.getMessage());
				return;
			}
		}
		try (
				BufferedReader reader = Files.newBufferedReader(input, StandardCharsets.UTF_8);
				BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
			String s;
			while ((s = reader.readLine()) != null) {
				try {
					Path start = Path.of(s);
					Files.walkFileTree(start, new PrintHashForFiles<>(new FNVHashFunction(), writer));
				} catch (IOException | InvalidPathException e) {
					writer.write(String.format("00000000 %s%n", s));
				}
			}
		} catch (IOException e) {
			System.err.println("I/O error with your files: " + e.getMessage());
		}
	}
}