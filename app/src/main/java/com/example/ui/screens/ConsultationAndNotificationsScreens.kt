package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.models.ConsultationMessage
import com.example.models.CropScanRecord
import com.example.models.Farm
import com.example.models.FarmAlert
import com.example.models.UserRole
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.formatEpochShort

@Composable
fun ExpertConsultationScreen(
    selectedFarm: Farm?,
    messages: List<ConsultationMessage>,
    farmScans: List<CropScanRecord>,
    userRole: UserRole,
    onSendMessage: (Farm, String) -> Unit,
    onVerifyScan: (scanId: String, confirmed: Boolean, notes: String) -> Unit
) {
    var messageInput by remember { mutableStateOf("") }
    var expertNoteInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("expert_consultation_screen")
    ) {
        if (selectedFarm == null) {
            EmptyStateCard(
                icon = Icons.Default.SupportAgent,
                title = "No Farm Selected",
                description = "Select a farm (or ask a farmer to share a farm with you) to start consultation."
            )
            return
        }

        Text(
            text = "Consultation: ${selectedFarm.name}",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Shared Experts: ${selectedFarm.sharedExpertIds.size} • Scans pending review: ${farmScans.count { !it.expertConfirmed }}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if ((userRole == UserRole.EXPERT || userRole == UserRole.ADMIN) && farmScans.any { !it.expertConfirmed }) {
                val pending = farmScans.first { !it.expertConfirmed }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Pending AI Scan Review", style = MaterialTheme.typography.titleMedium)
                            Text("${pending.possibleCondition} (${pending.confidencePct}%)")
                            OutlinedTextField(
                                value = expertNoteInput,
                                onValueChange = { expertNoteInput = it },
                                label = { Text("Agronomic Verification Notes") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(onClick = {
                                onVerifyScan(pending.id, true, expertNoteInput.ifBlank { "Reviewed by Agricultural Expert." })
                                expertNoteInput = ""
                            }) {
                                Text("Confirm Expert Assessment")
                            }
                        }
                    }
                }
            }

            if (messages.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.SupportAgent,
                        title = "No Messages Yet",
                        description = "Send a question or attach crop observations for shared Agricultural Experts."
                    )
                }
            } else {
                items(messages, key = { it.id }) { msg ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${msg.senderName} (${msg.senderRole})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = formatEpochShort(msg.createdAt?.toDate()?.time ?: 0L),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Text(msg.messageText, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageInput,
                onValueChange = { messageInput = it },
                placeholder = { Text("Ask an expert or post guidance...") },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    val trimmed = messageInput.trim()
                    if (trimmed.isNotEmpty()) {
                        onSendMessage(selectedFarm, trimmed)
                        messageInput = ""
                    }
                }
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
    }
}

@Composable
fun NotificationsScreen(
    alerts: List<FarmAlert>,
    lastSyncedEpochMs: Long,
    isOnline: Boolean
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("notifications_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isOnline) "Connected • Last sync: ${formatEpochShort(lastSyncedEpochMs)}"
                    else "Offline • Queued writes will sync automatically via WorkManager.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
        if (alerts.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Notifications,
                    title = "No Notifications",
                    description = "Farm threshold alerts and sync notifications appear here."
                )
            }
        } else {
            items(alerts, key = { it.id }) { alert ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(alert.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(alert.explanation, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "${alert.severity} • ${if (alert.acknowledged) "Read" else "Unread"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
