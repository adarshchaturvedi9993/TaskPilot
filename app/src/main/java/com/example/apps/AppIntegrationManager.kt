package com.example.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.CalendarContract
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.ui.graphics.vector.ImageVector

data class AppIntegrationInfo(
    val id: String,
    val name: String,
    val packageName: String?,
    val description: String,
    val icon: ImageVector,
    val isInstalled: Boolean,
    val supportedCapabilities: List<String>,
    val userRestrictedActions: List<String>,
    val intentSample: String,
    val troubleshootingHelp: String
)

class AppIntegrationManager(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    fun isPackageInstalled(packageName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun canResolveIntent(intent: Intent): Boolean {
        return intent.resolveActivity(packageManager) != null
    }

    fun getAllIntegrations(): List<AppIntegrationInfo> {
        val hasYouTube = isPackageInstalled("com.google.android.youtube")
        val hasChrome = isPackageInstalled("com.android.chrome")
        val hasMaps = isPackageInstalled("com.google.android.apps.maps")
        val hasDialer = canResolveIntent(Intent(Intent.ACTION_DIAL, Uri.parse("tel:123")))
        val hasSms = canResolveIntent(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:123")))
        val hasCalendar = canResolveIntent(Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI))

        return listOf(
            AppIntegrationInfo(
                id = "youtube",
                name = "YouTube",
                packageName = "com.google.android.youtube",
                description = "Search videos, discover creators, and navigate to YouTube channels.",
                icon = Icons.Default.PlayArrow,
                isInstalled = hasYouTube,
                supportedCapabilities = listOf(
                    "Search videos and topics",
                    "Open specific channels or handles",
                    "Deep link playback into YouTube app",
                    "Fallback to verified browser links"
                ),
                userRestrictedActions = listOf(
                    "Subscribing to channels (requires user tap)",
                    "Liking / Disliking videos (anti-bot policy)",
                    "Posting comments (user privacy restriction)"
                ),
                intentSample = "vnd.youtube://results?search_query=...",
                troubleshootingHelp = "If YouTube does not open, make sure YouTube app is installed from Play Store and set as default for supported links."
            ),
            AppIntegrationInfo(
                id = "browser",
                name = "Chrome / Browser",
                packageName = "com.android.chrome",
                description = "Query information via Google Search and open verified web destinations.",
                icon = Icons.Default.Language,
                isInstalled = hasChrome || canResolveIntent(Intent(Intent.ACTION_VIEW, Uri.parse("https://google.com"))),
                supportedCapabilities = listOf(
                    "Perform Google web searches",
                    "Open official documentation & articles",
                    "HTTPS validated link resolution"
                ),
                userRestrictedActions = listOf(
                    "Form submissions with sensitive personal data",
                    "Automated captcha or login completion"
                ),
                intentSample = "https://www.google.com/search?q=...",
                troubleshootingHelp = "Ensure Chrome or any standard browser is enabled in Android default app settings."
            ),
            AppIntegrationInfo(
                id = "maps",
                name = "Google Maps",
                packageName = "com.google.android.apps.maps",
                description = "Locate places, check distances, and initiate turn-by-turn navigation.",
                icon = Icons.Default.LocationOn,
                isInstalled = hasMaps || canResolveIntent(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=test"))),
                supportedCapabilities = listOf(
                    "Search destination coordinates or place names",
                    "Launch turn-by-turn navigation",
                    "View location details on interactive maps"
                ),
                userRestrictedActions = listOf(
                    "Selecting toll/avoid highway preferences (user choice)"
                ),
                intentSample = "google.navigation:q=Latitude,Longitude",
                troubleshootingHelp = "Ensure Google Maps is installed and location permission is granted if live GPS is required."
            ),
            AppIntegrationInfo(
                id = "phone",
                name = "Phone & Dialer",
                packageName = null,
                description = "Safely pre-populate telephone numbers into the system dialer.",
                icon = Icons.Default.Call,
                isInstalled = hasDialer,
                supportedCapabilities = listOf(
                    "Pre-fill contact or business telephone numbers",
                    "Safety confirmation prompt before opening dialer"
                ),
                userRestrictedActions = listOf(
                    "Placing calls without user tapping Call button (Safety Guardrail)"
                ),
                intentSample = "tel:+1234567890",
                troubleshootingHelp = "Dialer is built into Android. Tap the green call button after TaskPilot opens the dialer."
            ),
            AppIntegrationInfo(
                id = "messages",
                name = "Messages / SMS",
                packageName = null,
                description = "Draft messages with recipient and body for user confirmation.",
                icon = Icons.Default.Email,
                isInstalled = hasSms,
                supportedCapabilities = listOf(
                    "Draft SMS with pre-filled recipient and text",
                    "Interactive message preview card"
                ),
                userRestrictedActions = listOf(
                    "Silently sending messages (strict anti-spam policy)"
                ),
                intentSample = "smsto:+1234567890",
                troubleshootingHelp = "Default messaging app will open with the prepared text. Tap Send when ready."
            ),
            AppIntegrationInfo(
                id = "calendar",
                name = "Google Calendar",
                packageName = "com.google.android.calendar",
                description = "Schedule reminders, meetings, and events into the device calendar.",
                icon = Icons.Default.Today,
                isInstalled = hasCalendar,
                supportedCapabilities = listOf(
                    "Draft event with title, notes, and timestamp",
                    "Display confirmation dialog before launching calendar"
                ),
                userRestrictedActions = listOf(
                    "Silently overriding existing appointments"
                ),
                intentSample = "vnd.android.cursor.dir/event",
                troubleshootingHelp = "Ensure calendar sync is active in your Google account or device settings."
            ),
            AppIntegrationInfo(
                id = "files",
                name = "Files / Storage",
                packageName = null,
                description = "Access user-selected documents and photos via Storage Access Framework.",
                icon = Icons.Default.Folder,
                isInstalled = true,
                supportedCapabilities = listOf(
                    "Zero-permission Photo Picker",
                    "Storage Access Framework (SAF) document picker",
                    "Local metadata extraction from authorized files"
                ),
                userRestrictedActions = listOf(
                    "Arbitrary unprompted scanning of private storage folders"
                ),
                intentSample = "Intent.ACTION_OPEN_DOCUMENT",
                troubleshootingHelp = "Select files explicitly when prompted by the system document picker."
            ),
            AppIntegrationInfo(
                id = "settings",
                name = "Android Settings",
                packageName = null,
                description = "Direct shortcuts to Wi-Fi, Bluetooth, Apps, Display and Sound settings.",
                icon = Icons.Default.Settings,
                isInstalled = true,
                supportedCapabilities = listOf(
                    "Open Wi-Fi connection settings",
                    "Open Bluetooth pairing screen",
                    "Open Application details and permissions"
                ),
                userRestrictedActions = listOf(
                    "Silently disabling security or toggling airplane mode"
                ),
                intentSample = "android.settings.SETTINGS",
                troubleshootingHelp = "TaskPilot navigates you to the right system screen so you can make settings adjustments safely."
            )
        )
    }

    fun testLaunchApp(integrationId: String): Pair<Boolean, String> {
        return try {
            val intent = when (integrationId) {
                "youtube" -> {
                    val ytIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                        setPackage("com.google.android.youtube")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    if (canResolveIntent(ytIntent)) ytIntent else Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                "browser" -> Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                "maps" -> Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=restaurants")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                "phone" -> Intent(Intent.ACTION_DIAL, Uri.parse("tel:100")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                "messages" -> Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                "calendar" -> Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("content://com.android.calendar/time")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                "settings" -> Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                else -> null
            }

            if (intent != null) {
                context.startActivity(intent)
                Pair(true, "Successfully launched test intent for $integrationId.")
            } else {
                Pair(false, "No test intent configured for $integrationId.")
            }
        } catch (e: Exception) {
            Pair(false, "Could not launch: ${e.localizedMessage ?: "Unknown error"}")
        }
    }
}
