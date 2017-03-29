import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.Timer;

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
	private static final int PORT_NUMBER = 8080;
	public static boolean haveLeftChopstick = false;
	public static boolean haveRightChopstick = false;
	public static JFrame mainFrame;
	public static JTextField textArea;
	public static JLabel ipLabel;
	public static JLabel leftLabel;
	public static JLabel leftIP;
	public static JLabel rightLabel;
	public static JLabel rightIP;
	public static JPanel controlPanel;

	public static void main(String[] args) throws Exception {

		if (args.length < 2 || args.length > 3) {
			throw new Exception("Must pass in two IPs and optionally <gui>");
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

		if (args.length == 3 && args[2].equals("gui")) {
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
		hungryButton.addActionListener(new clientActionListener((Client) client));
		JButton stopEatingButton = new JButton("Stop Eating");
		stopEatingButton.addActionListener(new StopEatingListener((Client) client));

		controlPanel = new JPanel();
		controlPanel.setLayout(new FlowLayout());
		controlPanel.add(hungryButton);
		controlPanel.add(stopEatingButton);

		mainFrame.add(ipLabel);
		mainFrame.add(leftLabel);
		mainFrame.add(leftIP);
		mainFrame.add(rightLabel);
		mainFrame.add(rightIP);
		mainFrame.add(controlPanel);
		mainFrame.setVisible(true);
	}


}

class clientActionListener implements ActionListener {
	public Client client;

	public clientActionListener(Client client) {
		this.client = client;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		synchronized (Philosopher.stateLock) {
			if (this.client.getState() == Client.STATE.THINKING) {
				this.client.setState(Client.STATE.HUNGRY);
			}
		}
	}
}

class StopEatingListener implements ActionListener {
	public Client client;

	public StopEatingListener(Client client) {
		this.client = client;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		synchronized (Philosopher.stateLock) {
			if (this.client.getState() == Client.STATE.EATING) {
				this.client.setState(Client.STATE.THINKING);
			}
		}
	}
}

class Client implements Runnable {
	private int port;
	private String[] ipAddresses;
	private boolean isRandom = true;

	public enum STATE {
		THINKING, HUNGRY, EATING
	}

	private STATE state;

	public Client(int port, String[] ipAddresses) {
		this.port = port;
		this.ipAddresses = ipAddresses;
		this.state = STATE.THINKING;

		if (ipAddresses.length == 3) {
			this.isRandom = false;
		}
	}

	public STATE getState() {
		return state;
	}

	public void setState(STATE state) {
		synchronized(Philosopher.stateLock) {
			this.state = state;
		}
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
		int maxThinkWait = 10000;
		int maxHungryWait = 1000;
		int maxEatWait = 2000;
		
		long start = 0;
		long end = 0;
		
		long eatStart = 0;
		long eatEnd = 0;

		while (true) {

			if (this.state == STATE.THINKING) {
				eatStart = 0;
				eatEnd = 0;
				if (!this.isRandom) {
					Philosopher.textArea.setText("THINKING");
				}
				
//				try {
//					Thread.sleep(5);
//				} catch (InterruptedException e1) {
//					// TODO Auto-generated catch block
//					e1.printStackTrace();
//				}
				if (this.isRandom) {
					System.out.println("Thinking");
					int thinkingWait = rand.nextInt(maxThinkWait) + 1;
					try {
						Thread.sleep(thinkingWait);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					synchronized (Philosopher.stateLock) {
						this.state = STATE.HUNGRY;
					}
				}
			}

			if (this.state == STATE.HUNGRY) {
				if (start == 0) {
					start = System.currentTimeMillis();
				}
				System.out.println("Hungry");
				if (!this.isRandom) {
					Philosopher.textArea.setText("HUNGRY");
				}
				try {
					leftOut.write(1);
					int leftHas = leftIn.read();
					if (leftHas == 0) {
						synchronized (Philosopher.chopLock) {
							Philosopher.haveLeftChopstick = true;
						}
						rightOut.write(1);
						int rightHas = rightIn.read();
						if (rightHas == 0) {
							synchronized (Philosopher.chopLock) {
								Philosopher.haveRightChopstick = true;
							}
							synchronized (Philosopher.stateLock) {
								this.state = STATE.EATING;
							}
						} else {
							synchronized (Philosopher.chopLock) {
								Philosopher.haveLeftChopstick = false;
							}
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
				end = System.currentTimeMillis();
				if (end - start > 8000) {
					System.exit(1);
				}
			}

			if (this.state == STATE.EATING) {
				if (eatStart == 0) {
					eatStart = System.currentTimeMillis();
				}
				System.out.println("Eating");
				if (!this.isRandom) {
					Philosopher.textArea.setText("EATING");
				}
				
				if (this.isRandom) {
					int eatingWait = rand.nextInt(maxEatWait) + 1;
					
					try {
						Thread.sleep(eatingWait);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					synchronized (Philosopher.chopLock) {
						Philosopher.haveLeftChopstick = false;
						Philosopher.haveRightChopstick = false;
					}
					synchronized (Philosopher.stateLock) {
						this.state = STATE.THINKING;
					}
				}
				eatEnd = System.currentTimeMillis();
				if (eatEnd - eatStart > maxEatWait) {
					System.err.println("You have been eating for longer than two seconds! STOP IT!");
					eatStart = 0;
					eatEnd = 0;
				}
				start = 0;
				end = 0;
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
