package io.github.jan.supabase.exceptions

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.request

open class RestException(val error: String, val description: String?, val statusCode: Int, message: String): Exception(message) {

    constructor(error: String, response: HttpResponse, message: String? = null): this(error, message, response.status.value, """
        $error${message?.let { " ($it)" } ?: ""}
        URL: ${response.request.url}
        Headers: ${response.request.headers.entries()}
        Http Method: ${response.request.method.value}
    """.trimIndent())

}

class UnauthorizedRestException(error: String, response: HttpResponse, message: String? = null): RestException(error, response, message)

class BadRequestRestException(error: String, response: HttpResponse, message: String? = null): RestException(error, response, message)


class NotFoundRestException(error: String, response: HttpResponse, message: String? = null): RestException(error, response, message)

class UnknownRestException(error: String, response: HttpResponse, message: String? = null): RestException(error, response, message)
