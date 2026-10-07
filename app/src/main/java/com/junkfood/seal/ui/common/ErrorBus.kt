package com.junkfood.seal.ui.common

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 全局报错提示总线
 *
 * 任何地方（ViewModel、Downloader、后台协程）调用 [post] 发送错误消息，
 * HomeEntry 顶层的 SnackbarHost 会统一展示为 Material3 Snackbar。
 * 线程安全，可在任意线程调用。
 */
object ErrorBus {
    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    fun post(message: String) {
        _errors.tryEmit(message)
    }
}
