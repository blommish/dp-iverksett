package no.nav.dagpenger.iverksett.konsumenter.arbeidsoppfolging

import no.nav.dagpenger.iverksett.api.domene.IverksettOvergangsstønad
import org.springframework.stereotype.Service

@Service
class ArbeidsoppfølgingService(
    private val arbeidsoppfølgingKafkaProducer: ArbeidsoppfølgingKafkaProducer,
) {

    fun sendTilKafka(iverksettData: IverksettOvergangsstønad) {
        if (iverksettData is IverksettOvergangsstønad) {
            arbeidsoppfølgingKafkaProducer.sendVedtak(
                ArbeidsoppfølgingMapper.mapTilVedtakOvergangsstønadTilArbeidsoppfølging(iverksettData),
            )
        }
    }
}
