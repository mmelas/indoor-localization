package com.example.example4;

import android.app.Application;

import java.util.ArrayList;
import java.util.List;
import com.example.example4.ScanActivity.Room;

public class DataHolder extends Application {

    private List<Room> roomList = new ArrayList<>();
    private List<Room> testList = new ArrayList<>();

    public void setRoomList(List<Room> roomList) {
        this.roomList = roomList;
    }

    public List<Room> getRoomList() {
        return this.roomList;
    }

    public void setTestList(List<Room> testList) {
        this.testList = testList;
    }

    public List<Room> getTestList() {
        return this.testList;
    }

    //TODO: We use this class both here and in ScanActivity and in LocateMeActivity, how can we use it only in one of these?

}