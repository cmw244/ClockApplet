package cs671;

import javax.swing.JFrame;

/** A "binary clock" frame.  This implementation creates an instance
 * of <code>Clock</code> with a timer, passes the clock to a graphical
 * component, and displays this component in a simple frame.
 *
 * @author  Michel Charpentier
 * @version 3.1, 02/12/13
 * @see Clock
 * @see GraphicsClock
 */
public class ClockApplication {

  private final Clock clock;
  private final int width;

  /** Constructs (and displays) a frame with a binary clock.  The
   * clock is <code>size</code> pixels wide.  It is initially stopped
   * and must be started explicitly with the mouse.  The clock is
   * "destroyed" when the frame is closed.
   *
   * @param c the clock
   * @param size the width of the clock, in pixels
   * @see Clock#destroy
   */
  private ClockApplication (Clock c, int size) {
    clock = c;
    width = size;
  }

  private void init () {
    JFrame frame = new JFrame("Binary Clock");
    try {
      frame.setDefaultCloseOperation(javax.swing.JFrame.DISPOSE_ON_CLOSE);
      frame.addWindowListener(new java.awt.event.WindowAdapter() {
          public void windowClosed (java.awt.event.WindowEvent e) {
            clock.destroy();
          }
        });
      frame.setLocationRelativeTo(null); // center of display
      frame.getContentPane().add(new GraphicsClock(clock, width));
      frame.pack();
      frame.setResizable(false);
      frame.setVisible(true);
    } catch (RuntimeException e) {
      clock.destroy();
      throw e;
    }
  }

  /** Sets a handlers that Displays the state of the clock on stdout,
   * for debugging
   */
  private void debug () {
    clock.addObserver(new java.util.Observer() {
        public void update (java.util.Observable o1, Object o2) {
          System.out.println(clock);
        }
      });
  }

  private static void usage () {
    System.out.println("Usage: ClockApplication <#bits> <size> <timer impl>");
    System.out.println("Defaults: ClockApplication 8 500 "+
                       Clock.DEFAULT_TIMER_CLASS);
  }

  /** Starts a binary clock frame.  The first command line parameter
   * is the number of bits (default 8); the second parameter is the
   * width of the clock in pixels (default 500).  The third parameter
   * is the name of a timer class (default "cs671.SimpleClockTimer").
   * The clock is initially stopped.
   *
   * @param args command line parameters
   */
  public static void main (String[] args) {
    int n = 8;
    int size = 500;
    String t = "cs671.UtilClockTimer";//Clock.DEFAULT_TIMER_CLASS;
    String p = null;
    try {
      if (args.length > 0) n = Integer.parseInt(p = args[0]);
      if (args.length > 1) size = Integer.parseInt(p = args[1]);
      if (args.length > 2) t = args[2];
      if (n < 1) {
        System.err.println("not enough bits, using 1");
        n = 1;
      }
      if (size < 150) {
        System.err.println("size is too small, using 150");
        size = 150;
      }
      assert n >= 1; // so only t can be a problem
      final ClockApplication app = new ClockApplication(new Clock(n, t), size);
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
          public void run () {
            app.init();
            // one or both of the following can be commented out for debugging
            //app.debug();
            //app.clock.start();
          }
        });
    } catch (NumberFormatException e) {
      System.err.printf("cannot parse '%s' as a number%n", p);
      usage();
    } catch (IllegalArgumentException e) {
      System.err.printf("cannot create timer: %s%n", e.getMessage());
      usage();
    }
  }
}
