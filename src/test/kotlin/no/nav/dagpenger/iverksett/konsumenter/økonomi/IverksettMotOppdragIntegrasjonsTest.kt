package no.nav.dagpenger.iverksett.konsumenter.økonomi

import no.nav.dagpenger.iverksett.ServerTest
import no.nav.dagpenger.iverksett.api.IverksettingService
import no.nav.dagpenger.iverksett.api.tilstand.IverksettResultatService
import no.nav.dagpenger.iverksett.infrastruktur.util.opprettBrev
import no.nav.dagpenger.iverksett.infrastruktur.util.opprettIverksettDagpenger
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID

class IverksettMotOppdragIntegrasjonsTest : ServerTest() {

    @Autowired
    lateinit var iverksettResultatService: IverksettResultatService

    @Autowired
    lateinit var taskService: TaskService

    @Autowired
    lateinit var iverksettingService: IverksettingService

    @Autowired
    lateinit var iverksettMotOppdragTask: IverksettMotOppdragTask

    private val behandlingid: UUID = UUID.randomUUID()
    private val førsteAndel = lagAndelTilkjentYtelse(
        beløp = 1000,
        fraOgMed = LocalDate.of(2021, 1, 1),
        tilOgMed = LocalDate.of(2021, 1, 31),
    )
    private val iverksett =
        opprettIverksettDagpenger(behandlingid, andeler = listOf(førsteAndel), startdato = førsteAndel.periode.fom)

    @BeforeEach
    internal fun setUp() {
        iverksettingService.startIverksetting(iverksett, opprettBrev())
        iverksettMotOppdrag()
    }

    @Test
    internal fun `start iverksetting, forvent at andelerTilkjentYtelse er lik 1 og har periodeId 1`() {
        val tilkjentYtelse = iverksettResultatService.hentTilkjentYtelse(behandlingid)!!
        assertThat(tilkjentYtelse.andelerTilkjentYtelse).hasSize(1)
        assertThat(tilkjentYtelse.andelerTilkjentYtelse.first().periodeId).isEqualTo(1)
    }

    @Test
    internal fun `revurdering med en ny periode, forvent at den nye perioden har peker på den forrige`() {
        val behandlingIdRevurdering = UUID.randomUUID()
        val iverksettRevurdering = opprettIverksettDagpenger(
            behandlingIdRevurdering,
            behandlingid,
            listOf(
                førsteAndel.copy(id = UUID.randomUUID()),
                lagAndelTilkjentYtelse(
                    beløp = 1000,
                    fraOgMed = LocalDate.now(),
                    tilOgMed = LocalDate.now().plusMonths(1),
                ),
            ),
            forrigeIverksetting = iverksett,
        )

        taskService.deleteAll(taskService.findAll())
        iverksettingService.startIverksetting(iverksettRevurdering, opprettBrev())
        iverksettMotOppdrag()

        val tilkjentYtelse = iverksettResultatService.hentTilkjentYtelse(behandlingIdRevurdering)!!
        assertThat(tilkjentYtelse.andelerTilkjentYtelse).hasSize(2)
        assertThat(tilkjentYtelse.andelerTilkjentYtelse.first().periodeId).isEqualTo(1)
        assertThat(tilkjentYtelse.andelerTilkjentYtelse[1].periodeId).isEqualTo(2)
        assertThat(tilkjentYtelse.andelerTilkjentYtelse[1].forrigePeriodeId).isEqualTo(1)
    }

    @Test
    internal fun `revurdering der beløpet på den første endres, og en ny legges til, forvent at den første perioden erstattes`() {
        val behandlingIdRevurdering = UUID.randomUUID()
        val iverksettRevurdering = opprettIverksettDagpenger(
            behandlingId = behandlingIdRevurdering,
            forrigeBehandlingId = behandlingid,
            andeler = listOf(
                førsteAndel.copy(beløp = 299),
                lagAndelTilkjentYtelse(
                    beløp = 1000,
                    fraOgMed = LocalDate.now(),
                    tilOgMed = LocalDate.now().plusMonths(1),
                ),
            ),
            forrigeIverksetting = iverksett,
        )

        taskService.deleteAll(taskService.findAll())
        iverksettingService.startIverksetting(iverksettRevurdering, opprettBrev())
        iverksettMotOppdrag()

        val tilkjentYtelse = iverksettResultatService.hentTilkjentYtelse(behandlingIdRevurdering)!!
        assertThat(tilkjentYtelse.andelerTilkjentYtelse).hasSize(2)
        assertThat(tilkjentYtelse.andelerTilkjentYtelse.first().periodeId).isEqualTo(2)
        assertThat(tilkjentYtelse.andelerTilkjentYtelse[1].periodeId).isEqualTo(3)
        assertThat(tilkjentYtelse.andelerTilkjentYtelse[1].forrigePeriodeId).isEqualTo(2)
    }

    @Test
    internal fun `iverksett med opphør, forventer beløp lik 0 og dato lik LocalDate MIN`() {
        val opphørBehandlingId = UUID.randomUUID()
        val startdato = førsteAndel.periode.fom
        val iverksettMedOpphør =
            opprettIverksettDagpenger(
                opphørBehandlingId,
                behandlingid,
                emptyList(),
                startdato = startdato,
                forrigeIverksetting = iverksett,
            )

        taskService.deleteAll(taskService.findAll())
        iverksettingService.startIverksetting(iverksettMedOpphør, opprettBrev())
        iverksettMotOppdrag()

        val tilkjentYtelse = iverksettResultatService.hentTilkjentYtelse(opphørBehandlingId)!!
        assertThat(tilkjentYtelse.andelerTilkjentYtelse).hasSize(1)
        assertThat(tilkjentYtelse.andelerTilkjentYtelse.first().periodeId).isEqualTo(1)
        assertThat(tilkjentYtelse.andelerTilkjentYtelse.first().beløp).isEqualTo(0)
        assertThat(tilkjentYtelse.andelerTilkjentYtelse.first().periode.fom).isEqualTo(LocalDate.MIN)
        assertThat(tilkjentYtelse.andelerTilkjentYtelse.first().periode.tom).isEqualTo(LocalDate.MIN)
    }

    @Test
    internal fun `iverksetting med feil forrige iverksetting skal gi exception`() {
        val opphørBehandlingId = UUID.randomUUID()
        val startdato = førsteAndel.periode.fom

        val feilFørsteAndel = førsteAndel.copy(beløp = førsteAndel.beløp + 1)
        val feilForrigeIverksetting =
            opprettIverksettDagpenger(
                behandlingid,
                andeler = listOf(feilFørsteAndel),
                startdato = feilFørsteAndel.periode.fom,
            )

        val iverksettMedFeilForrige =
            opprettIverksettDagpenger(
                opphørBehandlingId,
                behandlingid,
                listOf(
                    førsteAndel,
                    lagAndelTilkjentYtelse(
                        beløp = 1000,
                        fraOgMed = LocalDate.now(),
                        tilOgMed = LocalDate.now().plusMonths(1),
                    ),
                ),
                startdato = startdato,
                forrigeIverksetting = feilForrigeIverksetting,
            )

        taskService.deleteAll(taskService.findAll())
        iverksettingService.startIverksetting(iverksettMedFeilForrige, opprettBrev())

        assertThatIllegalStateException().isThrownBy {
            iverksettMotOppdrag()
        }.withMessage("Lagret forrige tilkjent ytelse stemmer ikke med mottatt forrige tilkjent ytelse")
    }

    private fun iverksettMotOppdrag() {
        val tasks = taskService.findAll()
        assertThat(tasks).hasSize(1)
        iverksettMotOppdragTask.doTask(tasks.first())
    }
}
