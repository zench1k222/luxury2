package dev.luxury.utils.managers;


import java.util.ArrayList;
import java.util.List;

public class FriendManager {
    private static FriendManager instance;
    private final List<String> friends = new ArrayList<>();

    public FriendManager() {}

    public static FriendManager getInstance() {
        if (instance == null) {
            instance = new FriendManager();
        }
        return instance;
    }

    public boolean addFriend(String name) {
        if (friends.contains(name.toLowerCase())) return false;
        friends.add(name.toLowerCase());
        return true;
    }

    public boolean removeFriend(String name) {
        return friends.remove(name.toLowerCase());
    }

    public boolean isFriend(String name) {
        return friends.contains(name.toLowerCase());
    }

    public List<String> getFriends() {
        return new ArrayList<>(friends);
    }

    public void setFriends(List<String> newFriends) {
        friends.clear();
        friends.addAll(newFriends);
    }

    public void clear() {
        friends.clear();
    }
}