package no.nav.dagpenger.iverksett.infrastruktur.advice

import org.springframework.http.HttpStatus

data class ApiFeil(val feil: String, val httpStatus: HttpStatus) : RuntimeException()
