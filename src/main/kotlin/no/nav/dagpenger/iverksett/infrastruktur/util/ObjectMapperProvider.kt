package no.nav.dagpenger.iverksett.infrastruktur.util

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import java.time.ZoneId
import java.time.ZonedDateTime

object ObjectMapperProvider {

    val objectMapper: ObjectMapper =
        no.nav.dagpenger.kontrakter.felles.objectMapper
            .registerModule(
                SimpleModule().addDeserializer(
                    ZonedDateTime::class.java,
                    ZonedDateTimeDeserializer(),
                ),
            )

    /**
     * Vi ønsker å defaulte til Europe/Oslo ved deserialisering, i stedet for automatisk ZoneID-justering til UTC
     */
    private class ZonedDateTimeDeserializer : JsonDeserializer<ZonedDateTime>() {

        override fun deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): ZonedDateTime {
            val string = jsonParser.text
            return ZonedDateTime.parse(string).withZoneSameInstant(ZoneId.of("Europe/Oslo"))
        }
    }
}
