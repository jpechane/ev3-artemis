package org.jboss.demo.artemis.ev3.robot;

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
import org.jboss.demo.artemis.ev3.Broker;
import org.jboss.demo.artemis.ev3.Command;
import org.jboss.demo.artemis.ev3.RobotState;

public class MQTTBroker implements Broker {
	private static final String EV3_TOPICS_WILDCARD = "ev3/#";
	private static final int QOS_AT_MOST_ONCE = 0;
	public static final String CLIENT_ID = "LegoEV3";

	private final MqttClient client;

	public MQTTBroker(final String brokerHost, final String role) {
		try {
			final String url = String.format("tcp://%s:%s", brokerHost, ARTEMIS_MULTIPLEXED_PORT);
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

	@Override
	public void listenForMessages(final Consumer<String> command, final Consumer<String> distance,
			final Consumer<String> state) {
		try {
			client.setCallback(new MqttCallback() {
				@Override
				public void messageArrived(final String topic, final MqttMessage message) throws Exception {
					System.out.println("Arrived " + message + " for topic " + topic);
					if (topic.contains(MQTT_COMMAND_TOPIC) && command != null) {
						command.accept(new String(message.getPayload()));
					} else if (topic.contains(MQTT_DISTANCE_TOPIC) && distance != null) {
						distance.accept(new String(message.getPayload()));
					} else if (topic.contains(MQTT_STATE_TOPIC) && state != null) {
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
			client.subscribe(EV3_TOPICS_WILDCARD);
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

	@Override
	public void sendCommand(Command cmd) {
		sendMessage(cmd.name(), MQTT_COMMAND_TOPIC);
	}

	public void sendObstacleDistance(final int distance) {
		sendMessage(Integer.toString(distance), MQTT_DISTANCE_TOPIC);
	}

	public void sendRobotState(final RobotState state) {
		sendMessage(state.name(), MQTT_STATE_TOPIC);
	}

	@Override
	public void close() throws IOException {
		try {
			client.disconnect();
		} catch (MqttException e) {
			throw new IOException("MQTT connection disconnect failed", e);
		}
	}
}
