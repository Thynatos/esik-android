package com.thynatos.esik.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thynatos.esik.ui.theme.StatusHealthy
import com.thynatos.esik.ui.theme.StatusWarning

@Composable
fun MonitoringControlCard(
    monitoringStarted: Boolean,
    hasUsageAccess: Boolean,
    canDrawOverlays: Boolean,
    onStartMonitoring: () -> Unit,
    onStopMonitoring: () -> Unit,
    onOpenUsagePermission: () -> Unit,
    onOpenOverlayPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val permissionsReady = hasUsageAccess && canDrawOverlays

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Live Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Arka Plan Takibi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (monitoringStarted) "Uygulama kullanımı izleniyor" else "Servis kapalı",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Active dot badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (monitoringStarted) StatusHealthy.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (monitoringStarted) StatusHealthy
                                else MaterialTheme.colorScheme.outline
                            ),
                    )
                    Text(
                        text = if (monitoringStarted) "Aktif" else "Duraklatıldı",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = if (monitoringStarted) StatusHealthy else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Missing Permissions Warning Banner
            AnimatedVisibility(visible = !permissionsReady) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = StatusWarning.copy(alpha = 0.12f),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "⚠️ Takip için gerekli izinler eksik",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = StatusWarning,
                        )

                        if (!hasUsageAccess) {
                            PermissionRow(
                                label = "Kullanım Erişimi",
                                granted = false,
                                onOpen = onOpenUsagePermission,
                            )
                        }

                        if (!canDrawOverlays) {
                            PermissionRow(
                                label = "Üste Çizme İzni (Overlay)",
                                granted = false,
                                onOpen = onOpenOverlayPermission,
                            )
                        }
                    }
                }
            }

            // Action Button
            Button(
                onClick = if (monitoringStarted) onStopMonitoring else onStartMonitoring,
                enabled = monitoringStarted || permissionsReady,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = if (monitoringStarted) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                    )
                } else {
                    ButtonDefaults.buttonColors()
                },
            ) {
                Text(if (monitoringStarted) "Takibi Durdur" else "Takibi Başlat")
            }
        }
    }
}

@Composable
private fun PermissionRow(
    label: String,
    granted: Boolean,
    onOpen: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
        )
        OutlinedButton(
            onClick = onOpen,
            shape = RoundedCornerShape(8.dp),
        ) {
            Text(if (granted) "Açık" else "İzin Ver", style = MaterialTheme.typography.labelSmall)
        }
    }
}
