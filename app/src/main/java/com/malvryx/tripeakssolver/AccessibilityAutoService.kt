package com.malvryx.tripeakssolver

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.*

/**
 * Full Auto-Play Accessibility Service for Disney Solitaire
 */
class AccessibilityAutoService : AccessibilityService() {

    private val TAG = "TriPeaksAutoPlay"
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isAutoPlaying = false
    private val solver = TriPeaksSolver()
    
    companion object {
        const val DISNEY_SOLITAIRE_PACKAGE = "com.superplay.looney"
        
        // Card detection thresholds
        const val MIN_CARD_WIDTH = 100
        const val MAX_CARD_WIDTH = 300
        const val MIN_CARD_HEIGHT = 120
        const val MAX_CARD_HEIGHT = 400
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "✅ Auto-Play Service Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        
        // Only respond to Disney Solitaire
        if (event.packageName != DISNEY_SOLITAIRE_PACKAGE) return
        
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                Log.d(TAG, "Disney Solitaire window detected")
                if (!isAutoPlaying) {
                    startAutoPlay()
                }
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // Game state changed, continue playing
                if (isAutoPlaying) {
                    serviceScope.launch {
                        delay(500) // Wait for animations
                        playNextMove()
                    }
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
        stopAutoPlay()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "Service destroyed")
    }

    private fun startAutoPlay() {
        isAutoPlaying = true
        Log.d(TAG, "🤖 Auto-play STARTED")
        
        serviceScope.launch {
            delay(2000) // Give time for game to load
            playGameLoop()
        }
    }

    private fun stopAutoPlay() {
        isAutoPlaying = false
        Log.d(TAG, "⏹️ Auto-play STOPPED")
    }

    private suspend fun playGameLoop() {
        while (isAutoPlaying) {
            try {
                // Detect current game state
                val gameState = detectGameState()
                
                if (gameState == null) {
                    Log.d(TAG, "Could not detect game state, retrying...")
                    delay(2000)
                    continue
                }
                
                // Check if game is won
                if (gameState.isWin()) {
                    Log.d(TAG, "🎉 Game won! Waiting for next game...")
                    delay(5000)
                    continue
                }
                
                // Solve and execute next move
                val solution = solver.solve(gameState, maxIterations = 1000, timeoutMs = 2000)
                
                if (solution.moves.isNotEmpty()) {
                    val firstMove = solution.moves.first()
                    executeMove(firstMove)
                    delay(800) // Wait for animation
                } else {
                    // No moves available, try drawing from stock
                    Log.d(TAG, "No moves found, drawing from stock...")
                    tapStock()
                    delay(1000)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in game loop: ${e.message}")
                delay(2000)
            }
        }
    }

    private suspend fun playNextMove() {
        if (!isAutoPlaying) return
        
        try {
            val gameState = detectGameState() ?: return
            val solution = solver.solve(gameState, maxIterations = 500, timeoutMs = 1000)
            
            if (solution.moves.isNotEmpty()) {
                executeMove(solution.moves.first())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing next move: ${e.message}")
        }
    }

    /**
     * Detect game state from accessibility tree
     */
    private fun detectGameState(): BoardState? {
        val rootNode = rootInActiveWindow ?: return null
        
        try {
            // Find all card nodes
            val cardNodes = findCardNodes(rootNode)
            
            if (cardNodes.isEmpty()) {
                Log.d(TAG, "No cards found in accessibility tree")
                return createFallbackState()
            }
            
            Log.d(TAG, "Found ${cardNodes.size} card nodes")
            
            // Parse cards
            val tableCards = mutableListOf<Card?>()
            val wasteCards = mutableListOf<Card>()
            
            cardNodes.forEach { node ->
                val card = parseCardNode(node)
                if (card != null) {
                    // Determine if it's a table card or waste card based on position
                    val bounds = Rect()
                    node.getBoundsInScreen(bounds)
                    
                    if (bounds.centerY() < 600) { // Top half = table
                        tableCards.add(card)
                    } else if (bounds.centerX() > 800) { // Bottom right = waste
                        wasteCards.add(card)
                    }
                }
            }
            
            return BoardState(
                tableCards = if (tableCards.isEmpty()) List(28) { null } else tableCards,
                waste = wasteCards,
                stock = listOf(), // Assume stock available
                removed = emptySet()
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting game state: ${e.message}")
            return createFallbackState()
        } finally {
            rootNode.recycle()
        }
    }

    /**
     * Create a fallback state when detection fails
     */
    private fun createFallbackState(): BoardState {
        // Use the test board as fallback
        return BoardGenerator.createTestBoard()
    }

    /**
     * Find all card nodes in the accessibility tree
     */
    private fun findCardNodes(node: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val cards = mutableListOf<AccessibilityNodeInfo>()
        
        // Look for nodes that might be cards
        if (isCardNode(node)) {
            cards.add(node)
        }
        
        // Recursively search children
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                cards.addAll(findCardNodes(child))
            }
        }
        
        return cards
    }

    /**
     * Check if node is likely a card
     */
    private fun isCardNode(node: AccessibilityNodeInfo): Boolean {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        
        val width = bounds.width()
        val height = bounds.height()
        
        // Cards have specific size ranges
        if (width in MIN_CARD_WIDTH..MAX_CARD_WIDTH && 
            height in MIN_CARD_HEIGHT..MAX_CARD_HEIGHT) {
            return true
        }
        
        // Check for card-related class names or IDs
        val className = node.className?.toString() ?: ""
        val viewId = node.viewIdResourceName ?: ""
        
        return className.contains("Card", ignoreCase = true) ||
               viewId.contains("card", ignoreCase = true)
    }

    /**
     * Parse a card from an accessibility node
     */
    private fun parseCardNode(node: AccessibilityNodeInfo): Card? {
        try {
            // Try to get card info from content description
            val description = node.contentDescription?.toString() ?: ""
            
            // Look for text like "8 of Spades" or "8♠"
            val rankStr = extractRank(description)
            val suitStr = extractSuit(description)
            
            if (rankStr != null && suitStr != null) {
                val rank = parseRank(rankStr)
                val suit = parseSuit(suitStr)
                
                val bounds = Rect()
                node.getBoundsInScreen(bounds)
                
                return Card(
                    rank = rank,
                    suit = suit,
                    position = Card.Position(0, 0, bounds.centerX()),
                    isFaceUp = true
                )
            }
            
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse card node: ${e.message}")
        }
        
        return null
    }

    private fun extractRank(text: String): String? {
        val ranks = listOf("A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "Ace", "Jack", "Queen", "King")
        return ranks.firstOrNull { text.contains(it, ignoreCase = true) }
    }

    private fun extractSuit(text: String): String? {
        return when {
            text.contains("♠") || text.contains("Spade", ignoreCase = true) -> "Spades"
            text.contains("♥") || text.contains("Heart", ignoreCase = true) -> "Hearts"
            text.contains("♦") || text.contains("Diamond", ignoreCase = true) -> "Diamonds"
            text.contains("♣") || text.contains("Club", ignoreCase = true) -> "Clubs"
            else -> null
        }
    }

    private fun parseRank(rankStr: String): Card.Rank {
        return when (rankStr.uppercase()) {
            "A", "ACE" -> Card.Rank.ACE
            "2" -> Card.Rank.TWO
            "3" -> Card.Rank.THREE
            "4" -> Card.Rank.FOUR
            "5" -> Card.Rank.FIVE
            "6" -> Card.Rank.SIX
            "7" -> Card.Rank.SEVEN
            "8" -> Card.Rank.EIGHT
            "9" -> Card.Rank.NINE
            "10" -> Card.Rank.TEN
            "J", "JACK" -> Card.Rank.JACK
            "Q", "QUEEN" -> Card.Rank.QUEEN
            "K", "KING" -> Card.Rank.KING
            else -> Card.Rank.ACE
        }
    }

    private fun parseSuit(suitStr: String): Card.Suit {
        return when (suitStr.uppercase()) {
            "SPADES", "SPADE" -> Card.Suit.SPADES
            "HEARTS", "HEART" -> Card.Suit.HEARTS
            "DIAMONDS", "DIAMOND" -> Card.Suit.DIAMONDS
            "CLUBS", "CLUB" -> Card.Suit.CLUBS
            else -> Card.Suit.SPADES
        }
    }

    /**
     * Execute a move by tapping cards
     */
    private fun executeMove(move: Move) {
        when (move) {
            is Move.PlayCard -> {
                Log.d(TAG, "Playing: ${move.card}")
                tapCard(move.card)
            }
            is Move.DrawFromStock -> {
                Log.d(TAG, "Drawing from stock")
                tapStock()
            }
        }
    }

    /**
     * Tap a specific card
     */
    private fun tapCard(card: Card) {
        val rootNode = rootInActiveWindow ?: return
        
        try {
            val cardNodes = findCardNodes(rootNode)
            
            // Find the node matching this card
            for (node in cardNodes) {
                val parsedCard = parseCardNode(node)
                if (parsedCard?.rank == card.rank && parsedCard.suit == card.suit) {
                    val bounds = Rect()
                    node.getBoundsInScreen(bounds)
                    tapAt(bounds.centerX().toFloat(), bounds.centerY().toFloat())
                    return
                }
            }
            
            // Fallback: tap based on card position
            tapAt(card.position.col.toFloat(), 400f)
            
        } finally {
            rootNode.recycle()
        }
    }

    /**
     * Tap the stock pile (bottom left)
     */
    private fun tapStock() {
        // Stock pile is typically bottom-left
        tapAt(300f, 1200f)
    }

    /**
     * Simulate tap at screen coordinates
     */
    private fun tapAt(x: Float, y: Float) {
        val path = Path().apply {
            moveTo(x, y)
        }
        
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                Log.d(TAG, "✓ Tapped at ($x, $y)")
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                Log.w(TAG, "✗ Tap cancelled at ($x, $y)")
            }
        }, null)
    }
}
