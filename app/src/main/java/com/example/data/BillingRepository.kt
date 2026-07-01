package com.example.data

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class BillingResult {
    data object Success : BillingResult()
    data class Error(val message: String) : BillingResult()
    data object Pending : BillingResult()
}

sealed class SubscriptionStatus {
    data object Free : SubscriptionStatus()
    data class Monthly(val expiryTimestamp: Long) : SubscriptionStatus()
    data object Lifetime : SubscriptionStatus()
}

class BillingRepository(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        const val MONTHLY_SUBSCRIPTION_ID = "monthly_subscription"
        const val LIFETIME_SUBSCRIPTION_ID = "lifetime_subscription"
    }

    private var billingClient: BillingClient? = null
    
    private val _subscriptionStatus = MutableStateFlow<SubscriptionStatus>(SubscriptionStatus.Free)
    val subscriptionStatus: StateFlow<SubscriptionStatus> = _subscriptionStatus.asStateFlow()
    
    private val _billingConnectionState = MutableStateFlow(false)
    val billingConnectionState: StateFlow<Boolean> = _billingConnectionState.asStateFlow()

    init {
        initializeBillingClient()
    }

    private fun initializeBillingClient() {
        billingClient = BillingClient.newBuilder(context)
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    handlePurchases(purchases)
                }
            }
            .enablePendingPurchases()
            .build()

        connectToBillingService()
    }

    private fun connectToBillingService() {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: com.android.billingclient.api.BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _billingConnectionState.value = true
                    queryExistingPurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                _billingConnectionState.value = false
            }
        })
    }

    private fun queryExistingPurchases() {
        scope.launch {
            withContext(Dispatchers.IO) {
                val client = billingClient ?: return@withContext
                
                val subscriptionParams = QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
                
                val inAppParams = QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()

                val subscriptionResult = client.queryPurchasesAsync(subscriptionParams)
                val inAppResult = client.queryPurchasesAsync(inAppParams)

                val allPurchases = mutableListOf<Purchase>()
                allPurchases.addAll(subscriptionResult.purchasesList)
                allPurchases.addAll(inAppResult.purchasesList)

                handlePurchases(allPurchases)
            }
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        for (purchase in purchases) {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                if (!purchase.isAcknowledged) {
                    acknowledgePurchase(purchase)
                }
                
                when {
                    purchase.products.contains(LIFETIME_SUBSCRIPTION_ID) -> {
                        _subscriptionStatus.value = SubscriptionStatus.Lifetime
                    }
                    purchase.products.contains(MONTHLY_SUBSCRIPTION_ID) -> {
                        val expiryTime = purchase.purchaseTime + (30L * 24 * 60 * 60 * 1000)
                        _subscriptionStatus.value = SubscriptionStatus.Monthly(expiryTime)
                    }
                }
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        scope.launch {
            withContext(Dispatchers.IO) {
                val client = billingClient ?: return@withContext
                val params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                
                client.acknowledgePurchase(params) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        // Purchase acknowledged successfully
                    }
                }
            }
        }
    }

    suspend fun purchaseSubscription(
        activity: Activity,
        isMonthly: Boolean,
        onResult: (BillingResult) -> Unit
    ) {
        withContext(Dispatchers.Main) {
            val client = billingClient
            if (client == null || !_billingConnectionState.value) {
                onResult(BillingResult.Error("Billing service not connected"))
                return@withContext
            }

            val productId = if (isMonthly) MONTHLY_SUBSCRIPTION_ID else LIFETIME_SUBSCRIPTION_ID
            val productType = if (isMonthly) BillingClient.ProductType.SUBS else BillingClient.ProductType.INAPP

            val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(productId)
                            .setProductType(productType)
                            .build()
                    )
                )
                .build()

            client.queryProductDetailsAsync(queryProductDetailsParams) { billingResult, productDetailsList ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                    val productDetails = productDetailsList[0]
                    
                    val productDetailsParamsList = if (isMonthly) {
                        val offerToken = productDetails.subscriptionOfferDetails?.get(0)?.offerToken
                        if (offerToken == null) {
                            onResult(BillingResult.Error("Subscription offer not found"))
                            return@queryProductDetailsAsync
                        }
                        
                        listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .setOfferToken(offerToken)
                                .build()
                        )
                    } else {
                        listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .build()
                        )
                    }

                    val billingFlowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(productDetailsParamsList)
                        .build()

                    val launchResult = client.launchBillingFlow(activity, billingFlowParams)
                    
                    when (launchResult.responseCode) {
                        BillingClient.BillingResponseCode.OK -> onResult(BillingResult.Pending)
                        else -> onResult(BillingResult.Error("Failed to launch billing flow"))
                    }
                } else {
                    onResult(BillingResult.Error("Product not found"))
                }
            }
        }
    }

    fun hasActiveSubscription(): Boolean {
        return when (val status = _subscriptionStatus.value) {
            is SubscriptionStatus.Free -> false
            is SubscriptionStatus.Lifetime -> true
            is SubscriptionStatus.Monthly -> status.expiryTimestamp > System.currentTimeMillis()
        }
    }

    fun destroy() {
        billingClient?.endConnection()
        billingClient = null
    }
}
