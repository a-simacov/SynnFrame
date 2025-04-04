package com.synngate.synnframe.presentation.viewmodel

interface StateEventHandler<S> {
    fun handle(currentState: S): S
}