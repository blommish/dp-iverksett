package no.nav.dagpenger.iverksett.api.domene

import no.nav.dagpenger.kontrakter.oppdrag.OppdragStatus
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import java.time.LocalDateTime
import java.util.*

data class Iverksettingsresultat(
    @Id
    val behandlingId: UUID,
    @Column("tilkjentytelseforutbetaling")
    val tilkjentYtelseForUtbetaling: TilkjentYtelse? = null,
    @Column("oppdragresultat")
    val oppdragResultat: OppdragResultat? = null,
)

data class OppdragResultat(
    val oppdragStatus: OppdragStatus,
    val oppdragStatusOppdatert: LocalDateTime = LocalDateTime.now()
)
