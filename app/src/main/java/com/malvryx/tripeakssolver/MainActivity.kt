package com.malvryx.tripeakssolver

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: MaterialTextView
    private lateinit var resultText: MaterialTextView
    private lateinit var statsText: MaterialTextView
    private lateinit var solveButton: MaterialButton
    private lateinit var accessibilityButton: MaterialButton
    
    private val solver = TriPeaksSolver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initViews()
        setupListeners()
        
        showWelcomeMessage()
    }
    
    private fun initViews() {
        statusText = findViewById(R.id.statusText)
        resultText = findViewById(R.id.resultText)
        statsText = findViewById(R.id.statsText)
        solveButton = findViewById(R.id.solveButton)
        accessibilityButton = findViewById(R.id.accessibilityButton)
    }
    
    private fun setupListeners() {
        solveButton.setOnClickListener {
            runTestSolver()
        }
        
        accessibilityButton.setOnClickListener {
            openAccessibilitySettings()
        }
    }
    
    private fun showWelcomeMessage() {
        statusText.text = buildString {
            appendLine("🎮 Disney TriPeaks Solver")
            appendLine("Created by Dev Malvryx")
            appendLine()
            appendLine("Ready to solve TriPeaks puzzles!")
            appendLine()
            appendLine("Tap 'Test Solver' to see it in action")
        }
        
        resultText.text = "Results will appear here..."
        statsText.text = ""
    }
    
    private fun runTestSolver() {
        solveButton.isEnabled = false
        statusText.text = "🔄 Solving test board..."
        resultText.text = "Please wait..."
        statsText.text = ""
        
        lifecycleScope.launch {
            try {
                // Create test board
                val testBoard = BoardGenerator.createTestBoard()
                
                statusText.text = buildString {
                    appendLine("🔄 Solving test board...")
                    appendLine()
                    appendLine("Board state:")
                    appendLine("  Pyramids: 3")
                    appendLine("  Total cards: ${testBoard.cardsRemaining}")
                    appendLine("  Stock size: ${testBoard.stockSize}")
                    appendLine("  Waste top: ${testBoard.wasteTop}")
                }
                
                // Solve
                val solution = solver.solve(testBoard, maxIterations = 5000, timeoutMs = 3000)
                
                // Display results
                displaySolution(solution)
                
            } catch (e: Exception) {
                statusText.text = "❌ Error: ${e.message}"
                e.printStackTrace()
            } finally {
                solveButton.isEnabled = true
            }
        }
    }
    
    private fun displaySolution(solution: Solution) {
        statusText.text = if (solution.success) {
            "✅ SOLVED! Found winning solution!"
        } else {
            "⚠️ Partial solution (best effort)"
        }
        
        resultText.text = buildString {
            appendLine("=== SOLUTION ===")
            appendLine()
            appendLine("Success: ${if (solution.success) "YES ✅" else "NO"}")
            appendLine("Cards Cleared: ${solution.cardsCleared}/28")
            appendLine("Total Moves: ${solution.movesCount}")
            appendLine()
            
            if (solution.moves.isNotEmpty()) {
                appendLine("Move sequence:")
                solution.moves.take(20).forEachIndexed { idx, move ->
                    appendLine("  ${idx + 1}. $move")
                }
                
                if (solution.moves.size > 20) {
                    appendLine("  ... and ${solution.moves.size - 20} more moves")
                }
            } else {
                appendLine("No moves found")
            }
        }
        
        val stats = solver.getStats()
        statsText.text = buildString {
            appendLine("=== STATS ===")
            appendLine("States explored: ${stats["statesVisited"]}")
            appendLine("Max depth: ${stats["maxDepth"]}")
            appendLine("Algorithm: DFS with pruning")
        }
        
        Toast.makeText(this, "Solver completed!", Toast.LENGTH_SHORT).show()
    }
    
    private fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(
                this,
                "Enable 'TriPeaks Solver' in accessibility settings",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open settings", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onResume() {
        super.onResume()
        checkAccessibilityEnabled()
    }
    
    private fun checkAccessibilityEnabled() {
        val enabled = try {
            Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )?.contains(packageName) == true
        } catch (e: Exception) {
            false
        }
        
        accessibilityButton.text = if (enabled) {
            "✅ Accessibility Enabled"
        } else {
            "Enable Accessibility Service"
        }
    }
}
