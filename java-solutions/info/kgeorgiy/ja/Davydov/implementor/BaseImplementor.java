package info.kgeorgiy.ja.Davydov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

public abstract class BaseImplementor {
    abstract String generateImplementation(Class<?> token) throws ImplerException;

    private static void check(boolean condition, String message) throws ImplerException {
        if (condition) {
            throw new ImplerException(message);
        }
    }

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

    protected String defaultValue(final Class<?> returnType) {
        return DEFAULT_VALUES_TYPES.getOrDefault(returnType, "null");
    }

    protected boolean isTypeAccessible(final Class<?> type, final Class<?> context) {
        final int modifiers = type.getModifiers();
        if (Modifier.isPrivate(modifiers)) {
            return false;
        }
        final String typePackage = type.getPackageName();
        final String contextPackage = context.getPackageName();
        return typePackage.equals(contextPackage) || Modifier.isPublic(modifiers);
    }

    boolean areTypesAccessibles(final Class<?>[] types, final Class<?> context) {
        for (final Class<?> type : types) {
            if (!isTypeAccessible(type, context)) {
                return false;
            }
        }
        return true;
    }

    protected void checkMethodTypesAccessible(final Method method, final Class<?> context) throws ImplerException {
        if (!areTypesAccessibles(method.getParameterTypes(), context)
                || !isTypeAccessible(method.getReturnType(), context)
                || !areTypesAccessibles(method.getExceptionTypes(), context)) {
            throw new ImplerException("Method uses inaccessible types");
        }
    }

    protected void packageDeclaration(StringBuilder sb, final Class<?> token) {
        sb.append(token.getPackageName().isEmpty() ? "" : "package " + token.getPackageName() + ";\n\n");
    }

    protected String methodKey(final Method method) {
        final StringBuilder sb = new StringBuilder();
        sb.append(method.getName()).append('(');
        parseArguments(sb, method.getParameterTypes(), true);
        return sb.toString();
    }

    protected void generateClassHeader(StringBuilder sb, final Class<?> token, final String name,
            final String relationKeyword) {
        sb.append("public class ").append(name).append("Impl ").append(relationKeyword).append(" ")
                .append(token.getCanonicalName()).append(" {\n");
    }

    protected void generateModifiers(StringBuilder sb, final Method method, final String modifiers) {
        sb.append("    public ").append(modifiers).append(method.getReturnType().getCanonicalName()).append(' ')
                .append(method.getName()).append('(');
    }

    protected void parseArguments(StringBuilder sb, Class<?>[] parametrs, boolean withNames) {
        for (int i = 0; i < parametrs.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(withNames ? parametrs[i].getCanonicalName() : "").append(" arg").append(i);
        }
        sb.append(')');
    }

    protected void parseExceptions(StringBuilder sb, final Class<?>[] exceptions) {
        for (int i = 0; i < exceptions.length; i++) {
            sb.append(i == 0 ? " throws " : ", ").append(exceptions[i].getCanonicalName());
        }
    }

    protected void generateDefinition(StringBuilder sb, final Method method) {
        sb.append(" {\n").append("        return ").append(defaultValue(method.getReturnType())).append(";\n")
                .append("    }\n");
    }

    protected void generateMethod(StringBuilder sb, final Method method, final String modifiers) {
        generateModifiers(sb, method, modifiers);
        parseArguments(sb, method.getParameterTypes(), true);
        parseExceptions(sb, method.getExceptionTypes());
        generateDefinition(sb, method);
    }
}
