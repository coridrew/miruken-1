package com.miruken.callback

import com.miruken.callback.policy.PolicyMemberBinding
import com.miruken.concurrent.Promise
import com.miruken.concurrent.all
import com.miruken.runtime.ANY_STAR
import com.miruken.runtime.isCompatibleWith
import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.createType
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.jvmErasure

open class Inquiry(
        val key:    Any,
        val many:   Boolean = false,
        val parent: Inquiry? = null
) : Callback, AsyncCallback, DispatchingCallback,
    DispatchingCallbackGuard {
    private var _result: Any? = null
    private val _promises     = mutableListOf<Promise<*>>()
    private val _resolutions  = mutableListOf<Any>()
    private val _keyType: KType by lazy {
        when (key) {
            is KType -> key
            is KClass<*> -> key.starProjectedType
            else -> ANY_STAR
        }
    }
    private var _handler: Any? = null
    private var _binding: PolicyMemberBinding? = null

    override var wantsAsync: Boolean = false

    final override var isAsync: Boolean = false
        private set

    override val policy get() = ProvidesPolicy

    val resolutions: List<Any> get() = _resolutions.toList()

    val keyClass: KClass<*>?
        get() = (key as? KType)?.jvmErasure

    open fun createKeyInstance(): Any? {
        return when (key) {
            is KType -> {
                val clazz = key.jvmErasure
                if (clazz.isAbstract || clazz.java.isInterface ||
                        clazz.javaPrimitiveType != null) null
                else clazz.createInstance()
            }
            is KClass<*> ->
                if (key.isAbstract || key.java.isInterface ||
                        key.javaPrimitiveType != null) null
                else key.createInstance()
            is Class<*> ->
                if (key.isInterface || key.isPrimitive ||
                        Modifier.isAbstract(key.modifiers)) null
                else key.newInstance()
            else -> null
        }
    }

    override val resultType: KType?
        get() = _keyType.let {
            var resultType = it
            if (many) {
                resultType = List::class.createType(listOf(
                        KTypeProjection.invariant(resultType)))
            }
            if (wantsAsync || isAsync) {
                resultType = Promise::class.createType(listOf(
                        KTypeProjection.invariant(resultType)))
            }
            return resultType
        }

    override var result: Any?
        get() {
            if (_result == null) {
                _result = if (isAsync) {
                    Promise.all(_promises) then {
                        val flat = flatten(_resolutions, it)
                        if (many) flat else flat.firstOrNull()
                    }
                } else {
                    val flat = flatten(_resolutions)
                    if (many) flat else flat.firstOrNull()
                }
            }
            if (isAsync) {
                if (!wantsAsync) {
                    _result = (_result as? Promise<*>)?.get()
                }
            } else if (wantsAsync) {
                _result = _result?.let { Promise.resolve(it) }
                        ?: Promise.EMPTY
            }
            return _result
        }
        set(value) {
            _result = value
            isAsync = _result is Promise<*>
        }

    fun resolve(resolution: Any, composer: Handling) =
            resolve(resolution, false, false, composer)

    fun resolve(
            resolution: Any,
            strict:     Boolean,
            greedy:     Boolean,
            composer:   Handling
    ): Boolean {
        val resolved = when {
            !strict && resolution is Collection<*> ->
                resolution.filterNotNull().fold(false) { s, res ->
                    include(res, false, greedy, composer) || s
                }
            !strict && resolution is Array<*> ->
                resolution.filterNotNull().fold(false) { s, res ->
                    include(res, false, greedy, composer) || s
                }
            else -> include(resolution, strict, greedy, composer)
        }
        if (resolved) _result = null
        return resolved
    }

    private fun include(
            resolution: Any,
            strict:     Boolean,
            greedy:     Boolean,
            composer:   Handling
    ): Boolean {
        if (resolution is Promise<*>) {
            isAsync = true
            _promises.add(resolution.then { r ->
                when {
                    !strict && r is Collection<*> -> r.filter {
                        it != null && isSatisfied(it, greedy, composer)
                    }
                    !strict && r is Array<*> -> r.filter {
                        it != null && isSatisfied(it, greedy, composer)
                    }
                    else ->
                        r?.takeIf { isSatisfied(it, greedy, composer) }
                }
            })
        } else if (!isSatisfied(resolution, greedy, composer)) {
            return false
        } else {
            _resolutions.add(resolution)
        }
        return true
    }

    protected open fun isSatisfied(
            resolution: Any,
            greedy:     Boolean,
            composer:   Handling
    ): Boolean = true

    override fun canDispatch(
            handler: Any,
            binding: PolicyMemberBinding
    ): Boolean {
        if (inProgress(handler, binding)) return false
        _handler = handler
        _binding = binding
        return true
    }

    override fun dispatch(
            handler:      Any,
            callbackType: KType?,
            greedy:       Boolean,
            composer:     Handling
    ): HandleResult {
        when (key) {
            is KType -> (key.classifier as? KClass<*>)?.objectInstance
            is KClass<*> -> key.objectInstance
            else -> null
        }?.also {
            if (include(it, true, greedy, composer)) {
                return HandleResult.HANDLED_AND_STOP
            }
        }
        val result = if (implied(handler, greedy, composer))
            HandleResult.HANDLED else HandleResult.NOT_HANDLED
        if (result.handled && !greedy) return result

        val count = _resolutions.size + _promises.size
        return result then {
            policy.dispatch(
                    handler, this@Inquiry, callbackType, greedy, composer)
            { r, strict -> resolve(r, strict, greedy, composer) }
        } then {
            if (_resolutions.size + _promises.size > count)
                HandleResult.HANDLED else HandleResult.NOT_HANDLED
        }
    }

    private fun implied(
            item:      Any,
            greedy:    Boolean,
            composer:  Handling
    ) = isCompatibleWith(key, item) &&
            resolve(item, false, greedy, composer)

    private fun inProgress(
            handler: Any,
            binding: PolicyMemberBinding
    ): Boolean {
        return (handler == _handler  &&
                binding == _binding) ||
                parent?.inProgress(handler, binding) == true
    }

    private fun flatten(vararg lists: List<*>): List<Any> {
        val flat = mutableSetOf<Any>()
        lists.flatMap { it }
             .forEach { when (it) {
                 null -> return@forEach
                 is Iterable<*> -> flat.addAll(it.filterNotNull())
                 is Array<*> -> flat.addAll(it.filterNotNull())
                 else -> flat.add(it)
             } }
        return flat.toList()
    }
}

