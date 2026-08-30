package com.thynatos.esik.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thynatos.esik.R
import com.thynatos.esik.ui.theme.DarkPrimary
import com.thynatos.esik.ui.theme.ForestGreenPrimary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onTimeout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var startAnimation by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.7f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "splash_scale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "splash_alpha",
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(1200)
        onTimeout()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ForestGreenPrimary),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(scale)
                .alpha(alpha)
                .padding(24.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_esik_logo),
                contentDescription = "Eşik Logo",
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(22.dp)),
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "EŞİK",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp,
                    color = Color.White,
                ),
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Farkındalık & Dijital Denge",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                    color = DarkPrimary,
                ),
            )
        }

        // Bottom version tag
        Text(
            text = "Sürüm 0.1.0",
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp),
        )
    }
}
