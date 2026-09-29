package dev.muazkadan.rivecmp

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.muazkadan.rivecmp.utils.ExperimentalRiveCmpApi
import kotlin.test.Test

/**
 * Loads and then removes the CustomRiveAnimation overload that owns its composition, which must
 * release the native File it created exactly once: not when loading completes, and not twice.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalRiveCmpApi::class)
class CustomRiveAnimationLifecycleTest {

    private val bytes = requireNotNull(javaClass.classLoader.getResource("mode_switch.riv")) {
        "Test asset 'mode_switch.riv' not found in resources"
    }.readBytes()

    @Test
    fun removingTheByteArrayAnimationReleasesItsFileOnce() = runComposeUiTest {
        RiveDesktop.init()
        // The view requests a frame every frame, so it never goes idle on its own
        mainClock.autoAdvance = false
        var shown by mutableStateOf(true)

        setContent {
            if (shown) {
                CustomRiveAnimation(modifier = Modifier.size(100.dp).testTag(TAG), byteArray = bytes)
            }
        }
        waitUntil(timeoutMillis = 5_000) {
            mainClock.advanceTimeByFrame()
            onAllNodesWithTag(TAG).fetchSemanticsNodes().isNotEmpty()
        }

        shown = false
        mainClock.advanceTimeByFrame()
        waitForIdle()
    }

    private companion object {
        const val TAG = "rive"
    }
}
