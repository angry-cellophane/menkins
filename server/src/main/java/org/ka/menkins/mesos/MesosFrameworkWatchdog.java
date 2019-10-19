package org.ka.menkins.mesos;

import lombok.extern.slf4j.Slf4j;
import org.ka.menkins.NanoTimer;
import org.ka.menkins.aggregator.Runner;
import org.ka.menkins.storage.NodeRequestWithResources;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class MesosFrameworkWatchdog {
    public static Runnable initialize(AtomicReference<DriverState> stateRef,
                                      BlockingQueue<List<NodeRequestWithResources>> aggregated,
                                      Runnable stopAppCallback) {
        var pool = Executors.newSingleThreadExecutor(runnable -> {
            var t = new Thread(runnable);
            t.setDaemon(true);
            t.setName("menkins-watchdog-thread");
            return t;
        });

        var runner = Runner.newInfiniteLoopWithBreaks(TimeUnit.SECONDS.toNanos(1));
        var watchdog = newInstance(runner, NanoTimer.systemClock(), stateRef, aggregated, stopAppCallback);
        return () -> pool.execute(watchdog);
    }

    static Runnable newInstance(Runner runner,
                                NanoTimer timer,
                                AtomicReference<DriverState> stateRef,
                                BlockingQueue<List<NodeRequestWithResources>> aggregated,
                                Runnable stopAppCallback) {
        var DISCONNECTED_TIMEOUT = TimeUnit.SECONDS.toNanos(30);
        long[] lastActive = new long[] {timer.nanoTime()};
        return () -> {
            runner.run(() -> {
                try {
                    var state = stateRef.get();
                    var now = timer.nanoTime();

                    if (state.isRunning()) {
                        lastActive[0] = now;
                        if (state.isSuppressed() && !aggregated.isEmpty()) {
                            DriverState.update(stateRef, old -> old.withSuppressed(false));
                            var driver = state.getDriver();
                            if (driver != null) {
                                driver.reviveOffers();
                            } else {
                                log.error("driver is null, this is probably a bug");
                            }
                        }
                    } else {
                        if (now - lastActive[0] > DISCONNECTED_TIMEOUT) {
                            stopAppCallback.run();
                        }
                    }
                } catch (Exception e) {
                    log.error("error in watchdog main loop ", e);
                }
            });
        };
    }
}
