package com.miruken.runtime

import com.miruken.concurrent.Promise
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.isSuperclassOf

fun isAssignable(leftSide: Any, rightSide: Any?): Boolean {
    return when (leftSide) {
        is KType -> when (rightSide) {
            null -> false
            is KType -> rightSide.isSubtypeOf(leftSide)
            is Class<*> -> leftSide.arguments.isEmpty() &&
                    rightSide.typeParameters.isEmpty() &&
                    (leftSide.classifier as? KClass<*>)
                            ?.java?.isAssignableFrom(rightSide) == true
            else -> {
                val rightClass = (rightSide as? KClass<*>)
                        ?: (rightSide as? Class<*>)?.kotlin
                        ?: rightSide::class
                leftSide.arguments.isEmpty() &&
                        rightClass.typeParameters.isEmpty() &&
                        (leftSide.classifier as? KClass<*>)
                                ?.isSuperclassOf(rightClass) == true
            }
        }
        is KClass<*> -> when (rightSide) {
            null -> false
            is KType -> rightSide.arguments.isEmpty() &&
                    leftSide.typeParameters.isEmpty() &&
                    (rightSide.classifier as? KClass<*>)
                            ?.isSubclassOf(leftSide) == true
            is Class<*> -> leftSide.typeParameters.isEmpty() &&
                    rightSide.typeParameters.isEmpty() &&
                    leftSide.java.isAssignableFrom(rightSide)
            else -> {
                val rightClass = (rightSide as? KClass<*>)
                        ?: rightSide::class
                leftSide.typeParameters.isEmpty() &&
                        rightClass.typeParameters.isEmpty() &&
                        rightClass.isSubclassOf(leftSide)
            }
        }
        is Class<*> ->when (rightSide) {
            null -> false
            is KType -> rightSide.arguments.isEmpty() &&
                    leftSide.typeParameters.isEmpty() &&
                    (rightSide.classifier as? KClass<*>)?.let {
                        leftSide.isAssignableFrom(it.java)
                    } == true
            is Class<*> -> leftSide.typeParameters.isEmpty() &&
                    rightSide.typeParameters.isEmpty() &&
                    leftSide.isAssignableFrom(rightSide)
            else -> {
                val rightClass = (rightSide as? KClass<*>)
                        ?: rightSide::class
                leftSide.typeParameters.isEmpty() &&
                        rightClass.typeParameters.isEmpty() &&
                        leftSide.isAssignableFrom(rightClass.java)
            }
        }
        else -> false
    }
}

fun Iterable<*>.filterIsAssignable(key: Any): List<Any> {
    return filterIsAssignable(ArrayList(), key)
}

fun Iterable<*>.filterIsAssignable(
        destination: ArrayList<Any>, key: Any
): MutableList<Any> {
    filter { isAssignable(key, it) }.mapTo(destination) { it!! }
    return destination
}


val KType.isOpenGeneric: Boolean
    get() = classifier is KTypeParameter ||
                arguments.any { it.type?.isOpenGeneric == true }

val KType.componentType: KType?
    get() = if (isSubtypeOf(COLLECTION_TYPE))
        arguments.single().type else this

inline fun <reified T> KAnnotatedElement.getTaggedAnnotations() =
        annotations.flatMap { it.annotationClass.annotations }
                   .filterIsInstance<T>()

val COLLECTION_TYPE = getKType<Collection<Any>>()

val PROMISE_TYPE = getKType<Promise<Any>>()