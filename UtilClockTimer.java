package cs671;

import java.util.Timer;
import java.util.TimerTask;

/** Simple wrapping on general-purpose timers.  This implementation
 * relies on {@code java.util.Timer} and adapts it to the
 * specification of {@code ClockTimer}.  Timers can be stopped and
 * restarted, <em>without switching to a new thread</em>.  The thread
 * is terminated when the timer is cancelled.
 *
 * <p>Instances of the class <em>are not thread-safe</em> (i.e., a
 * timer instance should not be shared among multiple threads).
 *
 * @author  Michel Charpentier
 * @version 3.1, 2/12/13
 * @see #cancel
 * @see java.util.Timer
 */
public class UtilClockTimer implements ClockTimer {
    /** Util timer that schedules tasks to be run, and plays catch up if need be*/
    private Timer t;
    /** Delay interval in between calls to timer */
    private long delay;
    /* Runnable to be called by timer at every <code>delay</code> intervals */
    private Runnable r;
    
    /** Determines whether or not the timer has ever been started */
    private boolean started = false;
    /** Determines whether or not the timer has ever been started without being stopped */
    private boolean hasStarted = false;
    
    /** Determines whether or not the timer is currently running(i.e. stop not called) */
    private boolean running = false;
    /** Determines whether or not the timer has been canceled */
    private boolean canceled = false;
        
    /* Object which allows for synchronization of lock */
    private final Object lock;
    
    /* Saves task timer is running in case it needs to be canceled */
    private TimerTask oldTask;
    
  /** Creates a new timer.  The timer is initially stopped.
   * @param r the timer task
   * @param d the timer delay, in milliseconds
   */
  public UtilClockTimer (Runnable r, long d) {
    this.r = r;
    delay = d;
    t = new Timer();
    lock = new Object();
  }
  
  /** Creates a new timer.  The timer has no task and no delay. */
  public UtilClockTimer () {
    this.r = null;
    delay = 0;
    t = new Timer();
    lock = new Object();
  }
  
  /** 
   * Inner class Task extends TimerTask and runs given task using <code>t</code> 
   */
  class Task extends TimerTask {
    @Override
    public void run() {
        synchronized(lock) {
            if(canceled) { return; }
            if(running) {
                r.run();
            }
        }
    }
  }
  
    @Override
  public boolean isRunning () {
    return running;
  }

    @Override
  public Runnable setRunnable (Runnable r) {
    if(isRunning()) { throw new IllegalStateException("Timer is currently running"); }
    if(canceled) { throw new IllegalStateException("Timer was canceled"); }
    
    Runnable old = null;
    synchronized(lock) {
        old = this.r;
        this.r = r;
    }
    return old;
  }

    @Override
  public void setDelay (long d) {
    if(d < 0) { throw new IllegalArgumentException("Delay is not positive"); }
    if(isRunning()) { throw new IllegalStateException("Timer is currently running"); }
    if(canceled) { throw new IllegalStateException("Timer was canceled"); }
    
    synchronized(lock) {
        t.cancel();
        t = null;
        delay = d;
        t = new Timer();
        if(oldTask != null) {
            oldTask.cancel();
        }
        oldTask = new Task();
        t.scheduleAtFixedRate(oldTask, delay, delay);
    }
  }

    @Override
  public void start () {
    if(delay <= -1) { throw new IllegalStateException("Delay not set"); }
    if(r == null) { throw new IllegalStateException("Runnable was not set"); }
    if(canceled) { throw new IllegalStateException("Timer was canceled"); }
    if(hasStarted) { throw new IllegalStateException("Timer was already started"); }
    
    if(!started) {
        synchronized(lock) {
            if(oldTask != null) {
                oldTask.cancel();
            }
            oldTask = new Task();
            if(delay <= 0) { throw new IllegalStateException("Delay is 0"); }
            t.scheduleAtFixedRate(oldTask, delay, delay);
        }
    }
    synchronized(lock) {
        started = true;
        running = true;
        hasStarted = true;
    }
}

    @Override
  public void stop () {
    synchronized(lock) {
        running = false;
        hasStarted = false;
    }
  }

    @Override
  public void cancel () {
    synchronized(lock) {
        canceled = true;
        t.cancel();
    }
  }
}
