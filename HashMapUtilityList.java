import java.util.TreeMap;


public class HashMapUtilityList {
    public final Integer item;  // the item
    public long sumIutils = 0;  // the sum of item utilities
    public long sumRutils = 0;  // the sum of remaining utilities
    public int minPartID = Integer.MAX_VALUE - 1;
    public int maxPartID = -1;


    // <Pid, Partition--Tids>
    public final TreeMap<Integer, Long> element1 = new TreeMap<>();  // the element1

    // <Tid, {iutil, rutil}>
    public final TreeMap<Integer, PairIandR> element2 = new TreeMap<>();  // the element2

    // <Pid, {SumIutil, SumRutil}>
    public final TreeMap<Integer, PairSIandSR> element3 = new TreeMap<>(); // the element3


    /**
     * Constructor.
     * @param item the item that is used for this utility list
     */
    public HashMapUtilityList(Integer item) {
        this.item = item;
    }

    /**
     * Method to add an element to this utility list and update the sums at the same time.
     */
    public void addElement2(int tid, PairIandR element, int p_size) {
        sumIutils += element.iutils;
        sumRutils += element.rutils;
        element2.put(tid, element);
        int i = tid / p_size;
        if (element3.containsKey(i)) {
            PairSIandSR var = new PairSIandSR(element3.get(i).SumIutils + element.iutils,
                                                element3.get(i).SumRutils + element.rutils);
            element3.put(i, var);
            maxPartID = Math.max(maxPartID, i);
            minPartID = Math.min(minPartID, i);
        } else {
            PairSIandSR var = new PairSIandSR(element.iutils, element.rutils);
            element3.put(i, var);
            maxPartID = Math.max(maxPartID, i);
            minPartID = Math.min(minPartID, i);
        }
    }

    public void setElement1(int psize) {
        // element2
        element2.forEach((k, v) -> {
            int i = k / psize;
            int j = k % psize;
            if (element1.containsKey(i)) {
                element1.put(i, element1.get(i) + (long) (Math.pow(2, j)));
                PairSIandSR pair = new PairSIandSR(element3.get(i).SumIutils + v.iutils, element3.get(i).SumRutils + v.rutils);
                element3.put(i, pair);
                maxPartID = Math.max(maxPartID, i);
                minPartID = Math.min(minPartID, i);
            } else {
                element1.put(i, (long) (Math.pow(2, j)));
                PairSIandSR pair = new PairSIandSR(v.iutils, v.rutils);
                element3.put(i, pair);
                maxPartID = Math.max(maxPartID, i);
                minPartID = Math.min(minPartID, i);
            }
        });
    }
}