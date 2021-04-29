package io.netfoundry.ziti.sample

import android.os.Bundle
import android.os.Handler
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import okhttp3.*
import org.openziti.Ziti
import java.io.IOException
import java.net.InetAddress
import java.security.KeyStore
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager


class MainActivity : AppCompatActivity() {

    private var client: OkHttpClient = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        val ks: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }
        Ziti.init(ks, false)
        val tmf: TrustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())

        tmf.init(ks)

        val tm: X509TrustManager = tmf.trustManagers[0] as X509TrustManager

        class DnsSystem : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                try {
                    val address: InetAddress? = Ziti.getDNSResolver().resolve(hostname)
                    if (address == null) {
                        return arrayListOf<InetAddress>(InetAddress.getByName(hostname))
                    }
                    return Collections.singletonList(address)
                } catch (e: Exception) {
                    println("Exception: " + e.message)
                }
                return Collections.emptyList()
            }
        }

        val d = DnsSystem()

        client = client.newBuilder()
            .socketFactory(Ziti.getSocketFactory())
            .sslSocketFactory(Ziti.getSSLSocketFactory(), tm)
            .dns(d)
            .callTimeout(5, TimeUnit.MINUTES)
            .build()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener {
            loadData()
        }
    }

    fun loadData() {
        val request = Request.Builder()
            .url("http://wttr.ziti")
            .header("host", "wttr.in")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                var body = ""
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    for ((name, value) in response.headers) {
                        println("$name: $value")
                    }

                    body = response.body!!.string()
                }

                try {
                    result_text.post {
                        result_text.text = body
                    }
                } catch (ex: Exception) {
                    showEx(ex)
                }
            }
        })
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
