package com.example.appdir.model

data class Category(
    val category: String,
    var locked: Boolean = false,
    var adsWatched: Int = 0,
    var adsToUnlock: Int = 2,
    val rules: List<Rule>
) {
    fun unlock() {
        locked = false
        adsWatched = adsToUnlock
    }
}

data class Rule(
    val category: String,
    val text: String,
    var isFavorite: Boolean = false,
    var textLower: String? = null
)

sealed class ListItem {
    data class Header(val category: String) : ListItem()
    data class RuleItem(val rule: Rule) : ListItem()
}
