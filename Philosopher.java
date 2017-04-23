import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;


/**
 *
 * @author (your name here)
 *
 */
public class Philosopher {
	public static Object chopLock = new Object();
	public static Object stateLock = new Object();
	public static Object cupLock = new Object();
	private static final int PORT_NUMBER = 8080;
	public static boolean haveLeftChopstick = false;
	public static boolean haveRightChopstick = false;
	public static boolean hadCupLast = false;
	public static boolean needToPass = false;
	public static JFrame mainFrame;
	public static JTextField textArea;
	public static JLabel ipLabel;
	public static JLabel leftLabel;
	public static JLabel leftIP;
	public static JLabel rightLabel;
	public static JLabel rightIP;
	public static JPanel controlPanel;
	
	public enum STATE {
		THINKING, HUNGRY, EATING, SLEEPING, THIRSTY, DRINKING
	}

	public static STATE state;

	public static void main(String[] args) throws Exception {

		if (args.length < 4) {
			throw new Exception("Must pass in two IPs, <gui/nogui>, <hasCup>");
		}

		if (args.length == 3 && (!args[2].equals("gui"))) {
			throw new Exception("What is this optional argument???");
		}

		// create new instances of Client and Server
		Runnable r1 = new Client(PORT_NUMBER, args);
		Runnable r2 = new Server(PORT_NUMBER);

		// Create threads to run Client and Server as Threads
		Thread t1 = new Thread(r1);
		Thread t2 = new Thread(r2);

		if (args.length == 4 && args[2].equals("gui")) {
			createGUI(r1, args);
		}

		// start the threads
		t1.start();
		t2.start();

	}

	public static void createGUI(Runnable client, String[] args) {
		mainFrame = new JFrame("Philosopher Frame");
		mainFrame.setSize(400, 400);
		mainFrame.setLayout(new GridLayout(6, 1));
		
		textArea = new JTextField();
		textArea.setEditable(false);
//		textArea.setText("THINKING");
		mainFrame.add(textArea);

		ipLabel = new JLabel("My IP Address", JLabel.CENTER);
		leftLabel = new JLabel("Left", JLabel.LEFT);
		leftIP = new JLabel(args[0], JLabel.LEFT);
		rightLabel = new JLabel("Right", JLabel.LEFT);
		rightIP = new JLabel(args[1], JLabel.LEFT);
		leftLabel.setSize(350, 100);
		rightLabel.setSize(350, 100);
		
		

		mainFrame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent windowEvent) {
				System.exit(0);
			}
		});

		JButton hungryButton = new JButton("Hungry");
		hungryButton.addActionListener(new ClientActionListener((Client) client));
		JButton stopEatingButton = new JButton("Stop Eating");
		stopEatingButton.addActionListener(new StopEatingListener((Client) client));
		JButton thirstyButton = new JButton("Thirsty");
		thirstyButton.addActionListener(new ThirstyListener());
		JButton stopDrinkingButton = new JButton("Stop Drinking");
		stopDrinkingButton.addActionListener(new StopDrinkingListener());
		JButton sleepingButton = new JButton("Sleep");
		sleepingButton.addActionListener(new SleepingListener());
		JButton stopSleepingButton = new JButton("Stop Sleeping");
		stopSleepingButton.addActionListener(new StopSleepingListener());
		
		

		controlPanel = new JPanel();
		controlPanel.setLayout(new FlowLayout());
		controlPanel.add(hungryButton);
		controlPanel.add(stopEatingButton);
		controlPanel.add(thirstyButton);
		controlPanel.add(stopDrinkingButton);
		controlPanel.add(sleepingButton);
		controlPanel.add(stopSleepingButton);

		mainFrame.add(ipLabel);
		mainFrame.add(leftLabel);
		mainFrame.add(leftIP);
		mainFrame.add(rightLabel);
		mainFrame.add(rightIP);
		mainFrame.add(controlPanel);
		mainFrame.setVisible(true);
	}


}
