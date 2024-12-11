package epidemicModel2;

import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.grid.GridFactory;
import repast.simphony.context.space.grid.GridFactoryFinder;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.continuous.RandomCartesianAdder;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridBuilderParameters;
import repast.simphony.space.grid.SimpleGridAdder;
import repast.simphony.space.grid.WrapAroundBorders;
import repast.simphony.space.graph.Network;
import java.util.ArrayList;
import java.util.List;

public class EpidemicModel2Builder implements ContextBuilder<Object> {

    @Override
    public Context<Object> build(Context<Object> context) {
        context.setId("EpidemicModel2");

        // Create space and grid (as in your existing code)
        ContinuousSpaceFactory spaceFactory = ContinuousSpaceFactoryFinder.createContinuousSpaceFactory(null);
        ContinuousSpace<Object> space = spaceFactory.createContinuousSpace(
            "space", context, new RandomCartesianAdder<>(), 
            new repast.simphony.space.continuous.WrapAroundBorders(), 50, 50
        );

        GridFactory gridFactory = GridFactoryFinder.createGridFactory(null);
        Grid<Object> grid = gridFactory.createGrid(
            "grid", context, 
            new GridBuilderParameters<>(new WrapAroundBorders(), new SimpleGridAdder<>(), true, 50, 50)
        );

        // Retrieve parameters
        Parameters params = RunEnvironment.getInstance().getParameters();
        int susceptibleCount = (Integer) params.getValue("susceptible_count");
        int initialInfectedCount = (Integer) params.getValue("initial_infected_count");
        int recoveredCount = (Integer) params.getValue("recovered_count");
        boolean useSIR = (Boolean) params.getValue("use_SIR");

        // Add agents to context
        List<Object> agents = new ArrayList<>();
        for (int i = 0; i < susceptibleCount; i++) {
            Person person = new Person(space, grid, useSIR);
            context.add(person);
            NdPoint pt = space.getLocation(person);
            grid.moveTo(person, (int) pt.getX(), (int) pt.getY());
        }

        for (int i = 0; i < initialInfectedCount; i++) {
            Infected infected = new Infected(space, grid, useSIR);
            context.add(infected);
            NdPoint pt = space.getLocation(infected);
            grid.moveTo(infected, (int) pt.getX(), (int) pt.getY());
        }

        for (int i = 0; i < recoveredCount; i++) {
            Recovered recovered = new Recovered(space, grid);
            context.add(recovered);
            NdPoint pt = space.getLocation(recovered);
            grid.moveTo(recovered, (int) pt.getX(), (int) pt.getY());
        }

        // Create a network
        NetworkBuilder<Object> netBuilder = new NetworkBuilder<>("infection_network", context, true);
        Network<Object> network = netBuilder.buildNetwork();

        // Build a small-world network
        createSmallWorldNetwork(network, agents, 4, 0.1); // 4 neighbors, 10% rewiring probability

        // Align space and grid
        for (Object obj : context.getObjects(Object.class)) {
            NdPoint pt = space.getLocation(obj);
            grid.moveTo(obj, (int) pt.getX(), (int) pt.getY());
        }

        // Set simulation runtime
        if (RunEnvironment.getInstance().isBatch()) {
            RunEnvironment.getInstance().endAt(100); // Stop after 100 steps
        }

        return context;
    }

    private void createSmallWorldNetwork(Network<Object> network, List<Object> agents, int degree, double beta) {
        int numAgents = agents.size();

        // Create a ring lattice
        for (int i = 0; i < numAgents; i++) {
            for (int j = 1; j <= degree / 2; j++) {
                int neighborIndex = (i + j) % numAgents; // Wrap-around
                network.addEdge(agents.get(i), agents.get(neighborIndex));
            }
        }

        // Rewire edges with probability beta
        for (int i = 0; i < numAgents; i++) {
            for (int j = 1; j <= degree / 2; j++) {
                if (RandomHelper.nextDouble() < beta) {
                    int neighborIndex = (i + j) % numAgents;
                    network.removeEdge(network.getEdge(agents.get(i), agents.get(neighborIndex)));

                    // Add a new random edge
                    int newNeighborIndex;
                    do {
                        newNeighborIndex = RandomHelper.nextIntFromTo(0, numAgents - 1);
                    } while (newNeighborIndex == i || network.isAdjacent(agents.get(i), agents.get(newNeighborIndex)));

                    network.addEdge(agents.get(i), agents.get(newNeighborIndex));
                }
            }
        }
    }
}
