package wordGuesserGenerator;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;

import java.awt.Color;

import javax.swing.JOptionPane;

public class GeneratorGUI {

	private JFrame frame;

	private int numThreads = 12;
	private static JLabel maxLabel;

	static GeneratorInterface[] gens;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					GeneratorGUI window = new GeneratorGUI();
					window.frame.setVisible(true);

					Timer t = new Timer();
					t.scheduleAtFixedRate(new TimerTask() {
						@Override
						public void run() {
							double bestGenAvg = Double.POSITIVE_INFINITY;
							for (GeneratorInterface gen : gens) {
								synchronized (gen) {
									if (gen.getBestAlgAvg() < bestGenAvg) {
										bestGenAvg = gen.getBestAlgAvg();
									}
								}
							}
							DecimalFormat df = new DecimalFormat("#.000");
							maxLabel.setText("Global best average: " + ((bestGenAvg == Double.POSITIVE_INFINITY) ? "(not started yet)" : df.format(bestGenAvg)));
						}
					}, 0, 50);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public GeneratorGUI() {
		String num = JOptionPane.showInputDialog("Enter number of threads to use:");
		numThreads = Integer.parseInt(num);
		gens = new GeneratorInterface[numThreads];
		for (int i = 0; i < gens.length; i++) {
			gens[i] = new GeneratorInterface();
		}
		List<String> fileLines;
		String[] words = null;
		try {
			fileLines = Files.readAllLines(Paths.get("woerter.txt"));
			words = new String[fileLines.size() - 1];
			for (int i = 1; i < fileLines.size(); i++) {
				words[i - 1] = fileLines.get(i);
			}
			for (GeneratorInterface gen : gens) {
				gen.setWordList(words);
			}
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		for (GeneratorInterface gen : gens) {
			gen.start();
		}
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 779, 533);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JPanel panel = new JPanel();
		frame.getContentPane().add(panel);
		panel.setLayout(new BorderLayout(0, 0));

		JPanel panel_4 = new JPanel();
		panel.add(panel_4, BorderLayout.SOUTH);
		panel_4.setLayout(new BorderLayout(0, 0));

		JButton btnNewButton_1 = new JButton("Start generator!");
		btnNewButton_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for (GeneratorInterface gen : gens) {
					synchronized (gen) {
						gen.toggleGenRunning();
						btnNewButton_1.setText(gen.isGenRunning() ? "Pause generator" : "Resume generator");
						gen.notify();
					}
				}
			}
		});
		panel_4.add(btnNewButton_1, BorderLayout.CENTER);

		JButton btnNewButton = new JButton("Save global best");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				double bestGen = Double.POSITIVE_INFINITY;
				GeneratorInterface bestGenObj = null;
				for (GeneratorInterface gen : gens) {
					synchronized (gen) {
						if (gen.getBestAlgAvg() < bestGen) {
							bestGen = gen.getBestAlgAvg();
							bestGenObj = gen;
						}
					}
				}
				System.out.println(bestGen);
				synchronized (bestGenObj) {
					bestGenObj.saveAlg();
					bestGenObj.notify();
				}
			}
		});
		panel_4.add(btnNewButton, BorderLayout.WEST);

		JButton btnNewButton_2 = new JButton("Restart worst half");
		btnNewButton_2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ArrayList<GeneratorInterface> bestGens = new ArrayList<GeneratorInterface>();
				ArrayList<Double> averages = new ArrayList<Double>();
				for (GeneratorInterface gen : gens) {
					synchronized (gen) {
						// insert sorted
						double avg = gen.getBestAlgAvg();
						boolean gotInserted = false;
						for (int i = 0; i < bestGens.size(); i++) {
							if (avg < averages.get(i)) {
								gotInserted = true;
								bestGens.add(i, gen);
								averages.add(i, avg);
								break;
							}
						}
						if (!gotInserted) {
							bestGens.add(gen);
							averages.add(avg);
						}
					}
				}
				for (int i = bestGens.size() / 2; i < bestGens.size(); i++) {
					GeneratorInterface gen = bestGens.get(i);
					synchronized (gen) {
						gen.newSeed();
						gen.notify();
					}
				}
			}
		});
		panel_4.add(btnNewButton_2, BorderLayout.EAST);

		JPanel panel_1 = new JPanel();
		panel.add(panel_1, BorderLayout.CENTER);
		panel_1.setLayout(new GridLayout(numThreads / 2, 1, 0, 0));

		int i = 0;
		for (GeneratorInterface gen : gens) {
			JPanel panel_2 = new JPanel();
			panel_1.add(panel_2, BorderLayout.CENTER);
			panel_2.setLayout(new BorderLayout(2, 2));

			JLabel title = new JLabel("Thread #" + ++i);
			panel_2.add(title, BorderLayout.NORTH);

			JTextPane txtpnStartGeneratorTo = new JTextPane();
			panel_2.add(txtpnStartGeneratorTo, BorderLayout.CENTER);
			txtpnStartGeneratorTo.setText("Start generator to see stats.");
			txtpnStartGeneratorTo.setEnabled(false);
			txtpnStartGeneratorTo.setEditable(false);

			gen.registerStatsPane(txtpnStartGeneratorTo);
		}

		JPanel panel_2 = new JPanel();
		panel_2.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK));
		panel_2.setBackground(new Color(250, 250, 250));
		panel.add(panel_2, BorderLayout.NORTH);
		maxLabel = new JLabel("Global best average: " + "(not started yet)");
		panel_2.add(maxLabel);
	}

}
