package cs671;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.BitSet;

/** Binary clocks.  These clocks can either be passive objects or
 * include their own timer.  In the latter case, their value is
 * updated every second.  Clock updates (automatic or manual) may
 * increase or decrease the clock value depending on its current
 * "direction".  The direction of a clock can be changed at any time.
 * It is {@link Direction#FORWARD} by default.
 *
 * <p> The index of the least significant bit is 0.  In particular,
 * this means that the value of <code>getBit(0)</code> (or,
 * equivalently, the value of <code>getValues()[0]</code> changes with
 * each call to <code>step()</code> and, if the clock is running, with
 * each tick.
 *
 *<p> Clocks are observable and every state change is forwarded to the
 * clock's observers.  Note that some method calls do not trigger an
 * update, for instance if a clock is set to a value equal to its
 * current value or if the clock's direction is set to a value equal
 * to its current direction.
 *
 *<p> Since a timer thread (on active clocks) needs to access the
 * state of the clock, all state-changing and state-querying methods
 * are thread-safe.
 *
 * @author  Michel Charpentier
 * @version 3.1, 2/11/13
 * @see ClockTimer
 * @see java.util.Observable
 */
public class Clock extends java.util.Observable {

  static final String DEFAULT_TIMER_CLASS = SimpleClockTimer.class.getName();

  /** Number of bits clock will represent */
  private final int nbBits;
  /** Timer to be used to perform tasks */
  private ClockTimer ct;
  /** Array of bits which represent timer in binary */
  private BitSet bits;
  
  /** Direction of clock, either forward or backwards */
  private Direction dir = Clock.Direction.FORWARD;

  /** The "lock" that guards all clock state changes.  Every state
   * change, including automatic changes on active clocks, is
   * performed while owning this lock.
   */
  protected final Object lock;
  
  /** Determines whether or not the timer has already been started */
  private boolean hasStarted = false;

  /** Constructs a passive clock with <code>nbBits</code> bits.  Initially, all
   * bits are off (false).  The clock has no timer.
   *
   * @param nbBits the number of bits of this clock
   * @throws IllegalArgumentException if <code>nbBits &lt; 1</code>
   */
  public Clock (int nbBits) {
    //this(nbBits, DEFAULT_TIMER_CLASS);
     this.nbBits = nbBits;
     bits = new BitSet(nbBits);
     lock = new Object();
  }

  /** Constructs an active clock with <code>nbBits</code> bits.  Initially,
   * all bits are off (false) and the clock is associated with a new
   * timer of the specified class, if one can be constructed using a
   * no-argument constructor.  The clock is initially stopped.
   *
   * @param nbBits the number of bits of this clock
   * @param timerClass the name of a timer-implementing class.  The
   * class must implement the {@code ClockTimer} interface and it must
   * have a public, no-argument constructor.  Clock implementations
   * <em>must</em> at least accept {@code "cs671.SimpleClockTimer"} and
   * {@code "cs671.UtilClockTimer"} as valid timer classes.
   * @throws IllegalArgumentException if <code>nbBits &lt; 1</code> or if
   * the specified class cannot be loaded, cannot be instantiated or
   * is not of type {@code ClockTimer}
   * @see ClockTimer
   */
  public Clock (int nbBits, String timerClass) { // bonus question
    if(nbBits < 1) {
        throw new IllegalArgumentException("nbBits must be greater then zero");
    }
    
    bits = new BitSet(nbBits);
    bits.set(0, nbBits, false);
    
    this.nbBits = nbBits;
    lock = new Object();  
    
    try {
        Class<?> c = Class.forName(timerClass);
        if(ClockTimer.class.isAssignableFrom(c)) {
                Class<? extends ClockTimer> cc = c.asSubclass(ClockTimer.class);  
                Constructor<?> cons = cc.getConstructor();
                if(cons == null) {
                    System.err.println("Constructor does not exist");
                    return;
                }
                cons.setAccessible(true);
                int i = cons.getModifiers();
                if(i == Modifier.STATIC) {
                    
                }
                else {
                    ct = (ClockTimer) cons.newInstance();
                }
            }
            else {
                throw new IllegalArgumentException("Class " + timerClass +
                                                    " is not of type ClockTimer");
            }
    } catch (Exception | Error ex) {
        throw new IllegalArgumentException("Specified class cannot be loaded: " 
                                            + timerClass);
    }
    
    ct.setDelay(1000L);
    ct.setRunnable(new Task(this)); 
  }

  /** Constructs an active clock with <code>nbBits</code> bits.  Initially,
   * all bits are off (false) and the clock is associated with the given
   * timer.  The clock is initially stopped.
   *
   * @param nbBits the number of bits of this clock
   * @param t a timer; if the timer already has a delay and a
   * runnable, they will be reset
   * @throws IllegalArgumentException if <code>nbBits &lt; 1</code> or
   * if timer {@code t} is running
   */
  public Clock (int nbBits, ClockTimer t) {
      if(nbBits < 1) {
          throw new IllegalArgumentException("nbBits must be greater then zero");
      }
      if(t.isRunning()) {
          throw new IllegalArgumentException("Click is running");
      }
      
      bits = new BitSet(nbBits);
      bits.set(0, nbBits, false);
      
      ct = t;
      ct.setDelay(1000L);
      ct.setRunnable(new Task(this));
      this.nbBits = nbBits;
      lock = new Object();
  }
  
  /** 
   * Simple inner class which takes a clock and calls step with that clock 
   */
  class Task implements Runnable {
    Clock clock;  
    public Task(Clock c) { clock = c; }
      @Override
    public void run() {
        synchronized(clock.lock) {
          clock.step();
        }
    }
      
  }

  /** Clock size
   * @return the number of bits in the clock
   */
  public int size () {
    return nbBits;
  }

  /** Permanently terminates the timer of this clock.  A terminated
   * clock cannot be restarted and its timer becomes garbage.  The
   * clock is now a passive object.  The state of a passive clock can
   * still be changed with the various "set" methods but won't change
   * on its own.  If the clock was already passive, the method has no
   * effect and observers are not notified.
   *
   * @see ClockTimer#cancel
   */
  public void destroy () {
      if(ct != null) {
        ct.cancel();
        updateObs();
      }
  }

  /** Starts the clock.  The first bit update occurs after 1
   * second and every second after that, until the clock is stopped.
   *
   * @throws IllegalStateException if the clock is passive or is already running
   */
  public void start () {
      if(ct == null) { throw new IllegalStateException("Clock is passive"); }
      if(ct.isRunning()) { throw new IllegalStateException("Clock is already running"); }
      if(hasStarted == true) { throw new IllegalStateException("Clock was already started"); }
      
      hasStarted = true;
      ct.start();
  }

  /** Stops the clock.  Bit updates stop occurring immediately.  The
   * clock is <em>not</em> passive; it can be restarted later.
   *
   * @throws IllegalStateException if the clock is passive or is not running
   */
  public void stop () {
    if(ct == null) { throw new IllegalStateException("Clock is passive"); }
    if(!ct.isRunning()) { throw new IllegalStateException("Clock is not running"); }
    
    ct.stop();
  }

  /** The status of the clock, as a boolean.
   *
   * @return true iff the clock is currently running.
   */
  public boolean isTicking () {
    if(ct == null) { return false; }
    return ct.isRunning();
  }

  /** Resets the clock.  All bits are set to zero.  If the clock is
   * running, the next bit update will happen 1 second after bits are
   * cleared.  Observers are notified if the clock is running or if it
   * was non-zero.
   */
  public void clear () {
      synchronized(lock) {
        bits.clear();
      }
  }

  /** The value of bit number <code>n</code>.  Least significant bit is bit
   * number 0; most significant bit is bit <code>size()-1</code>.
   *
   * @param n bit number
   * @return boolean value of that bit.
   * @throws IndexOutOfBoundsException if no such bit exists
   */
  public boolean getBit (int n) {
      if((n < 0) || (n > (nbBits-1))) { throw new IndexOutOfBoundsException("No such bit (" + n + ") exists"); }
      
      boolean ret;
      synchronized(lock) {
          ret = bits.get(n);
      }
      return ret;
  }

  /** Sets bit number <code>n</code> to true.  Least significant bit is bit
   * number 0; most significant bit is bit <code>size()-1</code>.
   *
   * @param n bit number to be set to true
   * @return boolean value of that bit before it is set.
   * @throws IndexOutOfBoundsException if no such bit exists
   */
  public boolean setBit (int n) {
      if((n < 0) || (n > (nbBits-1))) { throw new IndexOutOfBoundsException("No such bit (" + n + ") exists"); }
      
      boolean ret;
      synchronized(lock) {
        ret = bits.get(n);
        bits.set(n, true);
        if(ret != bits.get(n)) {
         //updateObs();
        }
      }
      
      return ret;
  }

  /** Sets bit number <code>n</code> to false.  Least significant bit is bit
   * number 0; most significant bit is bit <code>size()-1</code>.
   *
   * @param n bit number to be set to false
   * @return boolean value of that bit before it is set.
   * @throws IndexOutOfBoundsException if no such bit exists
   */
  public boolean clearBit (int n) {
    if((n < 0) || (n > (nbBits-1))) { throw new IndexOutOfBoundsException("No such bit (" + n + ") exists"); }
    boolean ret;
    synchronized(lock) {
        ret = bits.get(n);
        bits.clear(n);
        if(ret != bits.get(n)) {
            //updateObs();
      }
    }
    return ret;
  }

  /** Sets bit number <code>n</code> to its next value.  If the bit
   * was false, it is now true; if it was true, it is now false.  The
   * method returns a "carry" (true when the bit changes from true to
   * false).  Roughly speaking, this is a <code>+1</code> operation on
   * the given bit.  Since the state of the clock is guaranteed to
   * change, observers are always notified.
   *
   * @param n bit number to be set to next value
   * @return carry after the bit is set.
   * @throws IndexOutOfBoundsException if no such bit exists
   */
  public boolean nextBit (int n) {
      if((n < 0) || (n > (nbBits-1))) { throw new IndexOutOfBoundsException("No such bit (" + n + ") exists"); }
      boolean ret;
      synchronized(lock) {
        ret = bits.get(n);
        bits.flip(n);
      }
      return ret;
  }

  /** Clock direction: FORWARD or BACKWARD.
   * @see #setDirection
   */
  public enum Direction {
    /** Forward direction.  Forward steps increase the binary value of
     * the clock by one.  "111111" becomes "000000".
     */
    FORWARD {
        @Override
      public Direction reverse () {
        return BACKWARD;
      }
    },
  /** Backward direction.  Backward steps decrease the binary value of
   * the clock by one.  "000000" becomes "111111".
   */
    BACKWARD {
        @Override
      public Direction reverse () {
        return FORWARD;
      }
    };

    /** Reverses the direction.
     * @return the other direction, i.e., {@code FORWARD.reverse()}
     * returns {@code BACKWARD} and vice-versa.
     */
    abstract public Direction reverse ();
  }

  /** Sets the clock direction, FORWARD or BACKWARD. */
  public void setDirection (Direction d) {
    synchronized(lock) {
      dir = d;
    }
  }

  /** Gets the clock direction.
   * @return the clock's current direction
   */
  public Direction getDirection () {
    return dir;
  }

  /** Steps the clock.  This method increases or decreases the value
   * of the clock by one, according to the current direction.  Note
   * that bit number 0 is guaranteed to change as a result of calling
   * this method and therefore observers are always notified.
   *
   * @see #setDirection
   */
  public void step () {
      synchronized(lock) {
        if(getDirection() == Direction.FORWARD) {
          boolean b = nextBit(0);
          int index = 1;
          while((index < nbBits) && b) {
              b = nextBit(index++);
          }
        }
        else {
          boolean b = nextBit(0);
          int index = 1;
          while((index < nbBits) && !b) {
              b = nextBit(index++);
          } 
        }
      }
       
      updateObs();
  }

  /** Sets each bit value according to the array of booleans.  The
   * array size <em>must</em> equal the number of bits in the clock.
   * Bit number <code>i</code> in the clock is set to the value of
   * boolean number <code>i</code> in the array (i.e., the clock and
   * the array store bits in the same direction).  Note that observers
   * are notified only if parameter {@code v} and the clock differ by
   * at least one bit.
   *
   * @param v boolean value for each bit
   * @throws IllegalArgumentException if the size of the array is
   * different from the number of bits in the clock.
   */
  public void setValue (boolean[] v) {
      if(v.length != nbBits) { 
          throw new IllegalArgumentException("Size of array is differnt from the" 
                 + " number of bits int the clock"); }
      synchronized(lock) {
        for(int i = 0; i < v.length; i++) {
            bits.set(i, v[i]);
        }
      }
      
      //updateObs();
  }

  /** Sets each bit value according the long parameter.  If the clock
   * has more than 64 bits, bits beyond 63 are set to zero.  The least
   * significant bit of the long is also the least significant bit of
   * the clock, e.g., after <code>setLongValue(1L)</code>,
   * <code>getBit(0)</code> is true.  Note that observers
   * are notified only if parameter {@code v} and the clock differ by
   * at least one bit.
   *
   * @param v value for each bit of the clock
   * @throws IndexOutOfBoundsException if <code>value</value> has a
   * bit set to true beyond the clock's capacity.
   */
  public void setLongValue (long v) {
    if(v > (Math.pow(2, nbBits)-1)) { throw new IndexOutOfBoundsException("Value is " + 
                            "beyond clock capacity"); }
    synchronized(lock) {
        bits.clear();
        for(int i = 0; i < nbBits; i++) {
            // sets bits 63 and beyond to 0
            if(i > 62) {
                bits.set(i, false);
                continue;
            }

            // mods with 2 to figure out even or odd, sets bit accordingly, then shifts
            if(v % 2L != 0) { bits.set(i, true); }
            else { bits.set(i, false); }
            v = v >>> 1;
        }
    }
  }

  /** Boolean value for each bit, as an array.  Modifications to this
   * array do not change the clock value.  Boolean number
   * <code>i</code> in the array is equal to bit number <code>i</code>
   * in the clock (i.e., the clock and the array store bits in the same
   * direction).
   *
   * @return boolean value for each bit
   */
  public boolean[] getValue () {
      boolean[] ret = new boolean[nbBits];
      
      synchronized(lock) {
        for(int i = 0; i < bits.length(); i++) {
            ret[i] = bits.get(i);
        }
      }
      return ret;
  }

  /** All bit values, as a long.  The least
   * significant bit of the long is also the least significant bit of
   * the clock.
   * @return boolean value for each bit
   * @throws IllegalStateException if the clock has more
   * than 64 bits <em>and</em> at least one bit beyond 63 is set
   */
  public long getLongValue () {
    if(bitExceed()) { throw new IllegalStateException("Bits are set beyond bit 63"); }
    
    long value = 0L;
    synchronized(lock) {
        for (int i = 0; i < bits.length(); ++i) {
            value += bits.get(i) ? (1L << i) : 0L;
        }
    }
    return value;
  }
  
  /** 
   * Checks to see if the long has bits exceeding our capability
   * @return True is exceeds, false if valid
   */
  private boolean bitExceed() {
    synchronized(lock) {
        for (int i = 64; i < bits.length(); i++) {
          if(bits.get(i)) {
              return true;
          }
        }
    }
    return false;
  }


  /** A string representation of the clock.  This is a string of the
   * form <code>"101010 [ON]"</code> (running clock) or <code>"101010
   * [OFF]"</code> (stopped clock).  The first character of the string
   * is the value of the most significant bit of the clock.  There is
   * a single space between the last bit of the clock and the opening
   * square bracket.  {@code ON} and {@code OFF} are in uppercase.
   * The closing square bracket is the last character in the string
   * (no newline).
   *
   * @return a string representation of the clock
   */
  @Override public String toString () {
      StringBuilder ret = new StringBuilder();
      String retStr;
      synchronized(lock) {
        for(int i = nbBits-1; i >= 0; i--) {
            if(bits.get(i)) { ret.append(1); }
            else { ret.append(0); }
        }
        if(ct != null && ct.isRunning()) { ret.append(" [ON]"); }
        else { ret.append(" [OFF]"); }
        retStr = ret.toString();
      }
      return retStr;
  }
  
  /**
   * Updates all observers a change has been made 
   */
  private void updateObs() {
    super.setChanged();
    super.notifyObservers();
    super.clearChanged();
  }
}
