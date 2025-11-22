package com.example.appdir

import android.content.Context
import androidx.lifecycle.*
import com.example.appdir.model.Category
import com.example.appdir.model.Rule
import com.example.appdir.util.JsonLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class MainViewModel : ViewModel() {

    // -------------------------------------------------------------
    // CATEGORY + SEARCH SYSTEM (unchanged except for small cleanup)
    // -------------------------------------------------------------

    private val _categories = MutableLiveData<List<Category>>(emptyList())
    val categories: LiveData<List<Category>> get() = _categories

    private val _searchResults = MutableLiveData<List<Category>>(emptyList())
    val searchResults: LiveData<List<Category>> get() = _searchResults

    /** Cache for rules to avoid reloading JSON for every search */
    private val ruleCache = mutableMapOf<String, List<Rule>>()

    /** Load all categories once */
    fun loadCategories(context: Context) {
        _categories.value = JsonLoader.loadCategories(context)
    }

    /** Search categories + rules */
    fun search(query: String, context: Context) {
        val lowerQuery = query.lowercase()

        viewModelScope.launch(Dispatchers.Default) {
            val categoriesList = _categories.value ?: emptyList()

            val results = categoriesList.mapNotNull { category ->

                // Load rules for category from cache or from loader
                val rules = ruleCache.getOrPut(category.category) {
                    JsonLoader.loadRulesForCategory(context, category.category)
                }

                // Filter rule texts
                val matchedRules = rules.filter {
                    it.text.lowercase().contains(lowerQuery)
                }

                // Category matches OR rule matches
                if (category.category.lowercase().contains(lowerQuery) ||
                    matchedRules.isNotEmpty()
                ) {
                    category.copy(rules = matchedRules)
                } else null
            }

            withContext(Dispatchers.Main) {
                _searchResults.value = results
            }
        }
    }

    // -------------------------------------------------------------
    // UPDATE STATUS SYSTEM (OLD + NEW TOGETHER)
    // -------------------------------------------------------------

    sealed class UpdateStatus {
        object Checking : UpdateStatus()
        object UpToDate : UpdateStatus()
        data class UpdateAvailable(val latestVersion: String) : UpdateStatus()
        data class Error(val message: String) : UpdateStatus()

        // NEW for content updates
        data class DownloadingContent(val progress: Int) : UpdateStatus()
        object ContentUpdated : UpdateStatus()
    }

    private val _updateStatus = MutableLiveData<UpdateStatus>()
    val updateStatus: LiveData<UpdateStatus> get() = _updateStatus

    // -------------------------------------------------------------
    // 1) OLD: APP VERSION UPDATE CHECK (unchanged)
    // -------------------------------------------------------------

    fun checkForUpdate(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _updateStatus.postValue(UpdateStatus.Checking)

            try {
                val url = URL("https://raw.githubusercontent.com/Gitmichkighe/ArchiTips/main/ArchiTips_v1.json")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                if (conn.responseCode == 200) {
                    val latestVersion = conn.inputStream.bufferedReader().use {
                        it.readText()
                    }.trim()

                    val currentVersion = context.packageManager
                        .getPackageInfo(context.packageName, 0)
                        .versionName

                    if (latestVersion != currentVersion) {
                        _updateStatus.postValue(UpdateStatus.UpdateAvailable(latestVersion))
                    } else {
                        _updateStatus.postValue(UpdateStatus.UpToDate)
                    }

                } else {
                    _updateStatus.postValue(UpdateStatus.Error("Server error: ${conn.responseCode}"))
                }

                conn.disconnect()

            } catch (e: Exception) {
                _updateStatus.postValue(UpdateStatus.Error(e.message ?: "Unknown error"))
            }
        }
    }

    // -------------------------------------------------------------
    // 2) NEW: CONTENT UPDATE DOWNLOADER WITH PROGRESS
    // -------------------------------------------------------------

    fun checkForContentUpdate(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _updateStatus.postValue(UpdateStatus.Checking)

            try {
                val url = URL("https://raw.githubusercontent.com/Gitmichkighe/ArchiTips/main/ArchiTips_v1.json")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                if (conn.responseCode != 200) {
                    _updateStatus.postValue(
                        UpdateStatus.Error("Server error: ${conn.responseCode}")
                    )
                    return@launch
                }

                val totalSize = conn.contentLength
                val input = conn.inputStream
                val buffer = ByteArray(8192)

                val fileOutput = context.openFileOutput("ArchiTips_v1.json", Context.MODE_PRIVATE)
                var downloaded = 0
                var bytesRead: Int

                // Download + actual progress
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    fileOutput.write(buffer, 0, bytesRead)
                    downloaded += bytesRead

                    if (totalSize > 0) {
                        val progress = (downloaded * 100) / totalSize
                        _updateStatus.postValue(UpdateStatus.DownloadingContent(progress))
                    }
                }

                fileOutput.flush()
                fileOutput.close()
                input.close()

                // Reset caches so new content loads
                JsonLoader.clearCache()
                ruleCache.clear()

                // Reload categories and update UI
                _categories.postValue(JsonLoader.loadCategories(context))

                _updateStatus.postValue(UpdateStatus.ContentUpdated)

            } catch (e: Exception) {
                _updateStatus.postValue(UpdateStatus.Error(e.message ?: "Unknown error"))
            }
        }
    }
}
