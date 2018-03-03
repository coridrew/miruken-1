package com.miruken.callback.policy

import com.miruken.runtime.isAssignableTo
import com.miruken.runtime.typeOf
import kotlin.reflect.KType

class BivariantPolicyBuilder<C: Any, out S: Any>(
        callbackType:  KType,
        targetType:    KType,
        targetFunctor: (C) -> S,
        private val output: CovariantPolicy,
        private val input:  ContravariantPolicy
) : CallbackPolicyBuilder(callbackType) {

    val key  = ReturnsKey
    val unit = ReturnsUnit

    val target: TargetArgument<C, S> =
            TargetArgument(callbackType, targetType, targetFunctor)

    inline fun <reified E: Any> extract(noinline block: (C) -> E) =
            ExtractArgument(typeOf<E>(), block)

    override fun build(): BivariantPolicy {
        return BivariantPolicy(rules, filters, output, input)
    }
}

class BivariantKeyBuilder {
    inline fun <reified C: Any> key(
            noinline keyFunctor: (C) -> Any
    ) = BivariantWithKeyBuilder(keyFunctor,  typeOf<C>())
}

class BivariantWithKeyBuilder<in C: Any>(
        val keyFunctor:   (C) -> Any,
        val callbackType: KType
) {
    inline fun <reified D: C, reified S: Any> target(
            noinline targetFunctor: (D) -> S
    ) = BivariantWithKeyTargetBuilder(keyFunctor,
            targetFunctor, typeOf<D>(), typeOf<S>())
}

class BivariantWithKeyTargetBuilder<C: Any, out S: Any>(
        private val keyFunctor:    (C) -> Any,
        private val targetFunctor: (C) -> S,
        private val callbackType:  KType,
        private val targetType:    KType
) {
    infix fun rules(
            define: BivariantPolicyBuilder<C,S>.() -> Unit
    ): BivariantPolicy {
        val co = CovariantPolicy(emptyList(), emptyList(), {
            @Suppress("UNCHECKED_CAST")
            if (isAssignableTo(callbackType, it))
                keyFunctor(it as C) else null
        })
        val contra = ContravariantPolicy(emptyList(), emptyList(), {
            @Suppress("UNCHECKED_CAST")
            if (isAssignableTo(callbackType, it))
                targetFunctor(it as C) else null
        })
        val builder = BivariantPolicyBuilder(callbackType, targetType,
                targetFunctor, co, contra)
        builder.define()
        return builder.build()
    }
}