package org.ka.menkins.mesos


import org.apache.mesos.Protos
import org.apache.mesos.Protos.Offer
import org.apache.mesos.Protos.Resource
import org.ka.menkins.app.init.AppConfig
import org.ka.menkins.storage.MesosResources
import org.ka.menkins.storage.NodeRequest
import org.ka.menkins.storage.NodeRequestWithResources
import org.ka.menkins.storage.ResourcesByLabelsLookup

trait MesosHelpers {
    static final AppConfig.Mesos MESOS_CONFIG = new AppConfig.Mesos(
            'some-path',
            '*',
            'nobody',
            'master:5050',
            'menkins-1',
            'http://webui',
            600,
            true
    )

    static class OfferSpec {
        String id = UUID.randomUUID().toString()
        double cpus = 0.0
        double mem = 0.0
        String role = '*'
        String frameworkId = UUID.randomUUID().toString()
        String hostname = 'hostname'
        String slaveId = UUID.randomUUID().toString()
    }

    Offer offer() {
        return offer {}
    }

    OfferRequestMatcher matcher(@DelegatesTo(value = OfferSpec, strategy = Closure.DELEGATE_FIRST) Closure config) {
        OfferRequestMatcher.from(offer(config))
    }

    Offer offer(@DelegatesTo(value = OfferSpec, strategy = Closure.DELEGATE_FIRST) Closure config) {
        def spec = new OfferSpec()
        config.delegate = spec
        config()

        Offer.newBuilder()
                .setId(Protos.OfferID.newBuilder().setValue(spec.id).build())
                .setFrameworkId(Protos.FrameworkID.newBuilder().setValue(spec.frameworkId).build())
                .setHostname(spec.hostname)
                .addResources(resource('cpus', spec.cpus, spec.role))
                .addResources(resource('mem', spec.mem, spec.role))
                .setSlaveId(Protos.SlaveID.newBuilder().setValue(spec.slaveId).build())
                .build()
    }

    static class NodeRequestWithResourcesSpec {

        NodeRequest request = new NodeRequest(
                UUID.randomUUID().toString(),
                "node",
                "labels",
                "http://jenkins",
                "http://jenkins/jnlp",
                "jnlp-args",
                "http://jenkins/jnlp.jar",
                "secret"
        )
        DockerConfig docker = ResourcesByLabelsLookup.DEFAULT_IMAGE

        void node(@DelegatesTo(value = NodeRequest, strategy = Closure.DELEGATE_FIRST) Closure<NodeRequest> config) {
            config.delegate = request
            request = config()
        }

        void docker(@DelegatesTo(value = DockerConfig, strategy = Closure.DELEGATE_FIRST) Closure<DockerConfig> config) {
            config.delegate = this.docker
            this.docker = config()
        }
    }

    NodeRequestWithResources request(@DelegatesTo(value = NodeRequestWithResourcesSpec, strategy = Closure.DELEGATE_FIRST) Closure config) {
        def spec = new NodeRequestWithResourcesSpec()
        config.delegate = spec
        config()

        new NodeRequestWithResources(spec.request, spec.docker)
    }

    MesosResources mesosResource(double cpus, double mem, String role = '*') {
        new MesosResources(role, cpus, mem)
    }

    Resource resource(String name, double value, String role) {
        Protos.Resource.newBuilder()
                .setName(name)
                .setType(Protos.Value.Type.SCALAR)
                .setRole(role)
                .setScalar(
                        Protos.Value.Scalar.newBuilder()
                                .setValue(value)
                                .build()
                )
                .build()
    }
}
