package skynet.helper;

import java.awt.geom.Point2D;

import robocode.*;

public final class HelperFunctions {
    public static double range(Point2D.Double f, Point2D.Double t) {
        return Math.sqrt((t.x - f.x) * (t.x - f.x) + (t.y - f.y) * (t.y - f.y));
    }

    public static double bearing(Point2D.Double f, Point2D.Double t) {
        return Math.atan2((t.x - f.x), (t.y - f.y));
    }
}