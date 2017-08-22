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
  public Component(Skynet skynet) {
    this.skynet = skynet;

    skynet.getBrain().addObserver(this);
    this.addObserver(skynet.getBrain());
  }

  /**
   * Returns the current instance of Skynet.
   * @return An valid instance of 'Skynet'.
   */
  public Skynet getSkynet() {
    return this.skynet;
  }

  /**
   * Callback for handling debugging drawing.
   */
  public void drawDebug(Graphics2D output) {
  }

  protected void sendSignal(Signal signal) {
    this.setChanged();
    this.notifyObservers(signal);
  }

  protected abstract void handleCommand(Signal.Command command);

  protected void handleEvent(Signal.Event event) {
  }

  @Override
  public final void update(Observable o, Object arg) {
    if (arg instanceof Signal.Event) {
      this.handleEvent((Signal.Event) arg);
    } else if (arg instanceof Signal.Command) {
      this.handleCommand((Signal.Command) arg);
    } else {
      throw new IllegalArgumentException("Signal expected");
    }
  }
}
