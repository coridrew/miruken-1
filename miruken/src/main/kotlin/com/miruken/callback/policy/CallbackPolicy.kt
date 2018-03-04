package com.miruken.callback.policy

import com.miruken.callback.*
import kotlin.reflect.KClass

typealias CollectResultsBlock = (Any, Boolean) -> Boolean

abstract class CallbackPolicy(
        val rules:   List<MethodRule>,
        val filters: List<FilteringProvider>,
        val strict:  Boolean = false
) : Comparator<Any> {
    val methodBindingComparator : Comparator<PolicyMethodBinding> =
            Comparator { a, b -> compare(a.key, b.key) }

    fun match(method: CallableDispatch) =
            rules.firstOrNull { rule -> rule.matches(method) }

    open fun bindMethod(
            bindingInfo: PolicyMethodBindingInfo
    ): PolicyMethodBinding =
            PolicyMethodBinding(this, bindingInfo)

    open fun createKey(bindingInfo: PolicyMethodBindingInfo): Any? =
            bindingInfo.inKey ?: bindingInfo.outKey

    open fun getKey(callback: Any): Any? =
            (callback as? Callback)?.getCallbackKey()

    abstract fun getCompatibleKeys(
            key:       Any,
            available: Collection<Any>
    ): Collection<Any>

    open fun acceptResult(result: Any?, binding: PolicyMethodBinding) =
         when (result) {
             null, Unit -> HandleResult.NOT_HANDLED
             is HandleResult -> result
             else -> HandleResult.HANDLED
         }

    open fun approve(callback: Any, binding: PolicyMethodBinding) = true

    fun getMethods() = HandlerDescriptor.getPolicyMethods(this)

    fun getMethods(key: Any) = HandlerDescriptor.getPolicyMethods(this, key)

    fun dispatch(
            handler:  Any,
            callback: Any,
            greedy:   Boolean,
            composer: Handling,
            results:  CollectResultsBlock? = null
    ) = HandlerDescriptor.getDescriptorFor(handler::class)
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