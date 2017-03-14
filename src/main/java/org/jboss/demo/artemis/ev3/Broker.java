package org.jboss.demo.artemis.ev3;

import java.io.Closeable;
import java.io.IOException;
import java.util.UUID;
import java.util.function.Consumer;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class Broker implements Closeable {
	private static final int QOS_AT_MOST_ONCE = 0;
	private static final int QOS_EXACTLY_ONCE = 2;
	public static final String COMMAND_TOPIC = "jms/topic/ev3/Commands";
	public static final String DISTANCE_TOPIC = "jms/topic/ev3/Sensor/Distance";
	public static final String STATE_TOPIC = "jms/topic/ev3/State";
	public static final String CLIENT_ID = "LegoEV3";

	private final MqttClient client;

	public Broker(final String brokerHost, final String role) {
		try {
			String url = "tcp://" + brokerHost + ":1883";
			client = new MqttClient(url, CLIENT_ID + "-" + role + "-" + UUID.randomUUID(), new MemoryPersistence());
			MqttConnectOptions options = new MqttConnectOptions();
			options.setCleanSession(true);
			// options.setUserName("guest");
			// options.setPassword("guest".toCharArray());
			client.connect(options);
		} catch (MqttException e) {
			throw new IllegalStateException("Cannot connect to MQTT broker", e);
		}
	}

	public void listenForMessages(final Consumer<String> command, final Consumer<String> distance,
			final Consumer<String> state) {
		try {
			client.setCallback(new MqttCallback() {
				@Override
				public void messageArrived(final String topic, final MqttMessage message) throws Exception {
					System.out.println("Arrived " + message + " for topic " + topic);
					if (topic.contains(COMMAND_TOPIC) && command != null) {
						command.accept(new String(message.getPayload()));
					} else if (topic.contains(DISTANCE_TOPIC) && distance != null) {
						distance.accept(new String(message.getPayload()));
					} else if (topic.contains(STATE_TOPIC) && state != null) {
						state.accept(new String(message.getPayload()));
					}
				}

				@Override
				public void deliveryComplete(final IMqttDeliveryToken token) {
				}

				@Override
				public void connectionLost(final Throwable e) {
				}
			});
			client.subscribe("jms/topic/ev3/#");
		} catch (MqttException e) {
			throw new IllegalStateException("Could not subscribe to MQTT broker", e);
		}
	}

	public void sendMessage(String text, String destination) {
		MqttMessage message = new MqttMessage(text.getBytes());
		message.setQos(QOS_AT_MOST_ONCE);
		try {
			client.publish(destination, message);
		} catch (MqttException e) {
			throw new IllegalStateException("Could not send message to MQTT broker", e);
		}
	}

	public void sendCommand(Command cmd) {
		sendMessage(cmd.name(), COMMAND_TOPIC);
	}

	public void sendObstacleDistance(final int distance) {
		sendMessage(Integer.toString(distance), DISTANCE_TOPIC);
	}

	public void sendRobotState(final RobotState state) {
		sendMessage(state.name(), STATE_TOPIC);
	}

	public void close() throws IOException {
		try {
			client.disconnect();
		} catch (MqttException e) {
			throw new IOException("MQTT connection dissconnect failed", e);
		}
	}
}
