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
    private var checkSiteTask: CheckSiteStatusTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        if (savedInstanceState == null) {
            if (isAppSaved()) {
                checkSiteTask = CheckSiteStatusTask()
                checkSiteTask?.execute()
            } else {
                openInstalledAppsFragment()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        checkSiteTask?.cancel(true)
    }


    private inner class CheckSiteStatusTask : AsyncTask<Void, String, Boolean>() {

        private val MAX_ATTEMPTS = 3

        override fun doInBackground(vararg params: Void?): Boolean {
            return isSiteReachable(localhost, 5001, 100)
        }

        override fun onProgressUpdate(vararg values: String?) {
            values[0]?.let { showToast(it) }
        }

        override fun onPostExecute(result: Boolean) {
            if (result) {
                openSavedApp()
                finish()
            } else {
                showToast("Server is offline ⬇️")
                Thread.sleep(2000)
                showToast("Attempting Restart...")
                Thread.sleep(2000)
                openApp("com.termux")

                AsyncTask.execute {
                    try {
                        Thread.sleep(2000)

                        runOnUiThread {
                            openSavedApp()
                        }

                        var serverCameUp = false
                        for (attempt in 1..MAX_ATTEMPTS) {
                            if (isCancelled) break

                            publishProgress("Waiting for server to come online ⬆️...")
                            Thread.sleep(6000)

                            if (isSiteReachable(localhost, 5001, 100)) {
                                publishProgress("Server started successfully! ✅")
                                serverCameUp = true
                                break
                            }
                        }

                        if (!serverCameUp) {
                            publishProgress("Server failed to start ❌. Try manually...")
                        }
                        Thread.sleep(3000)
                        runOnUiThread {
                            finish()
                        }

                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                        publishProgress("Server check interrupted.")
                        runOnUiThread {
                            finish()
                        }
                    }
                }
            }
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
        println(pkgName)
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
