package com.example.apprules

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class AppInfo(
    val packageName: String,
    val label: String
)

@Singleton
class InstalledAppsProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getInstalledApps(): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfoList = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        return resolveInfoList.map {
            AppInfo(
                packageName = it.activityInfo.packageName,
                label = it.loadLabel(pm).toString()
            )
        }.distinctBy { it.packageName }.sortedBy { it.label }
    }
}
