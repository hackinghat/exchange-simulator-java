package com.hackinghat.util.mbean;

import com.hackinghat.util.component.AbstractComponent;

import java.time.LocalDate;

@MBeanType(description = "Test service for MBean stuff")
public class MBeanTestService extends AbstractComponent {
    private String initialValue;
    private String testString;
    private LocalDate testDate;
    private int testInt;
    private double testFloat;

    @MBeanAttribute(description = "Test read-only string")
    public String getTestString() { return testString; }
    @MBeanAttribute(description = "Test read-only datetime")
    public void setTestString(final String value) { testString = value; }

    @MBeanAttribute(description = "Test read-only integer")
    public int getTestInt() { return testInt; }
    @MBeanAttribute(description = "Test read-only integer")
    public void setTestInt(final int testInt) { this.testInt = testInt; }

    @MBeanAttribute(description = "Test read-only float")
    public double getTestFloat() { return testFloat; }
    @MBeanAttribute(description = "Test read-only float")
    public void setTestFloat(final double  testFloat) { this.testFloat = testFloat; }

    @MBeanOperation(description = "Test reset")
    public void reset() {
        testInt = 0;
        testString = initialValue;
        testFloat = 0.0f;
    }

    @MBeanOperation(description = "Test reset")
    public void reset(final int testInt, final String testString, final float testFloat) {
        this.testInt = testInt;
        this.testString = testString;
        this.testFloat = testFloat;
    }

    public MBeanTestService() {
        super("MBeanTestService");
    }

    public MBeanTestService(final String testString, final int testInt, final float testFloat) {
        this();
        this.testString = testString;
        this.initialValue = testString;
        this.testInt = testInt;
        this.testFloat = testFloat;
    }
}
