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

    public static class Move implements Signal.Command {
    }

    public static class Stop implements Signal.Command {
    }

    public static class Scan implements Signal.Command {
    }

    public static class Attack implements Signal.Command {
        private final Enemy m_target;
        
        public Attack(final Enemy target) {
            this.m_target = target;
        }

        public Enemy getEnemy() {
            return this.m_target;
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
        this.setChanged();
        this.notifyObservers(mission);
    }

    @Override
    public void update(Observable o, Object arg) {
        if(!(arg instanceof Signal)) {
            throw new IllegalArgumentException("Signal expected");
        }

        // Print current signal to the console
        this.m_skynet.out.format("[%s] %s\n", arg instanceof Signal.Command ? "Command" : "Event", arg.getClass().getSimpleName());

        if (((arg instanceof Leg.MovementDone) || (arg instanceof Fist.BulletFired)) || (arg instanceof Fist.AimAborted)  && !this.m_skynet.getFist().isAiming()) {
            this.sendSignal(new Brain.Move());
            this.m_skynet.execute();
        } else if (arg instanceof Eye.RobotNearby && !this.m_skynet.getFist().isAiming()) {
            this.sendSignal(new Brain.Stop());
            this.sendSignal(new Brain.Attack(((Eye.RobotNearby) arg).getRobot()));
            this.m_skynet.execute();
        } else if(arg instanceof Eye.ScanningComplete) {
            this.sendSignal(new Brain.Scan());
            if(!this.m_skynet.getLeg().isMoving()) {
                this.sendSignal(new Brain.Move());
            }
            this.m_skynet.execute();
        }

        if(arg instanceof Signal.Event) {
            this.sendSignal((Signal.Event)arg);
        }
    }
}