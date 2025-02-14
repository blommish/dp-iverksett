package no.nav.dagpenger.iverksett.konsumenter.økonomi

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.dagpenger.iverksett.konsumenter.økonomi.grensesnitt.GrensesnittavstemmingPayload
import no.nav.dagpenger.iverksett.konsumenter.økonomi.grensesnitt.GrensesnittavstemmingTask
import no.nav.dagpenger.kontrakter.felles.StønadTypeDagpenger
import no.nav.dagpenger.kontrakter.felles.objectMapper
import no.nav.dagpenger.kontrakter.oppdrag.GrensesnittavstemmingRequest
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class GrensesnittavstemmingTaskTest {

    private val oppdragClient = mockk<OppdragClient>()
    val taskService = mockk<TaskService>()
    private val grensesnittavstemmingTask = GrensesnittavstemmingTask(oppdragClient, taskService)

    @Test
    fun `doTask skal kalle oppdragClient med fradato fra payload og dato for triggerTid som parametere`() {
        val grensesnittavstemmingRequestSlot = slot<GrensesnittavstemmingRequest>()

        every { oppdragClient.grensesnittavstemming(any()) }.returns("ok")

        grensesnittavstemmingTask.doTask(
            Task(
                type = GrensesnittavstemmingTask.TYPE,
                payload = payload,
                triggerTid = LocalDateTime.of(2018, 4, 19, 8, 0),
            ),
        )
        verify(exactly = 1) { oppdragClient.grensesnittavstemming(capture(grensesnittavstemmingRequestSlot)) }
        val capturedGrensesnittRequest = grensesnittavstemmingRequestSlot.captured
        assertThat(capturedGrensesnittRequest.fra).isEqualTo(LocalDate.of(2018, 4, 18).atStartOfDay())
        assertThat(capturedGrensesnittRequest.til).isEqualTo(LocalDate.of(2018, 4, 19).atStartOfDay())
        assertThat(capturedGrensesnittRequest.fagsystem).isEqualTo(StønadTypeDagpenger.DAGPENGER_ARBEIDSSOKER_ORDINAER.tilFagsystem())
    }

    @Test
    fun `onCompletion skal opprette ny grensesnittavstemmingTask med dato for forrige triggerTid som payload`() {
        val triggeTid = LocalDateTime.of(2018, 4, 19, 8, 0)
        val slot = slot<Task>()
        every { taskService.save(capture(slot)) } returns mockk()

        grensesnittavstemmingTask.onCompletion(
            Task(
                type = GrensesnittavstemmingTask.TYPE,
                payload = payload,
                triggerTid = triggeTid,
            ),
        )
        val forventetPayload =
            objectMapper.writeValueAsString(
                GrensesnittavstemmingPayload(
                    fraDato = LocalDate.of(2018, 4, 19),
                    stønadstype = StønadTypeDagpenger.DAGPENGER_ARBEIDSSOKER_ORDINAER,
                ),
            )
        assertThat(slot.captured.payload).isEqualTo(forventetPayload)
    }

    companion object {

        val payload: String =
            objectMapper.writeValueAsString(
                GrensesnittavstemmingPayload(
                    fraDato = LocalDate.of(2018, 4, 18),
                    stønadstype = StønadTypeDagpenger.DAGPENGER_ARBEIDSSOKER_ORDINAER,
                ),
            )
    }
}
