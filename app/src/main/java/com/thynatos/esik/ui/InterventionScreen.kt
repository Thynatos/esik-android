package com.thynatos.esik.ui

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thynatos.esik.ai.AiGateway
import com.thynatos.esik.ai.CrisisFilter
import com.thynatos.esik.ai.MockAiGateway
import com.thynatos.esik.ai.SafetyLanguageValidator
import com.thynatos.esik.data.AiCard
import com.thynatos.esik.data.UserChoice
import com.thynatos.esik.data.UserProfile

@Composable
fun InterventionScreen(
    profile: UserProfile,
    usageMinutes: Int,
    aiGateway: AiGateway,
    onChoice: (String, UserChoice) -> Unit,
    onBack: () -> Unit,
) {
    var userText by rememberSaveable { mutableStateOf("") }
    var generatedCard by remember { mutableStateOf<AiCard?>(null) }
    var crisisState by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Eşik", style = MaterialTheme.typography.labelLarge)
        Text(
            "Bugün $usageMinutes dakika oldu. Hedefin ${profile.dailyLimitMinutes}'tı. Şu an ne oluyor?",
            style = MaterialTheme.typography.headlineSmall,
        )

        OutlinedTextField(
            value = userText,
            onValueChange = {
                userText = it
                generatedCard = null
                crisisState = false
            },
            label = { Text("Kendi kelimelerinle yaz") },
            minLines = 4,
            enabled = generatedCard == null && !crisisState,
            modifier = Modifier.fillMaxWidth(),
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
            Button(
                onClick = {
                    val text = userText.trim()
                    if (text.isNotEmpty()) {
                        if (CrisisFilter.check(text).isCrisisSignal) {
                            crisisState = true
                        } else {
                            val result = runCatching {
                                aiGateway.generateCard(profile, usageMinutes, text)
                            }.getOrElse {
                                MockAiGateway().generateCard(profile, usageMinutes, text)
                            }
                            generatedCard = if (
                                SafetyLanguageValidator.isDisplaySafe(
                                    result.question,
                                    result.alternative,
                                )
                            ) {
                                result
                            } else {
                                MockAiGateway().generateCard(profile, usageMinutes, text)
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Yanıtı gönder")
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Geri")
            }
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(generatedCard?.question.orEmpty(), style = MaterialTheme.typography.titleMedium)
                    Text(generatedCard?.alternative.orEmpty())
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = { onChoice(userText.trim(), UserChoice.STOPPED) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Vazgeçtim")
                }
                Button(
                    onClick = { onChoice(userText.trim(), UserChoice.CONTINUE) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Yine de gir")
                }
            }
        }
    }
}
