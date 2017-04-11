package org.jboss.demo.artemis.ev3.controller;

import java.io.IOException;
import java.util.Properties;
import java.util.function.Consumer;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.demo.artemis.ev3.Broker;
import org.jboss.demo.artemis.ev3.Command;
import org.jboss.demo.artemis.ev3.RobotState;

public class JMSBroker implements Broker {

	private final ConnectionFactory connectionFactory;
	private final Connection connection;
	private final Session session;
	private InitialContext namingContext;

	public JMSBroker(final String brokerHost, final String role) {
		final Properties jmsProperties = new Properties();
		jmsProperties.setProperty("java.naming.factory.initial",
				"org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory");
		jmsProperties.setProperty("connectionFactory.ConnectionFactory",
				String.format("tcp://%s:%s", brokerHost, ARTEMIS_MULTIPLEXED_PORT));
		jmsProperties.setProperty("topic." + JMS_COMMAND_TOPIC, AMQP_COMMAND_TOPIC);
		jmsProperties.setProperty("topic." + JMS_DISTANCE_TOPIC, AMQP_DISTANCE_TOPIC);
		jmsProperties.setProperty("topic." + JMS_STATE_TOPIC, AMQP_STATE_TOPIC);

		try {
			namingContext = new InitialContext(jmsProperties);
			connectionFactory = (ConnectionFactory) namingContext.lookup("ConnectionFactory");
			connection = connectionFactory.createConnection();
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			connection.start();
		} catch (final NamingException | JMSException e) {
			throw new IllegalStateException(e);
		}
	}

	private MessageListener messageConsumer(final Consumer<String> consumer) {
		return x -> {
			try {
				String payload;
				if (x instanceof TextMessage) {
					payload = ((TextMessage) x).getText();
				}
				else if (x instanceof BytesMessage) {
					final BytesMessage msg = (BytesMessage) x;
					byte[] b = new byte[(int)msg.getBodyLength()];
					msg.readBytes(b);
					payload = new String(b);
				}
				else {
					payload = "Unknown format " + x.getClass().getName();
				}
				consumer.accept(payload);
			} catch (JMSException e) {
				throw new IllegalStateException(e);
			}
		};
	}
	
	public void listenForMessages(final Consumer<String> command, final Consumer<String> distance,
			final Consumer<String> state) {
		try {
			if (command != null) {
				session.createConsumer((Destination) namingContext.lookup(JMS_COMMAND_TOPIC))
					.setMessageListener(messageConsumer(command));
			}
			if (distance != null) {
				session.createConsumer((Destination) namingContext.lookup(JMS_DISTANCE_TOPIC))
					.setMessageListener(messageConsumer(distance));
			}
			if (state != null) {
				session.createConsumer((Destination) namingContext.lookup(JMS_STATE_TOPIC))
					.setMessageListener(messageConsumer(state));
			}
		} catch (final NamingException | JMSException e) {
			throw new IllegalStateException(e);
		}
	}

	public void sendMessage(final String text, final String destination) {
		try (final MessageProducer commandProducer = session
				.createProducer((Destination) namingContext.lookup(destination))) {
			TextMessage msg = session.createTextMessage();
			msg.setText(text);
			commandProducer.send(msg);
		} catch (final JMSException | NamingException e) {
			throw new IllegalStateException(e);
		}
	}

	public void sendCommand(final Command cmd) {
		sendMessage(cmd.name(), JMS_COMMAND_TOPIC);
	}

	public void sendObstacleDistance(final int distance) {
		sendMessage(Integer.toString(distance), MQTT_DISTANCE_TOPIC);
	}

	public void sendRobotState(final RobotState state) {
		sendMessage(state.name(), MQTT_STATE_TOPIC);
	}

	public void close() throws IOException {
		try {
			session.close();
		} catch (final Exception e) {
		}
		try {
			connection.stop();
		} catch (final Exception e) {
		}
		try {
			connection.close();
		} catch (final Exception e) {
		}
	}
}
