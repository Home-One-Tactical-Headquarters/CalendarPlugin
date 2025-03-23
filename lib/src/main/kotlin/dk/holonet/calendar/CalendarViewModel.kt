package dk.holonet.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CalendarViewModel(
    private val httpClient: HttpClient
): ViewModel() {

    val _state = MutableStateFlow<String?>(null)
    val state = _state.asStateFlow()

    fun fetch(type: Type, sentences: Int) {
        viewModelScope.launch {
            val response = httpClient.get("https://baconipsum.com/api/") {
                parameter("type", type.value)
                parameter("sentences", sentences)
            }

            if (response.status == HttpStatusCode.OK) {
                _state.value = response.bodyAsText()
                return@launch
            }

            _state.value = "Failed to fetch data"
        }
    }

    enum class Type(val value: String) {
        MEAT_AND_FILLER("meat-and-filler"),
        ALL_MEAT("all-meat");

        companion object {
            fun fromValue(value: String): Type {
                return entries.firstOrNull() { it.value == value } ?: ALL_MEAT
            }
        }
    }

}