package pl.recipesforsoftware.signalbrief.data.repository

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.posix.gettimeofday
import platform.posix.timeval

private const val MILLIS_PER_SECOND = 1000L
private const val MICROS_PER_MILLI = 1000L

/**
 * iOS epoch-millisecond timestamp backed by POSIX [gettimeofday].
 */
@OptIn(ExperimentalForeignApi::class)
internal actual fun currentTimeMillis(): Long =
    memScoped {
        val tv = alloc<timeval>()
        gettimeofday(tv.ptr, null)
        tv.tv_sec * MILLIS_PER_SECOND + tv.tv_usec / MICROS_PER_MILLI
    }
