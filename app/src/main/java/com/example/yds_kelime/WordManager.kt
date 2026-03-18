package com.example.yds_kelime

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.InputStreamReader

class WordManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("YDS_PREFS", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    private var allWords = mutableListOf<Word>()
    private var correctWords = mutableSetOf<String>()
    private var wrongWords = mutableListOf<Word>()
    private var remainingWords = mutableListOf<Word>()
    private var customWords = mutableSetOf<Word>()
    
    init {
        loadWords()
        loadProgress()
    }
    
    private fun loadWords() {
        val inputStream = context.resources.openRawResource(R.raw.yds_kelimeleri)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val tempWords = mutableListOf<Word>()
        val seenEnglish = mutableSetOf<String>()
        
        reader.useLines { lines ->
            lines.forEach { line ->
                if (line.isNotBlank() && line.contains("–")) {
                    val parts = line.split("–", limit = 2)
                    if (parts.size == 2) {
                        val english = parts[0].trim()
                        val turkish = parts[1].trim()
                        
                        if (english.isNotEmpty() && turkish.isNotEmpty() && !seenEnglish.contains(english.lowercase())) {
                            tempWords.add(Word(english, turkish))
                            seenEnglish.add(english.lowercase())
                        }
                    }
                }
            }
        }
        
        allWords = tempWords
        customWords = loadCustomWords()
        
        allWords.addAll(customWords.filter { customWord ->
            !seenEnglish.contains(customWord.english.lowercase())
        })
        
        if (remainingWords.isEmpty()) {
            remainingWords = allWords.toMutableList().apply { shuffle() }
        }
    }
    
    private fun loadCustomWords(): MutableSet<Word> {
        val json = prefs.getString("custom_words", "[]") ?: "[]"
        val type = object : TypeToken<MutableSet<Word>>() {}.type
        return gson.fromJson(json, type) ?: mutableSetOf()
    }
    
    private fun saveCustomWords() {
        val json = gson.toJson(customWords)
        prefs.edit().putString("custom_words", json).apply()
    }
    
    private fun loadProgress() {
        val correctJson = prefs.getString("correct_words", "[]") ?: "[]"
        val wrongJson = prefs.getString("wrong_words", "[]") ?: "[]"
        val remainingJson = prefs.getString("remaining_words", "[]") ?: "[]"
        
        val setType = object : TypeToken<MutableSet<String>>() {}.type
        val listType = object : TypeToken<MutableList<Word>>() {}.type
        
        correctWords = gson.fromJson(correctJson, setType) ?: mutableSetOf()
        wrongWords = gson.fromJson(wrongJson, listType) ?: mutableListOf()
        
        val savedRemaining: MutableList<Word>? = gson.fromJson(remainingJson, listType)
        if (!savedRemaining.isNullOrEmpty()) {
            remainingWords = savedRemaining.apply { shuffle() }
        }
    }
    
    private fun saveProgress() {
        prefs.edit().apply {
            putString("correct_words", gson.toJson(correctWords))
            putString("wrong_words", gson.toJson(wrongWords))
            putString("remaining_words", gson.toJson(remainingWords))
            apply()
        }
    }
    
    fun getCurrentWord(): Word? {
        if (remainingWords.isEmpty() && wrongWords.isNotEmpty()) {
            remainingWords = wrongWords.toMutableList().apply { shuffle() }
            wrongWords.clear()
            saveProgress()
        }
        
        if (remainingWords.isEmpty() && wrongWords.isEmpty()) {
            resetGame()
        }
        
        return remainingWords.firstOrNull()
    }
    
    fun markAsCorrect() {
        val currentWord = remainingWords.firstOrNull() ?: return
        correctWords.add(currentWord.english.lowercase())
        remainingWords.removeAt(0)
        saveProgress()
    }
    
    fun markAsWrong() {
        val currentWord = remainingWords.firstOrNull() ?: return
        wrongWords.add(currentWord)
        remainingWords.removeAt(0)
        saveProgress()
    }
    
    fun addCustomWord(english: String, turkish: String): Boolean {
        val normalizedEnglish = english.trim().lowercase()
        
        if (allWords.any { it.english.lowercase() == normalizedEnglish } || 
            customWords.any { it.english.lowercase() == normalizedEnglish }) {
            return false
        }
        
        val newWord = Word(english.trim(), turkish.trim())
        customWords.add(newWord)
        allWords.add(newWord)
        
        val randomIndex = (0..remainingWords.size).random()
        remainingWords.add(randomIndex, newWord)
        
        saveCustomWords()
        saveProgress()
        return true
    }
    
    private fun resetGame() {
        correctWords.clear()
        wrongWords.clear()
        remainingWords = allWords.toMutableList().apply { shuffle() }
        saveProgress()
    }
    
    fun getStats(): Triple<Int, Int, Int> {
        return Triple(
            correctWords.size,
            wrongWords.size,
            remainingWords.size
        )
    }
    
    fun getAllWordsList(): List<Word> {
        return allWords.sortedBy { it.english }
    }
    
    fun updateWord(oldWord: Word, newEnglish: String, newTurkish: String): Boolean {
        val normalizedNew = newEnglish.trim().lowercase()
        val normalizedOld = oldWord.english.lowercase()
        
        if (normalizedNew != normalizedOld && 
            allWords.any { it.english.lowercase() == normalizedNew }) {
            return false
        }
        
        val index = allWords.indexOfFirst { it.english.lowercase() == normalizedOld }
        if (index != -1) {
            val updatedWord = Word(newEnglish.trim(), newTurkish.trim())
            allWords[index] = updatedWord
            
            val remainingIndex = remainingWords.indexOfFirst { it.english.lowercase() == normalizedOld }
            if (remainingIndex != -1) {
                remainingWords[remainingIndex] = updatedWord
            }
            
            val wrongIndex = wrongWords.indexOfFirst { it.english.lowercase() == normalizedOld }
            if (wrongIndex != -1) {
                wrongWords[wrongIndex] = updatedWord
            }
            
            if (customWords.any { it.english.lowercase() == normalizedOld }) {
                customWords.removeIf { it.english.lowercase() == normalizedOld }
                customWords.add(updatedWord)
                saveCustomWords()
            }
            
            saveProgress()
            return true
        }
        return false
    }
    
    fun deleteWord(word: Word): Boolean {
        val normalizedEnglish = word.english.lowercase()
        
        val removed = allWords.removeIf { it.english.lowercase() == normalizedEnglish }
        remainingWords.removeIf { it.english.lowercase() == normalizedEnglish }
        wrongWords.removeIf { it.english.lowercase() == normalizedEnglish }
        correctWords.remove(normalizedEnglish)
        
        if (customWords.any { it.english.lowercase() == normalizedEnglish }) {
            customWords.removeIf { it.english.lowercase() == normalizedEnglish }
            saveCustomWords()
        }
        
        saveProgress()
        return removed
    }
}
