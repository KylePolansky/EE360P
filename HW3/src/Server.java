import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class Server {
	static final boolean debug = false;
	public static final int UDPPacketSize = 65507;
	static ProductHandler productHandler;
    public static void main (String[] args) {
        //Input validation
    	if (args.length != 3) {
            System.out.println("ERROR: Provide 3 arguments");
            System.out.println("\t(1) <tcpPort>: the port number for TCP connection");
            System.out.println("\t(2) <udpPort>: the port number for UDP connection");
            System.out.println("\t(3) <file>: the file of inventory");

            System.exit(-1);
        }

        //Parse input params
        int tcpPort = Integer.parseInt(args[0]);
        int udpPort = Integer.parseInt(args[1]);
        String fileName = args[2];

        //Parse the inventory file
        Map<String,Integer> inventory = new HashMap();
        readInventory(fileName,inventory);
        productHandler=new ProductHandler(inventory);

        //Setup ServerSockets
	    Thread tcpThread = new Thread(new TcpServerRunnable(tcpPort));
	    tcpThread.setName("TCP Listener Thread");
	    tcpThread.start();

	    Thread udpThread = new Thread(new UdpServerRunnable(udpPort));
	    udpThread.setName("UDP Listener Thread");
	    udpThread.start();
    }

    static class TcpServerRunnable implements Runnable {
	    ServerSocket tcpSocket;
    	int port;
    	public TcpServerRunnable(int port) {
    		this.port = port;
	    }

	    @Override
	    public void run() {
		    openTcpServerSocket();
		    ExecutorService es = Executors.newCachedThreadPool();
		    while(true){
			    Socket clientSocket = null;
			    try {
				    clientSocket = tcpSocket.accept();
				    es.submit(new TcpSocketHandler(clientSocket));
			    } catch (IOException e) {
			    	System.out.println("Error accepting new client socket") ;
			    }
		    }
	    }

	    private void openTcpServerSocket() {
		    try {
			    tcpSocket = new ServerSocket(port);
		    } catch (IOException e) {
			    throw new RuntimeException("Cannot start TCP server", e);
		    }
	    }
    }

    static class TcpSocketHandler implements Runnable {
		Socket clientSocket;
    	public TcpSocketHandler(Socket clientSocket) {
    		this.clientSocket = clientSocket;
	    }

	    @Override
	    public void run() {
		    try {
		    	//Get socket streams
			    try (InputStreamReader inputReader = new InputStreamReader(clientSocket.getInputStream())) {
			    	try (BufferedReader input = new BufferedReader(inputReader)) {
			    		try (DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream())) {
			    			while (clientSocket.isConnected()) {
							    //Read/write streams
							    String inputString = input.readLine();
							    if (debug) System.out.println("DEBUG: TCP Command Input: " + inputString);
							    output.writeBytes(handleRequest(inputString));
							    output.flush();
							    if (debug) System.out.println("DEBUG: Finished writingTCP Command Input: " + inputString);
						    }
					    }
				    }
			    }
		    } catch (IOException e) {
			    System.out.println("IOException in TCP Server Thread.");
			    e.printStackTrace();
		    }
	    }
    }

	static class UdpServerRunnable implements Runnable {
		DatagramSocket udpSocket;
		int port;
		public UdpServerRunnable(int port) {
			this.port = port;
		}

		@Override
		public void run() {
			openUdpServerSocket();
			ExecutorService es = Executors.newCachedThreadPool();
			while(true){
				byte[] receiveBuffer = new byte[UDPPacketSize];
				DatagramPacket packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);
				try {
					udpSocket.receive(packet);
					es.submit(new UdpSocketHandler(udpSocket, packet));
				} catch (IOException e) {
					System.out.println("Error accepting new client socket") ;
				}
			}
		}

		private void openUdpServerSocket() {
			try {
				udpSocket = new DatagramSocket(port);
			} catch (IOException e) {
				throw new RuntimeException("Cannot start UDP server", e);
			}
		}
	}

	static class UdpSocketHandler implements Runnable {
		DatagramPacket packet;
		DatagramSocket datagramSocket;
		public UdpSocketHandler(DatagramSocket datagramSocket, DatagramPacket packet) {
			this.datagramSocket = datagramSocket;
			this.packet = packet;
		}

		@Override
		public void run() {
			//Get packet data
			String inputString = new String(packet.getData()).trim();
			if (debug) System.out.println("DEBUG: UDP Command Input: " + inputString);
			InetAddress clientIP = packet.getAddress();
			int clientPort = packet.getPort();

			//Get Response
			byte[] outputBytes = handleRequest(inputString).getBytes();

			//Create output packet
			DatagramPacket outPacket = new DatagramPacket(outputBytes, outputBytes.length, clientIP, clientPort);
			try {
				datagramSocket.send(outPacket);
			} catch (IOException e) {
				System.out.print("Error sending packet to client " + e.getStackTrace());
			}
		}
	}

    private static void readInventory(String fileName, Map<String,Integer> inventory)
    {
        try{
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            try {
                String line = br.readLine();

                while (line != null && !line.isEmpty()) {
                    String[] tokens = line.split(" ");
                    inventory.put(tokens[0], Integer.parseInt(tokens[1]));
                    line = br.readLine();
                }
            } finally {
                br.close();
            }
        }catch(FileNotFoundException e)
        {
            System.out.println("Inventory file not found.");
        }catch(IOException e){
            System.out.println("A problem occurred while closing the file.");
        }
    }
    
    private static String handleRequest(String cmd)
    {
        String response = null;
        try {
	        if (debug) response = "DEBUG: Command not processed: " + cmd;
	        String[] tokens = cmd.split(" ");
	        if (tokens[0].equals("purchase")) {
		        try {
			        int orderId = productHandler.purchase(tokens[1], tokens[2], Integer.parseInt(tokens[3]));
			        response = "Your order has been placed, " + orderId + " " + tokens[1] + " " + tokens[2] + " " + tokens[3] + "\n";
		        } catch (ProductHandler.ProductNotFoundException e) {
			        response = "Not Available - We do not sell this product\n";
		        } catch (ProductHandler.NotEnoughException e) {
			        response = "Not Available - Not enough items\n";
		        }
	        } else if (tokens[0].equals("cancel")) {
		        boolean found = productHandler.cancelOrder(Integer.parseInt(tokens[1]));
		        if (found)
			        response = "Order " + tokens[1] + " is canceled\n";
		        else
			        response = tokens[1] + " not found, no such order\n";
	        } else if (tokens[0].equals("search")) {
		        response = productHandler.getUserOrders(tokens[1]);
		        if (response == null)
			        response = "No order found for " + tokens[1] + "\n";
	        } else if (tokens[0].equals("list")) {
		        response = productHandler.getInventory();
	        } else {
		        System.out.println("ERROR: No such command");
		        System.out.println("DEBUG: " + cmd);
	        }
	        if (debug) System.out.println("DEBUG: Sending response: " + response);
        }
        finally {
        	if (response == null) response = "\n";
	        return response;
        }
    }
}
