package io.github.pukkaone.graphql.configuration

import com.fasterxml.jackson.core.JsonFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.IOException
import org.elasticsearch.client.RestClientBuilder
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.common.xcontent.json.JsonXContent
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.elasticsearch.RestClientBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private val logger = KotlinLogging.logger {}

/**
 * Configures use of [String.intern] when Elasticsearch client parses JSON.
 *
 * Disable this feature to improve Elasticsearch client performance. See
 *   - [Elasticsearch issue](https://github.com/elastic/elasticsearch/issues/39890)
 *   - [Jackson issue](https://github.com/FasterXML/jackson-core/issues/378)
 *
 * @param enabled
 *   whether to enable the feature
 */
private fun configureJacksonInternFieldNames(enabled: Boolean) {
    try {
        // Ensure the static field is initialized.
        JsonXContent.contentBuilder()
    } catch (e: IOException) {
        logger.warn(e) { "Cannot configure JsonFactory.Feature.INTERN_FIELD_NAMES to $enabled" }
    }

    try {
        val jsonFactoryField = JsonXContent::class.java.getDeclaredField("jsonFactory")
        jsonFactoryField.isAccessible = true
        val jsonFactory = jsonFactoryField.get(null) as JsonFactory

        jsonFactory.configure(JsonFactory.Feature.INTERN_FIELD_NAMES, enabled)
    } catch (e: ReflectiveOperationException) {
        logger.warn(e) { "Cannot configure JsonFactory.Feature.INTERN_FIELD_NAMES to $enabled" }
    }
}

/**
 * Configures Elasticsearch client.
 */
@Configuration(proxyBeanMethods = false)
class ElasticsearchConfiguration {

    @Bean
    fun restClientBuilderCustomizer(
        @Value("\${search.jackson.intern-field-names:false}") jacksonStringIntern: Boolean,
        @Value("\${spring.elasticsearch.maximum-connections:20}") maximumConnections: Int,
    ): RestClientBuilderCustomizer {
        configureJacksonInternFieldNames(jacksonStringIntern)
        return RestClientBuilderCustomizer { restClientBuilder ->
            restClientBuilder.setHttpClientConfigCallback { httpClientBuilder ->
                httpClientBuilder
                    .setMaxConnPerRoute(maximumConnections)
                    .setMaxConnTotal(maximumConnections)
            }
        }
    }

    @Bean
    fun restHighLevelClient(restClientBuilder: RestClientBuilder): RestHighLevelClient {
        return RestHighLevelClient(restClientBuilder)
    }
}
