package eureka.helper;

import java.awt.geom.Point2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;

import robocode.*;

import eureka.Eureka;

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
     * @param eureka The robot.
     * @param enemy The enemy which was found.
     */
    public Contact(final Eureka eureka, final ScannedRobotEvent enemy) {
      super(
          eureka.getX()
              + Math.sin(Math.toRadians((eureka.getHeading() + enemy.getBearing()) % 360)) * enemy.getDistance(),
          eureka.getY()
              + Math.cos(Math.toRadians((eureka.getHeading() + enemy.getBearing()) % 360)) * enemy.getDistance());

      this.m_time = eureka.getTime();
      this.m_energy = enemy.getEnergy();
      this.m_velocity = enemy.getVelocity();
      this.m_heading = enemy.getHeading();
      this.m_bearing = enemy.getBearing();
      this.m_distance = this.distance(eureka.getX(), eureka.getY());
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
  public Enemy(final Eureka eureka, final ScannedRobotEvent enemy) {
    this.m_name = enemy.getName();
    this.m_events = new ArrayDeque<Contact>();
    this.m_dead = null;

    this.addContact(eureka, enemy);
  }

  /**
   * Adds a contact with an enemy.
   * @param eureka The robot.
   * @param enemy The enemy.
   */
  public void addContact(Eureka eureka, ScannedRobotEvent enemy) {
    if (!enemy.getName().equals(this.m_name)) {
      throw new IllegalArgumentException("Name different");
    }

    this.m_events.push(new Contact(eureka, enemy));
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
    if (this.m_events.size() < 2) {
      return this.lastContact();
    }

    // This algorithm is highly inspired by IBM (https://www.ibm.com/developerworks/library/j-circular/) 
    final Contact last = this.lastContact(), secondLast = (Contact) (this.m_events.toArray()[1]);
    final double headingChanged = last.getHeading() - secondLast.getHeading();
    final double diff = turn - last.getTurn();
    final double speed = last.distanceSq(secondLast) / (last.getTurn() - secondLast.getTurn());

    double newX, newY;

    if (Math.abs(headingChanged) > 0.00001) {
      // Choose circular targetting...
      double radius = speed / headingChanged;
      double tothead = diff * headingChanged;
      newY = last.getY() + (Math.sin(last.getHeading() + tothead) * radius) - (Math.sin(last.getHeading()) * radius);
      newX = last.getX() + (Math.cos(last.getHeading()) * radius) - (Math.cos(last.getHeading() + tothead) * radius);
    } else {
      // ... or the linear one.
      newY = last.getY() + Math.cos(last.getHeading()) * speed * diff;
      newX = last.getX() + Math.sin(last.getHeading()) * speed * diff;
    }

    return new Point2D.Double(newX, newY);
  }
}
