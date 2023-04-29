package com.hackinghat.agent.parameter;

import com.hackinghat.util.Nameable;

public abstract class AgentParameter<ParameterType extends Number> implements Nameable {
    private final String name;

    public AgentParameter(final String name) {
        this.name = name;
    }

    public abstract ParameterType next();

    @Override
    public String getName() {
        return name;
    }
}
