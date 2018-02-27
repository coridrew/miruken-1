package com.miruken.callback

open class Handler : Handling {
    override fun handle(
            callback: Any,
            greedy:   Boolean,
            composer: Handling?
    ): HandleResult {
        val scope = composer ?: this as? CompositionScope
                  ?: CompositionScope(this)
        return handleCallback(callback, greedy, scope)
    }

    protected open fun handleCallback(
            callback: Any,
            greedy:   Boolean,
            composer: Handling
    ) = dispatch(this, callback, greedy, composer)

    companion object {
        fun dispatch(
                handler:  Any,
                callback: Any,
                greedy:   Boolean,
                composer: Handling
        ): HandleResult = when {
                ExcludeTypes.contains(handler::class) ->
                    HandleResult.NOT_HANDLED
                callback is DispatchingCallback ->
                    callback.dispatch(handler, greedy, composer)
                else ->
                    HandlesPolicy.dispatch(handler, callback, greedy, composer)
        }

        private val ExcludeTypes = setOf(Handler::class,
                CascadeHandler::class, CompositeHandler::class,
                CompositionScope::class)
    }
}