package edu.upenn.cis.cis121.hw6;

import java.util.Iterator;
import java.util.Set;

/**
 * This class generates the huffman tree and builds the code book given an
 * input string.
 * @author Jevon Yeoh - CIS 121, Spring 2012
 *
 */

public class HuffmanBuilder {

	/**
	 * Default constructor
	 */
	public HuffmanBuilder() {
	}
	
	/**
	 * Given a string input, this method creates a CodeBookI which contains all
	 * the symbols, and their respective probabilities from the given input. 
	 * This CodeBookI defines a blank encoding for all symbols (the CodeBookI
	 * will have the information on each character of the string and just 
	 * default the encodings to a blank string.
	 * @param text - given input to make a code book
	 * @return the code book which contains all symbols and their respective
	 * probabilities
	 */
	public static CodeBookI blankCodeBook(String text) {
		
		CodeBook temp = new CodeBook();
		text = text.toLowerCase();
		
		//piazza post 572 says to use new Codebook() for empty coding (post 510)
		if (text.length() == 0) {
			return temp;
		}
		
		//traverse down the text input
		for (int i = 0; i < text.length(); i++) {
			char curr = text.charAt(i);
			Set<Character> _alphaSet = temp.getAlphabet();
			
			//if alphabet set does not contain the current char
			if (!_alphaSet.contains(curr)) {
				double freq = 1.0;
				//count the frequency of the character
				for (int j = 0; j < text.length(); j++) {
					if (j == i) {
						continue;
					}
					else if (curr == text.charAt(j)) {
						freq++;
					}
				}
				//add symbol to codebook
				temp.putSymbol(curr, freq, "");
			}			
		}
		return temp;
	}	
	
	/** 
	 * This method receives as input a CodeBookI containing an alphabet and the
	 * respective probabilities of each symbol. It returns a new CodeBookI, 
	 * where each symbol from the input codebook now has an appropriate Huffman
	 * encoding.
	 * @param codebook
	 * @return
	 */
	public static CodeBookI buildHuffmanCode(CodeBookI codebook) {
		
		//inner class to store the symbols, frequency and children of a node
		class probNode implements Comparable<probNode>{
			private String _symbol;
			private double _frequency;
			private probNode leftChild;
			private probNode rightChild;
			
			public probNode(String symbol, double freq) {
				_symbol = symbol;
				_frequency = freq;
			}
			
			public probNode(probNode min1, probNode min2) {
				_frequency = min1.getFreq() + min2.getFreq();
				leftChild = min1;
				rightChild = min2;
				_symbol = min1.getSymb() + min2.getSymb();
			}
			
			private double getFreq() {
				return _frequency;
			}
			
			private String getSymb() {
				return _symbol;
			}
			
			//override compareTo function to ensure nodes are compared properly
			public int compareTo(probNode other) {
				if (_frequency < other._frequency) {
					return -1;
				}
				else if (_frequency > other._frequency){
					return 1;
				}
				else {
					return 0;
				}
			}
			
			//helper method to generate the code of the leaves
			private void generateCode(String encoding, CodeBookI codebook, probNode node) {
				if (node.getSymb().length() > 1) {
					//recursively generate the code
					generateCode(encoding+"0", codebook, node.leftChild);
					generateCode(encoding+"1", codebook, node.rightChild);
				}
				else {
					//handle edge case of having only one char/node in the tree
					if (encoding.equals("")) {
						codebook.putSymbol(node.getSymb().charAt(0), node.getFreq(), "0");
					}
					else {
						codebook.putSymbol(node.getSymb().charAt(0), node.getFreq(), encoding);
					}
				}
			}
		}
		
		Set<Character> alphaSet = codebook.getAlphabet();
		Iterator<Character> iterator = alphaSet.iterator();
		BinaryMinHeap<probNode> heap = new BinaryMinHeap<probNode>();
		
		//create BinaryMinHeap
		while (iterator.hasNext()) {
			char curr = iterator.next();
			double prob = codebook.getProbability(curr);
			probNode temp = new probNode(Character.toString(curr), prob);
			heap.insert(temp);
		}

		//generate huffman tree
		while (heap.size() > 1) {
			probNode min1 = heap.removeMin();
			probNode min2 = heap.removeMin();
			probNode combinedProbNode = new probNode(min1, min2);
			heap.insert(combinedProbNode);
		}
		
		//populate codebook
		if (heap.size() > 0) {
			probNode huffmanTree = heap.removeMin();
			huffmanTree.generateCode("", codebook, huffmanTree);
		}

		return codebook;
		
	}
	

}
