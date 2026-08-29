package com.thynatos.esik.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.thynatos.esik.data.UserProfile

@Composable
fun HomeScreen(
    profile: UserProfile,
    usageMinutes: Int,
    recordCount: Int,
    monitoringStarted: Boolean,
    hasUsageAccess: Boolean,
    canDrawOverlays: Boolean,
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
    val progress = if (profile.dailyLimitMinutes <= 0) 0f else {
        (usageMinutes.toFloat() / profile.dailyLimitMinutes.toFloat()).coerceIn(0f, 1f)
    }
    val permissionsReady = hasUsageAccess && canDrawOverlays

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Merhaba, ${profile.name}", style = MaterialTheme.typography.headlineMedium)
        Text("${profile.targetAppLabel} için bugünkü durum")

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("$usageMinutes / ${profile.dailyLimitMinutes} dakika")
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Toplam kayıt: $recordCount", style = MaterialTheme.typography.bodySmall)
            }
        }

        OutlinedTextField(
            value = limitText,
            onValueChange = { limitText = it.filter(Char::isDigit).take(4) },
            label = { Text("Limiti değiştir — dakika") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { limitText.toIntOrNull()?.takeIf { it > 0 }?.let(onUpdateLimit) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Limiti kaydet")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = onRefresh, modifier = Modifier.weight(1f)) {
                Text("Yenile")
            }
            Button(onClick = onOpenReport, modifier = Modifier.weight(1f)) {
                Text("Rapor")
            }
        }

        HorizontalDivider()
        Text("Takip", style = MaterialTheme.typography.titleMedium)
        PermissionAction(
            label = "Kullanım erişimi",
            granted = hasUsageAccess,
            onOpen = onOpenUsagePermission,
        )
        PermissionAction(
            label = "Üste çizme izni",
            granted = canDrawOverlays,
            onOpen = onOpenOverlayPermission,
        )
        Button(
            onClick = if (monitoringStarted) onStopMonitoring else onStartMonitoring,
            enabled = monitoringStarted || permissionsReady,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (monitoringStarted) "Takibi durdur" else "Takibi başlat")
        }
        if (!permissionsReady) {
            Text(
                "Takibi başlatmak için iki özel izni de aç.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        OutlinedButton(onClick = onOpenTargetApp, modifier = Modifier.fillMaxWidth()) {
            Text("${profile.targetAppLabel} uygulamasını aç")
        }

        HorizontalDivider()
        Text("Hackathon testleri", style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = onOpenIntervention, modifier = Modifier.fillMaxWidth()) {
            Text("Kart ekranını test et")
        }
        OutlinedButton(onClick = onLoadDemoData, modifier = Modifier.fillMaxWidth()) {
            Text("4 günlük demo verisi yükle")
        }

        Spacer(Modifier.height(6.dp))
        TextButton(onClick = onClearData, modifier = Modifier.fillMaxWidth()) {
            Text("Tüm verileri sil")
        }
    }
}

@Composable
private fun PermissionAction(
    label: String,
    granted: Boolean,
    onOpen: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label)
            Text(
                if (granted) "Açık" else "Kapalı",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        OutlinedButton(onClick = onOpen) {
            Text(if (granted) "Ayarlar" else "İzin ver")
        }
    }
}
