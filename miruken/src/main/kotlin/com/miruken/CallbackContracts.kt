package com.miruken

import kotlin.reflect.KType

interface ICallback {
    val resultType: KType?
    var result:     Any?
}

interface IAsyncCallback {
    val isAsync:    Boolean
    val wantsAsync: Boolean
}

interface IBoundCallback {
    val bounds: Any
}

interface IResolveCallback {
    fun getResolveCallback() : Any
}

interface IBatchCallback {
    val allowBatching: Boolean
}

interface IFilterCallback {
    val allowFiltering: Boolean
}

interface IDispatchCallback {
    val policy: CallbackPolicy?

    fun dispatch(
            handler:  Any,
            greedy:   Boolean,
            composer: IHandler
    ) : HandleResult
}