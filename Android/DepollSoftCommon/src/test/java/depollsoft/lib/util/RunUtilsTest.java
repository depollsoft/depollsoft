package depollsoft.lib.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class RunUtilsTest {

  @Before
  public void setUp() {
    // Reset any static state if needed
  }

  @Test
  public void testRunOnce_FirstCall_ReturnsTrue() {
    try (MockedStatic<Preferences> mockedPreferences = Mockito.mockStatic(Preferences.class)) {
      // Arrange
      String testKey = "test_key";
      String expectedPrefKey = "depollsoft.lib.RunOnce.test_key";
      mockedPreferences.when(() -> Preferences.get(expectedPrefKey)).thenReturn(null);
      
      // Act
      boolean result = RunUtils.runOnce(testKey);
      
      // Assert
      assertTrue("First call should return true", result);
      mockedPreferences.verify(() -> Preferences.get(expectedPrefKey));
      mockedPreferences.verify(() -> Preferences.set(expectedPrefKey, true));
    }
  }

  @Test
  public void testRunOnce_SecondCall_ReturnsFalse() {
    try (MockedStatic<Preferences> mockedPreferences = Mockito.mockStatic(Preferences.class)) {
      // Arrange
      String testKey = "test_key";
      String expectedPrefKey = "depollsoft.lib.RunOnce.test_key";
      mockedPreferences.when(() -> Preferences.get(expectedPrefKey)).thenReturn(true);
      
      // Act
      boolean result = RunUtils.runOnce(testKey);
      
      // Assert
      assertFalse("Second call should return false", result);
      mockedPreferences.verify(() -> Preferences.get(expectedPrefKey));
      mockedPreferences.verify(() -> Preferences.set(anyString(), any()), never());
    }
  }

  @Test
  public void testRunOnce_DifferentKeys_BothReturnTrue() {
    try (MockedStatic<Preferences> mockedPreferences = Mockito.mockStatic(Preferences.class)) {
      // Arrange
      String testKey1 = "test_key_1";
      String testKey2 = "test_key_2";
      String expectedPrefKey1 = "depollsoft.lib.RunOnce.test_key_1";
      String expectedPrefKey2 = "depollsoft.lib.RunOnce.test_key_2";
      mockedPreferences.when(() -> Preferences.get(expectedPrefKey1)).thenReturn(null);
      mockedPreferences.when(() -> Preferences.get(expectedPrefKey2)).thenReturn(null);
      
      // Act
      boolean result1 = RunUtils.runOnce(testKey1);
      boolean result2 = RunUtils.runOnce(testKey2);
      
      // Assert
      assertTrue("First key should return true", result1);
      assertTrue("Second key should return true", result2);
      mockedPreferences.verify(() -> Preferences.get(expectedPrefKey1));
      mockedPreferences.verify(() -> Preferences.get(expectedPrefKey2));
      mockedPreferences.verify(() -> Preferences.set(expectedPrefKey1, true));
      mockedPreferences.verify(() -> Preferences.set(expectedPrefKey2, true));
    }
  }

  @Test
  public void testRunOnce_EmptyKey_WorksCorrectly() {
    try (MockedStatic<Preferences> mockedPreferences = Mockito.mockStatic(Preferences.class)) {
      // Arrange
      String testKey = "";
      String expectedPrefKey = "depollsoft.lib.RunOnce.";
      mockedPreferences.when(() -> Preferences.get(expectedPrefKey)).thenReturn(null);
      
      // Act
      boolean result = RunUtils.runOnce(testKey);
      
      // Assert
      assertTrue("Empty key should work", result);
      mockedPreferences.verify(() -> Preferences.get(expectedPrefKey));
      mockedPreferences.verify(() -> Preferences.set(expectedPrefKey, true));
    }
  }

  @Test
  public void testRunOnce_KeyWithSpecialCharacters_WorksCorrectly() {
    try (MockedStatic<Preferences> mockedPreferences = Mockito.mockStatic(Preferences.class)) {
      // Arrange
      String testKey = "test.key-with_special@chars";
      String expectedPrefKey = "depollsoft.lib.RunOnce.test.key-with_special@chars";
      mockedPreferences.when(() -> Preferences.get(expectedPrefKey)).thenReturn(null);
      
      // Act
      boolean result = RunUtils.runOnce(testKey);
      
      // Assert
      assertTrue("Key with special characters should work", result);
      mockedPreferences.verify(() -> Preferences.get(expectedPrefKey));
      mockedPreferences.verify(() -> Preferences.set(expectedPrefKey, true));
    }
  }

  @Test
  public void testRunOnce_VerifyPrefKeyFormat() {
    try (MockedStatic<Preferences> mockedPreferences = Mockito.mockStatic(Preferences.class)) {
      // Arrange
      String testKey = "example";
      String expectedPrefKey = "depollsoft.lib.RunOnce.example";
      mockedPreferences.when(() -> Preferences.get(expectedPrefKey)).thenReturn(null);
      
      // Act
      RunUtils.runOnce(testKey);
      
      // Assert - verify the exact preference key format
      mockedPreferences.verify(() -> Preferences.get(expectedPrefKey));
      mockedPreferences.verify(() -> Preferences.set(expectedPrefKey, true));
    }
  }

  @Test
  public void testRunOnce_WhenPreferenceExistsWithDifferentValue_ReturnsFalse() {
    try (MockedStatic<Preferences> mockedPreferences = Mockito.mockStatic(Preferences.class)) {
      // Arrange
      String testKey = "test_key";
      String expectedPrefKey = "depollsoft.lib.RunOnce.test_key";
      mockedPreferences.when(() -> Preferences.get(expectedPrefKey)).thenReturn("some_value");
      
      // Act
      boolean result = RunUtils.runOnce(testKey);
      
      // Assert
      assertFalse("Should return false when preference exists with any value", result);
      mockedPreferences.verify(() -> Preferences.get(expectedPrefKey));
      mockedPreferences.verify(() -> Preferences.set(anyString(), any()), never());
    }
  }

  @Test
  public void testRunOnce_NullKey_ThrowsException() {
    // Act & Assert
    try {
      RunUtils.runOnce(null);
      fail("Should throw exception for null key");
    } catch (NullPointerException e) {
      // Expected
    }
  }
}