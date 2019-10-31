package com.kotlin.trivialdrive.storms;

import android.app.Activity;
import android.app.Application;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.kotlin.trivialdrive.billingrepo.localdb.AugmentedSkuDetails;
import com.storms.proto.SvcCode;
import com.storms.sdk.protocol.IPurchases;
import com.storms.sdk.protocol.ISkuDetails;
import com.storms.sdk.protocol.LoggedInListener;
import com.storms.sdk.protocol.PurchasesUpdatedListener;
import com.storms.sdk.protocol.SkuDetailsParams;
import com.storms.sdk.protocol.SkuDetailsResponseListener;
import com.storms.sdk.protocol.StormsSDK;
import com.storms.sdk.protocol.StormsStateListener;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StormsRepo implements PurchasesUpdatedListener {
    private static String LOG_TAG = "StormsRepository";
    private StormsSDK stormsClient;
    private Application application;

    static {
        Log.d("Sys library loading", "This should call the onLoad function");
        System.loadLibrary("native");
    }

    private static volatile StormsRepo INSTANCE = null;
    private static final Object lock = new Object();
    public static StormsRepo getInstance(Application app) {
        synchronized (lock) {
            if(INSTANCE != null) {
                return  INSTANCE;
            }
            INSTANCE = new StormsRepo(app);
            return INSTANCE;
        }
    }

    private StormsRepo(Application app) {
        this.application = app;
    }

    private MutableLiveData<List<AugmentedSkuDetails>> liveSkuList = new MutableLiveData<>();

    public LiveData<List<AugmentedSkuDetails>> getLiveSkuList() {
        return liveSkuList;
    }

    @Override
    public void onPurchasesUpdated(SvcCode.StatusCode statusCode, List<IPurchases.Purchases> list) {
        Log.d(LOG_TAG, "onPurchasesUpdated: " + statusCode);
        Log.d(LOG_TAG, "onPurchasesUpdated: " + list);
    }

    public void startDataSourceConnections() {
        Log.d(LOG_TAG, "startDataSourceConnections");
        instantiateAndConnectToPlayBillingService();
    }

    public void endDataSourceConnections() {
        stormsClient.endConnection();
        // normally you don't worry about closing a DB connection unless you have more than
        // one DB open. so no need to call 'localCacheBillingClient.close()'
        Log.d(LOG_TAG, "endDataSourceConnections");


    }

    private void instantiateAndConnectToPlayBillingService() {
        stormsClient = StormsSDK.newBuilder(application.getApplicationContext())
                .setListener(this).build();
        connectToPlayBillingService();
}

    private boolean connectToPlayBillingService() {
        Log.d(LOG_TAG, "connectToPlayBillingService " + stormsClient.isReady());
        if (!stormsClient.isReady()) {
            stormsClient.startConnection(new StormsStateListener() {
                @Override
                public void onServiceConnected() {
                    Log.d(LOG_TAG, "onServiceConnected: ");
                    querySkuDetailsAsync(Arrays.asList(
                        "com.nianticlabs.pokemongo.sku01",
                        "com.nianticlabs.pokemongo.sku03",
                        "com.nianticlabs.pokemongo.sku999",
                        "com.nianticlabs.pokemongo.sku100"));
                    isLoggedIn();
                }

                @Override
                public void onServiceDisconnected() {
                    Log.d(LOG_TAG, "onServiceDisconnected: ");
                }

                @Override
                public void onServiceUpdateNeeded() {
                    Log.d(LOG_TAG, "onServiceUpdateNeeded: ");
                }

                @Override
                public void onAPIVersionNotSupported() {
                    Log.d(LOG_TAG, "onAPIVersionNotSupported");
                }
            });
            return true;
        }
        return false;
    }

    public void launchBillingFlow(Activity activity, AugmentedSkuDetails augmentedSkuDetails) {
        if(stormsClient.isReady()) {
            stormsClient.launchPurchaseFlow(activity, augmentedSkuDetails.getSku());
        }
    }

    public void isLoggedIn() {
        if(stormsClient.isReady()) {
            stormsClient.isLoggedIn(new LoggedInListener() {
                @Override
                public void onResult(boolean b) {
                    Log.d(LOG_TAG, "onResult: " + b);
                }
            });
        }
    }

    private void querySkuDetailsAsync(List<String> skuList) {
        if(stormsClient.isReady()) {
            SkuDetailsParams params = SkuDetailsParams.newBuilder()
                    .setSkuList(skuList)
                    .build();
            stormsClient.querySkuDetails(params, new SkuDetailsResponseListener() {
                @Override
                public void onSkuDetailsResponse(SvcCode.StatusCode statusCode, List<ISkuDetails.SkuDetails> list) {
                    Log.d(LOG_TAG, "onSkuDetailsResponse: " + statusCode);
                    if(statusCode == SvcCode.StatusCode.SUCCESS) {
                        List<AugmentedSkuDetails> item = new ArrayList<>();
                        for (ISkuDetails.SkuDetails it : list) {
                            item.add(new AugmentedSkuDetails(true, it.getSku(), it.getType(), it.getCurrencyCode() + " " + String.format("%.02f", it.getPrice()), it.getTitle() , String.format("%s\n%s", it.getDescription(), it.getSku()), ""));
                        }
                        liveSkuList.postValue(item);
                    }
                }
            });
        }
    }
}
