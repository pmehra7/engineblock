package io.engineblock.activityimpl.tracking;

/**
 * <p>Using the 64 bit structure of a long as a heap addressed
 * tracker, where the leaf nodes represent marked values and
 * all others are used to consolidate state.</p>
 * <p>
 * <p>One bit is ignored, the 2s compliment sign, leaving 63 bits:
 * 31 bits for root and path and 32 bits as leaf nodes.</p>
 * <p>
 * <p>Each time a leaf node is marked as complete, it's sibling
 * is checked for the same. While both are marked, the same
 * process is checked for its parent and its sibling, and so forth</p>
 * <p>
 * <p>This approach assumes that it is good to lower contention and
 * retries for atomics when there are many threads active against
 * the tracker. It should be benchmarked with simpler methods
 * to see the complexity is worth it.</p>
 */
public class LongTreeTracker {

    long timage = 0L;

    private static long odds = 0b0101010101010101010101010101010101010101010101010101010101010101L;
    private static long eens = 0b1010101010101010101010101010101010101010101010101010101010101010L;

    public LongTreeTracker(long timage) {
        this.timage = timage;
    }

    public LongTreeTracker() {
    }

//    public long parentOf(int posFromLSB) {
//        return 1 << (posFromLSB >> 1);
//    }

    private static long leftbit = 0b1000000000000000000000000000000000000000000000000000000000000000L;

    /**
     * Apply an index value between 0 and 31 inclusive. Return the accumulator.
     * If all 32 slots of this tracker have been completed, the returned value will
     * have LSB bit 2 set.
     * @param index a long value between 0 and 31 to mark as complete
     * @return the accumulator
     */
    public long applyPosition(int index) {
        long position = 63 - index;

        while (position > 0) {
            long applybt = 1L << position;
//            System.out.println("applybt:\n" + diagString(applybt));
//            System.out.print("image:\n" + this);
            timage |= applybt;
            long comask = applybt | (applybt & eens) >> 1 | (applybt & odds) << 1;
//            System.out.println("comask:\n" + diagString(comask));
            if ((comask & timage) != comask) {
                break;
            }
            position >>= 1;
        }
//        System.out.println("image:\n" + this);
        return timage;
    }

    public boolean completed(long index) {
        long l = leftbit >>> index;
        return (timage & l ) == l;
    }

    public long completed() {
        long l = Long.lowestOneBit(timage >> 32);
        return l; // not yet
    }

    public static String toBinaryString(long bitfield) {
        String s = Long.toBinaryString(bitfield);
        s = "0000000000000000000000000000000000000000000000000000000000000000".substring(s.length()) + s;
        return s;
    }

    public String toString() {
        return diagString(this.timage);
    }

    public static String diagString(long bitfield) {
        String s = toBinaryString(bitfield);
        String[] n = new String[64];
        for (int i = 0; i < n.length; i++) {
            n[i] = s.substring(i, i + 1);
        }

        String space = "                                                                ";
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 32; i++) {
            sb.append(n[i]).append(" ");
        }
        sb.append("\n");

        sb.append(" ");
        for (int i = 32; i < 48; i++) {
            sb.append(n[i]).append("   ");
        }
        sb.append("\n");

        sb.append("   ");
        for (int i = 48; i < 56; i++) {
            sb.append(n[i]).append("       ");
        }
        sb.append("\n");

        sb.append("       ");
        for (int i = 56; i < 60; i++) {
            sb.append(n[i]).append("               ");
        }
        sb.append("\n");

        sb.append("               ");
        for (int i = 60; i < 62; i++) {
            sb.append(n[i]).append("                               ");
        }
        sb.append("\n");

        sb.append("                               ").append(n[62]).append("\n");
        sb.append("                               ").append(n[63]).append("\n");

        return sb.toString();
    }

    public long getImage() {
        return timage;
    }

}
