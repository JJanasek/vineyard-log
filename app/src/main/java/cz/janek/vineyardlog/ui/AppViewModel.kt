package cz.janek.vineyardlog.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.janek.vineyardlog.AppContainer
import cz.janek.vineyardlog.appContainer

/** Create/retrieve a ViewModel that receives the app container. Use `key` for id-scoped screens. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(key: String? = null, crossinline factory: (AppContainer) -> VM): VM {
    val container = LocalContext.current.appContainer
    return viewModel(key = key) { factory(container) }
}
