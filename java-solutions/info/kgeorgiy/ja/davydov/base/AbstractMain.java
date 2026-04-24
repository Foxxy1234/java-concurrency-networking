package info.kgeorgiy.ja.davydov.base;

/**
 * Base template for command-line entrypoints.
 */
public abstract class AbstractMain {
    /**
     * Runs command-line processing with common validation and error handling.
     *
     * @param args command-line arguments
     */
    public final void main(final String[] args) {
        if (args == null || args.length < minArgs() || args.length > maxArgs()) {
            printUsage();
            return;
        }
        try {
            run(args);
        } catch (IllegalArgumentException e) {
            printUsage();
        } catch (Exception e) {
            handleException(e);
        }
    }

    /**
     * Executes command-specific logic.
     *
     * @param args validated command-line arguments
     * @throws Exception when execution fails
     */
    protected abstract void run(String[] args) throws Exception;

    /**
     * Returns usage string to print on invalid input.
     *
     * @return usage message
     */
    protected abstract String usage();

    /**
     * Minimum number of accepted arguments.
     *
     * @return minimum argument count
     */
    protected int minArgs() {
        return 0;
    }

    /**
     * Maximum number of accepted arguments.
     *
     * @return maximum argument count
     */
    protected int maxArgs() {
        return Integer.MAX_VALUE;
    }

    /**
     * Parses optional integer argument with a default fallback.
     *
     * @param args         command-line arguments
     * @param index        argument index
     * @param defaultValue fallback value when argument is absent
     * @param argumentName logical argument name for error reporting
     * @return parsed integer or fallback value
     * @throws IllegalArgumentException if argument is present but not an integer
     */
    protected final int parseOptionalInt(final String[] args, final int index, final int defaultValue,
            final String argumentName) {
        if (args.length <= index) {
            return defaultValue;
        }
        return parseInt(args[index], argumentName);
    }

    /**
     * Parses required integer argument.
     *
     * @param args         command-line arguments
     * @param index        argument index
     * @param argumentName logical argument name for error reporting
     * @return parsed integer value
     * @throws IllegalArgumentException if argument is missing or not an integer
     */
    protected final int parseRequiredInt(final String[] args, final int index, final String argumentName) {
        if (args.length <= index) {
            throw new IllegalArgumentException("Missing argument: " + argumentName);
        }
        return parseInt(args[index], argumentName);
    }

    /**
     * Prints usage message.
     */
    protected void printUsage() {
        System.out.println(usage());
    }

    /**
     * Handles execution failures after argument validation.
     *
     * @param e failure cause
     */
    protected void handleException(final Exception e) {
        System.err.println(e.getMessage());
    }

    private static int parseInt(final String value, final String argumentName) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Argument '" + argumentName + "' is not an integer", e);
        }
    }
}
