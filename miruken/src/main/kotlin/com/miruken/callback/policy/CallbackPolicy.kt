package com.miruken.callback.policy

import com.miruken.callback.*
import kotlin.reflect.KClass

typealias CollectResultsBlock = (Any, Boolean) -> Boolean

abstract class CallbackPolicy : Comparator<Any> {
    private val _rules   = mutableListOf<MethodRule>()
    private val _filters = mutableListOf<FilteringProvider>()

    fun addRule(vararg rules: MethodRule) =
            _rules.addAll(rules)

    fun addFilters(vararg filters: FilteringProvider) =
            _filters.addAll(filters)

    fun match(method: MethodDispatch) =
            _rules.firstOrNull { rule -> rule.matches(method) }

    open fun bindMethod(
            bindingInfo: PolicyMethodBindingInfo
    ): PolicyMethodBinding =
            PolicyMethodBinding(this, bindingInfo)

    open fun createKey(bindingInfo: PolicyMethodBindingInfo): Any? =
            bindingInfo.inKey ?: bindingInfo.outKey

    abstract fun getKey(callback: Any) : Any?

    abstract fun getCompatibleKeys(
            key:    Any,
            output: Collection<Any>
    ): Collection<Any>

    open fun approve(callback: Any, annotation: Annotation) = true

    fun getMethods() = HandlerDescriptor.getPolicyMethods(this)

    fun getMethods(key: Any) = HandlerDescriptor.getPolicyMethods(this, key)

    fun dispatch(
            handler:  Any,
            callback: Any,
            greedy:   Boolean,
            composer: Handling,
            results:  CollectResultsBlock? = null
    ) = HandlerDescriptor.getDescriptor(handler::class)
            .dispatch(this, handler, callback, greedy, composer, results)

    companion object {
        fun getCallbackHandlerClasses(callback: Any): List<KClass<*>> {
            val policy = getCallbackPolicy(callback)
            return HandlerDescriptor.getHandlersClasses(policy, callback)
        }

        fun getCallbackMethods(callback: Any): List<PolicyMethodBinding> {
            val policy = getCallbackPolicy(callback)
            return policy.getKey(callback)?.let {
                policy.getMethods(it)
            } ?: emptyList()
        }

        fun getCallbackPolicy(callback: Any) =
                (callback as? DispatchingCallback)?.policy ?: HandlesPolicy
    }
}