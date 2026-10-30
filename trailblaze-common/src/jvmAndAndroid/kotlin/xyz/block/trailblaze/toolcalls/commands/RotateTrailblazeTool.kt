package xyz.block.trailblaze.toolcalls.commands

import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable
import xyz.block.trailblaze.toolcalls.ExecutableTrailblazeTool
import xyz.block.trailblaze.toolcalls.TrailblazeToolClass
import xyz.block.trailblaze.toolcalls.TrailblazeToolExecutionContext
import xyz.block.trailblaze.toolcalls.TrailblazeToolResult

@Serializable
@TrailblazeToolClass("rotate")
@LLMDescription(
  "Two-finger rotation around a point, in device pixels (element-bounds units). The fingers start " +
    "on opposite sides of the center and both sweep the given angle around it. Positive degrees " +
    "turn clockwise on screen. Android only.",
)
data class RotateTrailblazeTool(
  @param:LLMDescription("X coordinate of the rotation center.")
  val x: Int,
  @param:LLMDescription("Y coordinate of the rotation center.")
  val y: Int,
  @param:LLMDescription("Distance between the two fingers, in pixels.")
  val span: Int,
  @param:LLMDescription("How far to sweep, in degrees. Positive is clockwise.")
  val degrees: Int,
  @param:LLMDescription("How long the gesture takes, in ms. Default 600.")
  val durationMs: Long = 600,
) : ExecutableTrailblazeTool {

  override suspend fun execute(
    toolExecutionContext: TrailblazeToolExecutionContext,
  ): TrailblazeToolResult {
    val gesture = try {
      MultiTouchGestures.rotate(
        centerX = x.toFloat(),
        centerY = y.toFloat(),
        span = span.toFloat(),
        degrees = degrees.toFloat(),
        durationMs = durationMs,
      )
    } catch (e: IllegalArgumentException) {
      return TrailblazeToolResult.Error.ExceptionThrown(errorMessage = "rotate: ${e.message}", command = this)
    }
    return dispatchMultiTouch("rotate", gesture, toolExecutionContext)
  }
}
