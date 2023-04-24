package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Message;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ChattingService implements Runnable{
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String currentUser;
    private Server server;

    ChattingService(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run(){
        try {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)) ;
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

    public void doService() throws IOException {
        while(true){
            try{
                String command = in.readLine();
                if(command != null){
                    String[] s = command.split(",(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)",-1);
                    switch (s[0]){ // keyword of command
                        case "NewUser" :
                            if(server.clientList.stream().noneMatch(client -> client.currentUser.equals(s[1]))) {
                                currentUser = s[1];
                                server.clientList.add(this);
                                server.map.put(s[1], this);
                                out.println("Succeed" + server.clientList.size());
                                out.flush();
                            } else {
                                out.println("Fail");
                                out.flush();
                            }
                            break;
                        case "ShowAllUsers" :
                            String users = "";
                            for(ChattingService client : server.clientList) {
                                users += client.currentUser;
                                users += ",";
                            }
                            users = users.substring(0, users.length() - 1);
                            out.println(users);
                            out.flush();
                            break;
                        case "Message" :
                            // Privat Message like "Message,userA,uerB,content,timestamp"
                            // Room Message like "Message,userA,"userB,userC,...",content,timestamp"
                            String sendTo = s[2];
                            if(sendTo.charAt(0) == '"'){
                                sendTo = sendTo.substring(1, sendTo.length() - 1);
                            }
                            String[] destinations = sendTo.split(",");
                            for(String destination : destinations) {
                                if (destination.equals(currentUser)) {
                                    continue;
                                }
                                synchronized (server.map.get(destination)) {
                                    server.map.get(destination).out.println(command);
                                    server.map.get(destination).out.flush();
                                }
                            }
                            break;
                        case "Exit" :
                            if(server.clientList.contains(this)){
                                server.clientList.remove(this);
                            }
                            out.println("Exit");
                            out.flush();
                            break;
                    }
                }
            } catch (IOException ioe){
                System.out.println(currentUser + " has logged out");
                break;
            }
        }
    }
}