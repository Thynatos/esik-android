package com.thynatos.esik.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thynatos.esik.ai.AiGateway
import com.thynatos.esik.ai.CrisisFilter
import com.thynatos.esik.ai.MockAiGateway
import com.thynatos.esik.ai.SafetyLanguageValidator
import com.thynatos.esik.data.AiCard
import com.thynatos.esik.data.InterventionInput
import com.thynatos.esik.data.InterventionInputMethod
import com.thynatos.esik.data.UserChoice
import com.thynatos.esik.data.UserProfile
import com.thynatos.esik.voice.SpeechInput
import kotlinx.coroutines.launch

@Composable
fun InterventionScreen(
    profile: UserProfile,
    usageMinutes: Int,
    aiGateway: AiGateway,
    onChoice: (InterventionInput, AiCard, UserChoice) -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val quickStates = remember(profile.personalization) {
        profile.personalization.quickStatesOrDefault().take(3)
    }
    var userText by rememberSaveable { mutableStateOf("") }
    var inputMethod by remember { mutableStateOf(InterventionInputMethod.TEXT) }
    var customMode by rememberSaveable { mutableStateOf(false) }
    var selectedInput by remember { mutableStateOf<InterventionInput?>(null) }
    var generatedCard by remember { mutableStateOf<AiCard?>(null) }
    var crisisState by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            SpeechInput.extractText(result.data)?.let { spokenText ->
                userText = spokenText
                inputMethod = InterventionInputMethod.VOICE
                customMode = true
                generatedCard = null
                crisisState = false
                message = "Sesli yanıt metne çevrildi. Göndermeden önce düzenleyebilirsin."
            }
        }
    }

    fun requestCard(input: InterventionInput) {
        if (isLoading || input.text.isBlank()) return
        message = null
        generatedCard = null
        selectedInput = input

        if (CrisisFilter.check(input.text).isCrisisSignal) {
            crisisState = true
            return
        }

        crisisState = false
        isLoading = true
        scope.launch {
            val result = try {
                aiGateway.generateCard(profile, usageMinutes, input)
            } catch (_: Exception) {
                MockAiGateway().generateCard(profile, usageMinutes, input)
            }
            generatedCard = if (
                SafetyLanguageValidator.isDisplaySafe(result.question, result.alternative)
            ) {
                result
            } else {
                MockAiGateway().generateCard(profile, usageMinutes, input)
            }
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Eşik", style = MaterialTheme.typography.labelLarge)
        Text(
            "${profile.targetAppLabel}: bugün $usageMinutes dakika. Kendi hedefin ${profile.dailyLimitMinutes} dakika.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "Şu an seni burada tutan ne?",
            style = MaterialTheme.typography.headlineSmall,
        )

        if (crisisState) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Bunu tek başına taşımak zorunda değilsin. Yakınındaki acil yardım hizmetine, güvendiğin bir kişiye veya profesyonel desteğe şimdi ulaş. Bu metin AI servisine gönderilmedi.",
                    modifier = Modifier.padding(16.dp),
                )
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Kapat")
            }
        } else if (generatedCard == null) {
            quickStates.forEach { option ->
                Button(
                    onClick = {
                        requestCard(
                            InterventionInput(
                                text = option.label,
                                stateId = option.id,
                                stateLabel = option.label,
                                method = InterventionInputMethod.QUICK_REPLY,
                            ),
                        )
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        listOf(option.emoji, option.label)
                            .filter(String::isNotBlank)
                            .joinToString(" "),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        customMode = true
                        inputMethod = InterventionInputMethod.TEXT
                        message = null
                    },
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("✏️ Yaz")
                }
                OutlinedButton(
                    onClick = {
                        message = null
                        try {
                            voiceLauncher.launch(SpeechInput.createIntent("Şu an ne olduğunu anlat"))
                        } catch (_: ActivityNotFoundException) {
                            customMode = true
                            inputMethod = InterventionInputMethod.TEXT
                            message = "Bu telefonda sesli giriş kullanılamıyor; yazarak devam edebilirsin."
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("🎙 Anlat")
                }
            }

            if (customMode) {
                OutlinedTextField(
                    value = userText,
                    onValueChange = {
                        userText = it
                        if (inputMethod != InterventionInputMethod.VOICE) {
                            inputMethod = InterventionInputMethod.TEXT
                        }
                        message = null
                    },
                    label = { Text("Kendi kelimelerinle anlat") },
                    minLines = 3,
                    maxLines = 7,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = {
                        val text = userText.trim()
                        if (text.isBlank()) {
                            message = "Devam etmek için kısa bir cümle yaz ya da anlat."
                        } else {
                            requestCard(
                                InterventionInput(
                                    text = text,
                                    method = inputMethod,
                                ),
                            )
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Yanıtı değerlendir")
                }
            }

            if (isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                    Text("Sana uygun küçük bir seçenek hazırlanıyor…")
                }
            }
            message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            OutlinedButton(
                onClick = onBack,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Geri")
            }
        } else {
            val card = generatedCard ?: return@Column
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(card.question, style = MaterialTheme.typography.titleMedium)
                    Text(card.alternative)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        onChoice(
                            selectedInput ?: InterventionInput(userText.trim()),
                            card,
                            UserChoice.STOPPED,
                        )
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Deneyeceğim")
                }
                Button(
                    onClick = {
                        onChoice(
                            selectedInput ?: InterventionInput(userText.trim()),
                            card,
                            UserChoice.CONTINUE,
                        )
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Yine de gir")
                }
            }
            OutlinedButton(
                onClick = {
                    generatedCard = null
                    selectedInput = null
                    customMode = true
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Yanıtımı değiştir")
            }
        }
    }
}
