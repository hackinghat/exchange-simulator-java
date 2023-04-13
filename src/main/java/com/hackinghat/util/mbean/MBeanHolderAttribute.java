package com.hackinghat.util.mbean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class MBeanHolderAttribute {
    private static final Logger LOG = LogManager.getLogger(MBeanHolderAttribute.class);

    private final String name;
    private final String description;
    private Method getter;
    private Method setter;

    MBeanHolderAttribute(final String name, final String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public Method getGetter() { return getter; }
    public void setGetter(final Method getter) { this.getter = getter; }
    public Method getSetter() { return setter; }
    public void setSetter(final Method setter) { this.setter = setter; }

    public Object invokeGetter(final Object obj) {
        try {
            final Object result = getter.invoke(obj);
            return result == null ? "" : result.toString();
        }
        catch (IllegalAccessException|InvocationTargetException ex) {
            LOG.error("Error getting value: " + name + ", because: " , ex);
            return ex.getMessage();
        }
    }

    public void invokeSetter(final Object obj, final Object arg1) {
        try {
            setter.invoke(obj, arg1);
        }
        catch (IllegalAccessException|InvocationTargetException ex) {
            LOG.error("Error setting value: " + name + ", because: " , ex);
        }
    }

    MBeanAttributeInfo getAttribute() throws IntrospectionException {
        return new MBeanAttributeInfo(name, description, getter, setter);
    }
}
