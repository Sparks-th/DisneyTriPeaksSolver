package com.malvryx.tripeakssolver

import kotlinx.coroutines.*
import kotlin.math.max

/**
 * Simplified TriPeaks Solver using DFS
 */
class TriPeaksSolver {
    
    private val visitedStates = mutableSetOf<String>()
    private var bestSolution: Solution? = null
    private var maxDepth = 0
    
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
        
        withTimeoutOrNull(timeoutMs) {
            job.join()
        } ?: job.cancel()
        
        bestSolution ?: Solution(
            moves = emptyList(),
            success = false,
            cardsCleared = 0
        )
    }
    
    private fun dfs(
        state: BoardState,
        movesSoFar: List<Move>,
        depth: Int,
        maxIterations: Int
    ) {
        if (visitedStates.size > maxIterations) return
        
        val stateHash = state.toHash()
        if (stateHash in visitedStates) return
        visitedStates.add(stateHash)
        
        maxDepth = max(maxDepth, depth)
        
        if (state.isWin()) {
            val solution = Solution(
                moves = movesSoFar,
                success = true,
                cardsCleared = 28
            )
            if (bestSolution == null || solution.movesCount < bestSolution!!.movesCount) {
                bestSolution = solution
            }
            return
        }
        
        bestSolution?.let { best ->
            if (movesSoFar.size >= best.movesCount) return
        }
        
        val possibleMoves = getPossibleMoves(state)
        
        for ((move, newState) in possibleMoves) {
            dfs(newState, movesSoFar + move, depth + 1, maxIterations)
            if (bestSolution?.success == true) return
        }
        
        if (possibleMoves.isEmpty() && state.stock.isNotEmpty()) {
            val newState = drawCard(state)
            dfs(newState, movesSoFar + Move.DrawFromStock, depth + 1, maxIterations)
        }
        
        val cardsCleared = 28 - state.cardsRemaining
        if (bestSolution == null || cardsCleared > bestSolution!!.cardsCleared) {
            bestSolution = Solution(
                moves = movesSoFar,
                success = false,
                cardsCleared = cardsCleared
            )
        }
    }
    
    private fun getPossibleMoves(state: BoardState): List<Pair<Move, BoardState>> {
        val wasteTop = state.wasteTop ?: return emptyList()
        val playableCards = state.getPlayableCards()
        
        return playableCards
            .filter { it.rank.canPlayOn(wasteTop.rank) }
            .map { card ->
                val move = Move.PlayCard(card, wasteTop)
                val newState = applyMove(state, card)
                move to newState
            }
    }
    
    private fun applyMove(state: BoardState, card: Card): BoardState {
        val newTableCards = state.tableCards.map { if (it == card) null else it }
        return state.copy(
            tableCards = newTableCards,
            waste = state.waste + card,
            removed = state.removed + card
        )
    }
    
    private fun drawCard(state: BoardState): BoardState {
        if (state.stock.isEmpty()) return state
        val drawnCard = state.stock.first()
        return state.copy(
            stock = state.stock.drop(1),
            waste = state.waste + drawnCard
        )
    }
    
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
    
    fun createTestBoard(): BoardState {
        // Create a simple solvable board with 28 cards
        val tableCards = listOf(
            // Bottom row (playable)
            card("7", "C", 0), card("6", "H", 1), card("5", "D", 2), card("4", "S", 3),
            card("7", "D", 4), card("8", "C", 5), card("9", "H", 6), card("10", "S", 7),
            card("6", "C", 8), card("5", "H", 9), card("4", "D", 10), card("3", "S", 11),
            // Middle rows
            card("10", "H", 12), card("9", "S", 13), card("8", "D", 14),
            card("4", "C", 15), card("5", "S", 16), card("6", "H", 17),
            card("9", "H", 18), card("8", "S", 19), card("7", "D", 20),
            // Top rows
            card("Q", "D", 21), card("J", "C", 22),
            card("2", "H", 23), card("3", "D", 24),
            card("J", "D", 25), card("10", "C", 26),
            // Peaks
            card("K", "H", 27)
        )
        
        val stock = listOf(
            card("K", "D", 100),
            card("A", "H", 101),
            card("2", "S", 102)
        )
        
        return BoardState(
            tableCards = tableCards,
            waste = listOf(card("5", "C", 200)),
            stock = stock
        )
    }
    
    private fun card(rank: String, suit: String, id: Int): Card {
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
        
        return Card(rankEnum, suitEnum, Card.Position(0, 0, id))
    }
}
