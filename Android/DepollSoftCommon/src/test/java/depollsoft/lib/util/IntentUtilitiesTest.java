package depollsoft.lib.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class IntentUtilitiesTest {

  @Mock
  private Context mockContext;
  
  @Mock
  private PackageManager mockPackageManager;
  
  @Mock
  private Intent mockIntent;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(mockContext.getPackageManager()).thenReturn(mockPackageManager);
  }

  @Test
  public void testIsIntentAvailable_WithAvailableIntent_ReturnsTrue() {
    // Arrange
    ResolveInfo resolveInfo = new ResolveInfo();
    List<ResolveInfo> resolveInfoList = Arrays.asList(resolveInfo);
    when(mockPackageManager.queryIntentActivities(eq(mockIntent), eq(PackageManager.MATCH_DEFAULT_ONLY)))
        .thenReturn(resolveInfoList);

    // Act
    boolean result = IntentUtilities.isIntentAvailable(mockContext, mockIntent);

    // Assert
    assertTrue("Intent should be available when there are resolve results", result);
    verify(mockPackageManager).queryIntentActivities(mockIntent, PackageManager.MATCH_DEFAULT_ONLY);
  }

  @Test
  public void testIsIntentAvailable_WithNoAvailableIntent_ReturnsFalse() {
    // Arrange
    List<ResolveInfo> emptyList = new ArrayList<>();
    when(mockPackageManager.queryIntentActivities(eq(mockIntent), eq(PackageManager.MATCH_DEFAULT_ONLY)))
        .thenReturn(emptyList);

    // Act
    boolean result = IntentUtilities.isIntentAvailable(mockContext, mockIntent);

    // Assert
    assertFalse("Intent should not be available when there are no resolve results", result);
    verify(mockPackageManager).queryIntentActivities(mockIntent, PackageManager.MATCH_DEFAULT_ONLY);
  }

  @Test
  public void testIsIntentAvailable_WithMultipleResolveInfos_ReturnsTrue() {
    // Arrange
    ResolveInfo resolveInfo1 = new ResolveInfo();
    ResolveInfo resolveInfo2 = new ResolveInfo();
    List<ResolveInfo> resolveInfoList = Arrays.asList(resolveInfo1, resolveInfo2);
    when(mockPackageManager.queryIntentActivities(eq(mockIntent), eq(PackageManager.MATCH_DEFAULT_ONLY)))
        .thenReturn(resolveInfoList);

    // Act
    boolean result = IntentUtilities.isIntentAvailable(mockContext, mockIntent);

    // Assert
    assertTrue("Intent should be available when there are multiple resolve results", result);
    verify(mockPackageManager).queryIntentActivities(mockIntent, PackageManager.MATCH_DEFAULT_ONLY);
  }

  @Test(expected = NullPointerException.class)
  public void testIsIntentAvailable_WithNullContext_ThrowsException() {
    // Act
    IntentUtilities.isIntentAvailable(null, mockIntent);
  }

  @Test(expected = NullPointerException.class)
  public void testIsIntentAvailable_WithNullIntent_ThrowsException() {
    // Act
    IntentUtilities.isIntentAvailable(mockContext, null);
  }

  @Test
  public void testIsIntentAvailable_VerifyCorrectParametersPassedToPackageManager() {
    // Arrange
    Intent specificIntent = new Intent(Intent.ACTION_VIEW);
    List<ResolveInfo> resolveInfoList = new ArrayList<>();
    when(mockPackageManager.queryIntentActivities(eq(specificIntent), eq(PackageManager.MATCH_DEFAULT_ONLY)))
        .thenReturn(resolveInfoList);

    // Act
    IntentUtilities.isIntentAvailable(mockContext, specificIntent);

    // Assert
    verify(mockPackageManager).queryIntentActivities(specificIntent, PackageManager.MATCH_DEFAULT_ONLY);
    verify(mockContext).getPackageManager();
  }
}