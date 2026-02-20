package com.malvryx.tripeakssolver

/**
 * Card representation
 */
data class Card(
    val rank: Rank,
    val suit: Suit,
    val position: Position,
    val isFaceUp: Boolean = true
) {
    enum class Rank(val value: Int) {
        ACE(1), TWO(2), THREE(3), FOUR(4), FIVE(5), SIX(6), SEVEN(7),
        EIGHT(8), NINE(9), TEN(10), JACK(11), QUEEN(12), KING(13);

        fun canPlayOn(other: Rank): Boolean {
            // TriPeaks: Can play if exactly 1 rank higher or lower
            // Standard rules: NO wrap-around (K and A are not adjacent)
            return Math.abs(this.value - other.value) == 1
        }

        override fun toString() = when(this) {
            ACE -> "A"
            JACK -> "J"
            QUEEN -> "Q"
            KING -> "K"
            else -> value.toString()
        }
    }

    enum class Suit {
        HEARTS, DIAMONDS, CLUBS, SPADES;
        
        override fun toString() = when(this) {
            HEARTS -> "♥"
            DIAMONDS -> "♦"
            CLUBS -> "♣"
            SPADES -> "♠"
        }
    }

    data class Position(val pyramid: Int, val row: Int, val col: Int)

    override fun toString() = "${rank}${suit}"
}

/**
 * Complete board state for TriPeaks
 */
data class BoardState(
    val pyramids: List<List<Card?>>,  // 3 pyramids, each with 4 rows
    val waste: List<Card>,            // Waste pile (top card is playable)
    val stock: List<Card>,            // Draw pile
    val removed: Set<Card> = emptySet() // Removed cards
) {
    val wasteTop: Card? get() = waste.lastOrNull()
    val stockSize: Int get() = stock.size
    val cardsRemaining: Int get() = pyramids.flatten().count { it != null }
    
    fun isWin(): Boolean = cardsRemaining == 0
    
    fun getPlayableCards(): List<Card> {
        val playable = mutableListOf<Card>()
        
        pyramids.forEachIndexed { pyramidIdx, pyramid ->
            pyramid.forEachIndexed { rowIdx, row ->
                row.forEachIndexed { colIdx, card ->
                    if (card != null && card.isFaceUp && isCardPlayable(pyramidIdx, rowIdx, colIdx)) {
                        playable.add(card)
                    }
                }
            }
        }
        
        return playable
    }
    
    private fun isCardPlayable(pyramidIdx: Int, row: Int, col: Int): Boolean {
        // Card is playable if no cards cover it (nothing in row below)
        if (row == pyramids[pyramidIdx].lastIndex) return true
        
        val nextRow = pyramids[pyramidIdx][row + 1]
        // Check if positions below are empty
        return nextRow.getOrNull(col) == null && nextRow.getOrNull(col + 1) == null
    }
    
    fun toHash(): String {
        // Create unique hash for state detection
        val pyramidHash = pyramids.flatten().joinToString(",") { it?.toString() ?: "X" }
        val wasteHash = wasteTop?.toString() ?: "empty"
        return "$pyramidHash|$wasteHash|${stock.size}"
    }
}

/**
 * A move in the game
 */
sealed class Move {
    data class PlayCard(val card: Card, val toWaste: Card) : Move()
    object DrawFromStock : Move()
    
    override fun toString() = when(this) {
        is PlayCard -> "Play ${card} on ${toWaste}"
        is DrawFromStock -> "Draw card"
    }
}

/**
 * Solution result
 */
data class Solution(
    val moves: List<Move>,
    val success: Boolean,
    val cardsCleared: Int,
    val movesCount: Int = moves.size
) {
    override fun toString() = buildString {
        appendLine("=== SOLUTION ===")
        appendLine("Success: $success")
        appendLine("Cards Cleared: $cardsCleared")
        appendLine("Total Moves: $movesCount")
        appendLine("\nMoves:")
        moves.forEachIndexed { idx, move ->
            appendLine("  ${idx + 1}. $move")
        }
    }
}
