package com.taskearn.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
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
    private int adLoadRetryCount = 0;
    private final Handler adRetryHandler = new Handler(Looper.getMainLooper());

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

                        // Keep trying to obtain a production rewarded ad
                        // after transient no-fill/network failures. The
                        // user's secure session is retained separately and
                        // is only used if an ad actually becomes available.
                        final long retryDelayMs =
                                Math.min(
                                        30000L,
                                        3000L * (1L << Math.min(adLoadRetryCount, 3))
                                );

                        adLoadRetryCount =
                                Math.min(adLoadRetryCount + 1, 4);

                        adRetryHandler.postDelayed(
                                TaskEarnBridge.this::loadRewardedAdInternal,
                                retryDelayMs
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

        activity.runOnUiThread(() -> {

            if (adCurrentlyShowing) {
                notifyJavascript(
                        "TaskEarnNativeAdError",
                        JSONObject.quote("A rewarded ad is already showing.")
                );
                return;
            }

            if (rewardedAd == null) {
                pendingShowAfterLoad = true;
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

        try {

            JSONObject started =
                    new JSONObject();

            started.put("ok", true);
            started.put("started", true);
            started.put("session_id", activeSessionId);

            return started.toString();

        } catch (Exception e) {

            return "{\"ok\":true,\"started\":true}";
        }
    }


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
             * This is display/reconciliation telemetry only.
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

                                /*
                                 * User sees only the money amount.
                                 * No "Your 50%" / "50% share" wording.
                                 */
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
                                        "gross_value_micros",
                                        valueMicros
                                );
                                display.put(
                                        "user_value_micros",
                                        valueMicros / 2L
                                );

                                notifyJavascript(
                                        "TaskEarnNativeAdPaid",
                                        display.toString()
                                );

                            } catch (Exception ignored) {
                                // Never break ad playback.
                            }
                        }
                    }
            );

            notifyJavascript(
                    "TaskEarnNativeAdStarted",
                    "true"
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

                            loadRewardedAdInternal();
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(
                                @NonNull AdError adError
                        ) {

                            adCurrentlyShowing = false;
                            rewardEarned = false;

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
             * ACTUAL REWARDED AD DISPLAY.
             */
            adToShow.show(
                    activity,
                    rewardItem -> {

                        rewardEarned = true;

                        /*
                         * reward_amount is the configured reward item,
                         * NOT actual advertising revenue.
                         */
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

    private String errorJson(
            String error
    ) {

        try {

            JSONObject result =
                    new JSONObject();

            result.put("ok", false);
            result.put("error", error);

            return result.toString();

        } catch (Exception e) {

            return "{\"ok\":false,\"error\":\"" +
                    error +
                    "\"}";
        }
    }


    private String precisionToString(
            int precision
    ) {

        /*
         * Google Mobile Ads SDK 24.x exposes the precision type
         * as an integer from getPrecisionType(). Keep this mapping
         * numeric so the code remains compatible with the 24.x API.
         *
         * 0 = UNKNOWN
         * 1 = ESTIMATED
         * 2 = PUBLISHER_PROVIDED
         * 3 = PRECISE
         */
        switch (precision) {

            case 3:
                return "PRECISE";

            case 1:
                return "ESTIMATED";

            case 2:
                return "PUBLISHER_PROVIDED";

            case 0:
            default:
                return "UNKNOWN";
        }
    }


    // =========================================================
    // JAVASCRIPT CALLBACK SYSTEM
    // =========================================================

    private void notifyJavascript(
            String functionName,
            String value
    ) {

        if (webView == null) {
            return;
        }


        activity.runOnUiThread(() -> {

            try {

                String safeValue =
                        value == null
                                ? "null"
                                : value;


                String javascript =
                        "window." +
                        functionName +
                        " && window." +
                        functionName +
                        "(" +
                        safeValue +
                        ");";


                webView.evaluateJavascript(
                        javascript,
                        null
                );


            } catch (Exception ignored) {
            }
        });
    }


    // =========================================================
    // NETWORK SETTINGS
    // =========================================================

    @JavascriptInterface
    public void openNetworkSettings() {

        try {

            Intent intent =
                    new Intent(
                            Settings.ACTION_WIRELESS_SETTINGS
                    );


            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
            );


            context.startActivity(
                    intent
            );


        } catch (Exception e) {

            try {

                Intent fallback =
                        new Intent(
                                Settings.ACTION_SETTINGS
                        );


                fallback.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                );


                context.startActivity(
                        fallback
                );


            } catch (Exception ignored) {
            }
        }
    }


    // =========================================================
    // PRIVATE DNS SETTINGS
    // =========================================================

    @JavascriptInterface
    public void openPrivateDnsSettings() {

        try {

            Intent intent =
                    new Intent(
                            Settings.ACTION_WIRELESS_SETTINGS
                    );


            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
            );


            context.startActivity(
                    intent
            );


        } catch (Exception ignored) {
        }
    }


    // =========================================================
    // NATIVE DETECTOR
    // =========================================================

    @JavascriptInterface
    public boolean isNativeDetectorAvailable() {

        return true;
    }
}
