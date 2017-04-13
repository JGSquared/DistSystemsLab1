import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Random;

class Client implements Runnable {
	private int port;
	private String[] ipAddresses;

	public Client(int port, String[] ipAddresses) {
		this.port = port;
		this.ipAddresses = ipAddresses;
		Philosopher.state = Philosopher.STATE.THINKING;
//		Philosopher.thirstState = Philosopher.STATE.QUENCHED;

		if (ipAddresses.length == 3) {
			Philosopher.isRandom = false;
		}
	}

	// public STATE getMainState() {
	// return this.mainState;
	// }

	public void setMainState(Philosopher.STATE state) {
		synchronized (Philosopher.stateLock) {
			Philosopher.state = state;
		}
	}

	// public STATE getThirstState() {
	// return this.thirstState;
	// }

//	public void setThirstState(Philosopher.STATE state) {
//		synchronized (Philosopher.thirstLock) {
//			Philosopher.thirstState = state;
//		}
//	}

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
			left.setSoTimeout(10000);
		} catch (SocketException e1) {
			e1.printStackTrace();
		}
		Socket right = connect(1);
		try {
			right.setSoTimeout(10000);
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

		while (true) {
			if (Philosopher.state != Philosopher.STATE.SLEEPING) {
				if (Philosopher.state == Philosopher.STATE.THINKING) {
					eatStart = 0;
					eatEnd = 0;
					tooLongFlag = false;
					if (!Philosopher.isRandom) {
						Philosopher.textArea.setText("THINKING");
					}
					if (Philosopher.isRandom) {
						System.out.println("Thinking");
						wait(rand, maxThinkWait);
						int d3 = (int) Math.ceil((Math.random() * 3));
						if (d3 == 1) {
							synchronized (Philosopher.stateLock) {
								Philosopher.state = Philosopher.STATE.HUNGRY;
							}
						}
						else if (d3 == 2) {
							synchronized (Philosopher.stateLock) {
								Philosopher.state = Philosopher.STATE.THIRSTY;
							}
						}
						else {
							synchronized (Philosopher.stateLock) {
								Philosopher.state = Philosopher.STATE.FAMISHED;
							}
						}
					}
				}

				if (Philosopher.state == Philosopher.STATE.HUNGRY) {
					if (start == 0) {
						start = System.currentTimeMillis();
					}
					System.out.println("Hungry");
					if (!Philosopher.isRandom) {
						Philosopher.textArea.setText("HUNGRY");
					}
					try {
						leftOut.write(2);
						int leftHas = leftIn.read();
						if (leftHas == 0) {
							synchronized (Philosopher.chopLock) {
								Philosopher.haveLeftChopstick = true;
							}
							rightOut.write(2);
							int rightHas = rightIn.read();
							if (rightHas == 0) {
								synchronized (Philosopher.chopLock) {
									Philosopher.haveRightChopstick = true;
								}
								synchronized (Philosopher.stateLock) {
									Philosopher.state = Philosopher.STATE.EATING;
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
					} catch (SocketTimeoutException e) {
						wait(rand, maxHungryWait);
					} catch (IOException e) {
						e.printStackTrace();
					}
					end = System.currentTimeMillis();
					if (end - start > 30000) {
						System.exit(1);
					}
				}
				
				if (Philosopher.state == Philosopher.STATE.THIRSTY) {
					System.out.println("THIRSTY");
					try {
						leftOut.write(3);
						Philosopher.haveAsked = true;
						int response = leftIn.read();
						while (Philosopher.count != -1) {
							if (Philosopher.haveCup) {
								synchronized (Philosopher.stateLock) {
									Philosopher.haveAsked = false;
									Philosopher.state = Philosopher.STATE.DRINKING;
								}
								break;
							}
						}
						if (Philosopher.state == Philosopher.STATE.THIRSTY) {
							Philosopher.haveAsked = false;
							wait(rand, 2000);
						}
					} catch (SocketTimeoutException e) {
						wait(rand, 2000);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
				if (Philosopher.state == Philosopher.STATE.FAMISHED) {
					System.out.println("FAMISHED");
					if (!Philosopher.isRandom) {
						Philosopher.textArea.setText("FAMISHED");
					}
					try {
						leftOut.write(2);
						int leftHas = leftIn.read();
						if (leftHas == 0) {
							synchronized (Philosopher.chopLock) {
								Philosopher.haveLeftChopstick = true;
							}
							rightOut.write(2);
							int rightHas = rightIn.read();
							if (rightHas == 0) {
								synchronized (Philosopher.chopLock) {
									Philosopher.haveRightChopstick = true;
								}
							} else {
								synchronized (Philosopher.chopLock) {
									Philosopher.haveLeftChopstick = false;
								}
							}
						}
					} catch (SocketTimeoutException e) {
						wait(rand, maxHungryWait);
					} catch (IOException e) {
						e.printStackTrace();
					}
					try {
						leftOut.write(3);
						Philosopher.haveAsked = true;
						int response = leftIn.read();
						while (Philosopher.count != -1) {
							if (Philosopher.haveCup) {
								if (Philosopher.haveLeftChopstick && Philosopher.haveRightChopstick) {
									synchronized (Philosopher.stateLock) {
										Philosopher.haveAsked = false;
										Philosopher.state = Philosopher.STATE.DINING;
									}
									break;
								}
							}
						}
						if (Philosopher.state == Philosopher.STATE.FAMISHED) {
							Philosopher.haveAsked = false;
							wait(rand, 2000);
						}
					} catch (SocketTimeoutException e) {
						wait(rand, 2000);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				if (Philosopher.state == Philosopher.STATE.EATING) {
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
						synchronized (Philosopher.stateLock) {
							Philosopher.state = Philosopher.STATE.THINKING;
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
				
				if (Philosopher.state == Philosopher.STATE.DRINKING) {
					System.out.println("DRINKING");
					int drinkTime = rand.nextInt(5000);
					try {
						Thread.sleep(drinkTime);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if (drinkTime > 4000) {
						synchronized (Philosopher.chopLock) {
							Philosopher.haveLeftChopstick = false;
							Philosopher.haveRightChopstick = false;
						}
						synchronized (Philosopher.cupLock) {
							Philosopher.haveCup = false;
						}
						synchronized (Philosopher.stateLock) {
							Philosopher.state = Philosopher.STATE.SLEEPING;
						}
					} else {
						synchronized (Philosopher.stateLock) {
							Philosopher.state = Philosopher.STATE.THINKING;
						}
					}
				}
				
				if (Philosopher.state == Philosopher.STATE.DINING) {
					System.out.println("DRINKING");
					int drinkTime = rand.nextInt(5000);
					try {
						Thread.sleep(drinkTime);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if (drinkTime > 4000) {
						synchronized (Philosopher.chopLock) {
							Philosopher.haveLeftChopstick = false;
							Philosopher.haveRightChopstick = false;
						}
						synchronized (Philosopher.cupLock) {
							Philosopher.haveCup = false;
						}
						synchronized (Philosopher.stateLock) {
							Philosopher.state = Philosopher.STATE.SLEEPING;
						}
					} else {
						synchronized (Philosopher.stateLock) {
							Philosopher.state = Philosopher.STATE.THINKING;
						}
					}
				}
				
				if (Philosopher.state == Philosopher.STATE.SLEEPING) {
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					while (Math.random() < .9) {
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					synchronized (Philosopher.stateLock) {
						Philosopher.state = Philosopher.STATE.THINKING;
					}
				}
				
				if (Philosopher.count == -1) {
					synchronized (Philosopher.countLock) {
						Philosopher.count = 0;
					}
				}
			} else {
				wait(rand, 4000);
				while (Math.random() < .9) {
					wait(rand, 4000);
				}
				synchronized (Philosopher.stateLock) {
					Philosopher.state = Philosopher.STATE.THINKING;
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
