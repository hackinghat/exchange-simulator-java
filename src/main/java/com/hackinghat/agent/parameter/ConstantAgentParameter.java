package com.hackinghat.agent.parameter;

public class ConstantAgentParameter<ParameterType extends Number> extends AgentParameter<ParameterType> {
    private final ParameterType parameter;

    public ConstantAgentParameter(final String name, final ParameterType constant) {
        super(name);
        this.parameter = constant;
    }

    public static <ParameterType extends Number> ConstantAgentParameter<ParameterType> of(final String name, final ParameterType parameter) {
        return new ConstantAgentParameter<>(name, parameter);
    }

    @Override
    public ParameterType next() {
        return parameter;
    }
}
