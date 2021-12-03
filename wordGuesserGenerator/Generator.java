package wordGuesserGenerator;

import java.util.HashSet;

/**
 * A class for generating guess trees.
 * @author libraun
 *
 */
public class Generator {
	
	private HashSet<String> words;
	private HeuristicGenerator gen;
	
	/**
	 * Constructs a generator from a word list.
	 * @param words The word list.
	 */
	public Generator(HashSet<String> words) {
		this.words = words;
		gen = new HeuristicGenerator((String[]) words.toArray(new String[words.size()]));
	}
	
	/**
	 * Generate multiple guess trees for the whole word list with certain heuristic parameters and return the best one.
	 * @param trials The number of trials to do.
	 * @param params The parameters for the heuristic generator.
	 * @return The root node best guess tree that was generated.
	 * @throws Exception If an error occurred while generating.
	 */
	public GeneratedGuessNode generateGuessTreeRandomized(int trials, HeuristicParams params) throws Exception {
		GeneratedGuessNode best = null;
		double bestScore = Double.POSITIVE_INFINITY;
		for (int i = 0; i < trials; i++) {
			GeneratedGuessNode myAlg = gen.generateForWordList(words, params);
			double avg = myAlg.calcAverage(words.toArray(new String[words.size()]));
			if (avg < bestScore) {
				bestScore = avg;
				best = myAlg.clone();
			}
		}
		
		return best;
	}

	/**
	 * For a guess node, generate a new node (and child nodes) with certain heuristic parameters.
	 * @param g The root node of the guess node to use.
	 * @param p The parameters for the heuristic generator.
	 * @return The root node of the guess subtree that was generated.
	 */
	public GeneratedGuessNode generateGuessNodeRandomized(GeneratedGuessNode g, HeuristicParams p) {
        HashSet<String> collated = collateWords(g);
		GeneratedGuessNode myAlg = gen.generateForWordList(collated, p);
		return myAlg;
	}

	/**
	 * For a guess node, generate a new node with certain heuristic parameters for all its children.
	 * @param g The root node of the guess node to use.
	 * @param p The parameters for the heuristic generator.
	 * @return The new guess node.
	 */
	public GeneratedGuessNode generateSubguessesFromGuess(GeneratedGuessNode g, HeuristicParams p) {
		GeneratedGuessNode node = new GeneratedGuessNode(g.guess);
		
        HashSet<String> collated = collateWords(g);
		HashSet<String>[] partition = HeuristicGenerator.computePartitions(collated, g.guess);
		for (int i = 0; i < partition.length; i++) {
			if (partition[i].size() > 0) {
				GeneratedGuessNode child = gen.generateForWordList(partition[i], p);
				node.setNthChild(i, child);
			} 
		}
		
		return node;
	}
	
	private HashSet<String> collateWords(GeneratedGuessNode root) {
		HashSet<String> list = new HashSet<String>();
	    if (words.contains(root.guess)) {
	        list.add(root.guess);
	    }
        for (int i = 0; i < 5; i++) {
	    	if (root.getChild(i) != null) {
	    		HashSet<String> subWords = collateWords(root.getChild(i));
		        for (String n : subWords) {
		        	list.add(n);
		        }
		    }
	    }
	    
	    return list;
	}

}
