package com.example.appdir

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class RewardedAdActivity : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME = "category_prefs"
        private const val TOTAL_ADS_REQUIRED = 2

        fun getAdsWatched(context: Context, categoryName: String): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getInt("${categoryName}_adsWatched", 0)
        }

        fun setAdsWatched(context: Context, categoryName: String, count: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putInt("${categoryName}_adsWatched", count).apply()
        }

        fun isUnlocked(context: Context, categoryName: String): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean("${categoryName}_unlocked", false)
        }

        fun setUnlocked(context: Context, categoryName: String, unlocked: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean("${categoryName}_unlocked", unlocked).apply()
        }
    }

    private lateinit var tvInstructions: TextView
    private lateinit var btnWatchAd: Button
    private lateinit var progressBar: ProgressBar


    private var rewardedAd: RewardedAd? = null
    private var adsWatched = 0
    private var categoryName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rewarded_ad)

        tvInstructions = findViewById(R.id.tvInstructions)
        btnWatchAd = findViewById(R.id.btnWatchAd)
        progressBar = findViewById(R.id.progressBar)

        categoryName = intent.getStringExtra("CATEGORY_NAME") ?: ""

        if (categoryName.isEmpty()) {
            Toast.makeText(this, "Category not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Load previously watched ads
        adsWatched = getAdsWatched(this, categoryName)

        // If already unlocked, finish immediately
        if (isUnlocked(this, categoryName)) {
            setResult(RESULT_OK)
            finish()
            return
        }

        updateInstructions()

        loadRewardedAd()

        btnWatchAd.setOnClickListener {
            showRewardedAd()
        }
    }

    private fun updateInstructions() {
        tvInstructions.text = "Watch $TOTAL_ADS_REQUIRED ads to unlock this category.\nAds watched: $adsWatched/$TOTAL_ADS_REQUIRED"
    }

    private fun loadRewardedAd() {
        progressBar.visibility = View.VISIBLE

        val testAdUnitId = "ca-app-pub-3940256099942544/5224354917" // AdMob test ID
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            this,
            testAdUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@RewardedAdActivity,
                        "Ad failed to load: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@RewardedAdActivity, "Ad ready!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun showRewardedAd() {
        if (rewardedAd == null) {
            Toast.makeText(this, "Ad not loaded yet. Try again.", Toast.LENGTH_SHORT).show()
            loadRewardedAd()
            return
        }

        rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {

            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                loadRewardedAd()

            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedAd = null
                loadRewardedAd()
            }
        }

        rewardedAd?.show(this) { rewardItem ->
            adsWatched++
            setAdsWatched(this, categoryName, adsWatched)

            if (adsWatched >= TOTAL_ADS_REQUIRED) {
                setUnlocked(this, categoryName, true)
                Toast.makeText(this, "Category unlocked!", Toast.LENGTH_SHORT).show()

                val resultIntent = Intent().apply {
                    putExtra("CATEGORY_NAME", categoryName)
                }

                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            } else {
                updateInstructions()
            }
        }
    }


    override fun onBackPressed() {
        setResult(Activity.RESULT_CANCELED)
        super.onBackPressed()
    }
}
