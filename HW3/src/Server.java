
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Server {
    static ProductHandler productHandler;
    public static void main (String[] args) {
        int tcpPort;
        int udpPort;
        if (args.length != 3) {
            System.out.println("ERROR: Provide 3 arguments");
            System.out.println("\t(1) <tcpPort>: the port number for TCP connection");
            System.out.println("\t(2) <udpPort>: the port number for UDP connection");
            System.out.println("\t(3) <file>: the file of inventory");

            System.exit(-1);
        }
        tcpPort = Integer.parseInt(args[0]);
        udpPort = Integer.parseInt(args[1]);
        String fileName = args[2];

        // parse the inventory file
        Map<String,Integer> inventory = new HashMap();
        readInventory(fileName,inventory);
        productHandler=new ProductHandler(inventory);
        // TODO: handle request from clients
        
        
        Thread tcpThread = new Thread(new Runnable(){
            @Override
            public void run() {
                String clientSentence;
                ServerSocket welcomeSocket;
                try{
                    welcomeSocket = new ServerSocket(tcpPort);
                
                    while(true)
                    {
                       try{
                            Socket connectionSocket = welcomeSocket.accept();
                            BufferedReader inFromClient =
                                new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
                            DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
                            clientSentence = inFromClient.readLine();
                            String response;
                            //DO SOMETHING WITH MESSAGE
                                                        
                            response=handleRequest(clientSentence);
                            //Send response to client
                            outToClient.writeBytes(response);
                       }catch(IOException e){
                           System.out.println("IOException in TCP Server Thread.");
                           e.printStackTrace();
                       }
                    }
                }catch(IOException e){
                    System.out.println("Failed to start TCP server in port "+tcpPort);
                }
            }  
        });
        
        Thread udpThread = new Thread(new Runnable(){
            @Override
            public void run() {
                try{
                    DatagramSocket serverSocket = new DatagramSocket(udpPort);
                    byte[] receiveData = new byte[1024];
                    byte[] sendData = new byte[1024];
                
                    while(true)
                    {
                       try{
                            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                            serverSocket.receive(receivePacket);
                            String clientSentence = new String( receivePacket.getData());

                            InetAddress IPAddress = receivePacket.getAddress();
                            int port = receivePacket.getPort();
                            String response=handleRequest(clientSentence);
                            sendData = response.getBytes();
                            DatagramPacket sendPacket =
                            new DatagramPacket(sendData, sendData.length, IPAddress, port);
                            serverSocket.send(sendPacket);

                       }catch(IOException e){
                           System.out.println("IOException in TCP Server Thread.");
                           e.printStackTrace();
                       }
                    }
                }catch(IOException e){
                    System.out.println("Failed to start TCP server in port "+tcpPort);
                }
            }  
        });
        
        tcpThread.start();
        udpThread.start();
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
        String response=null;
        String[] tokens = cmd.split(" ");
            if (tokens[0].equals("purchase")) {
                try{
                    int orderId=productHandler.purchase(tokens[1], tokens[2], Integer.parseInt(tokens[3]));
                    response="You order has been placed, "+orderId+" "+tokens[1]+" "+tokens[2]+" "+tokens[3];
                }catch(ProductHandler.ProductNotFoundException e){
                    response="Not Available - We do not sell this product";
                }catch(ProductHandler.NotEnoughException e){
                    response="Not Available - Not enough items";
                }
            } else if (tokens[0].equals("cancel")) {
                boolean found = productHandler.cancelOrder(Integer.parseInt(tokens[1]));
                if(found)
                    response="Order "+tokens[1]+" is canceled";
                else
                    response=tokens[1]+" not found, no such order";
            } else if (tokens[0].equals("search")) {
                response=productHandler.getUserOrders(tokens[1]);
                if(response==null)
                    response="`No order found for "+tokens[1];
            } else if (tokens[0].equals("list")) {
                response=productHandler.getInventory();
            } else {
                System.out.println("ERROR: No such command");
                System.out.println("DEBUG: "+cmd);
            }
        return response;
    }
}
