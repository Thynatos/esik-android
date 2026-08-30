package com.thynatos.esik.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thynatos.esik.ui.theme.StatusHealthy
import com.thynatos.esik.ui.theme.StatusHealthyBg
import com.thynatos.esik.ui.theme.StatusOverLimit
import com.thynatos.esik.ui.theme.StatusOverLimitBg
import com.thynatos.esik.ui.theme.StatusWarning
import com.thynatos.esik.ui.theme.StatusWarningBg

@Composable
fun UsageHeroCard(
    targetAppLabel: String,
    usageMinutes: Int,
    limitMinutes: Int,
    onOpenTargetApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = if (limitMinutes <= 0) 0f else {
        (usageMinutes.toFloat() / limitMinutes.toFloat()).coerceIn(0f, 1f)
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600),
        label = "usage_progress",
    )

    val isOverLimit = limitMinutes > 0 && usageMinutes >= limitMinutes
    val isWarning = !isOverLimit && limitMinutes > 0 && usageMinutes >= (limitMinutes * 0.75f)

    val statusColor by animateColorAsState(
        targetValue = when {
            isOverLimit -> StatusOverLimit
            isWarning -> StatusWarning
            else -> StatusHealthy
        },
        label = "status_color",
    )

    val statusBadgeBg by animateColorAsState(
        targetValue = when {
            isOverLimit -> StatusOverLimitBg
            isWarning -> StatusWarningBg
            else -> StatusHealthyBg
        },
        label = "status_badge_bg",
    )

    val statusText = when {
        isOverLimit -> "${usageMinutes - limitMinutes} dk aşıldı"
        limitMinutes <= 0 -> "Limit yok"
        else -> "${limitMinutes - usageMinutes} dk kaldı"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header Row: App name & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = targetAppLabel,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Bugünkü Kullanım",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Status pill badge
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(statusBadgeBg)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // Big Numbers: 45 / 60 dk
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "$usageMinutes",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverLimit) StatusOverLimit else MaterialTheme.colorScheme.onSurface,
                    ),
                )
                Text(
                    text = "/ $limitMinutes dk",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }

            // Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape),
                    color = statusColor,
                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "%${(progress * 100).toInt()} tamamlandı",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (isOverLimit) {
                        Text(
                            text = "Müdahale eşiği aşıldı",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = StatusOverLimit,
                        )
                    }
                }
            }

            // Target App Launch Button
            OutlinedButton(
                onClick = onOpenTargetApp,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("$targetAppLabel Uygulamasını Aç")
            }
        }
    }
}
