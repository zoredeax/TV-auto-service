package com.skylake.siddharthsky.sparkletv2

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket


class MainActivity : FragmentActivity() {

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        if (savedInstanceState == null) {
            if (isAppSaved()) {
                CheckSiteStatusTask().execute()
            } else {
                openInstalledAppsFragment()
            }
        }
    }


    private inner class CheckSiteStatusTask : AsyncTask<Void, String, Boolean>() {

        private val HOST = "localhost"
        private val PORT = 8000
        private val TIMEOUT_MS = 100
        private val MAX_ATTEMPTS = 10
        private val RETRY_DELAY_MS = 3000L

        override fun doInBackground(vararg params: Void?): Boolean {
            if (isSiteReachable(HOST, PORT, TIMEOUT_MS)) {
                publishProgress("Server is already up ⬆️")
                return true
            }

            publishProgress("Server is down ⬇️. Starting server via Termux...")
            runOnUiThread {
                openApp("com.termux")
            }

            for (attempt in 1..MAX_ATTEMPTS) {
                publishProgress("Waiting for server... (Attempt $attempt/$MAX_ATTEMPTS)")
                try {
                    Thread.sleep(RETRY_DELAY_MS)
                } catch (e: InterruptedException) {
                    return false
                }

                if (isSiteReachable(HOST, PORT, TIMEOUT_MS)) {
                    publishProgress("Server started successfully! ✅")
                    return true
                }
            }

            publishProgress("Server failed to start after $MAX_ATTEMPTS attempts. ❌")
            return false
        }

        override fun onProgressUpdate(vararg values: String?) {
            values[0]?.let { showToast(it) }
        }

        override fun onPostExecute(result: Boolean) {
            if (result) {
                showToast("Starting App...")
                openSavedApp()
            } else {
                showToast("Could not connect to server. Please check Termux.")
            }
            finish()
        }
    }


    private fun isSiteReachable(host: String, port: Int, timeout: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeout)
                true
            }
        } catch (e: IOException) {
            false
        }
    }


    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }


    private fun isAppSaved(): Boolean {
        val savedAppName = sharedPreferences.getString("savedAPP", null)
        return !savedAppName.isNullOrEmpty()
    }

    private fun openSavedApp() {
        val savedApp = sharedPreferences.getString("savedAPP", null)
        savedApp?.let {
            openApp(it)
        }
    }

    private fun openInstalledAppsFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_browse_fragment, InstalledAppsFragment())
            .commitNow()
    }


    private fun openApp(pkgName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(pkgName)
        if (launchIntent != null) {
            startActivity(launchIntent)
        } else {
            showToast("App not found: $pkgName")
            val playStoreIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=$pkgName")
            )
            startActivity(playStoreIntent)
        }
    }

}
