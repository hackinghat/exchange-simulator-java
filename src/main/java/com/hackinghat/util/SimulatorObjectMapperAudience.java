package com.hackinghat.util;

public enum SimulatorObjectMapperAudience {
    PUBLIC,
    PRIVATE;

    public static boolean isPublic(final SimulatorObjectMapperAudience audience) {
        return audience == PUBLIC;
    }

    public static boolean isPrivate(final SimulatorObjectMapperAudience audience) {
        return audience == PRIVATE;
    }
}
