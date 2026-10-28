package com.xanderjs.haptimap.core.network

/**
 * Cliente del modelo de lenguaje de DeepSeek para la versión 2 (Intelligence Layer: evaluar rutas y
 * POIs). Ninguna función lo usa todavía; solo existe [FakeDeepSeekClient].
 *
 * Contrato de toda implementación: sin clave ([hasApiKey] false) o sin red, [complete] devuelve el
 * error al momento, sin hacer la petición. La clave sale de BuildConfig.DEEPSEEK_API_KEY y cuenta
 * como ausente si es el marcador ([isApiKeyConfigured]), como la de Google.
 */
interface DeepSeekClient {
    val hasApiKey: Boolean

    /** Una respuesta a [messages], en orden. */
    suspend fun complete(messages: List<ChatMessage>): ChatResult
}

enum class ChatRole { SYSTEM, USER, ASSISTANT }

data class ChatMessage(val role: ChatRole, val content: String)

/** Resultado explícito, como RouteResult: la respuesta o por qué no la hay. */
sealed interface ChatResult {
    data class Success(val text: String) : ChatResult
    data class Failure(val error: ChatError) : ChatResult
}

/** MISSING_KEY: compilada sin clave, no se llega a pedir. Los API_* son fallos del servicio. */
enum class ChatError { MISSING_KEY, NO_CONNECTION, API_QUOTA, API_INVALID_KEY, API_ERROR }
