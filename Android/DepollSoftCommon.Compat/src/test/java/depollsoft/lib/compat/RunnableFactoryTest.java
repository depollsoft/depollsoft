package depollsoft.lib.compat;

import org.junit.Test;
import static org.junit.Assert.*;

public class RunnableFactoryTest {

  @Test
  public void testRunnableFactory_BasicImplementation() {
    // Arrange
    final boolean[] executed = {false};
    RunnableFactory factory = new RunnableFactory() {
      @Override
      public Runnable create() {
        return new Runnable() {
          @Override
          public void run() {
            executed[0] = true;
          }
        };
      }
    };
    
    // Act
    Runnable runnable = factory.create();
    runnable.run();
    
    // Assert
    assertTrue("Runnable should have been executed", executed[0]);
  }

  @Test
  public void testRunnableFactory_WithLambda() {
    // Arrange
    final String[] result = {null};
    RunnableFactory factory = () -> () -> result[0] = "executed";
    
    // Act
    Runnable runnable = factory.create();
    runnable.run();
    
    // Assert
    assertEquals("Runnable should have set result", "executed", result[0]);
  }

  @Test
  public void testRunnableFactory_MultipleCreations() {
    // Arrange
    final int[] counter = {0};
    RunnableFactory factory = new RunnableFactory() {
      @Override
      public Runnable create() {
        return new Runnable() {
          @Override
          public void run() {
            counter[0]++;
          }
        };
      }
    };
    
    // Act
    Runnable runnable1 = factory.create();
    Runnable runnable2 = factory.create();
    runnable1.run();
    runnable2.run();
    
    // Assert
    assertEquals("Both runnables should have incremented counter", 2, counter[0]);
  }

  @Test
  public void testRunnableFactory_ReturnsNull() {
    // Arrange
    RunnableFactory factory = new RunnableFactory() {
      @Override
      public Runnable create() {
        return null;
      }
    };
    
    // Act
    Runnable runnable = factory.create();
    
    // Assert
    assertNull("Factory should be able to return null", runnable);
  }

  @Test
  public void testRunnableFactory_WithCompatibility() {
    // Arrange
    final boolean[] executed = {false};
    RunnableFactory successFactory = () -> () -> executed[0] = true;
    
    RunnableFactory failFactory = () -> () -> {
      throw new NoSuchMethodError("Test error");
    };
    
    // Act
    boolean result1 = Compatibility.tryWithFallback(successFactory);
    boolean result2 = Compatibility.tryWithFallback(failFactory);
    
    // Assert
    assertTrue("Success factory should return true", result1);
    assertTrue("Runnable should have executed successfully", executed[0]);
    assertFalse("Fail factory should return false", result2);
  }

  @Test
  public void testRunnableFactory_CreatesNewInstanceEachTime() {
    // Arrange
    RunnableFactory factory = () -> new Runnable() {
      @Override
      public void run() {
        // Do nothing
      }
    };
    
    // Act
    Runnable runnable1 = factory.create();
    Runnable runnable2 = factory.create();
    
    // Assert
    assertNotNull("First runnable should not be null", runnable1);
    assertNotNull("Second runnable should not be null", runnable2);
    assertNotSame("Factory should create different instances", runnable1, runnable2);
  }

  @Test
  public void testRunnableFactory_ExceptionInCreate() {
    // Arrange
    RuntimeException testException = new RuntimeException("Test error");
    RunnableFactory factory = new RunnableFactory() {
      @Override
      public Runnable create() {
        throw testException;
      }
    };
    
    // Act & Assert
    try {
      factory.create();
      fail("Should throw exception from create method");
    } catch (RuntimeException e) {
      assertEquals("Should propagate the same exception", testException, e);
    }
  }
}