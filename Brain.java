package skynet;

import java.util.Observable;
import java.util.Observer;
import java.io.File;

import robocode.util.Utils;

import skynet.components.*;
import skynet.helper.*;
import skynet.config.*;

public class Brain extends Observable implements Observer {
    private Skynet m_skynet;
    private Memory<Parameter> m_memory;
    private boolean m_isTraining;

    public static class Move implements Signal.Command {
    }

    public static class Stop implements Signal.Command {
    }

    public static class Scan implements Signal.Command {
    }

    public static class Attack implements Signal.Command {
        private final Enemy m_target;
        private final boolean m_interpolate;

        public Attack(final Enemy target, final boolean interpolate) {
            this.m_target = target;
            this.m_interpolate = interpolate;
        }

        public Enemy getEnemy() {
            return this.m_target;
        }

        public boolean shouldInterpolate() {
            return this.m_interpolate;
        }
    }

    public static class Fire implements Signal.Command {
        private final double m_rotation, m_power;

        public Fire(final double rotation, final double power) {
            this.m_rotation = rotation;
            this.m_power = power;
        }

        public double getRotation() {
            return this.m_rotation;
        }

        public double getPower() {
            return this.m_power;
        }
    }

    public Brain(Skynet skynet) {
        this.m_skynet = skynet;
        if ((this.m_memory = Memory.load(new File(skynet.getDataDirectory(), Trainer.TRAINING_FILENAME))) != null) {
            this.m_isTraining = true;
        } else {
            this.m_memory = new Memory<Parameter>();
            this.m_isTraining = false;
        }
    }

    public double accessMemory(String name, Parameter defaultParameter) {
        return this.m_memory.getValue(name, defaultParameter).getValue();
    }

    public boolean isTraining() {
        return this.m_isTraining;
    }

    public void life() {
        this.sendSignal(new Brain.Scan());
    }

    private void sendSignal(Signal mission) {
        this.m_skynet.out.format("[%s] %s\n", mission instanceof Signal.Command ? "Command" : "Event",
                mission.getClass().getSimpleName());

        this.setChanged();
        this.notifyObservers(mission);
    }

    @Override
    public void update(Observable o, Object arg) {
        if (!(arg instanceof Signal)) {
            throw new IllegalArgumentException("Signal expected");
        }

        if (((arg instanceof Leg.MovementDone) || (arg instanceof Fist.BulletFired))
                || (arg instanceof Fist.AimAborted) && !this.m_skynet.getFist().isAiming()) {
            this.sendSignal(new Brain.Move());
            this.m_skynet.execute();
        } else if (arg instanceof Eye.RobotNearby && !this.m_skynet.getFist().isAiming()) {
            this.sendSignal(new Brain.Stop());
            this.sendSignal(new Brain.Attack(((Eye.RobotNearby) arg).getRobot(), true));
            this.m_skynet.execute();
        } else if (arg instanceof Eye.ScanningComplete) {
            this.sendSignal(new Brain.Scan());
            if (!this.m_skynet.getLeg().isMoving()) {
                this.sendSignal(new Brain.Move());
            }
            this.m_skynet.execute();
        } else if (arg instanceof Leg.RobotHit && !this.m_skynet.getFist().isAiming()) {
            this.sendSignal(new Brain.Stop());
            double angle = Utils.normalRelativeAngle(this.m_skynet.getHeadingRadians()
                    - this.m_skynet.getGunHeadingRadians() + ((Leg.RobotHit) arg).getBearing());
            this.sendSignal(new Brain.Fire(angle, 3));
            this.m_skynet.execute();
        }

        this.sendSignal((Signal) arg);
    }
}