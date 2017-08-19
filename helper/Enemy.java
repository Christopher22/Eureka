package skynet.helper;

import java.awt.geom.Point2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;

import robocode.*;
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
    return this.events.peekLast();
  }

  public Contact lastContact() {
    return this.events.peekFirst();
  }

  public double getDanger() {
    return this.lastContact().getEnergy() / 50;
  }

  public String getName() {
    return this.name;
  }

  public Point2D.Double predictPosition(long turn) {
    Contact last = this.lastContact();

    double ax = last.getVelocity() * Math.sin(last.getHeading());
    double ay = last.getVelocity() * Math.cos(last.getHeading());			
    double bx = (last.getAbsolutPosition().getX()) - (ax * last.getTurn());
    double by = (last.getAbsolutPosition().getY()) - (ay * last.getTurn());
    long t = last.getTurn() + turn;

    return new Point2D.Double(ax * t + bx, ay * t + by);
  }
}
