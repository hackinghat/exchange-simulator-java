package com.hackinghat.util.mbean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MBeanBuilder {
    private static final Logger LOG = LogManager.getLogger(MBeanBuilder.class);

    static String getAttributeName(final String methodName) {
        if (methodName == null)
            throw new IllegalArgumentException("Method name can not be null!");
        final int startPoint = (methodName.length() > 3 && (methodName.startsWith("get") || methodName.startsWith("set"))) ? 3 :
                (methodName.length() > 2 && methodName.startsWith("is")) ? 2 : 0;
        final StringBuilder aName = new StringBuilder();
        boolean lastUpCase = true;
        for (int i = startPoint; i < methodName.length(); ++i) {
            final char charAt = methodName.charAt(i);
            final boolean isUpcase = Character.isUpperCase(charAt);
            if (!lastUpCase && isUpcase)
                aName.append(" ");
            aName.append(charAt);
            lastUpCase = isUpcase;
        }
        return aName.toString();
    }

    static Class<?> getReturnType(final Method method) {
        Class<?> type = method.getReturnType();
        return (type == void.class) ? null : type;
    }

    static Class<?>[] getParameterTypes(final Method method) {
        return method.getParameterTypes();
    }

    public static MBeanInfo getMBeanInfo(final Class<?> mbeanType, final MBeanHolderAttribute[] attrs, final MBeanHolderOperation[] ops) {
        final MBeanType t = mbeanType.getAnnotation(MBeanType.class);
        if (t == null)
            throw new IllegalArgumentException(String.format("%s is not an MBeanType", mbeanType.getName()));
        final List<MBeanAttributeInfo> info = new ArrayList<>();
        for (final MBeanHolderAttribute a : attrs) {
            try {
                info.add(a.getAttribute());
            } catch (IntrospectionException ex) {
                LOG.error("Unexpected exception, building MBeanAttribute", ex);
            }
        }
        final List<MBeanOperationInfo> opsInfo = new ArrayList<>();
        for (final MBeanHolderOperation op : ops) {
            opsInfo.add(op.getOperation());
        }
        return new MBeanInfo(mbeanType.getSimpleName(), t.description(), info.toArray(new MBeanAttributeInfo[0]), null, opsInfo.toArray(new MBeanOperationInfo[0]), null);
    }

    public static MBeanHolderAttribute[] getAttributes(final Class<?> mbeanType) {
        final Map<String, MBeanHolderAttribute> attributes = new HashMap<>();
        for (final Method m : mbeanType.getMethods()) {
            final MBeanAttribute a = m.getAnnotation(MBeanAttribute.class);
            if (a != null) {
                final Class<?> returnType = getReturnType(m);
                final Class<?>[] parameterType = getParameterTypes(m);

                final boolean hasReturnType = returnType != null;
                final boolean hasParameters = parameterType.length > 0;
                if (hasParameters != hasReturnType) {
                    final String attrName = getAttributeName(m.getName());
                    MBeanHolderAttribute x = attributes.get(attrName);
                    if (x == null) {
                        x = new MBeanHolderAttribute(attrName, a.description());
                        attributes.put(attrName, x);
                    }
                    if (hasReturnType) {
                        x.setGetter(m);
                    } else {
                        x.setSetter(m);
                    }
                } else {
                    throw new IllegalArgumentException(m.getName() + " is" +
                            (hasReturnType ? "" : " not") + " a getter and is " +
                            (hasParameters ? "" : " not") + " a setter which is not the correct contract for an MBeanAttribute");
                }
            }
        }
        return attributes.values().toArray(new MBeanHolderAttribute[0]);
    }

    public static MBeanHolderOperation[] getOperations(final Class<?> mbeanType) {
        final List<MBeanHolderOperation> ops = new ArrayList<>();
        for (final Method m : mbeanType.getMethods()) {
            final MBeanOperation o = m.getAnnotation(MBeanOperation.class);
            if (o != null) {
                ops.add(new MBeanHolderOperation(o.description(), m));
            }
        }
        return ops.toArray(new MBeanHolderOperation[0]);
    }

}
