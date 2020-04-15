package dev.pgordon


data class Storage(
    var stats: Map<String, DeckStats> = mutableMapOf()
)