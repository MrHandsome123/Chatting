package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.UserList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Server {

    static final int ServerPort = 1234;
    List<String> userList;
    List<ChattingService> clientList;

    Server() {
        userList = new ArrayList<>();
        clientList = new ArrayList<>();
    }

    public static void main(String[] args) throws IOException {
        Server server = new Server();
        ServerSocket serverSocket = new ServerSocket(ServerPort);

        System.out.println("Starting server");
        System.out.println("Waiting for Clients to connect...");

        while(true) {
            Socket socket = serverSocket.accept();
            System.out.println("Client connected");
            ChattingService service = new ChattingService(socket, server);
            Thread thread = new Thread(service);
            thread.start();
        }
    }
}
