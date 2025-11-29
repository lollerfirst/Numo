package com.electricdreams.numo.util

import android.os.Handler
import android.os.Looper

object HandlerUtils {
    val mainHandler: Handler by lazy { Handler(Looper.getMainLooper()) }

    fun post(action: () -> Unit) {
        mainHandler.post(action)
    }

    fun post(runnable: Runnable) {
        mainHandler.post(runnable)
    }

    fun postDelayed(action: () -> Unit, delayMillis: Long) {
        mainHandler.postDelayed(action, delayMillis)
    }

    fun postDelayed(runnable: Runnable, delayMillis: Long) {
        mainHandler.postDelayed(runnable, delayMillis)
    }

    fun removeCallbacks(runnable: Runnable) {
        mainHandler.removeCallbacks(runnable)
    }

    fun removeCallbacksAndMessages(token: Any?) {
        mainHandler.removeCallbacksAndMessages(token)
    }
}
