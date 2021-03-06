package com.miruken.mvc

import com.miruken.protocol.Protocol
import com.miruken.protocol.ProtocolAdapter
import com.miruken.protocol.proxy
import com.miruken.typeOf

enum class NavigationStyle { NEXT, PUSH }

@Protocol
interface Navigate {
    fun navigate(
            controllerKey: Any,
            style:         NavigationStyle,
            action:        Controller.() -> Unit
    )

    fun goBack()

    companion object {
        val PROTOCOL = typeOf<Navigate>()
        operator fun invoke(adapter: ProtocolAdapter) =
                adapter.proxy(PROTOCOL) as Navigate
    }
}

@Suppress("UNCHECKED_CAST")
inline fun <reified C: Controller> Navigate.next(
        noinline action:  C.() -> Unit
) = navigate(typeOf<C>(), NavigationStyle.NEXT,
        action as Controller.() -> Unit)

@Suppress("UNCHECKED_CAST")
inline fun <reified C: Controller> Navigate.push(
        noinline action:      C.() -> Unit
) = navigate(typeOf<C>(), NavigationStyle.PUSH,
        action as Controller.() -> Unit)

@Suppress("UNCHECKED_CAST")
inline fun <reified C: Controller> Navigate.navigate(
        style:                NavigationStyle,
        noinline action:      C.() -> Unit
) = navigate(typeOf<C>(), style,
        action as Controller.() -> Unit)
