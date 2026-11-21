package se325.lab01.exercise01.server;

import se325.lab01.exercise01.common.Employee;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ServerMain {

    // Application port - Change if required on your machine, or
    // set to 0 to pick a random unused port.
    private static final int PORT = 8080;

    private long nextId = 1;
    private final List<Employee> employeeDatabase = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        new ServerMain().start();
    }

    public void start() throws IOException, ClassNotFoundException {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            InetAddress serverHost = InetAddress.getLocalHost();
            System.out.println("Server destination: " + serverHost.getHostAddress() + ":" + serverSocket.getLocalPort());

            while (true) {
                try (Socket clientConnection = serverSocket.accept()) {
                    handleClient(clientConnection);
                }
            }
        }
    }

    private void handleClient(Socket clientConnection) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(clientConnection.getInputStream())) {
            try (ObjectOutputStream out = new ObjectOutputStream(clientConnection.getOutputStream())) {
                while (true) {

                    Employee employee = (Employee) in.readObject();

                    addEmployee(employee, out);


                    out.flush();
                }
            }
        }
    }

    private void addEmployee(Employee employee, ObjectOutputStream out) throws IOException {
        employee.setId(nextId);
        nextId++;
        employeeDatabase.add(employee);

        out.writeLong(employee.getId());
    }
}
