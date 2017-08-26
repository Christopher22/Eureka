package eureka;

import java.util.Observable;
import java.util.Observer;
import java.io.File;

import robocode.util.Utils;
import eureka.Eureka;
import eureka.components.*;
import eureka.helper.*;
import eureka.config.*;

/**
 * The brain - home of the rational agent.
 */
public class Brain extends Observable implements Observer {
    private final Eureka m_eureka;
    private Memory<Parameter> m_memory;
    private final boolean m_isTraining;

    /**
     * The name of the file, which is used to serialize the settings memory.
     */
    public final static String CONFIG_FILENAME = "config.ser";

    /**
     * A commando which should result in a non-specified movement on the battlefield.
     */
    public static class Move implements Signal.Command {
    }

    /**
     * A commando which should result in a stop of any movement.
     */
    public static class Stop implements Signal.Command {
    }

    /**
     * A commando which should result in a non-specified scanning on the battlefield.
     */
    public static class Scan implements Signal.Command {
    }

    /**
     * A commando which should result in a attack towards an enemy.
     */
    public static class Attack implements Signal.Command {
        private final Enemy m_target;

        /**
         * Creates the attack.
         * @param target The target of the attack.
         */
        public Attack(final Enemy target) {
            this.m_target = target;
        }

        /**
         * Returns the target of the attack.
         * @return the target of the attack.
         */
        public Enemy getEnemy() {
            return this.m_target;
        }
    }

    /**
     * A commando which should result in the fire of a bullet with specified power into a specified direction.
     */
    public static class Fire implements Signal.Command {
        private final double m_rotation, m_power;

        /**
         * Creates the new commando.
         * @param rotation The rotation.
         * @param power The power of the bullet.
         */
        public Fire(final double rotation, final double power) {
            this.m_rotation = rotation;
            this.m_power = power;
        }

        /**
         * Returns the specified direction of the gun.
         * @return the rotation of the gun.
         */
        public double getRotation() {
            return this.m_rotation;
        }

        /**
         * Returns the power of the bullet.
         * @return the power of the bullet.
         */
        public double getPower() {
            return this.m_power;
        }
    }

    /**
     * Initialize the new AI.
     */
    public Brain(final Eureka eureka) {
        this.m_eureka = eureka;

        // Load training memory, if possible, or the serialized settings otherwise.
        if ((this.m_memory = Memory.load(new File(eureka.getDataDirectory(), Trainer.TRAINING_FILENAME))) != null) {
            this.m_isTraining = true;
        } else if ((this.m_memory = Memory.load(new File(eureka.getDataDirectory(), Brain.CONFIG_FILENAME))) == null) {
            this.m_isTraining = false;
            this.m_memory = new Memory<Parameter>();
        } else {
            this.m_isTraining = false;
        }
    }

    /**
     * Access the memory.
     * @param name The name of the parameter.
     * @param defaultParameter The parameter which is insert if the key does not exists.
     * @return the parameter or the default.
     */
    public double accessMemory(final String name, final Parameter defaultParameter) {
        return this.m_memory.getValue(name, defaultParameter).getValue();
    }

    /**
     * Checks if the AI is currently in training.
     * @return true if in training.
     */
    public boolean isTraining() {
        return this.m_isTraining;
    }

    /**
     * Start the AI.
     */
    public void life() {
        this.sendSignal(new Brain.Scan());
    }

    /**
     * Sends a signal towards all parts of the robot.
     * @param signal The signal which is to be transmitted.
     */
    private void sendSignal(final Signal signal) {
        if (signal instanceof Signal.Command) {
            this.m_eureka.out.format("[Command] %s\n", signal.getClass().getSimpleName());
        } else if (signal instanceof Signal.CustomEvent) {
            this.m_eureka.out.format("[Custom Event] %s\n",
                    ((Signal.CustomEvent) signal).getCondition().getClass().getSimpleName());
        } else if (signal instanceof Signal.Event) {
            this.m_eureka.out.format("[Event] %s\n", signal.getClass().getSimpleName());
        }

        this.setChanged();
        this.notifyObservers(signal);
    }

    @Override
    public void update(Observable o, Object arg) {
        if (!(arg instanceof Signal)) {
            throw new IllegalArgumentException("Signal expected");
        }

        // Transmit the signal towards the other components.
        this.sendSignal((Signal) arg);

        if (((arg instanceof Leg.MovementDone) || (arg instanceof Fist.BulletFired))
                || (arg instanceof Fist.AimAborted) && !this.m_eureka.getFist().isBusy()) {
            // Move forward after the end of a operation
            this.sendSignal(new Brain.Move());
            this.m_eureka.execute();
        } else if (arg instanceof Eye.RobotNearby && !this.m_eureka.getFist().isBusy()) {
            // Attack a robot nearby.
            this.sendSignal(new Brain.Stop());
            this.sendSignal(new Brain.Attack(((Eye.RobotNearby) arg).getRobot()));
            this.m_eureka.execute();
        } else if (arg instanceof Eye.ScanningComplete) {
            // Continuos scanning after end and move if it was the first one at the beginning.
            this.sendSignal(new Brain.Scan());
            if (!this.m_eureka.getLeg().isBusy()) {
                this.sendSignal(new Brain.Move());
            }
            this.m_eureka.execute();
        } else if (arg instanceof Leg.RobotHit) {
            // Fires in direction of a hitting robot
            double angle = Utils.normalRelativeAngle(this.m_eureka.getHeadingRadians()
                    - this.m_eureka.getGunHeadingRadians() + ((Leg.RobotHit) arg).getBearing());
            this.sendSignal(new Brain.Fire(angle, 3));
            this.m_eureka.execute();
        } else if (arg instanceof Eureka.BattleEnded && !this.isTraining()) {
            // Save or overwrite the settings, if not in training.
            try {
                this.m_memory.save(new File(this.m_eureka.getDataDirectory(), Brain.CONFIG_FILENAME));
            } catch (Exception ex) {
                this.m_eureka.out.println("[ERROR] Saving failed");
            }
        }
    }
}