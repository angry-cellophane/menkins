package org.ka.menkins.aggregator;

@FunctionalInterface
public interface Runner {
    void run(Runnable runnable);

    static Runner newInfiniteLoop() {
        return runnable -> {
            for (;;) {
                runnable.run();
            }
        };
    }

    static Runner oneTimeRunner() {
        return Runnable::run;
    }
}
