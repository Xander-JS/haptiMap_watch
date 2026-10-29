package com.xanderjs.haptimap.feature.search

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xanderjs.haptimap.core.model.PlaceCategory
import com.xanderjs.haptimap.designsystem.atoms.CategoryChip
import com.xanderjs.haptimap.designsystem.atoms.HaptiButton
import com.xanderjs.haptimap.designsystem.atoms.HaptiButtonVariant
import com.xanderjs.haptimap.designsystem.atoms.HeadingText
import com.xanderjs.haptimap.designsystem.atoms.NoticeLabel
import com.xanderjs.haptimap.designsystem.atoms.StatusTone
import com.xanderjs.haptimap.designsystem.atoms.VoiceSearchButton
import com.xanderjs.haptimap.designsystem.templates.RoundScreenTemplate
import com.xanderjs.haptimap.designsystem.theme.appLanguage
import com.xanderjs.haptimap.designsystem.R as DsR

/**
 * Modo Búsqueda (sección 7), la pantalla de entrada: voz y categorías. Cada búsqueda abre los resultados
 * ([onOpenResults]); la ruta activa se retoma con [onOpenRoute] (true = navegando, false = cargada sin empezar).
 * [onOpenCatalog] solo en debug.
 */
@Composable
fun SearchScreen(
    onOpenResults: () -> Unit,
    onOpenRoute: (navigating: Boolean) -> Unit,
    onOpenCatalog: (() -> Unit)? = null,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val prompt = stringResource(R.string.search_voice_prompt)
    val voiceInput = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // cancelado o sin texto: se queda en la búsqueda, como si no se hubiera pulsado
        val text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (result.resultCode == Activity.RESULT_OK && !text.isNullOrBlank()) {
            viewModel.searchText(text)
            onOpenResults()
        }
    }
    SearchContent(
        state = state,
        onVoice = {
            // el reconocedor del sistema (RecognizerIntent, lo recomendado en Wear OS): sin permiso de micrófono propio
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, context.appLanguage())
                .putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
            try {
                voiceInput.launch(intent)
            } catch (e: ActivityNotFoundException) {
                viewModel.voiceUnavailable()
            }
        },
        onCategory = {
            viewModel.searchCategory(it)
            onOpenResults()
        },
        onActiveRoute = { state.activeRoute?.let { onOpenRoute(it.navigating) } },
        onOpenCatalog = onOpenCatalog,
    )
}

/**
 * Lista redonda: título corto arriba (TransformingLazyColumn centra un primer elemento alto), la ruta activa
 * y los avisos antes de buscar, la voz como acción principal y las categorías debajo.
 */
@Composable
internal fun SearchContent(
    state: SearchUiState,
    onVoice: () -> Unit,
    onCategory: (PlaceCategory) -> Unit,
    onActiveRoute: () -> Unit,
    onOpenCatalog: (() -> Unit)? = null,
) {
    val selected = (state.query as? SearchQuery.Category)?.category
    RoundScreenTemplate {
        item { HeadingText(stringResource(R.string.search_title)) }
        state.activeRoute?.let { route ->
            item {
                HaptiButton(
                    text = stringResource(
                        if (route.navigating) R.string.search_active_navigating else R.string.search_active_ready,
                        route.destinationName,
                    ),
                    onClick = onActiveRoute,
                    modifier = Modifier.fillMaxWidth(),
                    variant = HaptiButtonVariant.Secondary,
                    icon = painterResource(if (route.navigating) DsR.drawable.ic_straight else DsR.drawable.ic_arrive),
                )
            }
        }
        // sin red se puede seguir navegando, pero no buscar: se dice antes de que el usuario lo intente
        if (state.offline) item { NoticeLabel(stringResource(R.string.search_offline), StatusTone.Warning) }
        if (state.voiceUnavailable) item { NoticeLabel(stringResource(R.string.search_voice_unavailable), StatusTone.Error) }
        item { VoiceSearchButton(label = stringResource(R.string.search_voice), onClick = onVoice) }
        PlaceCategory.entries.forEach { category ->
            item {
                CategoryChip(
                    label = stringResource(category.label),
                    icon = painterResource(category.icon),
                    selected = category == selected,
                    onClick = { onCategory(category) },
                )
            }
        }
        onOpenCatalog?.let { open ->
            item {
                HaptiButton(
                    text = stringResource(R.string.search_open_catalog),
                    onClick = open,
                    modifier = Modifier.fillMaxWidth(),
                    variant = HaptiButtonVariant.Secondary,
                )
            }
        }
    }
}
