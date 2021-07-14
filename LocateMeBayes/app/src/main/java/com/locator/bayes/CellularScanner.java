package com.locator.bayes;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.TelephonyManager;

import java.util.HashMap;
import java.util.List;

public class CellularScanner {

    private TelephonyManager telManager;
    private Context ctx;

    /**
     * Number of scans to be performed every time.The SSI values are averaged over these many scans/
     */
    private int reps = 3;

    /**
     * Delay between successive scans (ms)
     */
    private int scanDelay = 500;

    public CellularScanner(Context context, int repetitions) {
        telManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        ctx = context;
        reps = repetitions;
    }

    public CellularScanner(Context context) {
        telManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        ctx = context;
    }

    public HashMap<String, Integer> scanNetworks() {
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }

        HashMap <String, Integer> cellStrentgh = new HashMap<>();
        HashMap <String, Integer> cellOcc = new HashMap<>();
        List<CellInfo> cellInfos = telManager.getAllCellInfo();
        for (int i=0; i < reps; i++) {
            for (CellInfo cellInfo : cellInfos) {
                int cellID = 0;
                int strength = 0;
                String cellIDStr = "";
                if (cellInfo instanceof CellInfoGsm) {
                    CellInfoGsm GSMInfo = (CellInfoGsm) cellInfo;
                    cellID = GSMInfo.getCellIdentity().getCid();
                    if (cellID == CellInfo.CONNECTION_UNKNOWN) {
                        continue;
                    }
                    strength = GSMInfo.getCellSignalStrength().getDbm();
                    cellIDStr = "GSM_" + cellID;
                } else if (cellInfo instanceof CellInfoLte) {
                    CellInfoLte LTEInfo = (CellInfoLte) cellInfo;
                    cellID = LTEInfo.getCellIdentity().getCi();
                    if (cellID == CellInfo.CONNECTION_UNKNOWN) {
                        continue;
                    }
                    strength = LTEInfo.getCellSignalStrength().getDbm();
                    cellIDStr = "LTE_" + cellID;
                } else if (cellInfo instanceof CellInfoWcdma) {
                    CellInfoWcdma WCDMAInfo = (CellInfoWcdma) cellInfo;
                    cellID = WCDMAInfo.getCellIdentity().getCid();
                    if (cellID == CellInfo.CONNECTION_UNKNOWN) {
                        continue;
                    }
                    strength = WCDMAInfo.getCellSignalStrength().getDbm();
                    cellIDStr = "WCDMA_" + cellID;
                }

                if (cellIDStr != "") {
                    cellStrentgh.put(cellIDStr, cellStrentgh.getOrDefault(cellIDStr, 0) + strength);
                    cellOcc.put(cellIDStr, cellOcc.getOrDefault(cellIDStr, 0) + 1);
                }
            }
            SystemClock.sleep(scanDelay);
        }
        cellStrentgh.replaceAll((k,v) -> v/cellOcc.get(k));
        return cellStrentgh;
    }
}
