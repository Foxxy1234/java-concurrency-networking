package info.kgeorgiy.ja.Davydov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

public abstract class BaseImplementor {
    protected static final String NL = System.lineSeparator();

    /**
     * Generates source code for implementation of the specified token.
     *
     * @param token type token to implement.
     * @return generated Java source code.
     * @throws ImplerException if implementation cannot be generated.
     */
    abstract String generateImplementation(Class<?> token) throws ImplerException;

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
     * Builds a unique key for method name and parameter types.
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
        sb.append("public class ").append(name).append("Impl ").append(relationKeyword).append(" ")
                .append(token.getCanonicalName()).append(" {").append(NL);
    }

    /**
     * Appends method modifiers, return type and method name.
     *
     * @param sb        target source builder.
     * @param method    method token.
     * @param modifiers additional modifiers string.
     */
    protected void generateModifiers(StringBuilder sb, final Method method, final String modifiers) {
        sb.append("    public ").append(modifiers).append(method.getReturnType().getCanonicalName()).append(' ')
                .append(method.getName()).append('(');
    }

    /**
     * Appends method or constructor arguments.
     *
     * @param sb        target source builder.
     * @param parametrs argument types.
     * @param withNames if {@code true}, appends type names before argument names.
     */
    protected void generateArguments(StringBuilder sb, Class<?>[] parametrs, boolean withNames) {
        for (int i = 0; i < parametrs.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(withNames ? parametrs[i].getCanonicalName() : "").append(" arg").append(i);
        }
        sb.append(')');
    }

    /**
     * Appends declared exception list.
     *
     * @param sb         target source builder.
     * @param exceptions declared exception types.
     */
    protected void generateExceptions(StringBuilder sb, final Class<?>[] exceptions) {
        for (int i = 0; i < exceptions.length; i++) {
            sb.append(i == 0 ? " throws " : ", ").append(exceptions[i].getCanonicalName());
        }
    }

    /**
     * Appends method body with default return value.
     *
     * @param sb     target source builder.
     * @param method implemented method token.
     */
    protected void generateDefinition(StringBuilder sb, final Method method) {
        sb.append(" {").append(NL).append("        return ").append(defaultValue(method.getReturnType())).append(";")
                .append(NL).append("    }").append(NL);
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
