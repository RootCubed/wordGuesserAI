
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import wordGuesserGenerator.GeneratedGuessNode;

public class IntelligenterSpieler extends Spieler {
	
	ArrayList<String> remWords;
	
	DecimalFormat formatter = new DecimalFormat("##.#");
	
	private static GeneratedGuessNode guesser;
	private static int wordsHash = 0;
	private GeneratedGuessNode curr = null;
	
	@Override
	public void neuesSpiel(String[] verwendeteWoerter) {
		if (wordsHash != Arrays.hashCode(verwendeteWoerter)) {
			wordsHash = Arrays.hashCode(verwendeteWoerter);
			
			// Parse precomputed guesser
			// See the package wordGuesserGenerator for more information on how this works
			try {
				List<String> lines = Files.readAllLines(Paths.get("alg.txt"));
				
				guesser = GeneratedGuessNode.deserializeGuessTree((String[]) lines.toArray(new String[lines.size()]));
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		curr = guesser;
	}
	
	public String gibTipp() {
		if (curr == null) {
			System.out.println("My bot is bad!");
			return "";
		}
		return curr.guess;
    }
	
	@Override
	public void bekommeHinweis(String tipp, String hinweis) {
		if (hinweis.equals("ist")) return;
		
		if (hinweis.contains("nicht")) {
			curr = curr.notCont;
		} else if (hinweis.contains("und")) {
			curr = curr.begEnd;
		} else if (hinweis.contains("beginnt")) {
			curr = curr.begin;
		} else if (hinweis.contains("endet")) {
			curr = curr.end;
		} else if (hinweis.contains("enthält")) {
			curr = curr.onlyCont;
		}
	}
    
    public String name() {
        return "RootCubed";
    }
}
