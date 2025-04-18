package otus.homework.coroutines

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.squareup.picasso.Picasso

class CatsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), ICatsView {

//    var presenter :CatsPresenter? = null
    private val requestTag = "IMAGE_REQUEST_TAG"

    override fun setOnClickListener(p0: View.OnClickListener?) {
        findViewById<Button>(R.id.button).setOnClickListener(p0)
    }

//    override fun onFinishInflate() {
//        super.onFinishInflate()
//        findViewById<Button>(R.id.button).setOnClickListener {
//            Picasso.get().cancelTag(requestTag)
//            presenter?.onInitComplete()
//        }
//    }

    override fun populate(fact: UIFact) {
        findViewById<TextView>(R.id.fact_textView).text = fact.fact
        Picasso.get()
            .load(fact.url)
            .tag(requestTag)
            .placeholder(R.drawable.baseline_downloading_24)
            .error(R.drawable.ic_android_black_24dp)
            .into(findViewById<ImageView>(R.id.image))
    }

    override fun context(): Context {
        return context
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Picasso.get().cancelTag(requestTag)
    }
}

interface ICatsView {

    fun populate(fact: UIFact)

    fun context(): Context
}