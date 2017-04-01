import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ClientActionListener implements ActionListener {
	public Client client;

	public ClientActionListener(Client client) {
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
