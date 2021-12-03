package wordGuesserGenerator;

/**
 * Class for holding parameters for the heuristic guesser
 * 
 * @author libraun
 *
 */
public class HeuristicParams {
	/**
	 * How the remaining words should ideally get partitioned into the child nodes
	 */
	public double[] defaultGoalWeights = new double[] {0.2, 0.2, 0.2, 0.2, 0.2};
	/**
	 * With what probability the heuristic guesser should pick different goal weights for any given round.
	 * If this occurs, the new weights will all be a value between 0 and 1.
	 */
	public double useDefaultGoalWeightsProb = 0.2;
	
	/**
	 * If at least three partitions contain exactly one word, what the maximum number of words is to use that guess instead.
	 */
	public int countOnesThreshold = 5;
}
