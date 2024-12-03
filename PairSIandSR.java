/**
 * This class represents the sum of item utility and sum of remaining utility
 * for an item in a set of transactions. It is an immutable class.
 */
class PairSIandSR {
    /**
     * The sum of item utility.
     */
    public final long SumIutils;

    /**
     * The sum of remaining utility.
     */
    public final long SumRutils;

    /**
     * Constructor for creating a PairSIandSR object.
     *
     * @param SumIutils The sum of item utility.
     * @param SumRutils The sum of remaining utility.
     */
    public PairSIandSR(long SumIutils, long SumRutils) {
        this.SumIutils = SumIutils;
        this.SumRutils = SumRutils;
    }
}