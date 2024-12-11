package epidemicModel2;

import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;

public class Recovered {

    private ContinuousSpace<Object> space;
    private Grid<Object> grid;

    public Recovered(ContinuousSpace<Object> space, Grid<Object> grid) {
        this.space = space;
        this.grid = grid;
    }

    // Add any behaviors for recovered agents here, if needed
}
