package com.thynatos.esik.overlay

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.thynatos.esik.ai.AiGateway
import com.thynatos.esik.ai.CrisisFilter
import com.thynatos.esik.ai.MockAiGateway
import com.thynatos.esik.ai.SafetyLanguageValidator
import com.thynatos.esik.data.EsikRepository
import com.thynatos.esik.data.InterventionRecord
import com.thynatos.esik.data.JsonEsikRepository
import com.thynatos.esik.data.UserChoice
import com.thynatos.esik.data.UserProfile

class OverlayController(
    context: Context,
    private val repository: EsikRepository = JsonEsikRepository(context),
    private val aiGateway: AiGateway = MockAiGateway(),
) {
    private val appContext = context.applicationContext
    private val windowManager = appContext.getSystemService(WindowManager::class.java)
    private var overlayView: View? = null

    val isShowing: Boolean
        get() = overlayView != null

    fun show(profile: UserProfile, usageMinutes: Int): Boolean {
        if (isShowing || !Settings.canDrawOverlays(appContext)) return false

        val root = FrameLayout(appContext).apply {
            setBackgroundColor(Color.argb(175, 0, 0, 0))
            isClickable = true
            isFocusable = true
        }
        val card = LinearLayout(appContext).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20.dp, 20.dp, 20.dp, 20.dp)
            background = roundedBackground(Color.WHITE, 20f)
            elevation = 12.dp.toFloat()
        }
        root.addView(
            card,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                gravity = Gravity.CENTER
                marginStart = 22.dp
                marginEnd = 22.dp
            },
        )

        val headline = TextView(appContext).apply {
            text = "Bugün $usageMinutes dakika oldu. Hedefin ${profile.dailyLimitMinutes}'tı. Şu an ne oluyor?"
            textSize = 20f
            setTextColor(Color.BLACK)
        }
        val input = EditText(appContext).apply {
            hint = "Kendi kelimelerinle yaz"
            minLines = 3
            maxLines = 6
            setTextColor(Color.BLACK)
            setHintTextColor(Color.DKGRAY)
        }
        val status = TextView(appContext).apply {
            visibility = View.GONE
            textSize = 15f
            setTextColor(Color.DKGRAY)
        }
        val submit = Button(appContext).apply { text = "Yanıtı gönder" }
        val resultContainer = LinearLayout(appContext).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }

        card.addView(headline, matchWrap(bottom = 14.dp))
        card.addView(input, matchWrap(bottom = 10.dp))
        card.addView(status, matchWrap(bottom = 10.dp))
        card.addView(submit, matchWrap(bottom = 8.dp))
        card.addView(resultContainer, matchWrap())

        submit.setOnClickListener {
            val text = input.text.toString().trim()
            if (text.isBlank()) {
                input.error = "Bir cümle yaz"
                input.requestFocus()
                return@setOnClickListener
            }

            hideKeyboard(input)
            if (CrisisFilter.check(text).isCrisisSignal) {
                input.isEnabled = false
                submit.visibility = View.GONE
                status.apply {
                    visibility = View.VISIBLE
                    setTextColor(Color.rgb(120, 0, 0))
                    this.text = "Bunu tek başına taşımak zorunda değilsin. Yakınındaki acil yardım hizmetine, güvendiğin bir kişiye veya profesyonel desteğe şimdi ulaş. Bu metin AI servisine gönderilmedi."
                }
                resultContainer.removeAllViews()
                resultContainer.addView(Button(appContext).apply {
                    this.text = "Kapat"
                    setOnClickListener { dismiss() }
                })
                resultContainer.visibility = View.VISIBLE
                return@setOnClickListener
            }

            val generated = runCatching {
                aiGateway.generateCard(profile, usageMinutes, text)
            }.getOrElse {
                MockAiGateway().generateCard(profile, usageMinutes, text)
            }
            val cardResult = if (
                SafetyLanguageValidator.isDisplaySafe(generated.question, generated.alternative)
            ) {
                generated
            } else {
                MockAiGateway().generateCard(profile, usageMinutes, text)
            }

            input.isEnabled = false
            submit.visibility = View.GONE
            status.visibility = View.GONE
            resultContainer.removeAllViews()
            resultContainer.addView(TextView(appContext).apply {
                this.text = cardResult.question
                textSize = 18f
                setTextColor(Color.BLACK)
            }, matchWrap(bottom = 10.dp))
            resultContainer.addView(TextView(appContext).apply {
                this.text = cardResult.alternative
                textSize = 16f
                setTextColor(Color.DKGRAY)
            }, matchWrap(bottom = 14.dp))

            val actions = LinearLayout(appContext).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
            }
            actions.addView(Button(appContext).apply {
                this.text = "Vazgeçtim"
                setOnClickListener {
                    saveRecord(text, usageMinutes, UserChoice.STOPPED)
                    dismiss()
                    openLauncher()
                }
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 6.dp
            })
            actions.addView(Button(appContext).apply {
                this.text = "Yine de gir"
                setOnClickListener {
                    saveRecord(text, usageMinutes, UserChoice.CONTINUE)
                    dismiss()
                }
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = 6.dp
            })
            resultContainer.addView(actions, matchWrap())
            resultContainer.visibility = View.VISIBLE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.CENTER
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

        return runCatching {
            windowManager.addView(root, params)
            overlayView = root
            true
        }.getOrElse {
            overlayView = null
            false
        }
    }

    fun dismiss() {
        val view = overlayView ?: return
        overlayView = null
        runCatching { windowManager.removeView(view) }
    }

    private fun saveRecord(text: String, usageMinutes: Int, choice: UserChoice) {
        repository.appendRecord(
            InterventionRecord(
                timestampEpochMillis = System.currentTimeMillis(),
                usageMinutes = usageMinutes,
                text = text,
                choice = choice,
            ),
        )
    }

    private fun openLauncher() {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { appContext.startActivity(intent) }
    }

    private fun hideKeyboard(view: View) {
        appContext.getSystemService(InputMethodManager::class.java)
            .hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun roundedBackground(color: Int, radiusDp: Float): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radiusDp.dp
        }

    private fun matchWrap(bottom: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { bottomMargin = bottom }

    private val Int.dp: Int
        get() = (this * appContext.resources.displayMetrics.density).toInt()

    private val Float.dp: Float
        get() = this * appContext.resources.displayMetrics.density
}
