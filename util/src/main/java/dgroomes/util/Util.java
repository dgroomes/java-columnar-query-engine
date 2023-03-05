package dgroomes.util;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Locale;

public class Util {
  /**
   * Formats an integer value with commas.
   * <p>
   * For example, 1234567 becomes "1,234,567".
   */
  public static String formatInteger(int value) {
    return NumberFormat.getNumberInstance(Locale.US).format(value);
  }

  /**
   * Given two integer arrays that are each sorted in ascending order, return the intersection of the two arrays.
   * <p>
   * For example, given the arrays [1, 2, 3, 4, 5] and [2, 4, 6, 8, 10], the intersection is [2, 4].
   *
   * @param a an array of integers sorted in ascending order
   * @param b an array of integers sorted in ascending order
   * @return the intersection of the two arrays, sorted in ascending order
   */
  public static int[] zipperIntersection(int[] a, int[] b) {
    // The intersection can be as large as the smaller of the two arrays. We use this to initialize the intersection
    // array. Still, this is optimistic. The intersection will usually be smaller than this. Take note that at the end
    // of this method we have to return a slice of this array only up to the index of the largest (i.e. last)
    // intersection value.
    int maxSize = Math.min(a.length, b.length);
    int[] inter = new int[maxSize];

    int interIdx = 0, aIdx = 0, bIdx = 0;

    // While we haven't reached the end of either array, we can continue to look for points of intersection. The
    // procedure resembles a zipper.
    while (aIdx < a.length && bIdx < b.length) {
      int aHead = a[aIdx];
      int bHead = b[bIdx];

      if (aHead == bHead) {
        // The head values are equal. This is a point of intersection. We record the value and advance both indices.
        inter[interIdx++] = aHead;
        aIdx++;
        bIdx++;
      } else if (aHead < bHead) {
        // The head value of 'a' is less than the head value of 'b'. We need to look at higher values in 'a'.
        aIdx++;
      } else {
        // The head value of 'b' is less than the head value of 'a'. We need to look at higher values in 'b'.
        bIdx++;
      }
    }

    a = Arrays.copyOf(inter, interIdx);
    return a;
  }
}
