package com.dayquest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dayquest.data.backup.BackupManager
import com.dayquest.data.db.DbHolder
import com.dayquest.data.repository.DayquestRepository
import com.dayquest.domain.GoalSlot
import com.dayquest.domain.RewardKind
import com.dayquest.ui.components.ConfirmResetDialog
import com.dayquest.ui.components.ConfirmRestoreDialog
import com.dayquest.ui.components.DqToast
import com.dayquest.ui.components.RemoveQuestDialog
import com.dayquest.ui.components.RewardDialog
import com.dayquest.ui.screens.CelebrationDialog
import com.dayquest.ui.screens.CoverScreen
import com.dayquest.ui.screens.LibraryScreen
import com.dayquest.ui.screens.NewQuestSheet
import com.dayquest.ui.screens.OnboardingScreen
import com.dayquest.ui.screens.ProfileScreen
import com.dayquest.ui.screens.QuestDetailScreen
import com.dayquest.ui.screens.StatsScreen
import com.dayquest.ui.screens.TodayScreen
import com.dayquest.ui.theme.DayquestTheme
import com.dayquest.ui.theme.Dq
import com.dayquest.ui.theme.Sans
import kotlinx.coroutines.launch

enum class ScreenTab {
    Today, Library, Stats, Profile
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        val dbHolder = DbHolder(applicationContext)
        val repo = DayquestRepository(dbHolder)
        val backup = BackupManager(applicationContext, dbHolder)
        MainViewModelFactory(repo, backup)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DayquestTheme {
                val uiState by viewModel.ui.collectAsState()
                val state = uiState.app

                var currentTab by remember { mutableStateOf(ScreenTab.Today) }
                var isCoverMode by remember { mutableStateOf(false) }
                var viewingQuestId by remember { mutableStateOf<String?>(null) }
                var showNewQuestSheet by remember { mutableStateOf(false) }

                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                val scope = rememberCoroutineScope()

                val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

                // File launchers for Backup & Restore
                val backupLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument("application/octet-stream")
                ) { uri ->
                    if (uri != null) {
                        viewModel.backup(uri, BackupManager.suggestedBackupFilename())
                    }
                }

                val restoreLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri ->
                    if (uri != null) {
                        viewModel.inspectBackup(uri, fromOnboarding = (state == null))
                    }
                }

                BackHandler(enabled = viewingQuestId != null) {
                    viewingQuestId = null
                }
                BackHandler(enabled = viewingQuestId == null && isCoverMode) {
                    isCoverMode = false
                }

                LaunchedEffect(state == null) {
                    if (state == null) {
                        currentTab = ScreenTab.Today
                        isCoverMode = false
                        viewingQuestId = null
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Dq.Ground)
                        .padding(top = statusBarPadding)
                ) {
                    if (!uiState.loaded) {
                        // Room is initializing
                    } else if (state == null) {
                        OnboardingScreen(
                            onStartFirstDay = { name ->
                                viewModel.completeOnboarding(name, isDemo = false)
                            },
                            onRestoreBackup = {
                                restoreLauncher.launch(arrayOf("*/*"))
                            },
                            onStartDemo = { name ->
                                viewModel.completeOnboarding(name, isDemo = true)
                            },
                            restoreError = uiState.restoreError
                        )
                    } else {
                        Scaffold(
                            containerColor = Dq.Ground,
                            bottomBar = {
                                if (viewingQuestId == null) {
                                    BottomBar(
                                        currentTab = currentTab,
                                        onTabSelected = { tab ->
                                            if (tab == ScreenTab.Today && currentTab == ScreenTab.Today) {
                                                isCoverMode = !isCoverMode
                                            } else {
                                                if (tab != ScreenTab.Today) {
                                                    isCoverMode = false
                                                }
                                                currentTab = tab
                                            }
                                        },
                                        onPlusClicked = {
                                            showNewQuestSheet = true
                                        },
                                        bottomInset = navBarPadding
                                    )
                                }
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                if (viewingQuestId != null) {
                                    QuestDetailScreen(
                                        questId = viewingQuestId!!,
                                        state = state,
                                        onBack = { viewingQuestId = null },
                                        onToggleStep = { stepIndex ->
                                            viewModel.toggleStep(viewingQuestId!!, stepIndex)
                                        },
                                        onAdvanceStep = {
                                            viewModel.advanceQuest(viewingQuestId!!)
                                        },
                                        onClaim = {
                                            viewModel.claim(viewingQuestId!!)
                                        },
                                        onSetRewardClick = {
                                            viewModel.openDialog(Dialog.SetQuestReward(viewingQuestId!!))
                                        },
                                        onRemoveClick = {
                                            viewModel.openDialog(Dialog.RemoveQuest(viewingQuestId!!))
                                        }
                                    )
                                } else {
                                    when (currentTab) {
                                        ScreenTab.Today -> {
                                            if (isCoverMode) {
                                                CoverScreen(
                                                    state = state,
                                                    onQuestClick = { questId ->
                                                        viewModel.selectQuest(questId)
                                                        viewingQuestId = questId
                                                    },
                                                    onBackToToday = {
                                                        isCoverMode = false
                                                    }
                                                )
                                            } else {
                                                TodayScreen(
                                                    state = state,
                                                    onQuestClick = { questId ->
                                                        viewModel.selectQuest(questId)
                                                        viewingQuestId = questId
                                                    },
                                                    onNotificationClick = {
                                                        viewModel.showToast("No new notifications.")
                                                    },
                                                    onEnjoyReward = { rewardKey ->
                                                        viewModel.enjoy(rewardKey)
                                                    },
                                                    onOpenCover = {
                                                        isCoverMode = true
                                                    },
                                                    onResetClick = {
                                                        viewModel.openDialog(Dialog.ConfirmReset)
                                                    }
                                                )
                                            }
                                        }
                                        ScreenTab.Library -> LibraryScreen(
                                            state = state,
                                            onAddHabit = { item ->
                                                viewModel.addFromLibrary(item)
                                            }
                                        )
                                        ScreenTab.Stats -> StatsScreen(
                                            state = state
                                        )
                                        ScreenTab.Profile -> ProfileScreen(
                                            state = state,
                                            onRankStyleChange = { viewModel.setRankStyle(it) },
                                            onSetGoal = { slot ->
                                                viewModel.openDialog(Dialog.SetGoal(slot))
                                            },
                                            onEnjoyReward = { rewardKey ->
                                                viewModel.enjoy(rewardKey)
                                            },
                                            onBackupNow = {
                                                backupLauncher.launch(BackupManager.suggestedBackupFilename())
                                            },
                                            onRestoreFromFile = {
                                                restoreLauncher.launch(arrayOf("*/*"))
                                            },
                                            onResetClick = {
                                                viewModel.openDialog(Dialog.ConfirmReset)
                                            }
                                        )
                                    }
                                }

                                // Toast
                                DqToast(
                                    toast = uiState.toast,
                                    onDismiss = { viewModel.dismissToast() },
                                    onUndo = { questId -> viewModel.undoRemove(questId) },
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 8.dp)
                                )
                            }
                        }

                        // Modal Bottom Sheet for New Quest
                        if (showNewQuestSheet) {
                            NewQuestSheet(
                                sheetState = sheetState,
                                onDismiss = {
                                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                                        showNewQuestSheet = false
                                    }
                                },
                                onCreateQuest = { name, category, xp ->
                                    viewModel.createQuest(name, category, xp)
                                    currentTab = ScreenTab.Today
                                }
                            )
                        }
                    }

                    // Dialogs
                    when (val dlg = uiState.dialog) {
                        is Dialog.SetQuestReward -> {
                            val quest = state?.quests?.find { it.id == dlg.questId }
                            RewardDialog(
                                title = "Treat yourself",
                                body = "Give ${quest?.title ?: "this quest"} a small reward when finished.",
                                initialText = state?.rewards?.get(dlg.questId),
                                onDismiss = { viewModel.closeDialog() },
                                onSave = { text ->
                                    viewModel.setQuestReward(dlg.questId, text)
                                },
                                onRemove = if (state?.rewards?.containsKey(dlg.questId) == true) {
                                    { viewModel.setQuestReward(dlg.questId, null) }
                                } else null
                            )
                        }
                        is Dialog.RemoveQuest -> {
                            val quest = state?.quests?.find { it.id == dlg.questId }
                            if (quest != null) {
                                RemoveQuestDialog(
                                    quest = quest,
                                    onDismiss = { viewModel.closeDialog() },
                                    onConfirmRemove = {
                                        viewModel.removeQuest(dlg.questId)
                                        if (viewingQuestId == dlg.questId) {
                                            viewingQuestId = null
                                        }
                                    }
                                )
                            }
                        }
                        is Dialog.SetGoal -> {
                            val title = when (dlg.slot) {
                                is GoalSlot.Streak -> "${dlg.slot.days}-day streak reward"
                                is GoalSlot.Level -> "Level ${(state?.level ?: 14) + 1} reward"
                                is GoalSlot.Rank -> "Rank milestone reward"
                            }
                            val initial = when (dlg.slot) {
                                is GoalSlot.Streak -> state?.goals?.streak?.get(dlg.slot.days)
                                is GoalSlot.Level -> state?.goals?.level
                                is GoalSlot.Rank -> state?.goals?.rank?.get(dlg.slot.key)
                            }
                            RewardDialog(
                                title = title,
                                body = "Pick something memorable when you achieve this.",
                                initialText = initial,
                                onDismiss = { viewModel.closeDialog() },
                                onSave = { text ->
                                    viewModel.setGoal(dlg.slot, text)
                                },
                                onRemove = if (initial != null) {
                                    { viewModel.setGoal(dlg.slot, null) }
                                } else null
                            )
                        }
                        is Dialog.ConfirmRestore -> {
                            ConfirmRestoreDialog(
                                summary = dlg.summary,
                                onDismiss = { viewModel.closeDialog() },
                                onConfirm = { viewModel.restore(dlg.summary) }
                            )
                        }
                        is Dialog.ConfirmReset -> {
                            ConfirmResetDialog(
                                onDismiss = { viewModel.closeDialog() },
                                onConfirm = {
                                    viewModel.closeDialog()
                                    viewModel.reset()
                                }
                            )
                        }
                        is Dialog.NewQuest -> {
                            showNewQuestSheet = true
                            viewModel.closeDialog()
                        }
                        null -> {}
                    }

                    // Celebration Screen (Queue)
                    uiState.celebrations.firstOrNull()?.let { celebration ->
                        if (state != null) {
                            CelebrationDialog(
                                celebration = celebration,
                                appState = state,
                                onContinue = {
                                    viewModel.dismissCelebration()
                                },
                                onOpenProfile = {
                                    viewModel.dismissCelebration()
                                    viewingQuestId = null
                                    currentTab = ScreenTab.Profile
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomBar(
    currentTab: ScreenTab,
    onTabSelected: (ScreenTab) -> Unit,
    onPlusClicked: () -> Unit,
    bottomInset: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Dq.Ground)
            .border(width = 1.dp, color = Dq.Border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabItem(
                label = "Today",
                iconRes = R.drawable.ic_today,
                isSelected = currentTab == ScreenTab.Today,
                onClick = { onTabSelected(ScreenTab.Today) },
                modifier = Modifier.weight(1f)
            )

            TabItem(
                label = "Library",
                iconRes = R.drawable.ic_library,
                isSelected = currentTab == ScreenTab.Library,
                onClick = { onTabSelected(ScreenTab.Library) },
                modifier = Modifier.weight(1f)
            )

            // Centre + button (48dp circle, text fill, ground ink)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Dq.Text)
                    .clickable(role = Role.Button, onClick = onPlusClicked),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_plus),
                    contentDescription = "New quest",
                    tint = Dq.Ground,
                    modifier = Modifier.size(24.dp)
                )
            }

            TabItem(
                label = "Stats",
                iconRes = R.drawable.ic_stats,
                isSelected = currentTab == ScreenTab.Stats,
                onClick = { onTabSelected(ScreenTab.Stats) },
                modifier = Modifier.weight(1f)
            )

            TabItem(
                label = "Profile",
                iconRes = R.drawable.ic_profile,
                isSelected = currentTab == ScreenTab.Profile,
                onClick = { onTabSelected(ScreenTab.Profile) },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(bottomInset))
    }
}

@Composable
fun TabItem(
    label: String,
    iconRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = if (isSelected) Dq.Text else Dq.Muted

    Column(
        modifier = modifier
            .height(56.dp)
            .clickable(role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            fontFamily = Sans,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = contentColor
        )
    }
}
