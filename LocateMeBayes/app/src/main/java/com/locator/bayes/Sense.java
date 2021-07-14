package com.locator.bayes;

import android.os.SystemClock;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Sense implements Serializable {

    private HashMap<String, Table> tabs;
    // String: BSSID
    // Table: Corresponding WiFi Table
            // tab: Key: Cell Name
            //      Value: HashMap of RSSI and occurrences


    public Sense() {
        tabs = new HashMap<>();
    }

    /**
     * Add the results of the scan in the specified cell to the relevant tables
     * @param cellName
     * @param apRssi
     */
    public void addCellScan(String cellName, HashMap<String, Integer> apRssi) {
        for (Map.Entry<String, Integer> entry: apRssi.entrySet()) {
            Table tab = tabs.getOrDefault(entry.getKey(), new Table());
            tab.addOcc(cellName, entry.getValue());
            tabs.put(entry.getKey(), tab);
        }
    }

    /**
     * Get the names of cells from the trained table.
     * @return
     */
    public ArrayList<String> retrieveCellNames() {
        ArrayList<String> temp = new ArrayList<>();
        for (Table tab: tabs.values()) {
            temp.addAll(tab.retrieveCellNames());
        }
        return (new ArrayList<String>(new HashSet(temp)));
    }

    /**
     * Add another training dataset to this dataset
     * @param sense
     */
    public void addSense(Sense sense) {
        for (Map.Entry<String, Table> entry: sense.entrySet()) {
            if (!tabs.containsKey(entry.getKey())) {
                tabs.put(entry.getKey(), entry.getValue());
            }
            else {
                Table tab = tabs.get(entry.getKey());
                tab.addTab(entry.getValue());
                tabs.put(entry.getKey(), tab);
            }
        }
    }

    /**
     * Probability of getting the specified RSSI for the specified bssid, in the entire location -> P(RSSI)
     * @param bssid
     * @param rssi
     * @return
     */
    public float p(String bssid, int rssi) {
        return tabs.get(bssid).p(rssi);
    }

    /**
     * Probability of getting the specified RSSI for the specified bssid, given the specified cell -> P(RSSI|Cell).
     * @param bssid
     * @param rssi
     * @param cellName
     * @return
     */
    public float p(String bssid, int rssi, String cellName) {
        return tabs.getOrDefault(bssid, new Table()).p(rssi, cellName);
    }

    /**
     * Probability of being in the specified cell (posterior) for the specified bssid, given the specified RSSI -> P(Cell|RSSI).
     * Prior for the cell is supplied.
     * @param bssid
     * @param cellName
     * @param rssi
     * @param prior
     * @return
     */
    public float p(String bssid, String cellName, int rssi, float prior) {
        return tabs.getOrDefault(bssid, new Table()).p(cellName, rssi, prior);
    }

    /**
     * Probability of being in the specified cell (posterior) for the specified bssid, given the specified RSSI -> P(Cell|RSSI).
     * Number of cells is supplied. Prior is calculated as 1/(number of cells).
     * @param bssid
     * @param cellName
     * @param rssi
     * @param numCells
     * @return
     */
    public float p(String bssid, String cellName, int rssi, int numCells) {
        return tabs.getOrDefault(bssid, new Table()).p(cellName, rssi, numCells);
    }

    /**
     * Probability of being in the specified cell (posterior) for the specified bssid, given the specified RSSI -> P(Cell|RSSI).
     * Number of cells is obtained from training data. Prior is calculated as 1/(number of cells).
     * @param bssid
     * @param cellName
     * @param rssi
     * @return
     */
    public float p(String bssid, String cellName, int rssi) {
        return tabs.getOrDefault(bssid, new Table()).p(cellName, rssi);
    }

    /**
     * Get posteriors for all cells for the specified bssid, from the given RSSI and corresponding priors.
     * @param bssid
     * @param rssi
     * @param priors
     * @return
     */
    public HashMap<String, Float> ps(String bssid, int rssi, HashMap<String, Float> priors) {
        return tabs.getOrDefault(bssid, new Table()).ps(rssi, priors);
    }

    /**
     * Get posteriors for all cells, from the given RSSI for the specified bssid. Cell names are retrieved from training data.
     * Priors are assumed to be equal for each cell.
     * @param bssid
     * @param rssi
     * @return
     */
    public HashMap<String, Float> ps(String bssid, int rssi) {
        HashMap<String, Float> priors = new HashMap<>();
        ArrayList<String> cellNames = retrieveCellNames();
        for (String key: cellNames) {
            priors.put(key, 1.0f/cellNames.size());
        }
        return tabs.getOrDefault(bssid, new Table()).ps(rssi, priors);
    }

    /**
     * Get posteriors for all cells for the specified bssid, from the given RSSI and corresponding priors.
     * @param scan
     * @param priorsList
     * @return
     */
    public ArrayList<HashMap<String, Float>> ps(HashMap<String, Integer> scan,
                                                ArrayList<HashMap<String, Float>> priorsList,
                                                boolean parallel) {
        ArrayList<HashMap<String, Float>> postsList = new ArrayList<>();
        for (HashMap<String, Float> priors: priorsList) {
            for (Map.Entry<String, Integer> scanEntry: scan.entrySet()) {
                HashMap<String, Float> posts = ps(scanEntry.getKey(), scanEntry.getValue(),
                        priors);
                if (parallel) {
                    postsList.add(posts);
                }
                else {
                    priors = posts;
                }
            }
            if (!parallel) {
                postsList.add(priors);
            }
        }
        return postsList;
    }

    public String probCellFromPosts(HashMap<String, Float> posteriors) {
        String cellName = "";
        float maxPost = 0;
        for (Map.Entry<String, Float> entry: posteriors.entrySet()) {
            // Only consider those with more than 15% confidence
            if (entry.getValue() >= maxPost && entry.getValue() > 0.15) {
                cellName = entry.getKey();
                maxPost = entry.getValue();
            }
        }
        return cellName;
    }

    /**
     * Get the most probable cell for a given BSSID and RSSI.
     * Cells are retrieved from the training data. Priors are assumed to be equal.
     * @param bssid
     * @param rssi
     * @return
     */
    public String probableCell(String bssid, int rssi) {
        HashMap<String, Float> priors = new HashMap<>();
        ArrayList<String> cellNames = retrieveCellNames();
        for (String key: cellNames) {
            priors.put(key, 1.0f/cellNames.size());
        }
        return probCellFromPosts(ps(bssid, rssi, priors));
    }

    /**
     * Get the most probable cell for a given BSSID and RSSI.
     * Cells and their corresponding priors are supplied.
     * @param bssid
     * @param rssi
     * @param priors
     * @return
     */
    public String probableCell(String bssid, int rssi, HashMap<String, Float> priors) {
        return probCellFromPosts(ps(bssid, rssi, priors));
    }

    public String probCellFromOcc(HashMap<String, Integer> occurrences) {
        String cellName = "";
        int maxOcc = 0;
        for (Map.Entry<String, Integer> entry: occurrences.entrySet()) {
            if (entry.getValue() >= maxOcc) {
                cellName = entry.getKey();
                maxOcc = entry.getValue();
            }
        }
        return cellName;
    }

    /**
     * Get the most probable cell according to multiple BSSIDs and their corresponding RSSIs.
     * Cells are retrieved from the training data. Priors are assumed to be equal.
     * @param cellScan
     * @param range
     * @return
     */
    public String probableCell(HashMap<String, Integer> cellScan, int range) {
        // Change the range of all tables according to the specified range
        for (Map.Entry<String, Table> t : tabs.entrySet()) {
            t.getValue().setRange(range);
        }
        // Number of times a cell is selected as the probable one (according to various BSSIDs)
        HashMap<String, Integer> cellOcc = new HashMap<>();
        for (Map.Entry<String, Integer> entry: cellScan.entrySet()) {
            String probCell = probableCell(entry.getKey(), entry.getValue());
            if (probCell != "") {
                cellOcc.put(probCell, cellOcc.getOrDefault(probCell, 0) + 1);
            }
        }
        return probCellFromOcc(cellOcc);
    }

    /**
     * Get the most probable cell according to multiple priors.
     * @param postsList
     * @return
     */
    public String probCellFromPostsList(ArrayList<HashMap<String, Float>> postsList) {
        // Number of times a cell is selected as the probable one (according to various BSSIDs)
        HashMap<String, Integer> cellOcc = new HashMap<>();
        for (HashMap<String, Float> posts: postsList) {
            String probCell = probCellFromPosts(posts);
            if (probCell != "") {
                cellOcc.put(probCell, cellOcc.getOrDefault(probCell, 0) + 1);
            }
        }
        return probCellFromOcc(cellOcc);
    }

    /**
     * Get the most probable cell according to multiple BSSIDs and their corresponding RSSIs.
     * Cells and their corresponding priors are supplied.
     * @param cellScan
     * @param priors
     * @return
     */
    public String probableCell(HashMap<String, Integer> cellScan, HashMap<String, Float> priors) {
        // Number of times a cell is selected as the probable one (according to various BSSIDs)
        HashMap<String, Integer> cellOcc = new HashMap<>();

        for (Map.Entry<String, Integer> entry: cellScan.entrySet()) {
            String probCell = probableCell(entry.getKey(), entry.getValue(), priors);
            cellOcc.put(probCell, cellOcc.getOrDefault(probCell, 0) + 1);
        }
        return probCellFromOcc(cellOcc);
    }

    public HashMap<String, Table> getTabs() {
        return tabs;
    }

    public Table get(String key) {
        return tabs.getOrDefault(key, new Table());
    }

    public boolean containsKey(String key) {
        return tabs.containsKey(key);
    }

    public Set<Map.Entry<String, Table>> entrySet() {
        return tabs.entrySet();
    }

    public Set<String> keySet() {
        return tabs.keySet();
    }

    public Collection<Table> values() {
        return tabs.values();
    }

    public String toDictStr() {
        String dictStr = "{";
        for (Map.Entry<String, Table> entry: tabs.entrySet()) {
            dictStr += ("\n\t\"" + entry.getKey() + "\": " + entry.getValue().toDictStr().replace("\n", "\n\t") + ",");
        }
        if (dictStr.charAt(dictStr.length()-1) == ',') {
            dictStr = dictStr.substring(0, dictStr.length() - 1);
        }
        dictStr += "\n}";
        return dictStr;
    }

    public boolean isTrained() {
        if (tabs.size() > 0) {
            return true;
        }
        return false;
    }
}