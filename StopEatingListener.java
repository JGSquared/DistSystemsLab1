import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class StopEatingListener implements ActionListener {
	public Client client;

	public StopEatingListener(Client client) {
		this.client = client;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (Philosopher.state == Philosopher.STATE.EATING) {
			synchronized (Philosopher.chopLock) {
				Philosopher.haveLeftChopstick = false;
				Philosopher.haveRightChopstick = false;
			}
			synchronized (Philosopher.stateLock) {
				this.client.setMainState(Philosopher.STATE.THINKING);
			}
		}
	}
}
