package cs671;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Observable;
import java.util.Observer;


/** Graphical representation of a binary clock.
 *
 * @author  Michel Charpentier
 * @version 3.1, 02/12/13
 * @see Clock
 */
public class GraphicsClock extends javax.swing.JComponent implements MouseListener, Observer {

  private static final long serialVersionUID = -4385200557405026128L;

  private final Dot[] dots;
  private final Clock clock;

  /** Builds a graphical representation of the given clock.
   *
   * @param clock the clock to be displayed
   * @param width the width of the component
   */
  public GraphicsClock (Clock clock, int width) {
    this.clock = clock;
    int nbBits = clock.size();
    dots = new Dot[nbBits];
    double r = (width - 10) / (2.25 * nbBits);
    if (r < 10) { r = 10; }
    double y = r * 1.25;
    double x = 2.25 * r;
    for (int i=0; i<dots.length; i++) {
      Dot d = dots[i] = new Dot(r*1.25+i*x, y, r);
      if (clock.getBit(nbBits - i - 1)) {
        d.set();
      }
    }
    setPreferredSize(new java.awt.Dimension(width,(int)(1.1*width/nbBits)));
    
    clock.addObserver(this);
    addMouseListener(this);
  }

  /** Paints the clock as a line of big dots.
   * @see <a href="Dot.java">Dot.java</a>
   */
    @Override
  protected void paintComponent (java.awt.Graphics  g) {
    for (Dot dot : dots) {
          dot.paint(g);
      }
  }

    @Override
    public void mouseClicked(MouseEvent e) {
        if(e.getButton() == MouseEvent.BUTTON1) {// left
            if(e.getClickCount() == 1) { // single click
                double x = e.getX();
                double y = e.getY();
                int index = 0;
                for(Dot dot : dots) {
                    if(dot.contains(x, y)) {
                        if(clock.getBit(clock.size() - index-1)) {
                            clock.clearBit(clock.size() - index-1);
                        }
                        else {
                            clock.setBit(clock.size()-index-1);
                        }
                    }
                    index += 1;
                }
            }
            else if(e.getClickCount() == 2) { // double click
                if(e.isShiftDown()) {
                    long curTime = java.lang.System.currentTimeMillis()/1000;
                    int bits = clock.size();
                    // amount of bits we dont want to use
                    int shiftAmount = 64 - bits;
                    // shift over to lose excess bits but keep valued bits
                    curTime = curTime << shiftAmount;
                    // shift back to restore value with 0 in bits we dont want
                    curTime = curTime >> shiftAmount;
                    clock.setLongValue(curTime);
                }
                else {
                    clock.clear();
                }
            }
        }
        else if(e.getButton() == MouseEvent.BUTTON2) { // middle
            clock.setDirection(clock.getDirection().reverse());
        }
        else if(e.getButton() == MouseEvent.BUTTON3) { // right
            if(clock.isTicking()) {
                clock.stop();
            }
            else {
                clock.start();
            }
        } 
    }

    @Override
    public void mousePressed(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

    @Override
    public void update(Observable o, Object arg) {
        /* Re-draw dots and repaint */
        for (int i=0; i<dots.length; i++) {
            Dot d = dots[i];
         if (clock.getBit(clock.size() - i - 1)) {
                d.set();
            }
         else {
             d.unset();
         }
        }
        this.repaint();
    }
}
