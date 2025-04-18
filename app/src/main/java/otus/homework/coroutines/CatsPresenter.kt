package otus.homework.coroutines

import android.net.Uri
import android.widget.Toast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
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
    private val factService: CatsService,
    private val imageService: ImageService
) {

    private class PresenterScope: CoroutineScope {
        override val coroutineContext: CoroutineContext = Dispatchers.Main + CoroutineName("CatsCoroutine")
    }

    private val coroutineScope = PresenterScope()
    private var initJob: Job? = null

    private var _catsView: ICatsView? = null

    fun onInitComplete() {
        initJob?.cancel()
        initJob = coroutineScope.launch {
            supervisorScope {
                val context = _catsView?.context() ?: return@supervisorScope
                try {
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

                    _catsView?.populate(uiFact)
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