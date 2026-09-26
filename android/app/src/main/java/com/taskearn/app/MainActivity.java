package com.taskearn.app;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.MobileAds;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "taskearn_runtime";
    private static final String LAST_PAGE_KEY = "last_internal_page";
    private static final String DEFAULT_PAGE = "pages/login.html";

    private WebView webView;
    private TaskEarnBridge bridge;
    private SharedPreferences runtimePrefs;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        runtimePrefs = getSharedPreferences(
                PREFS_NAME,
                MODE_PRIVATE
        );

        // -----------------------------------------
        // GOOGLE MOBILE ADS INITIALIZATION
        // -----------------------------------------
        MobileAds.initialize(
                this,
                initializationStatus -> {
                    // AdMob SDK initialized
                }
        );

        // -----------------------------------------
        // WEBVIEW
        // -----------------------------------------
        webView = new WebView(this);

        setContentView(webView);

        WebSettings settings =
                webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        // The app's HTML pages use a native Android bridge for
        // Google Mobile Ads. File access is not needed for those
        // pages, but WebView must keep normal JavaScript/DOM
        // execution enabled.
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        settings.setMediaPlaybackRequiresUserGesture(false);

        if (android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.LOLLIPOP) {

            settings.setMixedContentMode(
                    WebSettings.MIXED_CONTENT_NEVER_ALLOW
            );
        }

        WebView.setWebContentsDebuggingEnabled(false);

        webView.setWebViewClient(
                new WebViewClient() {

                    @Override
                    public void onPageStarted(
                            WebView view,
                            String url,
                            android.graphics.Bitmap favicon
                    ) {
                        super.onPageStarted(view, url, favicon);
                        rememberInternalPage(url);
                    }
                }
        );

        webView.setWebChromeClient(
                new WebChromeClient()
        );

        // -----------------------------------------
        // TASK EARN NATIVE BRIDGE
        // -----------------------------------------
        bridge =
                new TaskEarnBridge(this);

        webView.addJavascriptInterface(
                bridge,
                "AndroidAds"
        );

        webView.addJavascriptInterface(
                bridge,
                "TaskEarnAds"
        );

        // Exact name expected by ad-revenue-share.html.
        webView.addJavascriptInterface(
                bridge,
                "AndroidTaskEarnAds"
        );

        // VERY IMPORTANT:
        // Java bridge ko WebView reference dena
        // taaki native callbacks HTML/JS tak pahunch sake.
        bridge.setWebView(webView);

        // -----------------------------------------
        // LOAD APP
        // -----------------------------------------
        //
        // Supabase keeps the authenticated session persistent.
        // We additionally remember the last internal page so
        // Android can restore the same page after the app has
        // been backgrounded or its Activity recreated.
        //
        // Only local TaskEarn pages are restored. External URLs
        // are never persisted as the app's startup page.
        String lastPage = runtimePrefs.getString(
                LAST_PAGE_KEY,
                DEFAULT_PAGE
        );

        if (!isSafeInternalPage(lastPage)) {
            lastPage = DEFAULT_PAGE;
        }

        webView.loadUrl(
                "file:///android_asset/" + lastPage
        );
    }

    private boolean isSafeInternalPage(String page) {

        if (page == null || page.trim().isEmpty()) {
            return false;
        }

        String value = page.trim();

        return value.startsWith("pages/")
                && value.endsWith(".html")
                && !value.contains("..")
                && !value.contains("\\")
                && !value.contains("?")
                && !value.contains("#");
    }

    private void rememberInternalPage(String url) {

        if (runtimePrefs == null || url == null) {
            return;
        }

        final String prefix = "file:///android_asset/";
        if (!url.startsWith(prefix)) {
            return;
        }

        String page = url.substring(prefix.length());

        if (isSafeInternalPage(page)) {
            runtimePrefs.edit()
                    .putString(LAST_PAGE_KEY, page)
                    .apply();
        }
    }

    private void rememberCurrentPage() {

        if (webView == null) {
            return;
        }

        rememberInternalPage(webView.getUrl());
    }

    @Override
    protected void onPause() {

        rememberCurrentPage();

        if (webView != null) {
            webView.onPause();
        }

        super.onPause();
    }

    @Override
    protected void onResume() {

        super.onResume();

        if (webView != null) {
            webView.onResume();
        }
    }

    @Override
    public void onBackPressed() {

        if (webView != null &&
                webView.canGoBack()) {

            webView.goBack();

        } else {

            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {

        if (webView != null) {

            webView.removeJavascriptInterface(
                    "AndroidAds"
            );

            webView.removeJavascriptInterface(
                    "TaskEarnAds"
            );

            webView.removeJavascriptInterface(
                    "AndroidTaskEarnAds"
            );

            webView.destroy();

            webView = null;
        }

        super.onDestroy();
    }
}