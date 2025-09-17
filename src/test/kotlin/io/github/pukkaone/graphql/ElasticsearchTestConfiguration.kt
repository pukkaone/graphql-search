package io.github.pukkaone.graphql

import org.springframework.boot.test.context.TestConfiguration
import org.testcontainers.containers.GenericContainer

private const val PORT = 9200

/**
 * Starts an Elasticsearch server in a local Docker container.
 */
@TestConfiguration
class ElasticsearchTestConfiguration {

    companion object {
        private val container = GenericContainer("docker.elastic.co/elasticsearch/elasticsearch:7.10.2")
            .withEnv("discovery.type", "single-node")
            .withExposedPorts(PORT)

        init {
            container.start()
            System.setProperty(
                "spring.elasticsearch.uris",
                "http://" + container.host + ":" + container.getMappedPort(PORT))
        }
    }
}
