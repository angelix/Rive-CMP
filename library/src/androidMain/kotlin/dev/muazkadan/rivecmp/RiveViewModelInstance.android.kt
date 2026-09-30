/**
 * Android implementation of the view model instance API over rive-android's ViewModelInstance, and
 * the helper that waits for rive-android to bind one.
 */
package dev.muazkadan.rivecmp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.core.ViewModelInstance
import app.rive.runtime.kotlin.core.ViewModelProperty
import app.rive.runtime.kotlin.core.ViewModelTriggerProperty
import app.rive.runtime.kotlin.core.errors.ViewModelException
import dev.muazkadan.rivecmp.utils.ExperimentalRiveCmpApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalRiveCmpApi::class)
internal class AndroidRiveViewModelInstance(
    private val instance: ViewModelInstance,
) : RiveViewModelInstance {

    override fun number(path: String): RiveProperty<Float>? = property { instance.getNumberProperty(path) }
    override fun string(path: String): RiveProperty<String>? = property { instance.getStringProperty(path) }
    override fun boolean(path: String): RiveProperty<Boolean>? = property { instance.getBooleanProperty(path) }
    override fun color(path: String): RiveProperty<Int>? = property { instance.getColorProperty(path) }
    override fun enum(path: String): RiveProperty<String>? = property { instance.getEnumProperty(path) }

    override fun trigger(path: String): RiveTrigger? =
        lookup { instance.getTriggerProperty(path) }?.let(::AndroidRiveTrigger)

    private fun <T> property(get: () -> ViewModelProperty<T>): RiveProperty<T>? =
        lookup(get)?.let(::AndroidRiveProperty)

    /** rive-android reports a missing or differently typed path by throwing. */
    private fun <P> lookup(get: () -> P): P? = try {
        get()
    } catch (e: ViewModelException) {
        null
    }
}

@OptIn(ExperimentalRiveCmpApi::class)
private class AndroidRiveProperty<T>(private val property: ViewModelProperty<T>) : RiveProperty<T> {
    override var value: T
        get() = property.value
        set(value) {
            property.value = value
        }

    override val valueFlow: StateFlow<T> get() = property.valueFlow
}

@OptIn(ExperimentalRiveCmpApi::class)
private class AndroidRiveTrigger(private val property: ViewModelTriggerProperty) : RiveTrigger {
    override fun trigger() = property.trigger()

    // The flow starts with a placeholder firing, which is not a firing by the graphic.
    override val triggers: Flow<Unit> = property.valueFlow.drop(1).map { }
}

/**
 * Calls [onViewModelInstance] once rive-android has set up [view]'s scene and bound a view model
 * instance. rive-android binds on attach for bytes and after the download for URLs and offers no
 * notification for it, so this waits frame by frame for the active artboard.
 */
@OptIn(ExperimentalRiveCmpApi::class)
@Composable
internal fun BindViewModelInstance(
    view: RiveAnimationView?,
    onViewModelInstance: ((RiveViewModelInstance) -> Unit)?,
) {
    val currentOnViewModelInstance by rememberUpdatedState(onViewModelInstance)
    val enabled = onViewModelInstance != null

    LaunchedEffect(view, enabled) {
        if (view == null || !enabled) return@LaunchedEffect
        while (view.controller.activeArtboard == null) withFrameNanos { }
        val instance = view.controller.activeArtboard?.viewModelInstance ?: return@LaunchedEffect
        currentOnViewModelInstance?.invoke(AndroidRiveViewModelInstance(instance))
    }
}
