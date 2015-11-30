package depollsoft.pitchperfect;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.widget.Toast;

import com.android.vending.billing.IInAppBillingService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;

public class PurchaseService {

  private static final String INAPP_PURCHASE_ITEM_LIST = "INAPP_PURCHASE_ITEM_LIST";
  private static final String INAPP_CONTINUATION_TOKEN = "INAPP_CONTINUATION_TOKEN";
  private static final String BUY_INTENT = "BUY_INTENT";
  public static final String RESPONSE_CODE = "RESPONSE_CODE";
  private static final String REMOVE_ADS_SKU = "depollsoft.pitchperfect.removeads";

  /**
   * success
   */
  private static final int RESULT_OK = 0;
  /**
   * user pressed back or canceled a dialog
   */
  private static final int RESULT_USER_CANCELED = 1;
  /**
   * this billing API version is not supported for the type requested
   */
  private static final int RESULT_BILLING_UNAVAILABLE = 3;
  /**
   * requested SKU is not available for purchase
   */
  private static final int RESULT_ITEM_UNAVAILABLE = 4;
  /**
   * invalid arguments provided to the API
   */
  private static final int RESULT_DEVELOPER_ERROR = 5;
  /**
   * Fatal error during the API action
   */
  private static final int RESULT_ERROR = 6;
  /**
   * Failure to purchase since item is already owned
   */
  private static final int RESULT_ITEM_ALREADY_OWNED = 7;
  /**
   * Failure to consume since item is not owned
   */
  private static final int RESULT_ITEM_NOT_OWNED = 8;

  private static class BillingServiceConnection implements ServiceConnection {
    private IInAppBillingService billingService;

    @Override
    public void onServiceDisconnected(ComponentName name) {
      billingService = null;
    }

    @Override
    public void onServiceConnected(ComponentName name,
                                   IBinder service) {
      billingService = IInAppBillingService.Stub.asInterface(service);
    }

    public IInAppBillingService getBillingService() {
      return billingService;
    }
  }

  private static WeakHashMap<ContextWrapper, BillingServiceConnection> connections = new WeakHashMap<>();

  public static boolean bind(ContextWrapper context, final Runnable callback) {
    Intent billingServiceIntent =
            new Intent("com.android.vending.billing.InAppBillingService.BIND");
    billingServiceIntent.setPackage("com.android.vending");
    BillingServiceConnection connection = new BillingServiceConnection() {
      @Override
      public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);
        if (callback != null) {
          callback.run();
        }
      }
    };
    boolean result = context.bindService(billingServiceIntent, connection, Context.BIND_AUTO_CREATE);
    connections.put(context, connection);
    return result;
  }

  public static void unbind(ContextWrapper context) {
    BillingServiceConnection connection = connections.get(context);
    if (connection != null) {
      context.unbindService(connection);
    }
  }

  private static IInAppBillingService getBillingService(ContextWrapper context) {
    BillingServiceConnection connection = connections.get(context);
    if (connection == null) {
      return null;
    }
    return connection.getBillingService();
  }

  public static boolean isSubscriptionBillingAvailable(ContextWrapper context) {
    try {
      IInAppBillingService billingService = getBillingService(context);
      if (billingService == null) {
        return false;
      }
      return billingService.isBillingSupported(3, context.getPackageName(), "subs") == RESULT_OK;
    } catch (RemoteException ex) {
      return false;
    }
  }

  private static List<String> getSubscriptions(ContextWrapper context) {
    IInAppBillingService billingService = getBillingService(context);
    if (billingService == null) {
      return Collections.emptyList();
    }
    List<String> skus = new ArrayList<>();
    String continuationToken = null;
    try {
      do {
        Bundle subs = billingService.getPurchases(3, context.getPackageName(), "subs", continuationToken);
        List<String> subsSkus = subs.getStringArrayList(INAPP_PURCHASE_ITEM_LIST);
        if (subsSkus != null) {
          skus.addAll(subsSkus);
        }
        continuationToken = subs.getString(INAPP_CONTINUATION_TOKEN);
      } while (continuationToken != null);
    } catch (RemoteException ex) {
    }
    return skus;
  }

  public static boolean areAdsRemoved(ContextWrapper context) {
    return getSubscriptions(context).contains(REMOVE_ADS_SKU);
  }

  public static void beginRemoveAds(Activity activity, int requestCode) {
    IInAppBillingService billingService = getBillingService(activity);
    if (billingService == null) {
      throw new RuntimeException("Billing is not enabled");
    }
    try {
      Bundle bundle = billingService.getBuyIntent(3, activity.getPackageName(), REMOVE_ADS_SKU, "subs", "");
      PendingIntent pendingIntent = bundle.getParcelable(BUY_INTENT);
      if (pendingIntent != null && bundle.getInt(RESPONSE_CODE) == RESULT_OK) {
        // Start purchase flow (this brings up the Google Play UI).
        // Result will be delivered through onActivityResult().
        activity.startIntentSenderForResult(pendingIntent.getIntentSender(),
                requestCode, new Intent(), 0, 0, 0);
      } else {
        throw new RuntimeException("Unable to build buy intent");
      }
    } catch (RemoteException ex) {
      throw new RuntimeException("Failed to create buy intent", ex);
    } catch (IntentSender.SendIntentException ex) {
      throw new RuntimeException("Failed to send intent", ex);
    }
  }
}
