package app.mimo.notes.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * A tiny scroll-reactive state for the floating dock. Scrolling down sinks the
 * dock a few dp; scrolling up lifts it. When the finger stops, it settles back
 * with one gentle spring.
 */
class DockGravityState(private val scope: CoroutineScope) {
    var sink by mutableFloatStateOf(0f)
        private set

    private var resetJob: Job? = null

    fun onScroll(delta: Float) {
        if (abs(delta) < 1.2f) return
        sink = if (delta < 0f) 1f else 0f
        resetJob?.cancel()
        resetJob = scope.launch {
            delay(240)
            sink = 0f
        }
    }
}

val LocalDockGravity = staticCompositionLocalOf<DockGravityState?> { null }
