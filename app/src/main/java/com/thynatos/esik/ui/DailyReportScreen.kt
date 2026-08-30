package com.thynatos.esik.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thynatos.esik.data.DailyReport
import com.thynatos.esik.ui.theme.ForestGreenPrimaryContainer
import com.thynatos.esik.ui.theme.ForestGreenOnPrimaryContainer
import com.thynatos.esik.ui.theme.StatusHealthy
import com.thynatos.esik.ui.theme.StatusOverLimit

@Composable
fun DailyReportScreen(
    report: DailyReport,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Screen Title
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Günlük Rapor 📊",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                ),
            )
            Text(
                text = "Bugünkü dijital kullanım ve müdahale özeti",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Metrics Summary Card
        ReportCard(title = "Kullanım ve Müdahaleler") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                NumberItem("Toplam Kullanım", "${report.totalUsageMinutes} dk", MaterialTheme.colorScheme.primary)
                NumberItem("Hedef Limit", "${report.limitMinutes} dk", MaterialTheme.colorScheme.onSurface)
                NumberItem("Toplam Kayıt", "${report.interventionCount}", MaterialTheme.colorScheme.secondary)
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(StatusHealthy.copy(alpha = 0.12f))
                        .padding(12.dp),
                ) {
                    Column {
                        Text("${report.stoppedCount}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = StatusHealthy)
                        Text("Vazgeçildi (Başarı)", style = MaterialTheme.typography.bodySmall, color = StatusHealthy)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(StatusOverLimit.copy(alpha = 0.12f))
                        .padding(12.dp),
                ) {
                    Column {
                        Text("${report.continuedCount}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = StatusOverLimit)
                        Text("Yine de Girildi", style = MaterialTheme.typography.bodySmall, color = StatusOverLimit)
                    }
                }
            }
        }

        if (report.insufficientData) {
            ReportCard(title = "Gözlem & İçgörü") {
                Text(
                    text = "Yeterli veri yok",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Anlamlı bir günlük analiz için bugün en az 7 kayıt gerekiyor. Gün boyu kullanım devam ettikçe burası güncellenecektir.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            ReportCard(title = "💡 Günün Gözlem Sorusu") {
                Text(
                    text = report.observationQuestion,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
            }

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = ForestGreenPrimaryContainer.copy(alpha = 0.7f),
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "🌱 Yarın İçin Tek Mikro-Adım",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreenOnPrimaryContainer,
                    )
                    Text(
                        text = report.microStep,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ForestGreenOnPrimaryContainer,
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onBack,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Ana Ekrana Dön")
        }
    }
}

@Composable
private fun ReportCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun NumberItem(label: String, value: String, color: Color) {
    Column {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
