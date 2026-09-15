package com.example.microware

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.positionChange
import kotlinx.coroutines.CancellationException
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.RestrictsSuspension
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.createCoroutineUnintercepted
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs

private const val DragDistance = 10

@RestrictsSuspension
sealed class PointerScope {
    abstract suspend fun next(): PointerInputChange
    abstract fun claim()
    abstract fun unexpected(event: PointerInputChange): Nothing

    suspend fun expectDown(): PointerInputChange {
        val pointer = next()
        if (!pointer.changedToDown() || pointer.isConsumed) {
            unexpected(pointer)
        }
        return pointer
    }

    suspend fun expectFor(id: PointerId): PointerInputChange {
        return expectFor(listOf(id))
    }

    suspend fun expectFor(ids: Collection<PointerId>): PointerInputChange {
        val pointer = next()
        if (!ids.contains(pointer.id) || pointer.isConsumed) {
            println("Failing in expectFor because $pointer")
            unexpected(pointer)
        }
        return pointer
    }

    suspend fun expectTap(): PointerInputChange {
        val pointer = expectDown()
        while (true) {
            val latest = expectFor(pointer.id)
            if ((latest.position - pointer.position).getDistance() > DragDistance) {
                unexpected(latest)
            }
            if (latest.changedToUp()) {
                claim()
                return latest
            }
        }
    }

    suspend fun followDrag(chaser: (PointerInputChange) -> Unit): PointerInputChange {
        val initial = expectDown()
        println("Starting with $initial")
        var dragging = false
        var traveled = 0f
        while (true) {
            val latest = expectFor(initial.id)
            println("Latest is $latest")
            if (latest.changedToUp()) {
                if (!dragging) {
                    println("Exiting because not a drag")
                    unexpected(latest)
                }
                return latest
            }
            traveled += latest.positionChange().getDistance()
            val wasDragging = dragging
            if (traveled > DragDistance) {
                dragging = true
            }
            if (dragging) {
                claim()
                if (!wasDragging) {
                    chaser(initial)
                }
                chaser(latest)
            }
        }
    }

    suspend fun followHorizontalDrag(chaser: (PointerInputChange) -> Unit): PointerInputChange {
        val initial = expectDown()
        var dragging = false
        var accumulated = Offset.Zero
        while (true) {
            val latest = expectFor(initial.id)
            if (latest.changedToUp()) {
                if (!dragging) {
                    unexpected(latest)
                }
                return latest
            }
            accumulated += latest.positionChange()
            if (!dragging && accumulated.getDistance() > DragDistance) {
                if (abs(accumulated.x) <= abs(accumulated.y)) {
                    unexpected(latest)
                }
                dragging = true
            }
            if (dragging) {
                claim()
                latest.consume()
                chaser(latest)
            }
        }
    }

    suspend fun followPinch(chaser: (Offset, Float) -> Unit): Pair<Offset, Float> {
        var first = expectDown()
        untilThrows<UnexpectedEvent> { first = expectFor(first.id) }
        var second = expectDown()
        claim()
        val center = (first.position + second.position) / 2f
        val size = (first.position - second.position).getDistance()
        chaser(center, size)

        while (true) {
            val new = expectFor(listOf(first.id, second.id))
            if (new.id == first.id) {
                first = new
            } else {
                second = new
            }
            val center = (first.position + second.position) / 2f
            val size = (first.position - second.position).getDistance()
            if (new.changedToUp()) {
                return center to size
            }
            chaser(center, size)
        }
    }
}

private class PointerStateMachine private constructor() : PointerScope() {
    var readded = ArrayDeque<PointerInputChange>()

    constructor(block: suspend PointerStateMachine.() -> Unit) : this() {
        block.createCoroutineUnintercepted(
            this, Continuation(
                EmptyCoroutineContext
            ) { it.getOrThrow().apply { complete = true; claimed = true } }
        ).resume(Unit)
    }

    var rest: Continuation<PointerInputChange>? = null
    var claimed = false
    var complete = false

    override suspend fun next(): PointerInputChange {
        readded.removeFirstOrNull()?.let { return it }
        return suspendCoroutineUninterceptedOrReturn { cont ->
            rest = cont
            COROUTINE_SUSPENDED
        }
    }

    override fun claim() {
        claimed = true
    }

    override fun unexpected(event: PointerInputChange): Nothing {
        readded.addLast(event)
        throw UnexpectedEvent()
    }

    fun resume(event: PointerInputChange) {
        val rest = this.rest
        this.rest = null
        rest?.resume(event)
    }
}

private class UnexpectedEvent : Exception()

class PointerRacer(val gestures: MutableList<suspend PointerScope.() -> Unit> = mutableListOf())

suspend fun PointerInputScope.racing(block: suspend context(PointerRacer) () -> Unit) {
    val racer = PointerRacer()
    context(racer) {
        block()
    }
    awaitEachGesture {
        var machines = mutableListOf<PointerStateMachine>()
        for (gesture in racer.gestures) {
            machines.add(PointerStateMachine(gesture))
        }
        try {
            while (machines.isNotEmpty()) {
                val pointerChangesFirst = awaitPointerEvent().changes.sortedByDescending {
                    it.changedToDown()
                }
                for (event in pointerChangesFirst) {
                    val nextMachines = mutableListOf<PointerStateMachine>()
                    for (machine in machines) {
                        try {
                            machine.resume(event)
                        } catch (_: UnexpectedEvent) {
                            continue
                        }
                        if (machine.claimed) {
                            machines.forEach {
                                if (it != machine) {
                                    it.runCatching {
                                        it.rest?.resumeWithException(CancellationException("Claimed by other"))
                                    }
                                }
                            }
                            nextMachines.clear()
                            if (!machine.complete) {
                                nextMachines.add(machine)
                            }
                            break
                        }
                        if (machine.complete) {
                            continue
                        }
                        nextMachines.add(machine)
                    }
                    machines = nextMachines
                }
            }
        } finally {
            machines.forEach {
                it.runCatching {
                    it.rest?.resumeWithException(CancellationException("cancelled"))
                }
            }
        }
    }
}


context(racer: PointerRacer)
fun <R> sequence(block: suspend PointerScope.() -> R) {
    racer.gestures.add { block() }
}
