package depollsoft.lib.compat;

import org.junit.Test;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CompatibilityTest {

  @Mock
  private RunnableFactory mockRunnableFactory;
  
  @Mock
  private Runnable mockRunnable;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testTryWithFallback_SuccessfulExecution_ReturnsTrue() {
    // Arrange
    when(mockRunnableFactory.create()).thenReturn(mockRunnable);
    
    // Act
    boolean result = Compatibility.tryWithFallback(mockRunnableFactory);
    
    // Assert
    assertTrue("Should return true when execution succeeds", result);
    verify(mockRunnableFactory).create();
    verify(mockRunnable).run();
  }

  @Test
  public void testTryWithFallback_WithNoSuchFieldError_ReturnsFalse() {
    // Arrange
    when(mockRunnableFactory.create()).thenReturn(mockRunnable);
    doThrow(new NoSuchFieldError("Test error")).when(mockRunnable).run();
    
    // Act
    boolean result = Compatibility.tryWithFallback(mockRunnableFactory);
    
    // Assert
    assertFalse("Should return false when NoSuchFieldError is thrown", result);
    verify(mockRunnableFactory).create();
    verify(mockRunnable).run();
  }

  @Test
  public void testTryWithFallback_WithNoSuchMethodError_ReturnsFalse() {
    // Arrange
    when(mockRunnableFactory.create()).thenReturn(mockRunnable);
    doThrow(new NoSuchMethodError("Test error")).when(mockRunnable).run();
    
    // Act
    boolean result = Compatibility.tryWithFallback(mockRunnableFactory);
    
    // Assert
    assertFalse("Should return false when NoSuchMethodError is thrown", result);
    verify(mockRunnableFactory).create();
    verify(mockRunnable).run();
  }

  @Test
  public void testTryWithFallback_WithNoClassDefFoundError_ReturnsFalse() {
    // Arrange
    when(mockRunnableFactory.create()).thenReturn(mockRunnable);
    doThrow(new NoClassDefFoundError("Test error")).when(mockRunnable).run();
    
    // Act
    boolean result = Compatibility.tryWithFallback(mockRunnableFactory);
    
    // Assert
    assertFalse("Should return false when NoClassDefFoundError is thrown", result);
    verify(mockRunnableFactory).create();
    verify(mockRunnable).run();
  }

  @Test
  public void testTryWithFallback_WithClassNotFoundException_ReturnsFalse() {
    // Arrange
    when(mockRunnableFactory.create()).thenReturn(mockRunnable);
    doThrow(new ClassNotFoundException("Test error")).when(mockRunnable).run();
    
    // Act
    boolean result = Compatibility.tryWithFallback(mockRunnableFactory);
    
    // Assert
    assertFalse("Should return false when ClassNotFoundException is thrown", result);
    verify(mockRunnableFactory).create();
    verify(mockRunnable).run();
  }

  @Test
  public void testTryWithFallback_WithNoSuchFieldException_ReturnsFalse() {
    // Arrange
    when(mockRunnableFactory.create()).thenReturn(mockRunnable);
    doThrow(new NoSuchFieldException("Test error")).when(mockRunnable).run();
    
    // Act
    boolean result = Compatibility.tryWithFallback(mockRunnableFactory);
    
    // Assert
    assertFalse("Should return false when NoSuchFieldException is thrown", result);
    verify(mockRunnableFactory).create();
    verify(mockRunnable).run();
  }

  @Test
  public void testTryWithFallback_WithNoSuchMethodException_ReturnsFalse() {
    // Arrange
    when(mockRunnableFactory.create()).thenReturn(mockRunnable);
    doThrow(new NoSuchMethodException("Test error")).when(mockRunnable).run();
    
    // Act
    boolean result = Compatibility.tryWithFallback(mockRunnableFactory);
    
    // Assert
    assertFalse("Should return false when NoSuchMethodException is thrown", result);
    verify(mockRunnableFactory).create();
    verify(mockRunnable).run();
  }

  @Test
  public void testTryWithFallback_WithNullPointerException_ReturnsFalse() {
    // Arrange
    when(mockRunnableFactory.create()).thenReturn(mockRunnable);
    doThrow(new NullPointerException("Test error")).when(mockRunnable).run();
    
    // Act
    boolean result = Compatibility.tryWithFallback(mockRunnableFactory);
    
    // Assert
    assertFalse("Should return false when NullPointerException is thrown", result);
    verify(mockRunnableFactory).create();
    verify(mockRunnable).run();
  }

  @Test
  public void testTryWithFallback_WithVerifyError_ReturnsFalse() {
    // Arrange
    when(mockRunnableFactory.create()).thenReturn(mockRunnable);
    doThrow(new VerifyError("Test error")).when(mockRunnable).run();
    
    // Act
    boolean result = Compatibility.tryWithFallback(mockRunnableFactory);
    
    // Assert
    assertFalse("Should return false when VerifyError is thrown", result);
    verify(mockRunnableFactory).create();
    verify(mockRunnable).run();
  }

  @Test
  public void testTryWithFallback_WithRuntimeException_ThrowsException() {
    // Arrange - test that other exceptions are not caught
    when(mockRunnableFactory.create()).thenReturn(mockRunnable);
    RuntimeException testException = new RuntimeException("Test error");
    doThrow(testException).when(mockRunnable).run();
    
    // Act & Assert
    try {
      Compatibility.tryWithFallback(mockRunnableFactory);
      fail("Should throw RuntimeException when not a compatibility exception");
    } catch (RuntimeException e) {
      assertEquals("Should propagate the same exception", testException, e);
    }
    
    verify(mockRunnableFactory).create();
    verify(mockRunnable).run();
  }

  @Test
  public void testTryWithFallback_WithMultipleCalls_EachCallIndependent() {
    // Arrange
    RunnableFactory successFactory = mock(RunnableFactory.class);
    Runnable successRunnable = mock(Runnable.class);
    when(successFactory.create()).thenReturn(successRunnable);
    
    RunnableFactory failFactory = mock(RunnableFactory.class);
    Runnable failRunnable = mock(Runnable.class);
    when(failFactory.create()).thenReturn(failRunnable);
    doThrow(new NoSuchMethodError()).when(failRunnable).run();
    
    // Act
    boolean result1 = Compatibility.tryWithFallback(successFactory);
    boolean result2 = Compatibility.tryWithFallback(failFactory);
    boolean result3 = Compatibility.tryWithFallback(successFactory);
    
    // Assert
    assertTrue("First call should succeed", result1);
    assertFalse("Second call should fail", result2);
    assertTrue("Third call should succeed", result3);
  }

  @Test
  public void testTryWithFallback_ExceptionInFactoryCreate_PropagatesException() {
    // Arrange
    RuntimeException factoryException = new RuntimeException("Factory error");
    when(mockRunnableFactory.create()).thenThrow(factoryException);
    
    // Act & Assert
    try {
      Compatibility.tryWithFallback(mockRunnableFactory);
      fail("Should propagate exception from factory create");
    } catch (RuntimeException e) {
      assertEquals("Should propagate the same exception", factoryException, e);
    }
    
    verify(mockRunnableFactory).create();
  }

  @Test
  public void testTryWithFallback_NullRunnableFromFactory_ThrowsException() {
    // Arrange
    when(mockRunnableFactory.create()).thenReturn(null);
    
    // Act & Assert
    try {
      Compatibility.tryWithFallback(mockRunnableFactory);
      fail("Should throw exception when factory returns null");
    } catch (NullPointerException e) {
      // Expected
    }
    
    verify(mockRunnableFactory).create();
  }
}