package com.taskearn.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.rewarded.ServerSideVerificationOptions;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class TaskEarnBridge {

    private final Activity activity;
    private final Context context;

    private WebView webView;

    private RewardedAd rewardedAd;

    private boolean isLoadingAd = false;
    private boolean rewardEarned = false;
    private boolean adCurrentlyShowing = false;
    private boolean pendingShowAfterLoad = false;

    private String activeSessionId = null;
    private String activeSessionNonce = null;
    private JSONObject lastPaidEvent = null;

    /*
     * ==========================================================
     * REAL TASKEARN PRODUCTION REWARDED AD UNIT
     * ==========================================================
     *
     * This is the actual AdMob-created ad unit for TaskEarn
     * ("TaskEarn Ad Revenue Share Rewarded"), NOT a Google
     * test ID.
     *
     * AdMob App ID (goes in AndroidManifest.xml, not here):
     *   ca-app-pub-6053164512172778~5887236512
     *
     * This Rewarded Ad Unit ID (used below):
     *   ca-app-pub-6053164512172778/9063055035
     *
     * DO NOT replace this with the Google sample/test ID
     * (ca-app-pub-6053164512172778/9063055035) in a
     * production build — that was the previous bug.
     */
    private static final String REWARDED_AD_UNIT_ID =
        "ca-app-pub-6053164512172778/9063055035";



    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public TaskEarnBridge(Activity activity) {

        this.activity = activity;
        this.context = activity.getApplicationContext();

        /*
         * Initialize Google Mobile Ads SDK.
         */
        activity.runOnUiThread(() -> {

            try {

                MobileAds.initialize(
                        activity,
                        initializationStatus -> {

                            /*
                             * SDK initialized.
                             *
                             * Immediately start loading the
                             * first rewarded ad.
                             */
                            loadRewardedAdInternal();
                        }
                );

            } catch (Exception e) {

                notifyJavascript(
                        "TaskEarnNativeAdError",
                        JSONObject.quote(
                                "Ad SDK initialization failed."
                        )
                );
            }
        });
    }


    // =========================================================
    // WEBVIEW CONNECTION
    // =========================================================

    public void setWebView(WebView webView) {

        this.webView = webView;

        /*
         * If SDK is already initialized / ad loading has
         * started, this simply makes callbacks available
         * to the web page.
         */
    }


    // =========================================================
    // CONNECTIVITY
    // =========================================================

    private ConnectivityManager getConnectivityManager() {

        return (ConnectivityManager)
                context.getSystemService(
                        Context.CONNECTIVITY_SERVICE
                );
    }


    // =========================================================
    // VPN DETECTION
    // =========================================================

    @JavascriptInterface
    public boolean isVpnActive() {

        try {

            ConnectivityManager cm =
                    getConnectivityManager();

            if (cm == null) {
                return false;
            }

            Network activeNetwork =
                    cm.getActiveNetwork();

            if (activeNetwork == null) {
                return false;
            }

            NetworkCapabilities capabilities =
                    cm.getNetworkCapabilities(
                            activeNetwork
                    );

            if (capabilities == null) {
                return false;
            }

            return capabilities.hasTransport(
                    NetworkCapabilities.TRANSPORT_VPN
            );

        } catch (Exception e) {

            return false;
        }
    }


    // =========================================================
    // PRIVATE DNS DETECTION
    // =========================================================

    @JavascriptInterface
    public String getPrivateDnsStatus() {

        JSONObject result =
                new JSONObject();

        try {

            ConnectivityManager cm =
                    getConnectivityManager();

            if (cm == null) {

                result.put(
                        "supported",
                        false
                );

                result.put(
                        "active",
                        false
                );

                result.put(
                        "hostname",
                        ""
                );

                result.put(
                        "adBlocking",
                        false
                );

                return result.toString();
            }


            Network network =
                    cm.getActiveNetwork();

            if (network == null) {

                result.put(
                        "supported",
                        true
                );

                result.put(
                        "active",
                        false
                );

                result.put(
                        "hostname",
                        ""
                );

                result.put(
                        "adBlocking",
                        false
                );

                return result.toString();
            }


            LinkProperties lp =
                    cm.getLinkProperties(
                            network
                    );

            if (lp == null) {

                result.put(
                        "supported",
                        true
                );

                result.put(
                        "active",
                        false
                );

                result.put(
                        "hostname",
                        ""
                );

                result.put(
                        "adBlocking",
                        false
                );

                return result.toString();
            }


            boolean active = false;
            String hostname = "";


            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.P) {

                active =
                        lp.isPrivateDnsActive();

                String privateDnsName =
                        lp.getPrivateDnsServerName();

                if (privateDnsName != null) {

                    hostname =
                            privateDnsName;
                }
            }


            boolean adBlocking =
                    isKnownAdBlockingDns(
                            hostname
                    );


            result.put(
                    "supported",
                    true
            );

            result.put(
                    "active",
                    active
            );

            result.put(
                    "hostname",
                    hostname
            );

            result.put(
                    "adBlocking",
                    adBlocking
            );


        } catch (Exception e) {

            try {

                result.put(
                        "supported",
                        false
                );

                result.put(
                        "active",
                        false
                );

                result.put(
                        "hostname",
                        ""
                );

                result.put(
                        "adBlocking",
                        false
                );

            } catch (Exception ignored) {
            }
        }


        return result.toString();
    }


    // =========================================================
    // KNOWN AD-BLOCKING DNS
    // =========================================================

    private boolean isKnownAdBlockingDns(
            String hostname
    ) {

        if (hostname == null) {
            return false;
        }


        String host =
                hostname
                        .trim()
                        .toLowerCase(
                                Locale.US
                        );


        if (host.isEmpty()) {
            return false;
        }


        Set<String> knownHosts =
                new HashSet<>(
                        Arrays.asList(

                                "dns.adguard-dns.com",
                                "family.adguard-dns.com",
                                "dns-family.adguard.com",
                                "dns.adguard.com",

                                "dns.nextdns.io",

                                "p0.freedns.controld.com",
                                "p1.freedns.controld.com",
                                "p2.freedns.controld.com",
                                "p3.freedns.controld.com",

                                "adblock.doh.mullvad.net",
                                "adblock.mullvad.net",

                                "adult-filter-dns.cleanbrowsing.org",
                                "family-filter-dns.cleanbrowsing.org",

                                "max.rethinkdns.com"
                        )
                );


        if (knownHosts.contains(host)) {
            return true;
        }


        return host.contains("adguard")
                || host.contains("adblock")
                || host.contains("nextdns")
                || host.contains("controld");
    }


    // =========================================================
    // NETWORK SECURITY STATUS
    // =========================================================

    @JavascriptInterface
    public String getNetworkSecurityStatus() {

        JSONObject result =
                new JSONObject();

        try {

            boolean vpn =
                    isVpnActive();


            String dnsJson =
                    getPrivateDnsStatus();


            JSONObject dns =
                    new JSONObject(
                            dnsJson
                    );


            boolean privateDns =
                    dns.optBoolean(
                            "active",
                            false
                    );


            boolean adBlockingDns =
                    dns.optBoolean(
                            "adBlocking",
                            false
                    );


            result.put(
                    "vpn",
                    vpn
            );


            result.put(
                    "privateDns",
                    privateDns
            );


            result.put(
                    "adBlockingDns",
                    adBlockingDns
            );


            result.put(
                    "privateDnsHostname",
                    dns.optString(
                            "hostname",
                            ""
                    )
            );


            result.put(
                    "blocked",
                    vpn || adBlockingDns
            );


            result.put(
                    "native",
                    true
            );


        } catch (Exception e) {

            try {

                result.put(
                        "vpn",
                        false
                );

                result.put(
                        "privateDns",
                        false
                );

                result.put(
                        "adBlockingDns",
                        false
                );

                result.put(
                        "privateDnsHostname",
                        ""
                );

                result.put(
                        "blocked",
                        false
                );

                result.put(
                        "native",
                        true
                );

            } catch (Exception ignored) {
            }
        }


        return result.toString();
    }


    // =========================================================
    // REWARDED AD — READY CHECK
    // =========================================================

    @JavascriptInterface
    public boolean isRewardedAdReady() {

        return rewardedAd != null
                && !adCurrentlyShowing;
    }


    // =========================================================
    // REWARDED AD — LOADING
    // =========================================================

    @JavascriptInterface
    public void loadRewardedAd() {

        activity.runOnUiThread(
                this::loadRewardedAdInternal
        );
    }


    private void loadRewardedAdInternal() {

        if (isLoadingAd) {
            return;
        }


        if (rewardedAd != null) {
            return;
        }


        if (adCurrentlyShowing) {
            return;
        }


        isLoadingAd = true;


        notifyJavascript(
                "TaskEarnNativeAdLoading",
                "true"
        );


        AdRequest adRequest =
                new AdRequest.Builder()
                        .build();


        RewardedAd.load(
                activity,
                REWARDED_AD_UNIT_ID,
                adRequest,
                new RewardedAdLoadCallback() {

                    @Override
                    public void onAdLoaded(
                            @NonNull RewardedAd ad
                    ) {

                        isLoadingAd =
                                false;

                        rewardedAd =
                                ad;


                        notifyJavascript(
                                "TaskEarnNativeAdReady",
                                "true"
                        );

                        // If the user tapped Watch Ad while the
                        // production ad was still loading, finish
                        // that same secure session automatically
                        // once AdMob becomes ready.
                        if (pendingShowAfterLoad
                                && activeSessionId != null
                                && activeSessionNonce != null
                                && !adCurrentlyShowing) {

                            pendingShowAfterLoad = false;
                            showLoadedRewardedAd();
                        }
                    }


                    @Override
                    public void onAdFailedToLoad(
                            @NonNull LoadAdError error
                    ) {

                        isLoadingAd =
                                false;

                        rewardedAd =
                                null;


                        String message =
                                error.getMessage();


                        if (
                                message == null ||
                                message.trim().isEmpty()
                        ) {

                            message =
                                    "Rewarded ad failed to load.";
                        }


                        notifyJavascript(
                                "TaskEarnNativeAdError",
                                JSONObject.quote(
                                        message
                                )
                        );
                    }
                }
        );
    }


    // =========================================================
    // REWARDED AD — SHOW
    // =========================================================

    @JavascriptInterface
    public String showRewardedAd(
            String sessionId,
            String nonce
    ) {

        if (sessionId == null || sessionId.trim().isEmpty()) {
            return errorJson("SESSION_ID_REQUIRED");
        }

        if (nonce == null || nonce.trim().isEmpty()) {
            return errorJson("SESSION_NONCE_REQUIRED");
        }

        activeSessionId = sessionId.trim();
        activeSessionNonce = nonce.trim();
        lastPaidEvent = null;
        pendingShowAfterLoad = true;

        activity.runOnUiThread(() -> {

            if (adCurrentlyShowing) {
                pendingShowAfterLoad = false;
                notifyJavascript(
                        "TaskEarnNativeAdError",
                        JSONObject.quote("A rewarded ad is already showing.")
                );
                return;
            }

            if (rewardedAd == null) {
                // Keep the secure session alive while AdMob loads.
                // The page's existing session expiry remains the
                // authoritative timeout.
                loadRewardedAdInternal();
                notifyJavascript(
                        "TaskEarnNativeAdLoading",
                        "true"
                );
                return;
            }

            pendingShowAfterLoad = false;
            showLoadedRewardedAd();
        });

        return successJson("AD_SHOW_REQUESTED");
    }

    /**
     * Shows an already-loaded production RewardedAd.
     * Must run on the Android UI thread.
     */
    private void showLoadedRewardedAd() {

        if (adCurrentlyShowing || rewardedAd == null) {
            return;
        }

        if (activeSessionId == null || activeSessionNonce == null) {
            notifyJavascript(
                    "TaskEarnNativeAdError",
                    JSONObject.quote("Ad session is not active.")
            );
            return;
        }

        rewardEarned = false;
        adCurrentlyShowing = true;

        RewardedAd adToShow = rewardedAd;
        rewardedAd = null;

        /*
         * Bind this exact TaskEarn session to AdMob SSV.
         * The nonce is only a correlation value, never money.
         */
        try {
            ServerSideVerificationOptions options =
                    new ServerSideVerificationOptions.Builder()
                            .setCustomData(activeSessionNonce)
                            .build();

            adToShow.setServerSideVerificationOptions(options);

        } catch (Exception e) {

            adCurrentlyShowing = false;
            rewardedAd = adToShow;
            notifyJavascript(
                    "TaskEarnNativeAdError",
                    JSONObject.quote(
                            "Secure ad verification could not be configured."
                    )
            );
            return;
        }

        /*
         * Impression-level paid event.
         * This is provider telemetry only and is never used
         * as a client-authoritative wallet amount.
         */
        adToShow.setOnPaidEventListener(
                new OnPaidEventListener() {

                    @Override
                    public void onPaidEvent(
                            @NonNull AdValue adValue
                    ) {

                        try {

                            long valueMicros =
                                    adValue.getValueMicros();

                            String currency =
                                    adValue.getCurrencyCode();

                            String precision =
                                    precisionToString(
                                            adValue.getPrecisionType()
                                    );

                            JSONObject paid =
                                    new JSONObject();

                            paid.put(
                                    "value_micros",
                                    valueMicros
                            );

                            paid.put(
                                    "currency_code",
                                    currency == null ? "" : currency
                            );

                            paid.put(
                                    "precision",
                                    precision
                            );

                            paid.put(
                                    "ad_unit_id",
                                    adToShow.getAdUnitId()
                            );

                            paid.put(
                                    "session_id",
                                    activeSessionId
                            );

                            paid.put(
                                    "timestamp",
                                    System.currentTimeMillis()
                            );

                            lastPaidEvent = paid;

                            JSONObject display =
                                    new JSONObject();

                            display.put("available", true);
                            display.put(
                                    "currency_code",
                                    currency == null ? "" : currency
                            );
                            display.put(
                                    "precision",
                                    precision
                            );
                            display.put(
                                    "value_micros",
                                    valueMicros
                            );

                            notifyJavascript(
                                    "TaskEarnNativeAdPaid",
                                    display.toString()
                            );

                        } catch (Exception ignored) {
                        }
                    }
                }
        );

        adToShow.setFullScreenContentCallback(
                new FullScreenContentCallback() {

                    @Override
                    public void onAdShowedFullScreenContent() {

                        notifyJavascript(
                                "TaskEarnNativeAdShown",
                                "true"
                        );
                    }

                    @Override
                    public void onAdDismissedFullScreenContent() {

                        adCurrentlyShowing = false;

                        notifyJavascript(
                                "TaskEarnNativeAdCompleted",
                                rewardEarned ? "true" : "false"
                        );

                        activeSessionId = null;
                        activeSessionNonce = null;
                        pendingShowAfterLoad = false;

                        loadRewardedAdInternal();
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(
                            @NonNull AdError adError
                    ) {

                        adCurrentlyShowing = false;
                        rewardEarned = false;
                        pendingShowAfterLoad = false;

                        notifyJavascript(
                                "TaskEarnNativeAdCompleted",
                                "false"
                        );

                        notifyJavascript(
                                "TaskEarnNativeAdError",
                                JSONObject.quote(
                                        adError.getMessage() == null
                                                ? "Rewarded ad failed to show."
                                                : adError.getMessage()
                                )
                        );

                        activeSessionId = null;
                        activeSessionNonce = null;

                        loadRewardedAdInternal();
                    }
                }
        );

        /*
         * ACTUAL PRODUCTION REWARDED AD DISPLAY.
         */
        adToShow.show(
                activity,
                rewardItem -> {

                    rewardEarned = true;

                    try {

                        JSONObject data =
                                new JSONObject();

                        data.put(
                                "earned",
                                true
                        );

                        data.put(
                                "reward_item_amount",
                                rewardItem.getAmount()
                        );

                        data.put(
                                "reward_item_type",
                                rewardItem.getType()
                        );

                        data.put(
                                "timestamp",
                                System.currentTimeMillis()
                        );

                        if (lastPaidEvent != null) {
                            data.put(
                                    "paid_event",
                                    lastPaidEvent
                            );
                        }

                        notifyJavascript(
                                "TaskEarnNativeRewardEarned",
                                data.toString()
                        );

                    } catch (Exception ignored) {
                    }
                }
        );
    }



