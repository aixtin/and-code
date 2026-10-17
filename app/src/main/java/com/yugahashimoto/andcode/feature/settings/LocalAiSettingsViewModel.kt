package com.yugahashimoto.andcode.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugahashimoto.andcode.data.connection.SecureSettingsRepository
import com.yugahashimoto.andcode.runtime.local.LocalAiController
import com.yugahashimoto.andcode.runtime.local.LocalAiModel
import com.yugahashimoto.andcode.runtime.local.LocalAiModelScanner
import com.yugahashimoto.andcode.runtime.local.LocalAiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class LocalAiUiState(
    val runtimeState: LocalAiState = LocalAiState.Idle,
    val models: List<LocalAiModel> = emptyList(),
    val selectedModel: LocalAiModel? = null,
    val scanning: Boolean = false,
)

class LocalAiSettingsViewModel(
    private val scanner: LocalAiModelScanner,
    private val controller: LocalAiController,
    private val settings: SecureSettingsRepository,
) : ViewModel() {
    private val models = MutableStateFlow<List<LocalAiModel>>(emptyList())
    private val selected = MutableStateFlow<LocalAiModel?>(null)
    private val scanning = MutableStateFlow(false)

    val state: StateFlow<LocalAiUiState> =
        combine(
            controller.state,
            models,
            selected,
            scanning,
            ::LocalAiUiState,
        ).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            LocalAiUiState(),
        )

    init {
        refreshModels()
    }

    fun refreshModels() {
        viewModelScope.launch {
            scanning.value = true
            val found = scanner.scan()
            models.value = found
            val persisted = settings.localAiModelPath?.let { File(it) }
            selected.value =
                found.firstOrNull { it.file == persisted } ?: persisted?.let { p ->
                    found.firstOrNull { it.file.absolutePath == p.absolutePath }
                } ?: found.firstOrNull()
            scanning.value = false
        }
    }

    fun select(model: LocalAiModel) {
        selected.value = model
        settings.localAiModelPath = model.file.absolutePath
    }

    fun start() {
        controller.start()
    }

    fun stop() {
        controller.stop()
    }
}
