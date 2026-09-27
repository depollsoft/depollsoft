package depollsoft.lib.util

import android.content.Context
import android.content.pm.PackageManager

/**
 * The installed package's version name. Release builds set it from the release plan, so it is the
 * version the store shows, whatever the version strings in resources say.
 */
fun appVersionName(context: Context): String =
    try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    } catch (_: PackageManager.NameNotFoundException) {
        ""
    }
