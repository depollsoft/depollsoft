package depollsoft.tagmaster

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

fun <TResult> bolts.Task<TResult>.asDeferred(): Deferred<TResult> {
    val deferred = CompletableDeferred<TResult>()
    this.continueWith {
        when {
            it.isCancelled -> deferred.completeExceptionally(CancellationException())
            it.isCompleted -> deferred.complete(it.result)
            it.isFaulted -> deferred.completeExceptionally(it.error)
            else -> deferred.completeExceptionally(IllegalStateException("Tasks must be in one of these states"))
        }
    }
    return deferred
}

suspend fun <TResult> bolts.Task<TResult>.await(): TResult = this.asDeferred().await()
suspend fun <TResult> bolts.Task<TResult>?.awaitOrNull(): TResult? = this?.asDeferred()?.await()