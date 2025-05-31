package depollsoft.lib.kotlin;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
  @Test
  public void addition_isCorrect() throws Exception {
    assertEquals(4, 2 + 2);
  }

  @Test
  public void subtraction_isCorrect() {
    assertEquals(2, 4 - 2);
  }

  @Test
  public void multiplication_isCorrect() {
    assertEquals(8, 4 * 2);
  }

  @Test
  public void division_isCorrect() {
    assertEquals(2, 4 / 2);
  }

  @Test
  public void stringConcatenation_isCorrect() {
    String result = "Hello" + " " + "World";
    assertEquals("Hello World", result);
  }

  @Test
  public void arrayLength_isCorrect() {
    int[] array = {1, 2, 3, 4, 5};
    assertEquals(5, array.length);
  }

  @Test
  public void booleanLogic_isCorrect() {
    assertTrue(true && true);
    assertFalse(true && false);
    assertTrue(true || false);
    assertFalse(false && false);
  }
}