package com.hackinghat.agent.parameter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AgentParameterSet {
    public static final String P_INSPREAD = "P_INSPREAD";

    private final Map<String, AgentParameter<? extends Number>> parameterMap;

    @SuppressWarnings("unchecked")
    public AgentParameterSet(final AgentParameter... parameterCollection) {
        this.parameterMap = new HashMap<>();
        Arrays.stream(parameterCollection).forEach(p -> parameterMap.put(p.getName(), p));
    }

    @SuppressWarnings("unchecked")
    public <ParameterType extends Number> ParameterType getParameter(final String parameterName) {
        Objects.requireNonNull(parameterName);
        return (ParameterType) parameterMap.get(parameterName).next();
    }
}
