import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SleepingListener implements ActionListener {

	@Override
	public void actionPerformed(ActionEvent e) {
		synchronized (Philosopher.stateLock) {
				Philosopher.state = Philosopher.STATE.SLEEPING;
		}
	}

}
