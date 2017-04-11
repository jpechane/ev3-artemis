package org.jboss.demo.artemis.ev3;

import java.io.Closeable;
import java.io.IOException;
import java.util.function.Consumer;

public interface Broker extends Closeable {
	public static final int ARTEMIS_MULTIPLEXED_PORT = 61616;

	public static final String AMQP_COMMAND_TOPIC = "ev3.Commands";
	public static final String MQTT_COMMAND_TOPIC = "ev3/Commands";
	public static final String JMS_COMMAND_TOPIC = "topic/ev3/Commands";

	public static final String AMQP_DISTANCE_TOPIC = "ev3.Sensor.Distance";
	public static final String MQTT_DISTANCE_TOPIC = "ev3/Sensor/Distance";
	public static final String JMS_DISTANCE_TOPIC = "topic/ev3/Sensor/Distance";

	public static final String AMQP_STATE_TOPIC = "ev3.State";
	public static final String MQTT_STATE_TOPIC = "ev3/State";
	public static final String JMS_STATE_TOPIC = "topic/ev3/State";

	public void listenForMessages(final Consumer<String> command, final Consumer<String> distance, final Consumer<String> state);

	public void sendCommand(Command cmd);

	public void close() throws IOException;

}