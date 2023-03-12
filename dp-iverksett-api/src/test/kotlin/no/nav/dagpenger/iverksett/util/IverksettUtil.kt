package no.nav.dagpenger.iverksett.util

import no.nav.dagpenger.iverksett.brev.domain.Brevmottakere
import no.nav.dagpenger.iverksett.iverksetting.domene.IverksettData
import no.nav.dagpenger.iverksett.iverksetting.domene.IverksettOvergangsstønad
import no.nav.dagpenger.iverksett.iverksetting.domene.TilkjentYtelse
import no.nav.dagpenger.iverksett.iverksetting.domene.Vedtaksdetaljer
import no.nav.dagpenger.iverksett.iverksetting.domene.VedtaksdetaljerOvergangsstønad

fun IverksettData.copy(vedtak: Vedtaksdetaljer): IverksettData {
    return when (this) {
        is IverksettOvergangsstønad -> this.copy(vedtak = vedtak as VedtaksdetaljerOvergangsstønad)
        else -> error("Ingen støtte ennå")
    }
}

fun Vedtaksdetaljer.copy(brevmottakere: Brevmottakere): Vedtaksdetaljer {
    return when (this) {
        is VedtaksdetaljerOvergangsstønad -> this.copy(brevmottakere = brevmottakere)
        else -> error("Ingen støtte ennå")
    }
}

fun Vedtaksdetaljer.copy(tilkjentYtelse: TilkjentYtelse): Vedtaksdetaljer {
    return when (this) {
        is VedtaksdetaljerOvergangsstønad -> this.copy(tilkjentYtelse = tilkjentYtelse)
        else -> error("Ingen støtte ennå")
    }
}
