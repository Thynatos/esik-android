package com.thynatos.esik

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.thynatos.esik.ai.GeminiAiGateway
import com.thynatos.esik.data.JsonEsikRepository
import com.thynatos.esik.ui.theme.EsikTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = JsonEsikRepository(this)
        val aiGateway = GeminiAiGateway()

        setContent {
            EsikTheme {
                EsikApp(
                    repository = repository,
                    aiGateway = aiGateway,
                )
            }
        }
    }
}
