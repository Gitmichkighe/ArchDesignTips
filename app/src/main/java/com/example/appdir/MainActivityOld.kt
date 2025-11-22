package com.example.appdir

import android.content.Intent
import android.content.res.Configuration
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appdir.model.Category
import com.example.appdir.model.Rule
import com.example.appdir.util.JsonLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.navigation.NavigationView
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.*
import androidx.core.net.toUri
import androidx.core.content.edit

class MainActivityOld : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var adapter: CategoryAdapter
    private lateinit var adView: AdView
    private lateinit var progressBar: ProgressBar
    private var allCategories: List<Category> = emptyList()
    private val mainViewModel: MainViewModel by viewModels()
    private val searchScope = CoroutineScope(Dispatchers.Main)

    // NEW: second progress bar for content downloading
    private lateinit var contentUpdateProgress: ProgressBar

    private val rewardAdLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val categoryName = result.data?.getStringExtra("CATEGORY_NAME") ?: return@registerForActivityResult
                val category = adapter.currentList.find { it.category == categoryName } ?: return@registerForActivityResult

                category.unlock()
                adapter.notifyItemChanged(adapter.currentList.indexOf(category))

                Toast.makeText(this, "${category.category} unlocked!", Toast.LENGTH_SHORT).show()

                onCategoryClicked(category)
            } else {
                Toast.makeText(this, "Category remains locked", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_old)

        progressBar = findViewById(R.id.progressBar)
        progressBar.visibility = View.VISIBLE

        // NEW: find new content update progress bar
        contentUpdateProgress = findViewById(R.id.contentUpdateProgress)

        FirebaseApp.initializeApp(this)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            toolbar.setRenderEffect(RenderEffect.createBlurEffect(25f, 25f, Shader.TileMode.CLAMP))
        } else {
            toolbar.background?.alpha = 230
        }
        toolbar.elevation = 8f

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        navView.setNavigationItemSelectedListener(this)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        val recycler = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recycler)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = CategoryAdapter { category -> onCategoryClicked(category) }
        recycler.adapter = adapter

        mainViewModel.categories.observe(this, Observer { categories ->
            if (categories.isNotEmpty()) {
                allCategories = categories
                adapter.submitList(allCategories)
                progressBar.visibility = View.GONE
            }
        })
        mainViewModel.loadCategories(this)

        setupSearchView()

        MobileAds.initialize(this) {}
        adView = findViewById(R.id.adView)
        adView.loadAd(AdRequest.Builder().build())

        // ------------------------------
        // UPDATED OBSERVER (keeps original behavior + adds content update)
        // ------------------------------
        mainViewModel.updateStatus.observe(this) { status ->
            when (status) {
                is MainViewModel.UpdateStatus.Checking -> {
                    Toast.makeText(this, "Checking for updates...", Toast.LENGTH_SHORT).show()
                }

                is MainViewModel.UpdateStatus.UpToDate -> {
                    Toast.makeText(this, "App is up to date", Toast.LENGTH_SHORT).show()
                }

                is MainViewModel.UpdateStatus.UpdateAvailable -> {
                    AlertDialog.Builder(this)
                        .setTitle("Update Available")
                        .setMessage("A new version ${status.latestVersion} is available. Do you want to update?")
                        .setPositiveButton("Update") { _, _ ->
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                "market://details?id=$packageName".toUri()
                            )
                            startActivity(intent)
                        }
                        .setNegativeButton("Later", null)
                        .show()
                }

                is MainViewModel.UpdateStatus.Error -> {
                    Toast.makeText(this, "Update check failed: ${status.message}", Toast.LENGTH_SHORT).show()
                }

                // NEW: show % progress during content download
                is MainViewModel.UpdateStatus.DownloadingContent -> {
                    contentUpdateProgress.visibility = View.VISIBLE
                    contentUpdateProgress.progress = status.progress
                }

                // NEW: hide bar + refresh content after download
                is MainViewModel.UpdateStatus.ContentUpdated -> {
                    contentUpdateProgress.visibility = View.GONE
                    Toast.makeText(this, "Content updated!", Toast.LENGTH_SHORT).show()
                    adapter.submitList(mainViewModel.categories.value)
                }
            }
        }
    }

    private fun setupSearchView() {
        val searchView = SearchView(this).apply {
            layoutParams = Toolbar.LayoutParams(
                Toolbar.LayoutParams.MATCH_PARENT,
                Toolbar.LayoutParams.WRAP_CONTENT
            )
            queryHint = "Search categories or rules"
        }
        toolbar.addView(searchView)

        val searchBanner: TextView = findViewById(R.id.searchBanner)
        var searchJob: Job? = null

        mainViewModel.searchResults.observe(this) { results ->
            progressBar.visibility = View.GONE
            val query = searchView.query.toString()
            if (query.isEmpty()) {
                adapter.submitList(allCategories)
                fadeOutBanner(searchBanner)
            } else {
                val totalMatches = results.sumOf { it.rules.size.coerceAtLeast(1) }
                val message = if (results.isEmpty()) {
                    "No results found for \"$query\""
                } else {
                    "Showing $totalMatches result${if (totalMatches != 1) "s" else ""} for \"$query\""
                }
                adapter.submitList(results)
                searchBanner.text = message
                fadeInBanner(searchBanner)
            }
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false

            override fun onQueryTextChange(newText: String?): Boolean {
                searchJob?.cancel()

                val query = newText?.trim().orEmpty()

                if (query.isNotEmpty()) {
                    progressBar.visibility = View.VISIBLE
                } else {
                    progressBar.visibility = View.GONE
                }

                searchJob = searchScope.launch {
                    delay(300)
                    mainViewModel.search(query, this@MainActivityOld)
                }
                return true
            }
        })
    }

    private fun fadeInBanner(banner: TextView) {
        if (!banner.isVisible) {
            banner.alpha = 0f
            banner.visibility = View.VISIBLE
            banner.animate().alpha(1f).setDuration(250).start()
        }
    }

    private fun fadeOutBanner(banner: TextView) {
        if (banner.isVisible) {
            banner.animate().alpha(0f).setDuration(250)
                .withEndAction { banner.visibility = View.GONE }
                .start()
        }
    }

    private fun onCategoryClicked(category: Category) {
        if (category.locked) {
            AlertDialog.Builder(this)
                .setTitle("Unlock Category")
                .setMessage("This category is locked. View two ads to unlock this category.")
                .setPositiveButton("View Ads") { _, _ ->
                    val intent = Intent(this, RewardedAdActivity::class.java)
                    intent.putExtra("CATEGORY_NAME", category.category)
                    rewardAdLauncher.launch(intent)
                }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }

        val rules: List<Rule> = if (category.rules.isNotEmpty()) {
            category.rules
        } else {
            JsonLoader.loadRulesForCategory(this, category.category)
        }

        if (rules.isEmpty()) {
            Toast.makeText(this, "No rules found for ${category.category}", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, RulesActivity::class.java).apply {
            putExtra(RulesActivity.EXTRA_CATEGORY, category.category)
            putStringArrayListExtra(
                RulesActivity.EXTRA_RULES,
                ArrayList(rules.map { it.text })
            )
        }
        startActivity(intent)
    }

    private fun showAboutDialog() {
        val version = packageManager.getPackageInfo(packageName, 0).versionName
        AlertDialog.Builder(this)
            .setTitle("About App")
            .setMessage(
                "App name: ArchDesignTips\nVersion: $version\nDeveloper: archdesigntips@gmail.com"
            )
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                drawerLayout.openDrawer(GravityCompat.START)
                true
            }
            R.id.menu_favorites -> {
                startActivity(Intent(this, FavoritesActivity::class.java))
                true
            }
            R.id.menu_bookmarks -> {
                startActivity(Intent(this, BookmarksActivity::class.java))
                true
            }
            R.id.menu_rate -> {
                openRateUs()
                true
            }
            R.id.menu_about -> {
                showAboutDialog()
                true
            }
            R.id.menu_contact -> {
                startActivity(Intent(this, ContactActivity::class.java))
                true
            }
            R.id.menu_dark_mode -> {
                toggleDarkMode()
                true
            }
            R.id.menu_check_update -> {
                mainViewModel.checkForContentUpdate(this)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun toggleDarkMode() {
        val prefs = getSharedPreferences("theme_prefs", MODE_PRIVATE)
        val currentlyDark = when (AppCompatDelegate.getDefaultNightMode()) {
            AppCompatDelegate.MODE_NIGHT_YES -> true
            AppCompatDelegate.MODE_NIGHT_NO -> false
            else -> {
                val mode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                mode == Configuration.UI_MODE_NIGHT_YES
            }
        }
        val newMode =
            if (currentlyDark) AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES
        prefs.edit { putBoolean("dark_mode", newMode == AppCompatDelegate.MODE_NIGHT_YES) }
        AppCompatDelegate.setDefaultNightMode(newMode)
        recreate()
    }

    private fun openRateUs() {
        val intent = Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri())
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        drawerLayout.closeDrawer(GravityCompat.START)
        when (item.itemId) {
            R.id.nav_home -> {}
            R.id.nav_favorites -> showFavorites()
            R.id.nav_bookmarks -> showBookmarks()
            R.id.nav_about -> showAboutDialog()
            R.id.nav_contact -> startActivity(Intent(this, ContactActivity::class.java))
            R.id.menu_check_update -> mainViewModel.checkForContentUpdate(this)
        }
        return true
    }

    private fun showBookmarks() = startActivity(Intent(this, BookmarksActivity::class.java))
    private fun showFavorites() = startActivity(Intent(this, FavoritesActivity::class.java))
}
