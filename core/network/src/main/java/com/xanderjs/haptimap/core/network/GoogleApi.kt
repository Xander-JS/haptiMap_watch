package com.xanderjs.haptimap.core.network

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** El marcador de local.defaults.properties: con él la app compila, pero no hay clave real. */
const val PLACEHOLDER_API_KEY = "DEFAULT_API_KEY"

fun isApiKeyConfigured(apiKey: String) = apiKey.isNotBlank() && apiKey != PLACEHOLDER_API_KEY

// encodeDefaults: los valores por defecto de las peticiones (rankPreference, travelMode…) deben viajar;
// explicitNulls = false: un campo null no se envía (un Waypoint lleva placeId o location, nunca los dos)
internal val googleJson = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false }

/**
 * Retrofit para una API REST de Google con la misma clave (Places y Routes son del mismo proyecto).
 * [certSha1] es la huella SHA-1 de la firma en hex sin separadores: con la clave restringida a la app
 * Android, Google la compara junto con el paquete en cada petición.
 */
internal fun googleRetrofit(baseUrl: String, apiKey: String, packageName: String, certSha1: String): Retrofit {
    val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("X-Goog-Api-Key", apiKey)
                    .header("X-Android-Package", packageName)
                    .header("X-Android-Cert", certSha1)
                    .build(),
            )
        }
        .build()
    return Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(googleJson.asConverterFactory("application/json".toMediaType()))
        .build()
}
