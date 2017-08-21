package skynet;

import java.util.Observable;
import java.util.Observer;
import java.io.File;

import skynet.components.*;
import skynet.helper.*;
import skynet.config.*;

public class Brain extends Observable implements Observer {
    private Skynet m_skynet;
    private static Settings m_memory;
    private static boolean m_isTraining;

    static {
        try {
            if ((Brain.m_memory = Settings.load(new File(Trainer.TRAINING_FILENAME))) != null) {
                Brain.m_isTraining = true;
            } else {
                Brain.m_memory = new Settings();
                Brain.m_isTraining = false;
            }
        } catch (Exception e) {
            throw new java.lang.ExceptionInInitializerError(e);
        }
    }

    public Brain(Skynet skynet) {
        this.m_skynet = skynet;
    }

    public static Settings getMemory() {
        return Brain.m_memory;
    }

    public static boolean isTraining() {
        return Brain.m_isTraining;
    }

    public void life() {
        this.m_skynet.getEye().scan(true);
        this.m_skynet.getLeg().flight();
    }

    public void sendProgress(Object mission) {
        this.update(this, mission);
        this.setChanged();
        this.notifyObservers(mission);
    }

    @Override
    public void update(Observable o, Object arg) {
        if (((arg instanceof Leg.MovementDone) || (arg instanceof Fist.BulletFired))
                && !this.m_skynet.getFist().isAiming()) {
            this.m_skynet.getEye().scan(false);
            this.m_skynet.getLeg().flight();
        } else if (arg instanceof Eye.RobotNearby && !this.m_skynet.getFist().isAiming()) {
            Enemy enemy = ((Eye.RobotNearby) arg).getRobot();

            this.m_skynet.getLeg().stop();
            if (!this.m_skynet.getFist().aim(enemy)) {
                this.m_skynet.getEye().scan(false);
                this.m_skynet.getLeg().flight();
            }
        }
    }
}