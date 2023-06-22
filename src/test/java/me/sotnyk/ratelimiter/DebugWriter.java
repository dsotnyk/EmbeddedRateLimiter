package me.sotnyk.ratelimiter;

public class DebugWriter {
    boolean on = true;

    public DebugWriter(boolean on) {
        this.on = on;
    }

    public DebugWriter twit(String s) {
        System.out.println(s);
        return this;
    }

}
