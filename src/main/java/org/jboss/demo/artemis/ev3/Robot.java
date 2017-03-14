package org.jboss.demo.artemis.ev3;

import java.io.IOException;

import lejos.hardware.BrickFinder;
import lejos.hardware.ev3.EV3;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.sensor.EV3IRSensor;
import lejos.robotics.SampleProvider;
import lejos.robotics.navigation.DifferentialPilot;

/**
 * Hello from EV3
 *
 */
public class Robot {
	private static final double WHEEL_DIAMETER = 43.2;
	private static final double TRACK_WIDTH = 146;

	private final EV3 ev3;
	private final DifferentialPilot pilot;
	private final EV3IRSensor irSensor;
	private final SampleProvider irData;
	boolean advertisedRobotState = false;

	public Robot() {
		ev3 = (EV3) BrickFinder.getDefault();
		pilot = new DifferentialPilot(WHEEL_DIAMETER, TRACK_WIDTH, new EV3LargeRegulatedMotor(ev3.getPort("A")),
				new EV3LargeRegulatedMotor(ev3.getPort("C")));
		irSensor = new EV3IRSensor(ev3.getPort("S4"));
		irData = irSensor.getDistanceMode();
	}

	public void run(String brokerAddress) {
		try (Broker broker = new Broker(brokerAddress, "robot");) {
			final float[] distances = new float[3];
			broker.listenForMessages(this::executeComand, null, null);
			ev3.getLED().setPattern(1);
			advertiseRobotState(broker, true);
			while (!ev3.getKey("Escape").isDown()) {
				irData.fetchSample(distances, 0);
				float averageDistance = 0;
				for (int i = 0; i < distances.length; i++) {
					averageDistance += distances[i];
				}
				averageDistance /= distances.length;
				broker.sendObstacleDistance((int) averageDistance);
				if (pilot.isMoving()) {

				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		ev3.getLED().setPattern(0);
	}

	private void advertiseRobotState(Broker broker, boolean forced) {
		boolean isMoving = pilot.isMoving();
		if (forced || isMoving != advertisedRobotState) {
			broker.sendRobotState(isMoving ? RobotState.RUNNING : RobotState.STOPPED);
			advertisedRobotState = isMoving;
		}
	}

	private void executeComand(String command) {
		if (Command.STOP.name().equals(command)) {
			pilot.stop();
		} else if (Command.FORWARD.name().equals(command)) {
			pilot.forward();
		} else if (Command.LEFT.name().equals(command)) {
			pilot.rotate(90);
		} else if (Command.RIGHT.name().equals(command)) {
			pilot.rotate(-90);
		}
	}

	public static void main(String[] args) {
		Robot robot = new Robot();
		if (args.length != 0) {
			robot.run(args[0]);
		} else {
			robot.run("10.0.1.11");
		}
	}
}
