// В каталоге presentation/common/lifecycle создадим файл RememberSaveable.kt
package com.synngate.synnframe.presentation.common.lifecycle

//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.saveable.Saver
//import androidx.compose.runtime.saveable.rememberSaveable
//import androidx.compose.ui.platform.LocalLifecycleOwner
//import androidx.lifecycle.flowWithLifecycle
//
///**
// * Сохраняет состояние с учетом жизненного цикла
// */
//@Composable
//fun <T : Any> rememberSaveableWithLifecycle(
//    key: String,
//    initialValue: T,
//    save: (T) -> Any = { it },
//    restore: (Any) -> T = { it as T }
//): T {
//    val lifecycleOwner = LocalLifecycleOwner.current
//
//    return rememberSaveable(
//        key = key,
//        saver = Saver(
//            save = save,
//            restore = restore
//        )
//    ) {
//        initialValue
//    }
//}