package Tourao;

import robocode.*;
import robocode.util.Utils;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.HashMap;

public class Tourao extends AdvancedRobot {
    private HashMap<String, Enemy> enemies = new HashMap<>();
    private Enemy currentTarget = null;
    private int moveDirection = 1;

    public void run() {
        setBodyColor(new Color(139, 69, 19));
        setGunColor(new Color(101, 67, 33));
        setRadarColor(new Color(160, 82, 45));
        setBulletColor(new Color(205, 133, 63));
        setScanColor(new Color(222, 184, 135));

        setAdjustRadarForGunTurn(true);
        setAdjustGunForRobotTurn(true);

        while (true) {
            if (currentTarget != null) {
                setTurnRadarRight(Utils.normalRelativeAngleDegrees(
                        getHeading() - getRadarHeading() + currentTarget.bearing
                ));
            } else {
                setTurnRadarRight(360);
            }
            doMovement();
            execute();
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        Enemy enemy = enemies.computeIfAbsent(e.getName(), k -> new Enemy());
        enemy.update(e, this);
        double energyDrop = enemy.lastEnergy - enemy.energy;
        if (energyDrop > 0 && energyDrop <= 3.0) {
            moveDirection *= -1;
            setAhead(150 * moveDirection);
        }
        enemy.lastEnergy = enemy.energy;
        currentTarget = enemy;
        aimAndFire(enemy);
    }

    public void onHitByBullet(HitByBulletEvent e) {
        moveDirection *= -1;
        setAhead(100 * moveDirection);
    }

    public void onHitWall(HitWallEvent e) {
        moveDirection *= -1;
        setAhead(100 * moveDirection);
    }

    private void doMovement() {
        if (currentTarget != null) {
            setTurnRight(currentTarget.bearing + 90 - (15 * moveDirection));
            setAhead(100 * moveDirection);
        } else {
            setTurnRight(10);
            setAhead(50);
        }
    }

    private void aimAndFire(Enemy enemy) {
        double bulletPower = Math.min(3.0, getEnergy() / 8);
        double bulletSpeed = 20 - 3 * bulletPower;
        double absBearing = Math.toRadians(getHeading() + enemy.bearing);
        double enemyX = getX() + enemy.distance * Math.sin(absBearing);
        double enemyY = getY() + enemy.distance * Math.cos(absBearing);
        double enemyHeading = Math.toRadians(enemy.heading);
        double enemyVelocity = enemy.velocity;
        double deltaTime = 0;
        double predictedX = enemyX;
        double predictedY = enemyY;
        while ((++deltaTime) * bulletSpeed <
                Point2D.distance(getX(), getY(), predictedX, predictedY)) {
            predictedX += Math.sin(enemyHeading) * enemyVelocity;
            predictedY += Math.cos(enemyHeading) * enemyVelocity;
            predictedX = Math.max(Math.min(predictedX, getBattleFieldWidth() - 18), 18);
            predictedY = Math.max(Math.min(predictedY, getBattleFieldHeight() - 18), 18);
        }
        double theta = Math.toDegrees(Math.atan2(predictedX - getX(), predictedY - getY()));
        setTurnGunRight(Utils.normalRelativeAngleDegrees(theta - getGunHeading()));
        if (getGunHeat() == 0 && Math.abs(Utils.normalRelativeAngleDegrees(theta - getGunHeading())) < 10) {
            setFire(bulletPower);
        }
    }

    class Enemy {
        double bearing, distance, energy, heading, velocity, lateralVelocity;
        double lastEnergy = 100;

        void update(ScannedRobotEvent e, AdvancedRobot robot) {
            this.bearing = e.getBearing();
            this.distance = e.getDistance();
            this.energy = e.getEnergy();
            this.heading = e.getHeading();
            this.velocity = e.getVelocity();
            this.lateralVelocity = e.getVelocity() *
                    Math.sin(Math.toRadians(e.getHeading() - robot.getHeading()));
        }
    }
}
