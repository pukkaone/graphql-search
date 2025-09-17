package io.github.pukkaone.graphql.search.schema

import graphql.language.ObjectTypeDefinition
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import graphql.schema.idl.TypeUtil
import io.github.pukkaone.graphql.scalar.CustomGraphQLScalarTypes
import java.io.IOException
import java.io.InputStreamReader
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver

private const val BASIC_DEFINITIONS = """
    directive @document on OBJECT

    directive @id on FIELD_DEFINITION

    directive @searchable(
        type: String
    ) on FIELD_DEFINITION

    type Aggregation {
      groupBy: String!
      buckets: [Bucket!]!
    }

    type Bucket {
      key: String!
      count: Int!
      aggregations: [Aggregation!]!
    }

    enum DistanceUnit {
      KILOMETERS
      MILES
    }

    input GeoDistanceRangeInput {
      center: GeoPointInput!
      lte: Float!
      unit: DistanceUnit
    }

    type GeoPoint {
      lat: Float!
      lon: Float!
    }

    input OperatorWithGeoPointInput {
      eq: GeoPointInput
      exists: Boolean
      geoDistance: GeoDistanceRangeInput
    }

    input TermsAggregationInput {
      first: Int
    }
    """

private fun parseSchemaFiles(filePattern: String): TypeDefinitionRegistry {
    val typeDefinitionRegistry = TypeDefinitionRegistry()

    for (scalarType in CustomGraphQLScalarTypes.scalarTypes) {
        typeDefinitionRegistry.add(scalarType.definition)
    }

    val schemaParser = SchemaParser()
    typeDefinitionRegistry.merge(schemaParser.parse(BASIC_DEFINITIONS))

    val patternResolver = PathMatchingResourcePatternResolver()
    val resources: Array<Resource>
    try {
        resources = patternResolver.getResources(filePattern)
    } catch (e: IOException) {
        throw IllegalStateException("Cannot find resource files matching pattern [$filePattern]", e)
    }

    for (resource in resources) {
        try {
            InputStreamReader(resource.inputStream).use { reader ->
                typeDefinitionRegistry.merge(schemaParser.parse(reader))
            }
        } catch (e: IOException) {
            throw IllegalStateException("Cannot read resource $resource", e)
        }
    }

    return typeDefinitionRegistry
}

/**
 * First schema from which other schema are derived.
 */
class ProtoSchema(
    apiVersion: String,
) {
    val typeDefinitionRegistry: TypeDefinitionRegistry = parseSchemaFiles("search/$apiVersion/**/*.graphqls")

    val documentTypeToIdPathMap: Map<String, List<String>> =
        typeDefinitionRegistry.getTypes(ObjectTypeDefinition::class.java)
            .filter { DocumentDirective.isPresent(it) }
            .associate { it.name to findIdField(it) }

    val documentTypes: Set<String>
        get() = documentTypeToIdPathMap.keys

    private fun findIdField(objectTypeDefinition: ObjectTypeDefinition, path: MutableList<String>): Boolean {
        for (fieldDefinition in objectTypeDefinition.fieldDefinitions) {
            if (IdDirective.isPresent(fieldDefinition)) {
                path.add(fieldDefinition.name)
                return true
            }

            val unwrappedFieldType = TypeUtil.unwrapAll(fieldDefinition.type)
            val fieldType = typeDefinitionRegistry.getType(unwrappedFieldType, ObjectTypeDefinition::class.java)
            if (fieldType.isPresent) {
                path.add(fieldDefinition.name)
                if (findIdField(fieldType.get(), path)) {
                    return true
                }

                path.removeLast()
            }
        }

        return false
    }

    private fun findIdField(objectTypeDefinition: ObjectTypeDefinition): MutableList<String> {
        val path = mutableListOf<String>()
        if (findIdField(objectTypeDefinition, path)) {
            return path
        }

        error("Document type ${objectTypeDefinition.name} does not have a field annotated with @id")
    }

    /**
     * Gets the path of field names to navigate from the document root to the ID field.
     *
     * @param documentType
     *   document type name
     * @return dot separated path to the ID field
     */
    fun getIdField(documentType: String): String {
        val idPath: List<String> = documentTypeToIdPathMap[documentType]
            ?: error("Unknown document type [$documentType]")
        return idPath.joinToString(".")
    }
}
