package com.mattmakesapps.mostrx

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.setupActionBarWithNavController
import androidx.navigation.ui.navigateUp
import com.android.volley.NoConnectionError
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import com.android.volley.toolbox.Volley
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds

class MainActivity : AppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration

    private val requestQueue: RequestQueue by lazy {
        Volley.newRequestQueue(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navController = findNavController(R.id.my_nav_host_fragment)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(this, navController, appBarConfiguration)
        MobileAds.initialize(this) {}
        findViewById<AdView>(R.id.adView).loadAd(AdRequest.Builder().build())

        findViewById<LinearLayout>(R.id.tip_layout_aux).children.forEach {
            it.setOnClickListener {
                val openURL = Intent(Intent.ACTION_VIEW)
                openURL.data = Uri.parse("https://ko-fi.com/fungames")
                startActivity(openURL)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return findNavController(R.id.my_nav_host_fragment).navigateUp(appBarConfiguration) ||
                super.onSupportNavigateUp()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.clear()
    }

    fun <T> addToRequestQueue(req: Request<T>) {
        println("req.url=${req.url}")
        requestQueue.add(req)
    }

    fun hideKeyboard(windowToken: IBinder) {
        val imm = ContextCompat.getSystemService(this, InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(windowToken, 0)
    }

    fun setEnteringText(isEnteringText: Boolean) {
        findViewById<RelativeLayout>(R.id.ad_bar)?.visibility = if (isEnteringText) {
            View.GONE
        } else {
            View.VISIBLE
        }

        if (!isEnteringText) {
            hideKeyboard(window.decorView.rootView.windowToken)
        }
    }

    fun volleyFailed(error: VolleyError, retry: () -> Unit) {
        val message = if (error is NoConnectionError) {
            getString(R.string.conn_message)
        } else {
            error.localizedMessage
        }

        val alert = AlertDialog.Builder(this)
            .setCancelable(false)
            .setMessage(message)
            .setPositiveButton(getString(R.string.try_again)) { _, _ -> retry() }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.cancel() }
            .create()

        alert.setTitle(error.javaClass.simpleName)
        alert.show()
    }
}
