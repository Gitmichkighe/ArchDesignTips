package com.example.appdir.util

import android.content.Context
import android.util.Log
import com.example.appdir.model.Category
import com.example.appdir.model.Rule
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

object JsonLoader {

    private const val TAG = "JsonLoader"

    /** Cache in-memory so JSON is loaded only once */
    private var cachedRawCategories: List<CategoryJson>? = null

    /**
     * Clear JSON cache so newly downloaded content can load.
     */
    fun clearCache() {
        cachedRawCategories = null
    }

    /**
     * Load JSON from internal storage *if present*,
     * otherwise fall back to assets.
     */
    private fun loadJsonString(context: Context): String {
        return try {
            // File where updates are saved
            val localFile = File(context.filesDir, "ArchiTips_v1.json")

            if (localFile.exists()) {
                Log.d(TAG, "Loading updated JSON from internal storage")
                localFile.readText()
            } else {
                Log.d(TAG, "Loading bundled JSON from assets")
                context.assets.open("ArchiTips_v1.json")
                    .bufferedReader()
                    .use { it.readText() }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error loading JSON", e)
            ""
        }
    }

    /**
     * Loads and caches the raw JSON only once.
     */
    private fun getRawCategories(context: Context): List<CategoryJson> {
        cachedRawCategories?.let { return it }

        val jsonString = loadJsonString(context)
        if (jsonString.isBlank()) return emptyList()

        return try {
            val type = object : TypeToken<List<CategoryJson>>() {}.type
            val parsed = Gson().fromJson<List<CategoryJson>>(jsonString, type)

            cachedRawCategories = parsed
            parsed

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON", e)
            emptyList()
        }
    }

    /**
     * Returns list of categories (NO rules loaded yet — same behavior as before)
     * Keeps your "every second category locked at first launch" logic unchanged.
     */
    fun loadCategories(context: Context): List<Category> {

        val sharedPrefs = context.getSharedPreferences("category_prefs", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()

        val isFirstLaunch = sharedPrefs.getBoolean("first_launch_done", false).not()

        val rawCategories = getRawCategories(context)

        val categories = rawCategories.mapIndexed { index, rawCat ->

            val key = rawCat.category
            val unlockedKey = "${key}_unlocked"

            val locked = if (isFirstLaunch) {
                // Lock every second category only on first launch
                val lockState = index % 2 == 1
                editor.putBoolean(unlockedKey, !lockState)  // save unlock state
                lockState

            } else {
                // Handle new categories correctly
                if (!sharedPrefs.contains(unlockedKey)) {
                    editor.putBoolean(unlockedKey, false)  // new category defaults to locked
                    true
                } else {
                    !sharedPrefs.getBoolean(unlockedKey, false)
                }
            }

            Category(
                category = rawCat.category,
                locked = locked,
                rules = emptyList()
            )
        }

        if (isFirstLaunch) {
            editor.putBoolean("first_launch_done", true)
        }
        editor.apply()

        return categories
    }

    /**
     * Returns rules for a category
     */
    fun loadRulesForCategory(context: Context, categoryName: String): List<Rule> {

        val raw = getRawCategories(context)
        val found = raw.find { it.category.equals(categoryName, ignoreCase = true) }
            ?: return emptyList()

        return found.rules.map { ruleText ->
            Rule(
                category = found.category,
                text = ruleText,
                isFavorite = false,
                textLower = null
            )
        }
    }

    /**
     * Raw JSON model from file.
     */
    data class CategoryJson(
        val category: String,
        val rules: List<String>
    )
}
