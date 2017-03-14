package org.jboss.demo.artemis.ev3;

import java.io.IOException;

import org.junit.Test;

/**
 * Unit test for sending a IR distance sensor value.
 */
public class SensorSenderTest {

	public SensorSenderTest() {
	}

	/**
	 * Test sending a message
	 * @throws IOException 
	 */
	@Test
	public void commands() throws IOException {
		try (Broker c = new Broker("localhost", "robot")) {
			c.listenForMessages(System.out::println, null, null);
			try {
				c.sendCommand(Command.FORWARD);
				c.sendCommand(Command.LEFT);
				c.sendCommand(Command.STOP);
				c.sendObstacleDistance(300);
				c.sendRobotState(RobotState.RUNNING);
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
