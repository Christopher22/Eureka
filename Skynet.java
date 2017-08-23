package skynet;

import java.awt.geom.Point2D;
import java.util.Observable;

import java.awt.Color;
import java.awt.Graphics2D;

import robocode.*;

import skynet.components.*;
import skynet.helper.Enemy;
import skynet.helper.Signal;

/**
 * Skynet - a robot by (your name here)
 */
public class Skynet extends AdvancedRobot {

	public static class BattleEnded implements Signal.GlobalEvent {
	}

	public static class RoundEnded implements Signal.GlobalEvent {
		private int m_turns;

		public RoundEnded(int turns) {
			this.m_turns = turns;
		}

		public int getTurns() {
			return this.m_turns;
		}
	}

	public static class EnemyDied implements Signal.GlobalEvent {
		private RobotDeathEvent m_enemy;

		public EnemyDied(RobotDeathEvent enemy) {
			this.m_enemy = enemy;
		}

		public RobotDeathEvent getEnemy() {
			return this.m_enemy;
		}
	}

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

	@Override
	public void onPaint(Graphics2D g) {
		this.eye.drawDebug(g);
		this.leg.drawDebug(g);
		this.fist.drawDebug(g);
	}

	@Override
	public void onScannedRobot(ScannedRobotEvent e) {
		this.m_brain.update(null, new Eye.RobotFound(e));
	}

	@Override
	public void onHitRobot(HitRobotEvent e) {
		this.m_brain.update(null, new Leg.RobotHit(e));
	}

	@Override
	public void onCustomEvent(CustomEvent event) {
		this.m_brain.update(null, new Signal.CustomEvent(event.getCondition()));
		this.removeCustomEvent(event.getCondition());
	}

	@Override
	public void onBattleEnded(BattleEndedEvent event) {
		this.m_brain.update(null, new Skynet.BattleEnded());
	}

	@Override
	public void onRoundEnded(RoundEndedEvent event) {
		this.m_brain.update(null, new Skynet.RoundEnded(event.getTurns()));
	}

	@Override
	public void onRobotDeath(RobotDeathEvent event) {
		this.m_brain.update(null, new Skynet.EnemyDied(event));
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
