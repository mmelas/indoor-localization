package com.example.example4;

import android.app.Application;

import java.util.ArrayList;
import java.util.List;
import com.example.example4.PosTrainActivity.Room;
import com.example.example4.MtnTrainActivity.Motion;

public class DataHolder extends Application {

    private List<Room> roomList = new ArrayList<>();

    public void setRoomList(List<Room> roomList) {
        this.roomList = roomList;
    }

    public List<Room> getRoomList() {
        return this.roomList;
    }

    private List<Motion> motionList = new ArrayList<>();

    public void setMotionList(List<Motion> motionList) {
        this.motionList = motionList;
    }

    public List<Motion> getMotionList() {
        return this.motionList;
    }

    //TODO: We use this class both here and in ScanActivity and in LocateMeActivity, how can we use it only in one of these?

}