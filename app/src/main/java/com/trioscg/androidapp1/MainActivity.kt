package com.trioscg.androidapp1

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit

class MainActivity : AppCompatActivity() {

    private lateinit var buttons: Map<String, ImageButton>
    private lateinit var startButton: Button
    private lateinit var scoreTextView: TextView
    private lateinit var topScoresText: TextView
    private lateinit var resetScoresButton: Button
    private lateinit var roundTextView: TextView

    private val colorNames = listOf("red", "green", "blue", "yellow")
    private val sequence = mutableListOf<String>()
    private val topScores = mutableListOf<Pair<Int, Int>>()
    private val prefsFile = "HighScores"
    private val scoresKey = "TopScores"

    private var playerIndex = 0
    private var playerTurn = false
    private var currentScore = 0
    private var soundEffect: MediaPlayer? = null
    private var currentRound = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        buttons = mapOf(
            "red" to findViewById(R.id.redButton),
            "green" to findViewById(R.id.greenButton),
            "blue" to findViewById(R.id.blueButton),
            "yellow" to findViewById(R.id.yellowButton)
        )

        startButton = findViewById(R.id.startButton)
        scoreTextView = findViewById(R.id.scoreTextView)
        roundTextView = findViewById(R.id.roundTextView)
        topScoresText = findViewById(R.id.topScoresText)
        resetScoresButton = findViewById(R.id.resetHighScoresButton)

        buttons.forEach { (color, button) ->
            button.setOnClickListener { handleColorPress(color) }
        }

        loadScores()
        showTopScores()

        startButton.setOnClickListener { initializeGame() }

        resetScoresButton.setOnClickListener {
            clearScores()
            topScoresText.text = getString(R.string.top_5_scores)
            Toast.makeText(this, R.string.high_scores_cleared, Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleColorPress(color: String) {
        if (!playerTurn) return

        playSoundEffect(R.raw.button_sound)
        animateButton(color)

        if (color == sequence[playerIndex]) {
            currentScore++
            updateScoreDisplay()
            playerIndex++

            if (playerIndex == sequence.size) {
                playerTurn = false
                Handler(Looper.getMainLooper()).postDelayed({ proceedToNextRound() }, 1000)
            }
        } else {
            playSoundEffect(R.raw.wrong)
            Toast.makeText(this, "Wrong! Game Over!", Toast.LENGTH_SHORT).show()
            recordScore()
            showTopScores()
            playerTurn = false
        }
    }

    private fun initializeGame() {
        currentScore = 0
        sequence.clear()
        playerIndex = 0
        currentRound = 1
        updateScoreDisplay()
        updateRoundDisplay()
        proceedToNextRound()
    }

    private fun proceedToNextRound() {
        sequence.add(colorNames.random())
        playerIndex = 0
        showSequence()
        currentRound++
        updateRoundDisplay()
    }

    private fun showSequence() {
        playerTurn = false
        var delay = 0L
        sequence.forEach { color ->
            Handler(Looper.getMainLooper()).postDelayed({ animateButton(color) }, delay)
            delay += 700
        }
        Handler(Looper.getMainLooper()).postDelayed({ playerTurn = true }, delay)
    }

    private fun animateButton(color: String) {
        buttons[color]?.let { button ->
            button.alpha = 0.3f
            Handler(Looper.getMainLooper()).postDelayed({ button.alpha = 1.0f }, 300)
        }
    }

    private fun updateScoreDisplay() {
        scoreTextView.text = "Score: $currentScore"
    }

    private fun updateRoundDisplay() {
        roundTextView.text = "Round: $currentRound"
    }

    private fun playSoundEffect(resId: Int) {
        soundEffect?.release()
        soundEffect = MediaPlayer.create(this, resId)
        soundEffect?.apply {
            start()
            setOnCompletionListener {
                it.release()
                soundEffect = null
            }
        }
    }

    private fun recordScore() {
        topScores.add(currentRound - 1 to currentScore)
        topScores.sortByDescending { it.second }
        if (topScores.size > 5) topScores.removeLast()
        saveScores()
    }

    private fun showTopScores() {
        val builder = StringBuilder(getString(R.string.top_5_scores)).append("\n")
        topScores.forEachIndexed { index, pair ->
            builder.append("${index + 1}: Round ${pair.first} - Score: ${pair.second}\n")
        }
        topScoresText.text = builder.toString()
    }

    private fun saveScores() {
        val prefs = getSharedPreferences(prefsFile, MODE_PRIVATE)
        val scoreStr = topScores.joinToString(",") { "${it.first}:${it.second}" }
        prefs.edit { putString(scoresKey, scoreStr) }
    }

    private fun loadScores() {
        val prefs = getSharedPreferences(prefsFile, MODE_PRIVATE)
        prefs.getString(scoresKey, "")?.takeIf { it.isNotBlank() }?.let { saved ->
            topScores.clear()
            saved.split(",").mapNotNull {
                val parts = it.split(":")
                if (parts.size == 2) parts[0].toIntOrNull()?.let { r ->
                    parts[1].toIntOrNull()?.let { s -> r to s }
                } else null
            }.also { topScores.addAll(it) }
        }
    }

    private fun clearScores() {
        getSharedPreferences(prefsFile, MODE_PRIVATE).edit {
            remove(scoresKey)
        }
        topScores.clear()
    }
} // MainActivity
