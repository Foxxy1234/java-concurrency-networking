package info.kgeorgiy.ja.Davydov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class BaseImplementor {
    protected static final String NL = System.lineSeparator();
    protected static final String INDENT = "    ";
    protected static final String DOUBLE_INDENT = INDENT + INDENT;

    /**
     * Generates source code for implementation of the specified token.
     *
     * @param token type token to implement.
     * @return generated Java source code.
     * @throws ImplerException if implementation cannot be generated.
     */
    abstract String generateImplementation(Class<?> token) throws ImplerException;

    /**
     * Escapes Unicode characters in string to Java source code format. Characters
     * outside ASCII range (0-127) are converted to \\uXXXX escape sequences.
     *
     * @param str string to escape.
     * @return escaped string suitable for Java source code.
     */
    protected static String escapeUnicode(final String str) {
        if (str == null) {
            return null;
        }
        final StringBuilder sb = new StringBuilder();
        for (final char c : str.toCharArray()) {
            if (c >= 128) {
                sb.append(String.format("\\u%04x", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Throws an {@link ImplerException} when a validation condition is met.
     *
     * @param condition condition that indicates a validation failure.
     * @param message   exception message.
     * @throws ImplerException if {@code condition} is {@code true}.
     */
    private static void check(boolean condition, String message) throws ImplerException {
        if (condition) {
            throw new ImplerException(message);
        }
    }

    /**
     * Validates that the token can be implemented.
     *
     * @param token type token to validate.
     * @throws ImplerException if the token cannot be implemented.
     */
    protected void validateToken(final Class<?> token) throws ImplerException {
        check(token == null, "Token must not be null");
        check(token.isPrimitive(), "Cannot implement primitive type");
        check(token.isArray(), "Cannot implement array type");
        check(Modifier.isFinal(token.getModifiers()), "Cannot extend/implement final type");
        check(Modifier.isPrivate(token.getModifiers()), "Cannot access private type");
        check(token.isEnum() || token == Enum.class, "Cannot implement enum type");
        check(token.isRecord() || token == Record.class, "Cannot implement record type");
    }

    private static final Map<Class<?>, String> DEFAULT_VALUES_TYPES = Map.of(void.class, "", boolean.class, "false",
            char.class, "'\\0'", byte.class, "0", short.class, "0", int.class, "0", long.class, "0L", float.class,
            "0.0f", double.class, "0.0");

    /**
     * Returns Java default value literal for the specified return type.
     *
     * @param returnType method return type.
     * @return default literal as source code.
     */
    protected String defaultValue(final Class<?> returnType) {
        return DEFAULT_VALUES_TYPES.getOrDefault(returnType, "null");
    }

    /**
     * Checks if a method should be skipped during implementation generation. Skips
     * private and non-abstract methods.
     *
     * @param method method to check.
     * @return {@code true} if method should be skipped.
     */
    protected boolean shouldSkipMethod(final Method method) {
        final int modifiers = method.getModifiers();
        return Modifier.isPrivate(modifiers) || !Modifier.isAbstract(modifiers);
    }

    /**
     * Checks whether a type is accessible from the specified context type.
     *
     * @param type    type to check.
     * @param context context where the type is used.
     * @return {@code true} if type is accessible, otherwise {@code false}.
     */
    protected boolean isTypeAccessible(final Class<?> type, final Class<?> context) {
        final int modifiers = type.getModifiers();
        if (Modifier.isPrivate(modifiers)) {
            return false;
        }
        final String typePackage = type.getPackageName();
        final String contextPackage = context.getPackageName();
        return typePackage.equals(contextPackage) || Modifier.isPublic(modifiers);
    }

    /**
     * Checks accessibility of each type from the specified context.
     *
     * @param types   types to check.
     * @param context context where types are used.
     * @return {@code true} if all types are accessible, otherwise {@code false}.
     */
    boolean areTypesAccessibles(final Class<?>[] types, final Class<?> context) {
        for (final Class<?> type : types) {
            if (!isTypeAccessible(type, context)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Verifies that method signature types are accessible from the given context.
     *
     * @param method  method to validate.
     * @param context context where generated implementation is located.
     * @throws ImplerException if method uses inaccessible parameter, return or
     *                             exception types.
     */
    protected void checkMethodTypesAccessible(final Method method, final Class<?> context) throws ImplerException {
        if (!areTypesAccessibles(method.getParameterTypes(), context)
                || !isTypeAccessible(method.getReturnType(), context)
                || !areTypesAccessibles(method.getExceptionTypes(), context)) {
            throw new ImplerException("Method uses inaccessible types");
        }
    }

    /**
     * Appends package declaration to generated source.
     *
     * @param sb    target source builder.
     * @param token implemented type token.
     */
    protected void packageDeclaration(StringBuilder sb, final Class<?> token) {
        sb.append(token.getPackageName().isEmpty() ? "" : "package " + token.getPackageName() + ";" + NL + NL);
    }

    /**
     * Builds a unique key for method name and parameter types. (kinda namemangling)
     *
     * @param method method to key.
     * @return key string used for method deduplication.
     */
    protected String methodKey(final Method method) {
        final StringBuilder sb = new StringBuilder();
        sb.append(method.getName()).append('(');
        generateArguments(sb, method.getParameterTypes(), true);
        return sb.toString();
    }

    /**
     * Appends class header for generated implementation.
     *
     * @param sb              target source builder.
     * @param token           base type token.
     * @param name            generated class base name.
     * @param relationKeyword relation keyword, for example {@code extends} or
     *                            {@code implements}.
     */
    protected void generateClassHeader(StringBuilder sb, final Class<?> token, final String name,
            final String relationKeyword) {
        sb.append(String.join("", "public class ", escapeUnicode(name), "Impl ", relationKeyword, " ",
                escapeUnicode(token.getCanonicalName()), " {", NL));
    }

    /**
     * Appends method modifiers, return type and method name.
     *
     * @param sb        target source builder.
     * @param method    method token.
     * @param modifiers additional modifiers string.
     */
    protected void generateModifiers(StringBuilder sb, final Method method, final String modifiers) {
        sb.append(String.join("", INDENT, "public ", modifiers,
                escapeUnicode(method.getReturnType().getCanonicalName()), " ", escapeUnicode(method.getName()), "("));
    }

    /**
     * Appends method or constructor arguments.
     *
     * @param sb        target source builder.
     * @param parametrs argument types.
     * @param withTypes if {@code true}, appends type names before argument names.
     */
    protected void generateArguments(StringBuilder sb, Class<?>[] parametrs, boolean withTypes) {
        final String args = IntStream
                .range(0, parametrs.length).mapToObj(i -> String.join("",
                        withTypes ? escapeUnicode(parametrs[i].getCanonicalName()) : "", " arg", String.valueOf(i)))
                .collect(Collectors.joining(", "));
        sb.append(args).append(')');
    }

    /**
     * Appends declared exception list.
     *
     * @param sb         target source builder.
     * @param exceptions declared exception types.
     */
    protected void generateExceptions(StringBuilder sb, final Class<?>[] exceptions) {
        if (exceptions.length > 0) {
            final String exceptionList = IntStream.range(0, exceptions.length)
                    .mapToObj(i -> escapeUnicode(exceptions[i].getCanonicalName())).collect(Collectors.joining(", "));
            sb.append(" throws ").append(exceptionList);
        }
    }

    /**
     * Appends method body with default return value.
     *
     * @param sb     target source builder.
     * @param method implemented method token.
     */
    protected void generateDefinition(StringBuilder sb, final Method method) {
        sb.append(String.join("", " {", NL, DOUBLE_INDENT, "return ", defaultValue(method.getReturnType()), ";", NL,
                INDENT, "}", NL));
    }

    /**
     * Appends full method implementation.
     *
     * @param sb        target source builder.
     * @param method    implemented method token.
     * @param modifiers additional modifiers string.
     */
    protected void generateMethod(StringBuilder sb, final Method method, final String modifiers) {
        generateModifiers(sb, method, modifiers);
        generateArguments(sb, method.getParameterTypes(), true);
        generateExceptions(sb, method.getExceptionTypes());
        generateDefinition(sb, method);
    }
}
