@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package dev.muazkadan.rivecmp

import nativeIosShared.RiveDataBindingViewModelInstance

// Throwaway: deliberate type mismatches so the compiler prints the real types.
internal fun probeTmp(i: RiveDataBindingViewModelInstance) {
    val n = i.numberPropertyFromPath("x")!!
    val t0: Nothing = n
    val t1: Nothing = n::addListener
    val t2: Nothing = n::removeListener
    val t3: Nothing = n.value()
    val t4: Nothing = n.getValue()
    val t5: Nothing = n::setValue
    val c = i.colorPropertyFromPath("x")!!
    val t6: Nothing = c
    val t7: Nothing = c.value()
    val t8: Nothing = c::setRed
    val s = i.stringPropertyFromPath("x")!!
    val t9: Nothing = s.value()
    val e = i.enumPropertyFromPath("x")!!
    val t10: Nothing = e.value()
    val b = i.booleanPropertyFromPath("x")!!
    val t11: Nothing = b.value()
}
