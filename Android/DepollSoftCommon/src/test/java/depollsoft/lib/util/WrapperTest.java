package depollsoft.lib.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class WrapperTest {

  @Test
  public void testWrapperCreation_WithInitialValue() {
    // Arrange
    String testValue = "test string";
    
    // Act
    Wrapper<String> wrapper = new Wrapper<>(testValue);
    
    // Assert
    assertEquals("Initial value should be set correctly", testValue, wrapper.getValue());
  }

  @Test
  public void testWrapperCreation_WithNullValue() {
    // Act
    Wrapper<String> wrapper = new Wrapper<>(null);
    
    // Assert
    assertNull("Wrapper should accept null initial value", wrapper.getValue());
  }

  @Test
  public void testSetValue() {
    // Arrange
    Wrapper<Integer> wrapper = new Wrapper<>(0);
    Integer newValue = 42;
    
    // Act
    wrapper.setValue(newValue);
    
    // Assert
    assertEquals("Value should be updated correctly", newValue, wrapper.getValue());
  }

  @Test
  public void testSetValue_WithNull() {
    // Arrange
    Wrapper<String> wrapper = new Wrapper<>("initial");
    
    // Act
    wrapper.setValue(null);
    
    // Assert
    assertNull("Wrapper should accept null value", wrapper.getValue());
  }

  @Test
  public void testValueChange() {
    // Arrange
    Wrapper<String> wrapper = new Wrapper<>("initial");
    String newValue = "changed";
    
    // Act
    wrapper.setValue(newValue);
    
    // Assert
    assertEquals("Value should change from initial to new value", newValue, wrapper.getValue());
    assertNotEquals("Value should no longer equal initial value", "initial", wrapper.getValue());
  }

  @Test
  public void testGenericTypes() {
    // Test with different types
    Wrapper<Integer> intWrapper = new Wrapper<>(123);
    Wrapper<Boolean> boolWrapper = new Wrapper<>(true);
    Wrapper<Double> doubleWrapper = new Wrapper<>(3.14);
    
    // Assert
    assertEquals("Integer wrapper should work", Integer.valueOf(123), intWrapper.getValue());
    assertEquals("Boolean wrapper should work", Boolean.TRUE, boolWrapper.getValue());
    assertEquals("Double wrapper should work", Double.valueOf(3.14), doubleWrapper.getValue(), 0.001);
  }

  @Test
  public void testMultipleValueChanges() {
    // Arrange
    Wrapper<String> wrapper = new Wrapper<>("first");
    
    // Act & Assert
    assertEquals("Initial value", "first", wrapper.getValue());
    
    wrapper.setValue("second");
    assertEquals("Second value", "second", wrapper.getValue());
    
    wrapper.setValue("third");
    assertEquals("Third value", "third", wrapper.getValue());
  }
}