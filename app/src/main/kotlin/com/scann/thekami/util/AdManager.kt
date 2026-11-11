package com.scann.thekami.util

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.scann.thekami.BuildConfig
import java.util.concurrent.atomic.AtomicBoolean

/**
 * All ad logic lives here.
 *  - App Open ad: cold start only, at most once every 2 hours.
 *  - Interstitial: after closing a scan result, every 3rd scan.
 *  - Never two ads within 3 minutes, never a scan ad within 2 minutes of the app-open ad.
 *  - Any load failure is silent: the app never waits for an ad.
 */
object AdManager {
    private const val TAG = "AdManager"
    private const val PREFS = "scann_ads"
    private const val KEY_LAST_APP_OPEN = "last_app_open_ad"
    private const val KEY_LAST_ANY_AD = "last_any_ad"
    private const val KEY_SCAN_COUNTER = "scans_since_ad"

    private const val APP_OPEN_GAP_MS = 2L * 60 * 60 * 1000
    private const val AD_GAP_MS = 3L * 60 * 1000
    private const val APP_OPEN_VS_SCAN_GAP_MS = 2L * 60 * 1000
    private const val SCANS_PER_AD = 3
    private const val APP_OPEN_MAX_WAIT_MS = 5_000L
    private const val INTERSTITIAL_TTL_MS = 50L * 60 * 1000

    private val started = AtomicBoolean(false)
    private val adsInitialized = AtomicBoolean(false)
    private var appStartElapsed = 0L

    private var interstitial: InterstitialAd? = null
    private var interstitialLoadedAt = 0L
    private var interstitialLoading = false
    private var adShowing = false

    /** True once any ad was shown in this app process. Used to skip the rating prompt. */
    var adShownThisSession = false
        private set

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun within(last: Long, now: Long, gap: Long) = last in 1..now && now - last < gap

    // ---------------------------------------------------------------- start

    /** Call once from MainActivity.onCreate on a fresh launch (not on recreation). */
    fun start(activity: Activity) {
        if (!started.compareAndSet(false, true)) return
        appStartElapsed = SystemClock.elapsedRealtime()

        val consent = UserMessagingPlatform.getConsentInformation(activity)
        consent.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                    if (error != null) Log.w(TAG, "Consent form: ${error.message}")
                    if (consent.canRequestAds()) initAds(activity)
                }
            },
            { error ->
                Log.w(TAG, "Consent info: ${error.message}")
                if (consent.canRequestAds()) initAds(activity)
            }
        )
        // Consent already given in an earlier session: no need to wait for the network.
        if (consent.canRequestAds()) initAds(activity)
    }

    private fun initAds(activity: Activity) {
        if (!adsInitialized.compareAndSet(false, true)) return
        val app = activity.applicationContext
        MobileAds.initialize(app) {
            activity.runOnUiThread {
                loadInterstitial(app)
                maybeShowAppOpen(activity, app)
            }
        }
    }

    // ------------------------------------------------------------- app open

    private fun maybeShowAppOpen(activity: Activity, app: Context) {
        val now = System.currentTimeMillis()
        if (within(prefs(app).getLong(KEY_LAST_APP_OPEN, 0L), now, APP_OPEN_GAP_MS)) return

        AppOpenAd.load(
            app,
            BuildConfig.ADMOB_APP_OPEN_ID,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    val tooLate = SystemClock.elapsedRealtime() - appStartElapsed > APP_OPEN_MAX_WAIT_MS
                    val visible = (activity as? LifecycleOwner)?.lifecycle?.currentState
                        ?.isAtLeast(Lifecycle.State.STARTED) == true
                    if (tooLate || !visible || activity.isFinishing || adShowing) return
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdShowedFullScreenContent() {
                            val t = System.currentTimeMillis()
                            prefs(app).edit()
                                .putLong(KEY_LAST_APP_OPEN, t)
                                .putLong(KEY_LAST_ANY_AD, t)
                                .apply()
                            adShownThisSession = true
                        }
                        override fun onAdDismissedFullScreenContent() { adShowing = false }
                        override fun onAdFailedToShowFullScreenContent(error: AdError) { adShowing = false }
                    }
                    adShowing = true
                    ad.show(activity)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "App open load failed: ${error.message}")
                }
            }
        )
    }

    // --------------------------------------------------------- interstitial

    private fun loadInterstitial(app: Context) {
        if (interstitial != null || interstitialLoading) return
        interstitialLoading = true
        InterstitialAd.load(
            app,
            BuildConfig.ADMOB_INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                    interstitialLoadedAt = SystemClock.elapsedRealtime()
                    interstitialLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Interstitial load failed: ${error.message}")
                    interstitial = null
                    interstitialLoading = false
                }
            }
        )
    }

    private fun freshInterstitial(): InterstitialAd? {
        val ad = interstitial ?: return null
        if (SystemClock.elapsedRealtime() - interstitialLoadedAt > INTERSTITIAL_TTL_MS) {
            interstitial = null
            return null
        }
        return ad
    }

    /** Call when the user closes a scan result. Shows an interstitial only when it is due. */
    fun onScanResultClosed(activity: Activity) {
        val app = activity.applicationContext
        val p = prefs(app)
        val count = p.getInt(KEY_SCAN_COUNTER, 0) + 1
        p.edit().putInt(KEY_SCAN_COUNTER, count).apply()

        if (!adsInitialized.get() || count < SCANS_PER_AD) return

        val ad = freshInterstitial()
        if (ad == null) {
            loadInterstitial(app)
            return
        }
        val now = System.currentTimeMillis()
        if (within(p.getLong(KEY_LAST_ANY_AD, 0L), now, AD_GAP_MS)) return
        if (within(p.getLong(KEY_LAST_APP_OPEN, 0L), now, APP_OPEN_VS_SCAN_GAP_MS)) return
        if (activity.isFinishing || adShowing) return

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                p.edit()
                    .putInt(KEY_SCAN_COUNTER, 0)
                    .putLong(KEY_LAST_ANY_AD, System.currentTimeMillis())
                    .apply()
            }
            override fun onAdDismissedFullScreenContent() {
                adShowing = false
                loadInterstitial(app)
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                adShowing = false
                loadInterstitial(app)
            }
        }
        interstitial = null
        adShowing = true
        adShownThisSession = true
        ad.show(activity)
    }
}
