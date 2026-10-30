package xyz.block.trailblaze.toolcalls.commands

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MultiTouchGesturesTest {

  private fun assertNear(expected: Float, actual: Float, tolerance: Float = 0.01f) =
    assertTrue(abs(expected - actual) <= tolerance, "expected $expected but was $actual")

  private fun spanAt(gesture: MultiTouchGesture, index: Int): Float {
    val a = gesture.fingers[0][index]
    val b = gesture.fingers[1][index]
    return hypot(a.x - b.x, a.y - b.y)
  }

  @Test
  fun `pinch spreads two fingers from the start span to the end span around the center`() {
    val g = MultiTouchGestures.pinch(centerX = 500f, centerY = 1000f, startSpan = 100f, endSpan = 400f)

    assertEquals(2, g.fingers.size)
    assertNear(100f, spanAt(g, 0))
    assertNear(400f, spanAt(g, g.fingers[0].lastIndex))
    // Fingers stay symmetric about the center throughout.
    g.fingers[0].indices.forEach { i ->
      assertNear(500f, (g.fingers[0][i].x + g.fingers[1][i].x) / 2)
      assertNear(1000f, (g.fingers[0][i].y + g.fingers[1][i].y) / 2)
    }
  }

  @Test
  fun `pinch angle 90 puts the fingers one above the other`() {
    val g = MultiTouchGestures.pinch(500f, 1000f, 200f, 100f, angleDegrees = 90f)

    assertNear(g.fingers[0][0].x, g.fingers[1][0].x)
    assertNear(200f, spanAt(g, 0))
    assertNear(100f, spanAt(g, 1))
  }

  @Test
  fun `rotate keeps both fingers on the circle and sweeps the requested angle`() {
    val g = MultiTouchGestures.rotate(centerX = 540f, centerY = 1200f, span = 400f, degrees = 90f)

    g.fingers.forEach { finger ->
      finger.forEach { p -> assertNear(200f, hypot(p.x - 540f, p.y - 1200f)) }
    }
    // The fingers stay opposite each other the whole way round.
    g.fingers[0].indices.forEach { i -> assertNear(400f, spanAt(g, i)) }
    // Finger 0 starts at 3 o'clock and ends at 6 o'clock after a clockwise 90 degrees (y grows downward).
    assertNear(740f, g.fingers[0].first().x)
    assertNear(1200f, g.fingers[0].first().y)
    assertNear(540f, g.fingers[0].last().x)
    assertNear(1400f, g.fingers[0].last().y)
  }

  @Test
  fun `two finger drag moves both fingers by the same delta`() {
    val g = MultiTouchGestures.twoFingerDrag(centerX = 540f, centerY = 1200f, span = 300f, deltaX = 0f, deltaY = -400f)

    g.fingers.forEach { f ->
      assertNear(0f, f.last().x - f.first().x)
      assertNear(-400f, f.last().y - f.first().y)
    }
    assertNear(300f, spanAt(g, 0))
    assertNear(300f, spanAt(g, 1))
  }

  @Test
  fun `invalid parameters are rejected`() {
    assertFailsWith<IllegalArgumentException> { MultiTouchGestures.pinch(0f, 0f, 0f, 100f) }
    assertFailsWith<IllegalArgumentException> { MultiTouchGestures.pinch(0f, 0f, 100f, 100f, durationMs = 0) }
    assertFailsWith<IllegalArgumentException> { MultiTouchGestures.rotate(0f, 0f, -5f, 90f) }
    assertFailsWith<IllegalArgumentException> { MultiTouchGestures.twoFingerDrag(0f, 0f, 100f, 0f, 0f) }
    assertFailsWith<IllegalArgumentException> { MultiTouchGesture(listOf(listOf(TouchPoint(0f, 0f), TouchPoint(1f, 1f))), 100) }
  }
}
