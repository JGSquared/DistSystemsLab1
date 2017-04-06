import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Random;

public class ThirstClient implements Runnable {
	private Socket left;
	private Socket right;
	
	public ThirstClient(Socket left, Socket right) {
		this.left = left;
		this.right = right;
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
		
		Random rand = new Random();
		
		int maxQuenchTime = 4000;
		boolean askedFlag = false;
		
		while (true) {
			if (Philosopher.mainState != Philosopher.STATE.THINKING && Philosopher.mainState != Philosopher.STATE.SLEEPING) {
				askedFlag = false;
				if (Philosopher.thirstState == Philosopher.STATE.QUENCHED) {
					System.out.println("QUENCHED");
					if (Philosopher.isRandom) {
						wait(rand, maxQuenchTime);
						synchronized (Philosopher.thirstLock) {
							Philosopher.thirstState = Philosopher.STATE.THIRSTY;
						}
					}
				}
				
				if (Philosopher.thirstState == Philosopher.STATE.THIRSTY) {
					System.out.println("THIRSTY");
					if (!askedFlag) {
						askedFlag = true;
						try {
							leftOut.write(3);
						} catch (IOException e) {
							e.printStackTrace();
						}
					} else {
						if (Philosopher.haveCup) {
							synchronized(Philosopher.thirstLock) {
								Philosopher.thirstState = Philosopher.STATE.DRINKING;
							}
						} else if (Philosopher.count == -1) {
							askedFlag = false;
							Philosopher.count = 0;
							wait(rand, 1500);
						}
					}
				}
				
				if (Philosopher.thirstState == Philosopher.STATE.DRINKING) {
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
							Philosopher.mainState = Philosopher.STATE.SLEEPING;
						}
					}
					synchronized (Philosopher.thirstLock) {
						Philosopher.thirstState = Philosopher.STATE.QUENCHED;
					}
				}
			}
		}
	}

}
