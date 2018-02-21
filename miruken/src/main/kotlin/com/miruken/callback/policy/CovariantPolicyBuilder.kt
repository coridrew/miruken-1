package com.miruken.callback.policy

import com.miruken.runtime.getKType
import com.miruken.runtime.isAssignableTo
import kotlin.reflect.KType

class CovariantPolicyBuilder<C: Any>(
        policy:       CovariantPolicy,
        keyFunctor:   (C) -> Any,
        callbackType: KType
) : CallbackPolicyBuilder(policy, callbackType) {

    init {
        policy.keyFunctor = {
            @Suppress("UNCHECKED_CAST")
            if (isAssignableTo(callbackType, it))
                keyFunctor(it as C) else null
        }
    }

    val returnKey = ReturnsKey

    inline fun <reified E: Any> extract (noinline block: (C) -> E) =
            ExtractArgument(getKType<E>(), block)
}

class CovariantKeyBuilder(val policy: CovariantPolicy) {
    inline fun <reified C: Any> key(
            noinline keyFunctor: (C) -> Any
    ) = CovariantWithKeyBuilder(policy, keyFunctor,  getKType<C>())
}

@Suppress("MemberVisibilityCanBePrivate")
class CovariantWithKeyBuilder<C: Any>(
        val policy:       CovariantPolicy,
        val keyFunctor:   (C) -> Any,
        val callbackType: KType
) {
    inline infix fun rules(build: CovariantPolicyBuilder<C>.() -> Unit) {
        val builder = CovariantPolicyBuilder(policy, keyFunctor, callbackType)
        builder.build()
    }
}