package info.kgeorgiy.ja.Davydov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class ClassImplementor extends BaseImplementor {

    /**
     * Appends all accessible constructors for generated implementation class.
     *
     * @param sb       target source builder.
     * @param token    class token being extended.
     * @param nameImpl generated implementation class name.
     * @throws ImplerException if there are no accessible constructors.
     */
    private void generateConstructors(StringBuilder sb, final Class<?> token, final String nameImpl)
            throws ImplerException {
        final Constructor<?>[] constructors = token.getDeclaredConstructors();
        if (constructors.length == 0) {
            sb.append(String.join("", INDENT, "public ", escapeUnicode(nameImpl), "() {", NL, DOUBLE_INDENT, "super();",
                    NL, INDENT, "}", NL));
            return;
        }

        boolean accessible = false;
        for (final Constructor<?> constructor : constructors) {
            if (Modifier.isPrivate(constructor.getModifiers())
                    || !areTypesAccessibles(constructor.getParameterTypes(), token)) {
                continue;
            }
            accessible = true;
            sb.append(generateConstructor(constructor, nameImpl));
        }
        if (!accessible) {
            throw new ImplerException("No accessible constructors found");
        }
    }

    /**
     * Appends implementations for all abstract methods that must be overridden.
     *
     * @param sb    target source builder.
     * @param token class token being extended.
     * @throws ImplerException if an abstract method uses inaccessible types.
     */
    private void generateAbstractMethods(StringBuilder sb, final Class<?> token) throws ImplerException {
        final Map<String, Method> methods = collectAbstractMethods(token);
        for (final Method method : methods.values()) {
            checkMethodTypesAccessible(method, token);
            generateMethod(sb, method, "");
        }
    }

    /**
     * Collects abstract methods that require implementation in generated class.
     *
     * @param token class token being extended.
     * @return ordered map of method keys to abstract methods.
     */
    private Map<String, Method> collectAbstractMethods(final Class<?> token) {
        final Map<String, Method> result = new LinkedHashMap<>();
        final List<Class<?>> classes = new ArrayList<>();
        for (Class<?> node = token; node != null; node = node.getSuperclass()) {
            classes.add(node);
        }
        Collections.reverse(classes);
        for (final Class<?> currentClass : classes) {
            for (final Method method : currentClass.getDeclaredMethods()) {
                final int modifiers = method.getModifiers();
                if (Modifier.isPrivate(modifiers)) {
                    continue;
                }
                final String key = methodKey(method);
                if (Modifier.isAbstract(modifiers)) {
                    result.put(key, method);
                } else {
                    result.remove(key);
                }
            }
        }
        for (final Method method : token.getMethods()) {
            final int modifiers = method.getModifiers();
            if (Modifier.isStatic(modifiers) || Modifier.isPrivate(modifiers) || !Modifier.isAbstract(modifiers)) {
                continue;
            }
            final String key = methodKey(method);
            result.putIfAbsent(key, method);
        }
        return result;
    }

    /**
     * Generates source code for constructor delegating to {@code super(...)}.
     *
     * @param constructor source constructor token.
     * @param implName    generated implementation class name.
     * @return generated constructor source code.
     */
    private String generateConstructor(final Constructor<?> constructor, final String implName) {
        final StringBuilder sb = new StringBuilder();
        sb.append(String.join("", INDENT, "public ", escapeUnicode(implName), "("));
        generateArguments(sb, constructor.getParameterTypes(), true);
        generateExceptions(sb, constructor.getExceptionTypes());
        sb.append(String.join("", " {", NL, DOUBLE_INDENT, "super("));
        generateArguments(sb, constructor.getParameterTypes(), false);
        sb.append(String.join("", ";", NL, INDENT, "}", NL));
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String generateImplementation(final Class<?> token) throws ImplerException {
        validateToken(token);
        final StringBuilder sb = new StringBuilder();
        packageDeclaration(sb, token);
        generateClassHeader(sb, token, token.getSimpleName(), "extends");
        generateConstructors(sb, token, token.getSimpleName() + "Impl");
        generateAbstractMethods(sb, token);
        sb.append("}").append(NL);
        return sb.toString();
    }
}