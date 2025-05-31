package depollsoft.lib.ui;

import android.app.Activity;
import android.view.View;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class ThreadSwitchContextTest {

  @Mock
  private Activity mockActivity;
  
  @Mock
  private View mockView;
  
  @Mock
  private Runnable mockRunnable;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testConstructorWithActivity() {
    // Act
    ThreadSwitchContext context = new ThreadSwitchContext(mockActivity);
    
    // Assert - no exception should be thrown
    // Constructor should store the activity reference
  }

  @Test
  public void testConstructorWithView() {
    // Act
    ThreadSwitchContext context = new ThreadSwitchContext(mockView);
    
    // Assert - no exception should be thrown
    // Constructor should store the view reference
  }

  @Test
  public void testPost_WithActivity() {
    // Arrange
    ThreadSwitchContext context = new ThreadSwitchContext(mockActivity);
    
    // Act
    context.post(mockRunnable);
    
    // Assert
    verify(mockActivity).runOnUiThread(mockRunnable);
    verify(mockView, never()).post(any(Runnable.class));
  }

  @Test
  public void testPost_WithView() {
    // Arrange
    ThreadSwitchContext context = new ThreadSwitchContext(mockView);
    
    // Act
    context.post(mockRunnable);
    
    // Assert
    verify(mockView).post(mockRunnable);
    verify(mockActivity, never()).runOnUiThread(any(Runnable.class));
  }

  @Test
  public void testPost_WithNullRunnable_Activity() {
    // Arrange
    ThreadSwitchContext context = new ThreadSwitchContext(mockActivity);
    
    // Act
    context.post(null);
    
    // Assert
    verify(mockActivity).runOnUiThread(null);
  }

  @Test
  public void testPost_WithNullRunnable_View() {
    // Arrange
    ThreadSwitchContext context = new ThreadSwitchContext(mockView);
    
    // Act
    context.post(null);
    
    // Assert
    verify(mockView).post(null);
  }

  @Test
  public void testPost_MultipleCallsWithActivity() {
    // Arrange
    ThreadSwitchContext context = new ThreadSwitchContext(mockActivity);
    Runnable runnable1 = mock(Runnable.class);
    Runnable runnable2 = mock(Runnable.class);
    
    // Act
    context.post(runnable1);
    context.post(runnable2);
    
    // Assert
    verify(mockActivity).runOnUiThread(runnable1);
    verify(mockActivity).runOnUiThread(runnable2);
  }

  @Test
  public void testPost_MultipleCallsWithView() {
    // Arrange
    ThreadSwitchContext context = new ThreadSwitchContext(mockView);
    Runnable runnable1 = mock(Runnable.class);
    Runnable runnable2 = mock(Runnable.class);
    
    // Act
    context.post(runnable1);
    context.post(runnable2);
    
    // Assert
    verify(mockView).post(runnable1);
    verify(mockView).post(runnable2);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorWithNullActivity() {
    // Act
    ThreadSwitchContext context = new ThreadSwitchContext((Activity) null);
    context.post(mockRunnable);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorWithNullView() {
    // Act
    ThreadSwitchContext context = new ThreadSwitchContext((View) null);
    context.post(mockRunnable);
  }
}