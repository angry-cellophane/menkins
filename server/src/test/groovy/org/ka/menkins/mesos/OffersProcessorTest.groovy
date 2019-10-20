package org.ka.menkins.mesos

import org.apache.mesos.Protos
import org.apache.mesos.SchedulerDriver
import org.ka.menkins.storage.NodeRequestWithResources
import spock.lang.Specification

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicReference

class OffersProcessorTest extends Specification implements MesosHelpers {

    BlockingQueue<List<NodeRequestWithResources>> queue
    OffersProcessor offersProcessor
    AtomicReference<DriverState> stateRef

    void setup() {
        queue = new LinkedBlockingQueue<List<NodeRequestWithResources>>()
        stateRef = new AtomicReference<>(DriverState.newState())
        offersProcessor = new OffersProcessor(MESOS_CONFIG, queue, stateRef)
    }

    void 'suppress framework when no requests'() {
        given:
        def driver = Mock(SchedulerDriver)
        stateRef.set(stateRef.get().withDriver(driver))

        def offers = [offer(), offer()]

        when:
        offersProcessor.accept(offers)

        then:
        1 * driver.suppressOffers()
    }

    void 'sufficient resources, same role'() {
        given:
        def driver = Mock(SchedulerDriver)
        stateRef.set(stateRef.get().withDriver(driver))

        def requests = [request {
            docker {
                withResources(mesosResource(2.0, 1024, 'jenkins'))
            }
        }]
        def offers = [
                offer {
                    id = '1'
                    cpus = 5.0
                    mem = 2048
                    role = 'jenkins'
                }
        ]

        when:
        queue.add(requests)
        offersProcessor.accept(offers)

        then:
        1 * driver.launchTasks([Protos.OfferID.newBuilder().setValue('1').build()], { it.size() == 1 })
    }

    void 'sufficient resources, * role in offer'() {
        given:
        def driver = Mock(SchedulerDriver)
        stateRef.set(stateRef.get().withDriver(driver))

        def requests = [request {
            docker {
                withResources(mesosResource(2.0, 1024, 'jenkins'))
            }
        }]
        def offers = [
                offer {
                    id = '1'
                    cpus = 5.0
                    mem = 2048
                    role = '*'
                }
        ]

        when:
        queue.add(requests)
        offersProcessor.accept(offers)

        then:
        1 * driver.launchTasks([Protos.OfferID.newBuilder().setValue('1').build()], { it.size() == 1 })
    }

    void 'no sufficient resources in offer, no tasks'() {
        given:
        def driver = Mock(SchedulerDriver)
        stateRef.set(stateRef.get().withDriver(driver))

        def requests = [request {
            docker {
                withResources(mesosResource(20.0, 1024, 'jenkins'))
            }
        }]
        def offers = [
                offer {
                    id = '2'
                    cpus = 5.0
                    mem = 2048
                    role = '*'
                }
        ]

        when:
        queue.add(requests)
        offersProcessor.accept(offers)

        then:
        1 * driver.declineOffer(Protos.OfferID.newBuilder().setValue('2').build(), _ as Protos.Filters)
        0 * driver.launchTasks(_ as Collection, _ as Collection)
    }

    void 'two tasks for offer#1, one task for offer#2'() {
        given:
        def driver = Mock(SchedulerDriver)
        stateRef.set(stateRef.get().withDriver(driver))

        def requests = (1 .. 3).collect {
            request {
                docker {
                    withResources(mesosResource(1.0, 50, 'jenkins'))
                }
            }
        }
        def offers = [
                offer {
                    id = '1'
                    cpus = 2.0
                    mem = 100
                    role = '*'
                },
                offer {
                    id = '2'
                    cpus = 3.0
                    mem = 150
                    role = '*'
                }
        ]

        when:
        queue.add(requests)
        offersProcessor.accept(offers)

        then:
        1 * driver.launchTasks([Protos.OfferID.newBuilder().setValue('1').build()], { it.size() == 2 })
        1 * driver.launchTasks([Protos.OfferID.newBuilder().setValue('2').build()], { it.size() == 1 })
    }

    void 'return not matched offers to global queue'() {
        given:
        def driver = Mock(SchedulerDriver)
        stateRef.set(stateRef.get().withDriver(driver))

        def requests = (1 .. 3).collect {
            request {
                docker {
                    withResources(mesosResource(1.0, 50, 'jenkins'))
                }
            }
        }
        def offers = [
                offer {
                    id = '1'
                    cpus = 2.0
                    mem = 100
                    role = '*'
                }
        ]

        when:
        queue.add(requests)
        offersProcessor.accept(offers)

        then:
        queue.size() == 1
        queue[0].size() == 1
    }
}
