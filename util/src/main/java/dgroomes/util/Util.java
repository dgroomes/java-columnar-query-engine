package dgroomes.util;

import java.text.NumberFormat;
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
}
