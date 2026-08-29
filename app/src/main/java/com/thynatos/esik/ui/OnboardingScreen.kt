package com.thynatos.esik.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.thynatos.esik.ai.AiGateway
import com.thynatos.esik.ai.MockAiGateway
import com.thynatos.esik.data.PersonalizationProfile
import com.thynatos.esik.data.ProfileIntake
import com.thynatos.esik.data.UserProfile
import com.thynatos.esik.permissions.LaunchableApp
import com.thynatos.esik.voice.SpeechInput
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    installedApps: List<LaunchableApp>,
    hasUsageAccess: Boolean,
    canDrawOverlays: Boolean,
    aiGateway: AiGateway,
    onOpenUsagePermission: () -> Unit,
    onOpenOverlayPermission: () -> Unit,
    onComplete: (UserProfile) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf("") }
    var department by rememberSaveable { mutableStateOf("") }
    var biography by rememberSaveable { mutableStateOf("") }
    var hobbiesText by rememberSaveable { mutableStateOf("") }
    var improvementArea by rememberSaveable { mutableStateOf("") }
    var reason by rememberSaveable { mutableStateOf("") }
    var generatedProfile by remember { mutableStateOf<PersonalizationProfile?>(null) }
    var isGeneratingProfile by remember { mutableStateOf(false) }
    var profileMessage by remember { mutableStateOf<String?>(null) }

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

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            SpeechInput.extractText(result.data)?.let { spokenText ->
                biography = listOf(biography.trim(), spokenText)
                    .filter(String::isNotBlank)
                    .joinToString(" ")
                generatedProfile = null
                profileMessage = "Sesli anlatım metne eklendi. İstersen düzenleyebilirsin."
            }
        }
    }

    val hobbies = hobbiesText
        .split(',')
        .map(String::trim)
        .filter(String::isNotEmpty)
    val limit = limitText.toIntOrNull()
    val valid = name.isNotBlank() && biography.isNotBlank() && targetPackage.isNotBlank() &&
        limit != null && limit > 0

    fun intake(): ProfileIntake = ProfileIntake(
        name = name.trim(),
        department = department.trim(),
        biography = biography.trim(),
        hobbies = hobbies,
        improvementArea = improvementArea.trim(),
        reason = reason.trim(),
    )

    fun generateProfile(onReady: (PersonalizationProfile) -> Unit = {}) {
        if (!valid || isGeneratingProfile) return
        isGeneratingProfile = true
        profileMessage = null
        scope.launch {
            val generated = try {
                aiGateway.generateProfile(intake())
            } catch (_: Exception) {
                MockAiGateway().generateProfile(intake())
            }
            generatedProfile = generated
            isGeneratingProfile = false
            profileMessage = "Profil özeti cihazında saklanır. Canlı AI açıksa üretim sırasında anlatım metni Anthropic API'ye gönderilir."
            onReady(generated)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Eşik", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Hesap yok. Kayıtlar cihazında tutulur. Canlı AI açıksa ilgili metin Anthropic API'ye; sesli giriş ise telefonundaki konuşma tanıma hizmetine gönderilebilir.",
            style = MaterialTheme.typography.bodyMedium,
        )

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                generatedProfile = null
            },
            label = { Text("İsim") },
            isError = attemptedSubmit && name.isBlank(),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = department,
            onValueChange = {
                department = it
                generatedProfile = null
            },
            label = { Text("Bölüm / alan — isteğe bağlı") },
            modifier = Modifier.fillMaxWidth(),
        )

        Text("Seni biraz tanıyalım", style = MaterialTheme.typography.titleLarge)
        Text(
            "Telefonu neden daha bilinçli kullanmak istediğini, boş zamanlarında neleri sevdiğini ve son zamanlarda seni neyin zorladığını anlat.",
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = biography,
            onValueChange = {
                biography = it
                generatedProfile = null
                profileMessage = null
            },
            label = { Text("Konuşarak ya da yazarak anlat") },
            minLines = 5,
            maxLines = 10,
            isError = attemptedSubmit && biography.isBlank(),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = {
                    profileMessage = null
                    try {
                        voiceLauncher.launch(
                            SpeechInput.createIntent("Kendinden ve hedeflerinden bahset"),
                        )
                    } catch (_: ActivityNotFoundException) {
                        profileMessage = "Bu telefonda sesli giriş kullanılamıyor; metinle devam edebilirsin."
                    }
                },
                modifier = Modifier.weight(1f),
            ) {
                Text("🎙 Anlat")
            }
            OutlinedButton(
                onClick = { biography = biography.trim() },
                modifier = Modifier.weight(1f),
            ) {
                Text("✏️ Yazarak devam")
            }
        }

        OutlinedTextField(
            value = hobbiesText,
            onValueChange = {
                hobbiesText = it
                generatedProfile = null
            },
            label = { Text("Sevdiğin aktiviteler — isteğe bağlı") },
            supportingText = { Text("Örn. gitar, koşu, kitap, podcast") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = improvementArea,
            onValueChange = {
                improvementArea = it
                generatedProfile = null
            },
            label = { Text("Geliştirmek istediğin alan — isteğe bağlı") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = reason,
            onValueChange = {
                reason = it
                generatedProfile = null
            },
            label = { Text("Kendi hedefin — isteğe bağlı kısa cümle") },
            supportingText = { Text("Örn. gece daha rahat uyumak istiyorum") },
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = { generateProfile() },
            enabled = valid && !isGeneratingProfile,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isGeneratingProfile) {
                CircularProgressIndicator(modifier = Modifier.height(22.dp), strokeWidth = 2.dp)
            } else {
                Text("AI profilimi oluştur")
            }
        }
        profileMessage?.let {
            Text(it, style = MaterialTheme.typography.bodySmall)
        }

        generatedProfile?.let { personalization ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Profil özeti", style = MaterialTheme.typography.titleMedium)
                    ProfileLine("Hedefler", personalization.goals)
                    ProfileLine("Sık karşılaşılan durumlar", personalization.recurringContexts)
                    ProfileLine("Sana uygun alternatifler", personalization.preferredActivities)
                    Text(
                        "Hızlı seçenekler: " + personalization.quickStatesOrDefault()
                            .take(3)
                            .joinToString(" · ") { option ->
                                listOf(option.emoji, option.label)
                                    .filter(String::isNotBlank)
                                    .joinToString(" ")
                            },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "Bu bir tanı değil; yalnızca kendi anlattıklarından çıkarılan düzenlenebilir bir başlangıç profili.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        HorizontalDivider()
        Text("Hedef uygulama", style = MaterialTheme.typography.titleMedium)
        OutlinedButton(
            onClick = { showAppPicker = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Hedef uygulama: $targetAppLabel")
        }
        Text(targetPackage, style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(
            value = limitText,
            onValueChange = {
                limitText = it.filter(Char::isDigit).take(4)
            },
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
                    val finish: (PersonalizationProfile) -> Unit = { personalization ->
                        val finalReason = reason.trim().ifBlank {
                            personalization.goals.firstOrNull().orEmpty().ifBlank {
                                "Telefonu daha bilinçli kullanmak istiyorum"
                            }
                        }
                        onComplete(
                            UserProfile(
                                name = name.trim(),
                                department = department.trim(),
                                hobbies = hobbies.ifEmpty {
                                    personalization.preferredActivities.take(3)
                                },
                                improvementArea = improvementArea.trim().ifBlank {
                                    personalization.goals.firstOrNull().orEmpty()
                                },
                                reason = finalReason,
                                targetAppLabel = targetAppLabel,
                                targetPackage = targetPackage,
                                dailyLimitMinutes = requireNotNull(limit),
                                biography = biography.trim(),
                                personalization = personalization,
                            ),
                        )
                    }
                    generatedProfile?.let(finish) ?: generateProfile(finish)
                }
            },
            enabled = !isGeneratingProfile,
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
private fun ProfileLine(label: String, values: List<String>) {
    if (values.isNotEmpty()) {
        Text("$label: ${values.joinToString(" · ")}")
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
