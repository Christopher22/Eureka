package skynet;

import skynet.components.*;
import skynet.helper.*;

import java.util.Observable;
import java.util.Observer;

public class Brain extends Observable implements Observer {
    private Skynet m_skynet;

    public Brain(Skynet skynet) {
        super();
        this.m_skynet = skynet;
    }

    public void life() {
        this.m_skynet.getEye().scan();
		this.m_skynet.getLeg().fly();
    }

    public void sendProgress(Object mission) {
        this.update(this, mission);
        this.setChanged();
        this.notifyObservers(mission);
    }

    @Override public void update(Observable o, Object arg) {
        if((arg instanceof Leg.MovementDone) || (arg instanceof Fist.BulletFired)) {
            this.m_skynet.getEye().scan();
            this.m_skynet.getLeg().fly();
        }
        /*} else if(arg instanceof Eye.RobotNearby && !this.m_skynet.getFist().isAiming()) {
            Enemy enemy = ((Eye.RobotNearby)arg).getEnemy();

            this.m_skynet.getLeg().stop();
            this.m_skynet.getFist().aim(enemy);
        }*/
    }
}