package skynet.components;

import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Observable;

import robocode.*;
import robocode.util.Utils;

import skynet.Skynet;
import skynet.helper.Enemy;

/**
 * MyClass - a class by (your name here)
 */
public class Eye extends Component {

	public static class RobotFound extends robocode.Condition {
		private ScannedRobotEvent m_robot;

		public RobotFound(ScannedRobotEvent robot) {
			super("RobotFound", 0);
			this.m_robot = robot;
		}

		public ScannedRobotEvent getRobot() {
			return this.m_robot;
		}

		@Override public boolean test() {
			return true;
		}
	}

	public static class RobotNearby {
		public final static int THRESHOLD = 200;

		private Enemy m_enemy;
		
		public RobotNearby(Enemy robot) {
			this.m_enemy = robot;
		}

		public Enemy getEnemy() {
			return this.m_enemy;
		}
	}

	public class CurrentEnemyIterator implements Iterator<Enemy>, Iterable<Enemy> {
		final long CURRENT = 15;

		private Iterator<Enemy> enemies;
		private Enemy nextEnemy;
		private long current;

		public CurrentEnemyIterator(Iterator<Enemy> enemies, long currentTurn) {
			this.enemies = enemies;
		}

		public boolean hasNext() {
			if (this.nextEnemy != null) {
				return true;
			} else {
				while (this.nextEnemy == null && this.enemies.hasNext()) {
					Enemy e = this.enemies.next();
					if (e.lastContact().getTurn() >= this.current - CURRENT) {
						this.nextEnemy = e;
						return true;
					}
				}
				return false;
			}
		}

		public Enemy next() {
			if (this.nextEnemy != null || this.hasNext()) {
				Enemy tmp = this.nextEnemy;
				this.nextEnemy = null;
				return tmp;
			} else {
				throw new NoSuchElementException();
			}
		}

		public Iterator<Enemy> iterator() {
			return this;
		}
	}

	private HashMap<String, Enemy> enemies;

	public Eye(Skynet skynet) {
		super(skynet);
		this.skynet.setAdjustRadarForRobotTurn(true);

		this.enemies = new HashMap<String, Enemy>();
	}

	@Override public void update(Observable o, Object arg) {
		if (arg instanceof RobotFound) {
			ScannedRobotEvent enemy = ((RobotFound)arg).getRobot();
			Enemy e = this.enemies.get(enemy.getName());
			if (e != null) {
				e.addContact(this.skynet, enemy);
			} else {
				e = new Enemy(this.skynet, enemy);
				this.enemies.put(enemy.getName(), e);
			}

			if(e.lastContact().getDistance() < RobotNearby.THRESHOLD) {
				this.setChanged();
				this.notifyObservers(new RobotNearby(e));
			}
		}
	}

	public Enemy getEnemy(String name) {
		return this.enemies.get(name);
	}
	
	public void focus(Enemy enemy) {

	}

	public void scan() {
		this.skynet.setTurnRadarLeft(Double.POSITIVE_INFINITY);
		this.skynet.execute();
	}

	public CurrentEnemyIterator getCurrentEnemies() {
		return new CurrentEnemyIterator(this.enemies.values().iterator(), this.skynet.getTime());
	}
}
