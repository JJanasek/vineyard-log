package cz.janek.vineyardlog.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import cz.janek.vineyardlog.appContainer
import cz.janek.vineyardlog.data.settings.Settings
import cz.janek.vineyardlog.ui.LocalSettings
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
import cz.janek.vineyardlog.ui.guide.GuideDetailScreen
import cz.janek.vineyardlog.ui.guide.GuideScreen
import cz.janek.vineyardlog.ui.map.MapPickerScreen
import cz.janek.vineyardlog.ui.plan.SeasonPlanScreen
import cz.janek.vineyardlog.ui.products.ProductEditScreen
import cz.janek.vineyardlog.ui.products.ProductsScreen
import cz.janek.vineyardlog.ui.settings.SettingsScreen
import cz.janek.vineyardlog.ui.timeline.TimelineScreen
import cz.janek.vineyardlog.ui.tools.CalculatorsScreen
import cz.janek.vineyardlog.ui.weather.WeatherScreen
import cz.janek.vineyardlog.ui.overview.OverviewScreen
import cz.janek.vineyardlog.ui.reminders.RemindersScreen
import cz.janek.vineyardlog.ui.reminders.ReminderEditScreen

private fun Long.orNull(): Long? = if (this < 0) null else this

@Composable
fun AppRoot(
    sharedUrl: String? = null,
    onSharedUrlConsumed: () -> Unit = {},
    pendingRoute: String? = null,
    onRouteConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()
    LaunchedEffect(pendingRoute) {
        if (!pendingRoute.isNullOrBlank()) {
            runCatching { navController.navigate(pendingRoute) }
            onRouteConsumed()
        }
    }
    LaunchedEffect(sharedUrl) {
        if (!sharedUrl.isNullOrBlank()) {
            navController.navigate(Routes.productEdit(url = sharedUrl))
            onSharedUrlConsumed()
        }
    }
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBar = Tab.entries.any { it.route == currentRoute }
    val settings by LocalContext.current.appContainer.settings.settings.collectAsState(initial = Settings())

    CompositionLocalProvider(LocalSettings provides settings) {
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
                        onOpenGuide = { navController.navigate(Routes.GUIDE) },
                        onOpenPlan = { navController.navigate(Routes.PLAN) },
                    )
                }
                composable(Tab.CELLAR.route) {
                    BatchesScreen(
                        onOpenBatch = { navController.navigate(Routes.batch(it)) },
                        onNewBatch = { navController.navigate(Routes.batchEdit()) },
                        onOpenCalculators = { navController.navigate(Routes.CALCULATORS) },
                    )
                }
                composable(Tab.OVERVIEW.route) {
                    OverviewScreen(
                        onOpenWeather = { navController.navigate(Routes.WEATHER) },
                        onOpenReminders = { navController.navigate(Routes.REMINDERS) },
                        onOpenPlan = { navController.navigate(Routes.PLAN) },
                    )
                }
                composable(Routes.WEATHER) {
                    WeatherScreen(onOpenReminders = { navController.navigate(Routes.REMINDERS) }, onBack = { navController.popBackStack() })
                }
                composable(Routes.REMINDERS) {
                    RemindersScreen(
                        onBack = { navController.popBackStack() },
                        onEdit = { navController.navigate(Routes.reminderEdit(it)) },
                    )
                }
                composable(
                    Routes.REMINDER_EDIT,
                    arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L }),
                ) { entry ->
                    val id = (entry.arguments?.getLong("id") ?: -1L).orNull()
                    ReminderEditScreen(reminderId = id, onDone = { navController.popBackStack() })
                }
                composable(Tab.PRODUCTS.route) {
                    ProductsScreen(
                        onOpenProduct = { navController.navigate(Routes.productEdit(it)) },
                        onNewProduct = { navController.navigate(Routes.productEdit()) },
                    )
                }
                composable(Routes.SETTINGS) { entry ->
                    val picked by entry.savedStateHandle.getStateFlow<DoubleArray?>("pickedLocation", null).collectAsState()
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                        pickedLocation = picked?.let { it[0] to it[1] },
                        onPickedConsumed = { entry.savedStateHandle["pickedLocation"] = null },
                        onPickOnMap = { lat, lon -> navController.navigate(Routes.mapPicker(lat, lon)) },
                    )
                }
                composable(
                    Routes.MAP_PICKER,
                    arguments = listOf(
                        navArgument("lat") { type = NavType.StringType; defaultValue = "" },
                        navArgument("lon") { type = NavType.StringType; defaultValue = "" },
                    ),
                ) { entry ->
                    val lat = entry.arguments?.getString("lat")?.toDoubleOrNull()
                    val lon = entry.arguments?.getString("lon")?.toDoubleOrNull()
                    MapPickerScreen(
                        initialLat = lat, initialLon = lon,
                        onPicked = { la, lo ->
                            navController.previousBackStackEntry?.savedStateHandle?.set("pickedLocation", doubleArrayOf(la, lo))
                            navController.popBackStack()
                        },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.CALCULATORS) { CalculatorsScreen(onBack = { navController.popBackStack() }) }
                composable(Routes.PLAN) {
                    SeasonPlanScreen(
                        onBack = { navController.popBackStack() },
                        onLogTask = { task ->
                            navController.navigate(Routes.entryEdit(domain = Domain.VINEYARD, type = task.entryType, title = task.title))
                        },
                    )
                }
                composable(Routes.GUIDE) {
                    GuideScreen(onOpen = { navController.navigate(Routes.guide(it)) }, onBack = { navController.popBackStack() })
                }
                composable(Routes.GUIDE_ENTRY, arguments = listOf(navArgument("key") { type = NavType.StringType })) { entry ->
                    val key = entry.arguments?.getString("key") ?: return@composable
                    GuideDetailScreen(
                        entryKey = key,
                        onBack = { navController.popBackStack() },
                        onLogObservation = { title ->
                            navController.navigate(Routes.entryEdit(domain = Domain.VINEYARD, type = EntryType.SCOUTING, title = title))
                        },
                    )
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
                        onNewBatch = { navController.navigate(Routes.batchEdit(blockId = id)) },
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
                    arguments = listOf(
                        navArgument("id") { type = NavType.LongType; defaultValue = -1L },
                        navArgument("blockId") { type = NavType.LongType; defaultValue = -1L },
                    ),
                ) { entry ->
                    val id = (entry.arguments?.getLong("id") ?: -1L).orNull()
                    val fromBlock = (entry.arguments?.getLong("blockId") ?: -1L).orNull()
                    BatchEditScreen(batchId = id, fromBlockId = fromBlock, onDone = { navController.popBackStack() })
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
                        navArgument("title") { type = NavType.StringType; defaultValue = "" },
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
                        initialTitle = args?.getString("title")?.takeIf { it.isNotBlank() },
                        onDone = { navController.popBackStack() },
                    )
                }
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
