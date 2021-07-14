package com.locator.bayes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class Table implements Serializable {
    private HashMap<String, HashMap<Integer, Integer>> tab;

    private int rssiHalfRange = 3;
    private int range = 3;

    private int normSamples = 100;

    public Table() {
        tab = new HashMap<>();
    }

    public void addOcc(String cellName, int rssi) {
        HashMap<Integer, Integer> cellRssiOcc = tab.getOrDefault(cellName, new HashMap<>());
        if (rssi != 0) {
            cellRssiOcc.put(rssi, cellRssiOcc.getOrDefault(rssi, 0) + 1);
        }
        tab.put(cellName, cellRssiOcc);
    }

    /**
     * Add the occurrences from the supplied table to those in the current table.
     * @param table
     */
    public void addTab(Table table) {
        for (Map.Entry<String, HashMap<Integer, Integer>> tabEntry: table.entrySet()) {
            if (!tab.containsKey(tabEntry.getKey())) {
                tab.put(tabEntry.getKey(), tabEntry.getValue());
            }
            else {
                HashMap<Integer, Integer> selfCellRssiOcc = tab.get(tabEntry.getKey());
                for (Map.Entry<Integer, Integer> cellRssiEntry: tabEntry.getValue().entrySet()) {
                    selfCellRssiOcc.put(cellRssiEntry.getKey(),
                            selfCellRssiOcc.getOrDefault(cellRssiEntry.getKey(), 0) +
                                    cellRssiEntry.getValue());
                }
                tab.put(tabEntry.getKey(), selfCellRssiOcc);
            }
        }
    }

    /**
     * Get the number of cells from the trained data.
     * @return
     */
    public int retrieveNumCells() {
        return retrieveCellNames().size();
    }

    /**
     * Get the names of cells from the trained table.
     * @return
     */
    public ArrayList<String> retrieveCellNames() {
        return new ArrayList<>(tab.keySet());
    }

    /**
     * Probability of getting the specified RSSI, in the entire location -> P(RSSI).
     * @param rssi
     * @return
     */
    public float p(int rssi) {
        int occRssi = 0;
        int occAll = 0;
        for (Map.Entry<String, HashMap<Integer, Integer>> tabEntry: tab.entrySet()) {
            int occCellRssi = 0;
            int occCellAll = 0;
            int cnt = 0;
            for (Map.Entry<Integer, Integer> cellRssiEntry: tabEntry.getValue().entrySet()) {
                occCellAll += cellRssiEntry.getValue();

                if ((rssi >= cellRssiEntry.getKey()-3) && (rssi <= cellRssiEntry.getKey()+3)) {
                    occCellRssi += cellRssiEntry.getValue();
                    cnt++;
                }
            }
            if (cnt != 0) {
                occCellRssi /= cnt;
            }

            // Consider the occurrences only if they appear in more than 10% of the scans
            if (occCellAll > normSamples/10) {
                //Normalize the specified number of samples.
                occRssi += (occCellRssi*normSamples)/occCellAll;
                occAll += normSamples;
            }
        }
        if (occAll == 0) {
            return 0;
        }
        return ((float)occRssi)/occAll;
    }

    /**
     * Probability of getting the specified RSSI, given the specified cell -> P(RSSI|Cell).
     * @param rssi
     * @param cellName
     * @return
     */
    public float p(int rssi, String cellName) {
        if (!tab.containsKey(cellName)) {
            return 0;
        }
        int occCellRssi = 0;
        int occCellAll = 0;
        int cnt = 0;
        for (Map.Entry<Integer, Integer> cellRssiEntry: tab.get(cellName).entrySet()) {
            occCellAll += cellRssiEntry.getValue();
            // Consider average of the rssi values within the specified range
            // to make it more 'continuous' rather than discrete where values can be missed
            if ((rssi >= cellRssiEntry.getKey()-range) && (rssi <= cellRssiEntry.getKey()+range)) {
                occCellRssi += cellRssiEntry.getValue();
                cnt++;
            }
        }
        if (cnt != 0) {
            occCellRssi /= cnt;
        }

        // Consider the occurrences only if the appear in more than 10% of the scans
        if (occCellAll > normSamples/10) {
            //Normalize the specified number of samples.
            occCellRssi = (occCellRssi*normSamples)/occCellAll;
        }
        else {
            occCellRssi = 0;
        }

        occCellAll = normSamples;

        return ((float)occCellRssi)/occCellAll;
    }

    /**
     * Probability of being in the specified cell (posterior), given the specified RSSI -> P(Cell|RSSI).
     * Prior for the cell is supplied.
     * @param cellName
     * @param rssi
     * @param prior
     * @return
     */
    public float p(String cellName, int rssi, float prior) {
        float temp = p(rssi);
        if (temp == 0) {
            return prior;
        }
        else {
            return prior * p(rssi, cellName) / p(rssi);
        }
    }

    /**
     * Probability of being in the specified cell (posterior), given the specified RSSI -> P(Cell|RSSI).
     * Number of cells is supplied. Prior is calculated as 1/(number of cells).
     * @param cellName
     * @param rssi
     * @param numCells
     * @return
     */
    public float p(String cellName, int rssi, int numCells) {
        return p(cellName, rssi, 1.0f/numCells);
    }

    /**
     * Probability of being in the specified cell (posterior), given the specified RSSI -> P(Cell|RSSI).
     * Number of cells is obtained from training data. Prior is calculated as 1/(number of cells).
     * @param cellName
     * @param rssi
     * @return
     */
    public float p(String cellName, int rssi) {
        return p(cellName, rssi, retrieveNumCells());
    }

    /**
     * Get posteriors for all cells, from the given RSSI and corresponding priors.
     * @param rssi
     * @param priors
     * @return
     */
    public HashMap<String, Float> ps(int rssi, HashMap<String, Float> priors) {
        HashMap<String, Float> posteriors = (HashMap<String, Float>) priors.clone();
        posteriors.replaceAll((k,v) -> p(k, rssi, v));
        float total = 0;
        for (Map.Entry<String, Float> entry: posteriors.entrySet()) {
            total += entry.getValue();
        }
        if (total != 0) {
            float finalTotal = total;
            posteriors.replaceAll((k, v) -> (float)v/ finalTotal);
        }
        return posteriors;
    }


    /**
     * Set the range for the table
     * @param curr_range
     * @return
     */
    public void setRange(int curr_range) {
        range = curr_range;
    }

    /**
     * Get posteriors for all cells, from the given RSSI. Cell names are retrieved from training data.
     * Priors are assumed to be equal for each cell.
     * @param rssi
     * @return
     */
    public HashMap<String, Float> ps(int rssi) {
        HashMap<String, Float> posteriors = new HashMap<>();
        ArrayList<String> cellNames = retrieveCellNames();
        float prior = 1.0f/cellNames.size();
        for (String cellName: cellNames) {
            posteriors.put(cellName, p(cellName, rssi, prior));
        }
        return posteriors;
    }

    public HashMap<String, HashMap<Integer, Integer>> getTab() {
        return tab;
    }

    public HashMap<Integer, Integer> get(String key) {
        return tab.getOrDefault(key, new HashMap<>());
    }

    public boolean containsKey(String key) {
        return tab.containsKey(key);
    }

    public Set<Map.Entry<String, HashMap<Integer, Integer>>> entrySet() {
        return tab.entrySet();
    }

    public Set<String> keySet() {
        return tab.keySet();
    }

    public Collection<HashMap<Integer, Integer>> values() {
        return tab.values();
    }

    public String toDictStr() {
        String dictStr = "{";
        for (Map.Entry<String, HashMap<Integer, Integer>> tabEntry: tab.entrySet()) {
            dictStr += ("\n\t\"" + tabEntry.getKey() + "\": {");
            for (Map.Entry<Integer, Integer> cellRssiEntry : tabEntry.getValue().entrySet()) {
                dictStr += ("\n\t\t" + String.valueOf(cellRssiEntry.getKey()) + ": " +
                        String.valueOf(cellRssiEntry.getValue()) + ",");
            }
            dictStr = dictStr.substring(0, dictStr.length()-1);
            dictStr += "\n\t},";
        }
        if (dictStr.charAt(dictStr.length()-1) == ',') {
            dictStr = dictStr.substring(0, dictStr.length() - 1);
        }
        dictStr += "\n}";
        return dictStr;
    }

}
