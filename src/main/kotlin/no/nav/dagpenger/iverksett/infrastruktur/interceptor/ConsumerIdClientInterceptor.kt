package no.nav.dagpenger.iverksett.infrastruktur.interceptor

import no.nav.dagpenger.iverksett.infrastruktur.util.NavHttpHeaders
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component

@Component
class ConsumerIdClientInterceptor(
    @Value("\${application.name}") private val appName: String,
    @Value("\${credential.username:}") private val serviceUser: String
) :
    ClientHttpRequestInterceptor {

    override fun intercept(request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution): ClientHttpResponse {
        request.headers.add(NavHttpHeaders.NAV_CONSUMER_ID.asString(), serviceUser.ifBlank { appName })
        return execution.execute(request, body)
    }
}
