import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

/**
 * Uses a hidden markov map and the viterbi algorithm to tag words in a sentence with their part of speech (eg noun, verb, adverb...etc)
 * @author Emmma Rafkin and Shannon Sartain, CS10 18W
 * 
 */
public class HMM { 
	HashMap<String, HashMap<String, Double>>obsMap; //holds word observations and which part of speech they are 
	HashMap<String, HashMap<String, Double>>transMap;//holds transition observations
	
	/**
	 * constructor initializes maps.
	 */
	public HMM() {
		obsMap = new HashMap<String, HashMap<String, Double>>();
		transMap = new HashMap<String, HashMap<String, Double>>();
	}
	
	/**
	 * helper method that runs the Viterbi algorithm on a list of words (one sentence at a time)
	 * will work most effectively if program is trained first --if the maps aren't empty.
	 * @param sentence - arraylist of words in a sentence
	 * @return tags of a sentence in order from beginning to end
	 */
	public ArrayList<String> viterbi(ArrayList<String> sentence) {
		ArrayList<String> ret = new ArrayList<String>();
		ArrayList<String> returnList = new ArrayList<String>();
		double unseen = -100; //arbitrary value
		ArrayList<String> currStates = new ArrayList<String>(); //list of current states
		HashMap<String, Double> currScores = new HashMap<String, Double>(); //map of current scores for each state
		HashMap<Integer, HashMap<String, String>> backtrace = new HashMap<Integer, HashMap<String, String>>(); 
		ArrayList<String> nextStates; 
		HashMap<String, Double> nextScores;
		currStates.add("#"); //hardcode the beginning of the sentence
		currScores.put("#", 0.0); //hardcode the value for beginning of the sentence
		for(int i = 0; i<sentence.size(); i++) { //do for all words in the sentence
			nextStates = new ArrayList<String>();
			nextScores = new HashMap<String, Double>();
			for(String currState: currStates) { //for each current state
				if(transMap.keySet().contains(currState)) { //if the map of transitions contains the current state
					for(String transition: transMap.get(currState).keySet()) { //for each transition in the current state's transitions
						nextStates.add(transition); //add the transition to the list of next states
						double obsScore; 
						if(obsMap.get(transition).get(sentence.get(i))==null) obsScore = unseen; //if we dont see the transition, set it to unseen (negative number)
						else obsScore = obsMap.get(transition).get(sentence.get(i)); //otherwise, get the score of the current transition for the current word
						double nextScore = currScores.get(currState) + transMap.get(currState).get(transition) + obsScore; //current + transition + observation
						if(!nextScores.containsKey(transition)||nextScore>nextScores.get(transition)) { //if next state isn't in nextScores or nextScore>the score of the nextState
							nextScores.put(transition, nextScore); //set nextScore
							if(!backtrace.keySet().contains(i)) backtrace.put(i, new HashMap<String, String>());//i backtrace doesn't contain this word value yet, put it in 
							backtrace.get(i).put(transition, currState); //if it is there, add the transition from the current state
							} else {
								nextStates.remove(transition); //do this to save memory, store less objects and preserve heap space
							}
					}
				}
			}
			currStates = nextStates; //reset by moving to the next state
			currScores = nextScores;	 //reset by moving to the next score
		}
		double highestScore = -100000;
		String curr = "";
		for(String state: currScores.keySet()) { //for each state of the current scores
			if(currScores.get(state)>highestScore) { //if the score for the current state is greater than the highest score
				curr = state; //reset curr to be this state
				highestScore = currScores.get(state); //set highest score to be the score for the current state
			}
		}
		currScores.clear();
		currStates.clear();
		ret.add(0,curr); //add the state to the path
		for(int i = sentence.size()-1; i>=0; i--) { //for each word in the sentence
			if(backtrace.get(i).get(curr)!=null) curr = backtrace.get(i).get(curr); //if there is a state for the current state, curr = that state
			ret.add(0, curr); //add that to ret
		}
		for(String s: ret) {
			returnList.add(s);
		}
		backtrace.clear();
		sentence.clear();
		return returnList;
	}
	
	/**
	 * reads in a file then calls the Viterbi method on it.
	 * @param filename
	 * @return tags for an entire file
	 * @throws IOException
	 */
	public ArrayList<String> build(String filename) throws IOException {
		BufferedReader input; 
		String wordLine;
		ArrayList<String> ret = new ArrayList<String>();
		ArrayList<String> vitList = new ArrayList<String>();
		try {
			input = new BufferedReader(new FileReader(filename));
			ArrayList<String> sentence = new ArrayList<String>();
			while((wordLine = input.readLine())!=null) { //read line by line
				String[] pieces = wordLine.split(" "); //split each line into an array of words
				for(String p: pieces) sentence.add(p);
				vitList = viterbi(sentence);
				for (String s: vitList) {
					ret.add(s);
				}
				vitList.clear();
				sentence.clear();
			}
			
		} catch(FileNotFoundException e) {
			System.err.println("need to provide filepath");
		} 
		return ret;
	}
	
	/**
	 * This method trains the program in order to use the Viterbi algorithm.
	 * @param filename is the file with the normal sentence
	 * @param filename1 is the file with the tagged sentence.
	 * @throws IOException 
	 */
	public void training(String filename, String filename1) throws IOException{
		BufferedReader words;
		BufferedReader tags;
		String wordLine;
		String tagLine;
		try {
			words = new BufferedReader(new FileReader(filename)); //try to read the file
			tags = new BufferedReader(new FileReader(filename1));
			ArrayList<String> sentence = new ArrayList<String>();
			ArrayList<String> tagSentence = new ArrayList<String>();
			while((wordLine = words.readLine())!=null&&(tagLine = tags.readLine())!=null) {
				String[] pieces = wordLine.split(" ");
				String[] tag = tagLine.split(" ");
				for(String p: pieces) {
					sentence.add(p);
				}
				for(String t: tag) {
					tagSentence.add(t);
				}
				for(int i = 0; i<tagSentence.size(); i++) {
					String t = tagSentence.get(i);
					String w = sentence.get(i);
					String nextTag = "";
					if(i!=tagSentence.size()-1)  nextTag = tagSentence.get(i+1);
					if(!obsMap.containsKey(t)) obsMap.put(t, new HashMap<String, Double>()); //if there is no instance of that tag, put in that tag w/ empty map
					if(!obsMap.get(t).containsKey(w)) obsMap.get(t).put(w, 1.0); //if there is no instance of that word in the tag, add it
					else obsMap.get(t).put(w, (obsMap.get(t).get(w)+1)); //otherwise increment instance of that word
					if(i==0) {
						if(!transMap.containsKey("#")) transMap.put("#", new HashMap<String, Double>());
						if(!transMap.get("#").containsKey(t)) transMap.get("#").put(t, 1.0);
						else transMap.get("#").put(t, (transMap.get("#").get(t)+1));
					}
					if(i!=0&&i!=tagSentence.size()-1) {
						if(!transMap.containsKey(t)) transMap.put(t, new HashMap<String, Double>());
						if(!transMap.get(t).containsKey(nextTag)) transMap.get(t).put(nextTag, 1.0);
						else transMap.get(t).put(nextTag, (transMap.get(t).get(nextTag)+1));
						}
					}
					sentence.clear();
					tagSentence.clear();
				}	
			words.close();
			tags.close();
		} catch (FileNotFoundException e) {
			System.err.println("need to provide filepath");
		} 
		int sum = 0;
		for(String s: obsMap.keySet()) {
			for(String s1: obsMap.get(s).keySet()) {
				sum+=obsMap.get(s).get(s1);
			}
			for(String s1: obsMap.get(s).keySet()) {
				double j = obsMap.get(s).get(s1);
				obsMap.get(s).put(s1, Math.log(j/sum));
			}
		}
		for(String s: transMap.keySet()) {
			for(String s1: transMap.get(s).keySet()) {
				sum+=transMap.get(s).get(s1);
			}
			for(String s1: transMap.get(s).keySet()) {
				double j = transMap.get(s).get(s1);
				transMap.get(s).put(s1, Math.log(j/sum));
			}
		}
	
	}
	
	/**
	 * compares a file to the output of the viterbi method to check for accuracy.
	 * prints number correct and total number
	 * @param tags -- output from viterbi method
	 * @param filename -- file holding the properly tagged sentences
	 * @throws IOException
	 */
	public void compare(ArrayList<String> tags, String filename) throws IOException {
		BufferedReader input;
		input = new BufferedReader(new FileReader(filename));
		String line;
		ArrayList<String> vit = new ArrayList<String>();
		while((line = input.readLine())!=null) {
			String[] pieces = line.split(" ");
			for(String p: pieces) {
				vit.add(p);
			}
		}
		while (tags.remove("#")) {
			tags.remove("#");
		}
		int countCorrect = 0;
		int size = vit.size();
		for (int i = 0; i < size; i ++) {
			if (vit.get(i).equals(tags.get(i))) countCorrect++;	
		}
		System.out.println("correct tags:   " + tags);
		System.out.println("estimated tags: " + vit);
		System.out.println(countCorrect + " out of " + size + " correct.");
		input.close();
	}
	/**
	 * allows the user to input a sentence and have it tagged by the viterbi method. 
	 * will work best if program has undergone training first. 
	 * @return tags of sentence (using viterbi method)
	 * @throws IOException
	 */
	public ArrayList<String> tagify() throws IOException {
		ArrayList<String> ret = new ArrayList<String>();
		Scanner sysReader = new Scanner(System.in);
		System.out.println("Write a sentence to tag or hit q to quit.");
		String input = sysReader.nextLine();	
		if (!input.equals("q")) {
			String[] pieces = input.split(" ");
			ArrayList<String> sentence = new ArrayList<String>();
			for(String p: pieces) {
				sentence.add(p);
			}
			ret = viterbi(sentence);
			System.out.println(ret);
			tagify();
		}
		if (input.equals("q")) System.out.println("exited.");
		sysReader.close();
		return ret;
	}
	/**
	 * testing.
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[]args) throws IOException {
		HMM h = new HMM();
		ArrayList<String> tags = new ArrayList<String>();
		try {
			h.training("tests/simple-train-sentences.txt","tests/simple-train-tags.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}		
		System.out.println("obsMap: " + h.obsMap);
		System.out.println("transMap: " + h.transMap);
		try {
			tags = h.build("tests/simple-test-sentences.txt");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			h.compare(tags, "tests/simple-test-tags.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}
		h.tagify();
		
	}
	
}
