import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Random;

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
	
	private void wait(Random rand, int waitTime) {
		int wait = rand.nextInt(waitTime) + 1;
		try {
			Thread.sleep(wait);
		} catch (InterruptedException e) {
			e.printStackTrace();
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
		
		boolean tooLongFlag = false;

		while (true) {

			if (this.state == STATE.THINKING) {
				eatStart = 0;
				eatEnd = 0;
				tooLongFlag = false;
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
					wait(rand, maxThinkWait);
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
							wait(rand, maxHungryWait);
						}
					} else {
						wait(rand, maxHungryWait);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				end = System.currentTimeMillis();
				if (end - start > 30000) {
					System.exit(1);
				}
			}

			if (this.state == STATE.EATING) {
				if (eatStart == 0) {
					eatStart = System.currentTimeMillis();
				}
				if (!this.isRandom && !tooLongFlag) {
					Philosopher.textArea.setText("EATING");
				}
				
				if (this.isRandom) {
					System.out.println("Eating");
					wait(rand, maxEatWait);
					synchronized (Philosopher.chopLock) {
						Philosopher.haveLeftChopstick = false;
						Philosopher.haveRightChopstick = false;
					}
					synchronized (Philosopher.stateLock) {
						this.state = STATE.THINKING;
					}
				}
				eatEnd = System.currentTimeMillis();
				
				if (eatEnd - eatStart > 2000) {
					Philosopher.textArea.setText("EATING TOO LONG");
					tooLongFlag = true;
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
