package com.locator.bayes;

import android.app.Application;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.lang3.SerializationException;
import org.apache.commons.lang3.SerializationUtils;

public class DataHolder extends Application {

    private Sense wifiSense;

    private Sense cellularSense;

    public Sense getWifiSense() {
        return wifiSense;
    }

    public void setWifiSense(Sense wifiSense) {
        this.wifiSense = wifiSense;
    }

    public void addWifiSense(Sense sense) {
        wifiSense.addSense(sense);
    }

    public Sense getCellularSense() {
        return cellularSense;
    }

    public void setCellSense(Sense cellularSense) {
        this.cellularSense = cellularSense;
    }

    public void addCellularSense(Sense sense) {
        cellularSense.addSense(sense);
    }

    public void addAndStore(Sense wifi, Sense cellular) {
        if (wifi != null) {
            addWifiSense(wifi);
        }
        if (cellular != null) {
            addCellularSense(cellular);
        }
        storeRaw();
    }

    public void clearTrainingData() {
        wifiSense = new Sense();
        cellularSense = new Sense();
        storeRaw();
    }

    public void storeRaw() {
        try {
            FileOutputStream wifiStream = openFileOutput("wifi.raw", MODE_PRIVATE);
            wifiStream.write(SerializationUtils.serialize(wifiSense));
            wifiStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            FileOutputStream cellularStream = openFileOutput("cellular.raw", MODE_PRIVATE);
            cellularStream.write(SerializationUtils.serialize(cellularSense));
            cellularStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean loadRaw() {
        boolean success = true;
        try {
            FileInputStream wifiStream = openFileInput("wifi.raw");
            byte[] data = new byte[2*wifiStream.available()];
            wifiStream.read(data);
            wifiSense = SerializationUtils.deserialize(data);
            wifiStream.close();
        } catch (FileNotFoundException | SerializationException e) {
            wifiSense = new Sense();
            success = false;
        } catch (IOException e) {
            e.printStackTrace();
            success = false;
        }

        try {
            FileInputStream cellularStream = openFileInput("cellular.raw");
            byte[] data = new byte[2*cellularStream.available()];
            cellularStream.read(data);
            cellularSense = SerializationUtils.deserialize(data);
            cellularStream.close();
        } catch (FileNotFoundException | SerializationException e) {
            cellularSense = new Sense();
            success = false;
        } catch (IOException e) {
            e.printStackTrace();
            success = false;
        }

        return success;
    }

    public void storeDictStr() {
        try {
            FileOutputStream wifiStream = openFileOutput("wifi.dict", MODE_PRIVATE);
            wifiStream.write(wifiSense.toDictStr().getBytes());
            wifiStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            FileOutputStream cellularStream = openFileOutput("cellular.dict", MODE_PRIVATE);
            cellularStream.write(cellularSense.toDictStr().getBytes());
            cellularStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean exportRaw() {
        boolean success = true;
        File file;
        try {
            file = new File(getExternalFilesDir(null), "wifi.raw");
            FileOutputStream wifiStream = new FileOutputStream(file);
            wifiStream.write(SerializationUtils.serialize(wifiSense));
            wifiStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            success = false;
        }

        try {
            file = new File(getExternalFilesDir(null), "cellular.raw");
            FileOutputStream cellularStream = new FileOutputStream(file);
            cellularStream.write(SerializationUtils.serialize(cellularSense));
            cellularStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            success = false;
        }
        return success;
    }

    public boolean importRaw() {
        boolean success = true;
        File file;
        try {
            file = new File(getExternalFilesDir(null), "wifi.raw");
            FileInputStream wifiStream = new FileInputStream(file);
            byte[] data = new byte[2*wifiStream.available()];
            wifiStream.read(data);
            wifiSense = SerializationUtils.deserialize(data);
            wifiStream.close();
        } catch (FileNotFoundException | SerializationException e) {
            wifiSense = new Sense();
            success = false;
        } catch (IOException e) {
            e.printStackTrace();
            success = false;
        }

        try {
            file = new File(getExternalFilesDir(null), "cellular.raw");
            FileInputStream cellularStream = new FileInputStream(file);
            byte[] data = new byte[2*cellularStream.available()];
            cellularStream.read(data);
            cellularSense = SerializationUtils.deserialize(data);
            cellularStream.close();
        } catch (FileNotFoundException | SerializationException e) {
            cellularSense = new Sense();
            success = false;
        } catch (IOException e) {
            e.printStackTrace();
            success = false;
        }

        if (success) {
            storeRaw();
        }

        return success;
    }

    public boolean exportDictStr() {
        boolean success = true;
        File file;
        try {
            file = new File(getExternalFilesDir(null), "wifi.dict");
            FileOutputStream wifiStream = new FileOutputStream(file);
            wifiStream.write(wifiSense.toDictStr().getBytes());
            wifiStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            success = false;
        }

        try {
            file = new File(getExternalFilesDir(null), "cellular.dict");
            FileOutputStream cellularStream = new FileOutputStream(file);
            cellularStream.write(cellularSense.toDictStr().getBytes());
            cellularStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            success = false;
        }

        return success;
    }

    //TODO: We use this class both here and in ScanActivity and in LocateMeActivity, how can we use it only in one of these?

}