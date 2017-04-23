import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class StopSleepingListener implements ActionListener {

	@Override
	public void actionPerformed(ActionEvent e) {
		if (Philosopher.state == Philosopher.STATE.SLEEPING) {
			synchronized (Philosopher.stateLock) {
				Philosopher.state = Philosopher.STATE.THINKING;
			}
		}
	}

}
