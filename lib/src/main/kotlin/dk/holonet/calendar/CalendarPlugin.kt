package dk.holonet.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dk.holonet.core.HoloNetModule
import dk.holonet.core.HoloNetPlugin
import dk.holonet.core.ModuleConfiguration
import dk.holonet.core.asInt
import dk.holonet.core.asString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.pf4j.Extension
import org.pf4j.PluginWrapper

class CalendarPlugin(wrapper: PluginWrapper) : HoloNetPlugin(wrapper), KoinComponent {

    private val module = module {
        viewModel { CalendarViewModel(get()) }
    }

    private val viewModel: CalendarViewModel by inject()

    override fun start() {
        super.start()
        loadDependencies(module)
    }

    override fun stop() {
        super.stop()
        viewModel.stopAutoRefresh()
    }

    @Extension
    class CalendarModule() : HoloNetModule() {

        private val viewModel: CalendarViewModel by inject()

        @Composable
        override fun render() {
            val events = viewModel.events.collectAsState()

            Column(
                modifier = Modifier.wrapContentWidth(Alignment.Start)
            ) {
                Text("Calendar")

                HorizontalDivider(
                    modifier = Modifier
                        .wrapContentWidth()
                        .padding(vertical = 4.dp)
                )

                events.value.forEach { event ->
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp, alignment = Alignment.Start)
                    ) {
                        Text(event.startDate.toPrettyString())

                        Text(event.summary)
                    }
                }

            }

        }

        override fun configure(configuration: ModuleConfiguration?) {
            super.configure(configuration)

            var url: String? = null
            var refreshInterval = 300 // Default to 300 seconds (5 minutes)
            var maxEvents = 5 // Default to 5 events

            configuration?.config?.let { props ->
                props["url"]?.let { url = it.asString() }
                props["refreshInterval"]?.let { refreshInterval = it.asInt() }
                props["maxEvents"]?.let { maxEvents = it.asInt() }
            }

            url?.let { url ->
                viewModel.startAutoRefresh(url, refreshInterval, maxEvents)
            } ?: println("No URL provided")
        }
    }
}


