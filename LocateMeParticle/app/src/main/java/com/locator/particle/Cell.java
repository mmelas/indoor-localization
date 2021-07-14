package com.locator.particle;

import java.util.List;

public class Cell extends Rectangle {
    private boolean isNearWindow;

//    ((x,y), (x,y), (x, y), (x, y))
    public Cell(double coords[][], boolean isNearWindow) {
        super(coords);
        this.isNearWindow = isNearWindow;
    }

    boolean is_near_window() {
        return isNearWindow;
    }
}
