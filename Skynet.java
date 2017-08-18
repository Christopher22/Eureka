package skynet;

import skynet.components.*;

import java.awt.geom.Point2D;
import java.util.Observable;

import java.awt.Color;
import java.awt.Graphics2D;

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

	public Point2D.Double getPosition() {
		return new Point2D.Double(this.getX(), this.getY());
	}
	 
	public void onPaint(Graphics2D g) {
		this.eye.drawDebug(g);
		this.leg.drawDebug(g);
		this.fist.drawDebug(g);
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
