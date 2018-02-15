package com.miruken.callback.policy

import com.miruken.Flags
import com.miruken.concurrent.Promise
import com.miruken.isOpenGeneric
import kotlin.reflect.*
import kotlin.reflect.full.isSubclassOf

class Argument(val parameter: KParameter) {

    val parameterClass: KClass<*>?
    val logicalType:    KType
    val logicalClass:   KClass<*>?
    val flags:          Flags<ArgumentFlags>

    inline val parameterType get() = parameter.type
    inline val annotations   get() = parameter.annotations

    init {
        var type       = parameter.type
        parameterClass = getClass(type.classifier)

        var flags = ArgumentFlags.NONE
                .setFlag(ArgumentFlags.OPEN) { type.isOpenGeneric }
                .setFlag(ArgumentFlags.OPTIONAL) { type.isMarkedNullable }

        type = extractType(type, Function0::class)?.let {
            flags += ArgumentFlags.LAZY; it } ?: type

        type = extractType(type, Promise::class)?.let {
            flags += ArgumentFlags.PROMISE; it } ?: type

        logicalType = extractType(type, List::class)?.let {
            flags += ArgumentFlags.LIST; it } ?: type

        logicalClass = getClass(logicalType.classifier)
        this.flags   = flags
    }

    private fun getClass(classifier: KClassifier?) : KClass<*>? {
        return classifier as? KClass<*> ?:
            (classifier as? KTypeParameter)?.let {
                it.upperBounds.firstOrNull()?.classifier as? KClass<*>
        }
    }

    private fun extractType(type: KType, criteria: KClass<*>) : KType? {
        return (type.classifier as? KClass<*>)
                ?.takeIf { it.isSubclassOf(criteria) }
                ?.let { type.arguments.firstOrNull()?.type}
    }
}