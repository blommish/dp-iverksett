package no.nav.dagpenger.iverksett.api.tilstand

import java.util.UUID
import no.nav.dagpenger.iverksett.api.domene.Iverksettingsresultat
import no.nav.dagpenger.iverksett.api.domene.OppdragResultat
import no.nav.dagpenger.iverksett.api.domene.TilkjentYtelse
import no.nav.dagpenger.iverksett.infrastruktur.repository.findByIdOrThrow
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class IverksettingsresultatService(private val iverksettingsresultatRepository: IverksettingsresultatRepository) {

    fun opprettTomtResultat(behandlingId: UUID) {
        iverksettingsresultatRepository.insert(Iverksettingsresultat(behandlingId))
    }

    fun oppdaterTilkjentYtelseForUtbetaling(
        behandlingId: UUID,
        tilkjentYtelseForUtbetaling: TilkjentYtelse,
        oppsplittetOppdrag: Boolean = false // todo fjern default
    ) {
        val iverksettResultat = iverksettingsresultatRepository.findByIdOrThrow(behandlingId)
        iverksettingsresultatRepository.update(
            iverksettResultat.copy(
                tilkjentYtelseForUtbetaling = tilkjentYtelseForUtbetaling,
                oppsplittetOppdrag = oppsplittetOppdrag,
                sisteUtbetalingsdato = tilkjentYtelseForUtbetaling.andelerTilkjentYtelse.maxOfOrNull { it.periode.tom }
            )
        )
    }

    // @Transactional(propagation = Propagation.REQUIRES_NEW) Denne trenger ikke å være i en ny transaksjon ??
    fun oppdaterOppdragResultat(behandlingId: UUID, oppdragResultat: OppdragResultat?) {
        val iverksettResultat = iverksettingsresultatRepository.findByIdOrThrow(behandlingId)
        iverksettingsresultatRepository.update(iverksettResultat.copy(oppdragResultat = oppdragResultat))
    }

    fun hentTilkjentYtelse(behandlingId: UUID): TilkjentYtelse? {
        return iverksettingsresultatRepository.findByIdOrNull(behandlingId)?.tilkjentYtelseForUtbetaling
    }

    fun hentTilkjentYtelse(behandlingId: Set<UUID>): Map<UUID, TilkjentYtelse> {
        val iverksettResultater = iverksettingsresultatRepository.findAllById(behandlingId)
        val tilkjenteYtelser = iverksettResultater.filter { it.tilkjentYtelseForUtbetaling != null }
            .associate { it.behandlingId to it.tilkjentYtelseForUtbetaling!! }
        if (behandlingId.size > tilkjenteYtelser.size) {
            error("Finner ikke tilkjent ytelse til behandlingIder=${behandlingId.minus(tilkjenteYtelser.keys)}}")
        }
        return tilkjenteYtelser
    }

    // Ettersom man alltid oppretter et resultat burde ikke denne trenge å være nullable
    fun hentIverksettResultat(behandlingId: UUID): Iverksettingsresultat? {
        return iverksettingsresultatRepository.findByIdOrNull(behandlingId)
    }
}
