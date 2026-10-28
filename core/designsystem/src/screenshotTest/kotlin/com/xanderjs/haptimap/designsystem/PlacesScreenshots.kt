package com.xanderjs.haptimap.designsystem

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import com.xanderjs.haptimap.designsystem.molecules.samplePlaceRows
import com.xanderjs.haptimap.designsystem.organisms.PlaceResultsList
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface

// Resultados de búsqueda (paso 04) en las 12 combinaciones de @HaptiPreviews. Los estados de búsqueda,
// que dependen del texto de cada error, se capturan donde vive ese mapeo (:app, SearchStatusScreenshots).
// Referencias: ./gradlew :core:designsystem:updateDebugScreenshotTest.

@PreviewTest @HaptiPreviews @Composable
fun PlacesResults() = PreviewSurface { PlaceResultsList(items = samplePlaceRows(), onItemClick = {}) }
