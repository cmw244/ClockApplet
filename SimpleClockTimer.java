package cs671;

/** Simple timers.  This implementation does not rely on more general
 * timers.  Each instance of {@code SimpleClockTimer} has its own
 * thread.  Timers can be stopped and restarted, <em>without switching
 * to a new thread</em>.  The thread is terminated when the timer is
 * canceled.
 *
 * <p> Instances of this class <em>are thread-safe</em> (i.e., a timer
 * instance can be shared among multiple threads).
 *
 * @author  Michel Charpentier
 * @version 3.1, 2/12/13
 * @see #cancel
 */
public class SimpleClockTimer implements ClockTimer {
    /* Runnable to be called by timer at every <code>delay</code> intervals */
    private Runnable r;
    /** Thread which all tasks given to the timer are run on */
    private Thread thr;
    /** Delay interval in between calls to timer */
    private long delay;

    /** Determines whether or not the timer is currently running(i.e. stop not called) */
    private boolean running = false;
    /** Determines whether or not the timer has ever been started */
    private boolean hasBeenStarted = false;
    /** Determines whether or not the timer has been canceled */
    private boolean canceled = false;
    /** Handles start being called twice without being stopped */
    private boolean doubleStart = false;
        
    /* Object which allows for synchronization of lock */
    private final Object lock;
    
  /** Creates a new timer.  The timer is initially stopped.
   * @param r the timer task
   * @param d the timer delay, in milliseconds
   */
  public SimpleClockTimer (Runnable r, long d) {
    this.r = r;
    delay = d;
    thr = new Thread(new T());
    lock = new Object();
  }

  /** Creates a new timer.  The timer has no task and no delay. */
  public SimpleClockTimer () {
    this.r = null;
    delay = 0;  
    thr = new Thread(new T());
    lock = new Object();
  }
  
  /**
   * Class T is passed to the thread to run, implements the run method which
   * performs the task given 
   */
  class T implements Runnable {
    private long saveDelay;
    private long curDelay;
    private boolean saveRunning;
    private boolean saveCanceled;
    private long startTime;
    
    @Override
    public void run() {
        synchronized(lock) {
            startTime = java.lang.System.currentTimeMillis() + delay;
        }
        // Loops to act like a timer
        while(true) {
            if(canceled) { return; }
            // Saves all values for use
            synchronized(lock) {
            saveDelay = delay;
            curDelay = delay;
            saveRunning = running;
            saveCanceled = canceled;
        }
        // If timer is ready to run i.e. perform given task
        if(saveRunning) {
            // Make sure we dont start too early
            while(java.lang.System.currentTimeMillis() < startTime) {
                if(saveCanceled) { return; }
                try {
                    thr.sleep(curDelay);
                } catch (Throwable ex) {
                    startTime = java.lang.System.currentTimeMillis() + delay;
                    curDelay = startTime - java.lang.System.currentTimeMillis();
                }
                synchronized(lock) {
                    saveCanceled = canceled;
                }
            }
            synchronized(lock) {
                saveRunning = running;
                saveDelay = delay;
            }
            // Double check stop hasnt been called
            if(saveRunning) {
                r.run();
                startTime += saveDelay; 
            }
        }
        // Wait for someone to call start, but do not busy wait
        else {
            synchronized(lock) {
                try {
                    lock.wait(saveDelay);
                } catch (Exception | Error ex) {
                }
                curDelay = delay;
                startTime = java.lang.System.currentTimeMillis() + delay;
            }
        }
      }
    }
  }

    @Override
  public boolean isRunning () {
    synchronized(lock) {    
        return running;
    }
  }

    @Override
  public Runnable setRunnable (Runnable r) {
    if(isRunning()) { throw new IllegalStateException("Timer is currently running"); }
    if(canceled) { throw new IllegalStateException("Timer was canceled"); }
    
    Runnable saveR;
    synchronized(lock) {
        saveR = this.r;
        this.r = r;
    }  
    return saveR;
  }

    @Override
  public void setDelay (long d) {
    if(d <= 0) { throw new IllegalArgumentException("Delay is not positive"); }
    if(isRunning()) { throw new IllegalStateException("Timer is currently running"); }
    if(canceled) { throw new IllegalStateException("Timer was canceled"); }
    
    synchronized (lock) {
        delay = d;
    }
  }

    @Override
  public void start () {
    if(delay <= 0) { throw new IllegalStateException("Delay not set"); }
    if(canceled) { throw new IllegalStateException("Timer was canceled"); }
    if(r == null) { throw new IllegalStateException("Timer was canceled"); }
    if(doubleStart) { throw new IllegalStateException("Start called twice without stop"); }
    
    synchronized(lock) {
        if(!hasBeenStarted) {
            thr.start();
            hasBeenStarted = true;
        }
        else {
            thr.interrupt();
            lock.notify();
        }
        running = true;
        doubleStart = true;
    }
  }

    @Override
  public void stop () {
    synchronized (lock) {
        running = false;
        doubleStart = false;
    }
  }

    @Override
  public void cancel () {
    synchronized (lock) {
        try {
            thr.interrupt();
        }
        catch(Throwable t) {}
        synchronized(lock){
            canceled = true;
        }
    }
  }
}