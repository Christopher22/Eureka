package skynet.helper;

import java.awt.geom.Point2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;

import robocode.*;
import skynet.helper.LinearRegression;
import skynet.Skynet;

public class Enemy {
  public class Contact {
    private long time;
    private double energy;
    private double m_distance;
    private double velocity;
    private Point2D.Double m_position;
    private double m_heading;

    public Contact(Skynet skynet, ScannedRobotEvent enemy) {
      this.time = skynet.getTime();
      this.energy = enemy.getEnergy();
      this.velocity = enemy.getVelocity();
      this.m_heading = enemy.getHeading();

      double angle = Math.toRadians((skynet.getHeading() + enemy.getBearing()) % 360);
      m_position = new Point2D.Double(skynet.getX() + Math.sin(angle) * enemy.getDistance(), skynet.getY() + Math.cos(angle) * enemy.getDistance());

      this.m_distance = this.m_position.distance(skynet.getX(), skynet.getY());
    }

    public double getHeading() {
      return this.m_heading;
    }

    public long getTurn() {
      return this.time;
    }

    public double getEnergy() {
      return this.energy;
    }

    public double getVelocity() {
      return this.velocity;
    }

    public Point2D.Double getAbsolutPosition() {
      return this.m_position;
    }

    public double getDistance() {
      return this.m_distance;
    }
  }

  private String name;
  private ArrayDeque<Contact> events;

  public Enemy(Skynet skynet, ScannedRobotEvent enemy) {
    this.name = enemy.getName();
    this.events = new ArrayDeque<Contact>();
    this.addContact(skynet, enemy);
  }

  public void addContact(Skynet skynet, ScannedRobotEvent enemy) {
    if (!enemy.getName().equals(this.name)) {
      throw new RuntimeException("Name different");
    }

    this.events.push(new Contact(skynet, enemy));
  }

  public Contact firstContact() {
    return this.events.peekFirst();
  }

  public Contact lastContact() {
    return this.events.peekLast();
  }

  public double getDanger() {
    return this.lastContact().getEnergy() / 50;
  }

  public String getName() {
    return this.name;
  }

  public Point2D.Double predictPosition(long turn) {
    Contact last = this.lastContact();
    return new Point2D.Double(
      last.getAbsolutPosition().getX() + Math.sin(Math.toRadians(last.getHeading())) * last.getVelocity() * turn,
      last.getAbsolutPosition().getY() + Math.cos(Math.toRadians(last.getHeading())) * last.getVelocity() * turn
    );
    
    /*final int PREDICTION_TURNS = 3;

    double[] xPos = new double[PREDICTION_TURNS], yPos = new double[PREDICTION_TURNS], timeline = new double[PREDICTION_TURNS];
    
    int index = 0;
    for (Contact contact : this.events) {
      if(index >= PREDICTION_TURNS) {
        break;
      }

      Point2D.Double pos = contact.getAbsolutPosition();
      timeline[index] = (double)contact.getTurn();
      xPos[index] = pos.getX();
      yPos[index] = pos.getY();

      index += 1;
    }

    // Missing data!
    if (index != PREDICTION_TURNS - 1) {
      xPos = Arrays.copyOf(xPos, index + 1);
      yPos = Arrays.copyOf(yPos, index + 1);
      timeline = Arrays.copyOf(timeline, index + 1);
    }

    return new Point2D.Double(
      (new LinearRegression(timeline, xPos)).predict(turn),
      (new LinearRegression(timeline, yPos)).predict(turn)
    ); */
  }
}
