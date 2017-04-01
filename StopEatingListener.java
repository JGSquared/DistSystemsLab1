import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class StopEatingListener implements ActionListener {
	public Client client;

	public StopEatingListener(Client client) {
		this.client = client;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (this.client.getState() == Client.STATE.EATING) {
			synchronized (Philosopher.chopLock) {
				Philosopher.haveLeftChopstick = false;
				Philosopher.haveRightChopstick = false;
			}
			synchronized (Philosopher.stateLock) {
				this.client.setState(Client.STATE.THINKING);
			}
		}
	}
}
