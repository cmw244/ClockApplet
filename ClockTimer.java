package cs671;

/** Clock timers.  Objects of this type implement simple timers
 * suitable for a single repeating task, like stepping a clock.  The
 * task is specified as a {@code Runnable} and the delay as a number
 * of milliseconds.  Clock timers should not drift: the task should
 * start after a fixed delay from the <em>beginning</em> of the
 * previous run, not the end.
 *
 * Note that, in contrast to {@link #setRunnable}, {@link #setDelay}
 * and {@link #start}, methods {@link #stop} and {@link #cancel} do
 * not throw an {@code IllegalStateException}.  They simply have no
 * effect when called at the wrong time.
 *
 * @author  Michel Charpentier
 * @version 3.1, 2/11/13
 */
public interface ClockTimer {

  /** Sets the timer task.
   * @param r the task
   * @return the previous task
   * @throws IllegalStateException if the timer is currently running or
   * if it was canceled
   */
  public Runnable setRunnable (Runnable r) throws IllegalStateException;

  /** Sets the timer delay.
   * @param d the delay, in milliseconds
   * @throws IllegalStateException if the timer is currently running or
   * if it was canceled
   * @throws IllegalArgumentException if the delay is not positive
   */
  public void setDelay (long d) throws IllegalStateException;

  /** Starts the timer.  The timer should already have a task and a
   * delay.  The first execution of the task is postponed by a value
   * equal to the delay.
   * @throws IllegalStateException if no task or no delay was
   * specified or if the timer was canceled
   */
  public void start () throws IllegalStateException;

  /** Stops the timer.  If the timer is already stopped (or
   * canceled), the method has no effect.  If the timer is later
   * restarted, it will have the same task and delay as before unless
   * they are changed explicitly.
   */
  public void stop ();

  /** Cancels the timer.  If the timer is running, it is first
   * stopped.  canceled timers cannot be restarted.
   */
  public void cancel ();

  /** Timer status.
   * @return true iff the timer is currently running
   */
  public boolean isRunning ();
}