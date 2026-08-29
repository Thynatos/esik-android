package com.thynatos.esik

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.thynatos.esik.ai.AiGateway
import com.thynatos.esik.data.DailyReport
import com.thynatos.esik.data.DemoDataSeeder
import com.thynatos.esik.data.EsikRepository
import com.thynatos.esik.data.InterventionRecord
import com.thynatos.esik.data.UserChoice
import com.thynatos.esik.data.UserProfile
import com.thynatos.esik.monitor.UsageMonitorService
import com.thynatos.esik.permissions.InstalledAppLoader
import com.thynatos.esik.permissions.PermissionNavigator
import com.thynatos.esik.ui.DailyReportScreen
import com.thynatos.esik.ui.HomeScreen
import com.thynatos.esik.ui.InterventionScreen
import com.thynatos.esik.ui.OnboardingScreen
import com.thynatos.esik.usage.UsageStatsReader
import java.time.LocalDate

private enum class AppScreen {
    ONBOARDING,
    HOME,
    INTERVENTION,
    REPORT,
}

@Composable
fun EsikApp(
    repository: EsikRepository,
    aiGateway: AiGateway,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val usageReader = remember(context) { UsageStatsReader(context) }
    val installedApps = remember(context) { InstalledAppLoader.load(context) }

    var profile by remember { mutableStateOf(repository.loadProfile()) }
    var records by remember { mutableStateOf(repository.loadRecords()) }
    var currentUsageMinutes by remember { mutableIntStateOf(0) }
    var interventionUsageMinutes by remember { mutableIntStateOf(0) }
    var monitoringStarted by rememberSaveable {
        mutableStateOf(UsageMonitorService.isMonitoringEnabled(context))
    }
    var permissionRefreshNonce by remember { mutableIntStateOf(0) }
    var report by remember { mutableStateOf<DailyReport?>(null) }
    var screen by rememberSaveable {
        mutableStateOf(if (profile == null) AppScreen.ONBOARDING else AppScreen.HOME)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionRefreshNonce++
                monitoringStarted = UsageMonitorService.isMonitoringEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val hasUsageAccess = remember(context, permissionRefreshNonce) {
        PermissionNavigator.hasUsageAccess(context)
    }
    val canDrawOverlays = remember(context, permissionRefreshNonce) {
        PermissionNavigator.canDrawOverlays(context)
    }

    LaunchedEffect(profile, permissionRefreshNonce) {
        currentUsageMinutes = profile?.let {
            usageReader.todayUsageMinutes(it.targetPackage)
        } ?: 0
        records = repository.loadRecords()
    }

    LaunchedEffect(profile, screen, report) {
        if (profile == null && screen != AppScreen.ONBOARDING) {
            screen = AppScreen.ONBOARDING
        } else if (screen == AppScreen.REPORT && report == null) {
            screen = AppScreen.HOME
        }
    }

    when (screen) {
        AppScreen.ONBOARDING -> OnboardingScreen(
            installedApps = installedApps,
            hasUsageAccess = hasUsageAccess,
            canDrawOverlays = canDrawOverlays,
            onOpenUsagePermission = {
                PermissionNavigator.openUsageAccessSettings(context)
            },
            onOpenOverlayPermission = {
                PermissionNavigator.openOverlaySettings(context)
            },
            onComplete = { newProfile ->
                repository.saveProfile(newProfile)
                UsageMonitorService.resetCooldown(context)
                profile = newProfile
                report = null
                currentUsageMinutes = usageReader.todayUsageMinutes(newProfile.targetPackage)
                screen = AppScreen.HOME
            },
        )

        AppScreen.HOME -> profile?.let { activeProfile ->
            HomeScreen(
                profile = activeProfile,
                usageMinutes = currentUsageMinutes,
                recordCount = records.size,
                monitoringStarted = monitoringStarted,
                hasUsageAccess = hasUsageAccess,
                canDrawOverlays = canDrawOverlays,
                onRefresh = {
                    permissionRefreshNonce++
                    currentUsageMinutes = usageReader.todayUsageMinutes(activeProfile.targetPackage)
                    records = repository.loadRecords()
                },
                onOpenUsagePermission = {
                    PermissionNavigator.openUsageAccessSettings(context)
                },
                onOpenOverlayPermission = {
                    PermissionNavigator.openOverlaySettings(context)
                },
                onUpdateLimit = { limit ->
                    val updated = activeProfile.copy(dailyLimitMinutes = limit)
                    repository.saveProfile(updated)
                    UsageMonitorService.resetCooldown(context)
                    profile = updated
                    report = null
                },
                onStartMonitoring = {
                    UsageMonitorService.start(context)
                    monitoringStarted = true
                },
                onStopMonitoring = {
                    UsageMonitorService.stop(context)
                    monitoringStarted = false
                },
                onOpenTargetApp = {
                    launchPackage(context, activeProfile.targetPackage)
                },
                onOpenIntervention = {
                    interventionUsageMinutes = maxOf(
                        currentUsageMinutes,
                        activeProfile.dailyLimitMinutes + 18,
                    )
                    screen = AppScreen.INTERVENTION
                },
                onOpenReport = {
                    val allRecords = repository.loadRecords()
                    val today = LocalDate.now()
                    val todayRecords = allRecords.filter { it.occursOn(today) }
                    records = allRecords
                    report = aiGateway.generateDailyReport(
                        profile = activeProfile,
                        records = todayRecords,
                        currentUsageMinutes = currentUsageMinutes,
                    )
                    screen = AppScreen.REPORT
                },
                onLoadDemoData = {
                    val seeded = DemoDataSeeder.records()
                    repository.replaceRecords(seeded)
                    records = seeded
                    report = null
                    if (currentUsageMinutes == 0) {
                        currentUsageMinutes = activeProfile.dailyLimitMinutes + 34
                    }
                },
                onClearData = {
                    UsageMonitorService.stop(context)
                    UsageMonitorService.resetCooldown(context)
                    monitoringStarted = false
                    repository.clearAll()
                    profile = null
                    records = emptyList()
                    report = null
                    currentUsageMinutes = 0
                    screen = AppScreen.ONBOARDING
                },
            )
        }

        AppScreen.INTERVENTION -> profile?.let { activeProfile ->
            InterventionScreen(
                profile = activeProfile,
                usageMinutes = interventionUsageMinutes,
                aiGateway = aiGateway,
                onChoice = { text, choice ->
                    val record = InterventionRecord(
                        timestampEpochMillis = System.currentTimeMillis(),
                        usageMinutes = interventionUsageMinutes,
                        text = text,
                        choice = choice,
                    )
                    repository.appendRecord(record)
                    records = records + record
                    report = null
                    screen = AppScreen.HOME
                    if (choice == UserChoice.CONTINUE) {
                        launchPackage(context, activeProfile.targetPackage)
                    }
                },
                onBack = { screen = AppScreen.HOME },
            )
        }

        AppScreen.REPORT -> report?.let { activeReport ->
            DailyReportScreen(
                report = activeReport,
                onBack = { screen = AppScreen.HOME },
            )
        }
    }
}

private fun launchPackage(context: Context, packageName: String) {
    val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (launchIntent != null) {
        runCatching { context.startActivity(launchIntent) }
    }
}
