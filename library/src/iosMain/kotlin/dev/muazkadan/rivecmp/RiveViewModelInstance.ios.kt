/**
 * iOS implementation of the view model instance API over rive-ios's data binding instance.
 * rive-ios reports changes through listeners, which feed the flows here.
 */
@file:OptIn(ExperimentalForeignApi::class)

package dev.muazkadan.rivecmp

import dev.muazkadan.rivecmp.utils.ExperimentalRiveCmpApi
import kotlinx.cinterop.DoubleVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import nativeIosShared.RiveDataBindingViewModelInstance
import platform.UIKit.UIColor
import kotlin.math.roundToInt

@OptIn(ExperimentalRiveCmpApi::class)
internal class IosRiveViewModelInstance(
    private val instance: RiveDataBindingViewModelInstance,
) : RiveViewModelInstance {

    private val releases = mutableListOf<() -> Unit>()

    override fun number(path: String): RiveProperty<Float>? {
        val property = instance.numberPropertyFromPath(path) ?: return null
        val flow = MutableStateFlow(property.value)
        val listener = property.addListener { flow.value = it }
        releases += { property.removeListener(listener) }
        return IosRiveProperty(flow, read = { property.value }, write = { property.value = it })
    }

    override fun string(path: String): RiveProperty<String>? {
        val property = instance.stringPropertyFromPath(path) ?: return null
        val flow = MutableStateFlow(property.value)
        val listener = property.addListener { flow.value = it.orEmpty() }
        releases += { property.removeListener(listener) }
        return IosRiveProperty(flow, read = { property.value }, write = { property.value = it })
    }

    override fun boolean(path: String): RiveProperty<Boolean>? {
        val property = instance.booleanPropertyFromPath(path) ?: return null
        val flow = MutableStateFlow(property.value)
        val listener = property.addListener { flow.value = it }
        releases += { property.removeListener(listener) }
        return IosRiveProperty(flow, read = { property.value }, write = { property.value = it })
    }

    override fun color(path: String): RiveProperty<Int>? {
        val property = instance.colorPropertyFromPath(path) ?: return null
        val flow = MutableStateFlow(property.value.toArgb())
        val listener = property.addListener { color -> color?.let { flow.value = it.toArgb() } }
        releases += { property.removeListener(listener) }
        return IosRiveProperty(
            flow,
            read = { property.value.toArgb() },
            write = { argb ->
                property.setRed(
                    red = ((argb shr 16) and 0xFF) / 255.0,
                    green = ((argb shr 8) and 0xFF) / 255.0,
                    blue = (argb and 0xFF) / 255.0,
                    alpha = ((argb shr 24) and 0xFF) / 255.0,
                )
            },
        )
    }

    override fun enum(path: String): RiveProperty<String>? {
        val property = instance.enumPropertyFromPath(path) ?: return null
        val flow = MutableStateFlow(property.value)
        val listener = property.addListener { flow.value = it.orEmpty() }
        releases += { property.removeListener(listener) }
        return IosRiveProperty(flow, read = { property.value }, write = { property.value = it })
    }

    override fun trigger(path: String): RiveTrigger? {
        val property = instance.triggerPropertyFromPath(path) ?: return null
        val flow = MutableSharedFlow<Unit>(extraBufferCapacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        val listener = property.addListener { flow.tryEmit(Unit) }
        releases += { property.removeListener(listener) }
        return object : RiveTrigger {
            override fun trigger() = property.trigger()
            override val triggers: Flow<Unit> get() = flow
        }
    }

    /** Removes the listeners registered on rive-ios's properties. */
    fun release() {
        releases.forEach { it() }
        releases.clear()
    }
}

@OptIn(ExperimentalRiveCmpApi::class)
private class IosRiveProperty<T>(
    private val flow: MutableStateFlow<T>,
    private val read: () -> T,
    private val write: (T) -> Unit,
) : RiveProperty<T> {
    override var value: T
        get() = read()
        set(value) {
            write(value)
            flow.value = value
        }

    override val valueFlow: StateFlow<T> get() = flow
}

/** This color as an ARGB integer. */
private fun UIColor.toArgb(): Int = memScoped {
    val red = alloc<DoubleVar>()
    val green = alloc<DoubleVar>()
    val blue = alloc<DoubleVar>()
    val alpha = alloc<DoubleVar>()
    getRed(red.ptr, green.ptr, blue.ptr, alpha.ptr)
    fun channel(component: Double) = (component * 255).roundToInt().coerceIn(0, 255)
    (channel(alpha.value) shl 24) or (channel(red.value) shl 16) or (channel(green.value) shl 8) or channel(blue.value)
}
