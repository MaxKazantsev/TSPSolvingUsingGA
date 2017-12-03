package edu.eom;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.Scanner;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.FileChooser;

public class SceneController implements Initializable{

	@FXML
	private Button openFileButton;
	@FXML
	private Button calculateButton;
	@FXML
	private TextField epochNumTF;
	@FXML
	private TextField popSizeTF;
	@FXML
	private TextField mutProbTF;
	@FXML
	private TextArea logArea;
	@FXML
    private FileChooser fileChooser = new FileChooser();
	@FXML
	private File file;
	@FXML
	private Pane optimalGraphPane;
	@FXML
	private Pane currentGraphPane;
	@FXML
	private ProgressBar progressBar;
	@FXML
	private ScrollBar scrollBar;
	@FXML
	private Label fitLabel;
	
	private int[][] fullMatrix;
	private double[][] coordinatesMatrix;
	private int[] bestTourArray;
	private Random rnd = new Random();
	private int epochNum;
	private int popSize;
	private double mutProb;
	private int[][] population;
	private Task<GeneticAlgorithmResults> task;
	private GeneticAlgorithmResults results;

	@FXML
	private void calculateButtonHandler(ActionEvent event) throws FileNotFoundException, UnsupportedEncodingException {
		
		if(popSizeTF.getText().trim().isEmpty() ||  epochNumTF.getText().trim().isEmpty() || 
				mutProbTF.getText().trim().isEmpty()) {
			logArea.appendText("\nSome of the fields are empty.\n");
			return;
		}
		if(!popSizeTF.getText().matches("[-+]?\\d+") || !epochNumTF.getText().matches("[-+]?\\d+") ||
				!mutProbTF.getText().matches("([0-9]*)\\.([0-9]*)")) {
			logArea.appendText("\nWrong format.\n");
			return;
		}
					
		popSize = Integer.parseInt(popSizeTF.getText());
		epochNum = Integer.parseInt(epochNumTF.getText());
		mutProb = Double.parseDouble(mutProbTF.getText()); 
		
		population = new int[popSize][29];
		
		boolean[] markers = new boolean[29];
		for(int i = 0; i < markers.length; i++) 
			markers[i] = true;
		
		int randomIndex;
		int j;
		for(int i = 0; i < popSize; i++) {
			j = 0;
			while (j < 29) {
				randomIndex = randInt(1, 29);
				if(markers[randomIndex-1]) {
					population[i][j] = randomIndex;
					markers[randomIndex-1] = false;
					j++;
				}
			}
			for(int q = 0; q < markers.length; q++) 
				markers[q] = true;			
		}
		
		progressBar.progressProperty().unbind();
		progressBar.setProgress(0.0);

		////////////////////////////////////TASK
		
		task = new Task<GeneticAlgorithmResults>() {
			@Override
			protected GeneticAlgorithmResults call() throws Exception {

				int[] globalBestPath = new int[29];
				int[][] pop = population;
				int[] fit = new int[popSize];
				int[] bestFit = new int[epochNum];
				int curGlobalBestFitness = Integer.MAX_VALUE;
				int curBestFitnessIndex = 0;
				
				int[][][] epochsPopulations = new int[epochNum][popSize][29];
				int[][] epochsFitnesses = new int[epochNum][popSize];
				int[][] bestPopulation = new int[popSize][29];
				int[][] bestEpochsIndividuals = new int[epochNum][29];
				
				for(int e = 0; e < epochNum; e++) {	
					epochsPopulations[e] = pop;
					
					int curBestFitness = Integer.MAX_VALUE;
					
					for(int i = 0; i < popSize; i++) {
						fit[i] = fitness(pop[i]);
						if(fit[i] < curBestFitness) {
							curBestFitness = fit[i];
							curBestFitnessIndex = i;
						}
					}
					bestFit[e] = curBestFitness;
					bestEpochsIndividuals[e] = pop[curBestFitnessIndex];
					epochsFitnesses[e] = fit;

					if(bestFit[e] < curGlobalBestFitness) {
						curGlobalBestFitness = bestFit[e];
						globalBestPath = pop[curBestFitnessIndex];
						bestPopulation = pop;
					}
					pop = rouletteWheelSelection(pop, fit);		
					
					updateProgress(e+1, epochNum);
				}
				
				GeneticAlgorithmResults res = new GeneticAlgorithmResults(epochsPopulations, epochsFitnesses, bestFit, globalBestPath, bestPopulation, bestEpochsIndividuals);
				
				return res;
			}
			
		};
		progressBar.progressProperty().bind(task.progressProperty());
		
		////////////////////////////////////
		task.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, t -> {
            results = task.getValue();


            drawPath(coordinatesMatrix, results.getGlobalBestIndividual(), currentGraphPane);
            logArea.appendText("\nBest result ever: ");
            logArea.appendText(""+ fitness(results.getGlobalBestIndividual()));
        });
		new Thread(task).start();
		scrollBar.setMin(0);
		scrollBar.setMax(epochNum-1);
		scrollBar.valueProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue<? extends Number> ov,
                    Number old_val, Number new_val) {          	
            	drawPath(coordinatesMatrix, results.getBestEpochsIndividuals()[new_val.intValue()], currentGraphPane);
            }
		});
		

	}
	
	private void resultsToFile() throws FileNotFoundException, UnsupportedEncodingException {
		
		PrintWriter writer = new PrintWriter("results.txt", "UTF-8");

		for(int e = 0; e < epochNum; e++) {
			System.out.println("\nEpoch " + e + ":\n");//writer.println("Epoch " + e + ":\n");
			for(int i = 0; i < popSize; i++) {
				for(int j = 0; j < 29; j++) {
					System.out.print(results.getEpochsPopulations()[e][i][j] + "  ");//writer.print(results.getEpochsPopulations()[e][i][j] + "  ");
				}
				System.out.println("\n");//writer.println("\n\n");
			}
			System.out.println("Fitnesses:");//writer.println("Fitnesses:\n");
			for(int i = 0; i < popSize; i++) {
				System.out.print(results.getEpochsFitnesses()[e][i] + "  ");//writer.print(results.getEpochsFitnesses()[e][i] + "  ");
			}
			System.out.println("\nBest fitness: " + results.getEpochsBestFitnesses()[e]);//writer.println("\nBest fitness: " + results.getEpochsBestFitnesses()[e]);
		}
		  
	    writer.close();		
	}
	
	private int[] mutation(int[] individual) {
		
		for(int i = 0; i < individual.length; i++) {		
			if(Math.random() <= mutProb) {
				int j = (int) (individual.length * Math.random());
				
				int city1 = individual[i];
				int city2 = individual[j];
				
				individual[j] = city1;
				individual[i] = city2;
				
			}		
		}
		
		return individual;
	}
	
	private int[] tournamentSelection(int[][] population, int[] fitness) {
        
		int tournamentSize = popSize;//5;
		int[][] tournament = new int[tournamentSize][29];
        
        for (int i = 0; i < tournamentSize; i++) {
            int randomId = (int) (Math.random() * popSize);
            tournament[i] = population[randomId];
        }
        
        int[] fittest = new int[tournamentSize];
        int best = Integer.MAX_VALUE;
        int bestIndex = 0;
        for(int i = 0; i < tournamentSize; i++) {
        	fittest[i] = fitness(tournament[i]);
        	if(fittest[i] < best) {
        		best = fittest[i];
        		bestIndex = i;
        	}
        }
        
        return tournament[bestIndex];
    }

	public int[][] rouletteWheelSelection(int[][] population, int[] fitness) {
		
		
		int[][] newPopulation = new int[popSize][29];
		int[][] parents = new int[2][29];

		int[][] children;
		int p = 0;
		while (p < popSize-1) {			

			
			parents[0] = tournamentSelection(population, fitness);
			parents[1] = tournamentSelection(population, fitness);
			

			children = OXCrossover(parents[0], parents[1]);
			
			newPopulation[p] = children[0];
			newPopulation[p] = mutation(newPopulation[p]);
			newPopulation[p+1] = children[1];
			newPopulation[p+1] = mutation(newPopulation[p+1]);

			p++;
		}
            
        return newPopulation;
    }
	
	private int[] PMXCrossover(int[] dad, int[] mom) {
		
		int[] child = new int[dad.length];
		
		int startPos = (int) (Math.random() * dad.length);
        int endPos = (int) (Math.random() * dad.length);
		
        for (int i = 0; i < child.length; i++) {         
            if (startPos < endPos && i > startPos && i < endPos) {
                child[i] = dad[i];
            }
            else if (startPos > endPos) {
                if (!(i < startPos && i > endPos)) {
                    child[i] = dad[i];
                }
            }
        }
             
        for (int i = 0; i < mom.length; i++) {           
            if (!hasElement(child, mom[i])) {         
                for (int j = 0; j < child.length; j++) {                   
                    if (child[j] == 0) {
                        child[j] = mom[i];
                        break;
                    }
                }
            }
        }
		return child;		
	}
	
	private int[][] OXCrossover(int[] dad, int[] mom) {
		
		int[] best_child = new int[dad.length];
		int[] child_1 = new int[dad.length];
        int[] child_2 = new int[dad.length];
        
		int number1;
		int number2;
		
		do
		{
			number1 = rnd.nextInt(best_child.length);
			number2 = rnd.nextInt(best_child.length);
		}
		while(number1 == number2);
		
		int start = Math.min(number1, number2);
        int end = Math.max(number1, number2);
        int q = 0;
        
        for(int i = start; i < end; i++) {
        	child_1[q] = dad[i];
        	child_2[q] = mom[i];
        	q++;
        }
		
        int current_city_1;
        int current_city_2;
        int current_index = 0;
        int q1 = q;
        int q2 = q;
        for(int i = 0; i < best_child.length; i++) {
        	current_index = (end + i) % best_child.length;
    
        	current_city_1 = dad[current_index];
        	current_city_2 = mom[current_index];
        	
        	if(!hasElement(child_1, current_city_2)) {
        		child_1[q1] = current_city_2;
        		q1++;
        	}
        	if(!hasElement(child_2, current_city_1)) {
        		child_2[q2] = current_city_1;
        		q2++;
        	}
        }
        
        rotate(child_1, start);
        rotate(child_2, start);
        
        if(fitness(child_1) < fitness(child_2)) 
        	best_child = child_1;
        else 
        	best_child = child_2;
        
        int[][] children = new int[2][29];
        children[0] = child_1;
        children[1] = child_2;
        
        //return best_child;
		return children;
	}
	
	public static void rotate(int[] arr, int order) {
		if (arr == null || order < 0) {
		    throw new IllegalArgumentException("Illegal argument!");
		}
	 
		for (int i = 0; i < order; i++) {
			for (int j = arr.length - 1; j > 0; j--) {
				int temp = arr[j];
				arr[j] = arr[j - 1];
				arr[j - 1] = temp;
			}
		}
	}
	
	private boolean hasElement (int[] array, int element) {
		
		for(int i = 0; i < array.length; i++) {
			if(array[i] == element)
				return true;
		}
		return false;
	}
	
	private void drawPath(double[][] coordinates, int[] individual, Pane pane) {
		pane.getChildren().clear();
		for(int i = 0; i < 29; i++) {
			Circle city = new Circle(coordinates[i][0]/4,coordinates[i][1]/4, 5);
			 pane.getChildren().add(city);				
		}
		
		for(int i = 0; i < 28; i++) {
			Line path = new Line(coordinates[individual[i]-1][0]/4, coordinates[individual[i]-1][1]/4, 
					coordinates[individual[i+1]-1][0]/4, coordinates[individual[i+1]-1][1]/4);
			path.setStroke(Color.RED);
            path.setStrokeWidth(3);
            pane.getChildren().add(path);
		}
		Line path = new Line(coordinates[individual[28]-1][0]/4, coordinates[individual[28]-1][1]/4, 
				coordinates[individual[0]-1][0]/4, coordinates[individual[0]-1][1]/4);
		path.setStroke(Color.GREEN);
        path.setStrokeWidth(3);
        pane.getChildren().add(path);
        fitLabel.setText("Ôèòíåñ ôóíêöèÿ: " + fitness(individual));
        pane.getChildren().add(fitLabel);
	}
	
	private int fitness(int[] individual) {
		//logArea.appendText("\nÐàññòîÿíèÿ:\n");
		int fitnessValue = 0;	
		for(int i = 0; i < individual.length-1; i++) {
			//logArea.appendText(fullMatrix[individual[i]-1][individual[i+1]-1] + "  ");
			fitnessValue += fullMatrix[individual[i]-1][individual[i+1]-1];
		}
		fitnessValue += fullMatrix[individual[individual.length-1]-1][individual[0]-1];
		//logArea.appendText("\n");
		return fitnessValue;
	}
	
	private void readData(File file, int rows, int columns, String task) throws FileNotFoundException {

		Scanner input = new Scanner (file);
		
		switch(task) {
			case "fullMatrix":
			{
				fullMatrix = new int[rows][columns];
				for(int i = 0; i < rows; ++i)
				    for(int j = 0; j < columns; ++j)
				    	if(input.hasNext())		        	
				            fullMatrix[i][j] = Integer.parseInt(input.next());
				input.close();
				break;
			}
			case "coordinatesMatrix":
			{
				coordinatesMatrix = new double[rows][columns];
				for(int i = 0; i < rows; i++)
				    for(int j = 0; j < columns; j++)
				        if(input.hasNext())		        	
				        	coordinatesMatrix[i][j] = Double.parseDouble(input.next());
				input.close();
				break;
			}
			case "bestTourArray": 
			{
				bestTourArray = new int[columns];
				for(int i = 0; i < columns; ++i)
					if(input.hasNext())
						bestTourArray[i] = Integer.parseInt(input.next());
				input.close();
				break;
			}
		}
	
	}

	private void printArray(String title, Object object) {
		
		logArea.appendText(title + "\n");
		if(object.getClass().getTypeName().trim().equals("double[][]")) {
			for(int i = 0; i < Array.getLength(object); i++) {
				 double[] row = (double[]) Array.get(object, i);
				 for(int j = 0; j < row.length; j++) 
					 logArea.appendText(row[j] + "  ");			 
				 logArea.appendText("\n\n");
			}
		}
		else if(object.getClass().getTypeName().trim().equals("int[][]")) {
			for(int i = 0; i < Array.getLength(object); i++) {
				 int[] row = (int[]) Array.get(object, i);
				 for(int j = 0; j < row.length; j++) 
					 logArea.appendText(row[j] + "  ");			 
				 logArea.appendText("\n\n");
			}		
		}
		else if(object.getClass().getTypeName().trim().equals("int[][][]")) {
			for(int e = 0; e < Array.getLength(object); e++) {
				 logArea.appendText("Ïîïóëÿöèÿ ýïîõè " + (e+1) + ":\n");
				 int[][] matrix = (int[][]) Array.get(object, e);
				 for(int i = 0; i < matrix.length; i++) {
					 for (int j = 0; j <matrix[i].length; j++) {					 
						 logArea.appendText(matrix[i][j] + "  ");
					 }
					 logArea.appendText("\n");
				 }
				 logArea.appendText("\n\n");
			}		
		}
		else if(object.getClass().getTypeName().trim().equals("int[]")) {
			for(int i = 0; i < Array.getLength(object); i++) {
				 int element = Array.getInt(object, i);
					 logArea.appendText(element + "\n");			 
			}		
		}
	}
	
	private int randInt(int min, int max) {

	    int randomNum = rnd.nextInt((max - min) + 1) + min;
	    return randomNum;
	}
	
	public void initialize(URL location, ResourceBundle resources) {
		popSizeTF.setText("500");
		epochNumTF.setText("10000");
		mutProbTF.setText("0.1");

		
		file = new File("bays29_full_matrix.txt");
		try {
			readData(file, 29, 29, "fullMatrix");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		
		file = new File("bays29_coordinates.txt");
		try {
			readData(file, 29, 2, "coordinatesMatrix");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		
		file = new File("bays29_best_tour.txt");
		try {
			readData(file, 0, 29, "bestTourArray");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		//printArray("Full matrix: ", fullMatrix);
		//printArray("Coords: ", coordinatesMatrix);
		//printArray("Best tour: ", bestTourArray);
		logArea.appendText("Best tour fitness: " + fitness(bestTourArray));

		drawPath(coordinatesMatrix, bestTourArray, optimalGraphPane);
	}
}
