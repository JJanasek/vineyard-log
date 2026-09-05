package cz.janek.vineyardlog.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cz.janek.vineyardlog.data.model.Domain
import cz.janek.vineyardlog.data.model.EntryType
import cz.janek.vineyardlog.ui.batches.BatchDetailScreen
import cz.janek.vineyardlog.ui.batches.BatchEditScreen
import cz.janek.vineyardlog.ui.batches.BatchesScreen
import cz.janek.vineyardlog.ui.blocks.BlockDetailScreen
import cz.janek.vineyardlog.ui.blocks.BlockEditScreen
import cz.janek.vineyardlog.ui.blocks.BlocksScreen
import cz.janek.vineyardlog.ui.entry.EntryDetailScreen
import cz.janek.vineyardlog.ui.entry.EntryEditScreen
import cz.janek.vineyardlog.ui.products.ProductEditScreen
import cz.janek.vineyardlog.ui.products.ProductsScreen
import cz.janek.vineyardlog.ui.settings.SettingsScreen
import cz.janek.vineyardlog.ui.timeline.TimelineScreen
import cz.janek.vineyardlog.ui.weather.WeatherScreen

private fun Long.orNull(): Long? = if (this < 0) null else this

@Composable
fun AppRoot(sharedUrl: String? = null, onSharedUrlConsumed: () -> Unit = {}) {
    val navController = rememberNavController()
    LaunchedEffect(sharedUrl) {
        if (!sharedUrl.isNullOrBlank()) {
            navController.navigate(Routes.productEdit(url = sharedUrl))
            onSharedUrlConsumed()
        }
    }
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBar = Tab.entries.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { navController.navigateToTab(tab) },
                            icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelRes)) },
                            label = { Text(stringResource(tab.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Tab.LOG.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Tab.LOG.route) {
                TimelineScreen(
                    onOpenEntry = { navController.navigate(Routes.entry(it)) },
                    onNewEntry = { domain -> navController.navigate(Routes.entryEdit(domain = domain)) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                )
            }
            composable(Tab.VINEYARD.route) {
                BlocksScreen(
                    onOpenBlock = { navController.navigate(Routes.block(it)) },
                    onNewBlock = { navController.navigate(Routes.blockEdit()) },
                )
            }
            composable(Tab.CELLAR.route) {
                BatchesScreen(
                    onOpenBatch = { navController.navigate(Routes.batch(it)) },
                    onNewBatch = { navController.navigate(Routes.batchEdit()) },
                )
            }
            composable(Tab.WEATHER.route) {
                WeatherScreen()
            }
            composable(Tab.PRODUCTS.route) {
                ProductsScreen(
                    onOpenProduct = { navController.navigate(Routes.productEdit(it)) },
                    onNewProduct = { navController.navigate(Routes.productEdit()) },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.BLOCK, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                val id = entry.arguments?.getLong("id") ?: return@composable
                BlockDetailScreen(
                    blockId = id,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(Routes.blockEdit(id)) },
                    onOpenEntry = { navController.navigate(Routes.entry(it)) },
                    onNewEntry = { type: EntryType? ->
                        navController.navigate(Routes.entryEdit(domain = Domain.VINEYARD, blockId = id, type = type))
                    },
                    onDeleted = { navController.popBackStack() },
                )
            }
            composable(
                Routes.BLOCK_EDIT,
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L }),
            ) { entry ->
                val id = (entry.arguments?.getLong("id") ?: -1L).orNull()
                BlockEditScreen(blockId = id, onDone = { navController.popBackStack() })
            }

            composable(Routes.BATCH, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                val id = entry.arguments?.getLong("id") ?: return@composable
                BatchDetailScreen(
                    batchId = id,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(Routes.batchEdit(id)) },
                    onOpenEntry = { navController.navigate(Routes.entry(it)) },
                    onNewEntry = { type: EntryType? ->
                        navController.navigate(Routes.entryEdit(domain = Domain.CELLAR, batchId = id, type = type))
                    },
                    onDeleted = { navController.popBackStack() },
                )
            }
            composable(
                Routes.BATCH_EDIT,
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L }),
            ) { entry ->
                val id = (entry.arguments?.getLong("id") ?: -1L).orNull()
                BatchEditScreen(batchId = id, onDone = { navController.popBackStack() })
            }

            composable(
                Routes.PRODUCT_EDIT,
                arguments = listOf(
                    navArgument("id") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("url") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { entry ->
                val id = (entry.arguments?.getLong("id") ?: -1L).orNull()
                val url = entry.arguments?.getString("url")?.takeIf { it.isNotBlank() }
                ProductEditScreen(productId = id, initialUrl = url, onDone = { navController.popBackStack() })
            }

            composable(Routes.ENTRY, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                val id = entry.arguments?.getLong("id") ?: return@composable
                EntryDetailScreen(
                    entryId = id,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(Routes.entryEdit(id = id)) },
                    onDeleted = { navController.popBackStack() },
                )
            }
            composable(
                Routes.ENTRY_EDIT,
                arguments = listOf(
                    navArgument("id") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("domain") { type = NavType.StringType; defaultValue = Domain.VINEYARD.name },
                    navArgument("blockId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("batchId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("type") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { entry ->
                val args = entry.arguments
                val id = (args?.getLong("id") ?: -1L).orNull()
                val domain = runCatching { Domain.valueOf(args?.getString("domain") ?: "") }.getOrDefault(Domain.VINEYARD)
                val blockId = (args?.getLong("blockId") ?: -1L).orNull()
                val batchId = (args?.getLong("batchId") ?: -1L).orNull()
                val type = args?.getString("type")?.takeIf { it.isNotBlank() }?.let { runCatching { EntryType.valueOf(it) }.getOrNull() }
                EntryEditScreen(
                    entryId = id,
                    initialDomain = domain,
                    initialBlockId = blockId,
                    initialBatchId = batchId,
                    initialType = type,
                    onDone = { navController.popBackStack() },
                )
            }
        }
    }
}

private fun NavHostController.navigateToTab(tab: Tab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
