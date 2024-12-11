package epidemicModel2;

import java.util.ArrayList;
import java.util.List;

import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.SpatialMath;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.ContextUtils;
import repast.simphony.util.SimUtilities;
import repast.simphony.space.graph.Network;

public class Infected {

    private ContinuousSpace<Object> space;
    private Grid<Object> grid;
    private boolean allowRecovery;

    public Infected(ContinuousSpace<Object> space, Grid<Object> grid, boolean allowRecovery) {
        this.space = space;
        this.grid = grid;
        this.allowRecovery = allowRecovery;
    }

    @ScheduledMethod(start = 1, interval = 1)
    public void step() {
        // Get the grid location of this Infected individual
        GridPoint pt = grid.getLocation(this);

        // Create GridCells for the surrounding neighborhood
        GridCellNgh<Person> nghCreator = new GridCellNgh<>(grid, pt, Person.class, 1, 1);
        List<GridCell<Person>> gridCells = nghCreator.getNeighborhood(true);
        SimUtilities.shuffle(gridCells, RandomHelper.getUniform());

        GridPoint pointWithMostSusceptible = null;
        int maxCount = -1;
        for (GridCell<Person> cell : gridCells) {
            for (Object obj : grid.getObjectsAt(cell.getPoint().getX(), cell.getPoint().getY())) {
                if (obj instanceof Person person && person.getStatus() == Person.Status.SUSCEPTIBLE) {
                    if (cell.size() > maxCount) {
                        pointWithMostSusceptible = cell.getPoint();
                        maxCount = cell.size();
                    }
                }
            }
        }

        // Move towards the location with the most susceptible individuals
        if (pointWithMostSusceptible != null) {
            moveTowards(pointWithMostSusceptible);
        }

        // Spread infection
        infect();
    }

    public void moveTowards(GridPoint pt) {
        // Only move if we are not already in this grid location
        if (!pt.equals(grid.getLocation(this))) {
            NdPoint myPoint = space.getLocation(this);
            NdPoint otherPoint = new NdPoint(pt.getX(), pt.getY());
            double angle = SpatialMath.calcAngleFor2DMovement(space, myPoint, otherPoint);
            space.moveByVector(this, 1, angle, 0);
            myPoint = space.getLocation(this);
            grid.moveTo(this, (int) myPoint.getX(), (int) myPoint.getY());
        }
    }

//    public void infect() {
//        GridPoint pt = grid.getLocation(this);
//        List<Object> neighbors = new ArrayList<>();
//        for (Object obj : grid.getObjectsAt(pt.getX(), pt.getY())) {
//            if (obj instanceof Person person && person.getStatus() == Person.Status.SUSCEPTIBLE) {
//                neighbors.add(obj);
//            }
//        }
//
//        // Infect one susceptible neighbor
//        if (!neighbors.isEmpty()) {
//            int index = RandomHelper.nextIntFromTo(0, neighbors.size() - 1);
//            Person target = (Person) neighbors.get(index);
//            target.setStatus(Person.Status.INFECTED);
//        }
//
//        // Optional: Handle recovery if allowed
//        if (allowRecovery && Math.random() < 0.1) { // 10% chance of recovery per step
//            recover();
//        }
//    }
//    
    public void infect() {
        Context<Object> context = ContextUtils.getContext(this);
        Network<Object> network = (Network<Object>) context.getProjection("infection_network");

        // Spread infection to connected nodes
        for (Object neighbor : network.getAdjacent(this)) {
            if (neighbor instanceof Person person && person.getStatus() == Person.Status.SUSCEPTIBLE) {
                person.setStatus(Person.Status.INFECTED);

                // Optionally, log the infection
                System.out.println("Infected: " + person);
            }
        }

        // Recovery logic
        if (allowRecovery && Math.random() < 0.1) { // 10% chance of recovery
            recover();
        }
    }

    

    private void recover() {
        GridPoint pt = grid.getLocation(this);
        NdPoint spacePt = space.getLocation(this);
        Context<Object> context = ContextUtils.getContext(this);
        context.remove(this);
        
        // Add a new Recovered object at the same location
        Recovered recovered = new Recovered(space, grid);

//        // Add a recovered person at the same location
//        Person recovered = new Person(space, grid, allowRecovery);
//        recovered.setStatus(Person.Status.RECOVERED);
        context.add(recovered);
        space.moveTo(recovered, spacePt.getX(), spacePt.getY());
        grid.moveTo(recovered, pt.getX(), pt.getY());
    }
}
