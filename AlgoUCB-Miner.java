import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is an implementation of the "UCB-MINER Algorithm" for High-Utility Itemsets Mining
 * as described in the journal paper : <br/><br/>
 * 
 *  Krishnamoorthy, S. (2014). Pruning Strategies for Mining High-Utility Itemsets.
 *  Expert Systems with Applications. 
 *  
 *  This algorithm is a variation of the HUI-Miner algorithm where the database is partitionned.
 *  It introduces and additional parameter, which is the number of partitions.
 *
 * @see UtilistUCBMiner
 * @see Element
 * @author Philippe Fournier-Viger
 */
public class AlgoHUPplusMiner {

	/** the time at which the algorithm started */
	public long startTimestamp = 0;  
	
	/** the time at which the algorithm ended */
	public long endTimestamp = 0; 
	
	/** the number of high-utility itemsets generated */
	public int huiCount =0; 
	
	/** Map to remember the TWU of each item */
	Map<Integer, Integer> mapItemToTWU;
	
	/** writer to write the output file  */
	BufferedWriter writer = null;  

	/** The eucs structure:  key: item   key: another item   value: twu */
	Map<Integer, Map<Integer, Long>> mapFMAP;  

	/** the number of utility-list that was constructed */
	private int joinCount;
	
	/** the number of utility-list constructions that were aborted by LA-Prune */
	private int partialJoinCount;
	
	/** the number of transactions in this database */
	private int transactionCount;
	
	/** the number of partitions k */
	private int p_count;
	
	/** the size of a partition as a number of transactions */
	private int p_size;
	
	/** buffer for storing the current itemset that is mined when performing mining
	* the idea is to always reuse the same buffer to reduce memory usage. */
	final int BUFFERS_SIZE = 200;
	private int[] itemsetBuffer = null;
	
	/** this class represent an item and its utility in a transaction */
	class Pair{
		int item = 0;
		int utility = 0;
	}
	
	/**
	 * Default constructor
	 */
	public AlgoUCBMiner() {
	}

	/**
	 * Run the algorithm
	 * @param input the input file path
	 * @param output the output file path
	 * @param minUtility the minimum utility threshold
	 * @param p_count the number of partitions
	 * @throws IOException exception if error while writing the file
	 */
	public void runAlgorithm(String input, String output, int minUtility, int p_count) throws IOException {
		
		// initialize the buffer for storing the current itemset
		itemsetBuffer = new int[BUFFERS_SIZE];
		
		mapFMAP =  new HashMap<Integer, Map<Integer, Long>>();
		
		writer = new BufferedWriter(new FileWriter(output));

		//  We create a  map to store the TWU of each item
		mapItemToTWU = new HashMap<Integer, Integer>();

		
		
		// We scan the database a first time to calculate the TWU of each item.
		BufferedReader myInput = null;
		String thisLine;
		transactionCount = 0;
		try {
			// prepare the object for reading the file
			myInput = new BufferedReader(new InputStreamReader( new FileInputStream(new File(input))));
			// for each line (transaction) until the end of file
			while ((thisLine = myInput.readLine()) != null) {
				// if the line is  a comment, is  empty or is a
				// kind of metadata
				if (thisLine.isEmpty() == true ||
						thisLine.charAt(0) == '#' || thisLine.charAt(0) == '%'
								|| thisLine.charAt(0) == '@') {
					continue;
				}
				
				// split the transaction according to the : separator
				String split[] = thisLine.split(":"); 
				// the first part is the list of items
				String items[] = split[0].split(" "); 
				// the second part is the transaction utility
				int transactionUtility = Integer.parseInt(split[1]);  
				// for each item, we add the transaction utility to its TWU
				for(int i=0; i <items.length; i++){
					// convert item to integer
					Integer item = Integer.parseInt(items[i]);
					// get the current TWU of that item
					Integer twu = mapItemToTWU.get(item);
					// add the utility of the item in the current transaction to its twu
					twu = (twu == null)? 
							transactionUtility : twu + transactionUtility;
					mapItemToTWU.put(item, twu);
				}
				transactionCount++;
			}
		} catch (Exception e) {
			// catches exception if error while reading the input file
			e.printStackTrace();
		}finally {
			if(myInput != null){
				myInput.close();
			}
	    }
		
		//===== BEGIN UCB-MINEr
		// save the desired number of partitions
		this.p_count = p_count;
		
		if(p_count > transactionCount) {
			this.p_size = transactionCount;
			this.p_count = 1;
		}else {
			this.p_size = transactionCount / this.p_count;
			this.p_count = (int) Math.ceil(transactionCount / (double) p_size);
		}
		//===== END UCB-MINER
		
		// CREATE A LIST TO STORE THE UTILITY LIST OF ITEMS WITH TWU  >= MIN_UTILITY.
		List<UtilistUCBMiner> listOfUtilityLists = new ArrayList<UtilistUCBMiner>();
		// CREATE A MAP TO STORE THE UTILITY LIST FOR EACH ITEM.
		// Key : item    Value :  utility list associated to that item
		Map<Integer, UtilistUCBMiner> mapItemToUtilityList = new HashMap<Integer, UtilistUCBMiner>();
		
		// For each item
		for(Integer item: mapItemToTWU.keySet()){
			// if the item is promising  (TWU >= minutility)
			if(mapItemToTWU.get(item) >= minUtility){
				// create an empty Utility List that we will fill later.
				UtilistUCBMiner uList = new UtilistUCBMiner(item, this.p_count);
				mapItemToUtilityList.put(item, uList);
				// add the item to the list of high TWU items
				listOfUtilityLists.add(uList); 
				
			}
		}
		// SORT THE LIST OF HIGH TWU ITEMS IN ASCENDING ORDER
		Collections.sort(listOfUtilityLists, new Comparator<UtilistUCBMiner>(){
			public int compare(UtilistUCBMiner o1, UtilistUCBMiner o2) {
				// compare the TWU of the items
				return compareItems(o1.item, o2.item);
			}
			} );
		
		// SECOND DATABASE PASS TO CONSTRUCT THE UTILITY LISTS 
		// OF 1-ITEMSETS  HAVING TWU  >= minutil (promising items)
		try {
			// prepare object for reading the file
			myInput = new BufferedReader(new InputStreamReader(new FileInputStream(new File(input))));
			// variable to count the number of transaction
			int tid =0;
			// for each line (transaction) until the end of file
			while ((thisLine = myInput.readLine()) != null) {
				// if the line is  a comment, is  empty or is a
				// kind of metadata
				if (thisLine.isEmpty() == true ||
						thisLine.charAt(0) == '#' || thisLine.charAt(0) == '%'
								|| thisLine.charAt(0) == '@') {
					continue;
				}
				
				// split the line according to the separator
				String split[] = thisLine.split(":");
				// get the list of items
				String items[] = split[0].split(" ");
				// get the list of utility values corresponding to each item
				// for that transaction
				String utilityValues[] = split[2].split(" ");
				
				// Copy the transaction into lists but 
				// without items with TWU < minutility
				
				int remainingUtility =0;

				// EUCS
				long newTWU = 0;
				
				// Create a list to store items
				List<Pair> revisedTransaction = new ArrayList<Pair>();
				// for each item
				for(int i=0; i <items.length; i++){
					/// convert values to integers
					Pair pair = new Pair();
					pair.item = Integer.parseInt(items[i]);
					pair.utility = Integer.parseInt(utilityValues[i]);
					// if the item has enough utility
					if(mapItemToTWU.get(pair.item) >= minUtility){
						// add it
						revisedTransaction.add(pair);
						remainingUtility += pair.utility;
						newTWU += pair.utility; // EUCS
					}
				}
				
				Collections.sort(revisedTransaction, new Comparator<Pair>(){
					public int compare(Pair o1, Pair o2) {
						return compareItems(o1.item, o2.item);
					}});

								
				// for each item left in the transaction
				for(int i = 0; i< revisedTransaction.size(); i++){
					Pair pair =  revisedTransaction.get(i);
					// subtract the utility of this item from the remaining utility
					remainingUtility = remainingUtility - pair.utility;
					
					// get the utility list of this item
					UtilistUCBMiner utilityListOfItem = mapItemToUtilityList.get(pair.item);
					
					// Add a new Element to the utility list of this item corresponding to this transaction
					Element element = new Element(tid, pair.utility, remainingUtility);
					
					utilityListOfItem.addElement(element, p_size);
					
					// BEGIN NEW OPTIMIZATION for FHM
					Map<Integer, Long> mapFMAPItem = mapFMAP.get(pair.item);
					if(mapFMAPItem == null) {
						mapFMAPItem = new HashMap<Integer, Long>();
						mapFMAP.put(pair.item, mapFMAPItem);
					}

					for(int j = i+1; j< revisedTransaction.size(); j++){
						Pair pairAfter = revisedTransaction.get(j);
						Long twuSum = mapFMAPItem.get(pairAfter.item);
						if(twuSum == null) {
							mapFMAPItem.put(pairAfter.item, newTWU);
						}else {
							mapFMAPItem.put(pairAfter.item, twuSum + newTWU);
						}
					}
					// END OPTIMIZATION of FHM
				
				}
				tid++; // increase tid number for next transaction

			}
		} catch (Exception e) {
			// to catch error while reading the input file
			e.printStackTrace();
		}finally {
			if(myInput != null){
				myInput.close();
			}
	    }
		
		// reset maximum
		MemoryLogger.getInstance().reset();
		
		startTimestamp = System.currentTimeMillis();

		// check the memory usage
		MemoryLogger.getInstance().checkMemory();

		// Mine the database recursively
		UCBMiner(itemsetBuffer, 0, null, listOfUtilityLists, minUtility);
		
		// check the memory usage again and close the file.
		MemoryLogger.getInstance().checkMemory();
		// close output file
		writer.close();
		// record end time
		endTimestamp = System.currentTimeMillis();
	}
	
	private int compareItems(int item1, int item2) {
		int compare = mapItemToTWU.get(item1) - mapItemToTWU.get(item2);
		// if the same, use the lexical order otherwise use the TWU
		return (compare == 0)? item1 - item2 :  compare;
	}
	
	/**
	 * This is the recursive method to find all high utility itemsets. It writes
	 * the itemsets to the output file.
	 * @param prefix  This is the current prefix. Initially, it is empty.
	 * @param pUL This is the Utility List of the prefix. Initially, it is empty.
	 * @param ULs The utility lists corresponding to each extension of the prefix.
	 * @param minUtility The minUtility threshold.
	 * @param prefixLength The current prefix length
	 * @throws IOException
	 */
	private void UCBMiner(int [] prefix, 
			int prefixLength, UtilistUCBMiner pUL, List<UtilistUCBMiner> ULs, int minUtility)
			throws IOException {
		
		// For each extension X of prefix P
		for(int i=0; i< ULs.size(); i++){
			UtilistUCBMiner X = ULs.get(i);

			// If pX is a high utility itemset.
			// we save the itemset:  pX 
			if(X.sumIutils >= minUtility){
				// save to file
				writeOut(prefix, prefixLength, X.item, X.sumIutils);
			}
			
			// If the sum of the remaining utilities for pX
			// is higher than minUtility, we explore extensions of pX.
			// (this is the pruning condition)
			if(X.sumIutils + X.sumRutils >= minUtility){
				// This list will contain the utility lists of pX extensions.
				List<UtilistUCBMiner> exULs = new ArrayList<UtilistUCBMiner>();
				// For each extension of p appearing
				// after X according to the ascending order
				for(int j=i+1; j < ULs.size(); j++){
					UtilistUCBMiner Y = ULs.get(j);

					// ======================== NEW OPTIMIZATION USED IN FHM
					Map<Integer, Long> mapTWUF = mapFMAP.get(X.item);
					if(mapTWUF != null) {
						Long twuF = mapTWUF.get(Y.item);
						if(twuF == null || twuF < minUtility) {
							continue;
						}
					}
					// =========================== END OF NEW OPTIMIZATION

					//=== BEGIN UCB-MINEr
					if(puPrune(X, Y, minUtility)) {
						continue;
					}
					//=== END UCB-MINEr
					
					// we construct the extension pXY 
					// and add it to the list of extensions of pX
					UtilistUCBMiner pul = construct(pUL, X, Y, minUtility);
					//=== BEGIN UCB-MINEr
					if(pul != null) {
					//=== END UCB-MINEr
						exULs.add(pul);
						joinCount++;
					}else {
						partialJoinCount++;
					}
						
				}
				// We create new prefix pX
				itemsetBuffer[prefixLength] = X.item;
				
				// We make a recursive call to discover all itemsets with the prefix pXY
				UCBMiner(itemsetBuffer, prefixLength+1, X, exULs, minUtility); 
			}
		}
	}
	
	//=== BEGIN UCB-MINEr
	/** This method check if the combination pXY of two itemsets
	 * pX and pY and extensions of pXY 
	 * should be pruned according to the PU-Prune property.
	 * @param X  the utility-list of item pX
	 * @param Y  the utility-list of itemset pY
	 * @param minUtil the minUtility threshold
	 * @return  true if pXY and its extensionsshould be pruned
	 */
	private boolean puPrune(UtilistUCBMiner X, UtilistUCBMiner Y, int minUtil) {
		long sum = 0;
		// for each partition 
		for(int i = 0; i < p_count; i++) {
			// if Y appear in that partition
			if(Y.partitionSURU[i] !=0) {
				// We add the utility + remaining utility of X to the sum
				sum += X.partitionSURU[i];
			}
		}
		// if the sum is less than minutil, we can prune pXY and its supersets
		return sum < minUtil;
	}
	//=== END UCB-MINEr

	/**
	 * This method constructs the utility list of pXY
	 * @param P :  the utility list of prefix P.
	 * @param px : the utility list of pX
	 * @param py : the utility list of pY
	 * @param minUtility : the minimum utility threshold
	 * @return the utility list of pXY
	 */
	private UtilistUCBMiner construct(UtilistUCBMiner P, 
			UtilistUCBMiner px, UtilistUCBMiner py, int minUtility) {
		// create an empy utility list for pXY
		UtilistUCBMiner pxyUL = new UtilistUCBMiner(py.item, p_size);
		
		// BEGIN UCB-MINEr
		// Initialize the sum of total utility
		long totalUtility = px.sumIutils + px.sumRutils;
		// END UCB-MINEr

        // My first storage  jianzhi
        if (px.elements.get(px.elements.size() - 1).tid < py.elements.get(0).tid || 
            py.elements.get(py.elements.size() - 1).tid < px.elements.get(0).tid ) {
                return null;
            }
            
        // My second storage  HashMap O(nlog(n)) ---> O(n)
        for (Element ex : px.elements) {
            if (!py.pos.containsKey(ex.tid)) {
                totalUtility -= (ex.iutils+ex.rutils);
                if(totalUtility < minUtility) {
                    return null;
                }
				// =============================================== /
				continue;                
            } else {
                Element ey = py.elements.get(py.pos.get(ex.tid));
                if (P == null) {
                    Element eXY = new Element(ex.tid, ex.iutils + ey.iutils, ey.rutils);
				    // add the new element to the utility list of pXY
				    pxyUL.addElement(eXY, p_size);
                } else {
                    // find the element in the utility list of p wih the same tid
                    Element e = P.elements.get(P.pos.get(ex.tid));
                    if(e != null){
                        // Create new element
                        Element eXY = new Element(ex.tid, ex.iutils + ey.iutils - e.iutils,
                                    ey.rutils);
                        // add the new element to the utility list of pXY
                        pxyUL.addElement(eXY, p_size);
                    }
                }
            }
        }
		
		return pxyUL;
	}
	
	/**
	 * Method to write a high utility itemset to the output file.
	 * @param the prefix to be writent o the output file
	 * @param an item to be appended to the prefix
	 * @param utility the utility of the prefix concatenated with the item
	 * @param prefixLength the prefix length
	 */
	private void writeOut(int[] prefix, int prefixLength, int item, long utility) throws IOException {
		huiCount++; // increase the number of high utility itemsets found
		
		//Create a string buffer
		StringBuilder buffer = new StringBuilder();
		// append the prefix
		for (int i = 0; i < prefixLength; i++) {
			buffer.append(prefix[i]);
			buffer.append(' ');
		}
		// append the last item
		buffer.append(item);
		// append the utility value
		buffer.append(" #UTIL: ");
		buffer.append(utility);
		// write to file
		writer.write(buffer.toString());
		writer.newLine();
	}
	
	/**
	 * Print statistics about the latest execution to System.out.
	 */
	public void printStats() {
		System.out.println("=============  UCB-MINER ALGORITHM v0.96r18 - STATS =============");
		System.out.println(" Transaction count: " + transactionCount +
				   		" Partition count: " + p_count );
		System.out.println(" Partition size: " + p_size);
		System.out.println(" Join count: "  + joinCount + " Partial join count:" + partialJoinCount);
		System.out.println(" Total time: " + (endTimestamp - startTimestamp) + " ms");
		System.out.println(" Max. Memory: " + MemoryLogger.getInstance().getMaxMemory() + " MB");
		System.out.println(" High-utility itemsets count: " + huiCount); 
		System.out.println("===================================================");
	}
}