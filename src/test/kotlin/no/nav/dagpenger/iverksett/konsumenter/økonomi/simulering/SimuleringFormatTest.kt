package no.nav.dagpenger.iverksett.konsumenter.økonomi.simulering

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.dagpenger.iverksett.ResourceLoaderTestUtil
import no.nav.dagpenger.kontrakter.felles.objectMapper
import no.nav.dagpenger.kontrakter.iverksett.SimuleringDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class SimuleringFormatTest {

    @Test
    fun `sjekk at v1 lar seg deserialisere`() {
        val v1: String = ResourceLoaderTestUtil.readResource("json/simulering_v1.json")
        val simuleringDtoV1 = objectMapper.readValue<SimuleringDto>(v1)

        assertNotNull(simuleringDtoV1.forrigeBehandlingId)
        assertEquals(2, simuleringDtoV1.utbetalinger.size)
    }
}
