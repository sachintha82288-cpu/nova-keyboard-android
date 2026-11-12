package com.nova.keyboard.ai

/** Prompt construction for every AI action exposed by the keyboard. */
object AiPrompts {

    const val SYSTEM = "You are NOVA, a writing assistant built into a mobile keyboard. " +
        "Return only the requested text with no preamble, no quotes and no markdown fences. " +
        "Never invent facts. Keep the user's language unless asked to translate."

    private const val SINHALA_NOTE =
        " Sinhala text must be written in correct Sinhala Unicode; never romanise it."

    fun rewrite(tone: String, text: String): Pair<String, String> {
        val instruction = when (tone) {
            "shorter" -> "Rewrite the text so it is meaningfully shorter while keeping the meaning."
            "longer" -> "Expand the text with relevant detail, keeping the same voice."
            "professional" -> "Rewrite the text in a professional tone."
            "casual" -> "Rewrite the text in a casual, conversational tone."
            "friendly" -> "Rewrite the text in a warm, friendly tone."
            "formal" -> "Rewrite the text in a formal tone."
            "confident" -> "Rewrite the text in a confident tone."
            "simple" -> "Rewrite the text in plain, simple language."
            "detailed" -> "Rewrite the text with more detail and structure."
            else -> "Improve the writing without changing the meaning."
        }
        return instruction to text
    }

    fun grammar(text: String): Pair<String, String> =
        "Fix grammar, spelling and punctuation. Keep the wording and language." to text

    fun translate(text: String, from: String, to: String): Pair<String, String> =
        ("Translate from " + from + " to " + to + "." + SINHALA_NOTE) to text

    fun generate(kind: String, prompt: String): Pair<String, String> {
        val instruction = when (kind) {
            "email" -> "Write a short, clear email."
            "caption" -> "Write three short social media captions, one per line."
            "reply" -> "Write a natural reply to the message."
            "description" -> "Write a concise description."
            "message" -> "Write a short message."
            else -> "Write short, useful text."
        }
        return instruction + SINHALA_NOTE to prompt
    }

    fun reply(context: String): Pair<String, String> =
        ("Write a natural, relevant reply to this conversation." + SINHALA_NOTE) to context

    fun summarize(text: String): Pair<String, String> =
        "Summarise the text in at most three sentences." to text
}
