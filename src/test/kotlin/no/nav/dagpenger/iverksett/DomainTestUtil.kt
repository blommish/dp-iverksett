package no.nav.dagpenger.iverksett

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.dagpenger.iverksett.api.domene.Iverksetting
import no.nav.dagpenger.iverksett.api.domene.IverksettingEntitet
import no.nav.dagpenger.iverksett.api.domene.Vedtaksperiode
import no.nav.dagpenger.iverksett.infrastruktur.util.behandlingsdetaljer
import no.nav.dagpenger.iverksett.infrastruktur.util.opprettIverksett
import no.nav.dagpenger.iverksett.infrastruktur.util.vedtaksdetaljer
import no.nav.dagpenger.iverksett.konsumenter.økonomi.lagAndelTilkjentYtelse
import no.nav.dagpenger.kontrakter.iverksett.Vedtaksresultat

fun Int.januar(år: Int) = LocalDate.of(år, 1, this)
fun Int.mai(år: Int) = LocalDate.of(år, 5, this)

fun lagIverksettingsdata(
    forrigeBehandlingId: UUID? = null,
    vedtaksresultat: Vedtaksresultat = Vedtaksresultat.INNVILGET,
    vedtaksperioder: List<Vedtaksperiode> = emptyList(),
    andelsdatoer: List<LocalDate> = emptyList(),
    beløp: Int = 100,
    vedtakstidspunkt: LocalDateTime = LocalDateTime.now(),
): Iverksetting {
    return opprettIverksett(
        behandlingsdetaljer = behandlingsdetaljer(
            forrigeBehandlingId = forrigeBehandlingId,
        ),
        vedtaksdetaljer = vedtaksdetaljer(
            vedtaksresultat = vedtaksresultat,
            vedtaksperioder = vedtaksperioder,
            andeler = andelsdatoer.map {
                lagAndelTilkjentYtelse(beløp = beløp, fraOgMed = it, tilOgMed = it)
            },
            vedtakstidspunkt = vedtakstidspunkt,
        ),
    )
}

fun lagIverksettingEntitet(iverksettingData: Iverksetting) = IverksettingEntitet(
    iverksettingData.behandling.behandlingId,
    iverksettingData,
)
