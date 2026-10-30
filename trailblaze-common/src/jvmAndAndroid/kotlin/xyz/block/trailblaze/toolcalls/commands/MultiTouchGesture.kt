package xyz.block.trailblaze.toolcalls.commands

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import xyz.block.trailblaze.toolcalls.TrailblazeToolExecutionContext
import xyz.block.trailblaze.toolcalls.TrailblazeToolResult

/** A point in device pixels. */
data class TouchPoint(val x: Float, val y: Float)

/**
 * Several fingers moving at once. Each entry of [fingers] is one finger's path: it touches down on
 * the first point, follows the points in order at a constant speed, and lifts after the last. All
 * fingers touch down together and lift together, and the whole gesture lasts [durationMs].
 */
data class MultiTouchGesture(
  val fingers: List<List<TouchPoint>>,
  val durationMs: Long,
) {
  init {
    require(fingers.size >= 2) { "A multi-touch gesture needs at least 2 fingers, got ${fingers.size}" }
    require(fingers.all { it.size >= 2 }) { "Every finger needs a start and an end point" }
    require(durationMs > 0) { "durationMs must be positive, was $durationMs" }
  }
}

/**
 * Builds the finger paths for the two-finger gestures a map needs. Pure geometry, so it is unit
 * tested without a device; the driver turns each [MultiTouchGesture] into simultaneous strokes.
 */
object MultiTouchGestures {

  /** Segments used to approximate the arc of a [rotate]. A straight chord would shrink the radius. */
  private const val ARC_STEPS = 16

  /**
   * Two fingers on the line through ([centerX], [centerY]) at [angleDegrees] (0 = horizontal,
   * 90 = vertical), [startSpan] px apart, moving to [endSpan] px apart. A larger end span zooms in.
   */
  fun pinch(
    centerX: Float,
    centerY: Float,
    startSpan: Float,
    endSpan: Float,
    angleDegrees: Float = 0f,
    durationMs: Long = 600,
  ): MultiTouchGesture {
    require(startSpan > 0 && endSpan > 0) { "Spans must be positive, were $startSpan and $endSpan" }
    val dx = cos(angleDegrees.toRadians())
    val dy = sin(angleDegrees.toRadians())
    fun finger(sign: Float) = listOf(
      TouchPoint(centerX + sign * startSpan / 2 * dx, centerY + sign * startSpan / 2 * dy),
      TouchPoint(centerX + sign * endSpan / 2 * dx, centerY + sign * endSpan / 2 * dy),
    )
    return MultiTouchGesture(listOf(finger(-1f), finger(1f)), durationMs)
  }

  /**
   * Two fingers [span] px apart on opposite sides of ([centerX], [centerY]), both sweeping
   * [degrees] around it. Positive is clockwise on screen (y grows downward).
   */
  fun rotate(
    centerX: Float,
    centerY: Float,
    span: Float,
    degrees: Float,
    durationMs: Long = 600,
  ): MultiTouchGesture {
    require(span > 0) { "span must be positive, was $span" }
    val radius = span / 2
    fun finger(startDegrees: Float) = (0..ARC_STEPS).map { step ->
      val a = (startDegrees + degrees * step / ARC_STEPS).toRadians()
      TouchPoint(centerX + radius * cos(a), centerY + radius * sin(a))
    }
    return MultiTouchGesture(listOf(finger(0f), finger(180f)), durationMs)
  }

  /**
   * Two fingers [span] px apart along [angleDegrees] (0 = side by side), both moving by ([deltaX],
   * [deltaY]) in parallel. Dragged vertically this is the two-finger shove that tilts a map.
   */
  fun twoFingerDrag(
    centerX: Float,
    centerY: Float,
    span: Float,
    deltaX: Float,
    deltaY: Float,
    angleDegrees: Float = 0f,
    durationMs: Long = 600,
  ): MultiTouchGesture {
    require(span > 0) { "span must be positive, was $span" }
    require(deltaX != 0f || deltaY != 0f) { "twoFingerDrag needs a non-zero delta" }
    val dx = cos(angleDegrees.toRadians()) * span / 2
    val dy = sin(angleDegrees.toRadians()) * span / 2
    fun finger(sign: Float): List<TouchPoint> {
      val startX = centerX + sign * dx
      val startY = centerY + sign * dy
      return listOf(TouchPoint(startX, startY), TouchPoint(startX + deltaX, startY + deltaY))
    }
    return MultiTouchGesture(listOf(finger(-1f), finger(1f)), durationMs)
  }

  private fun Float.toRadians(): Float = (this * PI / 180.0).toFloat()
}

/**
 * Hands [gesture] to the driver, or fails when the driver cannot inject several fingers at once.
 * Only the Android accessibility driver can today; the others report an error instead of silently
 * doing nothing, because a gesture that never happened would otherwise record as a success.
 */
internal suspend fun dispatchMultiTouch(
  toolName: String,
  gesture: MultiTouchGesture,
  toolExecutionContext: TrailblazeToolExecutionContext,
): TrailblazeToolResult =
  toolExecutionContext.maestroTrailblazeAgent
    ?.performMultiTouch(gesture, toolExecutionContext.traceId)
    ?: TrailblazeToolResult.Error.ExceptionThrown(
      errorMessage = "$toolName is not supported by this driver: only the Android accessibility driver can inject multi-touch gestures",
    )
