package skynet.helper;

import java.awt.geom.Point2D;

import robocode.*;
import robocode.util.Utils;

/**
 * A bunch of helper functions.
 */
public final class HelperFunctions {

    /**
     * Calculates the range between two points.
     * @param f Point1
     * @param t Point2
     * @return the range.
     */
    public static double range(Point2D.Double f, Point2D.Double t) {
        return Math.sqrt((t.x - f.x) * (t.x - f.x) + (t.y - f.y) * (t.y - f.y));
    }

    /**
     * Calculates the bearing between two points.
     * @param f Point1
     * @param t Point2
     * @return the bearing.
     */
    public static double bearing(Point2D.Double f, Point2D.Double t) {
        return Math.atan2((t.x - f.x), (t.y - f.y));
    }

    /**
     * Calculates the bearing between a robot and a point.
     * @param robot The robot
     * @param p The point.
     * @return the bearing releative to the rotation of the robot.
     */
    public static double bearing(AdvancedRobot robot, Point2D.Double p) {
        return Utils.normalAbsoluteAngle(
                bearing(new Point2D.Double(robot.getX(), robot.getY()), p) - robot.getHeadingRadians());

    }
}