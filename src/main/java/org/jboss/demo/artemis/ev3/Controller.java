package org.jboss.demo.artemis.ev3;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class Controller {

	private static final String TITLE_PREFIX = "EV3 Controller: ";
	private final JFrame window;
	private final JButton forward;
	private final JButton stop;
	private final JButton left;
	private final JButton right;
	private final JTextField distance;
	private final Broker broker;

	private Controller() {
		broker = new Broker("10.0.1.11", "controller");

		window = new JFrame();
		setWindowTitle(RobotState.UNKNOWN);
		window.setSize(800, 600);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Container pane = window.getContentPane();

		forward = new JButton("Forward");
		forward.setPreferredSize(new Dimension(200, 100));
		pane.add(forward, BorderLayout.NORTH);
		forward.addActionListener(e -> broker.sendCommand(Command.FORWARD));

		stop = new JButton("Stop");
		stop.setPreferredSize(new Dimension(200, 100));
		pane.add(stop, BorderLayout.SOUTH);
		stop.addActionListener(e -> broker.sendCommand(Command.STOP));

		left = new JButton("Left");
		left.setPreferredSize(new Dimension(200, 100));
		pane.add(left, BorderLayout.WEST);
		left.addActionListener(e -> broker.sendCommand(Command.LEFT));

		right = new JButton("Right");
		right.setPreferredSize(new Dimension(200, 100));
		pane.add(right, BorderLayout.EAST);
		right.addActionListener(e -> broker.sendCommand(Command.RIGHT));

		distance = new JTextField();
		pane.add(distance, BorderLayout.CENTER);
		Font font = new Font("Courier", Font.BOLD, 16);

		distance.setFont(font);
		distance.setText("Unknown");
		distance.setPreferredSize(new Dimension(300, 100));

		broker.listenForMessages(null, payload -> SwingUtilities
				.invokeLater(() -> setDistance(payload)),
				payload -> SwingUtilities
						.invokeLater(() -> setWindowTitle(RobotState
								.valueOf(payload))));
	}

	private void setWindowTitle(RobotState state) {
		window.setTitle(TITLE_PREFIX + state.name());
	}

	private void setDistance(String distance) {
		this.distance.setText(distance);
	}

	public void run() {
		window.pack();
		window.setVisible(true);
	}

	public static void main(String[] args) {
		Controller controller = new Controller();
		controller.run();
	}

}
