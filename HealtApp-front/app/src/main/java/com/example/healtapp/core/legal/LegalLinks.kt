package com.example.healtapp.core.legal

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.healtapp.BuildConfig

object LegalLinks {
    val privacyPolicyUrl: String = BuildConfig.PRIVACY_POLICY_URL
    val termsOfServiceUrl: String = BuildConfig.TERMS_OF_SERVICE_URL

    fun openPrivacyPolicy(context: Context) = openUrl(context, privacyPolicyUrl)

    fun openTermsOfService(context: Context) = openUrl(context, termsOfServiceUrl)

    fun openUrl(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }
}
