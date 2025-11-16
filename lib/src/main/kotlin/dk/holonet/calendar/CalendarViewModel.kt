package dk.holonet.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
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
import kotlin.text.format

class CalendarViewModel(
    private val httpClient: HttpClient
): ViewModel() {

    val _state = MutableStateFlow<List<Event>>(emptyList())
    val state = _state.asStateFlow()

    fun fetch(calendarUrl: String) {
        println("System zone: $systemZone")
        viewModelScope.launch {
            val response = httpClient.get(calendarUrl)
            val icalData = response.bodyAsText()

            val reader = StringReader(icalData)
            val calendar = CalendarBuilder().build(reader)

            val now = Instant.now()

            val upcomingEvents = calendar.getComponents<VEvent>("VEVENT")
                .mapNotNull { event ->
                    val startDate = event.getDateTimeStart<OffsetDateTime>() ?: return@mapNotNull null
                    val endDate = event.getDateTimeEnd<OffsetDateTime>()

                    val startDateValue = startDate.value
                    val endDateValue = endDate.value

                    val startEventTime = parseDateTime(startDateValue)
                    val endEventTime = parseDateTime(endDateValue)

                    if (startEventTime.isAfter(now)) {
                        Event(
                            summary = event.summary.value,
                            startDate = startEventTime,
                            endDate = endEventTime
                        )
                    } else null
                }
                .sortedBy { event -> event.startDate }
                .take(5)

            _state.value = upcomingEvents
        }
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
        val endDate: Instant
    )
}

private val systemZone = java.time.ZoneId.systemDefault()

fun Instant.toPrettyString(): String {
    return prettyFormatter.format(this.atZone(systemZone))
}

private val dateTimeFormatter = DateTimeFormatter
    .ofPattern("yyyyMMdd'T'HHmmss'Z'")
    .withZone(ZoneOffset.UTC)

private val dateFormatter = DateTimeFormatter
    .ofPattern("yyyyMMdd")
    .withZone(systemZone)

private val prettyFormatter = DateTimeFormatter
    .ofPattern("dd/MM HH:mm")
    .withZone(systemZone)