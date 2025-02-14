package no.nav.dagpenger.iverksett.konsumenter

import java.time.LocalDateTime
import no.nav.dagpenger.iverksett.konsumenter.økonomi.IverksettMotOppdragTask
import no.nav.dagpenger.iverksett.konsumenter.økonomi.VentePåStatusFraØkonomiTask
import no.nav.familie.prosessering.domene.Task

class TaskType(
    val type: String,
    val triggerTidAntallSekunderFrem: Long? = null,
)

fun hovedflyt() = listOf(
    TaskType(IverksettMotOppdragTask.TYPE),
    TaskType(VentePåStatusFraØkonomiTask.TYPE, 20)
)

fun TaskType.nesteHovedflytTask() = hovedflyt().zipWithNext().first { this.type == it.first.type }.second
fun Task.opprettNesteTask(): Task {
    val nesteTask = TaskType(this.type).nesteHovedflytTask()
    return lagTask(nesteTask)
}

private fun Task.lagTask(nesteTask: TaskType): Task {
    return if (nesteTask.triggerTidAntallSekunderFrem != null) {
        Task(
            type = nesteTask.type,
            payload = this.payload,
            properties = this.metadata,
        ).copy(
            triggerTid = LocalDateTime.now()
                .plusSeconds(nesteTask.triggerTidAntallSekunderFrem),
        )
    } else {
        Task(
            type = nesteTask.type,
            payload = this.payload,
            properties = this.metadata,
        )
    }
}
