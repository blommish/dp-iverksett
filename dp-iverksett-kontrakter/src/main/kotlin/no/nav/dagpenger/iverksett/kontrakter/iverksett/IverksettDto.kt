package no.nav.dagpenger.iverksett.kontrakter.iverksett

import no.nav.dagpenger.iverksett.kontrakter.felles.AvslagÅrsak
import no.nav.dagpenger.iverksett.kontrakter.felles.BehandlingType
import no.nav.dagpenger.iverksett.kontrakter.felles.BehandlingÅrsak
import no.nav.dagpenger.iverksett.kontrakter.felles.Månedsperiode
import no.nav.dagpenger.iverksett.kontrakter.felles.OpphørÅrsak
import no.nav.dagpenger.iverksett.kontrakter.felles.RegelId
import no.nav.dagpenger.iverksett.kontrakter.felles.StønadType
import no.nav.dagpenger.iverksett.kontrakter.felles.SvarId
import no.nav.dagpenger.iverksett.kontrakter.felles.Vedtaksresultat
import no.nav.dagpenger.iverksett.kontrakter.felles.VilkårType
import no.nav.dagpenger.iverksett.kontrakter.felles.Vilkårsresultat
import no.nav.dagpenger.iverksett.kontrakter.tilbakekreving.Tilbakekrevingsvalg
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

data class IverksettOvergangsstønadDto(
    val fagsak: FagsakdetaljerDto,
    val behandling: BehandlingsdetaljerDto,
    val søker: SøkerDto,
    val vedtak: VedtaksdetaljerOvergangsstønadDto,
)


data class SøkerDto(
    val personIdent: String,
    val barn: List<BarnDto> = emptyList(),
    val tilhørendeEnhet: String,
    val adressebeskyttelse: AdressebeskyttelseGradering? = null,
)

data class FagsakdetaljerDto(
    val fagsakId: UUID,
    val eksternId: Long,
    val stønadstype: StønadType,
)

data class BehandlingsdetaljerDto(
    val behandlingId: UUID,
    val forrigeBehandlingId: UUID? = null,
    val eksternId: Long,
    val behandlingType: BehandlingType,
    val behandlingÅrsak: BehandlingÅrsak,
    val vilkårsvurderinger: List<VilkårsvurderingDto> = emptyList(),
    val aktivitetspliktInntrefferDato: LocalDate? = null,
    val kravMottatt: LocalDate? = null,
    val årsakRevurdering: ÅrsakRevurderingDto? = null,
)

sealed class VedtaksdetaljerDto {

    abstract val resultat: Vedtaksresultat
    abstract val vedtakstidspunkt: LocalDateTime
    abstract val opphørÅrsak: OpphørÅrsak?
    abstract val saksbehandlerId: String
    abstract val beslutterId: String
    abstract val tilkjentYtelse: TilkjentYtelseDto?
    abstract val vedtaksperioder: List<VedtaksperiodeDto>
    abstract val tilbakekreving: TilbakekrevingDto?
    abstract val brevmottakere: List<Brevmottaker>
    abstract val avslagÅrsak: AvslagÅrsak?
}

data class VedtaksdetaljerOvergangsstønadDto(
    override val resultat: Vedtaksresultat,
    override val vedtakstidspunkt: LocalDateTime,
    override val opphørÅrsak: OpphørÅrsak?,
    override val saksbehandlerId: String,
    override val beslutterId: String,
    override val tilkjentYtelse: TilkjentYtelseDto?,
    override val vedtaksperioder: List<VedtaksperiodeOvergangsstønadDto> = emptyList(),
    override val tilbakekreving: TilbakekrevingDto? = null,
    override val brevmottakere: List<Brevmottaker> = emptyList(),
    override val avslagÅrsak: AvslagÅrsak? = null,
) : VedtaksdetaljerDto()


data class VilkårsvurderingDto(
    val vilkårType: VilkårType,
    val resultat: Vilkårsresultat,
    val delvilkårsvurderinger: List<DelvilkårsvurderingDto> = emptyList(),
)

data class DelvilkårsvurderingDto(
    val resultat: Vilkårsresultat,
    val vurderinger: List<VurderingDto> = emptyList(),
)

data class VurderingDto(
    val regelId: RegelId,
    val svar: SvarId? = null,
    val begrunnelse: String? = null,
)

sealed class VedtaksperiodeDto

data class VedtaksperiodeOvergangsstønadDto(
    val periode: Månedsperiode,
    val aktivitet: AktivitetType,
    val periodeType: VedtaksperiodeType,
) : VedtaksperiodeDto()


data class TilbakekrevingDto(
    val tilbakekrevingsvalg: Tilbakekrevingsvalg,
    val tilbakekrevingMedVarsel: TilbakekrevingMedVarselDto?,
)

data class PeriodeMedBeløpDto(
    val periode: Månedsperiode,
    val beløp: Int,
)

data class TilbakekrevingMedVarselDto(
    val varseltekst: String,
    val sumFeilutbetaling: BigDecimal? = null, // Hentes fra simulering hvis det mangler
    @Deprecated("Bruk fellesperioder!", ReplaceWith("fellesperioder"))
    val perioder: List<no.nav.dagpenger.iverksett.kontrakter.tilbakekreving.Periode>? = null,
    val fellesperioder: List<Månedsperiode> = perioder?.map { Månedsperiode(it.fom, it.tom) }
        ?: error("Perioder eller fellesperioder må ha verdi!"),
) // Hentes fra simulering hvis det mangler

enum class AdressebeskyttelseGradering {
    STRENGT_FORTROLIG,
    STRENGT_FORTROLIG_UTLAND,
    FORTROLIG,
    UGRADERT,
}

enum class IverksettStatus {
    SENDT_TIL_OPPDRAG,
    FEILET_MOT_OPPDRAG,
    OK_MOT_OPPDRAG,
    JOURNALFØRT,
    OK,
    IKKE_PÅBEGYNT,
}

enum class VedtaksperiodeType {
    MIGRERING,
    FORLENGELSE,
    HOVEDPERIODE,
    PERIODE_FØR_FØDSEL,
    UTVIDELSE,
    SANKSJON,
    NY_PERIODE_FOR_NYTT_BARN,
}

enum class AktivitetType {
    MIGRERING,
    IKKE_AKTIVITETSPLIKT,
    BARN_UNDER_ETT_ÅR,
    FORSØRGER_I_ARBEID,
    FORSØRGER_I_UTDANNING,
    FORSØRGER_REELL_ARBEIDSSØKER,
    FORSØRGER_ETABLERER_VIRKSOMHET,
    BARNET_SÆRLIG_TILSYNSKREVENDE,
    FORSØRGER_MANGLER_TILSYNSORDNING,
    FORSØRGER_ER_SYK,
    BARNET_ER_SYKT,
    UTVIDELSE_FORSØRGER_I_UTDANNING,
    UTVIDELSE_BARNET_SÆRLIG_TILSYNSKREVENDE,
    FORLENGELSE_MIDLERTIDIG_SYKDOM,
    FORLENGELSE_STØNAD_PÅVENTE_ARBEID,
    FORLENGELSE_STØNAD_PÅVENTE_ARBEID_REELL_ARBEIDSSØKER,
    FORLENGELSE_STØNAD_PÅVENTE_OPPSTART_KVALIFISERINGSPROGRAM,
    FORLENGELSE_STØNAD_PÅVENTE_TILSYNSORDNING,
    FORLENGELSE_STØNAD_PÅVENTE_UTDANNING,
    FORLENGELSE_STØNAD_UT_SKOLEÅRET,
}

// Brukes også fra [FrittståendeBrevDto]
data class Brevmottaker(
    val ident: String,
    val navn: String,
    val mottakerRolle: MottakerRolle,
    val identType: IdentType,
) {

    enum class MottakerRolle {
        BRUKER,
        VERGE,
        FULLMEKTIG,
    }

    enum class IdentType {
        PERSONIDENT,
        ORGANISASJONSNUMMER,
    }
}
