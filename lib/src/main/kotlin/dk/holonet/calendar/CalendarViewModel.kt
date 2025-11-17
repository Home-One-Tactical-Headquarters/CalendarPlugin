package dk.holonet.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.component.VEvent
import java.io.StringReader
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class CalendarViewModel(
    private val httpClient: HttpClient
): ViewModel() {

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events = _events.asStateFlow()

    private var refreshJob: Job? = null

    fun startAutoRefresh(calendarUrl: String, refreshIntervalSeconds: Int, maxEvents: Int) {
        refreshJob = viewModelScope.launch {
            while (true) {
                fetch(calendarUrl, maxEvents)
                delay(refreshIntervalSeconds * 1000L)
            }
        }
    }

    fun stopAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = null
    }

    suspend fun fetch(calendarUrl: String, maxEvents: Int) {
        val response = httpClient.get(calendarUrl)
        val iCalData = response.bodyAsText()

        val reader = StringReader(iCalData)
        val calendar = CalendarBuilder().build(reader)

        val now = Instant.now()

        // TODO: Use calendar name. Maybe do it per calendar if supporting multiple calendars
//        val calendarName = calendar.getProperty<Property>("X-WR-CALNAME").get().value ?: "Calendar"

        val upcomingEvents = calendar.getComponents<VEvent>("VEVENT")
            .mapNotNull { event ->
                val startDate = event.getDateTimeStart<OffsetDateTime>() ?: return@mapNotNull null
                val endDate = event.getDateTimeEnd<OffsetDateTime>()

                val startDateValue = startDate.value
                val endDateValue = endDate.value

                val startEventTime = parseDateTime(startDateValue)
                val endEventTime = parseDateTime(endDateValue)

                if (!startEventTime.isAfter(now)) return@mapNotNull null

                Event(
                    summary = event.summary.value,
                    startDate = startEventTime,
                    endDate = endEventTime,
                    timeUntil = startEventTime.epochSecond - now.epochSecond,
                    isAllDay = !startDateValue.contains("T")
                )
            }
            .sortedBy { event -> event.startDate }
            .take(maxEvents)

        _events.value = upcomingEvents
    }

    private fun parseDateTime(dateValue: String): Instant {
        return if (dateValue.contains("T") && dateValue.endsWith("Z")) {
            // Parse as UTC using the custom formatter, then convert to Instant
            java.time.ZonedDateTime.parse(dateValue, dateTimeFormatter.withZone(java.time.ZoneOffset.UTC)).toInstant()
        } else if (dateValue.contains("T")) {
            LocalDate.parse(dateValue, dateFormatter)
                .atStartOfDay(systemZone)
                .toInstant()
        } else {
            LocalDate.parse(dateValue, dateFormatter)
                .atStartOfDay(systemZone)
                .toInstant()
        }
    }

    data class Event(
        val summary: String,
        val startDate: Instant,
        val endDate: Instant,
        val timeUntil: Long,
        val isAllDay: Boolean = false
    )
}

private val systemZone = java.time.ZoneId.systemDefault()

fun Long.toReadableDuration(): String {
    val totalMinutes = this / 60
    val days = totalMinutes / (60 * 24)
    val hours = (totalMinutes / 60) % 24
    val minutes = totalMinutes % 60

    return when {
        days > 0 -> "${days}d"
        hours > 0 -> "${hours}h, ${minutes}m"
        else -> "${minutes}m"
    }
}

fun Instant.toPrettyString(isAllDay: Boolean): String {
    return if (isAllDay) prettyDateFormatter.format(this.atZone(systemZone)) + " All day"
    else prettyDateTimeFormatter.format(this.atZone(systemZone))
}

private val dateTimeFormatter = DateTimeFormatter
    .ofPattern("yyyyMMdd'T'HHmmss'Z'")
    .withZone(ZoneOffset.UTC)

private val dateFormatter = DateTimeFormatter
    .ofPattern("yyyyMMdd")
    .withZone(systemZone)

private val prettyDateTimeFormatter = DateTimeFormatter
    .ofPattern("dd/MM HH:mm")
    .withZone(systemZone)

private val prettyDateFormatter = DateTimeFormatter
    .ofPattern("dd/MM")
    .withZone(systemZone)