package info.kgeorgiy.ja.Davydov.walk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Set;

public class Walk {
	public static void main(String[] args) {
		if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
			System.err.println("Wrong usage");
			return;
		}

		final Path input, output;
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
				System.err.println("Cannot create output directories: " + e.getMessage());
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
					Files.walkFileTree(start, Set.of(), 0, new PrintHashForFiles<>(new FNVHashFunction(), writer));
				} catch (IOException | InvalidPathException e) {
					// :NOTE: 000 получать из FNVHashFunction
					writer.write(String.format("00000000 %s%n", s));
				}
			}
		} catch (IOException e) {
			System.err.println("I/O error with your files: " + e.getMessage());
		}
	}
}