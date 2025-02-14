package no.nav.dagpenger.iverksett.infrastruktur.configuration

import io.getunleash.DefaultUnleash
import io.getunleash.UnleashContext
import io.getunleash.UnleashContextProvider
import io.getunleash.util.UnleashConfig
import no.nav.dagpenger.iverksett.infrastruktur.featuretoggle.ByEnvironmentStrategy
import no.nav.dagpenger.iverksett.infrastruktur.featuretoggle.FeatureToggleService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import java.net.URI

@ConfigurationProperties("funksjonsbrytere")
class FeatureToggleConfig(
    private val enabled: Boolean,
    private val unleash: Unleash,
) {

    data class Unleash(
        val uri: URI,
        val apiKey: String,
        val environment: String,
        val applicationName: String,
    )

    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    @Bean
    fun featureToggle(): FeatureToggleService =
        if (enabled) {
            log.info("Funksjonsbryter-funksjonalitet er skrudd PÅ")
            lagUnleashFeatureToggleService()
        } else {
            log.warn(
                "Funksjonsbryter-funksjonalitet er skrudd AV. " +
                    "Gir standardoppførsel for alle funksjonsbrytere, dvs 'false'",
            )
            lagDummyFeatureToggleService()
        }

    private fun lagUnleashFeatureToggleService(): FeatureToggleService {
        val unleash = DefaultUnleash(
            UnleashConfig.builder()
                .appName(unleash.applicationName)
                .unleashAPI("${unleash.uri}/api/")
                .apiKey(unleash.apiKey)
                .unleashContextProvider(lagUnleashContextProvider())
                .build(),
            ByEnvironmentStrategy(),
        )

        return object : FeatureToggleService {
            override fun isEnabled(toggleId: String, defaultValue: Boolean): Boolean {
                return unleash.isEnabled(toggleId, defaultValue)
            }
        }
    }

    private fun lagUnleashContextProvider(): UnleashContextProvider {
        return UnleashContextProvider {
            UnleashContext.builder()
                .environment(unleash.environment)
                .appName(unleash.applicationName)
                .build()
        }
    }

    private fun lagDummyFeatureToggleService(): FeatureToggleService {
        return object : FeatureToggleService {
            override fun isEnabled(toggleId: String, defaultValue: Boolean): Boolean {
                if (unleash.environment == "local") {
                    return System.getenv(toggleId)?.toBoolean() ?: true
                }
                return defaultValue
            }
        }
    }

    companion object {
        const val STOPP_IVERKSETTING = "dp-iverksett.stopp-iverksetting"
        const val TILGANGSKONTROLL = "dp-iverksett.tilgangskontroll"
    }
}
