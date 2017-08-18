package skynet.components;

import java.awt.geom.Point2D;
import java.util.Observable;

import robocode.util.Utils;

import skynet.helper.*;
import skynet.Skynet;

/**
 * The fist - used to destroy enemies.
 */
public class Fist extends Component {

    /**
     * An event which is fired after a bulled was fired.
     */
    public static class BulletFired implements Event {
        private robocode.Bullet m_bullet;

        /**
         * Creates a new bullet.
         */
        public BulletFired(robocode.Bullet bullet) {
            this.m_bullet = bullet;
        }

        /**
         * Returns the bullet object.
         * @return The bullet.
         */
        public robocode.Bullet getBullet() {
            return this.m_bullet;
        }
    }

    private Enemy m_currentTarget;
    private double m_currentFirePower;

    /**
     * Creates a new bullet.
     */
    public Fist(Skynet skynet) {
        super(skynet);
        this.m_currentTarget = null;

        this.skynet.setAdjustRadarForGunTurn(true);
    }

    /**
     * Aim an enemy.
     */
    public void aim(Enemy target) {
        m_currentTarget = target;

        double distance = target.lastContact().getDistance();
        this.m_currentFirePower = Math.min(500 / distance, 3);
        long time = (long) (distance / (20 - this.m_currentFirePower * 3));
        //Point2D.Double prediction = target.predictPosition(this.skynet.getTime() + time);
        Point2D.Double prediction = target.predictPosition(time);

        this.skynet.out.printf("Distance: %f\n", distance);
        this.skynet.out.printf("Last contact: %f, %f\n", target.lastContact().getAbsolutPosition().x,
                target.lastContact().getAbsolutPosition().y);
        this.skynet.out.printf("Prediction: %f, %f\n", prediction.getX(), prediction.getY());

        //Point2D.Double prediction = target.lastContact().getAbsolutPosition();
        double absDeg = absoluteBearing(this.skynet.getX(), this.skynet.getY(), prediction.getX(), prediction.getY());

        this.skynet.setTurnGunRight(Utils.normalRelativeAngleDegrees(absDeg - this.skynet.getGunHeading()));
        this.skynet.addCustomEvent(new robocode.GunTurnCompleteCondition(this.skynet));
        this.skynet.execute();
    }

    /**
     * Checks if an enemy is aimed in the moment.
     */
    public boolean isAiming() {
        return m_currentTarget != null;
    }

    /**
     * Handles incoming events.
     */
    @Override
    public void update(Observable o, Object arg) {
        if (arg instanceof robocode.GunTurnCompleteCondition) {
            robocode.Bullet b = this.skynet.fireBullet(this.m_currentFirePower);
            m_currentTarget = null;

            this.setChanged();
            this.notifyObservers(new BulletFired(b));
        }
    }

    /**
     * Calculates the absolut bearing.
     * @return absolut bearing.
     */
    private double absoluteBearing(double x1, double y1, double x2, double y2) {
        // Math stolen from http://mark.random-article.com/robocode/lessons/PredictiveShooter.java
        double xo = x2 - x1;
        double yo = y2 - y1;
        double hyp = Point2D.distance(x1, y1, x2, y2);
        double arcSin = Math.toDegrees(Math.asin(xo / hyp));
        double bearing = 0;

        if (xo > 0 && yo > 0) { // both pos: lower-Left
            bearing = arcSin;
        } else if (xo < 0 && yo > 0) { // x neg, y pos: lower-right
            bearing = 360 + arcSin; // arcsin is negative here, actually 360 - ang
        } else if (xo > 0 && yo < 0) { // x pos, y neg: upper-left
            bearing = 180 - arcSin;
        } else if (xo < 0 && yo < 0) { // both neg: upper-right
            bearing = 180 - arcSin; // arcsin is negative here, actually 180 + ang
        }

        return bearing;
    }
}