package org.ka.menkins.mesos

import org.apache.mesos.Protos
import org.apache.mesos.SchedulerDriver
import org.ka.menkins.app.AppConfig
import org.ka.menkins.queue.NodeRequestWithResources
import spock.lang.Specification

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class OffersProcessorTest extends Specification {

    void 'suppress framework when no requests'() {
        given:
        def queue = new LinkedBlockingQueue<List<NodeRequestWithResources>>()
        def suppress = new AtomicBoolean(false)
        def driver = Mock(SchedulerDriver)
        def driveRef = new AtomicReference<>(driver)
        def state = new Schedulers.State(queue, suppress, driverRef)
        def offersProcessor = new OffersProcessor(MesosHelpers.MESOS_CONFIG, state)

        def offer = Protos.Offer.newBuilder()
                .addResources()

    }
}
