package dk.holonet.calendar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import dk.holonet.core.HoloNetModule
import dk.holonet.core.HoloNetPlugin
import dk.holonet.core.ModuleConfiguration
import dk.holonet.core.asInt
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

            println(state.value)
        }

        override fun configure(configuration: ModuleConfiguration?) {
            super.configure(configuration)

            var type: CalendarViewModel.Type = CalendarViewModel.Type.ALL_MEAT
            var sentences: Int = 1

            configuration?.config?.let { props ->
                props["type"]?.let { type = CalendarViewModel.Type.fromValue(it.asString()) }
                props["sentences"]?.let { sentences = it.asInt() }
            }

            viewModel.fetch(type, sentences)
        }
    }
}


