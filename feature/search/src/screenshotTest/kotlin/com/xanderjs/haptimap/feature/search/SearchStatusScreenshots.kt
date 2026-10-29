package com.xanderjs.haptimap.feature.search

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import com.xanderjs.haptimap.core.model.PlaceSearchError
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface

// Cada estado de una búsqueda de lugares con su texto y tono reales, en las 12 combinaciones de
// @HaptiPreviews. Referencias: ./gradlew :feature:search:updateDebugScreenshotTest.

@Composable
private fun Failed(error: PlaceSearchError) = PreviewSurface { SearchStatusMessage(SearchStatus.Failed(error)) }

@PreviewTest @HaptiPreviews @Composable
fun SearchLoading() = PreviewSurface { SearchStatusMessage(SearchStatus.Loading) }

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

@PreviewTest @HaptiPreviews @Composable
fun SearchNoPermission() = PreviewSurface { SearchStatusMessage(SearchStatus.NoPermission) }
