package eureka.components;

import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.Observable;
import java.util.Collections;
import java.awt.Graphics2D;

import robocode.util.Utils;

import eureka.Eureka;
import eureka.Brain;
import eureka.helper.*;
import eureka.helper.Signal.Command;
import eureka.helper.Signal.Event;
import eureka.helper.Signal.CustomEvent;
import eureka.config.*;

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

    private final double m_danger, m_radians;

    /**
     * The area which will be highly dangerous because they allows easy prediction of own movement.
     */
    public final static double PI_ENVIRONMENT = Math.PI / 9.0;

    /**
     * Creates a new flypoint.
     */
    public FlightPoint(final Leg leg, final double radians, final int distance) {
      super(leg.eureka.getX() + distance * Math.cos(radians), leg.eureka.getY() + distance * Math.sin(radians));
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
    public int compareTo(final FlightPoint o) {
      return (int) (this.m_danger - o.m_danger);
    }

    /**
     * Calculates the safety of a position.
     */
    private double calculateDanger(final Leg leg) {

      // Checks if the flightpoint is outside the battlefield.
      if (this.getX() < leg.eureka.getWidth() * leg.BorderDefinition
          || this.getX() > leg.eureka.getBattleFieldWidth() - leg.eureka.getWidth() * leg.BorderDefinition
          || this.getY() < leg.eureka.getHeight() * leg.BorderDefinition
          || this.getY() > leg.eureka.getBattleFieldHeight() - leg.eureka.getHeight() * leg.BorderDefinition) {
        return java.lang.Double.POSITIVE_INFINITY;
      }

      // Mark position linear from the current movement as dangerous
      double bearingToRobot = HelperFunctions.bearing(leg.eureka, this);
      if (bearingToRobot < PI_ENVIRONMENT || 2 * Math.PI - bearingToRobot < PI_ENVIRONMENT
          || (bearingToRobot > Math.PI - PI_ENVIRONMENT && bearingToRobot < Math.PI + PI_ENVIRONMENT)) {
        return java.lang.Double.POSITIVE_INFINITY;
      }

      // Cache the position of the robot
      final Point2D.Double ownPos = leg.eureka.getPosition();

      // Mark points nearby on the old position as dangerous
      double result = 0.08 / (leg.m_lastFlightpoint != null ? this.distanceSq(leg.m_lastFlightpoint) : 1);

      // Calculate the danger for enemies around
      for (Enemy e : leg.eureka.getEye().getCurrentEnemies()) {
        result += e.getDanger()
            * (1 + Math
                .abs(Math.cos(HelperFunctions.bearing(ownPos, this) - HelperFunctions.bearing(e.lastContact(), this))))
            / this.distance(e.lastContact());
      }

      return result;
    }
  }

  /**
   * A event which is fired if this robot touches another.
   */
  public static class RobotHit implements Signal.GlobalEvent {
    private robocode.HitRobotEvent m_hit;

    /**
     * Creates a new event.
     * @param hit The hit.
     */
    public RobotHit(robocode.HitRobotEvent hit) {
      this.m_hit = hit;
    }

    /**
     * Returns the bearing of the hit.
     * @return the bearing of the hit.
     */
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

  private FlightPoint[] m_flightPoints;
  private Point2D.Double m_lastFlightpoint;

  /**
   * Creates a new leg.
   */
  public Leg(final Eureka eureka) {
    super(eureka);

    this.m_flightPoints = new FlightPoint[FLIGHT_POINTS];

    // Loads the maximal movement - and check that it is bigger than the minimal movement.
    this.MaximalMovement = (int) eureka.getBrain().accessMemory("Leg/MaxMovement", new Range(180, 120, 300, 10) {
      @Override
      public boolean setValue(double value, Memory<Parameter> currentMemory) {
        if (currentMemory.getValue("Leg/MinMovement", null).getValue() <= value) {
          return super.setValue(value, currentMemory);
        } else {
          return false;
        }
      }
    });

    // Loads the minimal movement - and check that it is bigger than the maximal movement.
    this.MinimalMovement = (int) eureka.getBrain().accessMemory("Leg/MinMovement", new Range(80, 60, 200, 10) {
      @Override
      public boolean setValue(double value, Memory<Parameter> currentMemory) {
        if (currentMemory.getValue("Leg/MaxMovement", null).getValue() >= value) {
          return super.setValue(value, currentMemory);
        } else {
          return false;
        }
      }
    });

    this.BorderDefinition = eureka.getBrain().accessMemory("Leg/Border", new Range(3, 1, 4, 0.5));
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

    // Find the point with minimal danger
    FlightPoint min = Collections.min(Arrays.asList(this.m_flightPoints));
    this.move(min);
  }

  @Override
  /**
   * Aborts the current movement.
   */
  public void stop() {
    if (this.isBusy()) {
      this.eureka.stop();
    }
    super.stop();
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
  protected void handleOperationDone(CustomEvent event) {
    this.sendSignal(new MovementDone());
  }

  @Override
  protected void handleEvent(Signal.Event event) {
    if (event instanceof Leg.RobotHit) {
      this.m_lastFlightpoint = this.eureka.getPosition();
      this.stop();
      this.move(-15);
    } else if (event instanceof Eye.RobotNearby) {
      Point2D.Double position = ((Eye.RobotNearby) event).getRobot().lastContact();
      double bearing = HelperFunctions.bearing(this.eureka, position);
      if (position.distance(this.eureka.getPosition()) < 20 && (bearing > 340 || bearing < 20)) {
        this.m_lastFlightpoint = position;
        this.stop();
        this.move(-15);
      }
    }
  }

  /**
   * Moves to a specific point.
   * @param target The point to move to. 
   */
  protected void move(final FlightPoint target) {
    // Check that the robot does not drive against the wall
    double x = Math.min(Math.max(eureka.getWidth(), target.getX()), eureka.getBattleFieldWidth() - eureka.getWidth());
    double y = Math.min(Math.max(eureka.getHeight(), target.getY()),
        eureka.getBattleFieldHeight() - eureka.getHeight());

    this.m_lastFlightpoint = target;

    // Adapted from http://old.robowiki.net/robowiki?Movement/CodeSnippetBasicGoTo
    double a;
    this.eureka.setTurnRightRadians(
        Math.tan(a = Math.atan2(x -= (int) eureka.getX(), y -= (int) eureka.getY()) - eureka.getHeadingRadians()));

    this.move(Math.hypot(x, y) * Math.cos(a));
  }

  /**
   * Moves torwards a specific distance.
   * @param distance The distance.
   */
  protected void move(final double distance) {
    this.eureka.setAhead(distance);
    this.start(new robocode.MoveCompleteCondition(this.eureka));
  }
}
