package epidemicModel2;

import java.util.List;

import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.ContextUtils;
import repast.simphony.util.SimUtilities;
import repast.simphony.space.graph.Network;

public class Person {

    public enum Status {
        SUSCEPTIBLE, INFECTED
    }

    private ContinuousSpace<Object> space;
    private Grid<Object> grid;
    private Status status;
    private boolean allowRecovery; // Flag to toggle SI or SIR behavior

    public Person(ContinuousSpace<Object> space, Grid<Object> grid, boolean allowRecovery) {
        this.space = space;
        this.grid = grid;
        this.status = Status.SUSCEPTIBLE;
        this.allowRecovery = allowRecovery;
    }

    @ScheduledMethod(start = 1, interval = 1)
    public void step() {
        // Check if infected, then spread the infection via network
        if (status == Status.INFECTED) {
            Context<Object> context = ContextUtils.getContext(this);
            Network<Object> network = (Network<Object>) context.getProjection("infection_network");

            // Spread infection to neighbors in the network
            for (Object neighbor : network.getAdjacent(this)) {
                if (neighbor instanceof Person person && person.getStatus() == Status.SUSCEPTIBLE) {
                    person.setStatus(Status.INFECTED);
                    System.out.println("Person infected: " + person);
                }
            }

            if (allowRecovery) {
                recover();
            }
        }

        // Optionally add spatial movement
        moveRandomly();
    }

    private void infectNeighbors() {
        // Get the grid location of this person
        GridPoint pt = grid.getLocation(this);

        // Use GridCellNgh to get neighboring cells
        GridCellNgh<Person> nghCreator = new GridCellNgh<>(grid, pt, Person.class, 1, 1);
        List<GridCell<Person>> gridCells = nghCreator.getNeighborhood(true);
        SimUtilities.shuffle(gridCells, RandomHelper.getUniform());

        // Attempt to infect susceptible neighbors
        for (GridCell<Person> cell : gridCells) {
            for (Object obj : grid.getObjectsAt(cell.getPoint().getX(), cell.getPoint().getY())) {
                if (obj instanceof Person) {
                    Person neighbor = (Person) obj;
                    if (neighbor.status == Status.SUSCEPTIBLE) {
                        if (Math.random() < 0.3) { // Infection probability
                            neighbor.status = Status.INFECTED;
                        }
                    }
                }
            }
        }
    }

    private void recover() {
        if (Math.random() < 0.1) { // Recovery probability
            // Get the current location of this person
            GridPoint pt = grid.getLocation(this);
            NdPoint spacePt = space.getLocation(this);
            Context<Object> context = ContextUtils.getContext(this);

            // Remove this person from the context
            context.remove(this);

            // Add a new Recovered object at the same location
            Recovered recovered = new Recovered(space, grid);
            context.add(recovered);
            space.moveTo(recovered, spacePt.getX(), spacePt.getY());
            grid.moveTo(recovered, pt.getX(), pt.getY());
        }
    }

    public void moveRandomly() {
        Context<Object> context = ContextUtils.getContext(this);
        if (context == null) {
            throw new IllegalStateException("Agent must be added to the context before moving.");
        }

        NdPoint myPoint = space.getLocation(this);
        double angle = RandomHelper.nextDoubleFromTo(0, 2 * Math.PI);
        space.moveByVector(this, 1, angle, 0);

        // Update grid location
        myPoint = space.getLocation(this);
        grid.moveTo(this, (int) myPoint.getX(), (int) myPoint.getY());
    }

    // Getter and Setter for status
    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
