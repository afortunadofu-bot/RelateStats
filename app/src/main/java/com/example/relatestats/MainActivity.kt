package com.example.relatestats

import android.accessibilityservice.AccessibilityService
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.text.TextUtils
import android.util.Base64
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

// ==========================================
// 1. 視覺設計系統
// ==========================================
object AppColors {
    val Background = Color(0xFFF9FAFB)
    val Surface = Color(0xFFFFFFFF)
    val TextPrimary = Color(0xFF111827)
    val TextSecondary = Color(0xFF6B7280)
    val BorderLight = Color(0xFFE5E7EB)
    val BrandPrimary = Color(0xFF0F172A)
    val ActiveBlue = Color(0xFF3B82F6)

    val WorkBg = Color(0xFFFEE2E2); val WorkText = Color(0xFFB91C1C)
    val FamilyBg = Color(0xFFDBEAFE); val FamilyText = Color(0xFF1D4ED8)
    val FriendBg = Color(0xFFD1FAE5); val FriendText = Color(0xFF047857)
    val OtherBg = Color(0xFFF3F4F6); val OtherText = Color(0xFF4B5563)
    val GroupBg = Color(0xFFFEF3C7); val GroupText = Color(0xFFB45309)

    val AppBrands = mapOf(
        "LINE" to Color(0xFF06C755), "Instagram" to Color(0xFFE1306C),
        "Messenger" to Color(0xFF00B2FF), "WhatsApp" to Color(0xFF25D366),
        "Threads" to Color(0xFF000000), "Telegram" to Color(0xFF24A1DE),
        "電話" to Color(0xFF4ADE80)
    )

    val CustomPalette = listOf(
        Pair(Color(0xFF9333EA), Color(0xFFF3E8FF)), Pair(Color(0xFFC026D3), Color(0xFFFAE8FF)),
        Pair(Color(0xFFE11D48), Color(0xFFFFE4E6)), Pair(Color(0xFF0D9488), Color(0xFFCCFBF1)),
        Pair(Color(0xFF0284C7), Color(0xFFE0F2FE)), Pair(Color(0xFF4F46E5), Color(0xFFE0E7FF)),
        Pair(Color(0xFF059669), Color(0xFFD1FAE5))
    )
}

data class ContactCategory(val id: String, val label: String, val colorInt: Int, val bgColorInt: Int) {
    val color: Color get() = Color(colorInt)
    val bgColor: Color get() = Color(bgColorInt)
    fun toCsvString() = listOf(id, label, colorInt.toString(), bgColorInt.toString()).joinToString("|cat|")
    companion object {
        val WORK = ContactCategory("WORK", "工作", AppColors.WorkText.toArgb(), AppColors.WorkBg.toArgb())
        val FAMILY = ContactCategory("FAMILY", "家人", AppColors.FamilyText.toArgb(), AppColors.FamilyBg.toArgb())
        val FRIEND = ContactCategory("FRIEND", "朋友", AppColors.FriendText.toArgb(), AppColors.FriendBg.toArgb())
        val OTHER = ContactCategory("OTHER", "未分類", AppColors.OtherText.toArgb(), AppColors.OtherBg.toArgb())
        val defaultCategories = listOf(WORK, FAMILY, FRIEND, OTHER)
        fun fromCsvString(csv: String): ContactCategory? {
            try {
                val parts = csv.split("|cat|")
                if (parts.size >= 4) return ContactCategory(parts[0], parts[1], parts[2].toInt(), parts[3].toInt())
            } catch (_: Exception) { }
            return null
        }
    }
}

enum class FilterType { SPECIFIC_DAY, WEEK, ALL }
data class DateFilter(val type: FilterType, val timestamp: Long = System.currentTimeMillis()) {
    fun isSameDay(otherTime: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = this@DateFilter.timestamp }
        val cal2 = Calendar.getInstance().apply { timeInMillis = otherTime }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}

enum class SortType(val label: String) { TIME("停留時間"), MESSAGES("互動頻率") }
enum class ContactSortType(val label: String) { NAME("名稱"), INTERACTIONS("互動量") }

// 🏆 回歸純粹秒數，效能與精準度最高
fun formatDuration(seconds: Int): String {
    return if (seconds >= 3600) {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        "${h}h ${m}m"
    } else if (seconds >= 60) {
        val m = seconds / 60
        val s = seconds % 60
        "${m}m ${s}s"
    } else {
        "${seconds}s"
    }
}

fun getStartOfWeekTimestamp(timestamp: Long, startDay: Int): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    cal.firstDayOfWeek = startDay
    while (cal.get(Calendar.DAY_OF_WEEK) != startDay) {
        cal.add(Calendar.DATE, -1)
    }
    cal.apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

fun getEndOfWeekTimestamp(timestamp: Long, startDay: Int): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = getStartOfWeekTimestamp(timestamp, startDay) }
    cal.add(Calendar.DATE, 6)
    cal.apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }
    return cal.timeInMillis
}

// ==========================================
// 2. 資料模型
// ==========================================
data class InteractionRecord(
    val id: String = UUID.randomUUID().toString(),
    val rawSenderName: String,
    val appName: String,
    val isIncoming: Boolean,
    val wordCount: Int,
    val durationSeconds: Int = 0,
    val isGroup: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val isMissedCall: Boolean = false,
    val isCall: Boolean = false,
    val subSender: String? = null
) {
    fun toCsvString() = listOf(id, rawSenderName, appName, isIncoming.toString(), wordCount.toString(), durationSeconds.toString(), isGroup.toString(), timestamp.toString()).joinToString("|#|")
    companion object {
        fun fromCsvString(csv: String): InteractionRecord? {
            try {
                val parts = csv.split("|#|")
                if (parts.size >= 8) return InteractionRecord(parts[0], parts[1], parts[2], parts[3].toBoolean(), parts[4].toInt(), parts[5].toInt(), parts[6].toBoolean(), parts[7].toLong())
            } catch (e: Exception) { }
            return null
        }
    }
}


data class HourlyStat(val hour: Int, var durationSeconds: Int = 0, var msgCount: Int = 0, var missedCalls: Int = 0) {
    val score: Int get() = (durationSeconds / 60) + (if (durationSeconds % 60 > 0) 1 else 0) + msgCount + (missedCalls * 5)
}

data class SenderStat(
    val unifiedName: String,
    val incomingCount: Int,
    val msgCount: Int,
    val callCount: Int,
    val incomingWords: Int,
    val avgWordsPerMessage: Int,
    val totalDurationSeconds: Int,
    val callDurationSeconds: Int,
    val appNames: Set<String>,
    val category: ContactCategory,
    val isGroup: Boolean,
    val subSenders: Map<String, Int> = emptyMap()
)

data class AppUsageStat(val appName: String, val totalDurationSeconds: Int, val incomingCount: Int)
data class CategoryStat(val category: ContactCategory, val totalDurationSeconds: Int, val incomingCount: Int)
data class TimeOfDayStat(val period: String, val percentage: Float, val color: Color, val explicitTimeRange: String)

data class SocialPersona(val title: String, val description: String, val emoji: String) {
    companion object {
        val ALL_TYPES = listOf(
            SocialPersona("與世無爭", "保留了能量給自己，享受遠離喧囂的寧靜。\n(無任何互動紀錄)", "🍃"),
            SocialPersona("靜默傾聽者", "花了不少時間在對話中，或許正反覆閱讀或默默陪伴。\n(有較長停留時間但無訊息)", "🦉"),
            SocialPersona("潛水觀察員", "快速滑過對話不留痕跡，偶爾看看大家在聊什麼。\n(極短暫停留且無訊息)", "🤿"),
            SocialPersona("深度交流家", "傾向與他人進行深度的文字或語音交流，而非敷衍回覆。\n(平均每則訊息停留時間極長)", "☕"),
            SocialPersona("訊息機關槍", "訊息來得快回得也快！總能迅速處理大量對話，不拖泥帶水。\n(短時間內接收高頻率訊息)", "⚡"),
            SocialPersona("守護型陪伴者", "回話不多，但總是長時間開著對話框陪伴對方。\n(極長總停留時間但訊息量稀少)", "🛡️"),
            SocialPersona("平衡社交者", "社交節奏拿捏得恰到好處，不冷落他人也不過度消耗自己。\n(適度的停留時間與互動比例)", "⚖️")
        )
    }
}

// ==========================================
// 3. 資料中心 (Repository)
// ==========================================
object DataRepository {
    private val _interactions = MutableStateFlow<List<InteractionRecord>>(emptyList())
    val interactions: StateFlow<List<InteractionRecord>> = _interactions.asStateFlow()

    private val _categories = MutableStateFlow<Map<String, String>>(emptyMap())
    val categories: StateFlow<Map<String, String>> = _categories.asStateFlow()

    private val _aliases = MutableStateFlow<Map<String, String>>(emptyMap())
    val aliases: StateFlow<Map<String, String>> = _aliases.asStateFlow()

    private val _availableCategories = MutableStateFlow(ContactCategory.defaultCategories)
    val availableCategories: StateFlow<List<ContactCategory>> = _availableCategories.asStateFlow()

    var isFirstLaunch = MutableStateFlow(true)
    var weekStartDay = MutableStateFlow(Calendar.MONDAY)

    var autoBackupEnabled = MutableStateFlow(false)
    var autoBackupPassword = MutableStateFlow("")
    var lastAutoBackupTime = MutableStateFlow(0L)

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.getSharedPreferences("RelateStatsData", Context.MODE_PRIVATE)
        loadData()
        performAutoBackupIfNeeded(context)
    }

    fun loadData() {
        isFirstLaunch.value = prefs?.getBoolean("is_first_launch", true) ?: true
        weekStartDay.value = prefs?.getInt("week_start_day", Calendar.MONDAY) ?: Calendar.MONDAY
        autoBackupEnabled.value = prefs?.getBoolean("auto_backup_enabled", false) ?: false
        autoBackupPassword.value = prefs?.getString("auto_backup_pwd", "") ?: ""
        lastAutoBackupTime.value = prefs?.getLong("last_auto_backup_time", 0L) ?: 0L

        val recordsSet = prefs?.getStringSet("interactions", emptySet()) ?: emptySet()
        _interactions.value = recordsSet.mapNotNull { InteractionRecord.fromCsvString(it) }
        val aliasSet = prefs?.getStringSet("aliases", emptySet()) ?: emptySet()
        _aliases.value = aliasSet.associate { it.substringBefore("|=|") to it.substringAfter("|=|") }
        val catSet = prefs?.getStringSet("categories", emptySet()) ?: emptySet()
        _categories.value = catSet.associate { parts -> parts.split("|=|").let { it[0] to it[1] } }
        val customCatsSet = prefs?.getStringSet("custom_categories", emptySet()) ?: emptySet()
        val loadedCustomCats = customCatsSet.mapNotNull { ContactCategory.fromCsvString(it) }
        _availableCategories.value = ContactCategory.defaultCategories + loadedCustomCats
    }

    fun completeOnboarding() {
        isFirstLaunch.value = false
        prefs?.edit()?.putBoolean("is_first_launch", false)?.apply()
    }

    fun setWeekStartDay(day: Int) {
        weekStartDay.value = day
        prefs?.edit()?.putInt("week_start_day", day)?.apply()
    }

    fun setAutoBackup(enabled: Boolean, pwd: String) {
        autoBackupEnabled.value = enabled
        autoBackupPassword.value = pwd
        prefs?.edit()?.apply {
            putBoolean("auto_backup_enabled", enabled)
            putString("auto_backup_pwd", pwd)
            apply()
        }
    }

    private fun saveData() {
        prefs?.edit()?.apply {
            putStringSet("interactions", _interactions.value.map { it.toCsvString() }.toSet())
            putStringSet("aliases", _aliases.value.map { "${it.key}|=|${it.value}" }.toSet())
            putStringSet("categories", _categories.value.map { "${it.key}|=|${it.value}" }.toSet())
            putStringSet("custom_categories", _availableCategories.value.filter { it !in ContactCategory.defaultCategories }.map { it.toCsvString() }.toSet())
            apply()
        }
    }

    fun addInteraction(record: InteractionRecord) {
        _interactions.update { it + record }
        val unifiedName = _aliases.value[record.rawSenderName] ?: record.rawSenderName
        if (!_categories.value.containsKey(unifiedName)) {
            _categories.update { it + (unifiedName to ContactCategory.OTHER.id) }
        }
        saveData()
    }

    fun updateCategory(unifiedName: String, categoryId: String) { _categories.update { it + (unifiedName to categoryId) }; saveData() }
    fun setAlias(rawName: String, unifiedName: String) { _aliases.update { it + (rawName to unifiedName) }; saveData() }
    fun addCustomCategory(cat: ContactCategory) { _availableCategories.update { it + cat }; saveData() }

    fun clearInteractions(scope: String) {
        val now = System.currentTimeMillis()
        val newRecords = when (scope) {
            "TODAY" -> {
                val startOfToday = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                _interactions.value.filter { it.timestamp < startOfToday }
            }
            "WEEK" -> {
                val startOfWeek = getStartOfWeekTimestamp(now, weekStartDay.value)
                _interactions.value.filter { it.timestamp < startOfWeek }
            }
            else -> emptyList()
        }
        _interactions.value = newRecords
        if (scope == "ALL") {
            _aliases.value = emptyMap()
            _categories.value = emptyMap()
        }
        saveData()
    }

    fun clearInteractionsByDate(timestamp: Long) {
        val targetCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val targetYear = targetCal.get(Calendar.YEAR)
        val targetDay = targetCal.get(Calendar.DAY_OF_YEAR)

        _interactions.update { list ->
            list.filterNot { record ->
                val rCal = Calendar.getInstance().apply { timeInMillis = record.timestamp }
                rCal.get(Calendar.YEAR) == targetYear && rCal.get(Calendar.DAY_OF_YEAR) == targetDay
            }
        }
        saveData()
    }

    fun performAutoBackupIfNeeded(context: Context) {
        if (!autoBackupEnabled.value) return
        val now = System.currentTimeMillis()
        if (now - lastAutoBackupTime.value > 86400000L) {
            forceAutoBackup(context)
        }
    }

    fun forceAutoBackup(context: Context) {
        try {
            val backupStr = exportBackupString(autoBackupPassword.value)
            val file = File(context.filesDir, "RelateStats_AutoBackup.txt")
            file.writeText(backupStr)

            val now = System.currentTimeMillis()
            lastAutoBackupTime.value = now
            prefs?.edit()?.putLong("last_auto_backup_time", now)?.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun restoreAutoBackup(context: Context): Boolean {
        try {
            val file = File(context.filesDir, "RelateStats_AutoBackup.txt")
            if (!file.exists()) return false
            val backupStr = file.readText()
            return importBackupString(backupStr, autoBackupPassword.value)
        } catch (e: Exception) {
            return false
        }
    }

    fun exportBackupString(key: String): String {
        val records = _interactions.value.joinToString(";") { it.toCsvString() }
        val aliases = _aliases.value.map { "${it.key}|=|${it.value}" }.joinToString(";")
        val categories = _categories.value.map { "${it.key}|=|${it.value}" }.joinToString(";")
        val customCats = _availableCategories.value.filter { it !in ContactCategory.defaultCategories }.joinToString(";") { it.toCsvString() }
        val combined = "V11||$records||$aliases||$categories||$customCats"

        if (key.isEmpty()) return Base64.encodeToString(combined.toByteArray(), Base64.NO_WRAP)

        val bytes = combined.toByteArray()
        val keyBytes = key.toByteArray()
        for (i in bytes.indices) {
            bytes[i] = (bytes[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
        }
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun importBackupString(backupStr: String, key: String): Boolean {
        try {
            val bytes = Base64.decode(backupStr, Base64.NO_WRAP)
            val decodedStr = if (key.isNotEmpty()) {
                val keyBytes = key.toByteArray()
                if (keyBytes.isEmpty()) return false
                for (i in bytes.indices) {
                    bytes[i] = (bytes[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
                }
                String(bytes)
            } else {
                String(bytes)
            }

            val parts = decodedStr.split("||")
            if (!parts[0].startsWith("V")) return false
            val newRecords = parts[1].split(";").filter { it.isNotBlank() }.mapNotNull { InteractionRecord.fromCsvString(it) }
            val newAliases = parts[2].split(";").filter { it.isNotBlank() }.associate { it.substringBefore("|=|") to it.substringAfter("|=|") }
            val newCategories = parts[3].split(";").filter { it.isNotBlank() }.associate { p -> p.split("|=|").let { it[0] to it[1] } }
            val newCustomCats = parts[4].split(";").filter { it.isNotBlank() }.mapNotNull { ContactCategory.fromCsvString(it) }
            _interactions.value = newRecords; _aliases.value = newAliases; _categories.value = newCategories; _availableCategories.value = ContactCategory.defaultCategories + newCustomCats
            saveData()
            return true
        } catch (_: Exception) { return false }
    }
}

/// ==========================================
// 4. 核心服務
// ==========================================
class RelateNotificationService : NotificationListenerService() {
    private val targetApps = mapOf(
        "jp.naver.line.android" to "LINE", "com.facebook.orca" to "Messenger",
        "com.instagram.android" to "Instagram", "com.whatsapp" to "WhatsApp",
        "com.instagram.barcelona" to "Threads", "org.telegram.messenger" to "Telegram",
        "com.google.android.dialer" to "電話", "com.samsung.android.dialer" to "電話", "com.samsung.android.incallui" to "電話"
    )
    private val ignoreKeywords = listOf(
        "貼圖", "照片", "表情", "收回了", "attachment", "語音訊息", "影片",
        "正在傳送", "傳送中", "正在", "上傳", "sent a", "sending", "串文"
    )

    override fun onCreate() { super.onCreate(); DataRepository.init(applicationContext) }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val appName = targetApps[sbn.packageName] ?: return
        val extras = sbn.notification.extras

        val convTitle = extras.getString("android.conversationTitle")
        val baseTitle = extras.getString("android.title") ?: extras.getString("android.conversationTitle") ?: return

        var senderName: String
        var subSender: String? = null

        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val bigText = extras.getCharSequence("android.bigText")?.toString() ?: ""
        val content = if (bigText.isNotBlank()) bigText else text
        var isGroup = extras.getBoolean("android.isGroup", false)

        if (isGroup && !convTitle.isNullOrBlank()) {
            senderName = convTitle
            subSender = baseTitle
        } else {
            senderName = baseTitle
            subSender = null
        }

        if (appName == "Instagram") {

            if (baseTitle.startsWith("(") && baseTitle.contains(") ")) {
                isGroup = true
                senderName = baseTitle.substringAfter(") ").trim()

                val textStr = text.toString()
                if (textStr.contains(" : ")) {
                    subSender = textStr.substringBefore(" : ").trim()
                } else if (textStr.contains(": ")) {
                    subSender = textStr.substringBefore(": ").trim()
                }
            }
            else if (baseTitle.contains(" : ")) {
                isGroup = false
                senderName = baseTitle.substringAfterLast(" : ").trim()
            }
        }


        if (senderName.isBlank()) return

        var isMissedCall = false
        var isCall = false
        var parsedDurationSecs = 0

        if (appName == "電話" || content.contains("未接來電") || content.contains("取消通話") || senderName.contains("未接來電") || content.contains("通話時間") || content.contains("已通話")) {
            isCall = true
            if (content.contains("未接來電") || content.contains("取消通話") || senderName.contains("未接來電")) {
                isMissedCall = true
            } else {
                val timeMatch = Regex("(?:通話時間\\s*)?(\\d{1,2})[:：分](\\d{2})秒?").find(content)
                if (timeMatch != null) {
                    parsedDurationSecs = (timeMatch.groupValues[1].toInt() * 60) + timeMatch.groupValues[2].toInt()
                }
            }
        }

        if (!isCall && ignoreKeywords.any { content.contains(it) }) return

        val aliasesMap = DataRepository.aliases.value
        val finalSenderName = aliasesMap[senderName.trim()] ?: senderName.trim()
        val finalSubSender = subSender?.trim()?.let { aliasesMap[it] ?: it }

        var finalWordCount = content.length
        if (appName == "Instagram" && isGroup && subSender != null) {
            val prefix1 = "$subSender : "
            val prefix2 = "$subSender: "
            if (content.startsWith(prefix1)) finalWordCount = content.removePrefix(prefix1).length
            else if (content.startsWith(prefix2)) finalWordCount = content.removePrefix(prefix2).length
        }


        DataRepository.addInteraction(InteractionRecord(
            rawSenderName = senderName.trim(),
            appName = appName,
            isIncoming = true,
            wordCount = if (isCall) 0 else content.length,
            durationSeconds = parsedDurationSecs,
            isGroup = isGroup
        ))
    }
}

class ChatTrackerService : AccessibilityService() {
    private var currentChatName: String? = null
    private var currentAppName: String? = null
    private var chatStartTime: Long = 0
    private val targetApps = mapOf(
        "jp.naver.line.android" to "LINE", "com.facebook.orca" to "Messenger", "com.instagram.android" to "Instagram",
        "com.google.android.dialer" to "電話", "com.samsung.android.incallui" to "電話"
    )

    override fun onServiceConnected() { super.onServiceConnected(); DataRepository.init(applicationContext) }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val appName = targetApps[event.packageName?.toString()] ?: return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            val root = rootInActiveWindow ?: return
            val isChatScreen = hasEditText(root) || extractChatName(root) != null || appName == "電話"
            if (!isChatScreen) { leaveChat(); return }
            val chatName = extractChatName(root) ?: event.text.firstOrNull()?.toString()
            val ignoreList = listOf("LINE", "主頁", "聊天", "通話", "Instagram", "Messenger", "搜尋", "觀看完整 Reel", "更多選項", "應用程式圖示", "留言", "分享", "查看洞察報告")
            if (chatName != null && chatName !in ignoreList && chatName.length < 25) {
                enterChat(chatName, appName)
            } else {
                leaveChat()
            }
        }
    }

    private fun hasEditText(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        if (node.isEditable || node.className?.toString()?.contains("EditText") == true) return true
        for (i in 0 until node.childCount) {
            if (hasEditText(node.getChild(i))) return true
        }
        return false
    }

    private fun extractChatName(root: AccessibilityNodeInfo): String? {
        val ids = listOf(
            "jp.naver.line.android:id/header_title",
            "com.facebook.orca:id/thread_title_name",
            "com.instagram.android:id/thread_title",
            "com.instagram.android:id/action_bar_title",
            "com.instagram.android:id/thread_title_textview",
            "com.instagram.android:id/action_bar_textview_title",
            "com.samsung.android.incallui:id/name"
        )
        for (id in ids) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            if (nodes.isNotEmpty() && nodes[0].text != null) return nodes[0].text.toString()
        }
        return null
    }

    private fun enterChat(chatName: String, appName: String) {
        if (currentChatName == chatName) return
        leaveChat()
        currentChatName = chatName; currentAppName = appName; chatStartTime = System.currentTimeMillis()
    }

    private fun leaveChat() {
        if (currentChatName != null && chatStartTime > 0) {
            val durationSecs = ((System.currentTimeMillis() - chatStartTime) / 1000).toInt()
            val safeDurationSecs = durationSecs.coerceAtMost(3600)
            if (safeDurationSecs > 0) {
                val aliasesMap = DataRepository.aliases.value
                val finalSenderName = aliasesMap[currentChatName!!] ?: currentChatName!!

                DataRepository.addInteraction(InteractionRecord(
                    rawSenderName = finalSenderName, appName = currentAppName ?: "Unknown",
                    isIncoming = false, wordCount = 0, durationSeconds = safeDurationSecs
                ))
            }
            currentChatName = null; chatStartTime = 0
        }
    }
    override fun onInterrupt() {}
}

// ==========================================
// 5. ViewModel
// ==========================================
class RelateViewModel : ViewModel() {
    val isFirstLaunch = DataRepository.isFirstLaunch
    val interactions = DataRepository.interactions
    val categories = DataRepository.categories
    val aliases = DataRepository.aliases
    val availableCategories = DataRepository.availableCategories
    val weekStartDay = DataRepository.weekStartDay

    var timeFilter = MutableStateFlow(DateFilter(FilterType.SPECIFIC_DAY, System.currentTimeMillis()))
    var sortType = MutableStateFlow(SortType.TIME)
    var categoryFilter = MutableStateFlow<ContactCategory?>(null)
    var appFilter = MutableStateFlow<String?>(null)

    var contactsSearchQuery = MutableStateFlow("")
    var contactsSortType = MutableStateFlow(ContactSortType.INTERACTIONS)
    var contactsCatFilter = MutableStateFlow<ContactCategory?>(null)

    val autoBackupEnabled = DataRepository.autoBackupEnabled
    val lastAutoBackupTime = DataRepository.lastAutoBackupTime

    fun completeOnboarding() { DataRepository.completeOnboarding() }
    fun setWeekStartDay(day: Int) { DataRepository.setWeekStartDay(day) }
    fun checkAutoBackup(context: Context) { DataRepository.performAutoBackupIfNeeded(context) }
    fun setAutoBackup(enabled: Boolean, pwd: String) { DataRepository.setAutoBackup(enabled, pwd) }
    fun restoreAutoBackup(context: Context): Boolean { return DataRepository.restoreAutoBackup(context) }

    fun getFilteredRecords(records: List<InteractionRecord>, filter: DateFilter): List<InteractionRecord> {
        return when (filter.type) {
            FilterType.SPECIFIC_DAY -> {
                val cal = Calendar.getInstance().apply { timeInMillis = filter.timestamp }
                val start = cal.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
                val end = cal.apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999) }.timeInMillis
                records.filter { it.timestamp in start..end }
            }
            FilterType.WEEK -> {
                val start = getStartOfWeekTimestamp(filter.timestamp, weekStartDay.value)
                val end = getEndOfWeekTimestamp(filter.timestamp, weekStartDay.value)
                records.filter { it.timestamp in start..end }
            }
            FilterType.ALL -> records
        }
    }

    fun getSocialPersona(records: List<InteractionRecord>, filter: DateFilter): SocialPersona {
        val filtered = getFilteredRecords(records, filter)
        if (filtered.isEmpty()) return SocialPersona.ALL_TYPES[0]

        val totalDurSecs = filtered.sumOf { it.durationSeconds }
        val totalMsg = filtered.count { it.isIncoming }

        if (totalMsg == 0 && totalDurSecs > 60) return SocialPersona.ALL_TYPES[1]
        if (totalMsg == 0) return SocialPersona.ALL_TYPES[2]

        val avgSecsPerMsg = if(totalMsg > 0) totalDurSecs / totalMsg else totalDurSecs
        return when {
            avgSecsPerMsg > 120 -> SocialPersona.ALL_TYPES[3]
            totalMsg > 50 && avgSecsPerMsg < 15 -> SocialPersona.ALL_TYPES[4]
            totalDurSecs > 1800 && totalMsg < 20 -> SocialPersona.ALL_TYPES[5]
            else -> SocialPersona.ALL_TYPES[6]
        }
    }

    fun getTimeOfDayStats(records: List<InteractionRecord>, filter: DateFilter): List<TimeOfDayStat> {
        val filtered = getFilteredRecords(records, filter)
        if (filtered.isEmpty()) return emptyList()
        var morning = 0; var afternoon = 0; var night = 0
        filtered.forEach {
            val hour = Calendar.getInstance().apply { timeInMillis = it.timestamp }.get(Calendar.HOUR_OF_DAY)
            when (hour) { in 5..11 -> morning++; in 12..17 -> afternoon++; else -> night++ }
        }
        val total = filtered.size.toFloat()
        return listOf(
            TimeOfDayStat("早晨", morning / total, Color(0xFFFCD34D), "05:00 - 11:59"),
            TimeOfDayStat("下午", afternoon / total, Color(0xFF60A5FA), "12:00 - 17:59"),
            TimeOfDayStat("夜晚", night / total, Color(0xFF818CF8), "18:00 - 04:59")
        ).filter { it.percentage > 0 }
    }

    fun getAppUsageStats(records: List<InteractionRecord>, filter: DateFilter): List<AppUsageStat> {
        return getFilteredRecords(records, filter).groupBy { it.appName }.map { (appName, appRecords) ->
            AppUsageStat(appName = appName, totalDurationSeconds = appRecords.sumOf { it.durationSeconds }, incomingCount = appRecords.count { it.isIncoming })
        }.filter { it.totalDurationSeconds > 0 || it.incomingCount > 0 }.sortedByDescending { it.totalDurationSeconds + (it.incomingCount * 5L) }
    }

    fun getCategoryStats(records: List<InteractionRecord>, filter: DateFilter, catsMap: Map<String, String>, currentAliases: Map<String, String>, availableCats: List<ContactCategory>): List<CategoryStat> {
        return getFilteredRecords(records, filter).groupBy {
            val unifiedName = currentAliases[it.rawSenderName] ?: it.rawSenderName
            val catId = catsMap[unifiedName] ?: ContactCategory.OTHER.id
            availableCats.find { c -> c.id == catId } ?: ContactCategory.OTHER
        }.map { (category, catRecords) ->
            CategoryStat(category = category, totalDurationSeconds = catRecords.sumOf { it.durationSeconds }, incomingCount = catRecords.count { it.isIncoming })
        }.filter { it.totalDurationSeconds > 0 || it.incomingCount > 0 }.sortedByDescending { it.totalDurationSeconds }
    }

    fun getDynamicLeaderboard(baseRecords: List<InteractionRecord>, catsMap: Map<String, String>, currentAliases: Map<String, String>, availableCats: List<ContactCategory>): List<SenderStat> {
        return baseRecords.groupBy { currentAliases[it.rawSenderName] ?: it.rawSenderName }
            .map { (unifiedName, userRecords) ->
                val msgIncoming = userRecords.filter { it.isIncoming && !it.isCall && !it.isMissedCall }
                val calls = userRecords.filter { it.isCall || it.appName == "電話" }
                val avgWords = if (msgIncoming.isNotEmpty()) msgIncoming.sumOf { it.wordCount } / msgIncoming.size else 0
                val catId = catsMap[unifiedName] ?: ContactCategory.OTHER.id
                val categoryObj = availableCats.find { it.id == catId } ?: ContactCategory.OTHER

                val subSenderCounts = msgIncoming.mapNotNull { it.subSender }
                    .filter { it != unifiedName }
                    .groupingBy { it }.eachCount()

                SenderStat(
                    unifiedName = unifiedName, incomingCount = userRecords.size, msgCount = msgIncoming.size, callCount = calls.size,
                    incomingWords = msgIncoming.sumOf { it.wordCount }, avgWordsPerMessage = avgWords,
                    totalDurationSeconds = userRecords.sumOf { it.durationSeconds }, callDurationSeconds = calls.sumOf { it.durationSeconds },
                    appNames = userRecords.map { it.appName }.toSet(), category = categoryObj,
                    isGroup = userRecords.any { it.isGroup }, subSenders = subSenderCounts.toList().sortedByDescending { it.second }.toMap()
                )
            }.filter { it.totalDurationSeconds > 0 || it.incomingCount > 0 }.sortedByDescending { it.totalDurationSeconds + (it.incomingCount * 60L) }
    }
    fun getLeaderboard(records: List<InteractionRecord>, catsMap: Map<String, String>, currentAliases: Map<String, String>, availableCats: List<ContactCategory>, filter: DateFilter, sort: SortType, catFilter: ContactCategory?, appFlt: String?): List<SenderStat> {
        val filteredByTimeAndApp = getFilteredRecords(records, filter).let { if (appFlt != null) it.filter { r -> r.appName == appFlt } else it }
        val baseStats = filteredByTimeAndApp.groupBy { currentAliases[it.rawSenderName] ?: it.rawSenderName }
            .map { (unifiedName, userRecords) ->
                val chats = userRecords.filter { !it.isCall }
                val calls = userRecords.filter { it.isCall || it.appName == "電話" }
                val msgIncoming = chats.filter { it.isIncoming && !it.isMissedCall }

                val avgWords = if (msgIncoming.isNotEmpty()) msgIncoming.sumOf { it.wordCount } / msgIncoming.size else 0
                val catId = catsMap[unifiedName] ?: ContactCategory.OTHER.id
                val categoryObj = availableCats.find { it.id == catId } ?: ContactCategory.OTHER

                val subSenderCounts = msgIncoming.mapNotNull { it.subSender }.groupingBy { it }.eachCount()
                val sortedSubSenders = subSenderCounts.toList().sortedByDescending { it.second }.toMap()

                SenderStat(
                    unifiedName = unifiedName,
                    incomingCount = userRecords.size,
                    msgCount = msgIncoming.size,
                    callCount = calls.size,
                    incomingWords = msgIncoming.sumOf { it.wordCount },
                    avgWordsPerMessage = avgWords,
                    totalDurationSeconds = userRecords.sumOf { it.durationSeconds },
                    callDurationSeconds = calls.sumOf { it.durationSeconds },
                    appNames = userRecords.map { it.appName }.toSet(),
                    category = categoryObj,
                    isGroup = userRecords.any { it.isGroup },
                    subSenders = sortedSubSenders
                )
            }.filter { it.totalDurationSeconds > 0 || it.incomingCount > 0 }
        val filteredByCategory = if (catFilter != null) baseStats.filter { it.category.id == catFilter.id } else baseStats

        return if (sort == SortType.TIME) {
            filteredByCategory.sortedByDescending { it.totalDurationSeconds }
        } else {
            filteredByCategory.sortedByDescending { it.incomingCount }
        }
    }

    fun getHourlyActivityForContact(records: List<InteractionRecord>, aliases: Map<String, String>, unifiedName: String, filter: DateFilter): Array<HourlyStat> {
        val hourlyData = Array(24) { HourlyStat(it) }
        getFilteredRecords(records, filter).filter { (aliases[it.rawSenderName] ?: it.rawSenderName) == unifiedName }.forEach {
            val hour = Calendar.getInstance().apply { timeInMillis = it.timestamp }.get(Calendar.HOUR_OF_DAY)
            hourlyData[hour].durationSeconds += it.durationSeconds
            if (it.isMissedCall) hourlyData[hour].missedCalls += 1
            else if (it.wordCount > 0 || it.isIncoming) hourlyData[hour].msgCount += 1
        }
        return hourlyData
    }

    fun getMissedCallsForContact(records: List<InteractionRecord>, aliases: Map<String, String>, unifiedName: String, filter: DateFilter): Int {
        return getFilteredRecords(records, filter).count { (aliases[it.rawSenderName] ?: it.rawSenderName) == unifiedName && it.isMissedCall }
    }

    fun addNewCategory(label: String, targetUnifiedName: String? = null) {
        val randomPair = AppColors.CustomPalette.random()
        val newCat = ContactCategory(id = UUID.randomUUID().toString(), label = label, colorInt = randomPair.first.toArgb(), bgColorInt = randomPair.second.toArgb())
        DataRepository.addCustomCategory(newCat)
        if (targetUnifiedName != null) DataRepository.updateCategory(targetUnifiedName, newCat.id)
    }

    fun setCategory(name: String, categoryId: String) { DataRepository.updateCategory(name, categoryId) }
    fun setTimeFilter(filter: DateFilter) { timeFilter.value = filter }
    fun setSortType(type: SortType) { sortType.value = type }
    fun setCategoryFilter(cat: ContactCategory?) { categoryFilter.value = cat }
    fun setAppFilter(app: String?) { appFilter.value = app }
    fun setContactsSearch(q: String) { contactsSearchQuery.value = q }
    fun setContactsSort(type: ContactSortType) { contactsSortType.value = type }
    fun setContactsCatFilter(cat: ContactCategory?) { contactsCatFilter.value = cat }
    fun forceRefresh() { DataRepository.loadData() }
    fun clearInteractions(scope: String) { DataRepository.clearInteractions(scope) }
    fun clearInteractionsByDate(timestamp: Long) { DataRepository.clearInteractionsByDate(timestamp) }
    fun mergeContact(rawName: String, unifiedName: String) { DataRepository.setAlias(rawName, unifiedName) }
    fun exportBackup(key: String) = DataRepository.exportBackupString(key)
    fun importBackup(str: String, key: String) = DataRepository.importBackupString(str, key)
}

fun getDeviceUsageTimeSec(context: Context, filter: DateFilter, weekStartDay: Int): Int {
    if (!hasUsageStatsPermission(context)) return 0
    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val (start, end) = when (filter.type) {
        FilterType.SPECIFIC_DAY -> {
            val c = Calendar.getInstance().apply { timeInMillis = filter.timestamp }
            c.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis to c.apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999) }.timeInMillis
        }
        FilterType.WEEK -> {
            val s = getStartOfWeekTimestamp(filter.timestamp, weekStartDay)
            val e = getEndOfWeekTimestamp(filter.timestamp, weekStartDay)
            s to e
        }
        FilterType.ALL -> return 0
    }
    val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
    return (stats?.sumOf { it.totalTimeInForeground } ?: 0L / 1000L).toInt()
}

fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
    return mode == AppOpsManager.MODE_ALLOWED
}

// ==========================================
// 6. UI 視圖層
// ==========================================
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.navigationBarColor = android.graphics.Color.WHITE
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        DataRepository.init(applicationContext)
        setContent {
            MaterialTheme {
                Surface(color = AppColors.Background, modifier = Modifier.fillMaxSize()) {
                    RelateStatsApp()
                }
            }
        }
    }
}

@Composable
fun RelateStatsApp(viewModel: RelateViewModel = viewModel()) {
    val context = LocalContext.current
    val isFirstLaunch by viewModel.isFirstLaunch.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkAutoBackup(context)
    }

    if (isFirstLaunch) {
        OnboardingScreen(onFinish = { viewModel.completeOnboarding() })
    } else {
        MainAppContent(viewModel)
    }
}

@Composable
fun DemoDashboardScreen(onClose: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(AppColors.Background).systemBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("👀 App 視覺預覽", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = AppColors.TextPrimary)
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, contentDescription = "關閉", tint = AppColors.TextPrimary) }
        }

        // 🏆 防閃退：明確宣告 weight(1f) 佔滿剩餘空間，避免無限高度計算
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.weight(1f)) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = AppColors.BrandPrimary), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("你的社交人格解讀", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("☕", fontSize = 28.sp, modifier = Modifier.padding(end=8.dp))
                            Text("深度交流家", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        }
                        Text("傾向與他人進行深度的文字或語音交流，而非敷衍回覆。\n(平均每則訊息停留時間極長)", color = Color.White.copy(alpha=0.9f), fontSize = 13.sp, modifier = Modifier.padding(top=8.dp), lineHeight = 18.sp)
                    }
                }
            }

            item { Text("排行榜範例", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary, modifier = Modifier.padding(top = 8.dp)) }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = AppColors.Surface), border = BorderStroke(1.dp, AppColors.BorderLight), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)) {
                                Box(modifier = Modifier.background(AppColors.FamilyBg, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) { Text("家人", color = AppColors.FamilyText, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("老媽", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text("2h 15m", fontSize = 18.sp, fontWeight = FontWeight.Black, color = AppColors.TextPrimary, maxLines = 1, softWrap = false)
                        }
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(AppColors.BorderLight)) {
                            Box(modifier = Modifier.fillMaxWidth(0.9f).fillMaxHeight().background(AppColors.ActiveBlue))
                        }
                        Column {
                            Text("💬 45 則訊息 | 共 120 字 (平均 2 字)", fontSize = 12.sp, color = AppColors.TextPrimary, modifier = Modifier.padding(bottom = 4.dp))
                            Text("📞 2 通電話 | 總時長 45 分 30 秒", fontSize = 12.sp, color = AppColors.WorkText, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                        }
                    }
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = AppColors.Surface), border = BorderStroke(1.dp, AppColors.BorderLight), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)) {
                                Box(modifier = Modifier.background(AppColors.GroupBg, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) { Text("群組", color = AppColors.GroupText, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("大學死黨群", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text("1h 05m", fontSize = 18.sp, fontWeight = FontWeight.Black, color = AppColors.TextPrimary, maxLines = 1, softWrap = false)
                        }
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(AppColors.BorderLight)) {
                            Box(modifier = Modifier.fillMaxWidth(0.5f).fillMaxHeight().background(AppColors.ActiveBlue))
                        }
                        Text("💬 120 則訊息 | 共 650 字 (平均 5 字)", fontSize = 12.sp, color = AppColors.TextPrimary, modifier = Modifier.padding(bottom = 4.dp))
                    }
                }
            }

            item {
                Button(onClick = onClose, modifier = Modifier.fillMaxWidth().padding(top = 16.dp), colors = ButtonDefaults.buttonColors(containerColor = AppColors.BrandPrimary), shape = RoundedCornerShape(12.dp)) {
                    Text("結束預覽並開始使用", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val (step, setStep) = remember { mutableStateOf(0) }
    val (showDemo, setShowDemo) = remember { mutableStateOf(false) }

    if (showDemo) {
        Dialog(onDismissRequest = { setShowDemo(false) }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            DemoDashboardScreen(onClose = { setShowDemo(false); onFinish() })
        }
        return
    }

    val titles = listOf("洞察社交能量", "隱私與權限設定", "四大分頁解析")
    val descs = listOf(
        "透過精準的數據追蹤，了解你每天把時間和注意力花在哪些人、哪些 App 上。",
        "為確保精準度，我們需要「通知存取」與「無障礙服務」權限。所有數據皆完全存放在您的手機本地，絕不上傳。",
        "【總覽】查看個人性格與數位健康\n【排行】精準抓出時間黑洞與分析熱區\n【聯絡人】自訂群組、合併跨平台帳號\n【設定】自訂備份資料與設定"
    )
    val icons = listOf(Icons.Rounded.Face, Icons.Rounded.Lock, Icons.AutoMirrored.Rounded.List)

    Column(modifier = Modifier.fillMaxSize().background(AppColors.Background).systemBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onFinish) { Text("跳過", color = AppColors.TextSecondary, fontWeight = FontWeight.Bold) }
        }

        Column(modifier = Modifier.weight(1f).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            AnimatedContent(targetState = step, label = "onboarding") { currentStep ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(120.dp).background(AppColors.BrandPrimary.copy(alpha=0.05f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(icons[currentStep], contentDescription = null, modifier = Modifier.size(60.dp), tint = AppColors.BrandPrimary)
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(titles[currentStep], fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(descs[currentStep], fontSize = 15.sp, color = AppColors.TextSecondary, textAlign = TextAlign.Center, lineHeight = 24.sp)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row {
                for (i in 0..2) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (i == step) AppColors.BrandPrimary else AppColors.BorderLight))
                    if(i < 2) Spacer(modifier = Modifier.width(8.dp))
                }
            }
            if (step < 2) {
                Button(onClick = { setStep(step + 1) }, colors = ButtonDefaults.buttonColors(containerColor = AppColors.BrandPrimary), shape = RoundedCornerShape(12.dp)) {
                    Text("下一步", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            } else {
                Button(onClick = { setShowDemo(true) }, colors = ButtonDefaults.buttonColors(containerColor = AppColors.ActiveBlue), shape = RoundedCornerShape(12.dp)) {
                    Text("觀看排版範例", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: RelateViewModel) {
    val (activeTab, setActiveTab) = remember { mutableStateOf("overview") }

    Scaffold(
        containerColor = AppColors.Background,
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().background(AppColors.Surface).statusBarsPadding().padding(top = 8.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("RelateStats", color = AppColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)
                }
                HorizontalDivider(color = AppColors.BorderLight)
            }
        },
        bottomBar = {
            NavigationBar(containerColor = AppColors.Surface, tonalElevation = 8.dp) {
                NavigationBarItem(selected = activeTab == "overview", onClick = { setActiveTab("overview") }, icon = { Icon(Icons.Rounded.Home, "總覽") }, label = { Text("總覽") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = AppColors.ActiveBlue, selectedTextColor = AppColors.ActiveBlue))
                NavigationBarItem(selected = activeTab == "leaderboard", onClick = { setActiveTab("leaderboard") }, icon = { Icon(Icons.AutoMirrored.Rounded.List, "排行") }, label = { Text("排行") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = AppColors.ActiveBlue, selectedTextColor = AppColors.ActiveBlue))
                NavigationBarItem(selected = activeTab == "contacts", onClick = { setActiveTab("contacts") }, icon = { Icon(Icons.Rounded.Person, "聯絡人") }, label = { Text("聯絡人") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = AppColors.ActiveBlue, selectedTextColor = AppColors.ActiveBlue))
                NavigationBarItem(selected = activeTab == "settings", onClick = { setActiveTab("settings") }, icon = { Icon(Icons.Rounded.Settings, "設定") }, label = { Text("設定") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = AppColors.ActiveBlue, selectedTextColor = AppColors.ActiveBlue))
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (activeTab) {
                "overview" -> OverviewView(viewModel)
                "leaderboard" -> LeaderboardView(viewModel)
                "contacts" -> ContactsCategoryView(viewModel)
                "settings" -> SettingsView(viewModel)
            }
        }
    }
}

@Composable
fun MinimalCalendar(selectedTime: Long, onDateSelected: (Long) -> Unit) {
    val (currentMonthCal, setCurrentMonthCal) = remember(selectedTime) { mutableStateOf(Calendar.getInstance().apply { timeInMillis = selectedTime }) }

    val year = currentMonthCal.get(Calendar.YEAR)
    val month = currentMonthCal.get(Calendar.MONTH)

    val firstDayCal = Calendar.getInstance().apply { set(year, month, 1) }
    val firstDayOfWeek = firstDayCal.get(Calendar.DAY_OF_WEEK) - 1
    val daysInMonth = firstDayCal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val weekdays = listOf("日", "一", "二", "三", "四", "五", "六")

    Column(modifier = Modifier.fillMaxWidth().background(AppColors.Surface, RoundedCornerShape(16.dp)).border(1.dp, AppColors.BorderLight, RoundedCornerShape(16.dp)).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { setCurrentMonthCal(Calendar.getInstance().apply { timeInMillis = currentMonthCal.timeInMillis; add(Calendar.MONTH, -1) }) }) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "上個月", tint = AppColors.TextPrimary)
            }
            Text("${year}年 ${month + 1}月", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AppColors.TextPrimary)
            IconButton(onClick = { setCurrentMonthCal(Calendar.getInstance().apply { timeInMillis = currentMonthCal.timeInMillis; add(Calendar.MONTH, 1) }) }) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "下個月", tint = AppColors.TextPrimary)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            weekdays.forEach { day -> Text(day, fontSize = 12.sp, color = AppColors.TextSecondary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center) }
        }
        Spacer(modifier = Modifier.height(8.dp))

        val totalCells = firstDayOfWeek + daysInMonth
        val rows = Math.ceil(totalCells / 7.0).toInt()

        for (r in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceAround) {
                for (c in 0 until 7) {
                    val index = r * 7 + c
                    val dayNumber = index - firstDayOfWeek + 1
                    if (index < firstDayOfWeek || dayNumber > daysInMonth) {
                        Box(modifier = Modifier.weight(1f))
                    } else {
                        val calCheck = Calendar.getInstance().apply { timeInMillis = selectedTime }
                        val isSelected = calCheck.get(Calendar.YEAR) == year && calCheck.get(Calendar.MONTH) == month && calCheck.get(Calendar.DAY_OF_MONTH) == dayNumber
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) AppColors.ActiveBlue else Color.Transparent)
                                .clickable {
                                    val cal = Calendar.getInstance().apply { set(year, month, dayNumber) }
                                    onDateSelected(cal.timeInMillis)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = dayNumber.toString(), color = if (isSelected) Color.White else AppColors.TextPrimary, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimeFilterSelector(currentFilter: DateFilter, onFilterChange: (DateFilter) -> Unit, weekStartDay: Int) {
    val (showCalendar, setShowCalendar) = remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().background(AppColors.BorderLight.copy(alpha=0.5f), RoundedCornerShape(12.dp)).padding(4.dp)) {
            val types = listOf(FilterType.SPECIFIC_DAY to "單日", FilterType.WEEK to "單週", FilterType.ALL to "全部")
            types.forEach { (type, label) ->
                val isSelected = currentFilter.type == type
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (isSelected) AppColors.Surface else Color.Transparent)
                    .clickable {
                        onFilterChange(DateFilter(type, System.currentTimeMillis()))
                        setShowCalendar(false)
                    }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Text(label, color = if (isSelected) AppColors.TextPrimary else AppColors.TextSecondary, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                }
            }
        }

        if (currentFilter.type != FilterType.ALL) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    val offset = if(currentFilter.type == FilterType.SPECIFIC_DAY) -86400000L else -7 * 86400000L
                    onFilterChange(currentFilter.copy(timestamp = currentFilter.timestamp + offset))
                }, modifier = Modifier.background(AppColors.Surface, CircleShape).size(36.dp).border(1.dp, AppColors.BorderLight, CircleShape)) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "上一筆", tint = AppColors.TextPrimary)
                }

                Row(modifier = Modifier.clickable { setShowCalendar(!showCalendar) }.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.DateRange, contentDescription = null, modifier = Modifier.size(16.dp), tint = AppColors.ActiveBlue)
                    Spacer(modifier = Modifier.width(6.dp))

                    val dateText = if (currentFilter.type == FilterType.SPECIFIC_DAY) {
                        val cal = Calendar.getInstance().apply { timeInMillis = currentFilter.timestamp }
                        val isToday = DateFilter(FilterType.SPECIFIC_DAY, System.currentTimeMillis()).isSameDay(currentFilter.timestamp)
                        if (isToday) "今日" else "${cal.get(Calendar.MONTH) + 1}月${cal.get(Calendar.DAY_OF_MONTH)}日"
                    } else {
                        val startMs = getStartOfWeekTimestamp(currentFilter.timestamp, weekStartDay)
                        val endMs = getEndOfWeekTimestamp(currentFilter.timestamp, weekStartDay)
                        val startCal = Calendar.getInstance().apply { timeInMillis = startMs }
                        val endCal = Calendar.getInstance().apply { timeInMillis = endMs }

                        val currentStartMs = getStartOfWeekTimestamp(System.currentTimeMillis(), weekStartDay)
                        if (startMs == currentStartMs) "本週" else "${startCal.get(Calendar.MONTH) + 1}/${startCal.get(Calendar.DAY_OF_MONTH)} - ${endCal.get(Calendar.MONTH) + 1}/${endCal.get(Calendar.DAY_OF_MONTH)}"
                    }

                    Text(dateText, color = AppColors.ActiveBlue, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = AppColors.ActiveBlue)
                }

                IconButton(onClick = {
                    val offset = if(currentFilter.type == FilterType.SPECIFIC_DAY) 86400000L else 7 * 86400000L
                    onFilterChange(currentFilter.copy(timestamp = currentFilter.timestamp + offset))
                }, modifier = Modifier.background(AppColors.Surface, CircleShape).size(36.dp).border(1.dp, AppColors.BorderLight, CircleShape)) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "下一筆", tint = AppColors.TextPrimary)
                }
            }
        }

        AnimatedVisibility(visible = showCalendar, enter = expandVertically(), exit = shrinkVertically()) {
            Box(modifier = Modifier.padding(top = 12.dp)) {
                MinimalCalendar(selectedTime = currentFilter.timestamp, onDateSelected = {
                    onFilterChange(currentFilter.copy(timestamp = it))
                    setShowCalendar(false)
                })
            }
        }
    }
}

@Composable
fun OverviewDetailDialog(title: String, stats: List<SenderStat>, onDismiss: () -> Unit) {
    AlertDialog(
        containerColor = AppColors.Surface, onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            if (stats.isEmpty()) {
                Text("此條件下沒有互動數據", color = AppColors.TextSecondary, modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(stats) { stat ->
                        Column(modifier = Modifier.fillMaxWidth().background(AppColors.Background, RoundedCornerShape(8.dp)).padding(12.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(stat.unifiedName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    val mins = stat.totalDurationSeconds / 60
                                    val secs = stat.totalDurationSeconds % 60
                                    Text("💬 ${stat.msgCount} 則 | 📞 ${stat.callCount} 通 | ⏳ $mins 分 $secs 秒", fontSize = 11.sp, color = AppColors.TextSecondary, modifier = Modifier.padding(top=2.dp))
                                }
                                Box(modifier = Modifier.background(stat.category.bgColor, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                    Text(stat.category.label, color = stat.category.color, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }

                            // 🏆 在熱點分析內加入群組長條圖排行榜
                            if (stat.isGroup && stat.subSenders.isNotEmpty()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = AppColors.BorderLight)
                                Text("群組發言排行 (Top 3)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AppColors.TextSecondary, modifier = Modifier.padding(bottom = 4.dp))

                                val maxCount = stat.subSenders.values.maxOrNull()?.toFloat()?.coerceAtLeast(1f) ?: 1f
                                stat.subSenders.entries.sortedByDescending { it.value }.take(3).forEach { (name, count) ->
                                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(name, fontSize = 11.sp, color = AppColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(end = 8.dp))
                                            Text("$count 則", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppColors.ActiveBlue, maxLines = 1, softWrap = false)
                                        }
                                        Box(modifier = Modifier.fillMaxWidth().height(4.dp).padding(top = 2.dp).background(AppColors.BorderLight, RoundedCornerShape(2.dp))) {
                                            Box(modifier = Modifier.fillMaxWidth(count / maxCount).fillMaxHeight().background(AppColors.ActiveBlue, RoundedCornerShape(2.dp)))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = AppColors.BrandPrimary)) { Text("關閉") } }
    )
}

@Composable
fun OverviewView(viewModel: RelateViewModel) {
    val records by viewModel.interactions.collectAsState()
    val categoriesMap by viewModel.categories.collectAsState()
    val aliases by viewModel.aliases.collectAsState()
    val availableCats by viewModel.availableCategories.collectAsState()
    val filter by viewModel.timeFilter.collectAsState()
    val weekStartDay by viewModel.weekStartDay.collectAsState()

    val filteredRecords = remember(records, filter, weekStartDay) { viewModel.getFilteredRecords(records, filter) }
    val appStats = remember(filteredRecords) { viewModel.getAppUsageStats(filteredRecords, DateFilter(FilterType.ALL)) }
    val timeStats = remember(filteredRecords) { viewModel.getTimeOfDayStats(filteredRecords, DateFilter(FilterType.ALL)) }
    val persona = remember(filteredRecords) { viewModel.getSocialPersona(filteredRecords, DateFilter(FilterType.ALL)) }

    val leaderboard = remember(filteredRecords, categoriesMap, aliases, availableCats) { viewModel.getDynamicLeaderboard(filteredRecords, categoriesMap, aliases, availableCats) }
    val topTimeDrainer = leaderboard.maxByOrNull { it.totalDurationSeconds }

    val (showPersonaInfo, setShowPersonaInfo) = remember { mutableStateOf(false) }
    val (activeDetailDialog, setActiveDetailDialog) = remember { mutableStateOf<Pair<String, List<SenderStat>>?>(null) }
    val (showDemoUI, setShowDemoUI) = remember { mutableStateOf(false) }

    if (showDemoUI) {
        Dialog(onDismissRequest = { setShowDemoUI(false) }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            DemoDashboardScreen(onClose = { setShowDemoUI(false) })
        }
        return
    }

    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {

        item { TimeFilterSelector(filter, viewModel::setTimeFilter, weekStartDay) }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = AppColors.BrandPrimary), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("你的社交人格解讀", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Icon(Icons.Rounded.Info, contentDescription = "查看圖鑑", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(16.dp).clickable { setShowPersonaInfo(true) })
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(persona.emoji, fontSize = 28.sp, modifier = Modifier.padding(end=8.dp))
                        Text(persona.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    }
                    Text(persona.description.split("\n")[0], color = Color.White.copy(alpha=0.9f), fontSize = 13.sp, modifier = Modifier.padding(top=8.dp), lineHeight = 18.sp)
                }
            }
        }

        if (appStats.isEmpty() && timeStats.isEmpty() && topTimeDrainer == null) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.DateRange, contentDescription = null, modifier = Modifier.size(64.dp), tint = AppColors.BorderLight)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("這段期間沒有任何社交紀錄", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary, textAlign = TextAlign.Center)
                    Text("你可以保持背景執行以開始紀錄", fontSize = 13.sp, color = AppColors.TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp, bottom = 24.dp), lineHeight = 20.sp)

                    Button(onClick = { setShowDemoUI(true) }, colors = ButtonDefaults.buttonColors(containerColor = AppColors.ActiveBlue), shape = RoundedCornerShape(12.dp)) {
                        Text("觀看版面與功能範例", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }
        } else {
            if (appStats.isNotEmpty()) {
                item {
                    val (viewMode, setViewMode) = remember { mutableStateOf("TIME") }
                    val context = LocalContext.current
                    val totalTimeSec = appStats.sumOf { it.totalDurationSeconds }
                    val totalNotifs = appStats.sumOf { it.incomingCount }
                    val deviceTotalSecs = remember(filter, weekStartDay) { getDeviceUsageTimeSec(context, filter, weekStartDay) }
                    val screenPct = if (deviceTotalSecs > 0 && totalTimeSec > 0) ((totalTimeSec.toFloat() / deviceTotalSecs.toFloat()) * 100).toInt().coerceIn(0, 100) else 0

                    Card(colors = CardDefaults.cardColors(containerColor = AppColors.Surface), border = BorderStroke(1.dp, AppColors.BorderLight), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("📱 平台數位健康", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                                Row(modifier = Modifier.background(AppColors.BorderLight, RoundedCornerShape(8.dp)).padding(2.dp)) {
                                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if(viewMode=="TIME") AppColors.Surface else Color.Transparent).clickable{setViewMode("TIME")}.padding(horizontal=10.dp, vertical=4.dp)){ Text("時長", fontSize=11.sp, fontWeight=if(viewMode=="TIME") FontWeight.Bold else FontWeight.Normal) }
                                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if(viewMode=="NOTIF") AppColors.Surface else Color.Transparent).clickable{setViewMode("NOTIF")}.padding(horizontal=10.dp, vertical=4.dp)){ Text("通知", fontSize=11.sp, fontWeight=if(viewMode=="NOTIF") FontWeight.Bold else FontWeight.Normal) }
                                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if(viewMode=="PERIOD") AppColors.Surface else Color.Transparent).clickable{setViewMode("PERIOD")}.padding(horizontal=10.dp, vertical=4.dp)){ Text("時段", fontSize=11.sp, fontWeight=if(viewMode=="PERIOD") FontWeight.Bold else FontWeight.Normal) }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            if (viewMode == "TIME") {
                                val hrs = totalTimeSec / 3600
                                val mins = (totalTimeSec % 3600) / 60
                                val secs = totalTimeSec % 60
                                Row(verticalAlignment = Alignment.Bottom) {
                                    if (hrs > 0) {
                                        Text("$hrs", fontSize = 36.sp, fontWeight = FontWeight.Black, color = AppColors.BrandPrimary)
                                        Text("小時", fontSize = 14.sp, color = AppColors.TextSecondary, modifier = Modifier.padding(start=2.dp, end=6.dp, bottom=6.dp))
                                    }
                                    if (mins > 0 || hrs > 0) {
                                        Text("$mins", fontSize = 36.sp, fontWeight = FontWeight.Black, color = AppColors.BrandPrimary)
                                        Text("分鐘", fontSize = 14.sp, color = AppColors.TextSecondary, modifier = Modifier.padding(start=2.dp, end=6.dp, bottom=6.dp))
                                    }
                                    Text("$secs", fontSize = 36.sp, fontWeight = FontWeight.Black, color = AppColors.BrandPrimary)
                                    Text("秒", fontSize = 14.sp, color = AppColors.TextSecondary, modifier = Modifier.padding(start=2.dp, bottom=6.dp))
                                }
                                if (screenPct > 0) {
                                    Text("通訊軟體佔據您本期手機總使用的 $screenPct%", fontSize = 12.sp, color = AppColors.ActiveBlue, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top=2.dp))
                                } else if (!hasUsageStatsPermission(context)) {
                                    Text("前往「設定」開啟權限即可查看佔比分析", fontSize = 12.sp, color = AppColors.TextSecondary, modifier = Modifier.padding(top=2.dp).clickable { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) })
                                }
                            } else if (viewMode == "NOTIF") {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text("$totalNotifs", fontSize = 36.sp, fontWeight = FontWeight.Black, color = AppColors.BrandPrimary)
                                    Text("則通知", fontSize = 14.sp, color = AppColors.TextSecondary, modifier = Modifier.padding(start=4.dp, bottom=6.dp))
                                }
                            } else {
                                Text("社交活躍分佈", fontSize = 24.sp, fontWeight = FontWeight.Black, color = AppColors.BrandPrimary)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (viewMode == "PERIOD" && timeStats.isNotEmpty()) {
                                Row(modifier = Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp))) {
                                    timeStats.forEach { stat ->
                                        val weight = stat.percentage.coerceAtLeast(0.01f)
                                        Box(modifier = Modifier.weight(weight).fillMaxHeight().background(stat.color).border(0.5.dp, AppColors.Surface).clickable {
                                            val periodName = stat.period.split(" ")[0]
                                            val periodRecords = filteredRecords.filter {
                                                val h = Calendar.getInstance().apply{timeInMillis=it.timestamp}.get(Calendar.HOUR_OF_DAY)
                                                when(periodName) { "早晨"-> h in 5..11; "下午"-> h in 12..17; else -> h !in 5..17 }
                                            }
                                            setActiveDetailDialog(Pair("📅 $periodName 活躍對象排行", viewModel.getDynamicLeaderboard(periodRecords, categoriesMap, aliases, availableCats)))
                                        })
                                    }
                                }
                            } else {
                                val totalValue = if (viewMode == "TIME") totalTimeSec.toFloat() else totalNotifs.toFloat()
                                if (totalValue > 0) {
                                    Row(modifier = Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp))) {
                                        appStats.forEach { stat ->
                                            val value = if (viewMode == "TIME") stat.totalDurationSeconds.toFloat() else stat.incomingCount.toFloat()
                                            val weight = (value / totalValue).coerceAtLeast(0.001f)
                                            val brandColor = AppColors.AppBrands[stat.appName] ?: AppColors.TextSecondary
                                            Box(modifier = Modifier.weight(weight).fillMaxHeight().background(brandColor).border(0.5.dp, AppColors.Surface).clickable {
                                                val appRecords = filteredRecords.filter { it.appName == stat.appName }
                                                setActiveDetailDialog(Pair("📱 ${stat.appName} 互動對象排行", viewModel.getDynamicLeaderboard(appRecords, categoriesMap, aliases, availableCats)))
                                            })
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (viewMode == "PERIOD" && timeStats.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    timeStats.forEach { stat ->
                                        val progress by animateFloatAsState(targetValue = stat.percentage, animationSpec = tween(1000, easing = FastOutSlowInEasing), label = "")
                                        val periodName = stat.period.split(" ")[0]
                                        Row(modifier = Modifier.fillMaxWidth().clickable {
                                            val periodRecords = filteredRecords.filter {
                                                val h = Calendar.getInstance().apply{timeInMillis=it.timestamp}.get(Calendar.HOUR_OF_DAY)
                                                when(periodName) { "早晨"-> h in 5..11; "下午"-> h in 12..17; else -> h !in 5..17 }
                                            }
                                            setActiveDetailDialog(Pair("📅 $periodName 活躍對象排行", viewModel.getDynamicLeaderboard(periodRecords, categoriesMap, aliases, availableCats)))
                                        }, verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(stat.color))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.width(70.dp)) {
                                                Text(periodName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                                                Text(stat.explicitTimeRange, fontSize = 9.sp, color = AppColors.TextSecondary)
                                            }
                                            Box(modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(AppColors.Background)) {
                                                Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().background(stat.color))
                                            }
                                            Text("${(stat.percentage * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = AppColors.TextSecondary, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                                        }
                                    }
                                }
                            } else {
                                val totalValue = if (viewMode == "TIME") totalTimeSec.toFloat() else totalNotifs.toFloat()
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    appStats.forEach { stat ->
                                        val value = if (viewMode == "TIME") stat.totalDurationSeconds.toFloat() else stat.incomingCount.toFloat()
                                        val targetProgress = if(totalValue > 0) value / totalValue else 0f
                                        val progress by animateFloatAsState(targetValue = targetProgress, animationSpec = tween(1000, easing = FastOutSlowInEasing), label = "")
                                        val brandColor = AppColors.AppBrands[stat.appName] ?: AppColors.TextSecondary
                                        val valText = if (viewMode == "TIME") formatDuration(stat.totalDurationSeconds) else "${stat.incomingCount} 則"

                                        Row(modifier = Modifier.fillMaxWidth().clickable {
                                            val appRecords = filteredRecords.filter { it.appName == stat.appName }
                                            setActiveDetailDialog(Pair("📱 ${stat.appName} 互動對象排行", viewModel.getDynamicLeaderboard(appRecords, categoriesMap, aliases, availableCats)))
                                        }, verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(brandColor))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(stat.appName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary, modifier = Modifier.weight(1f).padding(end = 8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)

                                            Box(modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(AppColors.Background)) {
                                                Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().background(brandColor))
                                            }
                                            Text(valText, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = AppColors.TextSecondary, modifier = Modifier.width(60.dp), textAlign = TextAlign.End)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val catStats = viewModel.getCategoryStats(filteredRecords, DateFilter(FilterType.ALL), categoriesMap, aliases, availableCats)
            if (catStats.isNotEmpty()) {
                item {
                    val (showTime, setShowTime) = remember { mutableStateOf(true) }
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("標籤互動分析", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                        Row(modifier = Modifier.background(AppColors.BorderLight, RoundedCornerShape(8.dp)).padding(2.dp)) {
                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if(showTime) AppColors.Surface else Color.Transparent).clickable{setShowTime(true)}.padding(horizontal=10.dp, vertical=4.dp)){ Text("投入時間", fontSize=11.sp, fontWeight=if(showTime) FontWeight.Bold else FontWeight.Normal) }
                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if(!showTime) AppColors.Surface else Color.Transparent).clickable{setShowTime(false)}.padding(horizontal=10.dp, vertical=4.dp)){ Text("接收訊息", fontSize=11.sp, fontWeight=if(!showTime) FontWeight.Bold else FontWeight.Normal) }
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val sortedStats = if(showTime) catStats.sortedByDescending { it.totalDurationSeconds } else catStats.sortedByDescending { it.incomingCount }
                        items(sortedStats) { stat ->
                            Box(modifier = Modifier.clickable {
                                val catRecords = filteredRecords.filter { r ->
                                    val uName = aliases[r.rawSenderName] ?: r.rawSenderName
                                    val cId = categoriesMap[uName] ?: ContactCategory.OTHER.id
                                    cId == stat.category.id
                                }
                                setActiveDetailDialog(Pair("🏷️ ${stat.category.label} 標籤內排行", viewModel.getDynamicLeaderboard(catRecords, categoriesMap, aliases, availableCats)))
                            }) {
                                CatStatCard(stat, showTime)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPersonaInfo) {
        AlertDialog(
            containerColor = AppColors.Surface, onDismissRequest = { setShowPersonaInfo(false) },
            title = { Text("社交人格全圖鑑", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.TextPrimary) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(SocialPersona.ALL_TYPES) { type ->
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(type.emoji, fontSize = 20.sp, modifier = Modifier.padding(end=8.dp))
                                Text(type.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AppColors.TextPrimary)
                            }
                            Text(type.description, fontSize = 13.sp, color = AppColors.TextSecondary, modifier = Modifier.padding(top=4.dp), lineHeight = 18.sp)
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = { setShowPersonaInfo(false) }, colors = ButtonDefaults.buttonColors(containerColor = AppColors.ActiveBlue)) { Text("關閉") } }
        )
    }

    if (activeDetailDialog != null) {
        OverviewDetailDialog(
            title = activeDetailDialog!!.first,
            stats = activeDetailDialog!!.second,
            onDismiss = { setActiveDetailDialog(null) }
        )
    }
}

@Composable
fun CatStatCard(stat: CategoryStat, showTime: Boolean) {
    val valText = if (showTime) formatDuration(stat.totalDurationSeconds) else "${stat.incomingCount} 則"

    Card(colors = CardDefaults.cardColors(containerColor = stat.category.bgColor), border = BorderStroke(1.dp, stat.category.color.copy(alpha=0.3f)), shape = RoundedCornerShape(16.dp), modifier = Modifier.width(130.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stat.category.label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = stat.category.color, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(8.dp))
            Text(valText, fontSize = 18.sp, fontWeight = FontWeight.Black, color = AppColors.TextPrimary)
            Text(if(showTime) "總計投入時間" else "總接收訊息量", fontSize = 11.sp, color = AppColors.TextSecondary, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
fun ContactDetailDialog(stat: SenderStat, hourlyData: Array<HourlyStat>, missedCalls: Int, onDismiss: () -> Unit) {
    val (selectedHour, setSelectedHour) = remember { mutableStateOf<Int?>(null) }
    val maxAct = hourlyData.maxOfOrNull { it.score }?.coerceAtLeast(1) ?: 1
    val avgAct = hourlyData.map { it.score }.average().toFloat()

    AlertDialog(
        containerColor = AppColors.Surface, onDismissRequest = onDismiss,
        title = {
            Column {
                Text("24 小時活躍分析", fontSize = 14.sp, color = AppColors.TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.background(stat.category.bgColor, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text(stat.category.label, color = stat.category.color, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stat.unifiedName, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (missedCalls > 0) {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).background(AppColors.WorkBg, RoundedCornerShape(8.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Warning, contentDescription = null, tint = AppColors.WorkText, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("本期共有 $missedCalls 通未接來電", color = AppColors.WorkText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().height(64.dp).background(AppColors.Background, RoundedCornerShape(8.dp)).padding(10.dp)) {
                    if (selectedHour != null) {
                        val hStat = hourlyData[selectedHour!!]
                        val min = hStat.durationSeconds / 60
                        val sec = hStat.durationSeconds % 60
                        Column {
                            Text("📍 ${String.format(Locale.US, "%02d:00", hStat.hour)} - ${String.format(Locale.US, "%02d:59", hStat.hour)} (指數: ${hStat.score})", color = AppColors.ActiveBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("💬 互動 ${hStat.msgCount} 則 | ⏳ 停留通話 ${min}分 ${sec}秒", color = AppColors.TextSecondary, fontSize = 11.sp)
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("👆 點擊下方長條圖查看具體時段數據", color = AppColors.TextSecondary, fontSize = 12.sp)
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().height(110.dp).padding(top = 12.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val yPos = size.height - (avgAct / maxAct) * size.height
                        drawLine(
                            color = Color.Red.copy(alpha = 0.5f), start = Offset(0f, yPos), end = Offset(size.width, yPos),
                            strokeWidth = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                        )
                    }

                    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        for (hour in 0..23) {
                            val value = hourlyData[hour].score
                            val heightPct = (value.toFloat() / maxAct.toFloat()).coerceIn(0.02f, 1f)
                            val barColor = if(selectedHour == hour) AppColors.BrandPrimary else if(value > 0) AppColors.ActiveBlue else AppColors.BorderLight

                            Column(modifier = Modifier.weight(1f).fillMaxHeight().clickable { setSelectedHour(hour) }, verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(modifier = Modifier.fillMaxWidth(0.8f).fillMaxHeight(heightPct).background(barColor, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)))
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf(0, 6, 12, 18, 23).forEach { Text(String.format(Locale.US, "%02d", it), fontSize = 10.sp, color = AppColors.TextSecondary) }
                }
                Text("＊活躍指數 = 訊息數 + 停留分鐘數(每秒也會算)", fontSize = 9.sp, color = AppColors.TextSecondary, modifier = Modifier.padding(top = 8.dp))

                // 🏆 優化：群組成員發言次數長條圖
                if (stat.isGroup && stat.subSenders.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("👥 群組成員發言排行", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary, modifier = Modifier.padding(bottom = 8.dp))

                    val maxSubCount = stat.subSenders.values.maxOrNull()?.toFloat()?.coerceAtLeast(1f) ?: 1f

                    LazyColumn(modifier = Modifier.heightIn(max = 160.dp).fillMaxWidth().background(AppColors.Background, RoundedCornerShape(8.dp)).padding(8.dp)) {
                        items(stat.subSenders.toList().sortedByDescending { it.second }) { (name, count) ->
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(name, fontSize = 12.sp, color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(end = 8.dp))
                                    Text("$count 則", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppColors.ActiveBlue, maxLines = 1, softWrap = false)
                                }
                                Box(modifier = Modifier.fillMaxWidth().height(4.dp).padding(top = 2.dp).background(AppColors.BorderLight, RoundedCornerShape(2.dp))) {
                                    Box(modifier = Modifier.fillMaxWidth(count / maxSubCount).fillMaxHeight().background(AppColors.ActiveBlue, RoundedCornerShape(2.dp)))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = AppColors.BrandPrimary)) { Text("關閉") } }
    )
}

@Composable
fun LeaderboardView(viewModel: RelateViewModel) {
    val records by viewModel.interactions.collectAsState()
    val categoriesMap by viewModel.categories.collectAsState()
    val aliases by viewModel.aliases.collectAsState()
    val availableCats by viewModel.availableCategories.collectAsState()
    val filter by viewModel.timeFilter.collectAsState()
    val sort by viewModel.sortType.collectAsState()
    val catFilter by viewModel.categoryFilter.collectAsState()
    val appFilter by viewModel.appFilter.collectAsState()
    val weekStartDay by viewModel.weekStartDay.collectAsState()

    val (isRefreshing, setIsRefreshing) = remember { mutableStateOf(false) }
    val (selectedContact, setSelectedContact) = remember { mutableStateOf<SenderStat?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val leaderboard = remember(records, categoriesMap, aliases, availableCats, filter, sort, catFilter, appFilter, weekStartDay) {
        viewModel.getLeaderboard(records, categoriesMap, aliases, availableCats, filter, sort, catFilter, appFilter)
    }
    val availableApps = remember(records) { records.map { it.appName }.distinct().sorted() }
    val maxScore = if (sort == SortType.TIME) {
        leaderboard.maxOfOrNull { it.totalDurationSeconds }?.toFloat()?.coerceAtLeast(1f) ?: 1f
    } else {
        leaderboard.maxOfOrNull { it.incomingCount }?.toFloat()?.coerceAtLeast(1f) ?: 1f
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize()) {

            item {
                Column(modifier = Modifier.fillMaxWidth().background(AppColors.Surface).padding(bottom = 8.dp)) {
                    TimeFilterSelector(filter, viewModel::setTimeFilter, weekStartDay)
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        val isSelected = appFilter == null
                        Box(modifier = Modifier.background(if (isSelected) AppColors.BrandPrimary else AppColors.Background, RoundedCornerShape(20.dp)).clickable { viewModel.setAppFilter(null) }.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            Text("全平台", color = if (isSelected) Color.White else AppColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    items(availableApps) { app ->
                        val isSelected = appFilter == app
                        val brandColor = AppColors.AppBrands[app] ?: AppColors.BrandPrimary
                        Box(modifier = Modifier.background(if (isSelected) brandColor else AppColors.Background, RoundedCornerShape(20.dp)).clickable { viewModel.setAppFilter(app) }.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            Text(app, color = if (isSelected) Color.White else AppColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        val isSelected = catFilter == null
                        Box(modifier = Modifier.border(1.dp, if (isSelected) AppColors.BrandPrimary else AppColors.BorderLight, RoundedCornerShape(20.dp)).background(if (isSelected) AppColors.BrandPrimary else Color.Transparent, RoundedCornerShape(20.dp)).clickable { viewModel.setCategoryFilter(null) }.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            Text("全標籤", color = if (isSelected) Color.White else AppColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    items(availableCats) { cat ->
                        val isSelected = catFilter?.id == cat.id
                        Box(modifier = Modifier.border(1.dp, if (isSelected) cat.color else AppColors.BorderLight, RoundedCornerShape(20.dp)).background(if (isSelected) cat.color else Color.Transparent, RoundedCornerShape(20.dp)).clickable { viewModel.setCategoryFilter(cat) }.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            Text(cat.label, color = if (isSelected) Color.White else cat.color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("詳細排行數據", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary, modifier = Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.wrapContentWidth()) {
                        Surface(color = AppColors.Surface, border = BorderStroke(1.dp, AppColors.BorderLight), shape = RoundedCornerShape(12.dp), modifier = Modifier.clickable {
                            coroutineScope.launch { setIsRefreshing(true); viewModel.forceRefresh(); delay(500); setIsRefreshing(false) }
                        }) {
                            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (isRefreshing) AppColors.ActiveBlue else AppColors.TextPrimary)
                            }
                        }
                        Surface(color = AppColors.BorderLight, shape = RoundedCornerShape(12.dp), modifier = Modifier.clickable { viewModel.setSortType(if (sort == SortType.TIME) SortType.MESSAGES else SortType.TIME) }) {
                            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(16.dp), tint = AppColors.TextPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("依${sort.label}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                            }
                        }
                    }
                }
            }

            if (leaderboard.isEmpty()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.DateRange, contentDescription = null, modifier = Modifier.size(48.dp), tint = AppColors.BorderLight)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("沒有符合條件的排行數據", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppColors.TextSecondary, textAlign = TextAlign.Center)

                        val (showDemoUI, setShowDemoUI) = remember { mutableStateOf(false) }
                        if (showDemoUI) {
                            Dialog(onDismissRequest = { setShowDemoUI(false) }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                                DemoDashboardScreen(onClose = { setShowDemoUI(false) })
                            }
                        }

                        Button(onClick = { setShowDemoUI(true) }, colors = ButtonDefaults.buttonColors(containerColor = AppColors.ActiveBlue), shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(top = 16.dp)) {
                            Text("觀看排版範例", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }
            } else {
                items(leaderboard, key = { it.unifiedName }) { stat ->
                    Box(modifier = Modifier.clickable { setSelectedContact(stat) }) {
                        StatCardDetailed(stat, maxScore, sort)
                    }
                }
            }
        }
    }

    if (selectedContact != null) {
        val hourlyData = viewModel.getHourlyActivityForContact(records, aliases, selectedContact!!.unifiedName, filter)
        val missedCalls = viewModel.getMissedCallsForContact(records, aliases, selectedContact!!.unifiedName, filter)
        ContactDetailDialog(stat = selectedContact!!, hourlyData = hourlyData, missedCalls = missedCalls) {
            setSelectedContact(null)
        }
    }
}

// 🏆 防跑版最佳化：為 appNames 加入 LazyRow 滾動支援
@Composable
fun StatCardDetailed(stat: SenderStat, maxScore: Float, sortType: SortType) {
    val currentScore = if (sortType == SortType.TIME) stat.totalDurationSeconds.toFloat() else stat.incomingCount.toFloat()
    val progress by animateFloatAsState(targetValue = currentScore / maxScore, animationSpec = tween(800, easing = FastOutSlowInEasing), label = "")

    Card(colors = CardDefaults.cardColors(containerColor = AppColors.Surface), border = BorderStroke(1.dp, AppColors.BorderLight), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)) {
                    Box(modifier = Modifier.background(if(stat.isGroup) AppColors.GroupBg else stat.category.bgColor, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text(if (stat.isGroup) "群組" else stat.category.label, color = if(stat.isGroup) AppColors.GroupText else stat.category.color, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stat.unifiedName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.wrapContentWidth()) {
                    Text(formatDuration(stat.totalDurationSeconds), fontSize = 18.sp, fontWeight = FontWeight.Black, color = AppColors.TextPrimary, maxLines = 1, softWrap = false)
                    Text("總停留時間", fontSize = 10.sp, color = AppColors.TextSecondary)
                }
            }
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(AppColors.BorderLight)) {
                Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().background(AppColors.ActiveBlue))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    if (stat.msgCount > 0 || stat.callCount == 0) {
                        Text("💬 ${stat.msgCount} 則訊息 | 共 ${stat.incomingWords} 字 (平均 ${stat.avgWordsPerMessage} 字)", fontSize = 12.sp, color = AppColors.TextPrimary, modifier = Modifier.padding(bottom = 4.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (stat.callCount > 0) {
                        Text("📞 ${stat.callCount} 通電話 | 總時長 ${formatDuration(stat.callDurationSeconds)}", fontSize = 12.sp, color = AppColors.WorkText, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text("點擊卡片查看活躍時段", fontSize = 11.sp, color = AppColors.ActiveBlue)
                }

                // 🏆 橫向滾動保護機制：避免多個 App 標籤擠爆螢幕
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.wrapContentWidth(Alignment.End).horizontalScroll(rememberScrollState())) {
                    stat.appNames.forEach { app ->
                        Text(app, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AppColors.TextSecondary, maxLines = 1, softWrap = false, modifier = Modifier.border(1.dp, AppColors.BorderLight, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ContactsCategoryView(viewModel: RelateViewModel) {
    val records by viewModel.interactions.collectAsState()
    val categoriesMap by viewModel.categories.collectAsState()
    val aliases by viewModel.aliases.collectAsState()
    val availableCats by viewModel.availableCategories.collectAsState()

    val searchQuery by viewModel.contactsSearchQuery.collectAsState()
    val sortType by viewModel.contactsSortType.collectAsState()
    val catFilter by viewModel.contactsCatFilter.collectAsState()

    val uniqueRawNames = records.map { it.rawSenderName }.distinct()
    val unifiedNamesList = aliases.values.plus(uniqueRawNames).distinct()

    val filteredContacts = unifiedNamesList.filter { name ->
        val matchSearch = name.contains(searchQuery, ignoreCase = true)
        val catId = categoriesMap[name] ?: ContactCategory.OTHER.id
        val matchCat = catFilter == null || catId == catFilter?.id
        matchSearch && matchCat
    }.map { name ->
        val interactionsCount = records.count { (aliases[it.rawSenderName] ?: it.rawSenderName) == name }
        Pair(name, interactionsCount)
    }.let { list ->
        if (sortType == ContactSortType.NAME) list.sortedBy { it.first } else list.sortedByDescending { it.second }
    }.map { it.first }

    val (showAddCategoryDialog, setShowAddCategoryDialog) = remember { mutableStateOf(false) }
    val (newCategoryName, setNewCategoryName) = remember { mutableStateOf("") }
    val (pendingAssignName, setPendingAssignName) = remember { mutableStateOf<String?>(null) }

    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("聯絡人整合與標籤", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = AppColors.TextPrimary, modifier = Modifier.padding(top = 8.dp))
            Text("將跨平台的同一個人整合，並自由建立專屬標籤", fontSize = 14.sp, color = AppColors.TextSecondary, modifier = Modifier.padding(bottom = 16.dp))
        }

        item {
            Column(modifier = Modifier.fillMaxWidth().background(AppColors.Surface, RoundedCornerShape(16.dp)).border(1.dp, AppColors.BorderLight, RoundedCornerShape(16.dp)).padding(16.dp)) {
                OutlinedTextField(
                    value = searchQuery, onValueChange = viewModel::setContactsSearch,
                    placeholder = { Text("搜尋名字...", fontSize = 14.sp) }, singleLine = true,
                    textStyle = TextStyle(fontSize = 14.sp),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppColors.BrandPrimary, unfocusedBorderColor = AppColors.BorderLight)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(modifier = Modifier.background(AppColors.BorderLight, RoundedCornerShape(8.dp)).padding(2.dp)) {
                        ContactSortType.entries.forEach { type ->
                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if(sortType==type) AppColors.Surface else Color.Transparent).clickable{viewModel.setContactsSort(type)}.padding(horizontal=10.dp, vertical=4.dp)){ Text(type.label, fontSize=11.sp, fontWeight=if(sortType==type) FontWeight.Bold else FontWeight.Normal) }
                        }
                    }
                    var showCatDropdown by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { showCatDropdown = true }) { Text(catFilter?.label ?: "全標籤", color=AppColors.ActiveBlue, fontWeight=FontWeight.Bold, fontSize=12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        DropdownMenu(expanded = showCatDropdown, onDismissRequest = { showCatDropdown = false }, modifier = Modifier.background(AppColors.Surface)) {
                            DropdownMenuItem(text = { Text("全標籤", fontWeight = FontWeight.Bold) }, onClick = { viewModel.setContactsCatFilter(null); showCatDropdown = false })
                            availableCats.forEach { cat -> DropdownMenuItem(text = { Text(cat.label, color = cat.color, fontWeight = FontWeight.Bold) }, onClick = { viewModel.setContactsCatFilter(cat); showCatDropdown = false }) }
                        }
                    }
                }
            }
        }

        if (filteredContacts.isEmpty()) {
            item { Text("沒有符合的聯絡人", color = AppColors.TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) }
        }

        items(filteredContacts) { unifiedName ->
            val catId = categoriesMap[unifiedName] ?: ContactCategory.OTHER.id
            val currentCategory = availableCats.find { it.id == catId } ?: ContactCategory.OTHER
            val (expandedCat, setExpandedCat) = remember { mutableStateOf(false) }
            val (expandedMerge, setExpandedMerge) = remember { mutableStateOf(false) }

            val rawNamesForThis = uniqueRawNames.filter { (aliases[it] ?: it) == unifiedName }

            Card(colors = CardDefaults.cardColors(containerColor = AppColors.Surface), border = BorderStroke(1.dp, AppColors.BorderLight), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(unifiedName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (rawNamesForThis.isNotEmpty() && rawNamesForThis.first() != unifiedName) {
                                Text("包含: ${rawNamesForThis.joinToString()}", fontSize = 11.sp, color = AppColors.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Box {
                            TextButton(onClick = { setExpandedCat(true) }, modifier = Modifier.background(currentCategory.bgColor, RoundedCornerShape(8.dp))) {
                                Text(currentCategory.label, color = currentCategory.color, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            DropdownMenu(expanded = expandedCat, onDismissRequest = { setExpandedCat(false) }, modifier = Modifier.background(AppColors.Surface)) {
                                availableCats.forEach { category ->
                                    DropdownMenuItem(text = { Text(category.label, color = category.color, fontWeight = FontWeight.Bold) }, onClick = { viewModel.setCategory(unifiedName, category.id); setExpandedCat(false) })
                                }
                                HorizontalDivider(color = AppColors.BorderLight)
                                DropdownMenuItem(text = { Text("+ 新增自訂分類", color = AppColors.ActiveBlue, fontWeight = FontWeight.Bold) }, onClick = { setExpandedCat(false); setPendingAssignName(unifiedName); setShowAddCategoryDialog(true) })
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = AppColors.BorderLight)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth().clickable { setExpandedMerge(true) }.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = AppColors.ActiveBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("綁定其他帳號 (點擊合併)", fontSize = 12.sp, color = AppColors.ActiveBlue, fontWeight = FontWeight.Medium)
                        }
                        DropdownMenu(expanded = expandedMerge, onDismissRequest = { setExpandedMerge(false) }, modifier = Modifier.background(AppColors.Surface).fillMaxWidth(0.8f)) {
                            Text("選擇要合併的對象：", fontSize = 12.sp, color = AppColors.TextSecondary, modifier = Modifier.padding(8.dp))
                            unifiedNamesList.filter { it != unifiedName }.forEach { targetName ->
                                DropdownMenuItem(text = { Text("合併為「$targetName」", fontWeight = FontWeight.Bold) }, onClick = { viewModel.mergeContact(unifiedName, targetName); setExpandedMerge(false) })
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddCategoryDialog) {
        AlertDialog(
            containerColor = AppColors.Surface, onDismissRequest = { setShowAddCategoryDialog(false); setNewCategoryName("") },
            title = { Text("建立專屬分類標籤", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column {
                    Text("請輸入新分類名稱：", fontSize = 12.sp, color = AppColors.TextSecondary, modifier = Modifier.padding(bottom = 8.dp))
                    OutlinedTextField(value = newCategoryName, onValueChange = { setNewCategoryName(it) }, singleLine = true, placeholder = { Text("例: 健身房...", fontSize = 14.sp) }, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppColors.BrandPrimary, unfocusedBorderColor = AppColors.BorderLight), shape = RoundedCornerShape(12.dp))
                }
            },
            confirmButton = { Button(onClick = { if (newCategoryName.isNotBlank()) { viewModel.addNewCategory(newCategoryName.trim(), pendingAssignName); setShowAddCategoryDialog(false); setNewCategoryName("") } }, colors = ButtonDefaults.buttonColors(containerColor = AppColors.BrandPrimary)) { Text("建立並套用") } },
            dismissButton = { TextButton(onClick = { setShowAddCategoryDialog(false); setNewCategoryName("") }) { Text("取消", color = AppColors.TextSecondary) } }
        )
    }
}

@Composable
fun AdvancedDataManagerDialog(viewModel: RelateViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var pendingClearScope by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        AlertDialog(
            containerColor = AppColors.Surface, onDismissRequest = { showDatePicker = false },
            title = { Text("請選擇要清除的日期", fontWeight = FontWeight.Bold) },
            text = {
                Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                    MinimalCalendar(selectedTime = System.currentTimeMillis(), onDateSelected = { timestamp ->
                        viewModel.clearInteractionsByDate(timestamp)
                        Toast.makeText(context, "已清除該日資料", Toast.LENGTH_SHORT).show()
                        showDatePicker = false
                    })
                }
            },
            confirmButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消", color = AppColors.TextSecondary) } }
        )
    } else if (pendingClearScope != null) {
        val label = when(pendingClearScope) { "TODAY" -> "今日"; "WEEK" -> "本週"; else -> "所有歷史" }
        AlertDialog(
            containerColor = AppColors.Surface, onDismissRequest = { pendingClearScope = null },
            title = { Text("確認清除 $label 資料？", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            text = { Text("清除後，系統將無法回朔您在該時段內的通訊紀錄。\n此操作無法復原。") },
            confirmButton = {
                Button(onClick = {
                    viewModel.clearInteractions(pendingClearScope!!)
                    Toast.makeText(context, "已成功清除", Toast.LENGTH_SHORT).show()
                    pendingClearScope = null
                    onDismiss()
                }, colors = ButtonDefaults.buttonColors(containerColor = AppColors.WorkText)) { Text("確認清除", maxLines = 1) }
            },
            dismissButton = { TextButton(onClick = { pendingClearScope = null }) { Text("取消", color = AppColors.TextSecondary) } }
        )
    } else {
        AlertDialog(
            containerColor = AppColors.Surface, onDismissRequest = onDismiss,
            title = { Text("進階資料管理 🗑️", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("您可以針對特定時段進行淨化，或徹底重置所有標籤與連線資料。", fontSize = 13.sp, color = AppColors.TextSecondary, modifier = Modifier.padding(bottom = 8.dp))

                    Card(colors = CardDefaults.cardColors(containerColor = AppColors.Background), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().clickable { pendingClearScope = "TODAY" }) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🗑️ 清除今日資料", fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                        }
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = AppColors.Background), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().clickable { pendingClearScope = "WEEK" }) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🗑️ 清除本週資料", fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                        }
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = AppColors.Background), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("📅 清除特定日期資料", fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                        }
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = AppColors.WorkBg.copy(alpha=0.5f)), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().clickable { pendingClearScope = "ALL" }) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("⚠️ 徹底清除並重置所有資料", fontWeight = FontWeight.Bold, color = AppColors.WorkText)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("關閉", color = AppColors.TextSecondary) } }
        )
    }
}

@Composable
fun SettingsView(viewModel: RelateViewModel) {
    val context = LocalContext.current
    val (hasNotif, setHasNotif) = remember { mutableStateOf(isNotificationServiceEnabled(context)) }
    val (hasAccel, setHasAccel) = remember { mutableStateOf(isAccessibilityServiceEnabled(context, ChatTrackerService::class.java)) }
    val (hasUsageStats, setHasUsageStats) = remember { mutableStateOf(hasUsageStatsPermission(context)) }

    val (showDataManager, setShowDataManager) = remember { mutableStateOf(false) }
    val (showGuideDialog, setShowGuideDialog) = remember { mutableStateOf(false) }
    val weekStartDay by viewModel.weekStartDay.collectAsState()

    LaunchedEffect(Unit) {
        setHasNotif(isNotificationServiceEnabled(context))
        setHasAccel(isAccessibilityServiceEnabled(context, ChatTrackerService::class.java))
        setHasUsageStats(hasUsageStatsPermission(context))
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(viewModel.exportBackup("").toByteArray())
            }
            Toast.makeText(context, "已成功匯出備份檔案！", Toast.LENGTH_SHORT).show()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            if (content != null && viewModel.importBackup(content, "")) {
                Toast.makeText(context, "資料還原成功！", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "檔案無效或格式錯誤", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("設定與服務", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = AppColors.TextPrimary, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)) }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = AppColors.ActiveBlue.copy(0.1f)), border = BorderStroke(1.dp, AppColors.ActiveBlue.copy(0.2f)), shape = RoundedCornerShape(16.dp), modifier = Modifier.clickable { setShowGuideDialog(true) }) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Info, contentDescription = null, tint = AppColors.ActiveBlue)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("App 使用說明與核心邏輯", color = AppColors.ActiveBlue, fontWeight = FontWeight.Bold)
                        Text("瞭解如何發揮最大效用", color = AppColors.ActiveBlue.copy(0.8f), fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = AppColors.Surface), border = BorderStroke(1.dp, AppColors.BorderLight), shape = RoundedCornerShape(16.dp)) {
                Column {
                    PermissionRow("通知存取 (計算字數與訊息量)", hasNotif) { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                    HorizontalDivider(color = AppColors.BorderLight)
                    PermissionRow("無障礙服務 (計算真實停留時間)", hasAccel) { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                    HorizontalDivider(color = AppColors.BorderLight)
                    Column {
                        PermissionRow("使用紀錄存取 (計算手機總用量)", hasUsageStats) { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
                        if(!hasUsageStats) { Text("＊若找不到此 App，請至系統「應用程式」內找到本 App，點擊右上角「允許受限制的設定」", fontSize = 10.sp, color = AppColors.WorkText, modifier = Modifier.padding(start=16.dp, bottom=12.dp, end=16.dp)) }
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = AppColors.Surface), border = BorderStroke(1.dp, AppColors.BorderLight), shape = RoundedCornerShape(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("自訂一週起始日", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Row(modifier = Modifier.background(AppColors.Background, RoundedCornerShape(8.dp)).padding(2.dp)) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if(weekStartDay == Calendar.MONDAY) AppColors.BrandPrimary else Color.Transparent).clickable{ viewModel.setWeekStartDay(Calendar.MONDAY) }.padding(horizontal=10.dp, vertical=6.dp)){
                            Text("週一", fontSize=12.sp, color=if(weekStartDay == Calendar.MONDAY) Color.White else AppColors.TextSecondary, fontWeight=if(weekStartDay == Calendar.MONDAY) FontWeight.Bold else FontWeight.Normal)
                        }
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if(weekStartDay == Calendar.SUNDAY) AppColors.BrandPrimary else Color.Transparent).clickable{ viewModel.setWeekStartDay(Calendar.SUNDAY) }.padding(horizontal=10.dp, vertical=6.dp)){
                            Text("週日", fontSize=12.sp, color=if(weekStartDay == Calendar.SUNDAY) Color.White else AppColors.TextSecondary, fontWeight=if(weekStartDay == Calendar.SUNDAY) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = AppColors.Surface), border = BorderStroke(1.dp, AppColors.BorderLight), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("雲端硬碟 / 本機備份", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Icon(Icons.Rounded.Lock, contentDescription = null, tint = AppColors.TextSecondary)
                    }
                    Text("點擊匯出後，在系統選單左上角可直接選擇存入「Google 雲端硬碟」，換手機時一鍵選取即可無痛還原。", fontSize = 12.sp, color = AppColors.TextSecondary, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { exportLauncher.launch("RelateStats_Backup.txt") }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = AppColors.BrandPrimary)) { Text("匯出至手機或雲端", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        Button(onClick = { importLauncher.launch(arrayOf("text/plain")) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = AppColors.ActiveBlue)) { Text("選擇檔案還原", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(64.dp)) }

        item {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("⚠️ 進階資料管理 (清除舊資料)", color = AppColors.TextSecondary.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { setShowDataManager(true) }.padding(16.dp))
            }
        }
    }

    if (showGuideDialog) {
        AlertDialog(
            containerColor = AppColors.Surface, onDismissRequest = { setShowGuideDialog(false) },
            title = { Text("📖 App 核心運作邏輯", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.height(350.dp).padding(vertical = 8.dp)) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item { Text("1. 接收訊息怎麼算？", fontWeight = FontWeight.Bold, color = AppColors.TextPrimary) }
                        item { Text("依賴「通知存取」權限。只要對方傳訊息來跳出通知，App 就會在背景攔截，計算字數，並自動切除 IG/LINE 煩人的系統標籤。", fontSize = 13.sp, color = AppColors.TextSecondary) }
                        item { Text("2. 在聊天室內沒跳通知怎麼辦？", fontWeight = FontWeight.Bold, color = AppColors.TextPrimary) }
                        item { Text("依賴「無障礙服務」。只要你點進對方的聊天畫面，系統就會默默啟動『精確碼錶』。當你跳出畫面，這段期間就會被精準算入『注意力停留時間』。", fontSize = 13.sp, color = AppColors.ActiveBlue) }
                        item { Text("3. 為什麼有些數據沒出來？", fontWeight = FontWeight.Bold, color = AppColors.TextPrimary) }
                        item { Text("請確保已開啟設定頁的三項權限，並保持 App 允許在背景執行。通話時間則是在掛斷後的通知中自動抓取。", fontSize = 13.sp, color = AppColors.TextSecondary) }
                        item { Text("4. 時間區段如何定義？", fontWeight = FontWeight.Bold, color = AppColors.TextPrimary) }
                        item { Text("早晨為 05:00-11:59，下午為 12:00-17:59，夜晚則是 18:00-04:59。", fontSize = 13.sp, color = AppColors.TextSecondary) }
                    }
                }
            },
            confirmButton = { Button(onClick = { setShowGuideDialog(false) }, colors = ButtonDefaults.buttonColors(containerColor = AppColors.ActiveBlue)) { Text("我知道了") } }
        )
    }
}

@Composable
fun PermissionRow(title: String, isGranted: Boolean, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontWeight = FontWeight.Medium, color = AppColors.TextPrimary)
        Text(if (isGranted) "已開啟" else "去設定", color = if (isGranted) AppColors.FriendText else AppColors.WorkText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

fun isNotificationServiceEnabled(context: Context): Boolean {
    val pkgName = context.packageName
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat?.contains(pkgName) == true
}

fun isAccessibilityServiceEnabled(context: Context, serviceClass: Class<*>): Boolean {
    val expectedComponentName = ComponentName(context, serviceClass)
    val enabledServicesSetting = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
    val colonSplitter = TextUtils.SimpleStringSplitter(':')
    colonSplitter.setString(enabledServicesSetting)
    while (colonSplitter.hasNext()) {
        val componentNameString = colonSplitter.next()
        val enabledService = ComponentName.unflattenFromString(componentNameString)
        if (enabledService != null && enabledService == expectedComponentName) return true
    }
    return false
}