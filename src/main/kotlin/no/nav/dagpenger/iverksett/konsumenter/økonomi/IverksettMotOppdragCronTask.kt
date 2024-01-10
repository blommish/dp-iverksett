package no.nav.dagpenger.iverksett.konsumenter.økonomi

import no.nav.dagpenger.iverksett.api.IverksettingService
import no.nav.dagpenger.iverksett.api.domene.TilkjentYtelse
import no.nav.dagpenger.iverksett.api.tilstand.IverksettingsresultatService
import no.nav.dagpenger.iverksett.konsumenter.opprettNesteTask
import no.nav.dagpenger.kontrakter.oppdrag.OppdragStatus
import no.nav.dagpenger.kontrakter.oppdrag.Utbetalingsperiode
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*

@Service
@TaskStepBeskrivelse(
    taskStepType = IverksettMotOppdragCronTask.TYPE,
    beskrivelse = "Utfører iverksetting av utbetalning mot økonomi per måned",
)
class IverksettMotOppdragCronTask(
    private val iverksettingService: IverksettingService,
    private val oppdragClient: OppdragClient,
    private val taskService: TaskService,
    private val iverksettingsresultatService: IverksettingsresultatService,
) : AsyncTaskStep {

    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        // finn siste behandlingen for hver fagsak, denne tasken burde være opprettet på selve fagsaken?
        val behandlingId = UUID.randomUUID()
        val iverksettingsresultat = iverksettingsresultatService.hentIverksettResultat(behandlingId)!!

        val oppdragResultat = iverksettingsresultat.oppdragResultat ?: error("Finner ikke ")
        if (oppdragResultat.oppdragStatus != OppdragStatus.KVITTERT_OK) {
            error("...")
        }
        iverksettingsresultatService.oppdaterOppdragResultat(behandlingId, null)
        val tilkjentYtelse = iverksettingsresultat.tilkjentYtelseForUtbetaling!!
        val utbetalingsoppdrag = tilkjentYtelse?.utbetalingsoppdrag!!

        val perioderSomIverksettesNå =
            utbetalingsoppdrag.utbetalingsperiode.filter { it.iverksettId == null && it.vedtakdatoTom < LocalDate.now() }
        oppdaterTilkjentYtelse(perioderSomIverksettesNå, behandlingId, tilkjentYtelse)
        if (perioderSomIverksettesNå.isNotEmpty()) {
            oppdragClient.iverksettOppdrag(utbetalingsoppdrag.copy(utbetalingsperiode = perioderSomIverksettesNå))
        } else {
            log.warn("Iverksetter ikke noe mot oppdrag. Ingen utbetalingsperioder i utbetalingsoppdraget. behandlingId=$behandlingId")
        }
    }

    private fun oppdaterTilkjentYtelse(
        perioder: List<Utbetalingsperiode>,
        behandlingId: UUID,
        tilkjentYtelse: TilkjentYtelse
    ) {
        val perioderPåId = perioder.associateBy { it.periodeId }
        val utbetalingsoppdrag = tilkjentYtelse.utbetalingsoppdrag!!
        val oppdatertOppdrag =
            utbetalingsoppdrag.copy(utbetalingsperiode = utbetalingsoppdrag.utbetalingsperiode.map { periode ->
                perioderPåId[periode.periodeId]?.let {
                    periode.iverksettId = it.iverksettId // endre til copy
                    periode
                } ?: periode
            })
        iverksettingsresultatService.oppdaterTilkjentYtelseForUtbetaling(
            behandlingId = behandlingId,
            tilkjentYtelseForUtbetaling = tilkjentYtelse.copy(utbetalingsoppdrag = oppdatertOppdrag),
            oppsplittetOppdrag = true // denne skal egentlige ikke oppdateres
        )
    }

    // payload er unik, så blir krock på behandlingId
    override fun onCompletion(task: Task) {
        taskService.save(task.opprettNesteTask())
    }

    companion object {

        const val TYPE = "utførIverksettingAvUtbetalingMånedsjobb"
    }
}

// Endre til field på periode
var Utbetalingsperiode.iverksettId: UUID?
    get() = null
    set(value) {
        println("asd")
    }