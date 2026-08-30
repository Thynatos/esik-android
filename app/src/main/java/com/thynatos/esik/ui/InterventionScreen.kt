package com.thynatos.esik.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thynatos.esik.ai.AiGateway
import com.thynatos.esik.ai.CrisisFilter
import com.thynatos.esik.ai.MockAiGateway
import com.thynatos.esik.ai.SafetyLanguageValidator
import com.thynatos.esik.data.AiCard
import com.thynatos.esik.data.AiCardSource
import com.thynatos.esik.data.CardProvenance
import com.thynatos.esik.data.InterventionInput
import com.thynatos.esik.data.InterventionInputMethod
import com.thynatos.esik.data.InterventionRecord
import com.thynatos.esik.data.UserChoice
import com.thynatos.esik.data.UserProfile
import com.thynatos.esik.ui.components.EsikCard
import com.thynatos.esik.ui.components.EsikScreen
import com.thynatos.esik.ui.components.EsikTopBar
import com.thynatos.esik.ui.components.PrimaryActionButton
import com.thynatos.esik.ui.components.QuickStateButton
import com.thynatos.esik.ui.components.SecondaryActionButton
import com.thynatos.esik.ui.components.SectionTitle
import com.thynatos.esik.ui.components.StatItem
import com.thynatos.esik.ui.theme.EsikSpacing
import com.thynatos.esik.voice.SpeechInput
import kotlinx.coroutines.launch

@Composable
fun InterventionScreen(
    profile: UserProfile,
    usageMinutes: Int,
    aiGateway: AiGateway,
    history: List<InterventionRecord>,
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
                aiGateway.generateCard(profile, usageMinutes, input, history)
            } catch (_: Exception) {
                MockAiGateway().generateCard(profile, usageMinutes, input, history)
            }
            generatedCard = if (
                SafetyLanguageValidator.isDisplaySafe(result.question, result.alternative)
            ) {
                result
            } else {
                MockAiGateway().generateCard(profile, usageMinutes, input, history)
            }
            isLoading = false
        }
    }

    EsikScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = EsikSpacing.xLarge, vertical = EsikSpacing.xxLarge),
            verticalArrangement = Arrangement.spacedBy(EsikSpacing.xLarge),
        ) {
            EsikTopBar(
                title = "Kısa bir durak",
                subtitle = "Karar senin; Eşik yalnızca düşünmek için alan açar.",
            )
            EsikCard(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(EsikSpacing.section),
                ) {
                    StatItem(
                        value = "$usageMinutes dk",
                        label = "${profile.targetAppLabel} bugün",
                        modifier = Modifier.weight(1f),
                    )
                    StatItem(
                        value = "${profile.dailyLimitMinutes} dk",
                        label = "kendi hedefin",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (crisisState) {
                SectionTitle(
                    title = "Şu an destek önemli",
                    supportingText = "Bu yanıt herhangi bir AI servisine gönderilmedi.",
                )
                EsikCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
                    Column(verticalArrangement = Arrangement.spacedBy(EsikSpacing.medium)) {
                        Text(
                            "Bunu tek başına taşımak zorunda değilsin.",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Text(
                            "Yakınındaki acil yardım hizmetine, güvendiğin bir kişiye veya profesyonel desteğe şimdi ulaş.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
                SecondaryActionButton("Kapat", onClick = onBack)
            } else if (generatedCard == null) {
                SectionTitle(
                    title = "Şu an seni burada tutan ne?",
                    supportingText = "En yakın seçeneğe dokunabilir ya da kendi cümlelerinle anlatabilirsin.",
                )
                Column(verticalArrangement = Arrangement.spacedBy(EsikSpacing.medium)) {
                    quickStates.forEach { option ->
                        QuickStateButton(
                            label = option.label,
                            emoji = option.emoji,
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
                        )
                    }
                }

                EsikCard {
                    Column(verticalArrangement = Arrangement.spacedBy(EsikSpacing.medium)) {
                        Text(
                            "Başka bir şey mi var?",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(EsikSpacing.small),
                        ) {
                            OutlinedButton(
                                onClick = {
                                    customMode = true
                                    inputMethod = InterventionInputMethod.TEXT
                                    message = null
                                },
                                enabled = !isLoading,
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.medium,
                            ) {
                                Text("✏️ Yaz")
                            }
                            OutlinedButton(
                                onClick = {
                                    message = null
                                    try {
                                        voiceLauncher.launch(
                                            SpeechInput.createIntent("Şu an ne olduğunu anlat"),
                                        )
                                    } catch (_: ActivityNotFoundException) {
                                        customMode = true
                                        inputMethod = InterventionInputMethod.TEXT
                                        message = "Bu telefonda sesli giriş kullanılamıyor; yazarak devam edebilirsin."
                                    }
                                },
                                enabled = !isLoading,
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.medium,
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
                            PrimaryActionButton(
                                text = "Yanıtı değerlendir",
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
                            )
                        }
                    }
                }

                if (isLoading) {
                    EsikCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(EsikSpacing.medium),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                            )
                            Column {
                                Text(
                                    "Küçük bir seçenek hazırlanıyor",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    "Bu birkaç saniye sürebilir.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                message?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                SecondaryActionButton(
                    text = "Şimdi değil",
                    onClick = onBack,
                    enabled = !isLoading,
                )
            } else {
                generatedCard?.let { card ->
                    SectionTitle(
                        title = "Bir an için bunu deneyebilirsin",
                        supportingText = "Bu öneri karar vermene destek olmak için hazırlandı.",
                    )
                    EsikCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                        Column(verticalArrangement = Arrangement.spacedBy(EsikSpacing.large)) {
                            Text(
                                "DÜŞÜNMEK İÇİN",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                card.question,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                            ) {
                                Column(
                                    modifier = Modifier.padding(EsikSpacing.large),
                                    verticalArrangement = Arrangement.spacedBy(EsikSpacing.xSmall),
                                ) {
                                    Text(
                                        "Küçük adım",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(card.alternative, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    }
                    CardProvenanceDisclosure(card.provenance)
                    PrimaryActionButton(
                        text = "Bunu deneyeceğim",
                        onClick = {
                            onChoice(
                                selectedInput ?: InterventionInput(userText.trim()),
                                card,
                                UserChoice.STOPPED,
                            )
                        },
                    )
                    SecondaryActionButton(
                        text = "Yine de devam et",
                        onClick = {
                            onChoice(
                                selectedInput ?: InterventionInput(userText.trim()),
                                card,
                                UserChoice.CONTINUE,
                            )
                        },
                    )
                    TextButton(
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
    }
}

/**
 * Shows the locally compiled constraints the card was produced under.
 *
 * Eşik claims that the model never sets the threshold and never invents personal facts. This makes
 * that claim inspectable instead of asking the user to take it on trust. Everything shown here was
 * decided by the application, not by the model.
 */
@Composable
private fun CardProvenanceDisclosure(provenance: CardProvenance) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(EsikSpacing.small)) {
        TextButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (expanded) "Açıklamayı kapat" else "Bu öneri nasıl oluştu?")
        }
        AnimatedVisibility(visible = expanded) {
            EsikCard(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                Column(verticalArrangement = Arrangement.spacedBy(EsikSpacing.medium)) {
                    ProvenanceRow("Okunan durum", turkishStateLabel(provenance.stateId))
                    ProvenanceRow("İzin verilen yaklaşım", turkishStrategyLabel(provenance.strategyId))
                    ProvenanceRow(
                        label = "Süre sınırı",
                        value = "${provenance.maxDurationMinutes} dk · uygulamanın koyduğu üst sınır",
                    )
                    if (provenance.durationMinutes > 0) {
                        ProvenanceRow(
                            label = "Önerilen süre",
                            value = "${provenance.durationMinutes} dk",
                        )
                    }
                    ProvenanceRow(
                        label = "Kişisel çıpa",
                        value = provenance.anchor.ifBlank { "kullanılmadı" },
                    )
                    ProvenanceRow("Kaynak", turkishSourceLabel(provenance.source))
                    if (provenance.learnedPreferenceApplied) {
                        ProvenanceRow(
                            label = "Senin geri bildiriminden",
                            value = "Bu yaklaşımı daha önce işe yaradı olarak işaretlemiştin.",
                        )
                    }
                    Text(
                        "Bu kısıtları uygulama belirledi, model değil. Günlük hedefini yalnızca sen değiştirebilirsin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProvenanceRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(EsikSpacing.xSmall)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun turkishStateLabel(stateId: String): String = when (stateId) {
    "tired" -> "Yorgunluk"
    "procrastinating" -> "Erteleme"
    "relaxing" -> "Bilinçli mola"
    "bored" -> "Can sıkıntısı"
    "waiting" -> "Bekleme"
    "habit" -> "Alışkanlık"
    "late_night" -> "Gece kullanımı"
    else -> "Belirsiz"
}

private fun turkishStrategyLabel(strategyId: String): String = when (strategyId) {
    "low_energy_reset" -> "Düşük enerjili sıfırlama"
    "micro_start" -> "İki dakikalık başlangıç"
    "timed_intentional_use" -> "Süresi seçilmiş bilinçli kullanım"
    "environment_change" -> "Ortam değişikliği"
    "sensory_break" -> "Kısa duyusal mola"
    "brief_activity" -> "Kısa aktivite"
    else -> "Belirtilmedi"
}

private fun turkishSourceLabel(source: AiCardSource): String = when (source) {
    AiCardSource.LIVE -> "Canlı model · doğrulamayı ilk denemede geçti"
    AiCardSource.REPAIRED -> "Canlı model · bir kez düzeltildi"
    AiCardSource.LOCAL_FALLBACK -> "Cihaz içi hazır metin · modele gidilmedi"
}
