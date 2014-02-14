package cs671;

import java.awt.Paint;
import java.awt.Color;
import java.awt.Stroke;
import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

/** A "dot" for the binary clock.  A dot can be set or unset and is
 * displayed using two different colors to reflect its state.  It's
 * drawn with a line thickness that increases with the size of the
 * dot, so all dots, big and small look pretty.  Dots, as Swing
 * objects, are not thread-safe.  However, methods {@code set} and
 * {@code unset} are properly synchronized and can be called from any
 * thread.
 */
class Dot extends Ellipse2D.Double {

  private static final long serialVersionUID = 6488626274695860708L;

  private static final Paint OFF = Color.GRAY;
  private static final Paint ON  = Color.ORANGE;

  private final Stroke stroke;

  private Paint color;

  /** Builds a new Dot as a circle of radius <code>r</code>
   * centered in <code>(x,y)</code>.  For aesthetics reasons, small
   * radiuses are rejected.
   *
   * @param x X-coordinate of the center of the circle
   */
  public Dot (double x, double y, double r) {
    super(x-r,y-r,r*2,r*2);
    if (r < 10)
      throw new IllegalArgumentException("Dot radius must be at least 10");
    stroke = new BasicStroke((float)(r / 5));
    synchronized (this) {
      color = OFF;
    }
  }
  /** Paints the dot. */
  public void paint(Graphics g) {
    Paint color;
    synchronized (this) {
      color = this.color;
    }
    Graphics2D g2 = (Graphics2D)g;
    g2.setStroke(stroke);
    g2.setPaint(color);
    g2.fill(this);
    g2.setPaint(Color.BLACK);
    g2.draw(this);
  }
  /** Sets the dot.  In a set state, the dot is painted orange. */
  public synchronized void set () {
    color = ON;
  }
  /** Unsets the dot.  In an unset state, the dot is painted grey. */
  public synchronized void unset () {
    color = OFF;
  }
}
