package com.synngate.synnframe.presentation.common

import androidx.compose.runtime.compositionLocalOf
import com.synngate.synnframe.data.barcodescanner.ScannerService
import com.synngate.synnframe.domain.entity.User

/**
 * CompositionLocal для хранения данных о текущем пользователе
 */
val LocalCurrentUser = compositionLocalOf<User?> { null }

/**
 * CompositionLocal для хранения ScannerService
 */
val LocalScannerService = compositionLocalOf<ScannerService?> { null }