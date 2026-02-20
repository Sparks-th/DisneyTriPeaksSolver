package com.malvryx.tripeakssolver

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.*

/**
 * Accessibility Service for Disney Solitaire automation
 * Phase 2: Will detect cards and auto-play
 */
class AccessibilityAutoService : AccessibilityService() {

    private val TAG = "TriPeaksAccessibility"
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isAutoPlaying = false
    
    companion object {
        const val DISNEY_SOLITAIRE_PACKAGE = "com.superplay.looney"  // Update with actual package
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "✅ Accessibility Service Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        
        // Check if we're in Disney Solitaire
        if (event.packageName != DISNEY_SOLITAIRE_PACKAGE) return
        
        Log.d(TAG, "Event from Disney Solitaire: ${event.eventType}")
        
        // TODO Phase 2: Detect game state and auto-play
        // when (event.eventType) {
        //     AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> detectGameBoard()
        //     AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> updateGameState()
        // }
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

    // ===== AUTO-PLAY FUNCTIONS (Phase 2) =====

    private fun startAutoPlay() {
        isAutoPlaying = true
        Log.d(TAG, "🤖 Auto-play started")
        
        serviceScope.launch {
            // TODO: Implement game loop
            // while (isAutoPlaying) {
            //     detectBoard()
            //     solveBoard()
            //     executeMoves()
            //     delay(1000)
            // }
        }
    }

    private fun stopAutoPlay() {
        isAutoPlaying = false
        Log.d(TAG, "⏹️ Auto-play stopped")
    }

    /**
     * Detect current board state from accessibility tree
     */
    private fun detectBoard(): BoardState? {
        val rootNode = rootInActiveWindow ?: return null
        
        // TODO Phase 2: Parse accessibility tree to find cards
        // Look for card views by ID or contentDescription
        // Extract rank and suit information
        
        Log.d(TAG, "Detecting board state...")
        return null
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
                Log.d(TAG, "Tap completed at ($x, $y)")
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                Log.w(TAG, "Tap cancelled")
            }
        }, null)
    }

    /**
     * Find card nodes in accessibility tree
     */
    private fun findCardNodes(node: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val cards = mutableListOf<AccessibilityNodeInfo>()
        
        // Recursively search for card views
        if (node.className?.contains("Card") == true || 
            node.viewIdResourceName?.contains("card") == true) {
            cards.add(node)
        }
        
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                cards.addAll(findCardNodes(child))
            }
        }
        
        return cards
    }

    /**
     * Log accessibility tree for debugging
     */
    private fun logAccessibilityTree(node: AccessibilityNodeInfo, depth: Int = 0) {
        val indent = "  ".repeat(depth)
        Log.d(TAG, "$indent${node.className} [${node.viewIdResourceName}] '${node.text}'")
        
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                logAccessibilityTree(child, depth + 1)
            }
        }
    }
}
