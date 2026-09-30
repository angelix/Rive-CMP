@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package dev.muazkadan.rivecmp

import nativeIosShared.RiveDataBindingViewModelInstance

// Throwaway: deliberate type mismatches so the compiler prints the real types.
internal fun probeTmp(i: RiveDataBindingViewModelInstance) {
    val n = i.numberPropertyFromPath("x")!!
    val t0: Unit = n
    val t1: Unit = n::addListener
    val t2: Unit = n::removeListener
    val t3: Unit = n.value()
    val t4: Unit = n.getValue()
    val t5: Unit = n::setValue
    val c = i.colorPropertyFromPath("x")!!
    val t6: Unit = c
    val t7: Unit = c.value()
    val t8: Unit = c::setRed
    val s = i.stringPropertyFromPath("x")!!
    val t9: Unit = s.value()
    val e = i.enumPropertyFromPath("x")!!
    val t10: Unit = e.value()
    val b = i.booleanPropertyFromPath("x")!!
    val t11: Unit = b.value()
    val t12: Unit = n.floatValue
    val t13: Unit = n.value
    val t14: Unit = n.name
    val t15: Unit = n.hasChanged
}
