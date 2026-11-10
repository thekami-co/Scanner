package com.scann.thekami.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import com.google.android.play.core.review.ReviewManagerFactory

object ReviewHelper {

    private fun Context.findActivity(): Activity? {
        var current: Context? = this
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }

    fun openPlayStoreListing(context: Context) {
        try {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                )
            )
        } catch (e: Exception) {
            // No browser or Play Store available
        }
    }

    fun launchInAppReview(context: Context) {
        try {
            val activity = context.findActivity()
            if (activity == null) {
                openPlayStoreListing(context)
                return
            }
            val reviewManager = ReviewManagerFactory.create(context)
            reviewManager.requestReviewFlow().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    reviewManager.launchReviewFlow(activity, task.result)
                } else {
                    openPlayStoreListing(context)
                }
            }
        } catch (e: Exception) {
            openPlayStoreListing(context)
        }
    }
}
