import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ThirstyListener implements ActionListener {

	@Override
	public void actionPerformed(ActionEvent e) {
		synchronized (Philosopher.stateLock) {
			if (Philosopher.state == Philosopher.STATE.THINKING) {
				Philosopher.state = Philosopher.STATE.THIRSTY;
			}
		}
	}
}
