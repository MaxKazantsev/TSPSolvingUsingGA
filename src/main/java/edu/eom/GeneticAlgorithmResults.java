package edu.eom;

public class GeneticAlgorithmResults {

	private int[][][] epochsPopulations;
	private int[][] epochsFitnesses;
	private int[] epochsBestFitnesses;
	private int[] globalBestIndividual;
	private int[][] bestPopulation;
	private int[][] bestEpochsIndividuals;
	
	public GeneticAlgorithmResults(int[][][] pops, int[][] fits, int[] bestFits, int[] gBest, int[][] bestPop, int[][] bestEpochsIndividuals) {
		
		this.epochsPopulations = pops;
		this.epochsFitnesses = fits;
		this.epochsBestFitnesses = bestFits;
		this.globalBestIndividual = gBest;
		this.bestPopulation = bestPop;
		this.bestEpochsIndividuals = bestEpochsIndividuals;
	}
	
	public int[][] getBestEpochsIndividuals() {
		return bestEpochsIndividuals;
	}
	
	public int[][][] getEpochsPopulations() {
		return epochsPopulations;
	}
	
	public int[][] getEpochsFitnesses() {
		return epochsFitnesses;
	}
	
	public int[] getEpochsBestFitnesses() {
		return epochsBestFitnesses;
	}
	
	public int[] getGlobalBestIndividual() {
		return globalBestIndividual;
	}
	public int[][] getBestPopulation() {
		return bestPopulation;
	}
}
