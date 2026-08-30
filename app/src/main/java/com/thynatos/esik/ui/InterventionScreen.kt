package com.thynatos.esik.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thynatos.esik.ai.AiGateway
import com.thynatos.esik.ai.CrisisFilter
import com.thynatos.esik.ai.MockAiGateway
import com.thynatos.esik.ai.SafetyLanguageValidator
import com.thynatos.esik.data.AiCard
import com.thynatos.esik.data.UserChoice
import com.thynatos.esik.data.UserProfile
import com.thynatos.esik.ui.theme.AmberTertiaryContainer
import com.thynatos.esik.ui.theme.AmberOnTertiaryContainer
import com.thynatos.esik.ui.theme.StatusOverLimit
import com.thynatos.esik.ui.theme.StatusOverLimitBg

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
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // App Tag / Header
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(AmberTertiaryContainer)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                text = "Eşik Farkındalık Kartı",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = AmberOnTertiaryContainer,
            )
        }

        Text(
            text = "Bugün $usageMinutes dakika oldu. Hedefin ${profile.dailyLimitMinutes}'tı.",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        )
        Text(
            text = "Şu an ne oluyor? Birkaç kelimeyle düşünceni yaz:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = userText,
            onValueChange = {
                userText = it
                generatedCard = null
                crisisState = false
            },
            placeholder = { Text("Örn: Sıkıldım, sadece 5 dakika bakacaktım...") },
            minLines = 4,
            shape = RoundedCornerShape(16.dp),
            enabled = generatedCard == null && !crisisState,
            modifier = Modifier.fillMaxWidth(),
        )

        if (crisisState) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = StatusOverLimitBg,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Bunu tek başına taşımak zorunda değilsin. Yakınındaki acil yardım hizmetine, güvendiğin bir kişiye veya profesyonel desteğe şimdi ulaş. Bu metin AI servisine gönderilmedi.",
                    color = StatusOverLimit,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(18.dp),
                )
            }
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
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
                shape = RoundedCornerShape(12.dp),
                enabled = userText.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Yanıtı Gönder & Düşün")
            }
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Geri")
            }
        } else {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = generatedCard?.question.orEmpty(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = generatedCard?.alternative.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = { onChoice(userText.trim(), UserChoice.STOPPED) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Vazgeçtim 🛑")
                }
                Button(
                    onClick = { onChoice(userText.trim(), UserChoice.CONTINUE) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Yine de Gir ➔")
                }
            }
        }
    }
}
