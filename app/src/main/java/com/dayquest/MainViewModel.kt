package com.dayquest

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dayquest.data.backup.BackupManager
import com.dayquest.data.backup.BackupSummary
import com.dayquest.data.repository.DayquestRepository
import com.dayquest.domain.AppState
import com.dayquest.domain.Category
import com.dayquest.domain.DisplayRank
import com.dayquest.domain.EarnedReward
import com.dayquest.domain.Event
import com.dayquest.domain.GoalSlot
import com.dayquest.domain.LibraryItem
import com.dayquest.domain.Quest
import com.dayquest.domain.RankStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class Toast(val message: String, val undoQuestId: String? = null)

sealed interface Celebration {
    data class RankUp(val before: DisplayRank, val after: DisplayRank, val gain: Int) : Celebration
    data class Reward(val key: String, val reward: EarnedReward, val numeral: String, val isWord: Boolean) : Celebration
}

sealed interface Dialog {
    data class SetQuestReward(val questId: String) : Dialog
    data class RemoveQuest(val questId: String) : Dialog
    data class SetGoal(val slot: GoalSlot) : Dialog
    object NewQuest : Dialog
    data class ConfirmRestore(val summary: BackupSummary) : Dialog
    object ConfirmReset : Dialog
}

data class UiState(
    val loaded: Boolean = false,
    val app: AppState? = null, // null = no profile yet, show onboarding
    val toast: Toast? = null,
    val celebrations: List<Celebration> = emptyList(),
    val dialog: Dialog? = null,
    val restoreError: String? = null,
    val selectedTab: Int = 0,
    val selectedQuestId: String? = "body",
    val showCover: Boolean = false,
)

class MainViewModel(
    private val repo: DayquestRepository,
    private val backupManager: BackupManager
) : ViewModel() {

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            repo.observe().collect { app ->
                _ui.update { cur ->
                    val selected = cur.selectedQuestId ?: app?.selectedQuestId ?: "body"
                    cur.copy(
                        loaded = true,
                        app = app,
                        selectedQuestId = if (app != null && app.visibleQuests.any { it.id == selected }) selected else app?.visibleQuests?.firstOrNull()?.id
                    )
                }
            }
        }

        // Ticker to automatically refresh date at midnight while the app is open
        viewModelScope.launch {
            var lastDay = java.time.LocalDate.now().toString()
            while (isActive) {
                delay(30_000)
                val currentDay = java.time.LocalDate.now().toString()
                if (currentDay != lastDay) {
                    lastDay = currentDay
                    repo.refreshDate()
                }
            }
        }
    }

    fun onResume() {
        repo.refreshDate()
    }

    fun selectTab(tab: Int) = _ui.update { it.copy(selectedTab = tab) }
    fun selectQuest(id: String) = _ui.update { it.copy(selectedQuestId = id) }
    fun toggleCover() = _ui.update { it.copy(showCover = !it.showCover) }

    fun claim(questId: String? = null) = viewModelScope.launch {
        val app = _ui.value.app ?: return@launch
        val id = questId ?: _ui.value.selectedQuestId ?: app.selectedQuestId ?: return@launch
        val result = repo.claim(id) ?: return@launch
        _ui.update {
            it.copy(
                toast = Toast(result.message),
                celebrations = result.events.map(::toCelebration)
            )
        }
    }

    fun dismissCelebration() = _ui.update { it.copy(celebrations = it.celebrations.drop(1)) }

    fun toggleStep(questId: String, index: Int) = viewModelScope.launch {
        repo.toggleStep(questId, index)
    }

    fun advanceQuest(questId: String) = viewModelScope.launch {
        repo.advanceQuest(questId)
    }

    fun openDialog(d: Dialog) = _ui.update { it.copy(dialog = d) }
    fun closeDialog() = _ui.update { it.copy(dialog = null, restoreError = null) }

    fun removeQuest(id: String) = viewModelScope.launch {
        if (repo.removeQuest(id)) {
            _ui.update { it.copy(dialog = null, toast = Toast("Removed.", undoQuestId = id)) }
        }
    }

    fun undoRemove(id: String) = viewModelScope.launch {
        repo.undoRemove(id)
        _ui.update { it.copy(toast = null, selectedQuestId = id) }
    }

    fun setQuestReward(questId: String, text: String?) = viewModelScope.launch {
        repo.setQuestReward(questId, text)
        _ui.update {
            it.copy(
                dialog = null,
                toast = if (text.isNullOrBlank()) null else Toast("Reward saved: ${text.trim()}")
            )
        }
    }

    fun setGoal(slot: GoalSlot, text: String?) = viewModelScope.launch {
        repo.setGoal(slot, text)
        _ui.update {
            it.copy(
                dialog = null,
                toast = if (text.isNullOrBlank()) null else Toast("Reward saved: ${text.trim()}")
            )
        }
    }

    fun enjoy(key: String) = viewModelScope.launch {
        val app = _ui.value.app ?: return@launch
        val name = app.name
        val reward = app.earned[key]
        repo.enjoy(key)
        if (reward != null) {
            _ui.update { it.copy(toast = Toast("Enjoy it, $name: ${reward.text}")) }
        }
    }

    fun setRankStyle(style: RankStyle) = viewModelScope.launch {
        repo.setRankStyle(style)
    }

    fun addFromLibrary(item: LibraryItem) = viewModelScope.launch {
        val quest = Quest(
            id = item.id,
            category = item.category,
            title = item.name,
            shortTitle = item.name,
            xp = item.xp,
            story = item.meta,
            steps = item.steps,
            isCustom = false,
            libraryId = item.id
        )
        repo.addQuest(quest, null)
        _ui.update { it.copy(selectedQuestId = item.id, selectedTab = 0, toast = Toast("Added to Today: ${item.name}")) }
    }

    fun createQuest(name: String, category: Category, xp: Int, reward: String? = null) = viewModelScope.launch {
        val id = "custom_${System.currentTimeMillis()}"
        val steps = listOf(
            "Set a 15-minute timer",
            "Begin first action",
            "Wrap up and mark finished"
        )
        val quest = Quest(
            id = id,
            category = category,
            title = name,
            shortTitle = name,
            xp = xp,
            story = "A custom quest created by you.",
            steps = steps,
            isCustom = true
        )
        repo.addQuest(quest, reward)
        _ui.update { it.copy(dialog = null, selectedQuestId = id, selectedTab = 0, toast = Toast("Added to Today: $name")) }
    }

    fun completeOnboarding(name: String, isDemo: Boolean = false) = viewModelScope.launch {
        if (repo.completeOnboarding(name, isDemo)) {
            val msg = if (isDemo) {
                "Welcome to the demo, $name. Level 14 and 13-day streak loaded."
            } else {
                "Welcome, $name. Day 1 starts today."
            }
            _ui.update { it.copy(toast = Toast(msg)) }
        }
    }

    fun backup(destUri: Uri, fileName: String) = viewModelScope.launch {
        val result = backupManager.backupTo(destUri)
        if (result.isSuccess) {
            _ui.update { it.copy(toast = Toast("Backup saved: $fileName")) }
        } else {
            _ui.update { it.copy(toast = Toast("Backup failed: ${result.exceptionOrNull()?.message ?: "Unknown error"}")) }
        }
    }

    fun inspectBackup(uri: Uri, fromOnboarding: Boolean = false) = viewModelScope.launch {
        val result = backupManager.inspect(uri)
        if (result.isSuccess) {
            val summary = result.getOrThrow()
            if (fromOnboarding) {
                restore(summary)
            } else {
                _ui.update { it.copy(dialog = Dialog.ConfirmRestore(summary), restoreError = null) }
            }
        } else {
            val msg = result.exceptionOrNull()?.message ?: "The file isn't valid backup data."
            _ui.update { it.copy(restoreError = msg) }
        }
    }

    fun restore(summary: BackupSummary) = viewModelScope.launch {
        val result = backupManager.restore(summary)
        if (result.isSuccess) {
            _ui.update { it.copy(dialog = null, restoreError = null, toast = Toast("Restored ${summary.name}'s progress.")) }
        } else {
            _ui.update { it.copy(restoreError = result.exceptionOrNull()?.message ?: "The file isn't valid backup data.") }
        }
    }

    fun reset() = viewModelScope.launch {
        repo.reset()
        _ui.update {
            it.copy(
                dialog = null,
                toast = null,
                celebrations = emptyList(),
                restoreError = null,
                selectedTab = 0,
                selectedQuestId = "body",
                showCover = false
            )
        }
    }

    fun dismissToast() = _ui.update { it.copy(toast = null) }
    fun showToast(message: String) = _ui.update { it.copy(toast = Toast(message)) }
}

private fun toCelebration(e: Event): Celebration = when (e) {
    is Event.RankUp -> Celebration.RankUp(e.before, e.after, e.gain)
    is Event.RewardEarned -> Celebration.Reward(e.key, e.reward, e.numeral, e.isWord)
}

class MainViewModelFactory(
    private val repo: DayquestRepository,
    private val backupManager: BackupManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(repo, backupManager) as T
    }
}
