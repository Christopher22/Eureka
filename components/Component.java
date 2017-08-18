package skynet.components;

import java.util.Observable;
import java.util.Observer;

import robocode.*;
import skynet.Skynet;

public abstract class Component extends Observable implements Observer {
  protected final Skynet skynet;

  public Component(Skynet skynet) {
    this.skynet = skynet;
    
    skynet.getBrain().addObserver(this);
    this.addObserver(skynet.getBrain());
  }

  public Skynet getSkynet() {
    return this.skynet;
  }
}
