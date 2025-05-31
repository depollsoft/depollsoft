package depollsoft.lib.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class Base64Test {

  @Test
  public void testEncodeString_SimpleText() {
    // Arrange
    String input = "Hello";
    
    // Act
    String result = Base64.encode(input);
    
    // Assert
    assertNotNull("Encoded result should not be null", result);
    assertFalse("Encoded result should not be empty", result.isEmpty());
    assertTrue("Encoded result should contain base64 characters", result.matches("[A-Za-z0-9+/=\r\n]*"));
  }

  @Test
  public void testEncodeString_EmptyString() {
    // Arrange
    String input = "";
    
    // Act
    String result = Base64.encode(input);
    
    // Assert
    assertNotNull("Encoded empty string should not be null", result);
    assertEquals("Empty string should encode to line break", "\r\n", result);
  }

  @Test
  public void testEncodeString_WithSpecialCharacters() {
    // Arrange
    String input = "Hello, World! @#$%^&*()";
    
    // Act
    String result = Base64.encode(input);
    
    // Assert
    assertNotNull("Encoded result should not be null", result);
    assertTrue("Encoded result should contain base64 characters", result.matches("[A-Za-z0-9+/=\r\n]*"));
  }

  @Test
  public void testEncodeBytes_SimpleArray() {
    // Arrange
    byte[] input = {72, 101, 108, 108, 111}; // "Hello" in bytes
    
    // Act
    String result = Base64.encode(input);
    
    // Assert
    assertNotNull("Encoded result should not be null", result);
    assertTrue("Encoded result should contain base64 characters", result.matches("[A-Za-z0-9+/=\r\n]*"));
  }

  @Test
  public void testEncodeBytes_EmptyArray() {
    // Arrange
    byte[] input = {};
    
    // Act
    String result = Base64.encode(input);
    
    // Assert
    assertNotNull("Encoded empty array should not be null", result);
    assertEquals("Empty array should encode to line break", "\r\n", result);
  }

  @Test
  public void testEncodeBytes_SingleByte() {
    // Arrange
    byte[] input = {65}; // 'A'
    
    // Act
    String result = Base64.encode(input);
    
    // Assert
    assertNotNull("Encoded result should not be null", result);
    assertTrue("Encoded result should contain base64 characters", result.matches("[A-Za-z0-9+/=\r\n]*"));
    assertTrue("Single byte should require padding", result.contains("="));
  }

  @Test
  public void testEncodeBytes_TwoBytes() {
    // Arrange
    byte[] input = {65, 66}; // 'AB'
    
    // Act
    String result = Base64.encode(input);
    
    // Assert
    assertNotNull("Encoded result should not be null", result);
    assertTrue("Encoded result should contain base64 characters", result.matches("[A-Za-z0-9+/=\r\n]*"));
    assertTrue("Two bytes should require padding", result.contains("="));
  }

  @Test
  public void testEncodeBytes_ThreeBytes() {
    // Arrange
    byte[] input = {65, 66, 67}; // 'ABC'
    
    // Act
    String result = Base64.encode(input);
    
    // Assert
    assertNotNull("Encoded result should not be null", result);
    assertTrue("Encoded result should contain base64 characters", result.matches("[A-Za-z0-9+/=\r\n]*"));
  }

  @Test
  public void testSplitLines_ShortString() {
    // Arrange
    String input = "Hello";
    
    // Act
    String result = Base64.splitLines(input);
    
    // Assert
    assertNotNull("Split lines result should not be null", result);
    assertTrue("Result should end with CRLF", result.endsWith("\r\n"));
    assertTrue("Result should contain input", result.contains(input));
  }

  @Test
  public void testSplitLines_LongString() {
    // Arrange
    StringBuilder longString = new StringBuilder();
    for (int i = 0; i < 100; i++) {
      longString.append("A");
    }
    String input = longString.toString();
    
    // Act
    String result = Base64.splitLines(input);
    
    // Assert
    assertNotNull("Split lines result should not be null", result);
    assertTrue("Result should contain multiple CRLF", result.indexOf("\r\n") != result.lastIndexOf("\r\n"));
  }

  @Test
  public void testSplitLines_EmptyString() {
    // Arrange
    String input = "";
    
    // Act
    String result = Base64.splitLines(input);
    
    // Assert
    assertEquals("Empty string should result in just CRLF", "\r\n", result);
  }

  @Test
  public void testZeroPad_SmallerArray() {
    // Arrange
    byte[] input = {1, 2, 3};
    int targetLength = 5;
    
    // Act
    byte[] result = Base64.zeroPad(targetLength, input);
    
    // Assert
    assertEquals("Result should have target length", targetLength, result.length);
    assertEquals("First byte should be preserved", 1, result[0]);
    assertEquals("Second byte should be preserved", 2, result[1]);
    assertEquals("Third byte should be preserved", 3, result[2]);
    assertEquals("Fourth byte should be zero", 0, result[3]);
    assertEquals("Fifth byte should be zero", 0, result[4]);
  }

  @Test
  public void testZeroPad_SameLength() {
    // Arrange
    byte[] input = {1, 2, 3};
    int targetLength = 3;
    
    // Act
    byte[] result = Base64.zeroPad(targetLength, input);
    
    // Assert
    assertEquals("Result should have target length", targetLength, result.length);
    assertArrayEquals("Result should equal input when same length", input, result);
  }

  @Test
  public void testZeroPad_EmptyArray() {
    // Arrange
    byte[] input = {};
    int targetLength = 3;
    
    // Act
    byte[] result = Base64.zeroPad(targetLength, input);
    
    // Assert
    assertEquals("Result should have target length", targetLength, result.length);
    assertEquals("All bytes should be zero", 0, result[0]);
    assertEquals("All bytes should be zero", 0, result[1]);
    assertEquals("All bytes should be zero", 0, result[2]);
  }

  @Test
  public void testConsistentEncoding() {
    // Test that encoding the same input multiple times produces the same result
    String input = "Test string for consistency";
    
    String result1 = Base64.encode(input);
    String result2 = Base64.encode(input);
    
    assertEquals("Multiple encodings of same input should produce same result", result1, result2);
  }
}