package com.hackinghat.util.mbean;

public class IllegalMBeanTestService1 {
    private String testString;

    public IllegalMBeanTestService1() {
    }

    public IllegalMBeanTestService1(final String testString) {
        this.testString = testString;
    }

    @MBeanAttribute(description = "Test read-only string")
    public String getTestString(final String arg1) {
        return testString;
    }
}
