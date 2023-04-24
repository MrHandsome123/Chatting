package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.UserList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Server {

    static final int ServerPort = 1234;
    List<ChattingService> clientList;
    Map<String, ChattingService> map;

    Server() {
        clientList = new ArrayList<>();
        map = new HashMap<>();
    }

    public static void main(String[] args) throws IOException {
        Server server = new Server();
        ServerSocket serverSocket = new ServerSocket(ServerPort);

        System.out.println("Starting server");
        System.out.println("Waiting for Clients to connect...");

        while (true) {
            Socket socket = serverSocket.accept();
            System.out.println("Client connected");
            ChattingService service = new ChattingService(socket, server);
            Thread thread = new Thread(service);
            thread.start();
        }
    }
}
