//EIDS=KPP446,JC82563
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ProductHandler {
    
    Map<String,Integer> inventory; //Map Product->Quantity
    Map<Integer,Order> allOrders = new HashMap(); //Map Order Number->Order
    int nextId=1;

    public ProductHandler(Map<String, Integer> inventory) {
        this.inventory = inventory;
    }
    
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
}
