package com.example.appdir

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.appdir.util.JsonLoader
import com.google.android.gms.ads.AdView

class DisclaimerActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private val mainViewModel: MainViewModel by lazy { MainViewModel() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load saved theme
        val prefs = getSharedPreferences("theme_prefs", MODE_PRIVATE)
        val isDark = prefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )

        // Check first launch
        val launchPrefs = getSharedPreferences("launch_prefs", MODE_PRIVATE)
        val firstLaunch = !launchPrefs.getBoolean("first_launch_done", false)

        if (!firstLaunch) {
            openLastVisitedOrMain()
            return
        }

        setContentView(R.layout.activity_disclaimer)
        progressBar = findViewById(R.id.progressBar)

        findViewById<Button>(R.id.agree_button).setOnClickListener {
            progressBar.visibility = ProgressBar.VISIBLE
            launchPrefs.edit().putBoolean("first_launch_done", true).apply()
            navigateToMainActivityOld()
        }
    }

    private fun loadCategoriesUpfront() {
        mainViewModel.loadCategories(this) // synchronous for now
    }

    private fun navigateToMainActivityOld() {
        loadCategoriesUpfront()
        startActivity(Intent(this, MainActivityOld::class.java))
        finish()
    }

    private fun openLastVisitedOrMain() {
        val prefs = getSharedPreferences("last_visited", MODE_PRIVATE)
        val cat = prefs.getString("category", null)
        val page = prefs.getInt("page", -1)

        if (cat != null && page >= 0) {
            val rulesList = ArrayList(
                JsonLoader.loadRulesForCategory(this, cat).map { it.text }
            )
            val intent = Intent(this, RulesActivity::class.java).apply {
                putExtra(RulesActivity.EXTRA_CATEGORY, cat)
                putStringArrayListExtra(RulesActivity.EXTRA_RULES, rulesList)
                putExtra("pageNumber", page)
            }
            startActivity(intent)
            finish()
            return
        }

        // fallback to main activity
        startActivity(Intent(this, MainActivityOld::class.java))
        finish()
    }
}
