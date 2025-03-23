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
import dk.holonet.core.asString
import org.koin.core.component.inject
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.pf4j.Extension
import org.pf4j.PluginWrapper

class CalendarPlugin(wrapper: PluginWrapper) : HoloNetPlugin(wrapper) {

    private val module = module {
        viewModel { CalendarViewModel(get()) }
    }

    override fun start() {
        super.start()
        loadDependencies(module)
    }

    @Extension
    class CalendarModule() : HoloNetModule() {

        private val viewModel: CalendarViewModel by inject()

        @Composable
        override fun render() {
            val state = viewModel.state.collectAsState()

            Column(
                modifier = Modifier.wrapContentWidth(Alignment.Start)
            ) {
                Text("Calendar")

                HorizontalDivider(
                    modifier = Modifier
                        .wrapContentWidth()
                        .padding(vertical = 4.dp)
                )

                state.value.forEach { event ->
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

            configuration?.config?.let { props ->
                props["url"]?.let { url = it.asString() }
            }

            url?.let { viewModel.fetch(it) } ?: println("No URL provided")
        }
    }
}


