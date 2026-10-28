package com.xanderjs.haptimap.core.network

/** Para tests y previews: devuelve [result], o MISSING_KEY sin "pedir" nada si no hay clave, como exige el contrato. */
class FakeDeepSeekClient(var result: ChatResult = ChatResult.Success(""), override var hasApiKey: Boolean = true) : DeepSeekClient {
    var requests = 0
        private set
    var lastMessages: List<ChatMessage>? = null
        private set

    override suspend fun complete(messages: List<ChatMessage>): ChatResult {
        if (!hasApiKey) return ChatResult.Failure(ChatError.MISSING_KEY)
        requests++
        lastMessages = messages
        return result
    }
}
