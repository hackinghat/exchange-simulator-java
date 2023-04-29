package com.hackinghat.agent.parameter;

import com.hackinghat.util.RandomSource;

public class UniformAgentParameter<ParameterType extends Number> extends AgentParameter<ParameterType> {
    private final Class<ParameterType> parameterTypeClass;
    private final RandomSource randomSource;
    private final ParameterType min;
    private final ParameterType diff;

    public UniformAgentParameter(final String name, final Class<ParameterType> parameterTypeClass, final RandomSource randomSource, final ParameterType min, final ParameterType max) {
        super(name);
        this.randomSource = randomSource;
        this.parameterTypeClass = parameterTypeClass;
        this.min = min;
        if (parameterTypeClass == Double.class)
            diff = parameterTypeClass.cast(max.doubleValue() - min.doubleValue());
        else if (parameterTypeClass == Integer.class)
            diff = parameterTypeClass.cast(max.intValue() - min.intValue());
        else throw new IllegalArgumentException("Uniform parameters must be integer or double");
    }

    @Override
    public ParameterType next() {
        if (parameterTypeClass == Double.class)
            return parameterTypeClass.cast(randomSource.nextDouble() * diff.doubleValue() + min.doubleValue());
        else if (parameterTypeClass == Integer.class)
            return parameterTypeClass.cast(randomSource.nextInt(diff.intValue()) + min.intValue());
        else throw new IllegalArgumentException("Uniform parameters must be integer or double");
    }

}
