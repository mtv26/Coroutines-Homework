package otus.homework.coroutines

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    val vm by viewModels<CatsViewModel> {
        CatsViewModel.provideFactory(DiContainer())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val view = layoutInflater.inflate(R.layout.activity_main, null) as CatsView
        view.setOnClickListener {
            vm.onInitComplete()
        }
        setContentView(view)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                vm.state.collectLatest {
                    when (it) {
                        is Result.Error -> {
                            if (it.isTimeout) {
                                Toast.makeText(this@MainActivity, this@MainActivity.getText(R.string.timeoutError), Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@MainActivity, it.msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                        is Result.Success<*> -> {
                            view.populate(it.result as UIFact)
                        }
                    }
                }
            }
        }
    }

}