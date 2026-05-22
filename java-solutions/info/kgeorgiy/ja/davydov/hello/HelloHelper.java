package info.kgeorgiy.ja.davydov.hello;

import java.nio.charset.StandardCharsets;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class HelloHelper {
    static final int BUFFER_SIZE = 8192;
    static final int TIMEOUT_MS = 100;

    private HelloHelper() {
    }

    public static String formatRequest(String prefix, int request, int thread) {
        return prefix + request + "_" + thread;
    }

    public static String formatResponse(String format, String request) {
        return format.replaceAll("%%", request);
    }

    public static byte[] checkMessage(String s) {
        final byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > BUFFER_SIZE) {
            throw new IllegalArgumentException(
                    "Message too large: " + bytes.length + " bytes > BUFFER_SIZE (" + BUFFER_SIZE + ")");
        }
        return bytes;
    }

    public static Predicate<String> responseChecker(String prefix, int request, int thread) {
        final Pattern pattern = Pattern.compile(".*\\Q" + prefix + "\\E([\\p{Nd}]+)_([\\p{Nd}]+).*");
        return response -> {
            final Matcher matcher = pattern.matcher(response);
            return matcher.matches()
                    && Integer.parseInt(matcher.group(1)) == request
                    && Integer.parseInt(matcher.group(2)) == thread;
        };
    }
}
