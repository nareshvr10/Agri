package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.SensorsOff
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.models.AppLanguage
import com.example.models.CropType
import com.example.models.GrowthStage
import com.example.models.TelemetrySource
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.AlertCrimsonContainer
import com.example.ui.theme.ForestDeep
import com.example.ui.theme.HarvestAmber
import com.example.ui.theme.HarvestAmberContainer
import com.example.ui.theme.LeafGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgroGuardTopBar(
    title: String,
    canNavigateBack: Boolean,
    onBack: () -> Unit,
    language: AppLanguage,
    onToggleLanguage: () -> Unit,
    actions: @Composable () -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("top_bar_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back)
                    )
                }
            }
        },
        actions = {
            actions()
            LanguageTogglePill(language = language, onClick = onToggleLanguage)
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun LanguageTogglePill(
    language: AppLanguage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                text = if (language == AppLanguage.ENGLISH) "EN | தமிழ்" else "தமிழ் | EN",
                style = MaterialTheme.typography.labelLarge
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Translate,
                contentDescription = stringResource(R.string.switch_language),
                modifier = Modifier.size(16.dp)
            )
        },
        modifier = modifier
            .padding(end = 8.dp)
            .testTag("language_toggle_button"),
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
fun OfflineSyncBanner(
    isOnline: Boolean,
    lastSyncedEpochMs: Long,
    modifier: Modifier = Modifier
) {
    if (!isOnline || lastSyncedEpochMs > 0L) {
        val container = if (!isOnline) HarvestAmberContainer else MaterialTheme.colorScheme.surfaceVariant
        val content = if (!isOnline) Color(0xFF78350F) else MaterialTheme.colorScheme.onSurfaceVariant
        Surface(
            color = container,
            shape = RoundedCornerShape(12.dp),
            modifier = modifier
                .fillMaxWidth()
                .testTag("offline_sync_banner")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isOnline) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = "Offline",
                        tint = content,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    if (!isOnline) {
                        Text(
                            text = stringResource(R.string.offline_banner_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = content,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (lastSyncedEpochMs > 0L) {
                        Text(
                            text = stringResource(R.string.last_synced_prefix, formatEpochShort(lastSyncedEpochMs)),
                            style = MaterialTheme.typography.labelSmall,
                            color = content
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TelemetrySourceChip(
    source: TelemetrySource,
    language: AppLanguage,
    isCachedOffline: Boolean = false
) {
    val label = when {
        isCachedOffline -> if (language == AppLanguage.TAMIL) "ஆஃப்லைன் சேமிப்பு" else "Cached Offline"
        language == AppLanguage.TAMIL -> source.labelTa
        else -> source.labelEn
    }
    val (bg, fg) = when {
        isCachedOffline -> HarvestAmberContainer to Color(0xFF78350F)
        source == TelemetrySource.ESP32_VERIFIED -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        source == TelemetrySource.MANUAL_LOG -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        else -> AlertCrimsonContainer to AlertCrimson
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(50),
        modifier = Modifier.testTag("telemetry_source_chip_${source.name}")
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            action?.invoke()
        }
    }
}

@Composable
fun TelemetryChartCard(
    title: String,
    unit: String,
    points: List<Pair<Long, Double>>,
    lineColor: Color = LeafGreen,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                val latest = points.lastOrNull()?.second
                Text(
                    text = latest?.let { "${"%.1f".format(it)} $unit" } ?: "—",
                    style = MaterialTheme.typography.titleMedium,
                    color = lineColor,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(12.dp))
            if (points.size < 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SensorsOff,
                            contentDescription = "No chart series",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (points.isEmpty()) "No sensor dataset in selected range" else "Need at least 2 readings for trend",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val values = points.map { it.second }
                val minVal = (values.minOrNull() ?: 0.0)
                val maxVal = (values.maxOrNull() ?: 100.0).let { if (it <= minVal) minVal + 1.0 else it }
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val pad = 14f
                    val usableW = (w - 2 * pad).coerceAtLeast(1f)
                    val usableH = (h - 2 * pad).coerceAtLeast(1f)

                    // Subtle grid lines
                    for (i in 0..2) {
                        val y = pad + usableH * (i / 2f)
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.2f),
                            start = Offset(pad, y),
                            end = Offset(w - pad, y),
                            strokeWidth = 1.5f
                        )
                    }

                    val path = Path()
                    val stepX = usableW / (values.size - 1).coerceAtLeast(1)
                    values.forEachIndexed { idx, value ->
                        val normY = ((value - minVal) / (maxVal - minVal)).toFloat().coerceIn(0f, 1f)
                        val x = pad + idx * stepX
                        val y = pad + usableH * (1f - normY)
                        if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(width = 5f, cap = StrokeCap.Round)
                    )
                    values.forEachIndexed { idx, value ->
                        val normY = ((value - minVal) / (maxVal - minVal)).toFloat().coerceIn(0f, 1f)
                        val x = pad + idx * stepX
                        val y = pad + usableH * (1f - normY)
                        drawCircle(color = ForestDeep, radius = 6f, center = Offset(x, y))
                        drawCircle(color = lineColor, radius = 4f, center = Offset(x, y))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Min: ${"%.1f".format(minVal)} $unit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Max: ${"%.1f".format(maxVal)} $unit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

fun cropLabel(crop: CropType, language: AppLanguage): String =
    if (language == AppLanguage.TAMIL) crop.labelTa else crop.labelEn

fun stageLabel(stage: GrowthStage, language: AppLanguage): String =
    if (language == AppLanguage.TAMIL) stage.labelTa else stage.labelEn

fun formatEpochShort(epochMs: Long): String {
    if (epochMs <= 0L) return "—"
    return SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(epochMs))
}
