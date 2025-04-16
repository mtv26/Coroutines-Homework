package otus.homework.coroutines

import android.widget.Toast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.SocketTimeoutException
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

class CatsPresenter(
    private val catsService: CatsService
) {

    private class PresenterScope: CoroutineScope {
        override val coroutineContext: CoroutineContext = Dispatchers.Main + CoroutineName("CatsCoroutine")
    }

    private val coroutineScope = PresenterScope()
    private var initJob: Job? = null

    private var _catsView: ICatsView? = null

    fun onInitComplete() {
        if (initJob?.isActive == true) return
        initJob = coroutineScope.launch {
            supervisorScope {
                val context = _catsView?.context() ?: return@supervisorScope
                try {
                    val response = withContext(Dispatchers.IO) {
                        catsService.getCatFact()
                    }
                    if (response.isSuccessful && response.body() != null) {
                        _catsView?.populate(response.body()!!)
                    } else {
                        response.errorBody()?.string()?.let { error ->
                            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    when (e) {
                        is SocketTimeoutException -> {
                            Toast.makeText(context, context.getText(R.string.timeoutError), Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            CrashMonitor.trackWarning()
                            Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    fun attachView(catsView: ICatsView) {
        _catsView = catsView
    }

    fun detachView() {
        _catsView = null
        coroutineScope.coroutineContext.cancelChildren()
    }
}