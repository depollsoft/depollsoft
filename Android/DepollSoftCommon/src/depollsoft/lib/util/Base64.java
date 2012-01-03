package depollsoft.lib.util;

public class Base64 {

  private static final String base64code = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
      + "abcdefghijklmnopqrstuvwxyz" + "0123456789" + "+/";

  private static final int splitLinesAt = 76;

  public static String encode(byte[] bytes) {
    byte[] stringArray = bytes;
    StringBuffer encoded = new StringBuffer(stringArray.length);

    // determine how many padding bytes to add to the output
    int paddingCount = (3 - (stringArray.length % 3)) % 3;
    // add any necessary padding to the input
    stringArray = Base64
        .zeroPad(stringArray.length + paddingCount, stringArray);
    // process 3 bytes at a time, churning out 4 output bytes
    // worry about CRLF insertions later
    for (int i = 0; i < stringArray.length; i += 3) {
      int j = ((stringArray[i] & 0xff) << 16)
          + ((stringArray[i + 1] & 0xff) << 8) + (stringArray[i + 2] & 0xff);
      encoded.append(Base64.base64code.charAt(j >> 18 & 0x3f));
      encoded.append(Base64.base64code.charAt((j >> 12) & 0x3f));
      encoded.append(Base64.base64code.charAt((j >> 6) & 0x3f));
      encoded.append(Base64.base64code.charAt(j & 0x3f));
    }
    // replace encoded padding nulls with "="
    return Base64.splitLines(encoded.substring(0, encoded.length()
        - paddingCount)
        + "==".substring(0, paddingCount));
  }

  public static String encode(String string) {
    byte[] stringArray;
    try {
      stringArray = string.getBytes("UTF-8");
    }
    catch (Exception ignored) {
      stringArray = string.getBytes();
    }
    return Base64.encode(stringArray);
  }

  public static String splitLines(String string) {

    StringBuffer lines = new StringBuffer(string.length());
    for (int i = 0; i < string.length(); i += Base64.splitLinesAt) {

      lines.append(string.substring(i,
          Math.min(string.length(), i + Base64.splitLinesAt)));
      lines.append("\r\n");

    }
    return lines.toString();

  }

  public static byte[] zeroPad(int length, byte[] bytes) {
    byte[] padded = new byte[length];
    System.arraycopy(bytes, 0, padded, 0, bytes.length);
    return padded;
  }

}