package skynet.components;

import java.awt.geom.Point2D;
import java.util.Observable;
import java.awt.Graphics2D;
import robocode.util.Utils;

import skynet.Skynet;
import skynet.Brain;
import skynet.helper.*;
import skynet.helper.Signal.Command;
import skynet.helper.Signal.Event;
import skynet.config.*;

/**
 * The fist - used to destroy enemies.
 */
public class Fist extends Component {

    /**
     * An event which is fired after a bulled was fired.
     */
    public static class BulletFired implements Signal.Event {
    }

    public static class AimAborted implements Signal.Event {
    }

    public static class EyeSynchronized extends robocode.Condition {
        final Skynet m_skynet;

        public EyeSynchronized(Skynet skynet) {
            super("EyeSynchronized");
            this.m_skynet = skynet;
        }

        @Override
        public boolean test() {
            return this.m_skynet.getEye().getHeading() == this.m_skynet.getFist().getHeading();
        };
    }

    public final double PowerConstant;

    public final static long TICK_RANGE = 20;
    public final static int ITERATIONS = 15;
    public final static double ACCURACY = 0.01d;

    private Enemy m_currentTarget;
    private double m_firePower;
    private Point2D.Double m_DebuggingTarget;

    /**
     * Creates a new bullet.
     */
    public Fist(Skynet skynet) {
        super(skynet);
        this.m_currentTarget = null;
        this.PowerConstant = skynet.getBrain().accessMemory("Fist/PowerConstant", new Range(500, 400, 600, 50));

        this.skynet.setAdjustGunForRobotTurn(true);
        this.skynet.setAdjustRadarForGunTurn(true);
    }

    /**
     * Aim an enemy.
     */
    private boolean aim(Enemy target) {
        m_currentTarget = target;

        long ct = secant(time(target.lastContact().getDistance(), this.PowerConstant), target, this.PowerConstant);
        Point2D.Double p = target.predictPosition(ct);
        this.m_firePower = calculateFirePower(HelperFunctions.range(this.skynet.getPosition(), p), this.PowerConstant);
        double calculatedBearing = HelperFunctions.bearing(this.skynet.getPosition(), p)
                - this.skynet.getHeadingRadians();
        double turnGun = Utils.normalRelativeAngle(
                this.skynet.getHeadingRadians() - this.skynet.getGunHeadingRadians() + calculatedBearing);

        if (this.skynet.getGunHeat() != 0 || this.m_firePower < robocode.Rules.MIN_BULLET_POWER) {
            m_currentTarget = null;
            return false;
        }

        this.skynet.setTurnGunRightRadians(turnGun);
        this.skynet.addCustomEvent(new robocode.GunTurnCompleteCondition(this.skynet));
        return true;
    }

    private long secant(long time, Enemy e, double powerConstant) {
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

    private static double calculateFirePower(double distance, double powerConstant) {
        return Math.min(powerConstant / distance, robocode.Rules.MAX_BULLET_POWER);
    }

    private static double calculateBulletVelocity(double power) {
        return 20 - robocode.Rules.MAX_BULLET_POWER * power;
    }

    private static long time(double distance, double powerConstant) {
        return (long) (distance / calculateBulletVelocity(calculateFirePower(distance, powerConstant)));
    }

    private double f(long time, Enemy e, double powerConstant) {
        Point2D.Double d = e.predictPosition(time);
        double r = HelperFunctions.range(this.skynet.getPosition(), d);
        return r - calculateBulletVelocity(calculateFirePower(r, powerConstant)) * time;
    }

    public double getHeading() {
        return this.skynet.getGunHeading();
    }

    /**
     * Checks if an enemy is aimed in the moment.
     */
    public boolean isAiming() {
        return m_currentTarget != null;
    }

    @Override
    protected void handleCommand(Command command) {
        if (command instanceof Brain.Attack && !this.aim(((Brain.Attack) command).getEnemy())) {
            this.sendSignal(new AimAborted());
        }
    }

    protected void handleEvent(Event event) {
        if (event instanceof Signal.CustomEvent
                && ((Signal.CustomEvent) event).getCondition() instanceof robocode.GunTurnCompleteCondition
                && this.isAiming()) {
            this.skynet.fire(this.m_firePower);
            m_currentTarget = null;
            m_DebuggingTarget = null;
            this.sendSignal(new BulletFired());
        }
    }

    @Override
    public void drawDebug(Graphics2D g) {
        if (m_DebuggingTarget != null) {
            g.setColor(java.awt.Color.orange);
            g.fillOval((int) m_DebuggingTarget.getX() - 8, (int) m_DebuggingTarget.getY() - 8, 8, 8);
        }
    }
}