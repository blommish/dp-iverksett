package no.nav.dagpenger.iverksett.arena

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

@JacksonXmlRootElement(
    localName = "vedtakHendelser",
    namespace = "http://nav.no/melding/virksomhet/vedtakHendelser/v1/vedtakHendelser",
)
data class VedtakHendelser(
    val aktoerID: String,
    val avslutningsstatus: String,
    val behandlingstema: String,
    val hendelsesprodusentREF: String,
    val applikasjonSakREF: String,
    val hendelsesTidspunkt: String,
)
