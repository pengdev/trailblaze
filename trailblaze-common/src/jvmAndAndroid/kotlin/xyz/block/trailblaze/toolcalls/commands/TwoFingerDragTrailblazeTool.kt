package xyz.block.trailblaze.toolcalls.commands

import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable
import xyz.block.trailblaze.toolcalls.ExecutableTrailblazeTool
import xyz.block.trailblaze.toolcalls.TrailblazeToolClass
import xyz.block.trailblaze.toolcalls.TrailblazeToolExecutionContext
import xyz.block.trailblaze.toolcalls.TrailblazeToolResult

@Serializable
@TrailblazeToolClass("twoFingerDrag")
@LLMDescription(
  "Drag two fingers together, side by side, in device pixels (element-bounds units). Dragged " +
    "vertically this is the two-finger shove that tilts (pitches) a map. Android only.",
)
data class TwoFingerDragTrailblazeTool(
  @param:LLMDescription("X coordinate of the midpoint between the fingers at touch down.")
  val x: Int,
  @param:LLMDescription("Y coordinate of the midpoint between the fingers at touch down.")
  val y: Int,
  @param:LLMDescription("Distance between the two fingers, in pixels.")
  val span: Int,
  @param:LLMDescription("Horizontal distance both fingers move, in pixels. Negative is left.")
  val deltaX: Int = 0,
  @param:LLMDescription("Vertical distance both fingers move, in pixels. Negative is up.")
  val deltaY: Int = 0,
  @param:LLMDescription("Angle of the line through both fingers: 0 = side by side, 90 = one above the other.")
  val angleDegrees: Int = 0,
  @param:LLMDescription("How long the gesture takes, in ms. Default 600.")
  val durationMs: Long = 600,
) : ExecutableTrailblazeTool {

  override suspend fun execute(
    toolExecutionContext: TrailblazeToolExecutionContext,
  ): TrailblazeToolResult {
    val gesture = try {
      MultiTouchGestures.twoFingerDrag(
        centerX = x.toFloat(),
        centerY = y.toFloat(),
        span = span.toFloat(),
        deltaX = deltaX.toFloat(),
        deltaY = deltaY.toFloat(),
        angleDegrees = angleDegrees.toFloat(),
        durationMs = durationMs,
      )
    } catch (e: IllegalArgumentException) {
      return TrailblazeToolResult.Error.ExceptionThrown(errorMessage = "twoFingerDrag: ${e.message}", command = this)
    }
    return dispatchMultiTouch("twoFingerDrag", gesture, toolExecutionContext)
  }
}
