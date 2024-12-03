import java.util.HashMap;


/**
 * This class represents a Partitioned Utility List as used by the UCB-Miner algorithm.
 *
 * @see AlgoUCB-Miner
 * @see UtilityList
 * @see Element
 * @author Philippe Fournier-Viger
 */
class UtilistUCBMiner extends UtilityList {
	/** this variable stores the sum of SU+RU for each partition */
	int[] partitionSURU;
	HashMap<Integer, Integer> pos = new HashMap<Integer, Integer>();
	 
	/**
	 * Constructor
	 * @param item the item that is used for this utility list
	 * @param p_count the number of partitions
	 */
	public UtilistUCBMiner(int item, int p_count){
		super(item);
		this.partitionSURU = new int[p_count];
	}
	
	/**
	 * Method to add an element to this utility list and update the sums at the same time.
	 * @param element the element to be added
	 * @param partitionSize the size of the partition
	 */
	public void addElement(Element element, int partitionSize){
		
		sumIutils += element.iutils;
		sumRutils += element.rutils;
		elements.add(element);

		pos.put(element.tid, elements.size() - 1);
		
		// update the sum of utility and remaining utility for the partition
		// corresponding to this element
		int partition = element.tid / partitionSize;
		partitionSURU[partition] += element.iutils + element.rutils;
	}
}