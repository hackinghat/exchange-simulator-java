package com.hackinghat.util.mbean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.management.MBeanOperationInfo;
import java.lang.reflect.Method;
import java.util.Objects;

public class MBeanHolderOperation {
    private static final Logger LOG = LogManager.getLogger(MBeanHolderAttribute.class);

    private final String description;
    private final Method method;

    MBeanHolderOperation(final String description, final Method method) {
        Objects.requireNonNull(method);
        this.description = description;
        this.method = method;
    }

    public String getDescription() {
        return description;
    }

    public Method getMethod() {
        return method;
    }

    MBeanOperationInfo getOperation() {
        return new MBeanOperationInfo(description, method);
    }

}
