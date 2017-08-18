package skynet.components;

import robocode.util.Utils;
import robocode.*;

import java.awt.geom.Point2D;
import java.util.Random;
import java.util.LinkedList;
import java.util.Observable;

import skynet.Skynet;
import skynet.helper.Enemy;

public class Leg extends Component {

  final static int MIN_FLIGHT_DISTANCE = 150;
  final static int MAX_FLIGHT_DISTANCE = 300;

  final static int MIN_WAYPOINT_VARIANCE = 40;
  final static int MAX_WAYPOINT_VARIANCE = 120;

  final static int ENEMY_GRAVITY_POWER = 2000;
  final static int ENEMY_DISTANCE_MANIPULATOR = 2;

  final static int WALL_GRAVITY_POWER = 5000;
  final static int WALL_DISTANCE_MANIPULATOR = 0;

  final static int BORDER_DEFINITION = 4;

  public static class MovementDone {}

  private boolean m_isMoving;
  private Random randomGenerator;
  private LinkedList<Point2D.Double> m_waypoints;

  public Leg(Skynet skynet) {
    super(skynet);
    this.m_isMoving = false;
    this.randomGenerator = new Random();
    this.m_waypoints = new LinkedList<>();
  }

  public void fly() {
    Point2D.Double force = new Point2D.Double(0, 0);
    for (Enemy e : this.skynet.getEye().getCurrentEnemies()) {
      calculateForce(force, e.lastContact().getAbsolutPosition(), ENEMY_GRAVITY_POWER, ENEMY_DISTANCE_MANIPULATOR);
    }

    calculateForce(force, new Point2D.Double(this.skynet.getX(), 0), WALL_GRAVITY_POWER, WALL_DISTANCE_MANIPULATOR);
    calculateForce(force, new Point2D.Double(this.skynet.getX(), this.skynet.getBattleFieldHeight()), WALL_GRAVITY_POWER, WALL_DISTANCE_MANIPULATOR);
    calculateForce(force, new Point2D.Double(0, this.skynet.getY()), WALL_GRAVITY_POWER, WALL_DISTANCE_MANIPULATOR);
    calculateForce(force, new Point2D.Double(this.skynet.getBattleFieldWidth(), this.skynet.getY()), WALL_GRAVITY_POWER, WALL_DISTANCE_MANIPULATOR);

    double x = this.skynet.getX(), y = this.skynet.getY();
    if ((x < this.skynet.getWidth() * BORDER_DEFINITION || x > this.skynet.getBattleFieldWidth() - this.skynet.getWidth() * BORDER_DEFINITION)
        && (y < this.skynet.getHeight() * BORDER_DEFINITION || y > this.skynet.getBattleFieldHeight() - this.skynet.getHeight() * BORDER_DEFINITION)) {
      force.x = (force.x + this.randomGenerator.nextInt(3) + 2) * -1;
      force.y = (force.y + this.randomGenerator.nextInt(3) + 2) * -1;
    }

    int distance = this.randomGenerator.nextInt(MAX_FLIGHT_DISTANCE - MIN_FLIGHT_DISTANCE) + MIN_FLIGHT_DISTANCE;
    x = this.skynet.getX() - distance * (force.getX() + 1);
    y = this.skynet.getY() - distance * (force.getY() + 1);

    unpredictableMove(new Point2D.Double(x, y));
  }

  public boolean isMoving() {
    return this.m_isMoving;
  }

  public void stop() {
    this.skynet.stop();
    this.m_waypoints.clear();
  }

  @Override public void update(Observable o, Object arg) {
    if (arg instanceof MoveCompleteCondition) {
      if (!m_waypoints.isEmpty()) {
        this.move(m_waypoints.poll());
      } else {
        this.setChanged();
        this.notifyObservers(new MovementDone());
      }
    }
  }
  public void move(Point2D.Double target) {
    // Check that the robot does not drive against the wall
    double x = Math.min(Math.max(skynet.getWidth(), target.getX()), skynet.getBattleFieldWidth() - skynet.getWidth());
    double y = Math.min(Math.max(skynet.getHeight(), target.getY()),
        skynet.getBattleFieldHeight() - skynet.getHeight());

    // Adapted from http://old.robowiki.net/robowiki?Movement/CodeSnippetBasicGoTo
    double a;
    skynet.setTurnRightRadians(
        Math.tan(a = Math.atan2(x -= (int) skynet.getX(), y -= (int) skynet.getY()) - skynet.getHeadingRadians()));
    skynet.setAhead(Math.hypot(x, y) * Math.cos(a));

    this.skynet.addCustomEvent(new MoveCompleteCondition(this.skynet));

    this.skynet.execute();
    this.m_isMoving = true;
  }

  public void unpredictableMove(Point2D.Double target) {
    boolean direction = this.randomGenerator.nextBoolean();

    for (double time = 0.1; time < 1.0 - (1.0 / 4); time += 1.0 / 4.0) {
      // https://stackoverflow.com/questions/133897/how-do-you-find-a-point-at-a-given-perpendicular-distance-from-a-line
      Point2D.Double waypoint = new Point2D.Double(
        (1.0 - time) * (double)this.skynet.getX() + time * target.getX(),
        (1.0 - time) * (double)this.skynet.getY() + time * target.getY()
      );

      double variance = (double) this.randomGenerator.nextInt(MAX_WAYPOINT_VARIANCE - MIN_WAYPOINT_VARIANCE) + MIN_WAYPOINT_VARIANCE;

      double dx = waypoint.getX() - (double)this.skynet.getX();
      double dy = waypoint.getY() - (double)this.skynet.getY();
      double dist = Math.sqrt(dx * dx + dy * dy);
      dx /= dist;
      dy /= dist;

      if (direction) {
        waypoint.x += (variance / 2) * dy;
        waypoint.y -= (variance / 2) * dx;
      } else {
        waypoint.x -= (variance / 2) * dy;
        waypoint.y += (variance / 2) * dx;
      }

      this.m_waypoints.add(waypoint);
      direction = !direction;
    }

    this.m_waypoints.add(target);
    move(this.m_waypoints.poll());
  }

  private void calculateForce(Point2D.Double old_force, Point2D.Double position, double power, double distanceManipulator) {
    double force = power / Math
        .pow(Math.sqrt(Math.pow(position.getX() - this.skynet.getX(), 2) + Math.pow(position.getY() - this.skynet.getY(), 2)), distanceManipulator);
    double ang = Utils
        .normalRelativeAngleDegrees(Math.PI / 2 - Math.atan2(this.skynet.getY() - position.getY(), this.skynet.getX() - position.getX()));

    old_force.x += Math.sin(ang) * force;
    old_force.y += Math.cos(ang) * force;
  }
}
