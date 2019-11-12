package io.netfoundry.ziti.sample

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import io.netfoundry.ziti.Ziti
import kotlinx.android.synthetic.main.activity_main.*
import java.security.KeyStore

class MainActivity : AppCompatActivity() {

    val SELECT_ENROLLMENT_JWT = 2171
    lateinit var ks: KeyStore

    override fun onCreate(savedInstanceState: Bundle?) {
        ks = KeyStore.getInstance("AndroidKeyStore")
        ks.load(null)

        Ziti.init(ks, true)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            R.id.enroll_ziti -> {
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type ="*/*"
                }
                startActivityForResult(intent, SELECT_ENROLLMENT_JWT)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when(requestCode) {
            SELECT_ENROLLMENT_JWT -> {
                if (resultCode == Activity.RESULT_OK) {
                    data?.data?.let {
                        enrollZiti(it)
                    }
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    fun enrollZiti(jwtUri: Uri) {

        val jwt = contentResolver.openInputStream(jwtUri)!!.readBytes()
        val name = Settings.Global.getString(contentResolver, "device_name")

        Thread {
            try {
                Ziti.enroll(ks, jwt, name)
                showResult("Enrollment Success!!")
            } catch (ex: Exception) {
                Log.w("sample", "exeption", ex)
                showResult(ex.localizedMessage)
            }
        }.start()
    }

    fun showResult(str: String) {
        Handler(mainLooper).post {
            Toast.makeText(this, str, LENGTH_LONG).show()

        }
    }
}
