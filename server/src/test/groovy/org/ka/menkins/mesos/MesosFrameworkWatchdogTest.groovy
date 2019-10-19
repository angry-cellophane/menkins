package org.ka.menkins.mesos

import org.apache.mesos.SchedulerDriver
import org.ka.menkins.NanoTimer
import org.ka.menkins.TimerHelper
import org.ka.menkins.aggregator.Runner
import org.ka.menkins.storage.NodeRequestWithResources
import spock.lang.Specification

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicReference

class MesosFrameworkWatchdogTest extends Specification implements TimerHelper {

    BlockingQueue<List<NodeRequestWithResources>> aggregated = new LinkedBlockingQueue<>()

    void 'do nothing if driver is running and not suppressed'() {
        given:
        def timer = Mock(NanoTimer)
        def stateRef = new AtomicReference<>(DriverState.newState().withRunning(true).withSuppressed(false))
        def watchdog = MesosFrameworkWatchdog.newInstance(Runner.oneTimeRunner(), timer, stateRef, aggregated, {})

        when:
        watchdog.run()

        then:
        stateRef.get().running == true
        stateRef.get().suppressed == false
    }

    void 'invoke stop app callback when driver is down for 30 sec'() {
        given:
        def timer = Mock(NanoTimer) {
            nanoTime() >>> [0, seconds(60)]
        }
        def callback = Mock(Runnable)
        def stateRef = new AtomicReference<>(DriverState.newState().withRunning(false).withSuppressed(false))
        def watchdog = MesosFrameworkWatchdog.newInstance(Runner.oneTimeRunner(), timer, stateRef, aggregated, callback)

        when:
        watchdog.run()

        then:
        1 * callback.run()
    }

    void 'revive offers if queue is not empty and driver is in suppressed mode'() {
        given:
        def timer = Mock(NanoTimer)
        def driver = Mock(SchedulerDriver)
        def stateRef = new AtomicReference<>(DriverState.newState()
                .withRunning(true)
                .withSuppressed(true)
                .withDriver(driver)
        )
        def watchdog = MesosFrameworkWatchdog.newInstance(Runner.oneTimeRunner(), timer, stateRef, aggregated, {})

        when:
        aggregated.add([])
        watchdog.run()

        then:
        stateRef.get().suppressed == false
        1 * driver.reviveOffers()
    }
}
