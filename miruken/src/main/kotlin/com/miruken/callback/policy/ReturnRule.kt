package com.miruken.callback.policy

interface ReturnRule {
    fun matches(method: CallableDispatch) : Boolean

    fun configure(bindingInfo: PolicyMethodBindingInfo) {}

    infix fun and(otherRule: ReturnRule) =
            ReturnRuleConjunction(this, otherRule)

    infix fun or(otherRule: ReturnRule) =
            ReturnRuleDisjunction(this, otherRule)
}

