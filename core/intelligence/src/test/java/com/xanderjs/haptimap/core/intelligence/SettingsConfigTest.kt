package com.xanderjs.haptimap.core.intelligence

import com.xanderjs.haptimap.core.model.DetourTolerance
import com.xanderjs.haptimap.core.model.ExplorationCategory
import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.Maneuver
import com.xanderjs.haptimap.core.model.NavigationProfile
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.Route
import com.xanderjs.haptimap.core.model.RouteStep
import com.xanderjs.haptimap.core.model.UserSettings
import com.xanderjs.haptimap.core.model.distanceTo
import com.xanderjs.haptimap.core.model.interpolate
import com.xanderjs.haptimap.core.model.offset
import kotlin.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsConfigTest {

    @Test
    fun `every detour preset maps to a coherent config, wider as tolerance grows`() {
        // la construcción no lanza = cada preset pasa validate()
        val short = DetourTolerance.SHORT.toPoiEvaluationConfig()
        val medium = DetourTolerance.MEDIUM.toPoiEvaluationConfig()
        val long = DetourTolerance.LONG.toPoiEvaluationConfig()
        assertTrue(short.maxDistanceFromRouteMeters < medium.maxDistanceFromRouteMeters)
        assertTrue(medium.maxDistanceFromRouteMeters < long.maxDistanceFromRouteMeters)
        assertTrue(short.maxDetourMeters < medium.maxDetourMeters)
        assertTrue(medium.maxDetourMeters < long.maxDetourMeters)
    }

    @Test
    fun `a favorite category makes a borderline POI worth suggesting through the settings evaluator`() {
        // ruta recta; un café a 80 m (desvío 160 m) con tolerancia media. Sin favorita su relevancia (0.6) basta,
        // así que para ver el efecto comparamos el score: favorito puntúa más que no favorito.
        val start = GeoPoint(40.0, -3.0)
        val end = start.offset(bearingDegrees = 90.0, meters = 400.0)
        val mid = interpolate(start, end, 0.5)
        val route = Route(
            destination = Place("d", "Destino", end, null, null, 0),
            distanceMeters = start.distanceTo(end).toInt(),
            duration = Duration.ZERO,
            points = listOf(start, end),
            steps = listOf(RouteStep(Maneuver.ARRIVAL, 0, Duration.ZERO, emptyList(), start, end, null, null)),
        )
        val cafe = PoiCandidate("c", "Café", ExplorationCategory.CAFE_RESTAURANT, mid.offset(bearingDegrees = 0.0, meters = 40.0))
        fun scoreWithFavorites(favorites: Set<ExplorationCategory>): Double {
            val settings = UserSettings(detourTolerance = DetourTolerance.MEDIUM, favoriteCategories = favorites)
            return poiEvaluator(settings).evaluate(listOf(cafe), route, 0.0, NavigationProfile.TOUR, RejectedPoiMemory()).single().score
        }
        assertTrue(scoreWithFavorites(setOf(ExplorationCategory.CAFE_RESTAURANT)) > scoreWithFavorites(emptySet()))
    }
}
