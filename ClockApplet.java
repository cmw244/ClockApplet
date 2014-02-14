package cs671;

import javax.swing.JApplet;
import javax.swing.JPanel;
import javax.swing.BorderFactory;
import java.util.logging.*;

/** A "binary clock" applet.  This implementation creates an instance
 * of <code>Clock</code> with a timer, passes it to a graphical
 * component, and displays this component in the applet pane.
 *
 * @author  Michel Charpentier
 * @version 3.1, 02/12/13
 * @see Clock
 * @see GraphicsClock
 */
public class ClockApplet extends JApplet {

  private static final long serialVersionUID = -166487181622773875L;

  private volatile Clock clock;

  /** The clock is created but not started. */
  public void init () {
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
        public void run () {
          int nbDots = 8;
          int w = 600;
          String nbdots = getParameter("nbdots");
          String width = getParameter("width");
          String timerClass = getParameter("timer");
          if (timerClass == null)
            timerClass = Clock.DEFAULT_TIMER_CLASS;
          if (nbdots != null)
            nbDots = Integer.parseInt(nbdots);
          if (width != null)
            w = Integer.parseInt(width);
          try {
            clock = new Clock(nbDots, timerClass);
          } catch (IllegalArgumentException e) {
            System.err.printf("cannot create timer: %s%n", e.getMessage());
            clock = new Clock(nbDots);
          }
          JPanel pane = new JPanel(new java.awt.BorderLayout());
          pane.setBorder // When applets appear with no border at all, it's ugly
            (BorderFactory.createCompoundBorder
             (BorderFactory.createEtchedBorder(), pane.getBorder()));
          setContentPane(pane);
          pane.add(new GraphicsClock(clock, w));
        }
      });
  }

  /** starts the clock. */
  public void start () {
    clock.start();
  }
  /** Stops the clock. */
  public void stop () {
    clock.stop();
  }
  /** Stops the clock permanently. */
  public void destroy () {
    clock.destroy();
  }
  /** Applet info.
   * @return the string {@code "Binary Clock, \u00a9 2010 Michel Charpentier"}
   */
  public String getAppletInfo () {
    return "Binary Clock, \u00a9 2010 Michel Charpentier";
  }
}
