import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Timer;

/**
 *
 * @author (your name here)
 *
 */
public class Philosopher {
	private static final int PORT_NUMBER = 8080;


	public static void main(String[] args) throws Exception {
		
		if (args.length != 2) {
			throw new Exception("Must pass in two IPs");
		}

		//create new instances of Client and Server
		Runnable r1 = new Client(PORT_NUMBER, args);
		Runnable r2 = new Server(PORT_NUMBER);


		//Create threads to run Client and Server as Threads
		Thread t1 = new Thread(r1);
		Thread t2 = new Thread(r2);

		//start the threads
		t1.start();
		t2.start();

	}

}

class Client implements Runnable{
	private int port;
	private String[] ipAddresses;
	
	public Client(int port, String[] ipAddresses) {
		this.port = port;
		this.ipAddresses = ipAddresses;
	}

	@Override
	public void run() {
		//all client code here
		//you should have a "left" client connection
		//and a "right" client connection
		Socket left = connect(0);
		Socket right = connect(1);
		
		System.out.println("We connected yo. Left: " + left.getInetAddress() + " Right: " + right.getInetAddress());
		
	}
	
	private Socket connect(int ipIndex) {
		Socket s = null;
		try {
			s = new Socket(ipAddresses[ipIndex], port);
		} catch (Exception e) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			return connect(ipIndex);
		}
		return s;
	}

}

class Server implements Runnable{
	private int port;
	
	public Server(int port) {
		this.port = port;
	}

	@Override
	public void run() {
		//all server code here
		//you should have a "left" server connection
		//and a "right" server connection
		ServerSocket listener = null;
		try {
			listener = new ServerSocket(port);
			while (true) {
				Socket socket = listener.accept();
				System.out.println("Accepted connection from: " + socket.getInetAddress());
				try {
					PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
					out.println(new Date().toString());
				} finally {
					socket.close();
				}
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
