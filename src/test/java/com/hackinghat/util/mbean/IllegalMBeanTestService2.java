package com.hackinghat.util.mbean;

public class IllegalMBeanTestService2 {

    @MBeanAttribute(description = "Test read-only string")
    public void setTestString() { }

    public IllegalMBeanTestService2() {
    }

}
