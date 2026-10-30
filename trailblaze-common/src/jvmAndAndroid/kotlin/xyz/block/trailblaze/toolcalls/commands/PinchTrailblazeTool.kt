package xyz.block.trailblaze.toolcalls.commands

import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable
import xyz.block.trailblaze.toolcalls.ExecutableTrailblazeTool
import xyz.block.trailblaze.toolcalls.TrailblazeToolClass
import xyz.block.trailblaze.toolcalls.TrailblazeToolExecutionContext
import xyz.block.trailblaze.toolcalls.TrailblazeToolResult

@Serializable
@TrailblazeToolClass("pinch")
@LLMDescription(
  "Two-finger pinch around a point, in device pixels (element-bounds units). A larger endSpan than " +
    "startSpan spreads the fingers apart (zoom in); a smaller one pinches them together (zoom out). " +
    "Android only.",
)
data class PinchTrailblazeTool(
  @param:LLMDescription("X coordinate of the pinch center.")
  val x: Int,
  @param:LLMDescription("Y coordinate of the pinch center.")
  val y: Int,
  @param:LLMDescription("Distance between the two fingers when they touch down, in pixels.")
  val startSpan: Int,
  @param:LLMDescription("Distance between the two fingers when they lift, in pixels.")
  val endSpan: Int,
  @param:LLMDescription("Angle of the line through both fingers: 0 = horizontal, 90 = vertical.")
  val angleDegrees: Int = 0,
  @param:LLMDescription("How long the gesture takes, in ms. Default 600.")
  val durationMs: Long = 600,
) : ExecutableTrailblazeTool {

  override suspend fun execute(
    toolExecutionContext: TrailblazeToolExecutionContext,
  ): TrailblazeToolResult {
    val gesture = try {
      MultiTouchGestures.pinch(
        centerX = x.toFloat(),
        centerY = y.toFloat(),
        startSpan = startSpan.toFloat(),
        endSpan = endSpan.toFloat(),
        angleDegrees = angleDegrees.toFloat(),
        durationMs = durationMs,
      )
    } catch (e: IllegalArgumentException) {
      return TrailblazeToolResult.Error.ExceptionThrown(errorMessage = "pinch: ${e.message}", command = this)
    }
    return dispatchMultiTouch("pinch", gesture, toolExecutionContext)
  }
}
