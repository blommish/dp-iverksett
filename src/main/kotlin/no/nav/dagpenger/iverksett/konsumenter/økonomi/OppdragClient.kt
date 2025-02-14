package no.nav.dagpenger.iverksett.konsumenter.økonomi

import java.net.URI
import no.nav.dagpenger.iverksett.infrastruktur.advice.Ressurs
import no.nav.dagpenger.iverksett.infrastruktur.advice.getDataOrThrow
import no.nav.dagpenger.kontrakter.oppdrag.GrensesnittavstemmingRequest
import no.nav.dagpenger.kontrakter.oppdrag.OppdragId
import no.nav.dagpenger.kontrakter.oppdrag.OppdragStatus
import no.nav.dagpenger.kontrakter.oppdrag.Utbetalingsoppdrag
import no.nav.dagpenger.iverksett.infrastruktur.client.AbstractPingableRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder

@Service
class OppdragClient(
    @Value("\${DP_OPPDRAG_API_URL}")
    private val dagepngerOppdragUri: URI,
    @Qualifier("azure")
    restOperations: RestOperations,
) : AbstractPingableRestClient(restOperations, "dp.oppdrag") {

    private val postOppdragUri: URI = UriComponentsBuilder.fromUri(dagepngerOppdragUri).pathSegment("oppdrag").build().toUri()

    private val getStatusUri: URI = UriComponentsBuilder.fromUri(dagepngerOppdragUri).pathSegment("status").build().toUri()

    private val grensesnittavstemmingUri: URI =
        UriComponentsBuilder.fromUri(dagepngerOppdragUri).pathSegment("grensesnittavstemming").build().toUri()
    fun iverksettOppdrag(utbetalingsoppdrag: Utbetalingsoppdrag): String {
        return postForEntity<Ressurs<String>>(postOppdragUri, utbetalingsoppdrag).data!!
    }

    fun hentStatus(oppdragId: OppdragId): OppdragStatusMedMelding {
        val ressurs = postForEntity<Ressurs<OppdragStatus>>(getStatusUri, oppdragId)
        val data = ressurs.getDataOrThrow()
        return OppdragStatusMedMelding(data, ressurs.melding)
    }

    fun grensesnittavstemming(grensesnittavstemmingRequest: GrensesnittavstemmingRequest): String {
        val ressurs = postForEntity<Ressurs<String>>(grensesnittavstemmingUri, grensesnittavstemmingRequest)
        return ressurs.getDataOrThrow()
    }

    override val pingUri = postOppdragUri

    override fun ping() {
        operations.optionsForAllow(pingUri)
    }
}
