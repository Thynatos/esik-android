package com.thynatos.esik.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thynatos.esik.R
import com.thynatos.esik.data.UserProfile
import com.thynatos.esik.permissions.LaunchableApp
import com.thynatos.esik.ui.theme.ForestGreenPrimary
import com.thynatos.esik.ui.theme.ForestGreenPrimaryContainer
import kotlinx.coroutines.launch

private data class OnboardingPageData(
    val drawableRes: Int,
    val tag: String,
    val title: String,
    val description: String,
)

private val onboardingPages = listOf(
    OnboardingPageData(
        drawableRes = R.drawable.ic_slide_welcome,
        tag = "DİJİTAL FARKINDALIK",
        title = "Eşik'e Hoş Geldin",
        description = "Ekran süreni katı yasaklarla kısıtlamak için değil; dijital alışkanlıklarınla barışık, bilinçli ve huzurlu bir denge kurman için buradayız. Verilerin tamamen kendi cihazında kalır.",
    ),
    OnboardingPageData(
        drawableRes = R.drawable.ic_slide_freedom,
        tag = "ÖZGÜR VE YARGISIZ",
        title = "Yasak Yok, Sadece Bir Duraklama",
        description = "Belirlediğin hedefi aştığında seni asla kilitlemeyiz veya suçlu hissettirmeyiz. Yalnızca 'Şu an ne oluyor?' diye sorar, durmak ya da 'Yine de gir' demek konusundaki kararı sana bırakırız.",
    ),
    OnboardingPageData(
        drawableRes = R.drawable.ic_slide_insights,
        tag = "KİŞİSEL İÇGÖRÜLER",
        title = "Günün Özeti & Mikro-Adımlar",
        description = "Yapay zeka, gün sonu davranışlarını tarafsız bir dille özetler. Kendini daha iyi anlaman için tek bir derin soru ve yarın için tek bir uygulanabilir mikro-adım önerir.",
    ),
)

@Composable
fun OnboardingScreen(
    installedApps: List<LaunchableApp>,
    hasUsageAccess: Boolean,
    canDrawOverlays: Boolean,
    onOpenUsagePermission: () -> Unit,
    onOpenOverlayPermission: () -> Unit,
    onComplete: (UserProfile) -> Unit,
) {
    var showSetupForm by rememberSaveable { mutableStateOf(false) }

    Crossfade(
        targetState = showSetupForm,
        animationSpec = tween(400),
        label = "onboarding_crossfade",
    ) { inSetup ->
        if (!inSetup) {
            OnboardingSlider(
                onFinish = { showSetupForm = true },
            )
        } else {
            OnboardingSetupForm(
                installedApps = installedApps,
                hasUsageAccess = hasUsageAccess,
                canDrawOverlays = canDrawOverlays,
                onOpenUsagePermission = onOpenUsagePermission,
                onOpenOverlayPermission = onOpenOverlayPermission,
                onBackToSlider = { showSetupForm = false },
                onComplete = onComplete,
            )
        }
    }
}

@Composable
private fun OnboardingSlider(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val coroutineScope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == onboardingPages.size - 1

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        // Top Bar: Brand & Skip Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_esik_logo),
                    contentDescription = "Eşik",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
                Text(
                    text = "EŞİK",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        color = ForestGreenPrimary,
                    ),
                )
            }

            AnimatedVisibility(visible = !isLastPage) {
                TextButton(onClick = onFinish) {
                    Text(
                        text = "Atla",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Horizontal Pager for the 3 slides
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) { pageIndex ->
            val page = onboardingPages[pageIndex]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Slide Illustration
                Image(
                    painter = painterResource(id = page.drawableRes),
                    contentDescription = page.title,
                    modifier = Modifier.size(130.dp),
                )

                Spacer(Modifier.height(28.dp))

                // Tag Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ForestGreenPrimaryContainer)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = page.tag,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp,
                        ),
                        color = ForestGreenPrimary,
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Title
                Text(
                    text = page.title,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                    ),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Spacer(Modifier.height(12.dp))

                // Description
                Text(
                    text = page.description,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 24.sp,
                    ),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
        }

        // Bottom Controls: Page Indicators & Action Buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Animated Pager Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(onboardingPages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    val width by animateDpAsState(
                        targetValue = if (isSelected) 24.dp else 8.dp,
                        animationSpec = tween(300),
                        label = "indicator_width",
                    )
                    val color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }

                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(color),
                    )
                }
            }

            // Next / Get Started Button
            if (isLastPage) {
                Button(
                    onClick = onFinish,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                ) {
                    Text(
                        text = "Hemen Başla",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                }
            } else {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                ) {
                    Text(
                        text = "İlerle ➔",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingSetupForm(
    installedApps: List<LaunchableApp>,
    hasUsageAccess: Boolean,
    canDrawOverlays: Boolean,
    onOpenUsagePermission: () -> Unit,
    onOpenOverlayPermission: () -> Unit,
    onBackToSlider: () -> Unit,
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
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBackToSlider) {
                Text("← Tanıtıma Dön")
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(ForestGreenPrimaryContainer)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "Kurulum Adımı",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = ForestGreenPrimary,
                )
            }
        }

        Text("Hedefini ve Profilini Belirle", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
        Text(
            "Hesap yok, şifre yok. Veriler yalnızca bu cihazda saklanır.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("İsmin") },
                    placeholder = { Text("Örn: Deniz") },
                    isError = attemptedSubmit && name.isBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = { Text("Bölüm / İlgi Alanın") },
                    placeholder = { Text("Örn: Yazılım Mühendisliği") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = hobbiesText,
                    onValueChange = { hobbiesText = it },
                    label = { Text("Hobilerin (virgülle ayır)") },
                    placeholder = { Text("Örn: Kitap okumak, gitar, koşu") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = improvementArea,
                    onValueChange = { improvementArea = it },
                    label = { Text("Kendini geliştirmek istediğin alan") },
                    placeholder = { Text("Örn: Odaklanma ve derin çalışma") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Neden azaltmak istiyorsun?") },
                    placeholder = { Text("Örn: Akşamları daha kaliteli vakit geçirmek için") },
                    supportingText = { Text("Kendi cümlelerinle tek bir neden") },
                    isError = attemptedSubmit && reason.isBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Target App & Limit Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Hedef Uygulama & Limit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                OutlinedButton(
                    onClick = { showAppPicker = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Seçili Uygulama: $targetAppLabel")
                }

                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it.filter(Char::isDigit).take(4) },
                    label = { Text("Günlük Hedef Limit (Dakika)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = attemptedSubmit && (limit == null || limit <= 0),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Permissions Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Gerekli İzinler", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                PermissionRowItem(
                    label = "Kullanım Verisi Erişimi",
                    granted = hasUsageAccess,
                    onClick = onOpenUsagePermission,
                )
                HorizontalDivider()
                PermissionRowItem(
                    label = "Üste Çizme İzni (Intervention Overlay)",
                    granted = canDrawOverlays,
                    onClick = onOpenOverlayPermission,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

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
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text("Kurulumu Tamamla ve Başla", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }

        Spacer(Modifier.height(16.dp))
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
                                Text(app.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun PermissionRowItem(
    label: String,
    granted: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                if (granted) "✓ İzin verildi" else "⚠️ İzin gerekli",
                style = MaterialTheme.typography.bodySmall,
                color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
        OutlinedButton(
            onClick = onClick,
            shape = RoundedCornerShape(10.dp),
        ) {
            Text(if (granted) "Ayarlar" else "İzin Ver", style = MaterialTheme.typography.labelSmall)
        }
    }
}
