package info.kgeorgiy.ja.Davydov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class InterfaceImplementor extends BaseImplementor {

    /**
     * {@inheritDoc}
     */
    @Override
    public String generateImplementation(final Class<?> token) throws ImplerException {
        validateToken(token);
        final StringBuilder sb = new StringBuilder();
        packageDeclaration(sb, token);
        generateClassHeader(sb, token, token.getSimpleName(), "implements");
        final Set<String> checked = new HashSet<>();
        for (final Method method : token.getMethods()) {
            if (shouldSkipMethod(method) || method.isDefault()) {
                continue;
            }
            checkMethodTypesAccessible(method, token);
            final String key = methodKey(method);
            if (!checked.add(key)) {
                continue;
            }
            generateMethod(sb, method, "");
        }
        sb.append("}").append(NL);
        return sb.toString();
    }
}
