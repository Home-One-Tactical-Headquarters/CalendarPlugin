package dk.holonet.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import dk.holonet.core.HoloNetModule
import dk.holonet.core.HoloNetPlugin
import dk.holonet.core.ModuleConfiguration
import dk.holonet.core.asBoolean
import dk.holonet.core.asInt
import dk.holonet.core.asString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.pf4j.Extension
import org.pf4j.PluginWrapper
import kotlin.math.min

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
        private var enableGradient = true
        private var maxEventsToShowTimeUntil by mutableStateOf(1)

        @Composable
        override fun render() {
            val events = viewModel.events.collectAsState()
            val rowHeights = remember { mutableStateListOf<Int>() }

            Box {
                Column(
                    modifier = Modifier.wrapContentWidth(Alignment.Start)
                ) {
                    Text("Calendar")

                    HorizontalDivider(
                        modifier = Modifier
                            .wrapContentWidth()
                            .padding(vertical = 4.dp)
                    )

                    events.value.forEachIndexed { index, event ->
                        Row(
                            modifier = Modifier
                                .padding(8.dp)
                                .onGloballyPositioned { layoutCoordinates ->
                                    if (rowHeights.size > index) {
                                        rowHeights[index] = layoutCoordinates.size.height
                                    } else {
                                        rowHeights.add(layoutCoordinates.size.height)
                                    }
                                },
                            horizontalArrangement = Arrangement.spacedBy(16.dp, alignment = Alignment.Start)
                        ) {
                            Text(text = event.startDate.toPrettyString(event.isAllDay))

                            if (index < maxEventsToShowTimeUntil) {
                                Text("${event.summary} (${event.timeUntil.toReadableDuration()})")
                            } else {
                                Text(event.summary)
                            }
                        }
                    }

                }
                
                if (!enableGradient) return@Box

                val height = rowHeights.takeLast(2).sum() * 2

                // Gradient overlay at the bottom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height.dp)
                        .align(Alignment.BottomStart)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black)
                            )
                        )
                )
            }

        }

        override fun configure(configuration: ModuleConfiguration?) {
            super.configure(configuration)

            var url: String? = null
            var refreshInterval = 300 // Default to 300 seconds (5 minutes)
            var maxEvents = 5

            configuration?.config?.let { props ->
                props["url"]?.let { url = it.asString() }
                props["refreshInterval"]?.let { refreshInterval = it.asInt() }
                props["maxEvents"]?.let { maxEvents = it.asInt() }
                props["enableGradient"]?.let { enableGradient = it.asBoolean() }
                props["maxTimeUntilEvent"]?.let { maxEventsToShowTimeUntil = min(it.asInt(), maxEvents) }
            }

            url?.let { url ->
                viewModel.startAutoRefresh(url, refreshInterval, maxEvents)
            } ?: println("No URL provided")
        }
    }
}


