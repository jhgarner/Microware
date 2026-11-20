package com.example.microware.game.model

/** A trigger can only fire once every filter it watches names a tag. */
val Trigger.isComplete: Boolean
    get() = when (this) {
        Trigger.Start -> true
        is Trigger.Press -> !target.isEmpty
        is Trigger.Release -> !target.isEmpty
        is Trigger.Contact -> !firstTarget.isEmpty && !secondTarget.isEmpty
    }

/** An action can only run once every selector it uses names a tag or a parameter. */
val Action.isComplete: Boolean
    get() = when (this) {
        is Action.Move -> mover.isComplete
        is Action.MoveTo -> mover.isComplete && target.isComplete
        is Action.MoveWithin -> mover.isComplete && target.isComplete
        Action.Win, Action.Lose -> true
    }

private val Selector.isComplete: Boolean
    get() = this !is TagFilter || !isEmpty
