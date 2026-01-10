package eu.kanade.tachiyomi.ui.base.viewmodel

import eu.kanade.tachiyomi.ui.base.controller.BaseCoroutineController
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

abstract class BaseViewModel<S : UiState, E : UiEvent, F : UiEffect> :
    BaseCoroutinePresenter<BaseCoroutineController<*, *>>() {
    private val initialState: S by lazy { initialState() }

    protected abstract fun initialState(): S

    val _uiState: MutableStateFlow<S> = MutableStateFlow(initialState)

    val state = _uiState.asStateFlow()

    val uiState: StateFlow<S> by lazy { _uiState.asStateFlow() }

    private val _uiEvent: MutableSharedFlow<E> = MutableSharedFlow()

    private val _uiEffect: MutableSharedFlow<F> = MutableSharedFlow()

    val uiEffect: Flow<F> = _uiEffect

    init {
        subscribeEvents()
    }

    protected abstract suspend fun handleEvent(event: E, state: S): S?

    protected open fun isSyncReduceEvent(): Boolean = true

    /**
     * 收集事件
     */
    private fun subscribeEvents() {
        //
        presenterScope.launch {
            _uiEvent.collect {
                reduceEvent(_uiState.value, it)
            }
        }
    }

    /**
     * 发送事件
     */
    fun sendEvent(event: E) {
        presenterScope.launch {
            _uiEvent.emit(event)
        }
    }

    fun sendEvent(eventBuild: () -> E) {
        sendEvent(eventBuild())
    }

    /**
     * 发送effect
     */
    protected fun sendEffect(effect: F) {
        presenterScope.launch { _uiEffect.emit(effect) }
    }

    protected fun sendEffect(effectBuild: () -> F) {
        sendEffect(effectBuild())
    }

    internal fun sendState(newState: S.() -> S) {
        _uiState.update {
            uiState.value.newState().apply {
                Timber.d("New state send: $this")
            }
        }
    }

    /**
     * 处理事件，更新状态
     * @param state S
     * @param event E
     */
    private suspend fun reduceEvent(state: S, event: E) {
        if (isSyncReduceEvent()) {
            presenterScope.launch {
                handleEvent(event, state)?.let { newState -> sendState { newState } }
            }
        } else {
            handleEvent(event, state)?.let { newState -> sendState { newState } }
        }
    }
}
