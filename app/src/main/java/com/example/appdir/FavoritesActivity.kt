package com.example.appdir
import com.example.appdir.model.*
import com.example.appdir.data.FavoriteEntity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.lifecycle.lifecycleScope
import com.example.appdir.databinding.ActivityFavoritesBinding
import kotlinx.coroutines.launch

class FavoritesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFavoritesBinding
    private lateinit var adapter: RulesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoritesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        title = "Favorites"

        adapter = RulesAdapter { rule:Rule ->
            lifecycleScope.launch {
                MyApp.db.favoriteDao().deleteByCategoryAndText(rule.category, rule.text)
            }
        }


        binding.recyclerFav.layoutManager = LinearLayoutManager(this)
        binding.recyclerFav.adapter = adapter

        // Observe the favorites in database
        lifecycleScope.launch {
            MyApp.db.favoriteDao().getAll().collect { list: List<FavoriteEntity> ->
                val rules = list.map { favorite: FavoriteEntity ->
                    Rule(category = favorite.category, text = favorite.ruleText, isFavorite = true)
                }
                adapter.submitList(rules.map { rule -> ListItem.RuleItem(rule) })

            }
        }

    }
}
