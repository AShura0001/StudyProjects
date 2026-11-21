package se325.lab01.exercise01.client;

import se325.lab01.exercise01.common.Employee;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class ClientMain {

    public static void main(String[] args) throws Exception {
        new ClientMain().start();
    }

    private void start() throws IOException {
        InetAddress serverAddress = InetAddress.getByName(Keyboard.prompt("Server address:"));
        int serverPort = Integer.parseInt(Keyboard.prompt("Server port:"));

        try (Socket socket = new Socket(serverAddress, serverPort)) {
            handleConnection(socket);
        }
    }

    private void handleConnection(Socket socket) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                while (true) {
                    printOptions();
                    int option = Integer.parseInt(Keyboard.prompt("Enter your choice:"));
                    switch (option) {
                        case 1:
                            addEmployee(out, in);
                            break;

                        case 2:
                            // TODO Print list of employees
                            break;
                    }
                }

            }
        }
    }

    private void addEmployee(ObjectOutputStream out, ObjectInputStream in) throws IOException {
        String name = Keyboard.prompt("Name:");
        String jobTitle = Keyboard.prompt("Job title:");
        int salary = Integer.parseInt(Keyboard.prompt("Salary:"));
        Employee employee = new Employee(name, jobTitle, salary);

        out.writeObject(employee);
        long id = in.readLong();

        System.out.println("Employee added (id = " + id + ")");
    }

    private void printOptions() {
        System.out.println("1. Add employee");
        System.out.println("2. Get list of employees");
    }
}
