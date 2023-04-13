package com.hackinghat.util.component;

import com.hackinghat.util.Pair;
import com.hackinghat.util.mbean.MBeanHolder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.NotCompliantMBeanException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.hackinghat.util.component.ComponentState.*;

public class AbstractComponent implements Component {
    protected static final Logger LOG = LogManager.getLogger(AbstractComponent.class);

    final static ComponentState[][] TRANSITIONS;
    static {
        TRANSITIONS = new ComponentState[][]{
                new ComponentState[] { STOPPED,     STARTING            },
                new ComponentState[] { STARTING,    RUNNING,    FAILED  },
                new ComponentState[] { RUNNING,     SUSPENDED,  STOPPING,   FAILED  },
                new ComponentState[] { FAILED,      STARTING,   STOPPED },
                new ComponentState[] { SUSPENDED,   RUNNING,    FAILED  },
                new ComponentState[] { STOPPING,    STOPPED             },
        };
        // A fairly basic test that ensures we can use the enum ordinal to get the allowed transitions
        for (int i = 0; i < TRANSITIONS.length; ++i) {
            if (TRANSITIONS[i][0].ordinal() != i)
                throw new IllegalArgumentException("State transition graph is invalid");
        }
    }


    private final String name;
    private final ArrayList<Component> requires;
    private final ArrayList<Component> references;
    private final MBeanHolder holder;
    private final Object sync = new Object();

    private ComponentState state;
    private boolean closed = false;

    public AbstractComponent(final String name) {
        Objects.requireNonNull(name);
        this.name = name;
        this.state = STOPPED;
        this.requires = new ArrayList<>();
        this.references = new ArrayList<>();
        try {
            holder = new MBeanHolder(this, name);
            holder.registerMBean();
        }
        catch (final InstanceAlreadyExistsException | NotCompliantMBeanException | MBeanRegistrationException ex) {
            LOG.error("Unable to register component", ex);
            throw new RuntimeException(ex);
        }
    }

    public boolean isClosed() {
        return closed;
    }

    private void _close() {
        synchronized (sync) {
            if (!closed) {
                for (final Component d : getRequirementsOf()) {
                    unrequire(d);
                }
                holder.unreigsterMBean();
                closed = true;
            }
        }
    }

    public void close() {
        shutdown();
    }

    /**
     * Return true if this component is a requirement of the argument
     * @param requirement the component
     * @return true if the component is required by this component to function
     */
    boolean isRequired(final Component requirement) {
        return Arrays.stream(getRequirementsOf()).anyMatch(r -> r == requirement);
    }

    /**
     * Return true if this component is referenced by argument
     * @param reference the component
     * @return true if the component is referenced by this component
     */
    boolean isReferenced(final Component reference) {
        return Arrays.stream(getReferences()).anyMatch(r -> r == reference);
    }

    private void _shutdown(final Component component) {
        // Only a stopped component can be shutdown
        if (getState() != STOPPED) {
            stop();
        }
        _close();
    }

    @Override
    public void shutdown() {
        _shutdown(this);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ComponentState getState() {
        return state;
    }

    @Override
    public Pair<Component, Component> hasCycle(final Function<Component, Component[]> componentProvider, final Set<Pair<Component, Component>> components) {
        final Component[] relevantComponents = componentProvider.apply(this);
        for (final Component c : relevantComponents) {
            final Pair<Component, Component> transition = Pair.instanceOf(this, c);
            if (components.contains(transition))
                return transition;
            components.add(transition);
            final Pair<Component, Component> clash = c.hasCycle(componentProvider, new HashSet<>(components));
            if (clash != null)
                return clash;
        }
        return null;
    }

    @Override
    public Component[] getReferences() {
        synchronized (sync) {
            return references.toArray(new Component[0]);
        }
    }

    public <C extends AbstractComponent> C require(final C component) {
        if (component != null) {
            synchronized (sync) {
                if (alreadyPresent(component, Arrays.stream(getRequirementsOf())))
                    throw new IllegalStateException("Requirement already present: " + component.getName());
                requires.add(component);
                final Pair<Component, Component> dependent = hasCycle(Component::getRequirementsOf, new HashSet<>());
                // Throws if this is already referenced by component
                component.addReference(this);
                // No good, there's a cycle, rollback the changes
                if (dependent != null) {
                    requires.remove(component);
                    component.removeReference(this);
                    throw new IllegalStateException("Making component '" + component.getName() + "' a requirement of '" + getName() +
                            "' will create a cycle (transition '" + dependent.getFirst().getName() + "'->'" + dependent.getSecond().getName() + "')");
                }
            }
        }
        return component;
    }

    @Override
    public Component[] getRequirementsOf() {
        synchronized (sync) {
            return requires.toArray(new Component[0]);
        }
    }

    private boolean alreadyPresent(@Nonnull final Component component, @Nonnull final Stream<Component> components) {
        return components.anyMatch(c -> c == component);
    }

    @Override
    public void addReference(@Nonnull final Component component) {
        synchronized (sync) {
            this.references.add(component);
        }
    }

    /**
     * Removing a reference, that leaves this component unreferenced, will cause it to get shutdown
     * @param component the component we want to remove a reference to
     * @return true if the component reference was removed
     */
    @Override
    public boolean removeReference(final Component component) {
        synchronized (sync) {
            // Remove the first reference (NB there could be multiple references of the same component)
            if (references.remove(component)) {
                // When this component has no more referrers it is defunct
                if (references.size() == 0)
                    shutdown();
                return true;
            }
            return false;
        }
    }

    @Override
    public void unrequire(@Nonnull final Component component) {
        synchronized (sync) {
            if (!isRequired(component))
                throw new IllegalArgumentException(component.getName() + " is not a requirement of " + getName());
            requires.remove(component);
            component.removeReference(this);
        }
    }

    public MBeanHolder getHolder() { return holder; }

    void transition(final ComponentState target) throws IllegalStateException {
        transition(target, false);
    }

    boolean transition(final ComponentState target, final boolean isSafe) throws IllegalStateException {
        synchronized (sync) {
            final ComponentState[] allowableTargets = TRANSITIONS[state.ordinal()];
            for (final ComponentState allowableTarget : allowableTargets) {
                if (allowableTarget == target) {
                    if (state == target) {
                        LOG.info(getName() + " transition from " + state + " to " + allowableTarget + " (no-op)");
                    } else {
                        LOG.info(getName() + " transition from " + state + " to " + allowableTarget);
                        state = allowableTarget;
                        invokeTransition();
                    }
                    return true;
                }
            }
            if (isSafe)
                return false;
            throw new IllegalStateException("Can not transition from " + state + " to " + target);
        }
    }

    void invokeTransition() {
        switch (state) {
            case STARTING:
                starting();
                break;
            case STOPPING:
                stopping();
                break;
        }
    }

    /**
     * Apply a single function to all dependencies
     * @param successState the state to move to on success
     * @param failureState the state to move to on failure
     * @param consumer the function to call on each dependency
     */
    void applyToAllComponents(final ComponentState successState, final ComponentState failureState, Consumer<Component> consumer, final Component[] components) {
        try {
            for (final Component dependency : components) {
                consumer.accept(dependency);
            }
            transition(successState);
        }
        catch (Throwable t) {
            LOG.error("Unable to invoke transition to " + successState + " for " + getName() + ", reason: ", t);
            transition(failureState);
        }

    }

    void starting() {
        applyToAllComponents(RUNNING, FAILED, Component::start, getRequirementsOf());
    }

    void stopping() {
        applyToAllComponents(STOPPED, FAILED, Component::stop, getRequirementsOf());
    }

    @Override
    public void start() {
        if (getState() != RUNNING)
            transition(STARTING);
    }

    @Override
    public void stop() {
        if (getState() != STOPPED)
            transition(STOPPING);
    }

    @Override
    public void suspend() {

    }
}
