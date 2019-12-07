package org.tinymediamanager.scraper.util;

public class TvUtils {

  /**
   * gets the first episode number >0 from all supplied parameters, in a safe way<br>
   * order matters!
   * 
   * @param numbers
   *          epNr, DVDnr, aired, combined
   * @return episode number >0, or -1
   */
  public static int getEpisodeNumber(Object... numbers) {
    return getFirstValidNumber(-1, 1, numbers);
  }

  /**
   * gets the first season number >-1 from all supplied parameters, in a safe way<br>
   * order matters!
   * 
   * @param numbers
   *          epNr, DVDnr, aired, combined
   * @return season number 0+, or -1
   */
  public static int getSeasonNumber(Object... numbers) {
    return getFirstValidNumber(-1, 0, numbers);
  }

  /**
   * parses the parameters for first valid number from all supplied parameters, in a safe way<br>
   * order matters!
   * 
   * @param numbers
   *          epNr, DVDnr, aired, combined
   * @return number >0, or 0 as default
   */
  public static int parseInt(Object... numbers) {
    return getFirstValidNumber(0, 0, numbers);
  }

  static int getFirstValidNumber(int defaultValue, int minimumValue, Object... numbers) {
    int nr = defaultValue;
    if (numbers != null) {
      for (Object o : numbers) {
        if (o == null) {
          continue;
        }
        if (nr <= minimumValue) {
          if (o instanceof String) {
            String s = (String) o;
            if (!s.isEmpty()) {
              try {
                nr = Integer.parseInt(s);
              }
              catch (NumberFormatException e) {
                nr = (int) Math.floor(Double.parseDouble(s));
              }
            }
          }
          else if (o instanceof Integer) {
            nr = (int) o;
          }
          else if (o instanceof Double) {
            nr = (int) Math.floor((Double) o);
          }
          else if (o instanceof Float) {
            nr = (int) Math.floor((Float) o);
          }
        }
      }
    }
    if (nr < minimumValue) {
      nr = defaultValue;
    }
    return nr;
  }

}
