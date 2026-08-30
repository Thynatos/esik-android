package com.thynatos.esik.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thynatos.esik.data.UserProfile
import com.thynatos.esik.ui.components.DeveloperSection
import com.thynatos.esik.ui.components.LimitQuickPicker
import com.thynatos.esik.ui.components.MonitoringControlCard
import com.thynatos.esik.ui.components.QuickStatsRow
import com.thynatos.esik.ui.components.UsageHeroCard

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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // Welcome Header
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Merhaba, ${profile.name} 👋",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                ),
            )
            Text(
                text = "Bilinçli kullanım dengesi oluşturuyoruz.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // 1. Hero Card (Usage & Progress)
        UsageHeroCard(
            targetAppLabel = profile.targetAppLabel,
            usageMinutes = usageMinutes,
            limitMinutes = profile.dailyLimitMinutes,
            onOpenTargetApp = onOpenTargetApp,
        )

        // 2. Quick Stats Row (Interventions & Report)
        QuickStatsRow(
            recordCount = recordCount,
            onOpenReport = onOpenReport,
            onRefresh = onRefresh,
        )

        // 3. Limit Quick Picker & Custom Input
        LimitQuickPicker(
            currentLimitMinutes = profile.dailyLimitMinutes,
            onUpdateLimit = onUpdateLimit,
        )

        // 4. Background Monitoring & Permissions Card
        MonitoringControlCard(
            monitoringStarted = monitoringStarted,
            hasUsageAccess = hasUsageAccess,
            canDrawOverlays = canDrawOverlays,
            onStartMonitoring = onStartMonitoring,
            onStopMonitoring = onStopMonitoring,
            onOpenUsagePermission = onOpenUsagePermission,
            onOpenOverlayPermission = onOpenOverlayPermission,
        )

        // 5. Developer & Demo Tools Section (Collapsible)
        DeveloperSection(
            onOpenIntervention = onOpenIntervention,
            onLoadDemoData = onLoadDemoData,
            onClearData = onClearData,
        )

        Spacer(modifier = Modifier.height(12.dp))
    }
}
