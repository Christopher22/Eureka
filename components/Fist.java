package eureka.components;

import java.awt.geom.Point2D;
import java.util.Observable;
import java.awt.Graphics2D;
import robocode.util.Utils;
import robocode.Rules;

import eureka.Eureka;
import eureka.Brain;
import eureka.helper.*;
import eureka.helper.Signal.Command;
import eureka.helper.Signal.Event;
import eureka.helper.Signal.CustomEvent;
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

    private double m_firePower;

    /**
     * Creates a new fist.
     */
    public Fist(final Eureka eureka) {
        super(eureka);
        this.PowerConstant = eureka.getBrain().accessMemory("Fist/PowerConstant", new Range(3, 1, 3, 0.1));

        this.eureka.setAdjustGunForRobotTurn(true);
        this.eureka.setAdjustRadarForGunTurn(true);
    }

    /**
     * Aims an enemy.
     */
    private boolean aim(final Enemy target) {
        // Adopted from http://robowiki.net/wiki/Linear_Targeting.
        final double enemyBearingRadians = Math.toRadians(target.lastContact().getBearing());
        final double headOnBearing = this.eureka.getHeadingRadians() + enemyBearingRadians;
        final double linearBearing = headOnBearing + Math.asin(target.lastContact().getVelocity()
                / Rules.getBulletSpeed(this.PowerConstant) * Math.sin(enemyBearingRadians - headOnBearing));

        return this.aim(Utils.normalRelativeAngle(linearBearing - this.eureka.getGunHeadingRadians()),
                this.PowerConstant);
    }

    /**
     * Aims towards a position and prepare firing.
     */
    private boolean aim(final double gunRotation, final double firePower) {
        // Check for valid gun heat and fire power
        if (this.eureka.getGunHeat() != 0 || firePower < robocode.Rules.MIN_BULLET_POWER) {
            return false;
        }

        // Start turning the gun asynchroniously
        this.m_firePower = firePower;
        this.eureka.setTurnGunRightRadians(gunRotation);
        this.start(new robocode.GunTurnCompleteCondition(this.eureka));
        return true;
    }

    /**
     * Returns the heading of the gun.
     * @return the heading of the gun.
     */
    public double getHeading() {
        return this.eureka.getGunHeading();
    }

    @Override
    protected void handleCommand(Command command) {
        if (command instanceof Brain.Attack && !this.aim(((Brain.Attack) command).getEnemy())) {
            // Send aiming failure, i.e. caused through gun heat
            this.sendSignal(new AimAborted());
        } else if (command instanceof Brain.Move) {
            // Set the gun into the direction of the moving tank
            this.eureka.setTurnGunRightRadians(
                    Utils.normalRelativeAngle(this.eureka.getHeadingRadians() - this.eureka.getGunHeadingRadians()));
        }
    }

    @Override
    protected void handleOperationDone(CustomEvent event) {
        // Fire, if the gun is finally rotated
        this.eureka.fire(this.m_firePower);
        this.sendSignal(new BulletFired());
    }
}