package org.jboss.demo.artemis.ev3.robot;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.demo.artemis.ev3.Command;
import org.jboss.demo.artemis.ev3.RobotState;

import lejos.hardware.BrickFinder;
import lejos.hardware.ev3.EV3;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.motor.EV3MediumRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.Port;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3IRSensor;
import lejos.robotics.SampleProvider;
import lejos.robotics.chassis.Wheel;
import lejos.robotics.chassis.WheeledChassis;
import lejos.robotics.navigation.MovePilot;

public class Robot {
	private static final int ROBOT_VELOCITY = 20;
	private static final int HAND_MOTOR_ROTATE = 720;
	private static final double WHEEL_DIAMETER = 40;
	private static final double TRACK_DISTANCE_FROM_CENTER = 73;

	private static final Port PORT_DISTANCE = SensorPort.S4;
	private static final Port PORT_MOTOR_LEFT = MotorPort.B;
	private static final Port PORT_MOTOR_RIGHT = MotorPort.C;
	private static final Port PORT_MOTOR_HAND = MotorPort.A;

	private final EV3 ev3;
	private final MovePilot pilot;
	private final EV3IRSensor irSensor;
	private final EV3MediumRegulatedMotor handMotor;
	private final SampleProvider irData;
	boolean advertisedRobotState = false;
	private AtomicBoolean quitProgram = new AtomicBoolean(false);

	public Robot() {
		ev3 = (EV3) BrickFinder.getLocal();

		// Describe Snatch3r's movement system
		pilot = new MovePilot(
				new WheeledChassis(
						new Wheel[] {
								WheeledChassis.modelWheel(new EV3LargeRegulatedMotor(PORT_MOTOR_LEFT),
										WHEEL_DIAMETER).offset(-TRACK_DISTANCE_FROM_CENTER),
								WheeledChassis.modelWheel(new EV3LargeRegulatedMotor(PORT_MOTOR_RIGHT),
										WHEEL_DIAMETER).offset(TRACK_DISTANCE_FROM_CENTER) },
						WheeledChassis.TYPE_DIFFERENTIAL));
		pilot.setLinearSpeed(ROBOT_VELOCITY);
		
		irSensor = new EV3IRSensor(PORT_DISTANCE);
		irData = irSensor.getDistanceMode();

		handMotor = new EV3MediumRegulatedMotor(PORT_MOTOR_HAND);
	}

	public void run(final String brokerAddress) {
		try (MQTTBroker broker = new MQTTBroker(brokerAddress, "robot");) {
			final float[] distances = new float[3];
			broker.listenForMessages(this::executeComand, null, null);
			ev3.getLED().setPattern(1);
			advertiseRobotState(broker, true);
			while (!ev3.getKey("Escape").isDown() && !quitProgram.get()) {
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
		irSensor.close();
	}

	private void advertiseRobotState(MQTTBroker broker, boolean forced) {
		boolean isMoving = pilot.isMoving();
		if (forced || isMoving != advertisedRobotState) {
			broker.sendRobotState(isMoving ? RobotState.RUNNING : RobotState.STOPPED);
			advertisedRobotState = isMoving;
		}
	}

	private void executeComand(final String command) {
		switch (Command.valueOf(command)) {
			case STOP:
				pilot.stop();
				break;
			case FORWARD:
				pilot.forward();
				break;
			case BACKWARD:
				pilot.backward();
				break;
			case LEFT:
				pilot.rotate(-90);
				break;
			case RIGHT:
				pilot.rotate(90);
				break;
			case GRAB:
				grabHand();
				break;
			case RELEASE:
				releaseHand();
				break;
			case QUIT:
				quitProgram.set(true);
		}
	}

	private void grabHand() {
		handMotor.rotate(HAND_MOTOR_ROTATE);
	}

	private void releaseHand() {
		handMotor.rotate(-HAND_MOTOR_ROTATE);
	}

	public static void main(final String[] args) {
		Robot robot = new Robot();
		if (args.length != 0) {
			robot.run(args[0]);
		} else {
			robot.run("10.0.1.11");
		}
	}
}
