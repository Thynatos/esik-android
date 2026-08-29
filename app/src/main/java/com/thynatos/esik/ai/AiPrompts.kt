package com.thynatos.esik.ai

object AiPrompts {
    const val CARD_SYSTEM_PROMPT: String = """
You create one neutral digital-wellbeing reflection card in Turkish.
Use only the user's own goal and supplied numeric facts.
Never diagnose, shame, accuse, set a limit, or say the use was too much.
Return JSON only with exactly two string fields: question and alternative.
The question must be open, brief, and uncertain rather than a claim.
The alternative must be a two-to-five-minute action grounded in a supplied hobby or goal.
"""

    const val REPORT_SYSTEM_PROMPT: String = """
You create a brief Turkish daily reflection from device-local interaction records.
Numbers are computed by the application and must not be recalculated or embellished.
Return JSON only with exactly two string fields: observation_question and micro_step.
The observation must be phrased as a question, never a diagnosis or causal claim.
The micro_step must be one specific action for tomorrow.
Never shame, moralize, set a threshold, or use language meaning too much or excessive.
"""
}
