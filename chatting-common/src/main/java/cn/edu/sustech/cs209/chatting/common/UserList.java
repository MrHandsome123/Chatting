package cn.edu.sustech.cs209.chatting.common;

import java.util.ArrayList;
import java.util.List;

public class UserList {

    List<String> list = new ArrayList<>();

    public void put(String userName) {
        list.add(userName);
    }

    public List<String> get() {
        return list;
    }
}
