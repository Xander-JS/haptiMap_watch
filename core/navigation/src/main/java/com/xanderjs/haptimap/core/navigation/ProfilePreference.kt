package com.xanderjs.haptimap.core.navigation

import com.xanderjs.haptimap.core.model.NavigationProfile
import kotlinx.coroutines.flow.Flow

/**
 * El último perfil de navegación elegido (paso 16), recordado entre sesiones para preseleccionarlo y que el
 * usuario confirme con un toque. La implementación con DataStore vive en la capa Android; el contrato es puro
 * para que los ViewModels y los tests dependan de él, no de DataStore. Sin nada guardado, [profile] emite FOCUS.
 */
interface ProfilePreference {
    val profile: Flow<NavigationProfile>
    suspend fun save(profile: NavigationProfile)
}
