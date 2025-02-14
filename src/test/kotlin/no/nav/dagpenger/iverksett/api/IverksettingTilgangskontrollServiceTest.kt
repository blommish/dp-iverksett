package no.nav.dagpenger.iverksett.api

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.dagpenger.iverksett.api.tilgangskontroll.IverksettingTilgangskontrollService
import no.nav.dagpenger.iverksett.api.tilgangskontroll.TokenContext
import no.nav.dagpenger.iverksett.api.tilstand.IverksettingsresultatService
import no.nav.dagpenger.iverksett.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.dagpenger.iverksett.infrastruktur.util.opprettIverksettDto
import no.nav.dagpenger.iverksett.konsumenter.økonomi.OppdragClient
import no.nav.dagpenger.iverksett.lagIverksettingEntitet
import no.nav.dagpenger.iverksett.lagIverksettingsdata
import no.nav.dagpenger.iverksett.util.mockFeatureToggleService
import no.nav.dagpenger.kontrakter.iverksett.VedtakType
import no.nav.familie.prosessering.internal.TaskService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.http.HttpStatus

class IverksettingTilgangskontrollServiceTest {

    private val iverksettingRepository = mockk<IverksettingRepository>()
    private val featureToggleServiceMock = mockk<FeatureToggleService>()
    private val iverksettingServiceMock = IverksettingService(
        taskService = mockk<TaskService>(),
        iverksettingsresultatService = mockk<IverksettingsresultatService>(),
        iverksettingRepository = iverksettingRepository,
        oppdragClient = mockk<OppdragClient>(),
        featureToggleService = mockFeatureToggleService(),
    )
    private lateinit var iverksettingTilgangskontrollService: IverksettingTilgangskontrollService

    @BeforeEach
    fun setup() {
        iverksettingTilgangskontrollService = IverksettingTilgangskontrollService(
            iverksettingServiceMock,
            featureToggleServiceMock,
            BESLUTTERGRUPPE,
            APP_MED_SYSTEMTILGANG
        )

        every { featureToggleServiceMock.isEnabled(any(), any()) } returns true
        mockkObject(TokenContext)
    }

    @AfterEach
    fun cleanup() {
        unmockkObject(TokenContext)
    }

    @Test
    fun `skal få OK når rammevedtak sendes av beslutter`() {
        val nåværendeIverksetting = VedtakType.RAMMEVEDTAK.iverksetting()

        every { iverksettingRepository.findByFagsakId(any()) } returns emptyList()
        every { TokenContext.hentGrupper() } returns listOf(BESLUTTERGRUPPE)
        every { TokenContext.erSystemtoken() } returns false

        assertDoesNotThrow {
            iverksettingTilgangskontrollService.valider(nåværendeIverksetting)
        }
    }

    @Test
    fun `skal få FORBIDDEN når første vedtak på sak sendes uten beslutter-token`() {
        val nåværendeIverksetting = opprettIverksettDto()

        every { iverksettingRepository.findByFagsakId(any()) } returns emptyList()
        every { TokenContext.erSystemtoken() } returns true

        assertApiFeil(HttpStatus.FORBIDDEN) {
            iverksettingTilgangskontrollService.valider(nåværendeIverksetting)
        }
    }

    @Test
    fun `skal få OK når utbetalingsvedtak sendes etter autorisert vedtak`() {
        val forrigeIverksetting = VedtakType.RAMMEVEDTAK.iverksettData()
        val nåværendeIverksetting = VedtakType.UTBETALINGSVEDTAK.iverksetting()
        val iverksettListe = listOf(lagIverksettingEntitet(forrigeIverksetting))

        every { iverksettingRepository.findByFagsakId(any()) } returns iverksettListe
        every { TokenContext.erSystemtoken() } returns true
        every { TokenContext.hentKlientnavn() } returns APP_MED_SYSTEMTILGANG

        assertDoesNotThrow {
            iverksettingTilgangskontrollService.valider(nåværendeIverksetting)
        }
    }

    @Test
    fun `skal få FORBIDDEN når utbetalingsvedtak sendes av ukjent system`() {
        val forrigeIverksetting = VedtakType.RAMMEVEDTAK.iverksettData()
        val nåværendeIverksetting = VedtakType.UTBETALINGSVEDTAK.iverksetting()
        val iverksettListe = listOf(lagIverksettingEntitet(forrigeIverksetting))

        every { iverksettingRepository.findByFagsakId(any()) } returns iverksettListe
        every { TokenContext.erSystemtoken() } returns true
        every { TokenContext.hentKlientnavn() } returns "ukjent app"

        assertApiFeil(HttpStatus.FORBIDDEN) {
            iverksettingTilgangskontrollService.valider(nåværendeIverksetting)
        }
    }

    private fun VedtakType.iverksettData() = lagIverksettingsdata().let {
        it.copy(vedtak = it.vedtak.copy(vedtakstype = this))
    }

    private fun VedtakType.iverksetting() = opprettIverksettDto().let {
        it.copy(vedtak = it.vedtak.copy(vedtakstype = this))
    }

    companion object {
        private const val BESLUTTERGRUPPE = "0000-GA-Beslutter"
        private const val APP_MED_SYSTEMTILGANG = "dev-gcp:teamdagpenger:dp-vedtak-iverksett"
    }
}
