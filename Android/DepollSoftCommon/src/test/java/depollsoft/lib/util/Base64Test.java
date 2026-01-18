package depollsoft.lib.util;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for the Base64 utility class.
 */
public class Base64Test {

    // =====================================================================
    // encode(String) tests
    // =====================================================================

    @Test
    public void encode_emptyString_returnsEmptyEncodedString() {
        String result = Base64.encode("");
        // Empty string results in empty output (splitLines returns empty for empty input)
        assertNotNull(result);
        assertEquals("", result);
    }

    @Test
    public void encode_singleCharacter_returnsValidBase64() {
        // 'a' in ASCII is 97, which encodes to "YQ=="
        String result = Base64.encode("a");
        assertEquals("YQ==\r\n", result);
    }

    @Test
    public void encode_twoCharacters_returnsValidBase64WithOnePadding() {
        // 'ab' encodes to "YWI="
        String result = Base64.encode("ab");
        assertEquals("YWI=\r\n", result);
    }

    @Test
    public void encode_threeCharacters_returnsValidBase64NoPadding() {
        // 'abc' encodes to "YWJj" (no padding needed)
        String result = Base64.encode("abc");
        assertEquals("YWJj\r\n", result);
    }

    @Test
    public void encode_helloWorld_returnsCorrectBase64() {
        // "Hello, World!" standard test case
        String result = Base64.encode("Hello, World!");
        assertEquals("SGVsbG8sIFdvcmxkIQ==\r\n", result);
    }

    @Test
    public void encode_numericString_returnsCorrectBase64() {
        // "123456" encodes to "MTIzNDU2"
        String result = Base64.encode("123456");
        assertEquals("MTIzNDU2\r\n", result);
    }

    @Test
    public void encode_specialCharacters_handlesCorrectly() {
        // Test with special characters
        String input = "!@#$%^&*()";
        String result = Base64.encode(input);
        assertNotNull(result);
        assertTrue(result.endsWith("\r\n"));
        // Ensure it only contains valid Base64 characters plus padding and newlines
        String withoutNewline = result.replace("\r\n", "");
        assertTrue(withoutNewline.matches("[A-Za-z0-9+/=]+"));
    }

    @Test
    public void encode_unicodeCharacters_handlesUtf8() {
        // Test with unicode characters
        String input = "こんにちは"; // Japanese "Hello"
        String result = Base64.encode(input);
        assertNotNull(result);
        assertTrue(result.length() > 0);
    }

    // =====================================================================
    // encode(byte[]) tests
    // =====================================================================

    @Test
    public void encodeBytes_emptyArray_returnsEmptyEncodedString() {
        byte[] input = new byte[0];
        String result = Base64.encode(input);
        // Empty input produces empty output
        assertEquals("", result);
    }

    @Test
    public void encodeBytes_singleByte_returnsValidBase64() {
        byte[] input = { 0x00 };
        String result = Base64.encode(input);
        assertEquals("AA==\r\n", result);
    }

    @Test
    public void encodeBytes_twoBytes_returnsValidBase64WithOnePadding() {
        byte[] input = { 0x00, 0x01 };
        String result = Base64.encode(input);
        assertEquals("AAE=\r\n", result);
    }

    @Test
    public void encodeBytes_threeBytes_returnsValidBase64NoPadding() {
        byte[] input = { 0x00, 0x01, 0x02 };
        String result = Base64.encode(input);
        assertEquals("AAEC\r\n", result);
    }

    @Test
    public void encodeBytes_allZeros_encodesCorrectly() {
        byte[] input = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
        String result = Base64.encode(input);
        assertEquals("AAAAAAAA\r\n", result);
    }

    @Test
    public void encodeBytes_allOnes_encodesCorrectly() {
        byte[] input = { (byte)0xFF, (byte)0xFF, (byte)0xFF };
        String result = Base64.encode(input);
        assertEquals("////\r\n", result);
    }

    @Test
    public void encodeBytes_binaryData_producesValidBase64() {
        byte[] input = { 0x4D, 0x61, 0x6E }; // "Man" in ASCII
        String result = Base64.encode(input);
        assertEquals("TWFu\r\n", result);
    }

    // =====================================================================
    // splitLines tests
    // =====================================================================

    @Test
    public void splitLines_shortString_addsCRLF() {
        String result = Base64.splitLines("ABC");
        assertEquals("ABC\r\n", result);
    }

    @Test
    public void splitLines_emptyString_returnsEmpty() {
        String result = Base64.splitLines("");
        // Empty string input returns empty string (loop never executes)
        assertEquals("", result);
    }

    @Test
    public void splitLines_exactlyAtLimit_splitsCorrectly() {
        // Create a string of exactly 76 characters
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 76; i++) {
            sb.append('A');
        }
        String input = sb.toString();
        String result = Base64.splitLines(input);
        assertEquals(input + "\r\n", result);
    }

    @Test
    public void splitLines_overLimit_splitsIntoMultipleLines() {
        // Create a string longer than 76 characters
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append('A');
        }
        String input = sb.toString();
        String result = Base64.splitLines(input);
        
        String[] lines = result.split("\r\n");
        assertEquals(2, lines.length);
        assertEquals(76, lines[0].length());
        assertEquals(24, lines[1].length());
    }

    @Test
    public void splitLines_exactlyDoubleLimit_splitsIntoTwoLines() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 152; i++) {
            sb.append('A');
        }
        String input = sb.toString();
        String result = Base64.splitLines(input);
        
        String[] lines = result.split("\r\n");
        assertEquals(2, lines.length);
    }

    // =====================================================================
    // zeroPad tests
    // =====================================================================

    @Test
    public void zeroPad_sameLength_returnsCopy() {
        byte[] input = { 1, 2, 3 };
        byte[] result = Base64.zeroPad(3, input);
        assertArrayEquals(input, result);
    }

    @Test
    public void zeroPad_longerLength_padsWithZeros() {
        byte[] input = { 1, 2, 3 };
        byte[] result = Base64.zeroPad(6, input);
        assertEquals(6, result.length);
        assertEquals(1, result[0]);
        assertEquals(2, result[1]);
        assertEquals(3, result[2]);
        assertEquals(0, result[3]);
        assertEquals(0, result[4]);
        assertEquals(0, result[5]);
    }

    @Test
    public void zeroPad_emptyInput_returnsAllZeros() {
        byte[] input = {};
        byte[] result = Base64.zeroPad(3, input);
        assertEquals(3, result.length);
        assertEquals(0, result[0]);
        assertEquals(0, result[1]);
        assertEquals(0, result[2]);
    }

    @Test
    public void zeroPad_zeroLength_returnsEmptyArray() {
        byte[] input = {};
        byte[] result = Base64.zeroPad(0, input);
        assertEquals(0, result.length);
    }

    // =====================================================================
    // Edge case tests
    // =====================================================================

    @Test
    public void encode_longString_handlesMultipleLines() {
        // Create a string that will result in multiple lines of Base64
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("LongString");
        }
        String result = Base64.encode(sb.toString());
        assertNotNull(result);
        assertTrue(result.contains("\r\n"));
    }

    @Test
    public void encode_whitespaceString_encodesCorrectly() {
        String result = Base64.encode("   ");
        assertEquals("ICAg\r\n", result);
    }

    @Test
    public void encode_newlineCharacters_encodesCorrectly() {
        String result = Base64.encode("\n\r\n");
        assertNotNull(result);
        assertTrue(result.length() > 0);
    }

    @Test
    public void encodeBytes_negativeByteValues_handlesCorrectly() {
        // Test with bytes that have the high bit set (negative in Java)
        byte[] input = { (byte)0x80, (byte)0x90, (byte)0xA0 };
        String result = Base64.encode(input);
        assertNotNull(result);
        // Verify only valid Base64 chars (plus padding and newlines)
        String withoutNewline = result.replace("\r\n", "");
        assertTrue(withoutNewline.matches("[A-Za-z0-9+/=]+"));
    }
}
