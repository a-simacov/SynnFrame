package com.synngate.synnframe.domain.service

interface ClipboardService {

    fun copyToClipboard(text: String, label: String = ""): Boolean
}