package no.nav.dagpenger.iverksett.configuration

import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.iverksett.konsumenter.økonomi.OppdragClient
import no.nav.dagpenger.iverksett.konsumenter.økonomi.OppdragStatusMedMelding
import no.nav.dagpenger.kontrakter.oppdrag.OppdragStatus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("mock-oppdrag")
class OppdragClientMock {

    @Bean
    @Primary
    fun oppdragClient(): OppdragClient {
        val oppdragClientMock = mockk<OppdragClient>()

        every { oppdragClientMock.grensesnittavstemming(any()) } returns "OK"
        every { oppdragClientMock.iverksettOppdrag(any()) } returns "OK"
        every { oppdragClientMock.hentStatus(any()) } returns OppdragStatusMedMelding(OppdragStatus.KVITTERT_OK, "OK")

        return oppdragClientMock
    }
}
