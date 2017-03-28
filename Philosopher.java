import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.Random;
import javax.swing.*;

/**
 *
 * @author (your name here)
 *
 */
public class Philosopher {
	private static final int PORT_NUMBER = 8080;
	public static boolean haveLeftChopstick = false;
	public static boolean haveRightChopstick = false;
	public static JFrame mainFrame;
	public static JLabel ipLabel;
	public static JLabel leftLabel;
	public static JLabel leftIP;
	public static JLabel rightLabel;
	public static JLabel rightIP;
	public static JPanel controlPanel;

	public static void main(String[] args) throws Exception {

		if (args.length != 2) {
			throw new Exception("Must pass in two IPs");
		}

		createGUI(args);

		// create new instances of Client and Server
		Runnable r1 = new Client(PORT_NUMBER, args);
		Runnable r2 = new Server(PORT_NUMBER);

		// Create threads to run Client and Server as Threads
		Thread t1 = new Thread(r1);
		Thread t2 = new Thread(r2);

		// start the threads
		t1.start();
		t2.start();

	}

	private static void createGUI(String[] ipAddrs) {
		mainFrame = new JFrame("Philosopher Frame");
		mainFrame.setSize(400, 400);
	    mainFrame.setLayout(new GridLayout(6, 1));

		ipLabel = new JLabel("My IP Address", JLabel.CENTER);
		leftLabel = new JLabel("Left", JLabel.LEFT);
		leftIP = new JLabel(ipAddrs[0], JLabel.LEFT);
		rightLabel = new JLabel("Right", JLabel.LEFT);
		rightIP = new JLabel(ipAddrs[1], JLabel.LEFT);
		leftLabel.setSize(350, 100);
		rightLabel.setSize(350, 100);

		mainFrame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent windowEvent) {
				System.exit(0);
			}
		});
		
		JButton hungryButton = new JButton("Hungry");
		JButton dieButton = new JButton("Die");
		
		controlPanel = new JPanel();
		controlPanel.setLayout(new FlowLayout());
		controlPanel.add(hungryButton);
		controlPanel.add(dieButton);

		mainFrame.add(ipLabel);
		mainFrame.add(leftLabel);
		mainFrame.add(leftIP);
		mainFrame.add(rightLabel);
		mainFrame.add(rightIP);
		mainFrame.add(controlPanel);
		mainFrame.setVisible(true);
	}

}

class Client implements Runnable {
	private int port;
	private String[] ipAddresses;

	private enum STATE {
		THINKING, HUNGRY, EATING
	}

	private STATE state;

	public Client(int port, String[] ipAddresses) {
		this.port = port;
		this.ipAddresses = ipAddresses;
		this.state = STATE.THINKING;
	}

	@Override
	public void run() {
		// all client code here
		// you should have a "left" client connection
		// and a "right" client connection
		Socket left = connect(0);
		Socket right = connect(1);
		OutputStream leftOut = null;
		InputStream leftIn = null;
		OutputStream rightOut = null;
		InputStream rightIn = null;
		try {
			leftOut = left.getOutputStream();
			leftIn = left.getInputStream();
			rightOut = right.getOutputStream();
			rightIn = right.getInputStream();
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("Connected. Left: " + left.getInetAddress() + " Right: " + right.getInetAddress());

		Random rand = new Random();
		int maxThinkWait = 30000;
		int maxHungryWait = 1000;
		int maxEatWait = 2000;

		while (true) {
			if (this.state == STATE.THINKING) {
				System.out.println("Thinking");
				int thinkingWait = rand.nextInt(maxThinkWait) + 1;
				try {
					Thread.sleep(thinkingWait);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				this.state = STATE.HUNGRY;
			}

			if (this.state == STATE.HUNGRY) {
				System.out.println("Hungry");
				try {
					leftOut.write(1);
					int leftHas = leftIn.read();
					if (leftHas == 0) {
						Philosopher.haveLeftChopstick = true;
						rightOut.write(1);
						int rightHas = rightIn.read();
						if (rightHas == 0) {
							Philosopher.haveRightChopstick = true;
							this.state = STATE.EATING;
						} else {
							Philosopher.haveLeftChopstick = false;
							int hungryWait = rand.nextInt(maxHungryWait) + 1;
							try {
								Thread.sleep(hungryWait);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					} else {
						int hungryWait = rand.nextInt(maxHungryWait) + 1;
						try {
							Thread.sleep(hungryWait);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (this.state == STATE.EATING) {
				System.out.println("Eating");
				int eatingWait = rand.nextInt(maxEatWait) + 1;
				try {
					Thread.sleep(eatingWait);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				Philosopher.haveLeftChopstick = false;
				Philosopher.haveRightChopstick = false;
				this.state = STATE.THINKING;
			}
		}
	}

	private Socket connect(int ipIndex) {
		Socket s = null;
		try {
			s = new Socket(ipAddresses[ipIndex], port);
		} catch (Exception e) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			return connect(ipIndex);
		}
		return s;
	}

}

class Server implements Runnable {
	private int port;

	public Server(int port) {
		this.port = port;
	}

	@Override
	public void run() {
		// all server code here
		// you should have a "left" server connection
		// and a "right" server connection
		ServerSocket listener = null;
		try {
			listener = new ServerSocket(port);
			while (true) {
				Socket socket = listener.accept();
				System.out.println("Accepted connection from: " + socket.getInetAddress());
				ServerConnection connection = new ServerConnection(socket);
				Thread t = new Thread(connection);
				t.start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				listener.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}

class ServerConnection implements Runnable {
	private Socket s;

	public ServerConnection(Socket s) {
		this.s = s;
	}

	@Override
	public void run() {
		InputStream in;
		OutputStream out;
		try {
			in = s.getInputStream();
			out = s.getOutputStream();
			while (in.read() != -1) {
				if (!Philosopher.haveLeftChopstick && !Philosopher.haveRightChopstick) {
					out.write(0);
				} else {
					out.write(1);
				}
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
}
