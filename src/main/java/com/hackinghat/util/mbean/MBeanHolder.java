package com.hackinghat.util.mbean;

import com.hackinghat.util.component.AbstractComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.management.*;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public class MBeanHolder implements DynamicMBean {
    protected static final Logger LOG = LogManager.getLogger(AbstractComponent.class);
    private final static Map<String, MBeanHolder> MBEAN_REGISTRY = new HashMap<>();

    private final Object sync = new Object();
    private final MBeanHolderAttribute[] attributes;
    private final MBeanHolderOperation[] operations;
    private final MBeanInfo mbeanInfo;
    private final AbstractComponent instance;
    private final String objectPrefix;
    private ObjectName name;
    private ObjectInstance mbeanInstance;
    private Throwable constructionPoint;

    public MBeanHolder(@Nonnull final AbstractComponent instance) {
        this(instance, null);
    }

    public MBeanHolder(@Nonnull final AbstractComponent instance, final String prefix) {
        Objects.requireNonNull(instance);
        Class<?> instanceType = instance.getClass();
        this.attributes = MBeanBuilder.getAttributes(instanceType);
        this.operations = MBeanBuilder.getOperations(instanceType);
        this.mbeanInfo = MBeanBuilder.getMBeanInfo(instanceType, attributes, operations);
        this.instance = instance;
        this.objectPrefix = prefix;
        this.name = null;
        this.mbeanInstance = null;
        this.constructionPoint = new Throwable();
    }

    public void registerMBean() throws InstanceAlreadyExistsException, NotCompliantMBeanException, MBeanRegistrationException {
        synchronized (sync) {
            final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            final ObjectName name = getName();
            final MBeanHolder holder = MBEAN_REGISTRY.get(name.getCanonicalName());
            if (holder != null) {
                LOG.error("Component already registered here: " + Arrays.toString(holder.constructionPoint.getStackTrace()));
            }
            this.mbeanInstance = server.registerMBean(this, name);
            MBEAN_REGISTRY.put(name.getCanonicalName(), this);
        }
    }

    public void unreigsterMBean() {
        synchronized (sync) {
            try {
                ManagementFactory.getPlatformMBeanServer().unregisterMBean(name);
                try {
                    ManagementFactory.getPlatformMBeanServer().unregisterMBean(name);
                } catch (Exception ex) {
                }
            } catch (InstanceNotFoundException | MBeanRegistrationException mbeanEx) {
                LOG.trace("Instance could not be unregitered", mbeanEx);
            }
            MBEAN_REGISTRY.remove(getName().getCanonicalName());
        }
    }

    public ObjectName getName() {
        synchronized (sync) {
            if (name == null) {
                final String candidateName = instance.getClass().getPackageName() + ":type=" + instance.getClass().getSimpleName() + (objectPrefix == null ? "" : ",name=" + objectPrefix);
                try {
                    name = new ObjectName(candidateName);
                } catch (final MalformedObjectNameException mobex) {
                    throw new IllegalArgumentException("Internal error, bad object name: " + candidateName, mobex);
                }
            }
            return name;
        }
    }


    private MBeanHolderAttribute getMBeanHolderAttribute(final String name) {
        for (final MBeanHolderAttribute attr : attributes) {
            if (attr.getName().equals(name)) {
                return attr;
            }
        }
        throw new IllegalArgumentException("Unknown attribute: " + name);
    }

    public AbstractComponent getInstance() {
        return instance;
    }

    @Override
    public Object getAttribute(String s) throws AttributeNotFoundException, MBeanException, ReflectionException {
        MBeanHolderAttribute holderAttribute = getMBeanHolderAttribute(s);
        return holderAttribute.invokeGetter(instance);
    }

    @Override
    public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
        MBeanHolderAttribute holderAttribute = getMBeanHolderAttribute(attribute.getName());
        holderAttribute.invokeSetter(instance, attribute.getValue());
    }

    @Override
    public AttributeList getAttributes(String[] strings) {
        return null;
    }

    @Override
    public AttributeList setAttributes(AttributeList attributeList) {
        return null;
    }

    @Override
    public Object invoke(String s, Object[] objects, String[] strings) throws MBeanException, ReflectionException {
        for (final MBeanHolderOperation operation : operations) {
            final Method method = operation.getMethod();
            if (method.getName().equals(s)) {
                try {
                    // We do a parameter type check just in-case there are overloads
                    Class<?>[] paramTypes = method.getParameterTypes();
                    if (objects.length != paramTypes.length)
                        continue;
                    boolean match = true;
                    for (int i = 0; i < objects.length; ++i) {
                        if (objects.getClass().equals(paramTypes[i])) {
                            match = false;
                            break;
                        }
                    }
                    if (match) {
                        return method.invoke(instance, objects);
                    }
                } catch (final Exception ex) {
                    return new MBeanException(ex, "Couldn't invoke method");
                }
            }
        }
        throw new MBeanException(new IllegalArgumentException("No such operation found with name: '" + s + "'"));
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        return mbeanInfo;
    }
}
