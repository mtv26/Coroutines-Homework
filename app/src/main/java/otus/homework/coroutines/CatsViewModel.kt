package otus.homework.coroutines

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.net.SocketTimeoutException

class CatsViewModel(diContainer: DiContainer): ViewModel() {

    private val internalState = MutableSharedFlow<Result>()
    val state = internalState.asSharedFlow()
    private var initJob: Job? = null
    private val factService = diContainer.factService
    private val imageService = diContainer.imageService

    private val exceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        when (throwable) {
            is SocketTimeoutException -> {
                internalState.tryEmit(Result.Error(isTimeout = true))
            }
            else -> {
                internalState.tryEmit(Result.Error(msg = throwable.message, isTimeout = false))
            }
        }
        CrashMonitor.trackWarning()
    }

    init {
        onInitComplete()
    }

    fun onInitComplete() {
        initJob?.cancel()
        initJob = viewModelScope.launch(exceptionHandler) {
            supervisorScope {
                    val fact = async(Dispatchers.IO) {
                        factService.getCatFact().let { r ->
                            if (r.isSuccessful && r.body() != null) {
                                r.body()!!
                            } else {
                                throw Exception(r.errorBody()?.string())
                            }
                        }
                    }
                    val image = async(Dispatchers.IO) {
                        imageService.getCatImage().let { r ->
                            if (r.isSuccessful && r.body() != null) {
                                r.body()!!
                            } else {
                                throw Exception(r.errorBody()?.string())
                            }
                        }
                    }

                    val uiFact = UIFact(fact.await().fact, Uri.parse(image.await().firstOrNull()?.url ?: ""))

                internalState.emit(Result.Success(uiFact))
            }
        }
    }

    companion object {
        fun provideFactory(
            diContainer: DiContainer,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CatsViewModel(diContainer) as T
                }
            }
    }
}