/**
 * JS implementation of the view model instance API over @rive-app/canvas's ViewModelInstance.
 * The runtime reports changes through callbacks, which feed the flows here.
 */
package dev.muazkadan.rivecmp

import dev.muazkadan.rivecmp.utils.ExperimentalRiveCmpApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalRiveCmpApi::class)
internal class JsRiveViewModelInstance(private val instance: dynamic) : RiveViewModelInstance {

    private val observed = mutableListOf<dynamic>()

    override fun number(path: String): RiveProperty<Float>? =
        property(instance.number(path), read = { (it as Number).toFloat() }, write = { it })

    override fun string(path: String): RiveProperty<String>? =
        property(instance.string(path), read = { it as String }, write = { it })

    override fun boolean(path: String): RiveProperty<Boolean>? =
        property(instance.boolean(path), read = { it as Boolean }, write = { it })

    override fun color(path: String): RiveProperty<Int>? =
        property(instance.color(path), read = { (it as Number).toInt() }, write = { it })

    override fun enum(path: String): RiveProperty<String>? =
        property(instance.enum(path), read = { it as String }, write = { it })

    override fun trigger(path: String): RiveTrigger? {
        val property = instance.trigger(path) ?: return null
        observed.add(property)
        return JsRiveTrigger(property)
    }

    private fun <T> property(property: dynamic, read: (dynamic) -> T, write: (T) -> dynamic): RiveProperty<T>? {
        if (property == null) return null
        observed.add(property)
        return JsRiveProperty(property, read, write)
    }

    /** Removes the callbacks registered on the runtime's properties. */
    fun release() {
        observed.forEach { it.off() }
        observed.clear()
    }
}

@OptIn(ExperimentalRiveCmpApi::class)
private class JsRiveProperty<T>(
    private val property: dynamic,
    private val read: (dynamic) -> T,
    private val write: (T) -> dynamic,
) : RiveProperty<T> {
    private val flow = MutableStateFlow(read(property.value))

    init {
        property.on { newValue: dynamic -> flow.value = read(newValue) }
    }

    override var value: T
        get() = read(property.value)
        set(value) {
            property.value = write(value)
            flow.value = value
        }

    override val valueFlow: StateFlow<T> get() = flow
}

@OptIn(ExperimentalRiveCmpApi::class)
private class JsRiveTrigger(private val property: dynamic) : RiveTrigger {
    private val flow = MutableSharedFlow<Unit>(extraBufferCapacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    init {
        property.on { flow.tryEmit(Unit) }
    }

    override fun trigger() {
        property.trigger()
    }

    override val triggers: Flow<Unit> get() = flow
}
