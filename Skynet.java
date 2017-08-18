package skynet;

import skynet.components.*;

import java.util.Observable;

import robocode.*;

/**
 * Skynet - a robot by (your name here)
 */
public class Skynet extends AdvancedRobot {
	private Eye eye;
	private Leg leg;
	private Fist fist;
	private Brain m_brain;

	public void run() {
		this.m_brain = new Brain(this);
		
		this.eye = new Eye(this);
		this.leg = new Leg(this);
		this.fist = new Fist(this);
		
		m_brain.life();
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		this.m_brain.sendProgress(new Eye.RobotFound(e));
	}

	public void onCustomEvent(CustomEvent event) {
		this.m_brain.sendProgress(event.getCondition());
		this.removeCustomEvent(event.getCondition());
	}

	public Eye getEye() {
		return this.eye;
	}

	public Leg getLeg() {
		return this.leg;
	}

	public Fist getFist() {
		return this.fist;
	}

	public Brain getBrain() {
		return this.m_brain;
	}
}
