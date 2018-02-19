package com.miruken.callback.policy

import com.miruken.Flags
import com.miruken.concurrent.Promise
import com.miruken.runtime.*
import kotlin.reflect.*
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.withNullability

class Argument(val parameter: KParameter) {

    val logicalType: KType
    val flags:       Flags<ArgumentFlags>

    inline val parameterType get() = parameter.type
    inline val annotations   get() = parameter.annotations

    init {
        var type  = parameter.type
        var flags = if (type.isOpenGeneric)
            -ArgumentFlags.OPEN else -ArgumentFlags.NONE

        type = extractType(type, Lazy::class)?.let {
            flags += ArgumentFlags.LAZY; it } ?: type

        type = extractType(type, Promise::class)?.let {
            flags += ArgumentFlags.PROMISE; it } ?: type

        logicalType = extractType(type, Collection::class)?.let {
            flags += ArgumentFlags.COLLECTION; it } ?: type

        this.flags  = flags
    }

    private fun extractType(type: KType, criteria: KClass<*>) : KType? {
        return type.takeIf {
            it.isSubtypeOf(criteria.starProjectedType)
        } ?.let { it.arguments.firstOrNull()?.type}
    }
}