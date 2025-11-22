package com.example.appdir

import android.content.Intent
import android.content.res.Configuration
import android.graphics.RenderEffect
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appdir.data.FavoriteEntity
import com.example.appdir.databinding.ActivityRulesBinding
import com.example.appdir.model.ListItem
import com.example.appdir.model.Rule
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class RulesActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_RULES = "extra_rules"
        const val EXTRA_PAGE_NUMBER = "pageNumber"
    }

    private var adView: AdView? = null
    private lateinit var binding: ActivityRulesBinding
    private lateinit var adapter: RulesAdapter
    private lateinit var categoryName: String
    private val ruleList = mutableListOf<Rule>()

    private val pageSize = 4
    private var currentPage = 0
    private var totalPages = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply saved theme
        val themePrefs = getSharedPreferences("theme_prefs", MODE_PRIVATE)
        val isDark = themePrefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )

        binding = ActivityRulesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle back pressed correctly
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goBackToMain()
            }
        })

        setupToolbar()
        setupRecycler()
        setupPagination()

        // Restore page number if passed
        currentPage = intent.getIntExtra(EXTRA_PAGE_NUMBER, 0)

        setupAds()
        applyGlassEffects()
        loadRules()

        // Mark that user is currently in RulesActivity
        getSharedPreferences("last_visited", MODE_PRIVATE)
            .edit()
            .putBoolean("was_in_rules", true)
            .apply()
    }

    /** Toolbar setup */
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        categoryName = intent.getStringExtra(EXTRA_CATEGORY) ?: ""
        supportActionBar?.title = categoryName
    }

    /** Recycler setup */
    private fun setupRecycler() {
        adapter = RulesAdapter { rule -> toggleFavorite(rule) }
        binding.recyclerRules.layoutManager = LinearLayoutManager(this)
        binding.recyclerRules.adapter = adapter
    }

    /** Pagination buttons */
    private fun setupPagination() {
        binding.nextButton.setOnClickListener {
            if (currentPage < totalPages - 1) {
                currentPage++
                showCurrentPage()
            }
        }
        binding.backButton.setOnClickListener {
            if (currentPage > 0) {
                currentPage--
                showCurrentPage()
            }
        }
    }

    /** Load rules and favorites */
    private fun loadRules() {
        val rawRules = intent.getStringArrayListExtra(EXTRA_RULES) ?: arrayListOf()
        ruleList.addAll(rawRules.map { Rule(category = categoryName, text = it) })

        lifecycleScope.launch {
            val favDao = MyApp.db.favoriteDao()
            val favorites = favDao.getAll().first()
            val favoriteSet = favorites.filter { it.category == categoryName }
                .map { it.ruleText }.toSet()
            ruleList.forEach { it.isFavorite = favoriteSet.contains(it.text) }

            showCurrentPage()
        }
    }

    /** Display current page */
    private fun showCurrentPage() {
        val grouped = ruleList.groupBy { it.category }
        val flatList = grouped.flatMap { (category, rules) ->
            listOf(ListItem.Header(category)) + rules.map { ListItem.RuleItem(it) }
        }

        totalPages = (flatList.size + pageSize - 1) / pageSize
        val from = currentPage * pageSize
        val to = minOf(from + pageSize, flatList.size)
        adapter.submitList(flatList.subList(from, to))

        binding.pageIndicator.text = "Page ${currentPage + 1} of $totalPages"
        binding.nextButton.visibility = if (currentPage < totalPages - 1) View.VISIBLE else View.GONE
        binding.backButton.visibility = if (currentPage > 0) View.VISIBLE else View.GONE
    }

    /** Toggle favorites */
    private fun toggleFavorite(rule: Rule) {
        lifecycleScope.launch(Dispatchers.IO) {
            val favDao = MyApp.db.favoriteDao()
            if (rule.isFavorite) {
                favDao.insert(FavoriteEntity(0L, rule.category, rule.text))
            } else {
                favDao.deleteByCategoryAndText(rule.category, rule.text)
            }
            withContext(Dispatchers.Main) { showCurrentPage() }
        }
    }

    /** Glass effect blur */
    private fun applyGlassEffects() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val blur = RenderEffect.createBlurEffect(25f, 25f, Shader.TileMode.CLAMP)
            binding.toolbar.setRenderEffect(blur)
            binding.paginationLayout.setRenderEffect(blur)
            binding.toolbar.elevation = 8f
            binding.paginationLayout.elevation = 6f
        } else {
            binding.toolbar.background.alpha = 230
            binding.paginationLayout.background.alpha = 230
        }
    }

    /** Ads */
    private fun setupAds() {
        MobileAds.initialize(this) { loadAdaptiveBanner() }
    }

    private fun loadAdaptiveBanner() {
        try {
            val adContainer: FrameLayout = binding.adContainer
            adContainer.post {
                val adWidthPixels = adContainer.width
                val adWidthDp = (adWidthPixels / resources.displayMetrics.density).toInt()
                adView = AdView(this).apply {
                    adUnitId = getString(R.string.admob_banner_id)
                    val size = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                        this@RulesActivity, adWidthDp
                    )
                    setAdSize(size)
                }
                adContainer.removeAllViews()
                adContainer.addView(adView)
                adView?.loadAd(AdRequest.Builder().build())
            }
        } catch (e: Exception) {
            Log.e("AdViewError", "Error loading AdView", e)
        }
    }

    override fun onDestroy() {
        adView?.destroy()
        super.onDestroy()
    }

    /** Menu */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_rules, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_bookmark_all -> { showBookmarkDialog(); true }
            R.id.menu_rate -> { openRateUs(); true }
            R.id.menu_about -> { showAboutDialog(); true }
            R.id.menu_contact -> { openContactForm(); true }
            R.id.menu_dark_mode -> { toggleDarkMode(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /** Bookmarks */
    private fun showBookmarkDialog() {
        val editText = EditText(this).apply { hint = "Enter bookmark name" }
        AlertDialog.Builder(this)
            .setTitle("Save Bookmark")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val name = editText.text.toString().trim()
                saveBookmark(if (name.isNotEmpty()) name else "$categoryName: Pg. ${currentPage + 1}")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveBookmark(name: String) {
        val prefs = getSharedPreferences("bookmarks", MODE_PRIVATE)
        val json = prefs.getString("list", "[]")
        val bookmarks = JSONArray(json)
        val newBookmark = JSONObject().apply {
            put("name", name)
            put("category", categoryName)
            put("page", currentPage)
            put("rules", JSONArray(ruleList.map { it.text }))
        }
        bookmarks.put(newBookmark)
        prefs.edit().putString("list", bookmarks.toString()).apply()
        Toast.makeText(this, "Bookmark saved!", Toast.LENGTH_SHORT).show()
    }

    private fun openContactForm() = startActivity(Intent(this, ContactActivity::class.java))

    /** Dark mode */
    private fun toggleDarkMode() {
        val prefs = getSharedPreferences("theme_prefs", MODE_PRIVATE)
        val currentlyDark = when (AppCompatDelegate.getDefaultNightMode()) {
            AppCompatDelegate.MODE_NIGHT_YES -> true
            AppCompatDelegate.MODE_NIGHT_NO -> false
            else -> resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        }
        val newMode = if (currentlyDark) AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES
        prefs.edit().putBoolean("dark_mode", newMode == AppCompatDelegate.MODE_NIGHT_YES).apply()
        AppCompatDelegate.setDefaultNightMode(newMode)
        recreate()
    }

    private fun showAboutDialog() {
        val version = try { packageManager.getPackageInfo(packageName, 0).versionName } catch (e: Exception) { "Unknown" }
        AlertDialog.Builder(this)
            .setTitle("About App")
            .setMessage("App name: ArchDesignTips\nVersion: $version\nDeveloper: machinetomachinecom@gmail.com")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun openRateUs() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    /** Save last visited page on pause */
    override fun onPause() {
        super.onPause()
        saveLastVisited()
    }

    private fun saveLastVisited() {
        val prefs = getSharedPreferences("last_visited", MODE_PRIVATE)
        prefs.edit()
            .putString("category", categoryName)
            .putInt("page", currentPage)
            .putBoolean("was_in_rules", true)
            .apply()
    }

    /** Navigation */
    override fun onSupportNavigateUp(): Boolean {
        goBackToMain()
        return true
    }

    private fun goBackToMain() {
        val prefs = getSharedPreferences("last_visited", MODE_PRIVATE)
        prefs.edit().putBoolean("was_in_rules", false).apply()
        val intent = Intent(this, MainActivityOld::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }
}
