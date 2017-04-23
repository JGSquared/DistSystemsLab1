import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class StopDrinkingListener implements ActionListener {

	@Override
	public void actionPerformed(ActionEvent e) {
		if (Philosopher.state == Philosopher.STATE.DRINKING) {
			synchronized (Philosopher.stateLock) {
				Philosopher.state = Philosopher.STATE.THINKING;
			}
		}
	}

}
