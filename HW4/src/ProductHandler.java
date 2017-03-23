import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ProductHandler {

	Map<String,Integer> inventory = new HashMap<>(); //Map Product->Quantity
	Map<Integer,Order> allOrders = new HashMap(); //Map Order Number->Order
	int nextId=1;

	public synchronized int purchase(String user, String product, int quantity) throws ProductNotFoundException,NotEnoughException
	{
		if(!inventory.containsKey(product))
			throw new ProductNotFoundException();
		if(inventory.get(product)<quantity)
			throw new NotEnoughException();

		inventory.put(product,inventory.get(product)-quantity);

		int orderId = getOrderId();
		Order order = new Order(user,orderId,product,quantity);
		allOrders.put(orderId,order);
		return orderId;
	}

	public synchronized boolean cancelOrder(int orderId){
		Order order = allOrders.remove(orderId);
		if(order==null)
			return false;
		inventory.put(order.getProductName(), inventory.get(order.getProductName())+order.getQuantity());
		return true;
	}

	public synchronized String getInventory() {
		StringBuilder sb = new StringBuilder();
		for(Entry<String,Integer> e : inventory.entrySet())
		{
			sb.append(e.getKey());
			sb.append(" ");
			sb.append(e.getValue());
			sb.append("\n");
		}
		return sb.toString();
	}

	public synchronized String getUserOrders(String user)
	{
		StringBuilder sb = new StringBuilder();
		for(Order order : allOrders.values())
		{
			if(order.getUser().equals(user))
			{
				sb.append(order.getId());
				sb.append(", ");
				sb.append(order.getProductName());
				sb.append(", ");
				sb.append(order.getQuantity());
				sb.append("\n");
			}
		}
		if(sb.toString().isEmpty())
			return null;
		return sb.toString();
	}

	private synchronized int getOrderId() { return nextId++; }

	public static class ProductNotFoundException extends Exception{ };
	public static class NotEnoughException extends Exception{ };

	public static class Order{
		String user;
		int id;
		String productName;
		int quantity;

		public Order(String user, int id, String productName, int quantity) {
			this.user = user;
			this.id = id;
			this.productName = productName;
			this.quantity = quantity;
		}

		public String getUser() {
			return user;
		}

		public void setUser(String user) {
			this.user = user;
		}

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public String getProductName() {
			return productName;
		}

		public void setProductName(String productName) {
			this.productName = productName;
		}

		public int getQuantity() {
			return quantity;
		}

		public void setQuantity(int quantity) {
			this.quantity = quantity;
		}
	}

	public String handleRequest(String cmd)
	{
		String response = null;
		try {
			String[] tokens = cmd.split(" ");
			if (tokens[0].equals("purchase")) {
				try {
					int orderId = purchase(tokens[1], tokens[2], Integer.parseInt(tokens[3]));
					response = "Your order has been placed, " + orderId + " " + tokens[1] + " " + tokens[2] + " " + tokens[3] + "\n";
				} catch (ProductHandler.ProductNotFoundException e) {
					response = "Not Available - We do not sell this product\n";
				} catch (ProductHandler.NotEnoughException e) {
					response = "Not Available - Not enough items\n";
				}
			} else if (tokens[0].equals("cancel")) {
				boolean found = cancelOrder(Integer.parseInt(tokens[1]));
				if (found)
					response = "Order " + tokens[1] + " is canceled\n";
				else
					response = tokens[1] + " not found, no such order\n";
			} else if (tokens[0].equals("search")) {
				response = getUserOrders(tokens[1]);
				if (response == null)
					response = "No order found for " + tokens[1] + "\n";
			} else if (tokens[0].equals("list")) {
				response = getInventory();
			} else {
				System.out.println("ERROR: No such command");
				System.out.println("DEBUG: " + cmd);
			}
		}
		finally {
			if (response == null) response = "\n";
			return response;
		}
	}

	public void readInventory(String fileName)
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
}
