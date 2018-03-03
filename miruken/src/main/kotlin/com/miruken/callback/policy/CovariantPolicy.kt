package com.miruken.callback.policy

import com.miruken.callback.FilteringProvider
import com.miruken.runtime.isAssignableTo

open class CovariantPolicy(
        rules:   List<MethodRule>,
        filters: List<FilteringProvider>,
        private val keyFunctor: (Any) -> Any?
) : CallbackPolicy(rules, filters) {

    constructor(
            build: CovariantKeyBuilder.() -> CovariantPolicy
    ) : this(CovariantKeyBuilder().build())

    private constructor(prototype: CovariantPolicy) : this(
            prototype.rules, prototype.filters, prototype.keyFunctor
    )

    override fun getKey(callback: Any): Any? =
            keyFunctor(callback)

    override fun getCompatibleKeys(
            key:       Any,
            available: Collection<Any>
    ): Collection<Any> = available.filter {
        key != it && isAssignableTo(key, it)
    }

    override fun compare(o1: Any?, o2: Any?): Int {
        return when {
            o1 == o2 -> 0
            o1 == null -> 1
            o2 == null -> -1
            isAssignableTo(o1, o2) -> -1
            else -> 1
        }
    }
}