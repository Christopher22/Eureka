package skynet;

import java.util.Observable;
import java.util.Observer;
import java.io.File;

import skynet.components.*;
import skynet.helper.*;
import skynet.config.*;

public class Brain extends Observable implements Observer {
    private Skynet m_skynet;
    private Memory<Parameter> m_memory;
    private boolean m_isTraining;

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