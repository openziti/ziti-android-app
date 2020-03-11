package io.netfoundry.ziti.sample

import android.os.Bundle
import android.os.Handler
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import io.netfoundry.ziti.android.Ziti
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    val url = "http://weather.ziti.netfoundry.io/Charlotte?format=3"

    override fun onCreate(savedInstanceState: Bundle?) {
        Ziti.init(applicationContext)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            loadData()
        }
    }

    fun loadData() = GlobalScope.async (Dispatchers.IO) {
        val body = async {
            val con = URL(url).openConnection() as HttpURLConnection
            con.addRequestProperty("Host", "wttr.in")
            if (con.responseCode > 200) {
                throw Exception(con.responseMessage)
            } else {
                con.inputStream.reader().readText()
            }
        }

        try {
            val text = body.await()
            result_text.post {
                result_text.text = text
            }
        } catch (ex: Exception) {
            showEx(ex)
        }
    }

    internal fun showEx(ex: Throwable) {
        Handler(mainLooper).post {
            Snackbar.make(fab, ex.localizedMessage, Snackbar.LENGTH_LONG)
                .setAction("Dismiss", null).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }
}
