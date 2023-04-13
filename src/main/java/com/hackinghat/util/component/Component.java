package com.hackinghat.util.component;

import com.hackinghat.util.Nameable;
import com.hackinghat.util.Pair;
import com.hackinghat.util.mbean.MBeanAttribute;
import com.hackinghat.util.mbean.MBeanOperation;

import java.util.Set;
import java.util.function.Function;

public interface Component extends Nameable, AutoCloseable {
    @MBeanAttribute(description = "Component:State")
    ComponentState getState();

    @MBeanOperation(description = "Component:Start")
    void start();

    @MBeanOperation(description = "Component:Stop")
    void stop();
    @MBeanOperation(description = "Component:Start")
    void suspend();

    /**
     * Tells the component's dependencies to shutdown and then shuts itself down.  A component will only actually
     * honour the shutdown if it has no other registrations
     */
    @MBeanOperation(description = "Component:Shutdown")
    void shutdown();

    /**
     * Require the dependents for the correct running of this component
     * @param component a collection of dependencies for this component
     */
    <C extends AbstractComponent> C require(final C component);
    void unrequire(final Component component);
    Component[] getRequirementsOf();
    Pair<Component, Component> hasCycle(final Function<Component, Component[]> componentProvider, final Set<Pair<Component,Component>> components);


    Component[] getReferences();
    void addReference(final Component component);
    boolean removeReference(final Component component);

    default void close() { }
    boolean isClosed();
}
