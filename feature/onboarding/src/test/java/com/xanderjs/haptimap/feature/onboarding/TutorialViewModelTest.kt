package com.xanderjs.haptimap.feature.onboarding

import androidx.lifecycle.SavedStateHandle
import com.xanderjs.haptimap.core.haptics.HapticEngine
import com.xanderjs.haptimap.core.haptics.HapticPlayer
import com.xanderjs.haptimap.core.model.HapticEvent
import com.xanderjs.haptimap.core.model.HapticPattern
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TutorialViewModelTest {

    private class FakePlayer(override val canVibrate: Boolean = true) : HapticPlayer {
        val events = mutableListOf<HapticEvent>()
        override fun play(event: HapticEvent, pattern: HapticPattern) {
            events += event
        }
        override fun cancel() = Unit
    }

    private class FakeStore(override var seen: Boolean = false) : TutorialStore {
        override fun markSeen() {
            seen = true
        }
    }

    private class Setup(scope: TestScope, canVibrate: Boolean = true, saved: SavedStateHandle = SavedStateHandle()) {
        val player = FakePlayer(canVibrate)
        val engine = HapticEngine(player)
        val store = FakeStore()
        val viewModel = TutorialViewModel(engine, store, saved)

        init {
            // el motor reproduce lo que pide play() mientras corre follow(), como en la app
            scope.backgroundScope.launch { engine.follow(emptyFlow(), flowOf(false)) }
        }
    }

    /** El motor corre en backgroundScope: advanceUntilIdle no espera sus delay; esto deja sonar un patrón entero y su pausa. */
    private fun TestScope.settle() = advanceTimeBy(3_000)

    @Test
    fun `steps go welcome, each lesson in order, done`() {
        val count = hapticLessons.size
        var step: TutorialStep = TutorialStep.Welcome
        val seen = mutableListOf(step)
        repeat(count + 2) { step = step.next(count); seen += step }
        assertEquals(TutorialStep.Lesson(0), seen[1])
        assertEquals(TutorialStep.Lesson(count - 1), seen[count])
        assertEquals(TutorialStep.Done, seen[count + 1])
        assertEquals(TutorialStep.Done, seen.last())
        assertEquals(TutorialStep.Done, TutorialStep.Welcome.next(0))
    }

    @Test
    fun `the version 1 lessons, in order, without the POI suggestion`() {
        assertEquals(
            listOf(
                HapticEvent.TURN_LEFT, HapticEvent.TURN_RIGHT, HapticEvent.CONTINUE,
                HapticEvent.TURN_IMMINENT, HapticEvent.OFF_ROUTE, HapticEvent.ARRIVAL,
            ),
            hapticLessons.map { it.event },
        )
    }

    @Test
    fun `each lesson plays its event once on entering, through the engine, and play again repeats it`() = runTest {
        val s = Setup(this)
        settle()
        assertEquals(emptyList<HapticEvent>(), s.player.events)
        repeat(hapticLessons.size) {
            s.viewModel.next()
            settle()
        }
        assertEquals(hapticLessons.map { it.event }, s.player.events)
        s.viewModel.playAgain()
        settle()
        assertEquals(HapticEvent.ARRIVAL, s.player.events.last())
        assertEquals(hapticLessons.size + 1, s.player.events.size)
    }

    @Test
    fun `reaching done marks it seen and plays nothing`() = runTest {
        val s = Setup(this)
        repeat(hapticLessons.size) {
            s.viewModel.next()
            settle()
        }
        assertFalse(s.store.seen)
        s.viewModel.next()
        settle()
        assertEquals(TutorialStep.Done, s.viewModel.uiState.value.step)
        assertTrue(s.store.seen)
        s.viewModel.playAgain()
        settle()
        assertEquals(hapticLessons.size, s.player.events.size)
        s.viewModel.finish()
        assertTrue(s.viewModel.uiState.value.finished)
    }

    @Test
    fun `quick taps on next leave the newest lesson, not a backlog`() = runTest {
        val s = Setup(this)
        s.viewModel.next()
        s.viewModel.next()
        s.viewModel.next()
        settle()
        // los tres llegan en la misma ventana de 100 ms del motor y se funden en el más reciente
        assertEquals(listOf(HapticEvent.CONTINUE), s.player.events)
    }

    @Test
    fun `skip from any step marks it seen and finishes`() = runTest {
        val s = Setup(this)
        s.viewModel.next()
        s.viewModel.skip()
        assertTrue(s.store.seen)
        assertTrue(s.viewModel.uiState.value.finished)
    }

    @Test
    fun `without a vibrator nothing is asked of the engine`() = runTest {
        val s = Setup(this, canVibrate = false)
        assertFalse(s.viewModel.uiState.value.canVibrate)
        s.viewModel.next()
        s.viewModel.playAgain()
        settle()
        assertEquals(TutorialStep.Lesson(0), s.viewModel.uiState.value.step)
        assertEquals(emptyList<HapticEvent>(), s.player.events)
    }

    @Test
    fun `the step survives process death and is not played again`() = runTest {
        val saved = SavedStateHandle()
        val first = Setup(this, saved = saved)
        first.viewModel.next()
        first.viewModel.next()
        settle()
        val restored = Setup(this, saved = saved)
        settle()
        assertEquals(TutorialStep.Lesson(1), restored.viewModel.uiState.value.step)
        assertEquals(emptyList<HapticEvent>(), restored.player.events)
    }
}
