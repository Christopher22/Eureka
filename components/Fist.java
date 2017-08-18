package skynet.components;

import java.awt.geom.Point2D;
import java.util.Observable;

import robocode.*;
import robocode.util.Utils;

import skynet.helper.Enemy;
import skynet.Skynet;

public class Fist extends Component {

    public static class BulletFired {
        private Bullet m_bullet;

        public BulletFired(Bullet bullet) {
            this.m_bullet = bullet;
        }

        public Bullet getBullet() {
            return this.m_bullet;
        }
    }

    private Enemy m_currentTarget;
    private double m_currentFirePower;

    public Fist(Skynet skynet) {
        super(skynet);
        this.m_currentTarget = null;

        this.skynet.setAdjustRadarForGunTurn(true);
    }

    public void aim(Enemy target) {
        m_currentTarget = target;

        double distance = target.lastContact().getDistance();
        this.m_currentFirePower = Math.min(500 / distance, 3);
		long time = (long)(distance / (20 - this.m_currentFirePower * 3));
        //Point2D.Double prediction = target.predictPosition(this.skynet.getTime() + time);
        Point2D.Double prediction = target.predictPosition(time);

        this.skynet.out.printf("Distance: %f\n", distance);
        this.skynet.out.printf("Last contact: %f, %f\n", target.lastContact().getAbsolutPosition().x, target.lastContact().getAbsolutPosition().y);
        this.skynet.out.printf("Prediction: %f, %f\n", prediction.getX(), prediction.getY());

        //Point2D.Double prediction = target.lastContact().getAbsolutPosition();
        double absDeg = absoluteBearing(this.skynet.getX(), this.skynet.getY(), prediction.getX(), prediction.getY());

        this.skynet.setTurnGunRight(Utils.normalRelativeAngleDegrees(absDeg - this.skynet.getGunHeading()));
        this.skynet.addCustomEvent(new GunTurnCompleteCondition(this.skynet));
        this.skynet.execute();
    }

    public boolean isAiming() {
        return m_currentTarget != null;
    }
    @Override public void update(Observable o, Object arg) {
        if (arg instanceof GunTurnCompleteCondition) {
          Bullet b = this.skynet.fireBullet(this.m_currentFirePower);
          m_currentTarget = null;

          this.setChanged();
          this.notifyObservers(new BulletFired(b));
        }
      }

    private double absoluteBearing(double x1, double y1, double x2, double y2) {
        // Math stolen from http://mark.random-article.com/robocode/lessons/PredictiveShooter.java
		double xo = x2-x1;
		double yo = y2-y1;
		double hyp = Point2D.distance(x1, y1, x2, y2);
		double arcSin = Math.toDegrees(Math.asin(xo / hyp));
		double bearing = 0;

		if (xo > 0 && yo > 0) { // both pos: lower-Left
			bearing = arcSin;
		} else if (xo < 0 && yo > 0) { // x neg, y pos: lower-right
			bearing = 360 + arcSin; // arcsin is negative here, actually 360 - ang
		} else if (xo > 0 && yo < 0) { // x pos, y neg: upper-left
			bearing = 180 - arcSin;
		} else if (xo < 0 && yo < 0) { // both neg: upper-right
			bearing = 180 - arcSin; // arcsin is negative here, actually 180 + ang
		}

		return bearing;
	}
}