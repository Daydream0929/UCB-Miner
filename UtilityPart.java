/**
 * 
 *
 * @see AlgoHUIMiner
 * @see Element
 * @author
 */
public class UtilityListPart {
    public Integer part_size;
	public Integer item;  // the item
	public long sumIutils = 0;
	public long sumRutils = 0;
	public int[] tidsets;
    public int[] Suru;
    public int[] support;
	 
	/**
	 * Constructor.
	 * @param item the item that is used for this utility list
	 */
	public UtilityListPart(Integer item, Integer part_size){
		this.item = item;
        this.part_size = part_size;
		this.tidsets = new int[part_size];
		this.Suru = new int[part_size];
		this.support = new int[part_size];
    }
}