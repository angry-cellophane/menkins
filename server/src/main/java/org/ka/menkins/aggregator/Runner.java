package org.ka.menkins.aggregator;

import java.util.concurrent.TimeUnit;

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

    static Runner newInfiniteLoopWithBreaks(long nano) {
        return runnable -> {
            for (;;) {
                try {
                    runnable.run();
                    TimeUnit.NANOSECONDS.sleep(nano);
                } catch (InterruptedException e) {}
            }
        };
    }

    static Runner oneTimeRunner() {
        return Runnable::run;
    }
}
