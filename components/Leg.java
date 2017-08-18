package skynet.components;

import robocode.util.Utils;

import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.Observable;
import java.util.Collections;
import java.awt.Graphics2D;

import skynet.Skynet;
import skynet.helper.*;

/**
 * The leg - used to control movement.
 */
public class Leg extends Component {

  /**
   * The definition of a suitable distance towards a border.
   */
  final static double BORDER_DEFINITION = 1.5;

  /**
   * The minimal distance of a new point.
   */
  final static int MINIMAL_MOVEMENT = 100;

  /**
   * The maximal distance torwards a new point.
   */
  final static int MAXIMAL_MOVEMENT = 300;

  /**
   * The number of flightpoints which are to be evaluated.
   */
  final static int FLIGHT_POINTS = 40;

  /**
   * An event which is fired if a current movement is complete.
   */
  public static class MovementDone implements Event {}

  /**
   * A potential position which is evaluated in terms of safety.
   */
  public static class FlyPoint implements Comparable<FlyPoint> {

    private Point2D.Double m_point;
    private double m_danger, m_radians;

    /**
     * Creates a new flypoint.
     */
    public FlyPoint(Skynet skynet, double radians, int distance) {
      this.m_point = new Point2D.Double(skynet.getX() + distance * Math.cos(radians),
          skynet.getY() + distance * Math.sin(radians));

      this.m_radians = radians;
      this.m_danger = this.calculateDanger(skynet);
    }

    /**
     * Returns the position of the point.
     * @return the position.
     */
    public Point2D.Double getPoint() {
      return this.m_point;
    }

    /**
     * Returns the safety of this point.
     * @return the safety of the point.
     */
    public double getDanger() {
      return this.m_danger;
    }

    /**
     * Returns the radians of player torwards this point.
     * @return the direction.
     */
    public double getRadians() {
      return this.m_radians;
    }

    /**
     * Compares a flypoint to another in terms of danger.
     */
    public int compareTo(FlyPoint o) {
      return (int) (this.m_danger - o.m_danger);
    }

    /**
     * Calculates the safety of a position.
     */
    private double calculateDanger(Skynet skynet) {
      if (this.m_point.getX() < skynet.getWidth() * BORDER_DEFINITION
          || this.m_point.getX() > skynet.getBattleFieldWidth() - skynet.getWidth() * BORDER_DEFINITION
          || this.m_point.getY() < skynet.getHeight() * BORDER_DEFINITION
          || this.m_point.getY() > skynet.getBattleFieldHeight() - skynet.getHeight() * BORDER_DEFINITION) {
        return Double.POSITIVE_INFINITY;
      }

      final Point2D.Double ownPos = skynet.getPosition();
      double result = 0;

      for (Enemy e : skynet.getEye().getCurrentEnemies()) {
        Point2D.Double enemyPos = e.lastContact().getAbsolutPosition();
        result += e.getDanger()
            * (1 + Math.abs(Math.cos(calcAngle(ownPos, this.m_point) - calcAngle(enemyPos, this.m_point))))
            / m_point.distance(enemyPos);
      }

      return result;
    }

    /**
     * Calculates the angle between two points.
     * @return the angle between two points.
     */
    private static double calcAngle(Point2D.Double p2, Point2D.Double p1) {
      return Math.atan2(p2.x - p1.x, p2.y - p1.y);
    }
  }

  private boolean m_isMoving;
  private FlyPoint[] m_flyPoints;

  /**
   * Creates a new leg.
   */
  public Leg(Skynet skynet) {
    super(skynet);

    this.m_isMoving = false;
    this.m_flyPoints = new FlyPoint[FLIGHT_POINTS];
  }

  /**
   * Flight to an optimal safe point in range.
   */
  public void fly() {
    final double STEP = (2 * Math.PI) / FLIGHT_POINTS;
    for (int i = 0; i < FLIGHT_POINTS; i++) {
      int distance = Utils.getRandom().nextInt(MAXIMAL_MOVEMENT - MINIMAL_MOVEMENT) + MINIMAL_MOVEMENT;
      this.m_flyPoints[i] = new FlyPoint(this.skynet, STEP / 2 + i * STEP, distance);
    }

    this.move(Collections.min(Arrays.asList(this.m_flyPoints)).getPoint());
  }

  /**
   * Checks if the player is currently moving.
   * @return if the player is moving.
   */
  public boolean isMoving() {
    return this.m_isMoving;
  }

  /**
   * Aborts the current movement.
   */
  public void stop() {
    this.skynet.stop();
    this.m_isMoving = false;
  }

  /**
   * Handles incoming events.
   */
  @Override
  public void update(Observable o, Object arg) {
    if (arg instanceof robocode.MoveCompleteCondition) {
      this.m_isMoving = false;
      this.setChanged();
      this.notifyObservers(new MovementDone());
    }
  }

  @Override
  public void drawDebug(Graphics2D g) {
    g.setColor(java.awt.Color.RED);
		for (Leg.FlyPoint flypoint : this.m_flyPoints) {
			if(flypoint != null) {
				g.drawOval((int)flypoint.getPoint().getX() - 4, (int)flypoint.getPoint().getY() - 4, 4, 4);
			}
		}
  }

  /**
   * Moves to a specific point.
   */
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

    this.skynet.addCustomEvent(new robocode.MoveCompleteCondition(this.skynet));

    this.skynet.execute();
    this.m_isMoving = true;
  }
}
