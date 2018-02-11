package com.miruken.callback.policy

open class CovariantPolicy<A: Annotation, in C>(
        val key: (C) -> Any,
        build: CovariantPolicyBuilder.() -> Unit
) : CallbackPolicy() {

    init {
        val builder = CovariantPolicyBuilder()
        builder.build()
    }

    override fun getKey(callback: Any): Any? {
        TODO("not implemented")
    }

    override fun getCompatibleKeys(
            key:    Any,
            output: Collection<Any>
    ): Collection<Any> {
        TODO("not implemented")
    }
}