package com.locator.bayes;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.SystemClock;
import android.support.annotation.RequiresApi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WiFiScanner {
    /**
     * The wifi manager.
     */
    private WifiManager wifiManager;

    /**
     * Scan Status (0: Started, 1: Finished and succeeded, -1: Finished and failed)
     */
    private int scanStatus;

    /**
     * Number of scans to be performed every time.The SSI values are averaged over these many scans/
     */
    private int numScans = 3;

    /**
     * Delay between successive scans (ms)
     */
    private int scanDelay = 200; // Original: 200

    private Context ctx;

    WifiScanReceiver wifiReceiver;

    public WiFiScanner(Context context, int scans) {
        // Set wifi manager.
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        numScans = scans;
        ctx = context;
        context.registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    public WiFiScanner(Context context) {
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        ctx = context;
        context.registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    private class WifiScanReceiver extends BroadcastReceiver {
        public void onReceive(Context c, Intent intent) {
            boolean success = intent.getBooleanExtra(
                    WifiManager.EXTRA_RESULTS_UPDATED, false);
            if (success) {
                scanStatus = 1;
            } else {
                scanStatus = -1;
            }
        }
    };

    public HashMap<String, Integer> scanAPs(int numNearest) {
        HashMap<String, Integer> apRssi = new HashMap<>();
        HashMap<String, Integer> apOcc = new HashMap<>();
        for (int i=0; i<numScans; i++) {
            wifiManager.startScan();
            SystemClock.sleep(scanDelay);
            List<ScanResult> scanResults = wifiManager.getScanResults();
            for (ScanResult AP: scanResults) {
                String bssid = AP.BSSID;
                Integer rssi = AP.level;
                apRssi.put(bssid, apRssi.getOrDefault(bssid, 0) + rssi);
                apOcc.put(bssid, apOcc.getOrDefault(bssid, 0) + 1);
            }
        }
        apRssi.replaceAll((k,v) -> v/apOcc.get(k));
        return filter(apRssi, numNearest);
    }

    public HashMap<String, Integer> scanAPs() {
        return scanAPs(-1);
    }

    public HashMap<String, Integer> filter(HashMap<String, Integer> unfiltered, int numNearest) {
        HashMap<String, Integer> temp = new HashMap<>();
        for (Map.Entry<String, Integer> entry: unfiltered.entrySet()) {
            String bssid = entry.getKey().toUpperCase();

            // Leave out randomized MACs
            if (bssid.charAt(1) == '2' || bssid.charAt(1) == '6' || bssid.charAt(1) == 'A' ||
                    bssid.charAt(1) == 'E') {
                continue;
            }

            // Leave out unusual vendors
            String vendor = getVendor(bssid);
            if (vendor == "") {
                continue;
            }

            temp.put(bssid, entry.getValue());
        }

        if (numNearest == -1) {
            return temp;
        }

        HashMap<String, Integer> filtered = new HashMap<>();
        for (int i=0; i<numNearest; i++) {
            int maxRssi = -120;
            String bssid = "";
            for (Map.Entry<String, Integer> entry: temp.entrySet()) {
                if (entry.getValue() > maxRssi && !filtered.containsKey(entry.getKey())) {
                    maxRssi = entry.getValue();
                    bssid = entry.getKey();
                }
            }
            filtered.put(bssid, maxRssi);
        }
        return filtered;
    }

    private String getVendor(String bssid) {
        int id = ctx.getResources().getIdentifier(
                "X" + bssid.substring(0, 8).replace(":", "_"),
                "string", ctx.getPackageName());
        if (id == 0) {
            return "";
        }
        else {
            return ctx.getString(id);
        }
    }
}
