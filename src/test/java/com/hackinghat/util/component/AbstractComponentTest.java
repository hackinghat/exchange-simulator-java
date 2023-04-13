package com.hackinghat.util.component;

import com.hackinghat.util.mbean.MBeanType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Set;
import java.util.stream.Collectors;

import static com.hackinghat.util.component.ComponentState.RUNNING;
import static com.hackinghat.util.component.ComponentState.STOPPED;

public class AbstractComponentTest {
    protected static final Logger LOG = LogManager.getLogger(AbstractComponentTest.class);

    @MBeanType(description = "My abstract component")
    private static class MyAbstractComponent extends AbstractComponent {

        public MyAbstractComponent(final String name) {
            super(name);
        }

        public MyAbstractComponent(final String name, MyAbstractComponent dependents) {
            super(name);
            require(dependents);
        }
    }

    public static int checkNumberOfMBeans(final int expected, final String callSite) {
        try {
            Set<ObjectName> names = ManagementFactory.getPlatformMBeanServer().queryNames(new ObjectName("com.hacking*:*"), null);
            if (expected != names.size()) {
                final String message = "Was expecting " + expected + " registered mbeans in " + callSite + ", but found " + names.size() + ". ";
                LOG.error(message + String.join(",", names.stream().map(String::valueOf).collect(Collectors.joining(","))) + ")");
                throw new IllegalStateException(message  + " (see error log for names");
            }
            return names.size();
        }
        catch (final MalformedObjectNameException malex) {
            LOG.error(malex);
        }
        return 0;
    }

    @Before
    public void setup() {
        checkNumberOfMBeans(0, "AbstractComponentTest.setup");
    }

    @After
    public void teardown() {
        checkNumberOfMBeans(0, "AbstractComponentTest.teardown");
    }

    @Test
    public void testNoTransition() {
        try (final AbstractComponent ac = new MyAbstractComponent("BOGOF1")) {
            // Assert that you can transition into the state you're already in
            Assert.assertTrue(ac.transition(ac.getState(), true));
        }
    }

    @Test
    public void testOneDependent() {
        try (final MyAbstractComponent lotso = new MyAbstractComponent("LOTSO");
             final MyAbstractComponent bogof = new MyAbstractComponent("BOGOF3", lotso)) {
            Assert.assertEquals(1, bogof.getRequirementsOf().length);
            Assert.assertEquals(0, lotso.getRequirementsOf().length);
            Assert.assertEquals(1, lotso.getReferences().length);
            // Starting a component starts its dependents
            bogof.start();
            Assert.assertEquals(RUNNING, bogof.getState());
            Assert.assertEquals(RUNNING, lotso.getState());
            bogof.stop();
            Assert.assertEquals(STOPPED, bogof.getState());
            Assert.assertEquals(STOPPED, lotso.getState());
            // Starting a component does not start the things that depend on it
            lotso.start();
            Assert.assertEquals(STOPPED, bogof.getState());
            Assert.assertEquals(RUNNING, lotso.getState());
            lotso.stop();
            Assert.assertEquals(STOPPED, bogof.getState());
            Assert.assertEquals(STOPPED, lotso.getState());
        }
    }

    @Test(expected = RuntimeException.class)
    public void testDuplicateRegistration() {

        try (final AbstractComponent ac1 = new MyAbstractComponent("BOGOF1");
             final AbstractComponent ac2 = new MyAbstractComponent("BOGOF1")) {
            throw new IllegalStateException("Shouldn't get here");
        }
    }

    @Test
    public void testClose() {
        try (final AbstractComponent ac1 = new MyAbstractComponent("BOGOF1")) {
            Assert.assertNotNull(ac1);
        };
        try (final AbstractComponent ac2 = new MyAbstractComponent("BOGOF1")) {
            Assert.assertNotNull(ac2);
        };
    }

    @Test
    public void testRequiresOf() {
        try (final AbstractComponent client1 = new MyAbstractComponent("CLIENT1");
             final AbstractComponent client2 = new MyAbstractComponent("CLIENT2")) {
            try (final AbstractComponent ac2 = new MyAbstractComponent("BOGOF2")) {
                client1.require(ac2);
                client2.require(ac2);
                Assert.assertTrue(client1.isRequired(ac2));
                Assert.assertTrue(client2.isRequired(ac2));
                Assert.assertFalse(ac2.isRequired(client1));
                Assert.assertFalse(ac2.isRequired(client2));
                Assert.assertFalse(client1.isRequired(client2));
                Assert.assertFalse(client1.isRequired(client1));
            }
        }
    }

    @Test
    public void testReferences() {
        try (final AbstractComponent client1 = new MyAbstractComponent("CLIENT1");
             final AbstractComponent client2 = new MyAbstractComponent("CLIENT2")) {
            try (final AbstractComponent ac2 = new MyAbstractComponent("BOGOF2")) {
                client1.require(ac2);
                client2.require(ac2);
                Assert.assertTrue(ac2.isReferenced(client1));
                Assert.assertTrue(ac2.isReferenced(client2));
                Assert.assertFalse(client1.isReferenced(ac2));
                Assert.assertFalse(client2.isReferenced(ac2));
                Assert.assertFalse(client1.isReferenced(client2));
                Assert.assertFalse(client1.isReferenced(client1));
            }
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testDuplicateRequire() {
        try (final AbstractComponent client1 = new MyAbstractComponent("CLIENT1")) {
            try (final AbstractComponent ac2 = new MyAbstractComponent("BOGOF2")) {
                client1.require(ac2);
                client1.require(ac2);
            }
        }
    }

    @Test
    public void testDuplicateAddReference() {
        try (final AbstractComponent client1 = new MyAbstractComponent("CLIENT1")) {
            try (final AbstractComponent ac2 = new MyAbstractComponent("BOGOF2")) {
                ac2.addReference(client1);
                Assert.assertEquals(1, ac2.getReferences().length);
                Assert.assertEquals(0, client1.getReferences().length);
                ac2.addReference(client1);
            }
        }
    }
    @Test
    public void testShutdown() {
        try (final AbstractComponent client1 = new MyAbstractComponent("CLIENT1");
             final AbstractComponent client2 = new MyAbstractComponent("CLIENT2")) {
            try (final AbstractComponent ac2 = new MyAbstractComponent("BOGOF2")) {
                client1.require(ac2);
                client2.require(ac2);
                client1.start();
                Assert.assertEquals(RUNNING, client1.getState());
                Assert.assertEquals(RUNNING, ac2.getState());
                Assert.assertEquals(STOPPED, client2.getState());
                client2.start();
                Assert.assertEquals(RUNNING, client2.getState());
                client1.shutdown();
                Assert.assertTrue(client1.isClosed());
                Assert.assertFalse(client2.isClosed());
                Assert.assertFalse(ac2.isClosed());
                client2.shutdown();
                Assert.assertTrue(client1.isClosed());
                Assert.assertTrue(client2.isClosed());
                Assert.assertTrue(ac2.isClosed());
            }
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testRequirementCycle1() {
        try (final AbstractComponent a = new MyAbstractComponent("A");
             final AbstractComponent b = new MyAbstractComponent("B")) {
            a.require(b);
            b.require(a);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testRequirementCycle2() {
        try (final AbstractComponent a = new MyAbstractComponent("A");
             final AbstractComponent b = new MyAbstractComponent("B");
             final AbstractComponent c = new MyAbstractComponent("C")) {
            a.require(b);
            b.require(c);
            c.require(a);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testRequirementCycle3() {
        // There is a cycle here
        try (final AbstractComponent a = new MyAbstractComponent("A");
             final AbstractComponent b = new MyAbstractComponent("B");
             final AbstractComponent c = new MyAbstractComponent("C");
             final AbstractComponent z = new MyAbstractComponent("Z");) {
            a.require(b);
            b.require(c);
            c.require(z);
            a.require(z);
            a.unrequire(z);
            z.require(a);
        }
    }

    @Test
    public void testRequirementNoCycle1() {
        // There is no cycle here
        try (final AbstractComponent a = new MyAbstractComponent("A");
             final AbstractComponent b = new MyAbstractComponent("B");
             final AbstractComponent c = new MyAbstractComponent("C")) {
            a.require(b);
            b.require(c);
            a.require(c);
        }
    }

    @Test
    public void testRequirementNoCycle3() {
        try (final AbstractComponent sh = new MyAbstractComponent("sh");
             final AbstractComponent s = new MyAbstractComponent("s");
             final AbstractComponent mm = new MyAbstractComponent("mm");
             final AbstractComponent d = new MyAbstractComponent("d")){
            mm.require(d);
            sh.require(mm);
            s.require(mm);
            sh.require(s);
        }
    }

    @Test
    public void testNoDependents() {
        try (final AbstractComponent ac = new MyAbstractComponent("BOGOF3")) {
            Assert.assertEquals(STOPPED, ac.getState());
            ac.start();
            Assert.assertEquals(RUNNING, ac.getState());
            ac.stop();
            Assert.assertEquals(STOPPED, ac.getState());
        }
    }

}