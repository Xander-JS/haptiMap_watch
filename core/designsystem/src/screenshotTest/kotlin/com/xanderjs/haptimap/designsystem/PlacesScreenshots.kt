package com.xanderjs.haptimap.designsystem

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import com.xanderjs.haptimap.core.model.PlaceSearchError
import com.xanderjs.haptimap.designsystem.molecules.samplePlaces
import com.xanderjs.haptimap.designsystem.organisms.PlaceResultsList
import com.xanderjs.haptimap.designsystem.organisms.SearchStatus
import com.xanderjs.haptimap.designsystem.organisms.StatusMessage
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface

// Componentes de búsqueda (paso 04) en las 12 combinaciones de @HaptiPreviews. La pantalla de ruta (paso 05)
// se captura entera en :feature:navigation-ui. Referencias: ./gradlew :core:designsystem:updateDebugScreenshotTest.

@Composable
private fun Failed(error: PlaceSearchError) = PreviewSurface { StatusMessage(SearchStatus.Failed(error)) }

@PreviewTest @HaptiPreviews @Composable
fun PlacesResults() = PreviewSurface { PlaceResultsList(places = samplePlaces(), onPlaceClick = {}) }

@PreviewTest @HaptiPreviews @Composable
fun SearchLoading() = PreviewSurface { StatusMessage(SearchStatus.Loading) }

@PreviewTest @HaptiPreviews @Composable
fun SearchNoResults() = Failed(PlaceSearchError.NO_RESULTS)

@PreviewTest @HaptiPreviews @Composable
fun SearchNoConnection() = Failed(PlaceSearchError.NO_CONNECTION)

@PreviewTest @HaptiPreviews @Composable
fun SearchNoLocation() = Failed(PlaceSearchError.NO_LOCATION)

@PreviewTest @HaptiPreviews @Composable
fun SearchQuota() = Failed(PlaceSearchError.API_QUOTA)

@PreviewTest @HaptiPreviews @Composable
fun SearchInvalidKey() = Failed(PlaceSearchError.API_INVALID_KEY)

@PreviewTest @HaptiPreviews @Composable
fun SearchApiError() = Failed(PlaceSearchError.API_ERROR)
