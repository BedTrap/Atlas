package me.bedtrapteam.addon.utils;

public class Runtime extends RuntimeException {

    public Runtime(String msg) {
        super(msg);
        this.setStackTrace(new StackTraceElement[0]);
    }

    @Override
    public String toString() {
        return "";
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
