package io.github.pukkaone.graphql.apiversion

import io.github.pukkaone.graphql.search.GetDataFetcher
import io.github.pukkaone.graphql.search.GetDocumentsUseCase
import io.github.pukkaone.graphql.search.PutDataFetcher
import io.github.pukkaone.graphql.search.PutDocumentsUseCase
import io.github.pukkaone.graphql.search.SearchDataFetcher
import io.github.pukkaone.graphql.search.SearchDocumentsUseCase
import io.github.pukkaone.graphql.search.SearchProperties
import io.github.pukkaone.graphql.search.SearchSchemaGenerator
import io.github.pukkaone.graphql.search.schema.ProtoSchema
import org.springframework.boot.autoconfigure.graphql.GraphQlSourceBuilderCustomizer
import org.springframework.graphql.ExecutionGraphQlRequest
import org.springframework.graphql.ExecutionGraphQlResponse
import org.springframework.graphql.ExecutionGraphQlService
import org.springframework.graphql.execution.ConnectionTypeDefinitionConfigurer
import org.springframework.graphql.execution.DataFetcherExceptionResolver
import org.springframework.graphql.execution.DefaultExecutionGraphQlService
import org.springframework.graphql.execution.GraphQlSource
import org.springframework.graphql.execution.RuntimeWiringConfigurer
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder
import reactor.core.publisher.Mono

/**
 * [ExecutionGraphQlService] implementation that routes a query execution to one of various
 * target ExecutionGraphQlServices based on the API version in the request.
 */
@Component
class RoutingExecutionGraphQlService(
    searchProperties: SearchProperties,
    private val exceptionResolvers: List<DataFetcherExceptionResolver>,
    private val getDocumentsUseCase: GetDocumentsUseCase,
    private val putDocumentsUseCase: PutDocumentsUseCase,
    private val searchDocumentsUseCase: SearchDocumentsUseCase,
    private val sourceCustomizers: List<GraphQlSourceBuilderCustomizer>,
    private val wiringConfigurers: List<RuntimeWiringConfigurer>,
) : ExecutionGraphQlService {

    private val apiVersionToProtoSchemaMap: MutableMap<String, ProtoSchema> = mutableMapOf()

    private val apiVersionToExecutionGraphQlServiceMap: Map<String, ExecutionGraphQlService> =
        searchProperties.apiVersions.associateWith { createExecutionGraphQlService(it) }

    private fun addDataFetchers(
        builder: GraphQlSource.SchemaResourceBuilder,
        protoSchema: ProtoSchema,
    ) {
        builder.configureRuntimeWiring { runtimeWiringBuilder ->
            for (documentType in protoSchema.documentTypes) {
                runtimeWiringBuilder.type("Mutation") { typeBuilder ->
                    typeBuilder.dataFetcher("put$documentType", PutDataFetcher(documentType, putDocumentsUseCase))
                }

                runtimeWiringBuilder.type("Query") { typeBuilder ->
                    val fieldPrefix = documentType.replaceFirstChar(Char::lowercase)
                    typeBuilder.dataFetcher(fieldPrefix, GetDataFetcher(getDocumentsUseCase))
                    typeBuilder.dataFetcher(
                        "${fieldPrefix}Search",
                        SearchDataFetcher(documentType, searchDocumentsUseCase)
                    )
                }
            }
        }
    }

    private fun createExecutionGraphQlService(apiVersion: String): ExecutionGraphQlService {
        val protoSchema = ProtoSchema(apiVersion)
        apiVersionToProtoSchemaMap[apiVersion] = protoSchema

        val searchSchema = SearchSchemaGenerator.generate(protoSchema.typeDefinitionRegistry)

        val builder = GraphQlSource.schemaResourceBuilder()
            .configureTypeDefinitions { typeDefinitions -> typeDefinitions.merge(protoSchema.typeDefinitionRegistry) }
            .configureTypeDefinitions(ConnectionTypeDefinitionConfigurer())
            .schemaResources(searchSchema)
            .exceptionResolvers(exceptionResolvers)
        wiringConfigurers.forEach { builder.configureRuntimeWiring(it) }
        addDataFetchers(builder, protoSchema)
        sourceCustomizers.forEach { it.customize(builder) }

        return DefaultExecutionGraphQlService(builder.build())
    }

    private fun getApiVersion(): String {
        return requireNotNull(RequestContextHolder.getRequestAttributes())
            .getAttribute(ApiVersionGraphQlHttpHandler.API_VERSION, RequestAttributes.SCOPE_REQUEST) as String
    }

    override fun execute(request: ExecutionGraphQlRequest): Mono<ExecutionGraphQlResponse> {
        val apiVersion = getApiVersion()
        val executionGraphQlService = apiVersionToExecutionGraphQlServiceMap[apiVersion]
            ?: error("Unsupported API version [$apiVersion]")
        request.configureExecutionInput { input, builder ->
            builder.graphQLContext { contextBuilder ->
                contextBuilder.put(PROTO_SCHEMA, apiVersionToProtoSchemaMap[apiVersion])
            }
            input
        }

        return executionGraphQlService.execute(request)
    }

    companion object {
        const val PROTO_SCHEMA = "protoSchema"
    }
}
