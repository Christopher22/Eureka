package skynet.components;

import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Observable;
import java.awt.Graphics2D;

import robocode.util.Utils;

import skynet.Skynet;
import skynet.Brain;
import skynet.helper.*;
import skynet.config.*;

/**
 * The eye - tracks mutiple enemies and scan the environment.
 */
public class Eye extends Component {

	/**
	 * An event which is fired when a robot was found on the map.
	 */
	public static class RobotFound implements Event {
		private robocode.ScannedRobotEvent m_robot;

		/**
		 * Constructs a new event from a 'ScannedRobotEvent'.
		 */
		public RobotFound(robocode.ScannedRobotEvent robot) {
			this.m_robot = robot;
		}

		/** 
		 * Returns the scanned robot.
		 * @return The scanned robot.
		*/
		public robocode.ScannedRobotEvent getRobot() {
			return this.m_robot;
		}
	}

	/**
	 * An event which is fired if a robot is nearby.
	 */
	public static class RobotNearby implements Event {

		/**
		 * The threshold in which a robot is classified as "nearby".
		 */
		public final static int THRESHOLD = (int)Brain.getMemory().getValue("Eye/NearbyThreshold", new Range(100, 50, 200, 10));

		private Enemy m_robot;

		/**
		 * Constructs a new event.
		 */
		public RobotNearby(Enemy robot) {
			this.m_robot = robot;
		}

		/** 
		 * Returns the robot nearby.
		 * @return The nearby robot.
		*/
		public Enemy getRobot() {
			return this.m_robot;
		}
	}

	/**
	 * An iterator over all the enemies with more or less up-to-date information.
	 */
	public class CurrentEnemyIterator implements Iterator<Enemy>, Iterable<Enemy> {

		/**
		 * The threshold, after which information are classified as outdated.
		 */
		final static long TURN_THRESHOLD = 8;

		private Iterator<Enemy> m_enemies;
		private Enemy m_nextEnemy;
		private long m_current;

		/**
		 * Creates the iterator.
		 */
		protected CurrentEnemyIterator(Iterator<Enemy> enemies, long currentTurn) {
			this.m_enemies = enemies;
		}

		/**
		 * Check for next element.
		 * @return True, if there is still a new element.
		 */
		public boolean hasNext() {
			if (this.m_nextEnemy != null) {
				return true;
			} else {
				while (this.m_nextEnemy == null && this.m_enemies.hasNext()) {
					Enemy e = this.m_enemies.next();
					if (e.lastContact().getTurn() >= this.m_current - TURN_THRESHOLD) {
						this.m_nextEnemy = e;
						return true;
					}
				}
				return false;
			}
		}

		/**
		 * Returns the next element.
		 * @return Next up-to-date enemy.
		 * @thows NoSuchElementException if no element is present.
		 */
		public Enemy next() {
			if (this.m_nextEnemy != null || this.hasNext()) {
				Enemy tmp = this.m_nextEnemy;
				this.m_nextEnemy = null;
				return tmp;
			} else {
				throw new NoSuchElementException();
			}
		}

		/**
		 * Returns an iterator for an convinient usage in a foreach loop.
		 */
		public Iterator<Enemy> iterator() {
			return this;
		}
	}

	private HashMap<String, Enemy> m_enemies;

	/**
	 * Create the eye.
	 */
	public Eye(Skynet skynet) {
		super(skynet);
		this.skynet.setAdjustRadarForRobotTurn(true);

		this.m_enemies = new HashMap<String, Enemy>();
	}

	/**
	 * Handle incoming events.
	 */
	@Override
	public void update(Observable o, Object arg) {
		if (arg instanceof RobotFound) {
			robocode.ScannedRobotEvent enemy = ((RobotFound) arg).getRobot();

			// Add enemy to queue, if not already happen.
			Enemy e = this.m_enemies.get(enemy.getName());
			if (e != null) {
				e.addContact(this.skynet, enemy);
			} else {
				e = new Enemy(this.skynet, enemy);
				this.m_enemies.put(enemy.getName(), e);
			}

			if (e.lastContact().getDistance() < RobotNearby.THRESHOLD) {
				this.setChanged();
				this.notifyObservers(new RobotNearby(e));
			}
		}
	}

	/**
	 * Returns an enemy by its name.
	 * @return the Enemy or null elsewise.
	 */
	public Enemy getEnemy(String name) {
		return this.m_enemies.get(name);
	}

	public double getHeading() {
		return this.skynet.getRadarHeading();
	}
	
	/**
	 * Scans for other robots.
	 */
	public void scan(boolean blocking) {
		if(blocking) {
			this.skynet.setTurnRadarLeft(360);
			this.skynet.waitFor(new robocode.RadarTurnCompleteCondition(this.skynet));
			this.skynet.execute();
		}
		this.skynet.setTurnRadarLeft(Double.POSITIVE_INFINITY);
		this.skynet.execute();
	}

	/**
	 * Returns a iterator about current enemies.
	 * @return The iterator over the enemies.
	 */
	public CurrentEnemyIterator getCurrentEnemies() {
		return new CurrentEnemyIterator(this.m_enemies.values().iterator(), this.skynet.getTime());
	}

	@Override
	public void drawDebug(Graphics2D g) {
		g.setColor(java.awt.Color.GREEN);
		for (Enemy enemy :this.getCurrentEnemies()) {
			g.fillOval((int) enemy.lastContact().getAbsolutPosition().getX() - 8, (int) enemy.lastContact().getAbsolutPosition().getY() - 8, 8, 8);
		}
	}
}
