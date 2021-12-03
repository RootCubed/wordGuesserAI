package wordGuesserGenerator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

import javax.swing.JTextPane;

/**
 * A guess tree generator running on a thread.
 * Interfaces between the GUI and the heuristic generator.
 * 
 * @author libraun
 *
 */
public class GeneratorInterface extends Thread {
	private boolean stopThread = false;
	private boolean runGenerator = false;
	private boolean actionSaveAlg = false;
	private boolean actionNewSeed = false;
	
	private HashSet<String> words = null;
	private String[] wordArray = null; // duplicates are preserved
	
	private HeuristicParams params = new HeuristicParams();
	private int endPrePhaseIterations = 100; // Iterations for which to use default parameters
	private int endPrePhaseIterationsCurr = 0;
	
	private Generator g;
	private GeneratedGuessNode bestAlg = null;
	private double bestAlgAvg = Double.POSITIVE_INFINITY;
	
	private JTextPane statsPane;
	
	private String[] statsLines = new String[] {"", "", ""};
	private boolean needUpdateStats = false;
	
	private Random r = new Random();
	private DecimalFormat df = new DecimalFormat("##.000");
	
	/**
	 * Stops this thread.
	 */
	public synchronized void stopThread() {
		stopThread = true;
    }
	
	/**
	 * @return If the generator is initialized.
	 */
	public synchronized boolean isInitialized() {
		return words != null;
	}
    
	/**
	 * Toggle the state of the generator (started or not started/paused).
	 */
    public synchronized void toggleGenRunning() {
    	runGenerator = !runGenerator;
    	if (runGenerator) {
			statsPane.setEnabled(true);
			if (endPrePhaseIterationsCurr == 0) {
				statsPane.setText("Started generator!");
			}
    	}
    }
    /**
     * @return If the generator is running.
     */
    public synchronized boolean isGenRunning() {
		return runGenerator;
    }
    
    /**
     * Save the best guess tree.
     */
    public synchronized void saveAlg() {
    	actionSaveAlg = true;
    }

    /**
     * Reset this generator.
     */
    public synchronized void newSeed() {
    	actionNewSeed = true;
    	bestAlgAvg = Double.POSITIVE_INFINITY;
    	statsLines = new String[] {"", "", ""};
    	endPrePhaseIterationsCurr = 0;
    }
    
    /**
     * Sets the word list of the generator.
     * @param words The word list.
     */
    public synchronized void setWordList(String[] words) {
    	this.wordArray = words;
    	this.words = new HashSet<String>();
    	for (String w : words) {
    		this.words.add(w);
    	}
    }
    
    /**
     * @return Get the average of the best guess tree.
     */
    public synchronized double getBestAlgAvg() {
    	return bestAlgAvg;
    }

	private boolean firstTimeStep2 = true;
	
	@Override
	public void run() {
		super.run();
		
		while (!stopThread && words == null) {
			synchronized (this) {
				// wait till the a new action needs to be performed
				try {
					wait();
				} catch (InterruptedException e) { e.printStackTrace(); }
			}
		}
		
		g = new Generator(words);
		
		while (!stopThread) {
			if (!runGenerator && !actionSaveAlg && !actionNewSeed) {
				synchronized (this) {
					try {
						// wait till the a new action needs to be performed
						wait();
					} catch (InterruptedException e) { e.printStackTrace(); }
				}
			}
			if (actionSaveAlg) {
				if (bestAlg != null) {
					String serTree = bestAlg.serializeGuessTree();
					try {
						PrintStream out = new PrintStream(new File("alg.txt"));
						out.print(serTree);
					} catch (FileNotFoundException e) { e.printStackTrace(); }
				}
				actionSaveAlg = false;
			}
			if (actionNewSeed) {
				bestAlgAvg = Double.POSITIVE_INFINITY;
				bestAlg = null;
				actionNewSeed = false;
			}
			if (runGenerator) {
				if (endPrePhaseIterationsCurr < endPrePhaseIterations) {
					endPrePhaseIterationsCurr += 10;
					try {
						HeuristicParams p = new HeuristicParams();
						p.defaultGoalWeights = new double[] {r.nextDouble() * 0.3 + 0.1, r.nextDouble() * 0.3 + 0.1, r.nextDouble() * 0.3 + 0.1, r.nextDouble() * 0.3 + 0.1, r.nextDouble() * 0.3 + 0.1};
						p.useDefaultGoalWeightsProb = 1;
						p.countOnesThreshold = (int) (r.nextDouble() * 20);
						GeneratedGuessNode node = g.generateGuessTreeRandomized(10, p);
						double avg = node.calcAverage(wordArray);
						if (avg < bestAlgAvg) {
							bestAlgAvg = avg;
							bestAlg = node;
							needUpdateStats = true;
						}
						statsLines[0] = "Generating initial starting algorithm... (" +
							endPrePhaseIterationsCurr +  "/" + endPrePhaseIterations + ", avg = " + df.format(bestAlgAvg) + ")";
						
					} catch (Exception e) {
						statsLines[2] = "Exception occured! " + e.getLocalizedMessage();
						e.printStackTrace();
						statsPane.setText(statsLines[0] + "\n" + statsLines[1] + "\n" + statsLines[2]);
						stopThread = true;
						continue;
					}
				} else {
					if (firstTimeStep2) {
						firstTimeStep2 = false;
						needUpdateStats = true;
					}
					statsLines[1] = "Iteratively improving algorithm...";
					try {
						// create a copy in case mutated tree has worse average
					    GeneratedGuessNode oldTree = bestAlg.clone();
				        double oldAvg = bestAlg.calcAverage(wordArray);
				        
				        // choose between re-generating and swapping
						iterativeImproveTree(bestAlg, r.nextDouble() > 0.5);
					    
				        double newAvg = bestAlg.calcAverage(wordArray);
				        if (oldAvg < newAvg) {
				        	bestAlg = oldTree.clone();
				        } else if (oldAvg > newAvg) {
				            statsLines[1] += "\nImprovement: " + df.format(oldAvg) + " -> " + df.format(newAvg);
				            needUpdateStats = true;
				            bestAlgAvg = newAvg;
				        }
					} catch (Exception e) {
						statsLines[2] = "Exception occured! " + e.getLocalizedMessage();
						e.printStackTrace();
						statsPane.setText(statsLines[0] + "\n" + statsLines[1] + "\n" + statsLines[2]);
						stopThread = true;
						continue;
					}
				}
				if (needUpdateStats) {
					statsPane.setText(statsLines[0] + "\n" + statsLines[1] + "\n" + statsLines[2]);
					needUpdateStats = false;
				}
			}
		}
	}
	
	private void iterativeImproveTree(GeneratedGuessNode root, boolean methodIsSwap) throws Exception {
        if (!methodIsSwap) {
    	    int num = (int) Math.floor(r.nextDouble() * 12 + 1);
            for (int i = 0; i < num; i++) {
        	    ArrayList<GeneratedGuessNode> list = getGuessList(root, true);
            	if (list.size() == 0) break;
                int index = (int) Math.floor(r.nextDouble() * list.size());
                GeneratedGuessNode thisNode = list.get(index);
                
                params.defaultGoalWeights = new double[] {r.nextDouble(), r.nextDouble(), r.nextDouble(), r.nextDouble(), r.nextDouble()};
                params.useDefaultGoalWeightsProb = r.nextDouble();
                params.countOnesThreshold = (int) Math.floor(r.nextDouble() * 25);
                GeneratedGuessNode newNode = g.generateGuessNodeRandomized(thisNode, params);
                
                thisNode.guess = newNode.guess;
                for (int n = 0; n < 5; n++) {
                	thisNode.setNthChild(n, newNode.getChild(n));
                }
                root.calcAverage(wordArray);
            }
        } else {
    	    ArrayList<GeneratedGuessNode> list = getGuessList(root, false);
            int index = (int) Math.floor(r.nextDouble() * list.size());
            GeneratedGuessNode thisNode = list.get(index);
            
            ArrayList<GeneratedGuessNode> possChildren = new ArrayList<GeneratedGuessNode>();
            
            for (int i = 0; i < 5; i++) {
            	if (thisNode.getChild(i) != null) {
            		possChildren.add(thisNode.getChild(i));
            	}
            }

            int swapIndex = (int) Math.floor(r.nextDouble() * possChildren.size());
            GeneratedGuessNode swapNode = possChildren.get(swapIndex);
            
            String tmp = thisNode.guess;
            thisNode.guess = swapNode.guess;
            swapNode.guess = tmp;
            
            GeneratedGuessNode newNode = g.generateSubguessesFromGuess(thisNode, params);
            thisNode.guess = newNode.guess;
            for (int n = 0; n < 5; n++) {
            	thisNode.setNthChild(n, newNode.getChild(n));
            }
        }
	}
	
	private ArrayList<GeneratedGuessNode> getGuessList(GeneratedGuessNode root, boolean includeFinal) {
	    ArrayList<GeneratedGuessNode> list = new ArrayList<GeneratedGuessNode>();
	    if (includeFinal || !(root.notCont == null && root.onlyCont == null && root.begin == null && root.end == null && root.begEnd == null)) {
		    list.add(root);
	    }
        for (int i = 0; i < 5; i++) {
	    	if (root.getChild(i) != null) {
		    	ArrayList<GeneratedGuessNode> subGuesses = getGuessList(root.getChild(i), includeFinal);
		        for (GeneratedGuessNode n : subGuesses) {
		        	list.add(n);
		        }
		    }
	    }

	    return list;
	}
	
	public void registerStatsPane(JTextPane p) {
		statsPane = p;
	}
}
