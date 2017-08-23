package skynet.helper;

import java.awt.geom.Point2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;

import robocode.*;

import skynet.Skynet;

/**
 * Represents an enemy on the battlefield and allows to track it.
 */
public class Enemy {

  /**
  * The state of an enemy in a specific point in time.
  */
  public class Contact extends Point2D.Double {
    private long m_time;
    private double m_energy;
    private double m_distance;
    private double m_velocity;
    private double m_heading;
    private double m_bearing;

    /**
     * Creates a new contact from a 'ScannedRobotEvent'.
     * @param skynet The robot.
     * @param enemy The enemy which was found.
     */
    public Contact(final Skynet skynet, final ScannedRobotEvent enemy) {
      super(
          skynet.getX()
              + Math.sin(Math.toRadians((skynet.getHeading() + enemy.getBearing()) % 360)) * enemy.getDistance(),
          skynet.getY()
              + Math.cos(Math.toRadians((skynet.getHeading() + enemy.getBearing()) % 360)) * enemy.getDistance());

      this.m_time = skynet.getTime();
      this.m_energy = enemy.getEnergy();
      this.m_velocity = enemy.getVelocity();
      this.m_heading = enemy.getHeading();
      this.m_bearing = enemy.getBearing();
      this.m_distance = this.distance(skynet.getX(), skynet.getY());
    }

    /**
     * Get the heading of the enemy.
     * @return the heading of the enemy.
     */
    public double getHeading() {
      return this.m_heading;
    }

    /**
     * Returns the turn the enemy was seen.
     * @return the turn.
     */
    public long getTurn() {
      return this.m_time;
    }

    /**
     * Returns the energy of the enemy.
     * @return the energy of the enemy.
     */
    public double getEnergy() {
      return this.m_energy;
    }

    /**
    * Returns the velocity of the enemy.
    * @return the velocity of the enemy.
    */
    public double getVelocity() {
      return this.m_velocity;
    }

    /**
    * Returns the bearing of the enemy.
    * @return the bearing of the enemy.
    */
    public double getBearing() {
      return this.m_bearing;
    }

    /**
    * Returns the distance to the enemy.
    * @return the distance to the enemy.
    */
    public double getDistance() {
      return this.m_distance;
    }
  }

  private String m_name;
  private ArrayDeque<Contact> m_events;
  private Integer m_dead;

  /**
   * Creates a new enemy from a 'ScannedRobotEvent'.
   */
  public Enemy(final Skynet skynet, final ScannedRobotEvent enemy) {
    this.m_name = enemy.getName();
    this.m_events = new ArrayDeque<Contact>();
    this.m_dead = null;

    this.addContact(skynet, enemy);
  }

  /**
   * Adds a contact with an enemy.
   * @param skynet The robot.
   * @param enemy The enemy.
   */
  public void addContact(Skynet skynet, ScannedRobotEvent enemy) {
    if (!enemy.getName().equals(this.m_name)) {
      throw new IllegalArgumentException("Name different");
    }

    this.m_events.push(new Contact(skynet, enemy));
  }

  /**
   * Returns the last contact with the enemy.
   * @return the last contact or 'null'.
   */
  public Contact lastContact() {
    return this.m_events.peekFirst();
  }

  /**
   * Returns the danger of the enemy.
   * @return the danger of the enemy between 0 and 1.
   */
  public double getDanger() {
    return this.lastContact().getEnergy() / 50;
  }

  /**
   * Check if the enemy is still alive.
   * @return true if the enemy is alive.
   */
  public boolean isAlive() {
    return this.m_dead == null;
  }

  /**
   * Returns the turn the enemy died or 'null' if it is still alive.
   * @return the m_time of dead or 'null'
   */
  public Integer getDeadTurn() {
    return this.m_dead;
  }

  /**
   * Sets the time of dead of this enemy.
   * @return tick the tick the enemy died.
   */
  public void setDeadTurn(final int tick) {
    this.m_dead = tick;
  }

  /**
   * Returns the unique idnetifier of the enemy.
   * @return the id of the enemy.
   */
  public String getName() {
    return this.m_name;
  }

  /**
   * Return the name of the robot. If there are multiple robots on the battlefield, this name will be identical.
   * @return the name of the enemy.
   */
  public String getBaseName() {
    final int index = this.m_name.lastIndexOf('(');
    return index > 0 ? this.m_name.substring(0, index - 1).trim() : this.m_name;
  }

  /**
   * Predicts the position of an enemy in a specific turn.
   * @param The turn in future, for which the position may be predicted.
   * @return the predicted position.
   */
  public Point2D.Double predictPosition(final long turn) {
    final Contact last = this.lastContact();

    final double ax = last.getVelocity() * Math.sin(last.getHeading());
    final double ay = last.getVelocity() * Math.cos(last.getHeading());
    final double bx = (last.getX()) - (ax * last.getTurn());
    final double by = (last.getY()) - (ay * last.getTurn());
    final long t = last.getTurn() + turn;

    return new Point2D.Double(ax * t + bx, ay * t + by);
  }
}
