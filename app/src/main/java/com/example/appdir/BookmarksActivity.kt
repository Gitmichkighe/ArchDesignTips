package com.example.appdir

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appdir.databinding.ActivityBookmarksBinding
import com.example.appdir.util.JsonLoader
import org.json.JSONArray
import org.json.JSONObject
import android.graphics.Color

class BookmarksActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookmarksBinding
    private lateinit var adapter: BookmarksAdapter
    private val bookmarks = mutableListOf<JSONObject>()
    private var selectedBookmark: JSONObject? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookmarksBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Bookmarks"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

// Force title text to white
        binding.toolbar.setTitleTextColor(Color.WHITE)

// Force back arrow to white
        val icon = binding.toolbar.navigationIcon
        icon?.setTint(Color.WHITE)
        binding.toolbar.navigationIcon = icon


        // RecyclerView setup
        adapter = BookmarksAdapter(
            onClick = { bookmark -> openBookmark(bookmark) },
            onLongClick = { bookmark ->
                selectedBookmark = bookmark
                adapter.setSelectedItem(bookmark)
                invalidateOptionsMenu() // Update menu when item is selected
            }
        )

        binding.recyclerBookmarks.layoutManager = LinearLayoutManager(this)
        binding.recyclerBookmarks.adapter = adapter

        loadBookmarks()
    }

    private fun openBookmark(bookmark: JSONObject) {
        val categoryName = bookmark.getString("category")
        val pageNumber = bookmark.getInt("page")

        val categories = JsonLoader.loadCategories(this)
        val category = categories.find { it.category == categoryName }

        if (category != null) {
            // Now, load the rules for the selected category
            val rulesTextList = ArrayList(JsonLoader.loadRulesForCategory(this, categoryName).map { it.text })

            Log.i("Rules Loaded", "Loaded rules for category: $categoryName, Rules count: ${rulesTextList.size}")

            if (rulesTextList.isNotEmpty()) {
                val intent = Intent(this, RulesActivity::class.java).apply {
                    putExtra(RulesActivity.EXTRA_CATEGORY, categoryName)
                    putStringArrayListExtra(RulesActivity.EXTRA_RULES, rulesTextList)
                    putExtra("pageNumber", pageNumber)
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "No rules found for category", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Category not found", Toast.LENGTH_SHORT).show()
        }
    }





    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_bookmarks, menu)

        // Only show "Clear All" if there are bookmarks
        menu?.findItem(R.id.action_clear_bookmarks)?.isVisible = bookmarks.isNotEmpty()

        // Only show "Delete" if a bookmark is selected
        menu?.findItem(R.id.action_delete)?.isVisible = selectedBookmark != null

        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_clear_bookmarks -> {
                showClearConfirmation()
                true
            }
            R.id.action_delete -> {
                selectedBookmark?.let {
                    deleteBookmark(it)
                    selectedBookmark = null
                    adapter.setSelectedItem(null)
                    invalidateOptionsMenu()
                }
                true
            }
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showClearConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Clear All Bookmarks")
            .setMessage("Are you sure you want to remove all bookmarks?")
            .setPositiveButton("Yes") { _, _ -> clearBookmarks() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearBookmarks() {
        val prefs = getSharedPreferences("bookmarks", MODE_PRIVATE)
        prefs.edit().putString("list", "[]").apply()

        bookmarks.clear()
        adapter.submitList(bookmarks.toList())
        selectedBookmark = null
        adapter.setSelectedItem(null)
        invalidateOptionsMenu()

        Toast.makeText(this, "All bookmarks cleared", Toast.LENGTH_SHORT).show()
    }

    private fun deleteBookmark(bookmark: JSONObject) {
        val prefs = getSharedPreferences("bookmarks", MODE_PRIVATE)
        val jsonArray = JSONArray(prefs.getString("list", "[]"))
        val newArray = JSONArray()

        for (i in 0 until jsonArray.length()) {
            if (jsonArray.getJSONObject(i).toString() != bookmark.toString()) {
                newArray.put(jsonArray.getJSONObject(i))
            }
        }

        prefs.edit().putString("list", newArray.toString()).apply()
        loadBookmarks()
    }

    private fun loadBookmarks() {
        val prefs = getSharedPreferences("bookmarks", MODE_PRIVATE)
        val json = prefs.getString("list", "[]")
        val array = JSONArray(json)

        bookmarks.clear()
        for (i in 0 until array.length()) {
            bookmarks.add(array.getJSONObject(i))
        }

        adapter.submitList(bookmarks.toList())
        invalidateOptionsMenu()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
