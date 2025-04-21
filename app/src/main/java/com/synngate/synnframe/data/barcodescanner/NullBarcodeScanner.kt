package com.synngate.synnframe.data.barcodescanner

import com.synngate.synnframe.domain.common.BarcodeScanner
import com.synngate.synnframe.domain.common.ScanResultListener
import com.synngate.synnframe.domain.common.ScannerManufacturer


class NullBarcodeScanner : BarcodeScanner {
    private var isInitializedState = false
    private var isEnabledState = false

    override suspend fun initialize(): Result<Unit> {
        isInitializedState = true
        return Result.success(Unit)
    }

    override suspend fun dispose() {
        isInitializedState = false
        isEnabledState = false
    }

    override suspend fun enable(listener: ScanResultListener): Result<Unit> {
        isEnabledState = true
        return Result.success(Unit)
    }

    override suspend fun disable() {
        isEnabledState = false
    }

    override fun isInitialized(): Boolean = isInitializedState

    override fun isEnabled(): Boolean = isEnabledState

    override fun getManufacturer(): ScannerManufacturer = ScannerManufacturer.UNKNOWN
}