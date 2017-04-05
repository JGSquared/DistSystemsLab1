import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class Messenger implements Runnable {
	public OutputStream leftOut = null;
	public InputStream leftIn = null;
	public OutputStream rightOut = null;
	
	public Messenger(Socket left, Socket right) throws IOException {
		this.leftOut = left.getOutputStream();
		this.leftIn = left.getInputStream();
		this.rightOut = right.getOutputStream();
	}

	@Override
	public void run() {
		while (true) {
			Message message = null;
			
			if (!Philosopher.messages.isEmpty()) {
				synchronized (Philosopher.messageLock) {
					message = Philosopher.messages.poll();
				}
				if (message.goLeft) {
					try {
						this.leftOut.write(message.message);
						this.leftIn.read();
					} catch (SocketTimeoutException t) {
						try {
							this.rightOut.write(1);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					try {
						this.rightOut.write(message.message);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

}
