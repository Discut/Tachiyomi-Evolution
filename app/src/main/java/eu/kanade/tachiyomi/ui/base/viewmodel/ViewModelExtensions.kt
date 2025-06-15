package eu.kanade.tachiyomi.ui.base.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 订阅Effect事件
 *
 * @param lifecycleState 生命周期阶段
 * @param sideEffect 事件消费方法
 */
@Composable
fun <S : UiState, E : UiEvent, F : UiEffect> BaseViewModel<S, E, F>.CollectSideEffect(
    lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
    sideEffect: (suspend (sideEffect: F) -> Unit),
) {
    val sideEffectFlow = this.uiEffect
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(sideEffectFlow, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(lifecycleState) {
            sideEffectFlow.collect { sideEffect(it) }
        }
    }
}

/**
 * Collect state from ViewModel
 */
fun <S : UiState, E : UiEvent, F : UiEffect> BaseViewModel<S, E, F>.collectState(
    lifecycle: Lifecycle,
    lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
    handleState: (suspend (state: S) -> Unit),
) {
    val stateFlow = this.uiState
    lifecycle.coroutineScope.launch(Dispatchers.Main) {
        lifecycle.repeatOnLifecycle(lifecycleState) {
            stateFlow.collect { handleState(it) }
        }
    }
}

/**
 * Collect effect from ViewMode
 */
fun <S : UiState, E : UiEvent, F : UiEffect> BaseViewModel<S, E, F>.collectEffect(
    lifecycle: Lifecycle,
    lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
    sideEffect: (suspend (sideEffect: F) -> Unit),
) {
    val sideEffectFlow = this.uiEffect
    lifecycle.coroutineScope.launch(Dispatchers.Main) {
        lifecycle.repeatOnLifecycle(lifecycleState) {
            sideEffectFlow.collect { sideEffect(it) }
        }
    }
}
