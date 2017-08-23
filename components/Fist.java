package eureka.components;

import java.awt.geom.Point2D;
import java.util.Observable;
import java.awt.Graphics2D;
import robocode.util.Utils;

import eureka.Eureka;
import eureka.Brain;
import eureka.helper.*;
import eureka.helper.Signal.Command;
import eureka.helper.Signal.Event;
import eureka.config.*;

/**
 * The fist - used to destroy enemies.
 */
public class Fist extends Component {

    /**
     * An event which is fired after a bulled was fired.
     */
    public static class BulletFired implements Signal.Event {
    }

    /**
     * An event which is fired after the aiming had to be aborted.
     */
    public static class AimAborted implements Signal.Event {
    }

    /**
     * The power which is used to calculate the power of the bullet over distance.
     */
    public final double PowerConstant;

    public final static long TICK_RANGE = 20;
    public final static int ITERATIONS = 15;
    public final static double ACCURACY = 0.01d;

    private boolean m_hasTarget;
    private double m_firePower;

    /**
     * Creates a new fist.
     */
    public Fist(final Eureka eureka) {
        super(eureka);
        this.m_hasTarget = false;
        this.PowerConstant = eureka.getBrain().accessMemory("Fist/PowerConstant", new Range(500, 400, 600, 50));

        this.eureka.setAdjustGunForRobotTurn(true);
        this.eureka.setAdjustRadarForGunTurn(true);
    }

    /**
     * Aims an enemy.
     */
    private boolean aim(final Enemy target) {
        long ct = secant(time(target.lastContact().getDistance(), this.PowerConstant), target, this.PowerConstant);
        Point2D.Double p = target.predictPosition(ct);
        double calculatedBearing = HelperFunctions.bearing(this.eureka.getPosition(), p)
                - this.eureka.getHeadingRadians();
        double turnGun = Utils.normalRelativeAngle(
                this.eureka.getHeadingRadians() - this.eureka.getGunHeadingRadians() + calculatedBearing);

        return this.aim(turnGun,
                calculateFirePower(HelperFunctions.range(this.eureka.getPosition(), p), this.PowerConstant));
    }

    /**
     * Aims towards a position and prepare firing.
     */
    private boolean aim(final double gunRotation, final double firePower) {
        if (this.eureka.getGunHeat() != 0 || firePower < robocode.Rules.MIN_BULLET_POWER) {
            this.m_hasTarget = false;
            return false;
        }

        this.m_hasTarget = true;
        this.m_firePower = firePower;
        this.eureka.setTurnGunRightRadians(gunRotation);
        this.eureka.addCustomEvent(new robocode.GunTurnCompleteCondition(this.eureka));
        return true;
    }

    private long secant(final long time, final Enemy e, final double powerConstant) {
        double t0 = time - (TICK_RANGE / 2);
        double t1 = time + (TICK_RANGE / 2);
        double X = t1;
        double lastX = t0;
        int iterationCount = 0;
        double lastfX = f(Math.round(t0), e, powerConstant);
        while ((Math.abs(X - lastX) >= ACCURACY) && (iterationCount < ITERATIONS)) {
            iterationCount++;
            double fX = f(Math.round(X), e, powerConstant);
            if ((fX - lastfX) == 0.0)
                break;
            long nextX = (long) (X - fX * (X - lastX) / (fX - lastfX));
            lastX = X;
            X = nextX;
            lastfX = fX;
        }
        return Math.round(X);
    }

    private static double calculateFirePower(final double distance, final double powerConstant) {
        return Math.min(powerConstant / distance, robocode.Rules.MAX_BULLET_POWER);
    }

    private static double calculateBulletVelocity(final double power) {
        return 20 - robocode.Rules.MAX_BULLET_POWER * power;
    }

    private static long time(final double distance, final double powerConstant) {
        return (long) (distance / calculateBulletVelocity(calculateFirePower(distance, powerConstant)));
    }

    private double f(final long time, final Enemy e, final double powerConstant) {
        Point2D.Double d = e.predictPosition(time);
        double r = HelperFunctions.range(this.eureka.getPosition(), d);
        return r - calculateBulletVelocity(calculateFirePower(r, powerConstant)) * time;
    }

    /**
     * Returns the heading of the gun.
     * @return the heading of the gun.
     */
    public double getHeading() {
        return this.eureka.getGunHeading();
    }

    /**
     * Checks if an enemy is aimed in the moment.
     * @return true if aiming.
     */
    public boolean isAiming() {
        return this.m_hasTarget;
    }

    @Override
    protected void handleCommand(Command command) {
        if (command instanceof Brain.Attack && !this.aim(((Brain.Attack) command).getEnemy())) {
            this.sendSignal(new AimAborted());
        } else if (command instanceof Brain.Move && !this.isAiming()) {
            this.eureka.setTurnGunRightRadians(
                    Utils.normalRelativeAngle(this.eureka.getHeadingRadians() - this.eureka.getGunHeadingRadians()));
        }
    }

    protected void handleEvent(Event event) {
        if (event instanceof Signal.CustomEvent
                && ((Signal.CustomEvent) event).getCondition() instanceof robocode.GunTurnCompleteCondition
                && this.isAiming()) {
            this.eureka.fire(this.m_firePower);
            this.m_hasTarget = false;
            this.sendSignal(new BulletFired());
        }
    }
}