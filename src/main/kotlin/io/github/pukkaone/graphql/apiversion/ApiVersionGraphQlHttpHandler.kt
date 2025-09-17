package io.github.pukkaone.graphql.apiversion

import org.springframework.graphql.server.WebGraphQlHandler
import org.springframework.graphql.server.webmvc.GraphQlHttpHandler
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse

/**
 * Extracts API version from the URL path, and puts it into a request attribute for
 * [RoutingExecutionGraphQlService] to get.
 */
class ApiVersionGraphQlHttpHandler(
    webGraphQlHandler: WebGraphQlHandler,
) : GraphQlHttpHandler(webGraphQlHandler) {

    override fun handleRequest(serverRequest: ServerRequest): ServerResponse {
        val path = serverRequest.path();
        val apiVersionIndex = path.lastIndexOf('/');
        val apiVersion = path.substring(apiVersionIndex + 1);
        serverRequest.attributes()[API_VERSION] = apiVersion;

        return super.handleRequest(serverRequest);
    }

    companion object {
        /** name of request attribute that holds API version */
        val API_VERSION = ApiVersionGraphQlHttpHandler::class.qualifiedName + ".API_VERSION"
    }
}
