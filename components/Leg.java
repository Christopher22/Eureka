package skynet.components;

import robocode.util.Utils;

import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.Observable;
import java.util.Collections;
import java.awt.Graphics2D;

import skynet.Skynet;
import skynet.Brain;
import skynet.helper.*;
import skynet.helper.Signal.Command;
import skynet.helper.Signal.Event;
import skynet.config.*;

/**
 * The leg - used to control movement.
 */
public class Leg extends Component {

  /**
   * The number of flightpoints which are to be evaluated.
   */
  final static int FLIGHT_POINTS = 80;

  /**
   * An event which is fired if a current movement is complete.
   */
  public static class MovementDone implements Signal.Event {
  }

  /**
   * A potential position which is evaluated in terms of safety.
   */
  public static class FlightPoint extends Point2D.Double implements Comparable<FlightPoint> {

    private double m_danger, m_radians;

    public final static double PI_ENVIRONMENT = Math.PI / 9.0;

    /**
     * Creates a new flypoint.
     */
    public FlightPoint(Leg leg, double radians, int distance) {
      super(leg.skynet.getX() + distance * Math.cos(radians), leg.skynet.getY() + distance * Math.sin(radians));
      this.m_radians = radians;
      this.m_danger = this.calculateDanger(leg);
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
    public int compareTo(FlightPoint o) {
      return (int) (this.m_danger - o.m_danger);
    }

    /**
     * Calculates the safety of a position.
     */
    private double calculateDanger(Leg leg) {
      if (this.getX() < leg.skynet.getWidth() * leg.BorderDefinition
          || this.getX() > leg.skynet.getBattleFieldWidth() - leg.skynet.getWidth() * leg.BorderDefinition
          || this.getY() < leg.skynet.getHeight() * leg.BorderDefinition
          || this.getY() > leg.skynet.getBattleFieldHeight() - leg.skynet.getHeight() * leg.BorderDefinition) {
        return java.lang.Double.POSITIVE_INFINITY;
      }

      double bearingToRobot = HelperFunctions.bearing(leg.skynet, this);
      if (bearingToRobot < PI_ENVIRONMENT || 2 * Math.PI - bearingToRobot < PI_ENVIRONMENT
          || (bearingToRobot > Math.PI - PI_ENVIRONMENT && bearingToRobot < Math.PI + PI_ENVIRONMENT)) {
        return java.lang.Double.POSITIVE_INFINITY;
      }
      /*if ((bearingToRobot > -PI_ENVIRONMENT && bearingToRobot < PI_ENVIRONMENT) || ) {
        
      }*/

      final Point2D.Double ownPos = leg.skynet.getPosition();
      double result = 0.08 / (leg.m_lastFlightpoint != null ? this.distanceSq(leg.m_lastFlightpoint) : 1);

      for (Enemy e : leg.skynet.getEye().getCurrentEnemies()) {
        Point2D.Double enemyPos = e.lastContact().getAbsolutPosition();
        result += e.getDanger()
            * (1 + Math.abs(Math.cos(HelperFunctions.bearing(ownPos, this) - HelperFunctions.bearing(enemyPos, this))))
            / this.distance(enemyPos);
      }

      return result;
    }
  }

  public static class RobotHit implements Signal.GlobalEvent {
    private robocode.HitRobotEvent m_hit;

    public RobotHit(robocode.HitRobotEvent hit) {
      this.m_hit = hit;
    }

    public double getBearing() {
      return this.m_hit.getBearingRadians();
    }
  }

  /**
  * The definition of a suitable distance towards a border.
  */
  public final double BorderDefinition;

  /**
   * The minimal distance of a new point.
   */
  public final int MinimalMovement;

  /**
   * The maximal distance torwards a new point.
   */
  public final int MaximalMovement;

  private boolean m_isMoving;
  private FlightPoint[] m_flightPoints;
  private FlightPoint m_lastFlightpoint;

  /**
   * Creates a new leg.
   */
  public Leg(Skynet skynet) {
    super(skynet);

    this.m_isMoving = false;
    this.m_flightPoints = new FlightPoint[FLIGHT_POINTS];

    this.MaximalMovement = (int) skynet.getBrain().accessMemory("Leg/MaxMovement", new Range(180, 120, 300, 10) {
      @Override
      public boolean setValue(double value, Memory<Parameter> currentMemory) {
        if (currentMemory.getValue("Leg/MinMovement", null).getValue() <= value) {
          return super.setValue(value, currentMemory);
        } else {
          return false;
        }
      }
    });

    this.MinimalMovement = (int) skynet.getBrain().accessMemory("Leg/MinMovement", new Range(80, 60, 200, 10) {
      @Override
      public boolean setValue(double value, Memory<Parameter> currentMemory) {
        if (currentMemory.getValue("Leg/MaxMovement", null).getValue() >= value) {
          return super.setValue(value, currentMemory);
        } else {
          return false;
        }
      }
    });

    this.BorderDefinition = skynet.getBrain().accessMemory("Leg/Border", new Range(3, 1, 4, 0.5));
  }

  /**
   * Flight to an optimal safe point in range.
   */
  protected void flight() {
    final double STEP = (2 * Math.PI) / FLIGHT_POINTS;
    for (int i = 0; i < FLIGHT_POINTS; i++) {
      int distance = Utils.getRandom().nextInt(this.MaximalMovement - this.MinimalMovement) + this.MinimalMovement;
      this.m_flightPoints[i] = new FlightPoint(this, STEP / 2 + i * STEP, distance);
    }

    FlightPoint min = Collections.min(Arrays.asList(this.m_flightPoints));
    this.move(min);
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

  @Override
  public void drawDebug(Graphics2D g) {

    for (Leg.FlightPoint flypoint : this.m_flightPoints) {
      if (flypoint != null) {
        g.setColor(flypoint.m_danger != Double.POSITIVE_INFINITY ? java.awt.Color.RED : java.awt.Color.GRAY);
        g.fillOval((int) flypoint.getX() - 8, (int) flypoint.getY() - 8, 8, 8);
      }
    }
  }

  @Override
  protected void handleCommand(Command command) {
    if (command instanceof Brain.Move) {
      this.flight();
    }
  }

  @Override
  protected void handleEvent(Event event) {
    if (event instanceof Signal.CustomEvent
        && ((Signal.CustomEvent) event).getCondition() instanceof robocode.MoveCompleteCondition) {
      this.m_isMoving = false;
      this.setChanged();
      this.notifyObservers(new MovementDone());
    }
  }

  /**
   * Moves to a specific point.
   */
  public void move(FlightPoint target) {
    // Check that the robot does not drive against the wall
    double x = Math.min(Math.max(skynet.getWidth(), target.getX()), skynet.getBattleFieldWidth() - skynet.getWidth());
    double y = Math.min(Math.max(skynet.getHeight(), target.getY()),
        skynet.getBattleFieldHeight() - skynet.getHeight());

    m_lastFlightpoint = target;

    // Adapted from http://old.robowiki.net/robowiki?Movement/CodeSnippetBasicGoTo
    double a;
    skynet.setTurnRightRadians(
        Math.tan(a = Math.atan2(x -= (int) skynet.getX(), y -= (int) skynet.getY()) - skynet.getHeadingRadians()));
    skynet.setAhead(Math.hypot(x, y) * Math.cos(a));

    this.skynet.addCustomEvent(new robocode.MoveCompleteCondition(this.skynet));

    this.m_isMoving = true;
  }
}
