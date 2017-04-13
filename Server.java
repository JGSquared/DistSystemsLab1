import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Server implements Runnable {
	private int port;

	public Server(int port) {
		this.port = port;
	}

	@Override
	public void run() {
		// all server code here
		// you should have a "left" server connection
		// and a "right" server connection
		ServerSocket listener = null;
		try {
			listener = new ServerSocket(port);
			while (true) {
				Socket socket = listener.accept();
				System.out.println("Accepted connection from: " + socket.getInetAddress());
				ServerConnection connection = new ServerConnection(socket);
				Thread t = new Thread(connection);
				t.start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				listener.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}

class ServerConnection implements Runnable {
	private Socket s;

	public ServerConnection(Socket s) {
		this.s = s;
	}

	@Override
	public void run() {
		InputStream in;
		OutputStream out;
		try {
			int received;
			in = s.getInputStream();
			out = s.getOutputStream();
			while ((received = in.read()) != -1) {
				if (Philosopher.state != Philosopher.STATE.SLEEPING) {
					switch (received) {
					case 0:
						if (Philosopher.forwardPending) {
							synchronized (Philosopher.messageLock) {
								Philosopher.messages.add(new Message(false, 0));
							}
						} else if (Philosopher.haveAsked) {
							synchronized (Philosopher.countLock) {
								Philosopher.count++;
							}
						}
						break;
					case 1:
						if (Philosopher.haveAsked) {
							if (Philosopher.count == (Philosopher.numPhilosophers - 1)) {
								synchronized (Philosopher.cupLock) {
									Philosopher.haveCup = true;
								}
							} else {
								synchronized (Philosopher.countLock) {
									Philosopher.count = -1;
								}
							}
						} else if (Philosopher.forwardPending) {
							synchronized (Philosopher.messageLock) {
								Philosopher.messages.add(new Message(false, 1));
							}
						}

						Philosopher.forwardPending = false;
						break;
					case 2:
						if (!Philosopher.haveLeftChopstick && !Philosopher.haveRightChopstick) {
							out.write(0);
						} else {
							out.write(1);
						}
						break;
					case 3:
						out.write(1);
						if (Philosopher.haveAsked || Philosopher.haveCup) {
							synchronized (Philosopher.messageLock) {
								Philosopher.messages.add(new Message(false, 1));
							}
						} else {
							synchronized (Philosopher.messageLock) {
								Philosopher.messages.add(new Message(false, 0));
								Philosopher.messages.add(new Message(true, 3));
								System.out.println("Added: " + Philosopher.messages.size());
							}
							Philosopher.forwardPending = true;
						}
						break;
					default:
						System.out.println("WHAT THE HEY?");
					}
				}
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
}
