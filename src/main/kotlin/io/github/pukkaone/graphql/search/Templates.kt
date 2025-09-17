package io.github.pukkaone.graphql.search

import java.nio.charset.StandardCharsets
import org.springframework.context.expression.MapAccessor
import org.springframework.core.io.ClassPathResource
import org.springframework.expression.common.TemplateParserContext
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.SimpleEvaluationContext
import org.springframework.util.StreamUtils

/**
 * Replaces `#{variable}` placeholders in template with actual values.
 */
object Templates {

    private val EXPRESSION_PARSER = SpelExpressionParser()
    private val PARSER_CONTEXT = TemplateParserContext()
    private val EVALUATION_CONTEXT = SimpleEvaluationContext.forPropertyAccessors(MapAccessor()).build()

    /**
     * Reads template from classpath resource file.
     *
     * @param templateFile
     *   template file path
     * @return template
     */
    fun read(templateFile: String): String {
        return StreamUtils.copyToString(
            ClassPathResource(templateFile).inputStream, StandardCharsets.UTF_8)
    }

    /**
     * Replaces `#{variable}` placeholders in template with actual values.
     *
     * @param template
     *   string having variables to replace
     * @param variables
     *   variable name to value map
     * @return rendered result
     */
    fun substitute(template: String, variables: Map<String, Any>): String {
        val expression = EXPRESSION_PARSER.parseExpression(template, PARSER_CONTEXT)
        return expression.getValue(EVALUATION_CONTEXT, variables, String::class.java).orEmpty()
    }
}
