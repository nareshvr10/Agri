package com.example.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiDiagnosisRepository
import com.example.data.AlertRepository
import com.example.data.ScanRepository
import com.example.models.AiScanAnalysisResult
import com.example.models.AlertSeverity
import com.example.models.AlertType
import com.example.models.BackendSettings
import com.example.models.CropScanRecord
import com.example.models.CropType
import com.example.models.CropZone
import com.example.models.DiagnosisSeverity
import com.example.models.Farm
import com.example.models.FarmAlert
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ScannerUiState {
    data object Idle : ScannerUiState
    data object Analyzing : ScannerUiState
    data class ResultReady(
        val analysis: AiScanAnalysisResult,
        val cropType: CropType,
        val imageUri: String,
        val savedScanId: String? = null
    ) : ScannerUiState
    data class Error(val message: String) : ScannerUiState
}

class ScannerViewModel(
    private val currentUserId: String,
    private val aiDiagnosisRepository: AiDiagnosisRepository,
    private val scanRepository: ScanRepository,
    private val alertRepository: AlertRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScannerUiState>(ScannerUiState.Idle)
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    fun reset() {
        _uiState.value = ScannerUiState.Idle
    }

    fun analyzeAndSave(
        cropType: CropType,
        bitmap: Bitmap,
        imageUri: String,
        farm: Farm?,
        zone: CropZone?,
        settings: BackendSettings
    ) {
        viewModelScope.launch {
            _uiState.value = ScannerUiState.Analyzing
            aiDiagnosisRepository.analyzeCropImage(cropType, bitmap, settings)
                .onSuccess { analysis ->
                    if (!analysis.isServiceAvailable) {
                        _uiState.value = ScannerUiState.ResultReady(
                            analysis = analysis,
                            cropType = cropType,
                            imageUri = imageUri,
                            savedScanId = null
                        )
                        return@onSuccess
                    }

                    var savedId: String? = null
                    if (farm != null) {
                        val record = CropScanRecord(
                            farmId = farm.id,
                            cropZoneId = zone?.id ?: "general",
                            ownerId = currentUserId,
                            sharedExpertIds = farm.sharedExpertIds,
                            cropType = cropType.name,
                            imageUri = imageUri.ifBlank { "local://capture" },
                            identifiedCrop = analysis.identifiedCrop,
                            possibleCondition = analysis.possibleCondition,
                            visibleSymptoms = analysis.visibleSymptoms,
                            confidencePct = analysis.confidencePct.toLong(),
                            severity = analysis.severity.name,
                            recommendedNextSteps = analysis.recommendedNextSteps,
                            uncertaintyNotes = analysis.uncertaintyNotes,
                            expertConfirmed = false
                        )
                        savedId = scanRepository.saveScan(record).getOrNull()

                        if (analysis.severity == DiagnosisSeverity.HIGH || analysis.severity == DiagnosisSeverity.CRITICAL) {
                            alertRepository.createAlert(
                                FarmAlert(
                                    farmId = farm.id,
                                    cropZoneId = zone?.id ?: "general",
                                    ownerId = currentUserId,
                                    sharedExpertIds = farm.sharedExpertIds,
                                    alertType = AlertType.CROP_HEALTH.name,
                                    severity = AlertSeverity.WARNING.name,
                                    title = "Preliminary AI crop stress alert (${cropType.labelEn})",
                                    explanation = "${analysis.possibleCondition}. Preliminary screening — confirm with an Agricultural Expert."
                                )
                            )
                        }
                    }

                    _uiState.value = ScannerUiState.ResultReady(
                        analysis = analysis,
                        cropType = cropType,
                        imageUri = imageUri,
                        savedScanId = savedId
                    )
                }
                .onFailure { err ->
                    _uiState.value = ScannerUiState.Error(
                        err.localizedMessage ?: "AI crop analysis failed."
                    )
                }
        }
    }
}
