package no.nav.dagpenger.iverksett.vedtak

import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.dagpenger.iverksett.arbeidsoppfolging.SendVedtakTilArbeidsoppfølgingTask
import no.nav.dagpenger.iverksett.infrastruktur.transformer.toDomain
import no.nav.dagpenger.iverksett.iverksetting.IverksettingRepository
import no.nav.dagpenger.iverksett.lagIverksett
import no.nav.dagpenger.iverksett.repository.findByIdOrThrow
import no.nav.dagpenger.iverksett.util.opprettIverksettDto
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import no.nav.familie.kontrakter.felles.ef.StønadType as EksternStønadType

internal class PubliserVedtakTilKafkaTaskTest {

    private val taskService = mockk<TaskService>()
    private val iverksettingRepository = mockk<IverksettingRepository>()
    private val vedtakKafkaProducer = mockk<VedtakKafkaProducer>()

    private val task = PubliserVedtakTilKafkaTask(taskService, iverksettingRepository, vedtakKafkaProducer)

    private val taskSlot = CapturingSlot<Task>()

    @BeforeEach
    internal fun setUp() {
        every { iverksettingRepository.findByIdOrThrow(any()) }
            .returns(lagIverksett(opprettIverksettDto(behandlingId = UUID.randomUUID()).toDomain()))
        every { taskService.save(capture(taskSlot)) } answers { firstArg() }
    }

    @Test
    internal fun `doTask - ska publisere vedtak til kafka`() {
        every { vedtakKafkaProducer.sendVedtak(any()) } just runs
        task.doTask(lagTask())

        verify(exactly = 1) { vedtakKafkaProducer.sendVedtak(any()) }
    }

    @Test
    internal fun `onCompletion - skal opprette neste task`() {
        task.onCompletion(lagTask())

        verify(exactly = 1) { taskService.save(any()) }
        assertThat(taskSlot.captured.type).isEqualTo(SendVedtakTilArbeidsoppfølgingTask.TYPE)
    }

    @Test
    internal fun `skal mappe stønadstyper`() {
        assertThat(EksternStønadType.values().map { it.name }).isEqualTo(StønadType.values().map { it.name })
    }

    private fun lagTask() = Task(PubliserVedtakTilKafkaTask.TYPE, UUID.randomUUID().toString())
}
