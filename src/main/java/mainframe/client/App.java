package mainframe.client;

import mainframe.client.messages.Login;
import mainframe.client.messages.customer.Retrieve;
import mainframe.client.messages.customer.Search;
import mainframe.client.messages.customerEmail.List;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class App {
    public static void main(String[] args) throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Connection httpConnection = new HttpConnection("https://animating-eraser-arrogance.ngrok-free.dev/");
        Status status = httpConnection.Login("admin", "admin");
        Search searchCustomers = new Search(1, null, 1);
        searchCustomers.send(httpConnection);
        Search.Item[] ItemList = searchCustomers.getItems();
        for (Search.Item i: ItemList){
            System.out.println(i);
        }

        Retrieve retrieveCustomer = new Retrieve("01-0001-0000020");
        retrieveCustomer.send(httpConnection);

        for (Field field : retrieveCustomer.getClass().getDeclaredFields()){
            String fieldName = field.getName();

            // Construct the getter method name (e.g., "getName" or "getAge")
            String getterName = "get" + Character.toUpperCase(fieldName.charAt(1)) + fieldName.substring(2);

            // Retrieve and invoke the method
            Method getter = retrieveCustomer.getClass().getMethod(getterName);
            Object value = getter.invoke(retrieveCustomer);

            System.out.println(fieldName + ": " + value);
        }
    }
}
