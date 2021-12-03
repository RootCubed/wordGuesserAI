package wordGuesserGenerator;

import java.util.HashSet;
import java.util.Random;

/**
 * A class for generating guesses for the word guessing game heuristically.
 * 
 * @author libraun
 *
 */
public class HeuristicGenerator {
	private String[] words;
	// Speeds up heuristic by precomputing useful guesses.
	private String[] usefulTips;
	
	private Random r = new Random();
	
	/**
	 * Constructs the generator with a word list.
	 * @param words The word list to use.
	 */
	public HeuristicGenerator(String[] words) {
		this.words = words;
		generateUsefulTips();
	}
	
	// Hacky support for ä,ö,ü.
	private String transChar(char c) {
	    switch (c) {
	        case 'z' + 1:
	            return "ä";
	        case 'z' + 2:
	            return "ö";
	        case 'z' + 3:
	            return "ü";
	        case 'z' + 4:
	            return "";
	        default:
	            return "" + c;
	    }
	}
	
	private void generateUsefulTips() {
		// temporarily using HashSets because .contains can be done in log(n) time.
		HashSet<String> tmp = new HashSet<String>();
		for (char c1 = 'a'; c1 <= 'z' + 3; c1++) {
			for (char c2 = 'a'; c2 <= 'z' + 4; c2++) {
				for (char c3 = 'a'; c3 <= 'z' + 4; c3++) {
		            String tip = transChar(c1) + transChar(c2) + transChar(c3);
		            if (tmp.contains(tip)) continue;
		            int count = 0;
		            for (String w : words) {
		                if (w.contains(tip)) count++;
		            }
		            if (tip.length() == 3 && count <= 1) continue;
		            if (count > 1) {
		            	tmp.add(tip);
		            }
		        }
		    }
		}
		usefulTips = (String[]) tmp.toArray(new String[tmp.size()]);
	}
	
	/**
	 * Generate a guess tree heuristically.
	 * @param words The words to base the guess tree upon.
	 * @param params The parameters for the heuristic guesser.
	 * @return The root node of the resulting guess tree.
	 */
	public GeneratedGuessNode generateForWordList(HashSet<String> words, HeuristicParams params) {
		String bestGuess = heuristicForWordList(words, params);
		
		GeneratedGuessNode node = new GeneratedGuessNode(bestGuess);
		
		HashSet<String>[] partition = computePartitions(words, bestGuess);
		for (int i = 0; i < partition.length; i++) {
			if (partition[i].size() > 0) {
				GeneratedGuessNode child = generateForWordList(partition[i], params);
				node.setNthChild(i, child);
			}
		}
		
		return node;
	}

	/**
	 * Compute the partitioning of a certain guess on a word list.
	 * @param words The list of words to use.
	 * @param guess The guess to use.
	 * @return The resulting partitioning.
	 */
	public static HashSet<String>[] computePartitions(HashSet<String> words, String guess) {
		@SuppressWarnings("unchecked")
		HashSet<String>[] parts = new HashSet[5];
		for (int i = 0; i < 5; i++) parts[i] = new HashSet<String>();
	    for (String w : words) {
	        if (w == guess) continue;
	        if (w.contains(guess)) {
	            if (!w.startsWith(guess) && !w.endsWith(guess)) {
	            	parts[1].add(w);
	            } else {
	                if (w.startsWith(guess)) {
	                    if (w.startsWith(guess) && w.endsWith(guess)) {
	                    	parts[4].add(w);
	                    } else {
	                    	parts[2].add(w);
	                    }
	                } else {
	                	parts[3].add(w);
	                }
	            }
	        } else {
	        	parts[0].add(w);
	        }
	    }
	    return parts;
	}

	private String heuristicForWordList(HashSet<String> words, HeuristicParams params) {
		double[] meanGoals = params.defaultGoalWeights;
		if (r.nextDouble() > params.useDefaultGoalWeightsProb) {
			meanGoals = new double[] {r.nextDouble(), r.nextDouble(), r.nextDouble(), r.nextDouble(), r.nextDouble()};
		}
		if (words.size() <= 3) {
			// just use first word
			return words.iterator().next();
		}
		String bestTip = "";
		double bestTipWorstRed = 1;
		double bestTipDiffSum = Double.POSITIVE_INFINITY;

		int countTotal = words.size();
		for (String tip : usefulTips) {
			int countDoesntContain = 0;
    		int countOnlyCont = 0;
    		int countBegins = 0;
    		int countEnds = 0;
    		int countBeginsAndEnds = 0;
    		for (String w : words) {
    			if (w.contains(tip)) {
    				if (!w.startsWith(tip) && !w.endsWith(tip)) {
    					countOnlyCont++;
    				} else {
            			if (w.startsWith(tip)) {
            				if (w.startsWith(tip) && w.endsWith(tip)) {
                				countBeginsAndEnds++;
                			} else {
                				countBegins++;
                			}
            			} else {
            				countEnds++;
            			}
    				}
    			} else {
    				countDoesntContain++;
    			}
    		}

            int countOnes = 0;
            if (countDoesntContain == 1) countOnes++;
            if (countOnlyCont == 1) countOnes++;
            if (countBegins == 1) countOnes++;
            if (countEnds == 1) countOnes++;
            if (countBeginsAndEnds == 1) countOnes++;
            
    		double ratioNotCont = (double) countDoesntContain / countTotal;
    		double ratioCont = (double) countOnlyCont / countTotal;
    		double ratioBegins = (double) countBegins / countTotal;
    		double ratioEnds = (double) countEnds / countTotal;
    		double ratioBeginsAndEnds = (double) countBeginsAndEnds / countTotal;
    		
    		// worst outcome (percentage of how much remains)
    		double worstReduction = Math.max(
				Math.max(ratioNotCont, ratioCont),
				Math.max(Math.max(ratioBegins, ratioEnds), ratioBeginsAndEnds)
			);
    		
    		double[] ratios = new double[] {ratioNotCont, ratioCont, ratioBegins, ratioEnds, ratioBeginsAndEnds};

    		if ((countOnes >= 3 && words.size() < params.countOnesThreshold)) {
    			bestTipWorstRed = worstReduction;
    			bestTip = tip;
    			break;
    		}
    		
    		double reductionSum = 0;
    		for (int i = 0; i < ratios.length; i++) {
    			reductionSum += (ratios[i] - meanGoals[i]) * (ratios[i] - meanGoals[i]);
    		}
    		
    		if (reductionSum < bestTipDiffSum) {
    			bestTipWorstRed = worstReduction;
    			bestTipDiffSum = reductionSum;
    			bestTip = tip;
        	}
		}
		if (bestTipWorstRed > 0.95) { // something went wrong (e.g. duplicate words in list)
			return words.iterator().next();
		}
		return bestTip;
	}
}
