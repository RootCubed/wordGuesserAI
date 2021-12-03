package wordGuesserGenerator;

import java.util.ArrayList;

/**
 * Models a node in the guess tree.
 * Also provides functions for (de)serializing a guess tree from a file.
 * 
 * @author libraun
 *
 */
public class GeneratedGuessNode {
	
	/**
	 * The guess to be used if at this node.
	 */
	public String guess;
	/**
	 * The node to go to next if the guess did not occur in the word.
	 */
	public GeneratedGuessNode notCont;
	/**
	 * The node to go to next if the guess occured in the word, but not at the beginning or end.
	 */
	public GeneratedGuessNode onlyCont;
	/**
	 * The node to go to next if the guess occured at least at the beginning of the word (but not the end).
	 */
	public GeneratedGuessNode begin;
	/**
	 * The node to go to next if the guess occured at least at the end of the word (but not the beginning).
	 */
	public GeneratedGuessNode end;
	/**
	 * The node to go to next if the guess occured at least at the beginning and the end of the word.
	 */
	public GeneratedGuessNode begEnd;
	// private, so that the invariant children[i] == {notCont, onlyCont, begin, end, begEnd}[i] is guaranteed
	private GeneratedGuessNode[] children = new GeneratedGuessNode[] {notCont, onlyCont, begin, end, begEnd};
	
	/**
	 * Constructs a leaf guess node.
	 * @param guess The guess to use.
	 */
	public GeneratedGuessNode(String guess) {
		this(guess, null, null, null, null, null);
	}

	/**
	 * Constructs a guess node with supplied child nodes.
	 * @param guess The guess to use.
	 * @param notCont The child node for not containing the guess.
	 * @param onlyCont The child node for containing the guess, but not at the beginning or end.
	 * @param begin The child node for containing the guess at the beginning, but not the end
	 * @param end The child node for containing the guess at the end, but not the beginning
	 * @param begEnd The child node for containing the guess at the end and the beginning
	 */
	public GeneratedGuessNode(String guess,
			GeneratedGuessNode notCont, GeneratedGuessNode onlyCont,
			GeneratedGuessNode begin, GeneratedGuessNode end, GeneratedGuessNode begEnd) {
		this.guess = guess;
		this.notCont = notCont;
		this.begin = begin;
		this.end = end;
		this.begEnd = begEnd;
		children = new GeneratedGuessNode[] {notCont, onlyCont, begin, end, begEnd};
	}
	
	/**
	 * Creates and returns a copy of this object.
	 */
	@Override
	protected GeneratedGuessNode clone() {
		GeneratedGuessNode clone = new GeneratedGuessNode(guess); // this is fine because String is immutable
		for (int i = 0; i < children.length; i++) {
			if (children[i] != null) {
				clone.setNthChild(i, children[i].clone());
			}
		}
		return clone;
	}
	
	/**
	 * Serializes the guess tree starting from this node with ASCII encoding.
	 * @return A serialized version of the tree.
	 */
	public String serializeGuessTree() {
		ArrayList<String> lines = new ArrayList<String>();
		serializeGuessTree(lines);
		String treeStr = "";
		boolean isFirstLine = true;
		for (String line : lines) {
			treeStr += ((isFirstLine) ? "" : "\n") + line;
			isFirstLine = false;
		}
		return treeStr;
	}
	
	// Recursive helper function
	private int serializeGuessTree(ArrayList<String> lines) {
		int lineNum = lines.size();
		lines.add("");
		String currLine = this.guess;
		
		for (GeneratedGuessNode g : children) {
			if (g == null) {
				currLine += " -1";
			} else {
				currLine += " " + g.serializeGuessTree(lines);
			}
		}
		lines.set(lineNum, currLine);
		
		return lineNum;
	}
	
	/**
	 * Deserializes a guess tree.
	 * @param lines The lines of the text file.
	 * @return The root node of the tree.
	 */
	public static GeneratedGuessNode deserializeGuessTree(String[] lines) {
		return deserializeGuessTree(lines, 0);
	}

	// Recursive helper function
	private static GeneratedGuessNode deserializeGuessTree(String[] lines, int line) {
		String[] lineEls = lines[line].split(" ");
		
	    GeneratedGuessNode node = new GeneratedGuessNode(lineEls[0]);
	    for (int i = 1; i < lineEls.length; i++) {
	    	int num = Integer.parseInt(lineEls[i]);
	    	if (num > -1) {
	    		node.setNthChild(i - 1, deserializeGuessTree(lines, num));
	    	}
	    }

	    return node;
	}

	/**
	 * Deterministically calculates the average number of guesses the guess tree takes, starting from this node.
	 * @param wordArray The word list to use. Must be the same one that was used to generate the guess tree.
	 * @return The average number of guesses the guess tree needed.
	 * @throws Exception If a word was not found with the guess tree.
	 */
	public double calcAverage(String[] wordArray) throws Exception {
		int sum = 0;
	    int count = 0;
	    for (String w : wordArray) {
	        int g = simGuess(w);
	        if (g == -1) {
	        	simGuess(w);
	        	throw new Exception("Word " + w + " not found. This probably means my code is bad!");
	        }
	        sum += g;
	        count++;
	    }
	    return (double) sum / count;
	}
	
	// count guesses for a certain word
	private int simGuess(String word) {
	    int count = 0;
	    GeneratedGuessNode curr = this;
	    while (curr != null) {
	        count++;
	        String guess = curr.guess;
	        if (word.equals(guess)) return count;
	        if (word.contains(guess)) {
	            if (!word.startsWith(guess) && !word.endsWith(guess)) {
	                curr = curr.onlyCont;
	            } else {
	                if (word.startsWith(guess)) {
	                    if (word.startsWith(guess) && word.endsWith(guess)) {
	                        curr = curr.begEnd;
	                    } else {
	                        curr = curr.begin;
	                    }
	                } else {
	                    curr = curr.end;
	                }
	            }
	        } else {
	            curr = curr.notCont;
	        }
	    }
	    return -1;
	}
	
	/**
	 * Get the i-th child of the node.
	 * @param i
	 * @return The requested child node, or null if out of bounds.
	 */
	public GeneratedGuessNode getChild(int i) {
		if (i < 0 || i >= 5) return null;
		return children[i];
	}

	/**
	 * Set the i-th child of the node. Do nothing if out of bounds.
	 * @param i
	 * @param n The child node to set it to.
	 */
	public void setNthChild(int i, GeneratedGuessNode n) {
		children[i] = n;
		switch (i) {
		case 0:
			notCont = n;
			break;
		case 1:
			onlyCont = n;
			break;
		case 2:
			begin = n;
			break;
		case 3:
			end = n;
			break;
		case 4:
			begEnd = n;
			break;
		}
	}
}
