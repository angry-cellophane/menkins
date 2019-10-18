package org.ka.menkins;

@FunctionalInterface
public interface NanoTimer {
    long nanoTime();

    static NanoTimer systemClock() {
        return System::nanoTime;
    }
}
