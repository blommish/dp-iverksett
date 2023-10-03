package no.nav.dagpenger.iverksett.konsumenter.økonomi

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.iverksett.api.IverksettingRepository
import no.nav.dagpenger.iverksett.api.IverksettingService
import no.nav.dagpenger.iverksett.api.domene.OppdragResultat
import no.nav.dagpenger.iverksett.api.domene.TilkjentYtelse
import no.nav.dagpenger.iverksett.api.tilstand.IverksettResultatService
import no.nav.dagpenger.iverksett.infrastruktur.repository.findByIdOrThrow
import no.nav.dagpenger.iverksett.infrastruktur.transformer.toDomain
import no.nav.dagpenger.iverksett.infrastruktur.util.opprettIverksettDto
import no.nav.dagpenger.iverksett.lagIverksett
import no.nav.dagpenger.iverksett.util.mockFeatureToggleService
import no.nav.dagpenger.kontrakter.felles.Fagsystem
import no.nav.dagpenger.kontrakter.oppdrag.OppdragStatus
import no.nav.dagpenger.kontrakter.oppdrag.Utbetalingsoppdrag
import no.nav.dagpenger.kontrakter.oppdrag.Utbetalingsoppdrag.KodeEndring.NY
import no.nav.dagpenger.kontrakter.oppdrag.Utbetalingsperiode
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Properties
import java.util.UUID

internal class VentePåStatusFraØkonomiTaskTest {

    private val oppdragClient = mockk<OppdragClient>()
    private val iverksettingRepository = mockk<IverksettingRepository>()
    private val taskService = mockk<TaskService>()
    private val iverksettResultatService = mockk<IverksettResultatService>()
    private val behandlingId: UUID = UUID.randomUUID()
    private val sakId: UUID = UUID.randomUUID()
    private val iverksettingService = IverksettingService(
        taskService = taskService,
        oppdragClient = oppdragClient,
        iverksettingRepository = iverksettingRepository,
        iverksettResultatService = iverksettResultatService,
        featureToggleService = mockFeatureToggleService(),
    )

    private val ventePåStatusFraØkonomiTask =
        VentePåStatusFraØkonomiTask(
            iverksettingRepository,
            iverksettingService,
            taskService,
            iverksettResultatService,
            mockFeatureToggleService(),
        )

    @BeforeEach
    internal fun setUp() {
        every { oppdragClient.hentStatus(any()) } returns OppdragStatusMedMelding(OppdragStatus.KVITTERT_OK, "OK")
        every { iverksettingRepository.findByIdOrThrow(any()) } returns lagIverksett(opprettIverksettDto(behandlingId, sakId).toDomain())
        every { iverksettResultatService.oppdaterOppdragResultat(behandlingId, any()) } just runs
        every { taskService.save(any()) } answers { firstArg() }
    }

    @Test
    internal fun `kjør doTask for VentePåStatusFraØkonomiTaskhvis, forvent ingen unntak`() {
        val oppdragResultatSlot = slot<OppdragResultat>()
        every { iverksettResultatService.hentTilkjentYtelse(behandlingId) } returns tilkjentYtelse(
            listOf(
                utbetalingsperiode,
            ),
        )

        runTask(Task(IverksettMotOppdragTask.TYPE, behandlingId.toString(), Properties()))

        verify(exactly = 1) {
            iverksettResultatService.oppdaterOppdragResultat(
                behandlingId,
                capture(oppdragResultatSlot),
            )
        }
        assertThat(oppdragResultatSlot.captured.oppdragStatus).isEqualTo(OppdragStatus.KVITTERT_OK)
        verify(exactly = 1) { taskService.save(any()) }
    }

    @Test
    internal fun `Skal ikke gjøre noe hvis ingen utbetalingoppdrag på tilkjent ytelse`() {
        every { iverksettResultatService.hentTilkjentYtelse(behandlingId) } returns tilkjentYtelse()

        runTask(Task(IverksettMotOppdragTask.TYPE, behandlingId.toString(), Properties()))

        verify(exactly = 0) { iverksettResultatService.oppdaterOppdragResultat(behandlingId, any()) }
        verify(exactly = 1) { taskService.save(any()) }
    }

    @Test
    @Disabled
    internal fun `migrering - skal ikke opprette task for journalføring av vedtaksbrev`() {
        // TODO hvorfor feiler denne
        val opprettIverksettDto = opprettIverksettDto(behandlingId, sakId)
        every { iverksettingRepository.findByIdOrThrow(any()) } returns lagIverksett(opprettIverksettDto.toDomain())
        every { iverksettResultatService.hentTilkjentYtelse(behandlingId) } returns tilkjentYtelse(
            listOf(
                utbetalingsperiode,
            ),
        )

        runTask(Task(IverksettMotOppdragTask.TYPE, behandlingId.toString(), Properties()))

        verify(exactly = 0) { taskService.save(any()) }
    }

    private fun runTask(task: Task) {
        ventePåStatusFraØkonomiTask.doTask(task)
        ventePåStatusFraØkonomiTask.onCompletion(task)
    }

    private val utbetalingsperiode = Utbetalingsperiode(
        erEndringPåEksisterendePeriode = false,
        opphør = null,
        periodeId = 0,
        forrigePeriodeId = null,
        datoForVedtak = LocalDate.now(),
        klassifisering = "",
        vedtakdatoFom = LocalDate.of(2021, 1, 1),
        vedtakdatoTom = LocalDate.of(2021, 6, 1),
        sats = BigDecimal.TEN,
        satsType = Utbetalingsperiode.SatsType.DAG,
        utbetalesTil = "x",
        behandlingId = UUID.randomUUID(),
        utbetalingsgrad = null,
    )

    private fun tilkjentYtelse(utbetalingsperioder: List<Utbetalingsperiode> = listOf()): TilkjentYtelse {
        return TilkjentYtelse(
            id = UUID.randomUUID(),
            utbetalingsoppdrag = Utbetalingsoppdrag(
                kodeEndring = NY,
                fagSystem = Fagsystem.Dagpenger,
                saksnummer = UUID.randomUUID(),
                aktoer = "",
                saksbehandlerId = "",
                avstemmingTidspunkt = LocalDateTime.now(),
                utbetalingsperiode = utbetalingsperioder,
            ),
            andelerTilkjentYtelse = listOf(),
        )
    }
}
