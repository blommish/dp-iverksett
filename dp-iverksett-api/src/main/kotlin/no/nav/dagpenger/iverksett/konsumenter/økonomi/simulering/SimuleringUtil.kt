package no.nav.dagpenger.iverksett.konsumenter.økonomi.simulering

import no.nav.dagpenger.iverksett.konsumenter.økonomi.simulering.SimuleringsperiodeEtterbetaling.etterbetaling
import no.nav.dagpenger.iverksett.konsumenter.økonomi.simulering.SimuleringsperiodeEtterbetaling.medEtterbetaling
import no.nav.dagpenger.iverksett.kontrakter.felles.Datoperiode
import no.nav.dagpenger.iverksett.kontrakter.simulering.BeriketSimuleringsresultat
import no.nav.dagpenger.iverksett.kontrakter.simulering.DetaljertSimuleringResultat
import no.nav.dagpenger.iverksett.kontrakter.simulering.FagOmrådeKode
import no.nav.dagpenger.iverksett.kontrakter.simulering.PosteringType
import no.nav.dagpenger.iverksett.kontrakter.simulering.PosteringType.FEILUTBETALING
import no.nav.dagpenger.iverksett.kontrakter.simulering.PosteringType.YTELSE
import no.nav.dagpenger.iverksett.kontrakter.simulering.SimuleringMottaker
import no.nav.dagpenger.iverksett.kontrakter.simulering.Simuleringsoppsummering
import no.nav.dagpenger.iverksett.kontrakter.simulering.Simuleringsperiode
import no.nav.dagpenger.iverksett.kontrakter.simulering.SimulertPostering
import no.nav.dagpenger.kontrakter.utbetaling.StønadType
import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.time.LocalDate
import java.util.WeakHashMap

fun lagSimuleringsoppsummering(
    detaljertSimuleringResultat: DetaljertSimuleringResultat,
    tidSimuleringHentet: LocalDate,
): Simuleringsoppsummering {
    val perioder = grupperPosteringerEtterDato(detaljertSimuleringResultat.simuleringMottaker)

    val framtidigePerioder =
        perioder.filter {
            it.fom > tidSimuleringHentet ||
                (it.tom > tidSimuleringHentet && it.forfallsdato > tidSimuleringHentet)
        }

    val nestePeriode = framtidigePerioder.filter { it.feilutbetaling == ZERO }.minByOrNull { it.fom }
    val tomSisteUtbetaling =
        perioder.filter { nestePeriode == null || it.fom < nestePeriode.fom }.maxOfOrNull { it.tom }

    return Simuleringsoppsummering(
        perioder = perioder,
        fomDatoNestePeriode = nestePeriode?.fom,
        etterbetaling = hentTotalEtterbetaling(perioder, nestePeriode?.fom),
        feilutbetaling = hentTotalFeilutbetaling(perioder, nestePeriode?.fom),
        fom = perioder.minOfOrNull { it.fom },
        tomDatoNestePeriode = nestePeriode?.tom,
        forfallsdatoNestePeriode = nestePeriode?.forfallsdato,
        tidSimuleringHentet = tidSimuleringHentet,
        tomSisteUtbetaling = tomSisteUtbetaling,
    )
}

fun grupperPosteringerEtterDato(mottakere: List<SimuleringMottaker>?): List<Simuleringsperiode> {
    return mottakere
        ?.flatMap { it.simulertPostering }
        ?.filter { it.posteringType == FEILUTBETALING || it.posteringType == YTELSE }
        ?.groupBy { PeriodeMedForfall(fom = it.fom, tom = it.tom, forfallsdato = it.forfallsdato) }
        ?.map { (periodeMedForfall, posteringListe) ->
            Simuleringsperiode(
                periodeMedForfall.fom,
                periodeMedForfall.tom,
                periodeMedForfall.forfallsdato,
                nyttBeløp = hentNyttBeløp(posteringListe),
                tidligereUtbetalt = hentTidligereUtbetalt(posteringListe),
                resultat = hentResultat(posteringListe),
                feilutbetaling = posteringListe.sumBarePositiv(FEILUTBETALING),
            ).medEtterbetaling(hentEtterbetaling(posteringListe))
        } ?: emptyList()
}

fun fagområdeKoderForPosteringer(stønadType: StønadType): Set<FagOmrådeKode> = when (stønadType) {
    StønadType.DAGPENGER_ARBEIDSSOKER_ORDINAER,
    StønadType.DAGPENGER_PERMITTERING_ORDINAER,
    StønadType.DAGPENGER_PERMITTERING_FISKEINDUSTRI,
    StønadType.DAGPENGER_EOS,
    -> setOf(FagOmrådeKode.DAGPENGER)
}

private fun hentNyttBeløp(posteringer: List<SimulertPostering>) =
    posteringer.sumBarePositiv(YTELSE) - posteringer.sumBarePositiv(FEILUTBETALING)

private fun hentTidligereUtbetalt(posteringer: List<SimulertPostering>) =
    posteringer.sumBareNegativ(FEILUTBETALING) - posteringer.sumBareNegativ(YTELSE)

private fun hentResultat(posteringer: List<SimulertPostering>): BigDecimal {
    val positivFeilutbetaling = posteringer.sumBarePositiv(FEILUTBETALING)

    return when {
        positivFeilutbetaling > ZERO -> -positivFeilutbetaling
        else -> hentNyttBeløp(posteringer) - hentTidligereUtbetalt(posteringer)
    }
}

private fun hentEtterbetaling(posteringer: List<SimulertPostering>) =
    when {
        posteringer.sumBarePositiv(FEILUTBETALING) > ZERO -> ZERO
        else -> hentResultat(posteringer) + posteringer.sumBareNegativ(FEILUTBETALING)
    }

private fun hentTotalEtterbetaling(simuleringsperioder: List<Simuleringsperiode>, fomDatoNestePeriode: LocalDate?) =
    simuleringsperioder
        .filter { fomDatoNestePeriode == null || it.fom < fomDatoNestePeriode }
        .sumOf { it.etterbetaling ?: ZERO }
        .let { maxOf(it, ZERO) }

private fun hentTotalFeilutbetaling(simuleringsperioder: List<Simuleringsperiode>, fomDatoNestePeriode: LocalDate?) =
    simuleringsperioder
        .filter { fomDatoNestePeriode == null || it.fom < fomDatoNestePeriode }
        .sumOf { it.feilutbetaling }

private fun List<SimulertPostering>.sumBarePositiv(type: PosteringType) =
    this.filter { it.posteringType == type && it.beløp > ZERO }.sumOf { it.beløp }

private fun List<SimulertPostering>.sumBareNegativ(type: PosteringType) =
    this.filter { it.posteringType == type && it.beløp < ZERO }.sumOf { it.beløp }

private data class PeriodeMedForfall(
    val fom: LocalDate,
    val tom: LocalDate,
    val forfallsdato: LocalDate,
)

private object SimuleringsperiodeEtterbetaling {

    // Simuleringsperiode mangler etterbetaling. Dette er et lite påbygg for å emulere at det finnes.
    fun Simuleringsperiode.medEtterbetaling(etterbetaling: BigDecimal?): Simuleringsperiode {
        simuleringsperiodeEtterbetalingMap[this] = etterbetaling
        return this
    }

    val Simuleringsperiode.etterbetaling: BigDecimal?
        get() = simuleringsperiodeEtterbetalingMap[this]

    private val simuleringsperiodeEtterbetalingMap = WeakHashMap<Simuleringsperiode, BigDecimal?>()
}

fun BeriketSimuleringsresultat.harFeilutbetaling(): Boolean {
    return this.oppsummering.feilutbetaling > ZERO
}

fun Simuleringsoppsummering.hentSammenhengendePerioderMedFeilutbetaling(): List<Datoperiode> {
    val perioderMedFeilutbetaling =
        perioder?.sortedBy { it.fom }?.filter { it.feilutbetaling > BigDecimal(0) }?.map {
            Datoperiode(it.fom, it.tom)
        } ?: emptyList()

    return perioderMedFeilutbetaling.fold(mutableListOf()) { akkumulatorListe, nestePeriode ->
        val gjeldendePeriode = akkumulatorListe.lastOrNull()

        if (gjeldendePeriode != null && erPerioderSammenhengende(gjeldendePeriode, nestePeriode)) {
            val oppdatertGjeldendePeriode = Datoperiode(fom = gjeldendePeriode.fom, tom = nestePeriode.tom)
            akkumulatorListe.removeLast()
            akkumulatorListe.add(oppdatertGjeldendePeriode)
        } else {
            akkumulatorListe.add(nestePeriode)
        }
        akkumulatorListe
    }
}

private fun erPerioderSammenhengende(gjeldendePeriode: Datoperiode, nestePeriode: Datoperiode) =
    gjeldendePeriode.tom.plusDays(1) == nestePeriode.fom
