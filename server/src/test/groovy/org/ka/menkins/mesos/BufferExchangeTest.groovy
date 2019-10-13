package org.ka.menkins.mesos

import org.apache.mesos.SchedulerDriver
import org.ka.menkins.queue.NodeRequestWithResources
import spock.lang.Specification

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class BufferExchangeTest extends Specification implements MesosHelpers {

    static final Schedulers.TimingConfiguration timing = Schedulers.TimingConfiguration.builder()
            .bufferFlushIntervalMs(200)
            .globalQueueTimeoutMs(100)
            .build();

    BlockingQueue<NodeRequestWithResources> globalQueue
    BlockingQueue<List<NodeRequestWithResources>> localQueue
    AtomicBoolean suppress
    Schedulers.State state
    ExecutorService pool

    void setup() {
        globalQueue = new LinkedBlockingQueue<>()
        localQueue = new ArrayBlockingQueue<>(1)
        suppress = new AtomicBoolean(false)
        state = new Schedulers.State(localQueue, suppress, new AtomicReference<>(null))
        pool = Executors.newSingleThreadExecutor()
    }

    void cleanup() {
        pool.shutdownNow()
    }

    void 'one item from global queue into local'() {
        given:
        def handler = Schedulers.newBufferHandler(state, globalQueue, timing)

        when:
        globalQueue.add(request {})
        pool.execute(handler)
        def requests = localQueue.poll(1, TimeUnit.SECONDS)

        then:
        requests != null
        requests.size() == 1
    }

    void '6 items from global queue into local'() {
        given:
        def handler = Schedulers.newBufferHandler(state, globalQueue, timing)

        when:
        6.times { globalQueue.add(request {}) }
        pool.execute(handler)
        def requests1 = localQueue.poll(1, TimeUnit.SECONDS)
        def requests2 = localQueue.poll(1, TimeUnit.SECONDS)

        then:
        requests1 != null
        requests2 != null
        requests1.size() == 5
        requests2.size() == 1
    }

    void 'revive offers when framework is in suppress mode and there is a request'() {
        given:
        def handler = Schedulers.newBufferHandler(state, globalQueue, timing)
        def driver = Mock(SchedulerDriver)
        state.driver.set(driver)
        state.suppress.set(true)

        when:
        6.times { globalQueue.add(request {}) }
        pool.execute(handler)
        // handler adds requests to the local queue first and then revives offers
        // it may happen that this test will check suppress status between these two operations
        // second polls guarantees the check happens after revive
        2.times { localQueue.poll(1, TimeUnit.SECONDS) }

        then:
        state.suppress.get() == false
        1 * driver.reviveOffers()
    }
}
