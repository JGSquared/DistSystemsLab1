import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Random;

class Client implements Runnable {
	private int port;
	private String[] ipAddresses;

	public Client(int port, String[] ipAddresses) {
		this.port = port;
		this.ipAddresses = ipAddresses;
		Philosopher.mainState = Philosopher.STATE.THINKING;
		Philosopher.thirstState = Philosopher.STATE.QUENCHED;

		if (ipAddresses.length == 3) {
			Philosopher.isRandom = false;
		}
	}

	// public STATE getMainState() {
	// return this.mainState;
	// }

	public void setMainState(Philosopher.STATE state) {
		synchronized (Philosopher.stateLock) {
			Philosopher.mainState = state;
		}
	}

	// public STATE getThirstState() {
	// return this.thirstState;
	// }

	public void setThirstState(Philosopher.STATE state) {
		synchronized (Philosopher.thirstLock) {
			Philosopher.thirstState = state;
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
		Socket left = connect(0);
		try {
			left.setSoTimeout(1000);
		} catch (SocketException e1) {
			e1.printStackTrace();
		}
		Socket right = connect(1);
		try {
			right.setSoTimeout(1000);
		} catch (SocketException e1) {
			e1.printStackTrace();
		}
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

		try {
			Thread messengerThread = new Thread(new Messenger(left, right));
			messengerThread.start();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		Thread thirstThread = new Thread(new ThirstClient(left, right));
		thirstThread.start();

		while (true) {
			if (Philosopher.mainState != Philosopher.STATE.SLEEPING) {
				if (Philosopher.mainState == Philosopher.STATE.THINKING) {
					eatStart = 0;
					eatEnd = 0;
					tooLongFlag = false;
					if (!Philosopher.isRandom) {
						Philosopher.textArea.setText("THINKING");
					}
					if (Philosopher.isRandom) {
						System.out.println("Thinking");
						wait(rand, maxThinkWait);
						synchronized (Philosopher.stateLock) {
							Philosopher.mainState = Philosopher.STATE.HUNGRY;
						}
					}
				}

				if (Philosopher.mainState == Philosopher.STATE.HUNGRY) {
					if (start == 0) {
						start = System.currentTimeMillis();
					}
					System.out.println("Hungry");
					if (!Philosopher.isRandom) {
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
									Philosopher.mainState = Philosopher.STATE.EATING;
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

				if (Philosopher.mainState == Philosopher.STATE.EATING) {
					if (eatStart == 0) {
						eatStart = System.currentTimeMillis();
					}
					if (!Philosopher.isRandom && !tooLongFlag) {
						Philosopher.textArea.setText("EATING");
					}

					if (Philosopher.isRandom) {
						System.out.println("Eating");
						wait(rand, maxEatWait);
						synchronized (Philosopher.chopLock) {
							Philosopher.haveLeftChopstick = false;
							Philosopher.haveRightChopstick = false;
						}
						if (Philosopher.thirstState == Philosopher.STATE.QUENCHED) {
							synchronized (Philosopher.stateLock) {
								Philosopher.mainState = Philosopher.STATE.THINKING;
							}
						} else {
							synchronized (Philosopher.stateLock) {
								Philosopher.mainState = Philosopher.STATE.SATISFIED;
							}
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
				
				if (Philosopher.mainState == Philosopher.STATE.SATISFIED && Philosopher.thirstState == Philosopher.STATE.QUENCHED) {
					synchronized (Philosopher.stateLock) {
						Philosopher.mainState = Philosopher.STATE.THINKING;
					}
				}
			} else {
				wait(rand, 4000);
				while (Math.random() < .9) {
					wait(rand, 4000);
				}
				synchronized (Philosopher.stateLock) {
					Philosopher.mainState = Philosopher.STATE.THINKING;
				}
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
