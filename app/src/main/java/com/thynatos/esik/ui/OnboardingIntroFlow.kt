package com.thynatos.esik.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thynatos.esik.R
import com.thynatos.esik.ai.AiGateway
import com.thynatos.esik.data.UserProfile
import com.thynatos.esik.permissions.LaunchableApp
import com.thynatos.esik.ui.theme.EsikSpacing
import kotlinx.coroutines.launch

private data class IntroPage(
    val illustration: Int,
    val tag: String,
    val title: String,
    val description: String,
)

private val introPages = listOf(
    IntroPage(
        illustration = R.drawable.ic_slide_welcome,
        tag = "DİJİTAL FARKINDALIK",
        title = "Eşik'e Hoş Geldin",
        description = "Ekran süreni cezalandırmak için değil, kendi koyduğun sınırda daha bilinçli bir karar verebilmen için kısa bir durak sunuyoruz. Profilin ve kayıtların cihazında kalır; canlı AI üretiminde yalnızca ilgili metin Gemini'ye gönderilir.",
    ),
    IntroPage(
        illustration = R.drawable.ic_slide_freedom,
        tag = "ÖZGÜR VE YARGISIZ",
        title = "Yasak Yok, Sadece Bir Duraklama",
        description = "Kendi belirlediğin sınıra ulaştığında Eşik uygulamayı kilitlemez ve seni suçlamaz. 'Şu an ne oluyor?' diye sorar; devam etmek ya da küçük bir alternatif denemek yine senin kararındır.",
    ),
    IntroPage(
        illustration = R.drawable.ic_slide_insights,
        tag = "KİŞİSEL YANSIMA",
        title = "Sana Göre Küçük Bir Sonraki Adım",
        description = "Eşik, kendi anlattığın hedefleri ve o an verdiğin bağlamı kullanarak kısa bir soru ve uygulanabilir bir mikro-alternatif üretir. Yeterli günlük kayıt olduğunda sayıları cihazda hesaplayıp temkinli bir günlük yansıma sunar.",
    ),
)

/**
 * First-run explanation followed by the existing AI-v3 setup.
 * This remains part of the ONBOARDING route; it does not introduce a new product destination.
 */
@Composable
fun OnboardingIntroFlow(
    installedApps: List<LaunchableApp>,
    hasUsageAccess: Boolean,
    canDrawOverlays: Boolean,
    aiGateway: AiGateway,
    onOpenUsagePermission: () -> Unit,
    onOpenOverlayPermission: () -> Unit,
    onComplete: (UserProfile) -> Unit,
) {
    var showIntro by rememberSaveable { mutableStateOf(true) }

    if (showIntro) {
        OnboardingIntroScreen(onFinish = { showIntro = false })
    } else {
        OnboardingScreen(
            installedApps = installedApps,
            hasUsageAccess = hasUsageAccess,
            canDrawOverlays = canDrawOverlays,
            aiGateway = aiGateway,
            onOpenUsagePermission = onOpenUsagePermission,
            onOpenOverlayPermission = onOpenOverlayPermission,
            onComplete = onComplete,
        )
    }
}

@Composable
private fun OnboardingIntroScreen(
    onFinish: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { introPages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == introPages.lastIndex

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = EsikSpacing.xLarge,
                    vertical = EsikSpacing.xLarge,
                ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(EsikSpacing.small),
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_esik_logo),
                        contentDescription = null,
                        modifier = Modifier.size(34.dp),
                    )
                    Text(
                        text = "EŞİK",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                AnimatedVisibility(visible = !isLastPage) {
                    TextButton(onClick = onFinish) {
                        Text("Atla")
                    }
                }
            }

            Spacer(Modifier.height(EsikSpacing.medium))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { pageIndex ->
                val page = introPages[pageIndex]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = EsikSpacing.small),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Image(
                        painter = painterResource(page.illustration),
                        contentDescription = null,
                        modifier = Modifier.size(132.dp),
                    )

                    Spacer(Modifier.height(EsikSpacing.xLarge))

                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = page.tag,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }

                    Spacer(Modifier.height(EsikSpacing.medium))

                    Text(
                        text = page.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(EsikSpacing.medium))

                    Text(
                        text = page.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(EsikSpacing.large),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(introPages.size) { index ->
                        val selected = pagerState.currentPage == index
                        val indicatorWidth by animateDpAsState(
                            targetValue = if (selected) 24.dp else 8.dp,
                            animationSpec = tween(250),
                            label = "intro_indicator_width",
                        )
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(indicatorWidth)
                                .clip(CircleShape)
                                .background(
                                    if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                ),
                        )
                    }
                }

                Button(
                    onClick = {
                        if (isLastPage) {
                            onFinish()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                ) {
                    Text(
                        text = if (isLastPage) "Hemen Başla" else "Devam",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(Modifier.height(EsikSpacing.small))
            }
        }
    }
}
