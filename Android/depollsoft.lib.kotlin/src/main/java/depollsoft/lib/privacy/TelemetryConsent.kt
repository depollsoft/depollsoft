package depollsoft.lib.privacy

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import depollsoft.lib.kotlin.R

class PrivacyChoices(context: Context) {
    private val prefs = context.getSharedPreferences("telemetry_consent", Context.MODE_PRIVATE)
    val hasChosen: Boolean get() = prefs.getBoolean("chosen", false)
    val analytics: Boolean get() = hasChosen && prefs.getBoolean("analytics", false)
    val crashes: Boolean get() = hasChosen && prefs.getBoolean("crashes", false)

    fun save(analytics: Boolean, crashes: Boolean) {
        prefs.edit().putBoolean("chosen", true).putBoolean("analytics", analytics)
            .putBoolean("crashes", crashes).commit()
    }
}

object TelemetryConsent {
    var applyChoices: (Boolean, Boolean) -> Unit = { _, _ -> }
    var adPrivacyRequired: () -> Boolean = { false }
    var showAdPrivacy: (Activity) -> Unit = { }
    private var promptedThisSession = false

    fun showIfNeeded(activity: FragmentActivity) {
        if (!PrivacyChoices(activity).hasChosen && !promptedThisSession) {
            promptedThisSession = true
            show(activity)
        }
    }

    fun show(activity: FragmentActivity) {
        if (!activity.supportFragmentManager.isStateSaved &&
            activity.supportFragmentManager.findFragmentByTag("telemetry_consent") == null) {
            PrivacyDialog().show(activity.supportFragmentManager, "telemetry_consent")
        }
    }

    interface Host {
        fun onPrivacyChoicesClosed()
    }
}

class PrivacyDialog : DialogFragment() {
    private lateinit var analytics: SwitchCompat
    private lateinit var crashes: SwitchCompat

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val choices = PrivacyChoices(context)
        val spacing = (20 * resources.displayMetrics.density).toInt()
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(spacing, spacing / 2, spacing, spacing / 2)
        }
        fun paragraph(id: Int) = TextView(context).apply {
            setText(id)
            textSize = 16f
            val colors = context.obtainStyledAttributes(intArrayOf(android.R.attr.textColorPrimary))
            colors.getColorStateList(0)?.let { setTextColor(it) }
            colors.recycle()
            setPadding(0, 0, 0, spacing)
            content.addView(this)
        }
        paragraph(R.string.privacy_intro)
        fun choice(title: Int, detail: Int, checked: Boolean) = SwitchCompat(context).apply {
            setText(title)
            textSize = 17f
            minHeight = (48 * resources.displayMetrics.density).toInt()
            isChecked = checked
            content.addView(this, ViewGroup.LayoutParams(-1, -2))
            paragraph(detail)
        }
        analytics = choice(R.string.privacy_analytics, R.string.privacy_analytics_detail,
            savedInstanceState?.getBoolean("analytics") ?: choices.analytics)
        crashes = choice(R.string.privacy_crashes, R.string.privacy_crashes_detail,
            savedInstanceState?.getBoolean("crashes") ?: choices.crashes)
        fun link(title: Int, action: () -> Unit) {
            val button = Button(context).apply {
                setText(title)
                isAllCaps = false
                setOnClickListener { action() }
            }
            content.addView(button, ViewGroup.LayoutParams(-1, -2))
        }
        link(R.string.privacy_policy) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://apps.depoll.com/privacy/")))
        }
        if (TelemetryConsent.adPrivacyRequired()) {
            link(R.string.privacy_ads) { TelemetryConsent.showAdPrivacy(requireActivity()) }
        }
        val builder = AlertDialog.Builder(context)
            .setTitle(R.string.privacy_title)
            .setView(ScrollView(context).apply { addView(content) })
            .setPositiveButton(R.string.privacy_save) { _, _ -> save(analytics.isChecked, crashes.isChecked) }
            .setNegativeButton(R.string.privacy_decline) { _, _ -> save(false, false) }
        if (choices.hasChosen) builder.setNeutralButton(android.R.string.cancel, null)
        return builder.create()
    }

    private fun save(analytics: Boolean, crashes: Boolean) {
        PrivacyChoices(requireContext()).save(analytics, crashes)
        TelemetryConsent.applyChoices(analytics, crashes)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("analytics", analytics.isChecked)
        outState.putBoolean("crashes", crashes.isChecked)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        (activity as? TelemetryConsent.Host)?.onPrivacyChoicesClosed()
    }
}
