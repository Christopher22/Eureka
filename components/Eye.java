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
	public static class RobotFound implements Signal.Event {
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
	public static class RobotNearby implements Signal.Event {

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

	/**ary
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

	public enum Direction {
		Right(1), Left(-1);

		private int m_direction;

		private Direction(int direction) {
			this.m_direction = direction;
		}

		public double getRotationToRight(double degrees) {
			return degrees * this.m_direction;
		}

		public Direction reverse() {
			 return (this == Direction.Right ? Direction.Left : Direction.Right);
		}
	}

	public static class ScanningComplete implements Signal.Event {
	}

	/**
	* The threshold in which a robot is classified as "nearby".
	*/
	public final int Threshold;

	private HashMap<String, Enemy> m_enemies;
	private boolean m_isTurningComplete;
	private Direction m_direction;

	/**
	 * Create the eye.
	 */
	public Eye(Skynet skynet) {
		super(skynet);
		this.skynet.setAdjustRadarForRobotTurn(true);
		//this.skynet.setAdjustRadarForGunTurn(false);

		this.Threshold = (int) skynet.getBrain().accessMemory("Eye/NearbyThreshold", new Range(150, 100, 300, 10));

		this.m_enemies = new HashMap<String, Enemy>();
		this.m_isTurningComplete = true;
		this.m_direction = Direction.Left;
	}

	/**
	 * Handle incoming events.
	 */
	@Override
	public void update(Observable o, Object arg) {
		if (arg instanceof Brain.Scan) {
			this.scan();
		} else if (arg instanceof RobotFound) {
			robocode.ScannedRobotEvent enemy = ((RobotFound) arg).getRobot();

			// Add enemy to queue, if not already happen.
			Enemy e = this.m_enemies.get(enemy.getName());
			if (e != null) {
				e.addContact(this.skynet, enemy);
			} else {
				e = new Enemy(this.skynet, enemy);
				this.m_enemies.put(enemy.getName(), e);
			}

			if (e.lastContact().getDistance() < this.Threshold) {
				this.sendSignal(new RobotNearby(e));
			}
		} else if (arg instanceof robocode.RadarTurnCompleteCondition) {
			if (this.m_isTurningComplete) {
				this.m_isTurningComplete = false;
			}
			this.sendSignal(new ScanningComplete());
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
	private void scan() {
		if (m_isTurningComplete || Utils.getRandom().nextInt(3) == 2) {
			this.skynet.setTurnRadarRight(360);
		} else {
			// Highly modified version from https://www.ibm.com/developerworks/library/j-radar/index.html
			double maxBearingAbs = 0, maxBearing = 0;
			int scannedBots = 0;
			for (Enemy enemy : this.getCurrentEnemies()) {
				double bearing = Utils.normalRelativeAngle(
						this.skynet.getHeading() + enemy.lastContact().getBearing() - this.skynet.getRadarHeading());
				if (Math.abs(bearing) > maxBearingAbs) {
					maxBearingAbs = Math.abs(bearing);
					maxBearing = bearing;
				}
				scannedBots++;
			}

			double radarTurn = this.m_direction.getRotationToRight(180);
			if (scannedBots == this.skynet.getOthers())
				radarTurn = maxBearing + Math.signum(maxBearing) * 22.5;

			this.skynet.setTurnRadarRight(radarTurn);
			this.m_direction = this.m_direction.reverse();
		}
		this.skynet.addCustomEvent(new robocode.RadarTurnCompleteCondition(this.skynet));
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
		for (Enemy enemy : this.getCurrentEnemies()) {
			g.fillOval((int) enemy.lastContact().getAbsolutPosition().getX() - 8,
					(int) enemy.lastContact().getAbsolutPosition().getY() - 8, 8, 8);
		}
	}
}
