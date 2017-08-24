package eureka.components;

import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Observable;
import java.io.File;
import java.awt.Graphics2D;
import java.io.Serializable;

import robocode.util.Utils;

import eureka.Eureka;
import eureka.Brain;
import eureka.helper.*;
import eureka.helper.Signal.CustomEvent;
import eureka.config.*;

/**
 * The eye - tracks multiple enemies, calculates their danger and scans the environment.
 */
public class Eye extends Component {

	/**
	 * An event which is fired when a robot was found on the map.
	 */
	public static class RobotFound implements Signal.GlobalEvent {
		private final robocode.ScannedRobotEvent m_robot;

		/**
		 * Constructs a new event from a 'ScannedRobotEvent'.
		 * @param robot The robot found.
		 */
		public RobotFound(final robocode.ScannedRobotEvent robot) {
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

		private final Enemy m_robot;

		/**
		 * Constructs a new event.
		 * @param robot The robot nearby.
		 */
		public RobotNearby(final Enemy robot) {
			this.m_robot = robot;
		}

		/** 
		 * Returns the robot nearby.
		 * @return the nearby robot.
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
		private final long m_current;

		/**
		 * Creates the iterator.
		 * @param enemies A list of possible enemies.
		 * @param currentTurn The current turn.
		 */
		protected CurrentEnemyIterator(final Iterator<Enemy> enemies, final long currentTurn) {
			this.m_enemies = enemies;
			this.m_current = currentTurn;
		}

		/**
		 * Checks for next element.
		 * @return True, if there is still a new element.
		 */
		public boolean hasNext() {
			if (this.m_nextEnemy != null) {
				return true;
			} else {
				while (this.m_nextEnemy == null && this.m_enemies.hasNext()) {
					Enemy e = this.m_enemies.next();
					if (e.isAlive() && e.lastContact().getTurn() >= this.m_current - TURN_THRESHOLD) {
						this.m_nextEnemy = e;
						return true;
					}
				}
				return false;
			}
		}

		/**
		 * Returns the next element.
		 * @return next up-to-date enemy.
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

	/**
	 * Represents the direction a radar may turn.
	 */
	public enum Direction {
		Right(1), Left(-1);

		private int m_direction;

		private Direction(int direction) {
			this.m_direction = direction;
		}

		/**
		 * Calculates the number of degrees which might be insert into the 'setTurn' function.
		 * @param degrees The degrees the radar should turn into the direction.
		 */
		public double getRotationToRight(double degrees) {
			return degrees * this.m_direction;
		}

		/**
		 * Returns the reversed direction.
		 * @return the reserved direction.
		 */
		public Direction reverse() {
			return (this == Direction.Right ? Direction.Left : Direction.Right);
		}
	}

	/**
	 * An event which is be fired if the scanning is complete.
	 */
	public static class ScanningComplete implements Signal.Event {
	}

	/**
	 * A calculator for the performance of an enemy on base of its survived turns.
	 */
	private static class EnemyPerformance implements Serializable {
		private double m_avg, m_number;

		/**
		 * Adds a normalized value.
		 */
		public void addValue(double value) {
			this.m_avg -= (this.m_avg - value) / ++m_number;
		}

		/**
		 * Returns the average of the performance.
		 */
		public double getAverage() {
			return this.m_avg;
		}
	}

	/**
	* The threshold in which a robot is classified as "nearby".
	*/
	public final int Threshold;

	public static String ENEMY_FILENAME = "enemy_scores.ser";

	private HashMap<String, Enemy> m_enemies;
	private Memory<EnemyPerformance> m_performance;
	private boolean m_isTurningComplete;
	private Direction m_direction;

	/**
	 * Create the eye.
	 */
	public Eye(final Eureka eureka) {
		super(eureka);
		this.eureka.setAdjustRadarForRobotTurn(true);

		this.Threshold = (int) eureka.getBrain().accessMemory("Eye/NearbyThreshold", new Range(200, 100, 300, 10));

		// Tries to load the performance of former seen robots from a file
		if ((this.m_performance = Memory.load(new File(eureka.getDataDirectory(), Eye.ENEMY_FILENAME))) == null) {
			m_performance = new Memory<>();
		}

		this.m_enemies = new HashMap<String, Enemy>();
		this.m_isTurningComplete = true;
		this.m_direction = Direction.Left;
	}

	@Override
	protected void handleOperationDone(CustomEvent event) {
		this.sendSignal(new ScanningComplete());
	}

	@Override
	protected void handleEvent(final Signal.Event event) {
		if (event instanceof RobotFound) {
			robocode.ScannedRobotEvent enemy = ((RobotFound) event).getRobot();

			// Add enemy to queue, if not already happen.
			Enemy e = this.m_enemies.get(enemy.getName());
			if (e != null) {
				e.addContact(this.eureka, enemy);
			} else {
				e = new Enemy(this.eureka, enemy);
				this.m_enemies.put(enemy.getName(), e);
			}

			// Checks the current distance of the new-seen enemy.
			if (e.lastContact().getDistance() < this.Threshold) {
				this.sendSignal(new RobotNearby(e));
			}
		} else if (event instanceof Eureka.EnemyDied) {
			// Save enemy as dead
			Enemy e = this.getEnemy(((Eureka.EnemyDied) event).getEnemy().getName());
			if (e != null) {
				// If enemy does not die unseen...
				e.setDeadTurn((int) this.eureka.getTime());
			}
		} else if (event instanceof Eureka.RoundEnded) {
			// Calculate the performance of every enemy after the round ends.
			final int turns = ((Eureka.RoundEnded) event).getTurns();
			for (Enemy e : this.m_enemies.values()) {
				EnemyPerformance performance = this.m_performance.getValue(e.getBaseName(), new EnemyPerformance());
				performance.addValue(e.isAlive() ? 1.0d : (e.getDeadTurn() / turns));
			}
		} else if (event instanceof Eureka.BattleEnded) {
			// Tries to save the heuristics for further use
			try {
				this.m_performance.save(new File(eureka.getDataDirectory(), Eye.ENEMY_FILENAME));
			} catch (Exception ex) {
				this.eureka.out.println("[ERROR] Saving failed");
			}
		}
	}

	@Override
	protected void handleCommand(final Signal.Command command) {
		if (command instanceof Brain.Scan) {
			this.scan();
		} else if (command instanceof Brain.Attack) {
			Enemy e = ((Brain.Attack) command).getEnemy();
			this.turnTo(this.eureka.getHeading() - this.eureka.getRadarHeading() + e.lastContact().getBearing());
		}
	}

	/**
	 * Returns an enemy by its name.
	 * @return the Enemy or null elsewise.
	 */
	public Enemy getEnemy(final String name) {
		return this.m_enemies.get(name);
	}

	/**
	 * Returns the heading of the radar.
	 * @return the heading of the radar.
	 */
	public double getHeading() {
		return this.eureka.getRadarHeading();
	}

	/**
	 * Scans for other robots.
	 */
	protected void scan() {
		// Turn full at the beginning or random during the battle
		if (m_isTurningComplete || Utils.getRandom().nextInt(3) == 2) {
			this.turnTo(360);
		} else {
			// Highly modified version from https://www.ibm.com/developerworks/library/j-radar/index.html
			double maxBearingAbs = 0, maxBearing = 0;
			int scannedBots = 0;
			for (Enemy enemy : this.getCurrentEnemies()) {
				double bearing = Utils.normalRelativeAngle(
						this.eureka.getHeading() - this.eureka.getRadarHeading() + enemy.lastContact().getBearing());
				if (Math.abs(bearing) > maxBearingAbs) {
					maxBearingAbs = Math.abs(bearing);
					maxBearing = bearing;
				}
				scannedBots++;
			}

			double radarTurn = this.m_direction.getRotationToRight(180);
			if (scannedBots == this.eureka.getOthers())
				radarTurn = maxBearing + Math.signum(maxBearing) * 22.5;

			this.m_direction = this.m_direction.reverse();
			this.turnTo(radarTurn);
		}
	}

	/**
	 * Turns the radar for a specific number of degrees.
	 * @param degrees The degrees to turn.
	 */
	protected void turnTo(final double degrees) {
		this.eureka.setTurnRadarRight(degrees);
		this.start(new robocode.RadarTurnCompleteCondition(this.eureka));
	}

	/**
	 * Returns a iterator about current enemies.
	 * @return The iterator over the enemies.
	 */
	public CurrentEnemyIterator getCurrentEnemies() {
		return new CurrentEnemyIterator(this.m_enemies.values().iterator(), this.eureka.getTime());
	}

	@Override
	public void drawDebug(Graphics2D g) {
		g.setColor(java.awt.Color.GREEN);
		for (Enemy enemy : this.getCurrentEnemies()) {
			g.fillOval((int) enemy.lastContact().getX() - 8, (int) enemy.lastContact().getY() - 8, 8, 8);
		}
	}
}
