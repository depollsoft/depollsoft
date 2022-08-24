package depollsoft.pitchperfect

import android.content.ContextWrapper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.*
import com.bindroid.trackable.trackable
import depollsoft.lib.activity.RichApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

object PurchaseService {
    const val REMOVE_ADS_SKU = "depollsoft.pitchperfect.removeads"
    private var productDetails: ProductDetails? = null

    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            CoroutineScope(Dispatchers.IO + Job()).launch {
                var newAdsValue = false
                purchases?.forEach {
                    if (!it.isAcknowledged) {
                        billingClient.acknowledgePurchase(
                            AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(it.purchaseToken)
                                .build()
                        )
                    }
                    if (it.products.contains(REMOVE_ADS_SKU)) {
                        newAdsValue = true
                    }
                }
                areAdsRemoved = newAdsValue
            }
        }

    private lateinit var billingClient: BillingClient

    fun bind(context: ContextWrapper, callback: Runnable?): Boolean {
        billingClient = BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                    CoroutineScope(Dispatchers.IO + Job()).launch {
                        val skus = listOf(
                            QueryProductDetailsParams.Product.newBuilder()
                                .setProductId(REMOVE_ADS_SKU)
                                .setProductType(BillingClient.ProductType.SUBS)
                                .build()
                        )
                        val productDetailsResults = billingClient.queryProductDetails(
                            QueryProductDetailsParams.newBuilder().setProductList(skus).build()
                        )
                        productDetails =
                            productDetailsResults.productDetailsList?.elementAtOrNull(0)
                        val purchases = billingClient.queryPurchasesAsync(
                            QueryPurchasesParams.newBuilder()
                                .setProductType(BillingClient.ProductType.SUBS).build()
                        )
                        areAdsRemoved =
                            purchases.purchasesList.any { it.products.contains(REMOVE_ADS_SKU) }
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                billingClient.startConnection(this)
            }
        })
        return true
    }

    var areAdsRemoved: Boolean by trackable(false) {
        SettingsModel.areAdsRemoved = it
    }

    fun beginRemoveAds(activity: AppCompatActivity, requestCode: Int) {
        if (productDetails == null) {
            Toast.makeText(
                RichApplication.getAppContext(),
                "Subscriptions not available on this device.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val result = billingClient.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setOfferToken(productDetails!!.subscriptionOfferDetails!!.map { it.offerToken }
                                .first())
                            .setProductDetails(productDetails!!).build()
                    )
                )
                .build()
        )
        result.responseCode
    }
}