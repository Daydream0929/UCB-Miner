/**
 * This class represents an item's utility and remaining utility in a
 * transaction.
 * It is an immutable class.
 */
class PairIandR {
    /**
     * The item utility.
     */
    public final int iutils;

    /**
     * The remaining utility.
     */
    public final int rutils;

    /**
     * Constructor for creating a PairIandR object.
     *
     * @param iutils The item utility.
     * @param rutils The remaining utility.
     */
    public PairIandR(int iutils, int rutils) {
        this.iutils = iutils;
        this.rutils = rutils;
    }
}