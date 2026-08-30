package com.thynatos.esik.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.thynatos.esik.data.DailyReport
import com.thynatos.esik.ui.components.EsikCard
import com.thynatos.esik.ui.components.EsikScreen
import com.thynatos.esik.ui.components.EsikTopBar
import com.thynatos.esik.ui.components.PrimaryActionButton
import com.thynatos.esik.ui.components.SectionTitle
import com.thynatos.esik.ui.components.StatItem
import com.thynatos.esik.ui.theme.EsikSpacing

@Composable
fun DailyReportScreen(
    report: DailyReport,
    onBack: () -> Unit,
) {
    EsikScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = EsikSpacing.xLarge, vertical = EsikSpacing.xxLarge),
            verticalArrangement = Arrangement.spacedBy(EsikSpacing.xLarge),
        ) {
            EsikTopBar(
                eyebrow = "BUGÜN",
                title = "Günlük yansıman",
                subtitle = "Önce cihazında hesaplanan sayılar, ardından kısa bir düşünme alanı.",
            )

            SectionTitle(
                title = "Bugünün sayıları",
                supportingText = "Bu değerlerin tamamı cihazında hesaplandı.",
            )
            EsikCard {
                Column(verticalArrangement = Arrangement.spacedBy(EsikSpacing.large)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(EsikSpacing.xLarge),
                    ) {
                        StatItem(
                            value = "${report.totalUsageMinutes} dk",
                            label = "kullanım",
                            modifier = Modifier.weight(1f),
                        )
                        StatItem(
                            value = "${report.limitMinutes} dk",
                            label = "kendi hedefin",
                            modifier = Modifier.weight(1f),
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(EsikSpacing.medium),
                    ) {
                        StatItem(
                            value = report.interventionCount.toString(),
                            label = "kayıt",
                            modifier = Modifier.weight(1f),
                        )
                        StatItem(
                            value = report.continuedCount.toString(),
                            label = "devam kararı",
                            modifier = Modifier.weight(1f),
                        )
                        StatItem(
                            value = report.stoppedCount.toString(),
                            label = "deneme kararı",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            if (report.insufficientData) {
                SectionTitle(
                    title = "Yansıma için yeterli veri yok",
                    supportingText = "Bugün için 7 kayıt oluştuğunda gözlem sorusu ve tek bir mikro-adım hazırlanır.",
                )
                EsikCard(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                    Column(verticalArrangement = Arrangement.spacedBy(EsikSpacing.medium)) {
                        Text(
                            "${report.interventionCount.coerceAtMost(7)} / 7 kayıt",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        LinearProgressIndicator(
                            progress = {
                                (report.interventionCount.toFloat() / 7f).coerceIn(0f, 1f)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surface,
                        )
                        Text(
                            "Bu durum bir değerlendirme değil; yalnızca günlük yansıma için veri kuralıdır.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                SectionTitle(
                    title = "Düşünmek için",
                    supportingText = "Aşağıdaki metinler AI tarafından hazırlandı; sayılar ve hedef değişmedi.",
                )
                EsikCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                    Column(verticalArrangement = Arrangement.spacedBy(EsikSpacing.medium)) {
                        Text(
                            "BİR SORU",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            report.observationQuestion,
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }
                }
                EsikCard {
                    Column(verticalArrangement = Arrangement.spacedBy(EsikSpacing.medium)) {
                        Text(
                            "YARIN İÇİN TEK MİKRO-ADIM",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(report.microStep, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            PrimaryActionButton("Ana sayfaya dön", onClick = onBack)
        }
    }
}
