package com.kotlin.trivialdrive.storms

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.kotlin.trivialdrive.billingrepo.localdb.AugmentedSkuDetails
import com.storms.proto.SvcCode
import com.storms.sdk.protocol.*
import com.storms.sdk.utils.Utils

class StormsRepository private constructor(private val application: Application) : PurchasesUpdatedListener {
    private lateinit var stormsClient: StormsSDK

    companion object {
        private const val LOG_TAG = "StormsRepository"

        @Volatile
        private var INSTANCE: StormsRepository? = null

        fun getInstance(application: Application): StormsRepository =
                INSTANCE ?: synchronized(this) {
                    INSTANCE
                            ?: StormsRepository(application)
                                    .also { INSTANCE = it }
                }
    }

    private val liveSkuList = MutableLiveData<List<AugmentedSkuDetails>>()
    val inappSkuDetailsListLiveData: LiveData<List<AugmentedSkuDetails>> by lazy {
        liveSkuList
    }

    override fun onPurchasesUpdated(code: SvcCode.StatusCode?, purchases: MutableList<IPurchases.Purchases>?) {
        println("$code $purchases")
    }

    fun startDataSourceConnections() {
        Log.d(LOG_TAG, "startDataSourceConnections")
        instantiateAndConnectToPlayBillingService()
    }

    fun endDataSourceConnections() {
        stormsClient.endConnection()
        // normally you don't worry about closing a DB connection unless you have more than
        // one DB open. so no need to call 'localCacheBillingClient.close()'
        Log.d(LOG_TAG, "endDataSourceConnections")
    }

    private fun instantiateAndConnectToPlayBillingService() {
        stormsClient = StormsSDK.newBuilder(application.applicationContext)
                .setListener(this).build()
        connectToPlayBillingService()
    }

    private fun connectToPlayBillingService(): Boolean {
        Log.d(LOG_TAG, "connectToPlayBillingService " + stormsClient.isReady)
        if (!stormsClient.isReady) {
            stormsClient.startConnection(object : StormsStateListener {
                override fun onServiceConnected() {
                    println("onServiceConnected")
                    querySkuDetailsAsync(listOf("abc", "bcd"))
                    isLoggedIn()
                }

                override fun onServiceDisconnected() {
                    println("onServiceDisconnected")
                }

                override fun onServiceUpdateNeeded() {
                    println("onServiceUpdateNeeded")

                }

                override fun onAPIVersionNotSupported() {
                    println("onAPIVersionNotSupported")
                }

            })
            return true
        }
        return false
    }

    fun launchBillingFlow(activity: Activity, augmentedSkuDetails: AugmentedSkuDetails) {
        if(stormsClient.isReady) {
            stormsClient.launchPurchaseFlow(activity, augmentedSkuDetails.sku)
        }
    }

    fun isLoggedIn() {
        if(stormsClient.isReady) {
            stormsClient.isLoggedIn { isLoggedIn ->
                println(isLoggedIn)
            }
        }
    }

    private fun querySkuDetailsAsync(skuList : List<String>) {
        if(stormsClient.isReady) {
            val params = SkuDetailsParams.newBuilder()
                    .setSkuList(skuList)
                    .build()
            stormsClient.querySkuDetails(params) { code, details ->
                    println(code)
                    val l = ArrayList<AugmentedSkuDetails>()
                    details?.forEach {
                        l.add(AugmentedSkuDetails(true, "aaa", it.type ?: "", it.currencyCode + " " + String.format("%.02f", it.price), it.title ?: "", it.description ?: "", ""))
                        l.add(AugmentedSkuDetails(true, "aaa", it.type ?: "", it.currencyCode + " " + String.format("%.02f", it.price), it.title ?: "", it.description ?: "", ""))
                        l.add(AugmentedSkuDetails(true, "aaa", it.type ?: "", it.currencyCode + " " + String.format("%.02f", it.price), it.title ?: "", it.description ?: "", ""))
                        l.add(AugmentedSkuDetails(true, "aaa", it.type ?: "", it.currencyCode + " " + String.format("%.02f", it.price), it.title ?: "", it.description ?: "", ""))
                    }
                    liveSkuList.postValue(l)
                }
        }
    }
}