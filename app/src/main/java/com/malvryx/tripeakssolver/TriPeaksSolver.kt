package com.malvryx.tripeakssolver

import kotlinx.coroutines.*
import kotlin.math.max

/**
 * Intelligent TriPeaks Solver using DFS with pruning and heuristics
 */
class TriPeaksSolver {
    
    private val visitedStates = mutableSetOf<String>()
    private var bestSolution: Solution? = null
    private var maxDepth = 0
    
    /**
     * Solve the board and find optimal solution
     */
    suspend fun solve(
        initialState: BoardState,
        maxIterations: Int = 10000,
        timeoutMs: Long = 5000
    ): Solution = withContext(Dispatchers.Default) {
        visitedStates.clear()
        bestSolution = null
        maxDepth = 0
        
        val job = launch {
            dfs(initialState, emptyList(), 0, maxIterations)
        }
        
        // Timeout protection
        withTimeoutOrNull(timeoutMs) {
            job.join()
        } ?: job.cancel()
        
        bestSolution ?: Solution(
            moves = emptyList(),
            success = false,
            cardsCleared = 0
        )
    }
    
    /**
     * Depth-First Search with intelligent pruning
     */
    private fun dfs(
        state: BoardState,
        movesSoFar: List<Move>,
        depth: Int,
        maxIterations: Int
    ) {
        // Check iteration limit
        if (visitedStates.size > maxIterations) return
        
        // Check if already visited this state
        val stateHash = state.toHash()
        if (stateHash in visitedStates) return
        visitedStates.add(stateHash)
        
        maxDepth = max(maxDepth, depth)
        
        // Check win condition
        if (state.isWin()) {
            val solution = Solution(
                moves = movesSoFar,
                success = true,
                cardsCleared = 28  // Total cards in 3 pyramids
            )
            if (bestSolution == null || solution.movesCount < bestSolution!!.movesCount) {
                bestSolution = solution
            }
            return
        }
        
        // Pruning: If we've found a solution, only continue if we can beat it
        bestSolution?.let { best ->
            if (movesSoFar.size >= best.movesCount) return
        }
        
        // Get all possible moves with heuristic scoring
        val possibleMoves = getPrioritizedMoves(state)
        
        // Try each move
        for ((move, newState) in possibleMoves) {
            dfs(newState, movesSoFar + move, depth + 1, maxIterations)
            
            // Early exit if perfect solution found
            if (bestSolution?.success == true) return
        }
        
        // If no card moves available, try drawing
        if (possibleMoves.isEmpty() && state.stock.isNotEmpty()) {
            val newState = drawCard(state)
            dfs(newState, movesSoFar + Move.DrawFromStock, depth + 1, maxIterations)
        }
        
        // Update best partial solution
        val cardsCleared = 28 - state.cardsRemaining
        if (bestSolution == null || cardsCleared > bestSolution!!.cardsCleared) {
            bestSolution = Solution(
                moves = movesSoFar,
                success = false,
                cardsCleared = cardsCleared
            )
        }
    }
    
    /**
     * Get all valid moves prioritized by heuristics
     */
    private fun getPrioritizedMoves(state: BoardState): List<Pair<Move, BoardState>> {
        val wasteTop = state.wasteTop ?: return emptyList()
        val playableCards = state.getPlayableCards()
        
        return playableCards
            .filter { it.rank.canPlayOn(wasteTop.rank) }
            .map { card ->
                val move = Move.PlayCard(card, wasteTop)
                val newState = applyMove(state, card)
                val score = scoreMove(state, card)
                Triple(move, newState, score)
            }
            .sortedByDescending { it.third }  // Sort by score
            .map { it.first to it.second }
    }
    
    /**
     * Heuristic scoring for move prioritization
     */
    private fun scoreMove(state: BoardState, card: Card): Int {
        var score = 0
        
        // Priority 1: Moves that uncover multiple cards (peaks)
        val position = card.position
        if (position.row == 0) score += 100  // Peak card - uncovers 2 cards
        
        // Priority 2: Cards that create more opportunities
        val uncoveredCount = countUncoveredCards(state, card)
        score += uncoveredCount * 50
        
        // Priority 3: Prefer middle ranks (avoid stranding K or A)
        val rankValue = card.rank.value
        if (rankValue in 4..10) score += 20
        
        // Priority 4: Clear same pyramid (better card organization)
        score += position.pyramid * 5
        
        return score
    }
    
    /**
     * Count how many cards will be uncovered by playing this card
     */
    private fun countUncoveredCards(state: BoardState, card: Card): Int {
        val pos = card.position
        val pyramid = state.pyramids[pos.pyramid]
        
        if (pos.row == pyramid.lastIndex) return 0
        
        var count = 0
        val nextRow = pyramid[pos.row + 1]
        
        if (nextRow.getOrNull(pos.col) != null && !nextRow[pos.col]!!.isFaceUp) count++
        if (nextRow.getOrNull(pos.col + 1) != null && !nextRow[pos.col + 1]!!.isFaceUp) count++
        
        return count
    }
    
    /**
     * Apply a card play move to create new state
     */
    private fun applyMove(state: BoardState, card: Card): BoardState {
        val newPyramids = state.pyramids.map { row ->
            row.map { c ->
                if (c == card) null else c
            }
        }
        
        return state.copy(
            pyramids = newPyramids,
            waste = state.waste + card,
            removed = state.removed + card
        )
    }
    
    /**
     * Draw a card from stock
     */
    private fun drawCard(state: BoardState): BoardState {
        if (state.stock.isEmpty()) return state
        
        val drawnCard = state.stock.first()
        return state.copy(
            stock = state.stock.drop(1),
            waste = state.waste + drawnCard
        )
    }
    
    /**
     * Get statistics about the solving process
     */
    fun getStats() = mapOf(
        "statesVisited" to visitedStates.size,
        "maxDepth" to maxDepth,
        "hasSolution" to (bestSolution != null),
        "solutionMoves" to (bestSolution?.movesCount ?: 0)
    )
}

/**
 * Helper to create test boards
 */
object BoardGenerator {
    
    /**
     * Create a simple solvable test board
     */
    fun createTestBoard(): BoardState {
        // Create all cards for all pyramids in a flat 2D structure
        // Row 0: 3 peaks (one from each pyramid)
        // Row 1: 6 cards (2 from each pyramid)
        // Row 2: 9 cards (3 from each pyramid)
        // Row 3: 12 cards (4 from each pyramid)
        
        val allCards = listOf(
            // Row 0 - Peaks
            listOf(
                card("K", "H", 0, 0, 0),  // Pyramid 1 peak
                card("A", "C", 1, 0, 0),  // Pyramid 2 peak
                card("Q", "H", 2, 0, 0)   // Pyramid 3 peak
            ),
            // Row 1
            listOf(
                card("Q", "D", 0, 1, 0), card("J", "C", 0, 1, 1),  // Pyramid 1
                card("2", "H", 1, 1, 0), card("3", "D", 1, 1, 1),  // Pyramid 2
                card("J", "D", 2, 1, 0), card("10", "C", 2, 1, 1)  // Pyramid 3
            ),
            // Row 2
            listOf(
                card("10", "H", 0, 2, 0), card("9", "S", 0, 2, 1), card("8", "D", 0, 2, 2),  // Pyramid 1
                card("4", "C", 1, 2, 0), card("5", "S", 1, 2, 1), card("6", "H", 1, 2, 2),   // Pyramid 2
                card("9", "H", 2, 2, 0), card("8", "S", 2, 2, 1), card("7", "D", 2, 2, 2)    // Pyramid 3
            ),
            // Row 3
            listOf(
                card("7", "C", 0, 3, 0), card("6", "H", 0, 3, 1), card("5", "D", 0, 3, 2), card("4", "S", 0, 3, 3),  // Pyramid 1
                card("7", "D", 1, 3, 0), card("8", "C", 1, 3, 1), card("9", "H", 1, 3, 2), card("10", "S", 1, 3, 3), // Pyramid 2
                card("6", "C", 2, 3, 0), card("5", "H", 2, 3, 1), card("4", "D", 2, 3, 2), card("3", "S", 2, 3, 3)   // Pyramid 3
            )
        )
        
        val stock = listOf(
            card("K", "D", -1, 0, 0),
            card("A", "H", -1, 0, 0),
            card("2", "S", -1, 0, 0)
        )
        
        return BoardState(
            pyramids = allCards,
            waste = listOf(card("5", "C", -1, 0, 0)),
            stock = stock
        )
    }
    
    private fun card(rank: String, suit: String, p: Int, r: Int, c: Int): Card {
        val rankEnum = when(rank) {
            "A" -> Card.Rank.ACE
            "J" -> Card.Rank.JACK
            "Q" -> Card.Rank.QUEEN
            "K" -> Card.Rank.KING
            else -> Card.Rank.values()[rank.toInt() - 1]
        }
        
        val suitEnum = when(suit) {
            "H" -> Card.Suit.HEARTS
            "D" -> Card.Suit.DIAMONDS
            "C" -> Card.Suit.CLUBS
            "S" -> Card.Suit.SPADES
            else -> Card.Suit.HEARTS
        }
        
        return Card(rankEnum, suitEnum, Card.Position(p, r, c))
    }
}
