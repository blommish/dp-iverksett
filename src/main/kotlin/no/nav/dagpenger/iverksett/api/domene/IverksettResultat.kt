package no.nav.dagpenger.iverksett.api.domene

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDateTime
import java.util.UUID
import no.nav.dagpenger.kontrakter.oppdrag.OppdragStatus
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column

data class IverksettResultat(
    @Id
    val behandlingId: UUID,
    @Column("tilkjentytelseforutbetaling")
    val tilkjentYtelseForUtbetaling: TilkjentYtelse? = null,
    @Column("oppdragresultat")
    val oppdragResultat: OppdragResultat? = null,
    @Column("journalpostresultat")
    val journalpostResultat: JsonNode? = null,
    @Column("vedtaksbrevresultat")
    val vedtaksbrevResultat: JsonNode? = null,
    @Column("tilbakekrevingresultat")
    val tilbakekrevingResultat: JsonNode? = null,
)

data class OppdragResultat(val oppdragStatus: OppdragStatus, val oppdragStatusOppdatert: LocalDateTime = LocalDateTime.now())
