package org.ka.menkins.aggregator

import org.ka.menkins.NanoTimer
import org.ka.menkins.TimerHelper
import org.ka.menkins.mesos.MesosHelpers
import org.ka.menkins.storage.NodeRequestWithResources
import org.ka.menkins.storage.Storage.StorageConfiguration
import spock.lang.Specification

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class RequestsAggregatorTest extends Specification implements MesosHelpers, TimerHelper {

    BlockingQueue<NodeRequestWithResources> global = new LinkedBlockingQueue<>();
    BlockingQueue<List<NodeRequestWithResources>> aggregated = new LinkedBlockingQueue<>();

    void 'send to aggregated when buffer is full'() {
        given:
        [request {}, request {}, request {}].each { global.add(it) }

        def config = StorageConfiguration.builder()
                .bufferSize(2)
                .build()
        def timer = Mock(NanoTimer) {
            nanoTime() >>> [1, 2, 3]
        }
        def aggregator = RequestsAggregator.newInstance(config, global, aggregated, timer, Runner.oneTimeRunner())
        when:
        3.times {
            aggregator.run()
        }

        then:
        aggregated.size() == 1
        aggregated[0].size() == 2
    }

    void 'send to aggregated when exceeded flush interval'() {
        given:
        [request {}, request {}].each { global.add(it) }

        def config = StorageConfiguration.builder()
                .bufferSize(2)
                .build()
        def timer = Mock(NanoTimer) {
            nanoTime() >>> [0, 0, seconds(10), seconds(20)]
        }
        def aggregator = RequestsAggregator.newInstance(config, global, aggregated, timer, Runner.oneTimeRunner())
        when:
        2.times {
            aggregator.run()
        }

        then:
        aggregated.size() == 1
        aggregated[0].size() == 1
    }

    void 'no exception if global.poll() throws exception'() {
        given:
        def all = Mock(BlockingQueue) {
            poll(TimeUnit.SECONDS.toNanos(1), TimeUnit.NANOSECONDS) >> {
                throw new InterruptedException()
            }
        }

        def config = StorageConfiguration.builder()
                .bufferSize(2)
                .build()
        def timer = Mock(NanoTimer) {
            nanoTime() >> 0
        }
        def aggregator = RequestsAggregator.newInstance(config, all, aggregated, timer, Runner.oneTimeRunner())
        when:
        aggregator.run()

        then:
        aggregated.size() == 0
    }

    void 'returns to global queue after long time'() {
        given:
        def all = Mock(BlockingQueue) {
            poll(seconds(1), TimeUnit.NANOSECONDS) >>> [request {}, null]
        }

        def aggregatedQueue = Mock(BlockingQueue) {
            add(_) >> false
        }

        def config = StorageConfiguration.builder()
                .bufferSize(2)
                .build()
        def timer = Mock(NanoTimer) {
            nanoTime() >>> [0, 0, seconds(60), seconds(120)]
        }
        def aggregator = RequestsAggregator.newInstance(config, all, aggregatedQueue, timer, Runner.oneTimeRunner())
        when:
        2.times { aggregator.run() }

        then:
        1 * all.add(_)
    }
}
