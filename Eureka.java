package eureka;

import java.awt.geom.Point2D;
import java.util.Observable;
import java.awt.Color;
import java.awt.Graphics2D;

import robocode.*;

import eureka.components.*;
import eureka.helper.Enemy;
import eureka.helper.Signal;

/**
 * Eureka - a robot by Christopher Gundler.
 */
public class Eureka extends AdvancedRobot {

	/**
	 * A signal which is fired if the battle ends.
	 */
	public static class BattleEnded implements Signal.GlobalEvent {
	}

	/**
	 * A signal which is fired if the round ends.
	 */
	public static class RoundEnded implements Signal.GlobalEvent {
		private final int m_turns;

		/**
		 * Creates a new event.
		 * @param turns The overall number of turns.
		 */
		public RoundEnded(final int turns) {
			this.m_turns = turns;
		}

		/**
		 * Returns the overall number of turns.
		 * @return the overall number of turns.
		 */
		public int getTurns() {
			return this.m_turns;
		}
	}

	/**
	 * An event which is spawned if an enemy died.
	 */
	public static class EnemyDied implements Signal.GlobalEvent {
		private final RobotDeathEvent m_enemy;

		/**
		 * Creates a new event.
		 * @param turns The dead enemy.
		 */
		public EnemyDied(final RobotDeathEvent enemy) {
			this.m_enemy = enemy;
		}

		/**
		 * Returns the enemy.
		 * @return the enemy.
		 */
		public RobotDeathEvent getEnemy() {
			return this.m_enemy;
		}
	}

	private Eye m_eye;
	private Leg m_leg;
	private Fist m_fist;
	private Brain m_brain;

	/**
	 * Runs the robot.
	 */
	public void run() {
		this.m_brain = new Brain(this);

		this.m_eye = new Eye(this);
		this.m_leg = new Leg(this);
		this.m_fist = new Fist(this);

		this.m_brain.life();
	}

	/**
	* Returns the position of the robot.
	* @return the position of the robot.
	*/
	public Point2D.Double getPosition() {
		return new Point2D.Double(this.getX(), this.getY());
	}

	@Override
	public void onPaint(Graphics2D g) {
		this.m_eye.drawDebug(g);
		this.m_leg.drawDebug(g);
		this.m_fist.drawDebug(g);
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
	}

	@Override
	public void onBattleEnded(BattleEndedEvent event) {
		this.m_brain.update(null, new Eureka.BattleEnded());
	}

	@Override
	public void onRoundEnded(RoundEndedEvent event) {
		this.m_brain.update(null, new Eureka.RoundEnded(event.getTurns()));
	}

	@Override
	public void onRobotDeath(RobotDeathEvent event) {
		this.m_brain.update(null, new Eureka.EnemyDied(event));
	}

	@Override
	public void onSkippedTurn(SkippedTurnEvent event) {
		this.out.println("[INFO] Turn skipped!");
	}

	/**
	 * Returns the eye of the robot.
	 * @return the eye of the robot.
	 */
	public Eye getEye() {
		return this.m_eye;
	}

	/**
	 * Returns the leg of the robot.
	 * @return the leg of the robot.
	 */
	public Leg getLeg() {
		return this.m_leg;
	}

	/**
	 * Returns the fist of the robot.
	 * @return the fist of the robot.
	 */
	public Fist getFist() {
		return this.m_fist;
	}

	/**
	 * Returns the brain of the robot.
	 * @return the brain of the robot.
	 */
	public Brain getBrain() {
		return this.m_brain;
	}
}
