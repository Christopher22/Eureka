package eureka.components;

import java.util.Observable;
import java.util.Observer;
import java.awt.Graphics2D;

import robocode.*;

import eureka.Eureka;
import eureka.helper.Signal;

/**
 * An functional unit of 'Eureka', mimicking human anatomy.
 */
public abstract class Component extends Observable implements Observer {
  protected final Eureka eureka;

  private robocode.Condition m_currentStatus;

  /**
   * Initialize a unit and register it for event handling.
   * @param eureka The instance of eureka.
   */
  public Component(final Eureka eureka) {
    this.eureka = eureka;
    this.m_currentStatus = null;

    eureka.getBrain().addObserver(this);
    this.addObserver(eureka.getBrain());
  }

  /**
   * Starts an asynchronous operation, determined by a condition.
   * @param condition The condition which determines the end of a asynchronous operation.
   */
  protected void start(robocode.Condition condition) {
    this.stop();
    this.m_currentStatus = condition;
    this.eureka.addCustomEvent(this.m_currentStatus);
  }

  /**
   * Stops an asynchronous operation.
   */
  protected void stop() {
    if (this.m_currentStatus != null) {
      this.eureka.removeCustomEvent(this.m_currentStatus);
      this.m_currentStatus = null;
    }
  }

  /**
   * Checks if a operation is going on.
   * @return True if a operation is going on.
   */
  public boolean isBusy() {
    return this.m_currentStatus != null;
  }

  /**
   * Returns the current instance of Eureka.
   * @return an valid instance of 'Eureka'.
   */
  public Eureka getEureka() {
    return this.eureka;
  }

  /**
   * Callback for handling debugging drawing.
   */
  public void drawDebug(Graphics2D output) {
  }

  /**
   * Sends a signal towards the brain and all the other body parts.
   * @param signal The signal which is to be sended.
   */
  protected void sendSignal(final Signal signal) {
    this.setChanged();
    this.notifyObservers(signal);
  }

  /**
   * Handles an incoming command from the brain.
   * @param command The command which is to be processed.
   */
  protected abstract void handleCommand(Signal.Command command);

  /**
   * Handles an incoming event from the brain or other part of the body.
   * @param event The incoming event.
   */
  protected void handleEvent(Signal.Event event) {
  }

  /**
   * Handes an finished operation of this componend.
   * @param event The condition which determints the end of the operation, already stopped.
   */
  protected void handleOperationDone(Signal.CustomEvent event) {
  }

  /**
   * Gets notified from the Java Observable and maps the the 'handle' functions.
   * @param o The observable aka the brain.
   * @param arg The signal which is to be processed.
   */
  @Override
  public final void update(final Observable o, final Object arg) {
    if (arg instanceof Signal.CustomEvent && ((Signal.CustomEvent) arg).getCondition() == this.m_currentStatus) {
      this.stop();
      this.handleOperationDone((Signal.CustomEvent) arg);
    } else if (arg instanceof Signal.Event) {
      this.handleEvent((Signal.Event) arg);
    } else if (arg instanceof Signal.Command) {
      this.handleCommand((Signal.Command) arg);
    } else {
      throw new IllegalArgumentException("Signal expected");
    }
  }
}
