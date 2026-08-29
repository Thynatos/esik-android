package com.thynatos.esik.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.thynatos.esik.data.UserProfile
import com.thynatos.esik.permissions.LaunchableApp

@Composable
fun OnboardingScreen(
    installedApps: List<LaunchableApp>,
    hasUsageAccess: Boolean,
    canDrawOverlays: Boolean,
    onOpenUsagePermission: () -> Unit,
    onOpenOverlayPermission: () -> Unit,
    onComplete: (UserProfile) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var department by rememberSaveable { mutableStateOf("") }
    var hobbiesText by rememberSaveable { mutableStateOf("") }
    var improvementArea by rememberSaveable { mutableStateOf("") }
    var reason by rememberSaveable { mutableStateOf("") }
    val initialTarget = remember(installedApps) {
        installedApps.firstOrNull { it.packageName == "com.instagram.android" }
            ?: installedApps.firstOrNull()
    }
    var targetAppLabel by rememberSaveable {
        mutableStateOf(initialTarget?.label ?: "Instagram")
    }
    var targetPackage by rememberSaveable {
        mutableStateOf(initialTarget?.packageName ?: "com.instagram.android")
    }
    var limitText by rememberSaveable { mutableStateOf("60") }
    var showAppPicker by remember { mutableStateOf(false) }
    var attemptedSubmit by rememberSaveable { mutableStateOf(false) }

    val hobbies = hobbiesText
        .split(',')
        .map(String::trim)
        .filter(String::isNotEmpty)
    val limit = limitText.toIntOrNull()
    val valid = name.isNotBlank() && reason.isNotBlank() && targetPackage.isNotBlank() &&
        limit != null && limit > 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Eşik", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Hesap yok. Şifre yok. Veriler bu cihazda kalır.",
            style = MaterialTheme.typography.bodyMedium,
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("İsim") },
            isError = attemptedSubmit && name.isBlank(),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = department,
            onValueChange = { department = it },
            label = { Text("Bölüm / alan") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = hobbiesText,
            onValueChange = { hobbiesText = it },
            label = { Text("Hobiler — virgülle ayır") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = improvementArea,
            onValueChange = { improvementArea = it },
            label = { Text("Kendini geliştirmek istediğin alan") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = reason,
            onValueChange = { reason = it },
            label = { Text("Neden azaltmak istiyorum?") },
            supportingText = { Text("Tek cümle, kendi kelimelerinle") },
            isError = attemptedSubmit && reason.isBlank(),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedButton(
            onClick = { showAppPicker = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Hedef uygulama: $targetAppLabel")
        }
        Text(
            targetPackage,
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
            value = limitText,
            onValueChange = { limitText = it.filter(Char::isDigit).take(4) },
            label = { Text("Günlük limit — dakika") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = attemptedSubmit && (limit == null || limit <= 0),
            modifier = Modifier.fillMaxWidth(),
        )

        HorizontalDivider()
        Text("İzinler", style = MaterialTheme.typography.titleMedium)
        PermissionRow(
            label = "Kullanım verisi erişimi",
            granted = hasUsageAccess,
            onClick = onOpenUsagePermission,
        )
        PermissionRow(
            label = "Diğer uygulamaların üzerine çizme",
            granted = canDrawOverlays,
            onClick = onOpenOverlayPermission,
        )

        Spacer(Modifier.height(4.dp))
        Button(
            onClick = {
                attemptedSubmit = true
                if (valid) {
                    onComplete(
                        UserProfile(
                            name = name.trim(),
                            department = department.trim(),
                            hobbies = hobbies,
                            improvementArea = improvementArea.trim(),
                            reason = reason.trim(),
                            targetAppLabel = targetAppLabel,
                            targetPackage = targetPackage,
                            dailyLimitMinutes = limit ?: 60,
                        ),
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Başla")
        }
    }

    if (showAppPicker) {
        AlertDialog(
            onDismissRequest = { showAppPicker = false },
            title = { Text("Hedef uygulamayı seç") },
            text = {
                if (installedApps.isEmpty()) {
                    Text("Başlatılabilir uygulama bulunamadı. Instagram varsayılanı kullanılacak.")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                        items(installedApps, key = { it.packageName }) { app ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        targetAppLabel = app.label
                                        targetPackage = app.packageName
                                        showAppPicker = false
                                    }
                                    .padding(vertical = 10.dp),
                            ) {
                                Text(app.label, style = MaterialTheme.typography.bodyLarge)
                                Text(app.packageName, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAppPicker = false }) { Text("Kapat") }
            },
        )
    }
}

@Composable
private fun PermissionRow(
    label: String,
    granted: Boolean,
    onClick: () -> Unit,
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
        OutlinedButton(onClick = onClick) {
            Text(if (granted) "Ayarlar" else "İzin ver")
        }
    }
}
