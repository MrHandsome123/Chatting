package cn.edu.sustech.cs209.chatting.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;

public class ClientThread implements Runnable{
    Socket socket;
    Controller controller;

    BufferedReader in;
    PrintWriter out;


    ClientThread(Socket socket, Controller controller) {
        this.socket = socket;
        this.controller = controller;
        try {
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try{
            try{
                doService();
            } finally {
                in.close();
                out.close();
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void doService() {
        while (true) {
            try{
                String command = in.readLine();
                if(command != null) {
                    String[] s = command.split(",");
                    switch (s[0]){
                        case "Exit":
                            break;
                    }
                } else {
                    Thread.sleep(50);
                }
            } catch(IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
