package org.jboss.demo.artemis.ev3.controller;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.jboss.demo.artemis.ev3.Broker;
import org.jboss.demo.artemis.ev3.Command;
import org.jboss.demo.artemis.ev3.RobotState;

public class Controller {

	private static final String ROLE_CONTROLLER = "controller";
	private static final String TITLE_PREFIX = "EV3 Controller: ";
	private final JFrame window;
	private final JButton forward;
	private final JButton backward;
	private final JButton stop;
	private final JButton left;
	private final JButton right;
	private final JButton grab;
	private final JButton release;
	private final JButton quit;
	private final JTextField distance;
	private final Broker broker;

	private Controller() {
		broker = new JMSBroker("localhost", ROLE_CONTROLLER);

		window = new JFrame();
		setWindowTitle(RobotState.UNKNOWN);
		window.setSize(800, 600);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		final Container mainPane = window.getContentPane();
		final Container movementPane = new JPanel();
		movementPane.setLayout(new BorderLayout());
		final Container handPane = new JPanel();
		handPane.setLayout(new FlowLayout());
		mainPane.add(movementPane, BorderLayout.WEST);
		mainPane.add(handPane, BorderLayout.SOUTH);

		forward = new JButton("Forward");
		forward.setPreferredSize(new Dimension(200, 100));
		movementPane.add(forward, BorderLayout.NORTH);
		forward.addActionListener(e -> broker.sendCommand(Command.FORWARD));

		backward = new JButton("Backward");
		backward.setPreferredSize(new Dimension(200, 100));
		movementPane.add(backward, BorderLayout.SOUTH);
		backward.addActionListener(e -> broker.sendCommand(Command.BACKWARD));

		stop = new JButton("Stop");
		stop.setPreferredSize(new Dimension(200, 100));
		movementPane.add(stop, BorderLayout.CENTER);
		stop.addActionListener(e -> broker.sendCommand(Command.STOP));

		left = new JButton("Left");
		left.setPreferredSize(new Dimension(200, 100));
		movementPane.add(left, BorderLayout.WEST);
		left.addActionListener(e -> broker.sendCommand(Command.LEFT));

		right = new JButton("Right");
		right.setPreferredSize(new Dimension(200, 100));
		movementPane.add(right, BorderLayout.EAST);
		right.addActionListener(e -> broker.sendCommand(Command.RIGHT));

		distance = new JTextField();
		mainPane.add(distance, BorderLayout.EAST);
		Font font = new Font("Courier", Font.BOLD, 16);

		grab = new JButton("Grab");
		grab.setPreferredSize(new Dimension(120, 40));
		handPane.add(grab, BorderLayout.EAST);
		grab.addActionListener(e -> broker.sendCommand(Command.GRAB));

		release = new JButton("Release");
		release.setPreferredSize(new Dimension(120, 40));
		handPane.add(release);
		release.addActionListener(e -> broker.sendCommand(Command.RELEASE));

		quit = new JButton("Quit");
		quit.setPreferredSize(new Dimension(120, 40));
		handPane.add(quit);
		quit.addActionListener(e -> {
			broker.sendCommand(Command.QUIT);
			window.dispose();
		});

		distance.setFont(font);
		distance.setText("Unknown");
		distance.setPreferredSize(new Dimension(300, 100));

		broker.listenForMessages(null, payload -> SwingUtilities.invokeLater(() -> setDistance(payload)),
				payload -> SwingUtilities.invokeLater(() -> setWindowTitle(RobotState.valueOf(payload))));
	}

	private void setWindowTitle(final RobotState state) {
		window.setTitle(TITLE_PREFIX + state.name());
	}

	private void setDistance(final String distance) {
		this.distance.setText(Integer.toString(Integer.MAX_VALUE).equals(distance) ? "INFINITY" : distance);
	}

	public void run() {
		window.pack();
		window.setVisible(true);
	}

	public static void main(final String[] args) {
		Controller controller = new Controller();
		controller.run();
	}

}
