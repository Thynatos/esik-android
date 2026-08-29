package com.thynatos.esik.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thynatos.esik.data.DailyReport

@Composable
fun DailyReportScreen(
    report: DailyReport,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Günlük rapor", style = MaterialTheme.typography.headlineMedium)

        ReportCard(title = "Sayılar") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                NumberItem("Kullanım", "${report.totalUsageMinutes} dk")
                NumberItem("Hedef", "${report.limitMinutes} dk")
                NumberItem("Kayıt", report.interventionCount.toString())
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                NumberItem("Yine de gir", report.continuedCount.toString())
                NumberItem("Vazgeçtim", report.stoppedCount.toString())
            }
        }

        if (report.insufficientData) {
            ReportCard(title = "Gözlem") {
                Text("Yeterli veri yok")
                Text("Bugün için en az 7 kayıt gerekiyor.")
            }
        } else {
            ReportCard(title = "Bir soru") {
                Text(report.observationQuestion)
            }
            ReportCard(title = "Yarın için tek mikro-adım") {
                Text(report.microStep)
            }
        }

        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Ana ekrana dön")
        }
    }
}

@Composable
private fun ReportCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun NumberItem(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}
