package com.commissioning.momrecorder.ui.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.commissioning.momrecorder.ui.history.HistoryScreen
import com.commissioning.momrecorder.ui.record.RecordScreen
import com.commissioning.momrecorder.ui.tracker.TrackerScreen
import com.commissioning.momrecorder.viewmodel.MainViewModel

private enum class Tab(val label: String, val icon: ImageVector) {
    RECORD("Record", Icons.Filled.Mic),
    HISTORY("History", Icons.Filled.History),
    TRACKER("Tracker", Icons.Filled.TaskAlt)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: MainViewModel,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(Tab.RECORD) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = {
                            selectedTab = tab
                            when (tab) {
                                Tab.HISTORY -> vm.loadSavedMoms()
                                Tab.TRACKER -> vm.loadTrackerItems()
                                else -> {}
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        when (selectedTab) {
            Tab.RECORD -> RecordScreen(
                vm = vm,
                onNavigateToSettings = onNavigateToSettings,
                onMomGenerated = { mom -> onNavigateToDetail(mom.id) },
                modifier = Modifier.padding(padding)
            )
            Tab.HISTORY -> HistoryScreen(
                vm = vm,
                onOpenMom = { mom -> onNavigateToDetail(mom.id) },
                modifier = Modifier.padding(padding)
            )
            Tab.TRACKER -> TrackerScreen(
                vm = vm,
                modifier = Modifier.padding(padding)
            )
        }
    }
}
