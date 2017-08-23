package skynet.components;

import java.util.Observable;
import java.util.Observer;
import java.awt.Graphics2D;

import robocode.*;

import skynet.Skynet;
import skynet.helper.Signal;

/**
 * An functional unit of 'Skynet', mimicking human anatomy.
 */
public abstract class Component extends Observable implements Observer {
  protected final Skynet skynet;

  /**
   * Initialize a unit and register it for event handling.
   * @param skynet The instance of skynet.
   */
  public Component(final Skynet skynet) {
    this.skynet = skynet;

    skynet.getBrain().addObserver(this);
    this.addObserver(skynet.getBrain());
  }

  /**
   * Returns the current instance of Skynet.
   * @return an valid instance of 'Skynet'.
   */
  public Skynet getSkynet() {
    return this.skynet;
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
   * Gets notified from the Java Observable and maps the the 'handle' functions.
   * @param o The observable aka the brain.
   * @param arg The signal which is to be processed.
   */
  @Override
  public final void update(final Observable o, final Object arg) {
    if (arg instanceof Signal.Event) {
      this.handleEvent((Signal.Event) arg);
    } else if (arg instanceof Signal.Command) {
      this.handleCommand((Signal.Command) arg);
    } else {
      throw new IllegalArgumentException("Signal expected");
    }
  }
}
