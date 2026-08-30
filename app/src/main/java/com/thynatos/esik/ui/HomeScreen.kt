package com.thynatos.esik.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.thynatos.esik.data.UserProfile
import com.thynatos.esik.ui.components.DeveloperToolsCard
import com.thynatos.esik.ui.components.EsikCard
import com.thynatos.esik.ui.components.EsikScreen
import com.thynatos.esik.ui.components.EsikTopBar
import com.thynatos.esik.ui.components.PermissionBanner
import com.thynatos.esik.ui.components.PrimaryActionButton
import com.thynatos.esik.ui.components.SecondaryActionButton
import com.thynatos.esik.ui.components.SectionTitle
import com.thynatos.esik.ui.components.StatItem
import com.thynatos.esik.ui.components.StatusPill
import com.thynatos.esik.ui.theme.EsikSpacing

@Composable
fun HomeScreen(
    profile: UserProfile,
    usageMinutes: Int,
    recordCount: Int,
    monitoringStarted: Boolean,
    hasUsageAccess: Boolean,
    canDrawOverlays: Boolean,
    reportLoading: Boolean,
    onRefresh: () -> Unit,
    onOpenUsagePermission: () -> Unit,
    onOpenOverlayPermission: () -> Unit,
    onUpdateLimit: (Int) -> Unit,
    onStartMonitoring: () -> Unit,
    onStopMonitoring: () -> Unit,
    onOpenTargetApp: () -> Unit,
    onOpenIntervention: () -> Unit,
    onOpenReport: () -> Unit,
    onLoadDemoData: () -> Unit,
    onClearData: () -> Unit,
) {
    var limitText by rememberSaveable(profile.dailyLimitMinutes) {
        mutableStateOf(profile.dailyLimitMinutes.toString())
    }
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }
    val progress = if (profile.dailyLimitMinutes <= 0) 0f else {
        (usageMinutes.toFloat() / profile.dailyLimitMinutes.toFloat()).coerceIn(0f, 1f)
    }
    val permissionsReady = hasUsageAccess && canDrawOverlays
    val compactLayout = LocalConfiguration.current.screenWidthDp < 360

    EsikScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = EsikSpacing.xLarge, vertical = EsikSpacing.xxLarge),
            verticalArrangement = Arrangement.spacedBy(EsikSpacing.xLarge),
        ) {
            EsikTopBar(
                title = "Merhaba, ${profile.name}",
                subtitle = "Bugünkü kullanımını kendi hedefinle birlikte gör.",
                trailing = {
                    StatusPill(
                        label = if (monitoringStarted) "Takip açık" else "Takip kapalı",
                        active = monitoringStarted,
                    )
                },
            )

            EsikCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                Column(verticalArrangement = Arrangement.spacedBy(EsikSpacing.large)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile.targetAppLabel,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = "Bugünkü kullanım",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = onRefresh) {
                            Text("Yenile")
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(EsikSpacing.small),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(
                            text = usageMinutes.toString(),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "dakika",
                            modifier = Modifier.padding(bottom = 5.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "0 dk",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Kendi hedefin ${profile.dailyLimitMinutes} dk",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }

            EsikCard {
                Column(verticalArrangement = Arrangement.spacedBy(EsikSpacing.large)) {
                    if (compactLayout) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(EsikSpacing.xSmall),
                        ) {
                            Text("Günün yansıması", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "Yerel kayıtlarına ve bugünkü sayılara birlikte bak.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        StatItem(value = recordCount.toString(), label = "cihazdaki kayıt")
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(EsikSpacing.large),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(EsikSpacing.xSmall),
                            ) {
                                Text("Günün yansıması", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    "Yerel kayıtlarına ve bugünkü sayılara birlikte bak.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            StatItem(value = recordCount.toString(), label = "cihazdaki kayıt")
                        }
                    }
                    PrimaryActionButton(
                        text = if (reportLoading) "Rapor hazırlanıyor…" else "Günlük raporu aç",
                        onClick = onOpenReport,
                        enabled = !reportLoading,
                    )
                    if (reportLoading) {
                        Text(
                            "Bugünkü yerel kayıtlar değerlendiriliyor. Bu birkaç saniye sürebilir.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (!hasUsageAccess || !canDrawOverlays) {
                SectionTitle(
                    title = "Kurulumu tamamla",
                    supportingText = "Takibin çalışması için eksik izinleri aç.",
                )
            }
            if (!hasUsageAccess) {
                PermissionBanner(
                    title = "Kullanım erişimi kapalı",
                    explanation = "Eşik, seçtiğin uygulamanın bugünkü süresini okuyabilsin.",
                    actionLabel = "İzin ver",
                    onAction = onOpenUsagePermission,
                )
            }
            if (!canDrawOverlays) {
                PermissionBanner(
                    title = "Ekran üstü kart izni kapalı",
                    explanation = "Kendi hedefine ulaştığında destek kartı görünebilsin.",
                    actionLabel = "İzin ver",
                    onAction = onOpenOverlayPermission,
                )
            }

            SectionTitle(
                title = "Takip ayarları",
                supportingText = "Hedefini ve izlemeyi buradan yönet.",
            )
            EsikCard {
                Column(verticalArrangement = Arrangement.spacedBy(EsikSpacing.large)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Kullanım takibi", style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (monitoringStarted) "Arka planda çalışıyor" else "Şu anda duraklatıldı",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        StatusPill(
                            label = if (monitoringStarted) "Açık" else "Kapalı",
                            active = monitoringStarted,
                        )
                    }
                    if (monitoringStarted) {
                        SecondaryActionButton("Takibi durdur", onClick = onStopMonitoring)
                    } else {
                        PrimaryActionButton(
                            text = "Takibi başlat",
                            onClick = onStartMonitoring,
                            enabled = permissionsReady,
                        )
                    }
                    if (!permissionsReady && !monitoringStarted) {
                        Text(
                            "Takibi başlatmak için iki izni de aç.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                    Text("Günlük hedef", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = limitText,
                        onValueChange = { limitText = it.filter(Char::isDigit).take(4) },
                        label = { Text("Dakika") },
                        supportingText = { Text("Bu sayıyı yalnızca sen belirlersin.") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SecondaryActionButton(
                        text = "Hedefi güncelle",
                        onClick = {
                            limitText.toIntOrNull()?.takeIf { it > 0 }?.let(onUpdateLimit)
                        },
                        enabled = limitText.toIntOrNull()?.let { it > 0 } == true,
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                    Text("Seçili uygulama", style = MaterialTheme.typography.titleMedium)
                    Text(
                        profile.targetAppLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SecondaryActionButton(
                        text = "${profile.targetAppLabel} uygulamasını aç",
                        onClick = onOpenTargetApp,
                    )
                }
            }

            SectionTitle(
                title = "Verilerin",
                supportingText = "Profilin ve müdahale kayıtların yalnızca bu cihazda tutulur.",
            )
            EsikCard {
                Column(verticalArrangement = Arrangement.spacedBy(EsikSpacing.medium)) {
                    Text(
                        "Eşik hesabı oluşturmaz. İstersen profilini, yerel kayıtlarını ve takip durumunu tek adımda temizleyebilirsin.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(
                        onClick = { showDeleteConfirmation = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Tüm verileri sil", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            DeveloperToolsCard {
                SecondaryActionButton("Kart ekranını test et", onClick = onOpenIntervention)
                SecondaryActionButton("4 günlük demo verisi yükle", onClick = onLoadDemoData)
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Tüm Eşik verileri silinsin mi?") },
            text = {
                Text(
                    "Profilin, yerel müdahale kayıtların ve takip durumu bu cihazdan silinecek. Bu işlem geri alınamaz.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onClearData()
                    },
                ) {
                    Text("Sil", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Vazgeç")
                }
            },
        )
    }
}
