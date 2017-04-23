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
	private boolean isRandom = true;

	public Client(int port, String[] ipAddresses) {
		this.port = port;
		this.ipAddresses = ipAddresses;
		Philosopher.state = Philosopher.STATE.THINKING;
		// Philosopher.state = Philosopher.STATE.SLEEPING;

		if (ipAddresses[2].equals("gui")) {
			this.isRandom = false;
		}

		if (ipAddresses[3].equals("1")) {
			synchronized (Philosopher.cupLock) {
				Philosopher.hadCupLast = true;
			}
		}
	}

	public Philosopher.STATE getState() {
		return Philosopher.state;
	}

	public void setState(Philosopher.STATE state) {
		synchronized (Philosopher.stateLock) {
			Philosopher.state = state;
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

		try {
			left.setSoTimeout(1500);
			right.setSoTimeout(1500);
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
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

		long drinkStart = 0;
		long drinkEnd = 0;

		boolean tooLongFlag = false;

		while (true) {

			if (Philosopher.state != Philosopher.STATE.SLEEPING) {
				if (Philosopher.needToPass) {
					try {
						leftOut.write(2);
						int response = leftIn.read();
						if (response == 1) {
							synchronized (Philosopher.cupLock) {
								Philosopher.hadCupLast = false;
								Philosopher.needToPass = false;
							}
						}
					} catch (SocketTimeoutException e) {

					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				if (Philosopher.state == Philosopher.STATE.THINKING) {
					eatStart = 0;
					eatEnd = 0;
					drinkStart = 0;
					drinkEnd = 0;
					tooLongFlag = false;
					if (!this.isRandom) {
						Philosopher.textArea.setText("THINKING");
					}

					// try {
					// Thread.sleep(5);
					// } catch (InterruptedException e1) {
					// // TODO Auto-generated catch block
					// e1.printStackTrace();
					// }
					if (this.isRandom) {
						System.out.println("Thinking");
						wait(rand, maxThinkWait);
						synchronized (Philosopher.stateLock) {
							boolean isThirsty = Math.random() > .5;
							if (isThirsty) {
								Philosopher.state = Philosopher.STATE.THIRSTY;
							} else {
								Philosopher.state = Philosopher.STATE.HUNGRY;
							}
						}
					}
				}

				if (Philosopher.state == Philosopher.STATE.HUNGRY) {
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
							try {
								rightOut.write(1);
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
							} catch (SocketTimeoutException ste) {
								synchronized (Philosopher.chopLock) {
									Philosopher.haveRightChopstick = true;
								}
								synchronized (Philosopher.stateLock) {
									Philosopher.state = Philosopher.STATE.EATING;
								}
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					} catch (SocketTimeoutException ste) {
						synchronized (Philosopher.chopLock) {
							Philosopher.haveLeftChopstick = true;
						}
						try {
							rightOut.write(1);
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
						} catch (SocketTimeoutException ste2) {
							synchronized (Philosopher.chopLock) {
								Philosopher.haveRightChopstick = true;
							}
							synchronized (Philosopher.stateLock) {
								Philosopher.state = Philosopher.STATE.EATING;
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
					end = System.currentTimeMillis();
					if (end - start > 30000) {
						System.exit(1);
					}
				}

				if (Philosopher.state == Philosopher.STATE.EATING) {
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

				if (Philosopher.state == Philosopher.STATE.THIRSTY) {
					if (!this.isRandom) {
						Philosopher.textArea.setText("THIRSTY");
					}
					System.out.println("THIRSTY");
					if (Philosopher.hadCupLast) {
						Philosopher.state = Philosopher.STATE.DRINKING;
					} else {
						try {
							Thread.sleep(1500);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}

				if (Philosopher.state == Philosopher.STATE.DRINKING) {
					if (drinkStart == 0) {
						drinkStart = System.currentTimeMillis();
					}
					if (!this.isRandom && !tooLongFlag) {
						Philosopher.textArea.setText("DRINKING");
					}

					if (this.isRandom) {
						int drinkTime = rand.nextInt(6000);
						try {
							System.out.println("DRINKING");
							Thread.sleep(drinkTime);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						Philosopher.needToPass = true;
						if (drinkTime > 5000) {
							synchronized (Philosopher.stateLock) {
								Philosopher.state = Philosopher.STATE.SLEEPING;
							}
						} else {
							try {
								leftOut.write(2);
								int response = leftIn.read();
								System.out.println("PASSED THE CUP");
								if (response == 1) {
									synchronized (Philosopher.cupLock) {
										Philosopher.hadCupLast = false;
										Philosopher.needToPass = false;
									}
								}
							} catch (SocketTimeoutException e) {
								if (Philosopher.state != Philosopher.STATE.SLEEPING) {
									continue;
								}
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}

					drinkEnd = System.currentTimeMillis();

					if (drinkEnd - drinkStart > 5000) {
						Philosopher.textArea.setText("DRINKING TOO LONG");
						tooLongFlag = true;
					}
				}

//				if (Philosopher.hadCupLast) {
//					System.out.println("I HAVE THE CUP");
//					boolean isThirsty = Math.random() > .5;
//					if (isThirsty) {
//						System.out.println("THIRSTY");
//						int drinkTime = rand.nextInt(6000);
//						try {
//							System.out.println("DRINKING");
//							Thread.sleep(drinkTime);
//						} catch (InterruptedException e) {
//							e.printStackTrace();
//						}
//						Philosopher.needToPass = true;
//						if (drinkTime > 5000) {
//							synchronized (Philosopher.stateLock) {
//								Philosopher.state = Philosopher.STATE.SLEEPING;
//							}
//						} else {
//							try {
//								leftOut.write(2);
//								int response = leftIn.read();
//								System.out.println("PASSED THE CUP");
//								if (response == 1) {
//									synchronized (Philosopher.cupLock) {
//										Philosopher.hadCupLast = false;
//										Philosopher.needToPass = false;
//									}
//								}
//							} catch (SocketTimeoutException e) {
//								if (Philosopher.state != Philosopher.STATE.SLEEPING) {
//									continue;
//								}
//							} catch (IOException e) {
//								e.printStackTrace();
//							}
//						}
//					} else {
//						try {
//							leftOut.write(2);
//							int response = leftIn.read();
//							if (response == 1) {
//								synchronized (Philosopher.cupLock) {
//									Philosopher.hadCupLast = false;
//									Philosopher.needToPass = false;
//								}
//							}
//						} catch (SocketTimeoutException e) {
//							continue;
//						} catch (IOException e) {
//							e.printStackTrace();
//						}
//					}
//				}
			} else {
				synchronized (Philosopher.chopLock) {
					Philosopher.haveLeftChopstick = false;
					Philosopher.haveRightChopstick = false;
				}
				
				try {
					System.out.println("SLEEPING");
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				while (Math.random() < .9) {
					try {
						System.out.println("SLEEPING");
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				if (Philosopher.needToPass) {
					try {
						leftOut.write(2);
						int response = leftIn.read();
						if (response == 1) {
							synchronized (Philosopher.cupLock) {
								Philosopher.hadCupLast = false;
								Philosopher.needToPass = false;
							}
						}
					} catch (SocketTimeoutException e) {
						continue;
					} catch (IOException e) {
						e.printStackTrace();
					}
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
