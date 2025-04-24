package alp.solver;

import alp.model.ALPInstance;
import alp.model.ALPSolution;

/**
 * Interface for ALP solvers.
 */
public interface ALPSolver {
    
    /**
     * Solves the given ALP instance.
     * 
     * @param instance The ALP instance to solve
     * @return The computed solution
     */
    ALPSolution solve(ALPInstance instance);
    
    /**
     * Returns the name of this solver.
     */
    String getName();
}
