package com.example.relatestats

import android.accessibilityservice.AccessibilityService
import android.app.AppOpsManager
import android.app.Notification
import com.example.relatestats.BuildConfig
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.text.SimpleDateFormat
import android.app.usage.UsageStatsManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Process
import android.Manifest
import android.content.pm.PackageManager
import android.provider.CallLog
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
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.foundation.verticalScroll
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

// ==========================================
// 1. 視覺設計系統
// ==========================================
object AppColors {
    // ── 底層 ──
    val Background = Color(0xFFF4F6F8)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceAlt = Color(0xFFF7F9FB)   // 表格斑馬紋、次級區塊
    val BorderLight = Color(0xFFE4E8ED)
    val BorderStrong = Color(0xFFCBD5E1)

    // ── 文字階層 ──
    val TextPrimary = Color(0xFF0F172A)
    val TextSecondary = Color(0xFF64748B)
    val TextTertiary = Color(0xFF94A3B8)

    // ── 強調 ──
    val BrandPrimary = Color(0xFF0F172A)
    val ActiveBlue = Color(0xFF2563EB)
    val Positive = Color(0xFF059669)

    val WorkBg = Color(0xFFFEE2E2); val WorkText = Color(0xFFB91C1C)
    val FamilyBg = Color(0xFFDBEAFE); val FamilyText = Color(0xFF1D4ED8)
    val FriendBg = Color(0xFFD1FAE5); val FriendText = Color(0xFF047857)
    val OtherBg = Color(0xFFF3F4F6); val OtherText = Color(0xFF4B5563)
    val GroupBg = Color(0xFFFEF3C7); val GroupText = Color(0xFFB45309)
    val PartnerBg = Color(0xFFFCE7F3); val PartnerText = Color(0xFFBE185D)

    val AppBrands = mapOf(
        "LINE" to Color(0xFF06C755), "Instagram" to Color(0xFFE1306C),
        "Messenger" to Color(0xFF00B2FF), "WhatsApp" to Color(0xFF25D366),
        "Threads" to Color(0xFF000000), "Telegram" to Color(0xFF24A1DE),
        "電話" to Color(0xFF4ADE80)
    )

    // 標籤可挑選的配色（文字色, 底色）
    val CategoryPalette: List<Pair<Color, Color>> get() = listOf(
        FamilyText to FamilyBg, PartnerText to PartnerBg, FriendText to FriendBg,
        WorkText to WorkBg, GroupText to GroupBg, OtherText to OtherBg
    ) + CustomPalette

    val CustomPalette = listOf(
        Pair(Color(0xFF9333EA), Color(0xFFF3E8FF)), Pair(Color(0xFFC026D3), Color(0xFFFAE8FF)),
        Pair(Color(0xFFE11D48), Color(0xFFFFE4E6)), Pair(Color(0xFF0D9488), Color(0xFFCCFBF1)),
        Pair(Color(0xFF0284C7), Color(0xFFE0F2FE)), Pair(Color(0xFF4F46E5), Color(0xFFE0E7FF)),
        Pair(Color(0xFF059669), Color(0xFFD1FAE5))
    )
}

/** 字級與間距代幣。儀錶板風格：小字、緊湊、階層靠字重與顏色區分而非放大字體。 */
object T {
    val Kpi = 22.sp        // KPI 主數字
    val KpiSm = 17.sp      // 次級數字
    val Title = 14.sp      // 區塊標題
    val Body = 13.sp
    val Label = 11.sp
    val Micro = 9.sp
}

object D {
    val Gutter = 12.dp     // 頁面左右內距
    val Gap = 8.dp         // 區塊間距
    val CardPad = 12.dp    // 卡片內距
    val Radius = 12.dp
    val RowH = 40.dp       // 表格列高
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
        // IG 讚／追蹤者／串文推播等非真人通知統一歸到這裡，排行預設不顯示
        val SYSTEM = ContactCategory("SYSTEM", "系統通知", AppColors.GroupText.toArgb(), AppColors.GroupBg.toArgb())
        val PARTNER = ContactCategory("PARTNER", "伴侶", AppColors.PartnerText.toArgb(), AppColors.PartnerBg.toArgb())

        // 首次啟動的預設清單。名稱與顏色之後都可以在「標籤管理」裡改，
        // 但 OTHER / SYSTEM 這兩個 id 程式內部會用到，不可刪除。
        val seedCategories = listOf(FAMILY, PARTNER, FRIEND, WORK, OTHER, SYSTEM)
        val PROTECTED_IDS = setOf(OTHER.id, SYSTEM.id)

        /** 確保 OTHER / SYSTEM 一定存在 */
        fun ensureEssential(list: List<ContactCategory>): List<ContactCategory> {
            val out = list.distinctBy { it.id }.toMutableList()
            if (out.none { it.id == OTHER.id }) out.add(OTHER)
            if (out.none { it.id == SYSTEM.id }) out.add(SYSTEM)
            return out
        }
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
enum class GroupFilter(val label: String) { ALL("全部"), PERSON("個人"), GROUP("群組") }

/** 去掉 emoji、標點、空白後的比對用名稱 */
fun normalizeContactName(raw: String): String {
    val sb = StringBuilder()
    raw.forEach { c ->
        when {
            c.code in 0x4e00..0x9fa5 -> sb.append(c)
            c.code < 0x2000 && c.isLetterOrDigit() -> sb.append(c.lowercaseChar())
        }
    }
    return sb.toString()
}

private val NON_PERSON_PATTERNS = listOf(
    Regex("^\\d+\\+?\\s*(位|個|則)?\\s*新?(的)?(讚|追蹤者|粉絲|留言|通知|互動|訊息邀請)"),
    Regex("(的串文|的貼文|的限時動態|的近況|開始追蹤你|追蹤了你|正在直播|開始直播|標註了你|提到了你|對你的貼文|新增了|優惠|折扣|限時搶|活動通知|官方帳號)"),
    Regex("^(LINE TODAY|LINE 貼文串|Instagram|Threads|Facebook|Messenger)$", RegexOption.IGNORE_CASE)
)

/**
 * 無障礙服務掃畫面時抓到的**介面文字**，不是聊天室。
 *
 * 這些名字光靠「刪掉垃圾聯絡人」處理沒有用 —— 下次掃到又會重建，
 * 使用者還得重新分類一次。真正的解法是在源頭就不要收，
 * 所以 ChatTrackerService 接受標題前也會查這張表。
 */
private val UI_LABEL_NAMES = setOf(
    "返回", "關閉", "全部", "更多", "設定", "搜尋", "首頁", "新訊息", "訊息", "聊天",
    "編輯", "取消", "確定", "完成", "刪除", "分享", "儲存", "選項", "通知", "相機",
    "串文", "貼文", "限時動態", "探索", "個人檔案", "大頭貼照", "大頭照", "頭像",
    "留言", "珍藏", "摘要", "動態", "訊息要求", "已傳送", "傳送", "輸入訊息",
    "back", "close", "all", "more", "settings", "search", "home", "messages",
    "reels", "explore", "profile", "story", "stories", "notifications"
)

private val NON_PERSON_EXTRA = listOf(
    // 「1.2萬 個讚」「按讚數 345」這種數字型標籤
    Regex("^[\\d.,]+\\s*(萬|k|K|M)?\\s*(個)?\\s*(讚|留言|次觀看|views|likes)"),
    Regex("^(按讚數|觀看次數|留言數|分享次數)"),
    // Reels / 貼文殘骸
    Regex("(reels?|限時動態|連續短片)\\s*$", RegexOption.IGNORE_CASE)
)

/** 判斷是否為非真人通知（讚、追蹤、推播、促銷、介面文字） */
fun isNonPersonContact(name: String): Boolean {
    val n = name.trim()
    if (n.isEmpty()) return true
    if (n.lowercase() in UI_LABEL_NAMES) return true
    if (NON_PERSON_PATTERNS.any { it.containsMatchIn(n) }) return true
    return NON_PERSON_EXTRA.any { it.containsMatchIn(n) }
}

/** 統計時要不要排除這個人：非真人，或被歸到「系統通知」標籤 */
fun isExcludedFromStats(name: String, catsMap: Map<String, String>): Boolean =
    isNonPersonContact(name) || catsMap[name] == ContactCategory.SYSTEM.id

private val WORD_STOPWORDS = setOf(
    "的", "了", "是", "我", "你", "他", "她", "在", "有", "和", "就", "不", "都", "也", "很", "到", "去", "會", "著", "沒", "把", "被", "跟", "但", "而", "又", "還",
    "https", "http", "com", "www", "the", "you", "and", "for", "that", "this", "with", "have", "are", "was",
    "我們", "你們", "他們", "這個", "那個", "一個", "就是", "不是", "沒有", "可是", "所以", "因為", "但是", "然後", "如果", "還有", "已經", "這樣", "那樣", "什麼", "怎麼"
)

/**
 * 「已傳送貼圖」「傳送了一張照片」這類是 LINE / IG 自動產生的系統文字，
 * 不是使用者真的打的字。整句符合就完全不計入詞頻，
 * 否則詞頻榜會被「傳送 / 送了 / 貼圖 / 了貼 / 照片」洗版。
 */
// ⚠️ 這個 Regex 絕對不能加 ^ $ 錨點。
//    真實通知長這樣：「王小明傳送了一則貼圖」——前面掛著人名，
//    加了錨點就變成整句要完全吻合才算數，結果一句都過濾不到，
//    連人名都被切成詞算進常用詞。要能在句子「任何位置」命中。
private val SYSTEM_PHRASE = Regex(
    "(傳送|傳了|送出|發送|分享|收回|撤回|貼上)了?\\s*(一[張個則段份支部條]|一則)?\\s*" +
    "(貼圖|表情|照片|圖片|影片|檔案|文件|語音訊息|語音|音檔|位置|訊息|連結|網址|禮物|紅包|名片|動態|限時動態|GIF)" +
    "|(sent|shared)\\s+(you\\s+)?(an?\\s+)?(photo|video|sticker|attachment|message|voice|file|gif|link|reel|post|story)" +
    "|\\[(貼圖|照片|圖片|影片|檔案|語音|位置|禮物|紅包|動畫貼圖)\\]" +
    "|(拍了拍|已加入群組|已退出群組|開始了通話|結束了通話|發起了通話|邀請你|加你為好友)" +
    // 使用者實際回報的字樣
    "|(標註[了]?[您你]的|標註您的訊息|對[你您]的訊息按了|按了[^\\s]{0,4}的?表情|傳達了|" +
    "回覆了[你您]的限時動態|回覆了[你您]的|已回覆|回應了[你您]的|提及了[你您]|在留言中提到)"
)
private val SYSTEM_HINT = Regex(
    "(已傳送|傳送了|收回了訊息|已收回|正在輸入|加入了聊天|退出了聊天|邀請|已讀|" +
    "未接來電|未接電話|通話時間|通話時長|則新訊息|個新的讚|位新追蹤者|開始追蹤|" +
    "標註|按了讚|限時動態|傳達了|已讀取|訊息要求|新的訊息|查看全部)"
)

/**
 * 已經存進詞頻裡的垃圾詞。
 *
 * v2.2 修好了「之後」不再計入，但舊資料還在。而且 n-gram 會把
 * 人名跟片語黏在一起（「王小明傳送了一則貼圖」切出「明傳送」「了貼圖」），
 * 光比對完整片語清不掉，所以双軌：
 *   (a) 詞裡含有只會出現在系統通知的標記字串 → 垃圾
 *   (b) 詞本身是系統句的任一子串（「了一」「一則」「則貼」） → 垃圾
 */
private val JUNK_WORD_MARKERS = listOf(
    "傳送", "傳了", "送出", "發送", "收回", "撤回", "貼圖", "貼文", "照片", "圖片",
    "影片", "檔案", "語音", "音檔", "限時", "已讀", "追蹤", "新的讚", "按讚", "未接",
    "來電", "通話時", "則新", "新訊息", "邀請", "加入了", "退出了", "拍了拍", "分享了", "回覆了"
)
private val JUNK_WORD_EXACT: Set<String> by lazy {
    val corpus = listOf(
        "傳送了一則貼圖", "傳送了一張照片", "傳送了一個檔案", "傳送了一段影片",
        "傳送了語音訊息", "傳送了一則訊息", "收回了訊息", "分享了一則貼文",
        "你有一則新訊息", "已傳送", "正在輸入中", "加入了聊天", "退出了聊天"
    )
    val out = HashSet<String>()
    corpus.forEach { c -> for (n in 2..4) for (i in 0..c.length - n) out.add(c.substring(i, i + n)) }
    out
}

fun isJunkWord(w: String): Boolean =
    JUNK_WORD_MARKERS.any { w.contains(it) } || w in JUNK_WORD_EXACT

fun isSystemGeneratedText(text: String): Boolean {
    val t = text.trim()
    if (t.isEmpty()) return true
    // 命中就整句丟棄。系統句裡夾帶的人名同樣不是「常用詞」，
    // 不能只切掉片語留下名字。
    if (SYSTEM_PHRASE.containsMatchIn(t)) return true
    if (SYSTEM_HINT.containsMatchIn(t)) return true
    // 沒有任何中文或英文字母（純數字符號）的也不是詞彙
    return t.none { it.code in 0x4e00..0x9fa5 || it.isLetter() }
}

/**
 * 只累計詞頻，不保留原文。
 * 中文同時產生 2/3/4 字組合，顯示時再依使用者選擇的字數過濾 ——
 * 因為只存詞頻不存原文，事後無法重新切詞，必須先都算好。
 */
/**
 * 通知本文常常是「小明：今天要吃什麼」這種「發言人：內容」格式。
 * 前綴是名字不是詞彙，切詞前必須先砍掉，否則名字會被算成常用詞。
 */
fun stripSpeakerPrefix(text: String): String {
    val idx = text.indexOfFirst { it == '\uff1a' || it == ':' }
    // 名字不會太長；冒號後面要真的還有內容才切
    if (idx in 1..20 && text.length > idx + 1) {
        val rest = text.substring(idx + 1).trim()
        if (rest.isNotEmpty()) return rest
    }
    return text
}

fun tokenizeForWordFreq(rawText: String): List<String> {
    val text = stripSpeakerPrefix(rawText.trim())
    if (text.isBlank() || text.length > 500) return emptyList()
    if (isSystemGeneratedText(rawText) || isSystemGeneratedText(text)) return emptyList()
    val out = ArrayList<String>()
    Regex("[A-Za-z][A-Za-z']{1,15}").findAll(text).forEach { out.add(it.value.lowercase()) }
    Regex("[\\u4e00-\\u9fa5]{2,}").findAll(text).forEach { m ->
        val s = m.value
        for (n in 2..4) {
            if (s.length < n) break
            for (i in 0..s.length - n) out.add(s.substring(i, i + n))
        }
    }
    return out.filter { it.length >= 2 && it !in WORD_STOPWORDS }
}
enum class ContactSortType(val label: String) { NAME("名稱"), INTERACTIONS("互動量") }

// 用 Long 累加，避免資料量大時 Int 溢位
// 單次停留上限，同時作為異常紀錄的判定門檻
const val MAX_STAY_SECONDS = 1800

fun formatDuration(seconds: Long): String {
    val s = seconds.coerceAtLeast(0L)
    return when {
        s >= 3600 -> "${s / 3600}h ${(s % 3600) / 60}m"
        s >= 60 -> "${s / 60}m ${s % 60}s"
        else -> "${s}s"
    }
}

fun formatDuration(seconds: Int): String = formatDuration(seconds.toLong())

// 名稱含分隔符會讓 CSV 解析錯位
private val CSV_SEPARATORS = listOf("|#|", "|=|", "|cat|", "||", ";")
fun sanitizeForCsv(raw: String): String {
    var out = raw
    CSV_SEPARATORS.forEach { out = out.replace(it, " ") }
    return out.replace('\n', ' ').replace('\r', ' ').trim()
}

fun getStartOfWeekTimestamp(timestamp: Long, startDay: Int): Long {
    // startDay 值異常時避免無限迴圈
    val safeDay = if (startDay in Calendar.SUNDAY..Calendar.SATURDAY) startDay else Calendar.MONDAY
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    cal.firstDayOfWeek = safeDay
    var guard = 0
    while (cal.get(Calendar.DAY_OF_WEEK) != safeDay && guard < 7) {
        cal.add(Calendar.DATE, -1)
        guard++
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
    // 11 個欄位；仍相容舊的 8 欄位資料
    fun toCsvString() = listOf(
        id,
        sanitizeForCsv(rawSenderName),
        sanitizeForCsv(appName),
        isIncoming.toString(),
        wordCount.toString(),
        durationSeconds.toString(),
        isGroup.toString(),
        timestamp.toString(),
        isMissedCall.toString(),
        isCall.toString(),
        subSender?.let { sanitizeForCsv(it) } ?: ""
    ).joinToString("|#|")

    companion object {
        fun fromCsvString(csv: String): InteractionRecord? {
            try {
                val parts = csv.split("|#|")
                if (parts.size < 8) return null
                return InteractionRecord(
                    id = parts[0],
                    rawSenderName = parts[1],
                    appName = parts[2],
                    isIncoming = parts[3].toBoolean(),
                    wordCount = parts[4].toIntOrNull() ?: 0,
                    durationSeconds = parts[5].toIntOrNull() ?: 0,
                    isGroup = parts[6].toBoolean(),
                    timestamp = parts[7].toLongOrNull() ?: 0L,
                    isMissedCall = parts.getOrNull(8)?.toBoolean() ?: false,
                    isCall = parts.getOrNull(9)?.toBoolean() ?: false,
                    subSender = parts.getOrNull(10)?.takeIf { it.isNotBlank() }
                )
            } catch (_: Exception) { }
            return null
        }
    }
}


data class HourlyStat(val hour: Int, var durationSeconds: Long = 0L, var msgCount: Int = 0, var missedCalls: Int = 0) {
    val score: Long get() = (durationSeconds / 60) + (if (durationSeconds % 60 > 0) 1 else 0) + msgCount + (missedCalls * 5L)
}

data class SenderStat(
    val unifiedName: String,
    // recordCount = 全部紀錄；incomingCount = 對方傳進來的次數
    val recordCount: Int,
    val incomingCount: Int,
    val msgCount: Int,
    val callCount: Int,
    val incomingWords: Int,
    val avgWordsPerMessage: Int,
    val totalDurationSeconds: Long,
    val callDurationSeconds: Long,
    val appNames: Set<String>,
    val category: ContactCategory,
    val isGroup: Boolean,
    val subSenders: Map<String, Int> = emptyMap()
)

// ── 深度分析用的模型 ──
data class TrendPoint(val label: String, val dayStart: Long, val durationSeconds: Long, val msgCount: Int)

data class PeriodDelta(val current: Long, val previous: Long) {
    /** 與上一個同長度區間相比的變化百分比；上期為 0 時無法計算成長率 */
    val deltaPct: Int? get() = if (previous <= 0L) null else (((current - previous) * 100.0) / previous).toInt()
}

data class ConcentrationStat(val activeContacts: Int, val top1Pct: Int, val top3Pct: Int, val gini: Int)
data class ChurnStat(val newcomers: List<String>, val dormant: List<Pair<String, Int>>)
/**
 * @param timed 真正抓到通話秒數的通數。平均時長只能除以它，
 *   不能除以 answered —— 多數 Android 原生撥號 App 接通後根本不發通知，
 *   那些通話沒有秒數，除進去只會把平均值稀釋成 0。
 */
data class CallSummary(
    val total: Int, val answered: Int, val missed: Int,
    val totalSeconds: Long, val timed: Int = 0
) {
    val avgSeconds: Long get() = if (timed > 0) totalSeconds / timed else 0L
    val missRate: Int get() = if (total > 0) (missed * 100) / total else 0
    val hasDuration: Boolean get() = timed > 0
}
data class CoverageStat(val activeDays: Int, val totalDays: Int, val longestStreak: Int, val currentStreak: Int) {
    val pct: Int get() = if (totalDays > 0) (activeDays * 100) / totalDays else 0
}
data class GroupSplit(val groupDuration: Long, val personDuration: Long, val groupMsgs: Int, val personMsgs: Int)

data class AppUsageStat(val appName: String, val totalDurationSeconds: Long, val incomingCount: Int)
data class CategoryStat(val category: ContactCategory, val totalDurationSeconds: Long, val incomingCount: Int)
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

    private val _availableCategories = MutableStateFlow(ContactCategory.seedCategories)
    val availableCategories: StateFlow<List<ContactCategory>> = _availableCategories.asStateFlow()

    var isFirstLaunch = MutableStateFlow(true)
    var weekStartDay = MutableStateFlow(Calendar.MONDAY)

    var autoBackupEnabled = MutableStateFlow(false)
    var autoBackupPassword = MutableStateFlow("")
    var lastAutoBackupTime = MutableStateFlow(0L)

    // 自動清理掉的異常紀錄數（設定頁顯示用）
    val lastCleanupRemoved = MutableStateFlow(0)

    // 服務心跳：讓使用者一眼看出無障礙服務到底還活著沒有
    val lastServiceBeat = MutableStateFlow(0L)
    fun beat() { lastServiceBeat.value = System.currentTimeMillis() }

    private const val CRASH_FILE = "RelateStats_LastCrash.txt"

    /**
     * 攔截未捕捉例外並寫檔。
     * 沒有裝置可以看 logcat 時，這是唯一能問出閃退原因的方法；
     * 寫完一定要交回原本的 handler，否則系統無法正常結束程序。
     */
    fun installCrashReporter(context: Context) {
        val ctx = context.applicationContext
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, err ->
            runCatching {
                val sw = java.io.StringWriter()
                err.printStackTrace(java.io.PrintWriter(sw))
                val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                File(ctx.filesDir, CRASH_FILE).writeText(
                    "時間：$stamp\n版本：${BuildConfig.VERSION_NAME}\n執行緒：${thread.name}\n\n$sw"
                )
            }
            prev?.uncaughtException(thread, err)
        }
    }

    fun readLastCrash(): String? {
        val ctx = appContext ?: return null
        return runCatching {
            val f = File(ctx.filesDir, CRASH_FILE)
            if (f.exists()) f.readText() else null
        }.getOrNull()
    }

    fun clearLastCrash() {
        val ctx = appContext ?: return
        runCatching { File(ctx.filesDir, CRASH_FILE).delete() }
    }

    // 偵測診斷用的環狀日誌。無法在開發機上重現使用者的 App 版本時，
    // 這是唯一能問出「服務到底看到什麼」的方法。
    val debugLog = MutableStateFlow<List<String>>(emptyList())
    private var lastLogLine = ""

    fun logDetect(line: String) {
        if (line == lastLogLine) return
        lastLogLine = line
        val stamp = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        debugLog.update { (it + "$stamp  $line").takeLast(60) }
    }

    fun clearDetectLog() { debugLog.value = emptyList(); lastLogLine = "" }

    private val _wordFreq = MutableStateFlow<Map<String, Int>>(emptyMap())
    val wordFreq: StateFlow<Map<String, Int>> = _wordFreq.asStateFlow()
    // 同時存 2/3/4 字組合，上限需放大
    private const val MAX_WORDS = 9000

    @Volatile
    private var prefs: SharedPreferences? = null
    @Volatile
    private var appContext: Context? = null

    // 存檔一律在背景執行
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pendingSaveJob: Job? = null

    // 避免資料無限膨脹拖慢存檔
    private const val MAX_RECORDS = 200000

    /** v2.3：舊詞頻裡的系統通知殘渣只需要清一次 */
    private fun runWordPurgeIfNeeded() {
        val p = prefs ?: return
        if (p.getBoolean("word_purge_v23_done", false)) return
        lastWordPurge.value = purgeJunkWords()
        p.edit().putBoolean("word_purge_v23_done", true).apply()
    }

    fun init(context: Context) {
        if (prefs != null) return
        synchronized(this) {
            if (prefs != null) return
            appContext = context.applicationContext
            prefs = context.getSharedPreferences("RelateStatsData", Context.MODE_PRIVATE)
        }
        loadData()
        val appContext = context.applicationContext
        ioScope.launch {
            runOneTimeCleanupIfNeeded(appContext)
            runWordPurgeIfNeeded()
            performAutoBackupIfNeeded(appContext)
        }
    }

    // 每段獨立防護，單筆資料損毀不影響其餘載入
    fun loadData() {
        val p = prefs ?: return
        isFirstLaunch.value = p.getBoolean("is_first_launch", true)
        weekStartDay.value = p.getInt("week_start_day", Calendar.MONDAY)
        autoBackupEnabled.value = p.getBoolean("auto_backup_enabled", false)
        autoBackupPassword.value = p.getString("auto_backup_pwd", "") ?: ""
        lastAutoBackupTime.value = p.getLong("last_auto_backup_time", 0L)

        _interactions.value = runCatching {
            (p.getStringSet("interactions", emptySet()) ?: emptySet())
                .mapNotNull { InteractionRecord.fromCsvString(it) }
                .sortedBy { it.timestamp }
        }.getOrDefault(emptyList())

        _aliases.value = runCatching { parsePairs(p.getStringSet("aliases", emptySet())) }.getOrDefault(emptyMap())
        _categories.value = runCatching { parsePairs(p.getStringSet("categories", emptySet())) }.getOrDefault(emptyMap())

        _availableCategories.value = runCatching {
            val stored = p.getString("all_categories", null)
            if (!stored.isNullOrBlank()) {
                ContactCategory.ensureEssential(stored.split("|;|").mapNotNull { ContactCategory.fromCsvString(it) })
            } else {
                // 舊版只存自訂標籤，內建是寫死的 —— 合併後改用新格式
                val old = (p.getStringSet("custom_categories", emptySet()) ?: emptySet())
                    .mapNotNull { ContactCategory.fromCsvString(it) }
                ContactCategory.ensureEssential(ContactCategory.seedCategories + old)
            }
        }.getOrDefault(ContactCategory.seedCategories)

        _wordFreq.value = runCatching {
            (p.getStringSet("word_freq", emptySet()) ?: emptySet()).mapNotNull { line ->
                val i = line.lastIndexOf("|w|")
                if (i <= 0) null else line.substring(0, i) to (line.substring(i + 3).toIntOrNull() ?: return@mapNotNull null)
            }.toMap()
        }.getOrDefault(emptyMap())
    }

    private fun parsePairs(raw: Set<String>?): Map<String, String> {
        if (raw.isNullOrEmpty()) return emptyMap()
        val out = HashMap<String, String>(raw.size)
        raw.forEach { line ->
            val idx = line.indexOf("|=|")
            if (idx > 0 && idx + 3 < line.length) {
                out[line.substring(0, idx)] = line.substring(idx + 3)
            }
        }
        return out
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

    // 背景執行緒 + 1.5 秒去抖動，避免每則通知都全量寫檔
    private fun saveData(immediate: Boolean = false) {
        pendingSaveJob?.cancel()
        if (immediate) {
            ioScope.launch { writeToDisk() }
        } else {
            pendingSaveJob = ioScope.launch {
                delay(1500)
                writeToDisk()
            }
        }
    }

    private fun writeToDisk() {
        val p = prefs ?: return
        runCatching {
            val records = _interactions.value.map { it.toCsvString() }.toSet()
            val aliases = _aliases.value.map { "${it.key}|=|${it.value}" }.toSet()
            val cats = _categories.value.map { "${it.key}|=|${it.value}" }.toSet()
            val allCats = _availableCategories.value.joinToString("|;|") { it.toCsvString() }
            val words = _wordFreq.value.entries.map { "${it.key}|w|${it.value}" }.toSet()
            p.edit().apply {
                putStringSet("interactions", records)
                putStringSet("aliases", aliases)
                putStringSet("categories", cats)
                putString("all_categories", allCats)
                putStringSet("word_freq", words)
            }.apply()
        }
    }

    fun addInteraction(record: InteractionRecord) {
        // 服務可能先於 Activity 啟動，prefs 未就緒時不能靜靜丟資料
        if (prefs == null) return

        _interactions.update { list ->
            val next = list + record
            if (next.size > MAX_RECORDS) next.takeLast(MAX_RECORDS) else next
        }
        val unifiedName = _aliases.value[record.rawSenderName] ?: record.rawSenderName
        if (!_categories.value.containsKey(unifiedName)) {
            val cat = if (isNonPersonContact(unifiedName)) ContactCategory.SYSTEM.id else ContactCategory.OTHER.id
            _categories.update { it + (unifiedName to cat) }
        }
        saveData()
    }

    /** 批次新增：匯入時用，避免每筆都觸發一次存檔與分類建立 */
    fun addInteractions(records: List<InteractionRecord>) {
        if (records.isEmpty() || prefs == null) return
        _interactions.update { list ->
            val next = list + records
            if (next.size > MAX_RECORDS) next.takeLast(MAX_RECORDS) else next
        }
        val newCats = HashMap<String, String>()
        records.forEach { r ->
            val unified = _aliases.value[r.rawSenderName] ?: r.rawSenderName
            if (!_categories.value.containsKey(unified) && !newCats.containsKey(unified)) {
                newCats[unified] = if (isNonPersonContact(unified)) ContactCategory.SYSTEM.id else ContactCategory.OTHER.id
            }
        }
        if (newCats.isNotEmpty()) _categories.update { it + newCats }
        saveData(immediate = true)
    }

    /**
     * 從系統通話紀錄匯入。
     *
     * 為什麼需要這個：Android 原生撥號 App 接通後不發通知，
     * 只靠通知永遠拿不到「接通的通話有多長」，平均通話時長就永遠是 0。
     * 只有讀系統通話紀錄才拿得到真實秒數。
     *
     * id 用固定規則而非隨機 UUID，重複匯入不會產生重複紀錄。
     * @return (新增筆數, 已存在跳過筆數)
     */
    fun importCallLog(context: Context): Pair<Int, Int> {
        val existing = _interactions.value.mapTo(HashSet()) { it.id }
        val fresh = ArrayList<InteractionRecord>()
        var skipped = 0
        val cols = arrayOf(
            CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE, CallLog.Calls.DATE, CallLog.Calls.DURATION
        )
        context.contentResolver.query(
            CallLog.Calls.CONTENT_URI, cols, null, null, CallLog.Calls.DATE + " DESC"
        )?.use { c ->
            val iNum = c.getColumnIndex(CallLog.Calls.NUMBER)
            val iName = c.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val iType = c.getColumnIndex(CallLog.Calls.TYPE)
            val iDate = c.getColumnIndex(CallLog.Calls.DATE)
            val iDur = c.getColumnIndex(CallLog.Calls.DURATION)
            if (iDate < 0 || iType < 0) return@use
            while (c.moveToNext()) {
                val date = c.getLong(iDate)
                val type = c.getInt(iType)
                // 阻擋、語音信箱不是人對人的互動
                if (type == CallLog.Calls.BLOCKED_TYPE || type == CallLog.Calls.VOICEMAIL_TYPE) continue
                val id = "calllog-" + date
                if (id in existing) { skipped++; continue }
                val number = if (iNum >= 0) c.getString(iNum).orEmpty() else ""
                val cached = if (iName >= 0) c.getString(iName).orEmpty() else ""
                val name = cached.ifBlank { number }
                if (name.isBlank()) continue
                val missed = type == CallLog.Calls.MISSED_TYPE || type == CallLog.Calls.REJECTED_TYPE
                val dur = if (iDur >= 0) c.getLong(iDur).toInt().coerceAtLeast(0) else 0
                fresh.add(
                    InteractionRecord(
                        id = id,
                        rawSenderName = name,
                        appName = "電話",
                        isIncoming = type != CallLog.Calls.OUTGOING_TYPE,
                        wordCount = 0,
                        durationSeconds = if (missed) 0 else dur,
                        isGroup = false,
                        timestamp = date,
                        isMissedCall = missed,
                        isCall = true
                    )
                )
                existing.add(id)
            }
        }
        if (fresh.isNotEmpty()) {
            snapshotForUndo("匯入通話紀錄")
            addInteractions(fresh.sortedBy { it.timestamp })
        }
        return fresh.size to skipped
    }

    /**
     * 每個對話「已處理到哪一則訊息」的時間戳。
     * 通知服務被系統殺掉重啟後，若沒有這張表，通知裡夾帶的整段歷史會被重算一次。
     * 資料極小（一個對話一個 Long），直接放 prefs。
     */
    private val handledTimes = HashMap<String, Long>()

    fun loadHandledTimes(): Map<String, Long> {
        val p = prefs ?: return emptyMap()
        if (handledTimes.isEmpty()) {
            runCatching {
                (p.getStringSet("handled_times", emptySet()) ?: emptySet()).forEach { line ->
                    val i = line.lastIndexOf("|=|")
                    if (i > 0) handledTimes[line.substring(0, i)] = line.substring(i + 3).toLongOrNull() ?: 0L
                }
            }
        }
        return handledTimes
    }

    fun saveHandledTime(key: String, value: Long) {
        val p = prefs ?: return
        handledTimes[key] = value
        // 只留最近 300 個對話，避免無限成長
        val trimmed = handledTimes.entries.sortedByDescending { it.value }.take(300)
        handledTimes.clear()
        trimmed.forEach { handledTimes[it.key] = it.value }
        runCatching {
            p.edit().putStringSet("handled_times", trimmed.map { it.key + "|=|" + it.value }.toSet()).apply()
        }
    }

    fun addWords(words: List<String>, senderName: String? = null) {
        if (words.isEmpty() || prefs == null) return
        // 發話者名字的碎片不是詞彙，先擋掉
        val nameFrags: Set<String> = senderName?.trim()?.takeIf { it.length in 2..24 }?.let { nm ->
            HashSet<String>().apply { for (n in 2..4) for (i in 0..nm.length - n) add(nm.substring(i, i + n)) }
        } ?: emptySet()
        val clean = words.filter { it !in nameFrags }
        if (clean.isEmpty()) return
        _wordFreq.update { cur ->
            val m = HashMap(cur)
            clean.forEach { m[it] = (m[it] ?: 0) + 1 }
            if (m.size > MAX_WORDS) m.entries.sortedByDescending { it.value }.take(MAX_WORDS).associate { it.key to it.value }
            else m
        }
        saveData()
    }

    val lastWordPurge = MutableStateFlow(0)

    /**
     * 把已經存進詞頻的系統通知殘渣刪掉，保留其他累積。
     *
     * 除了系統片語，**人名碎片也要清**。通知長「王小明傳送了一則貼圖」，
     * n-gram 會切出「王小」「小明」「王小明」——那是聯絡人名字，不是常用詞。
     * 名字沒辦法用規則認出來，但我們手上就有完整聯絡人清單，直接拿來比對最準。
     *
     * @return 刪掉幾個詞
     */
    fun purgeJunkWords(): Int {
        val cur = _wordFreq.value
        if (cur.isEmpty()) return 0

        // 所有聯絡人名字的 2~4 字子字串
        val nameFragments = HashSet<String>()
        val names = (_interactions.value.map { _aliases.value[it.rawSenderName] ?: it.rawSenderName } +
            _aliases.value.keys + _aliases.value.values).toHashSet()
        names.forEach { raw ->
            val nm = raw.trim()
            if (nm.length in 2..24) {
                for (n in 2..4) for (i in 0..nm.length - n) nameFragments.add(nm.substring(i, i + n))
            }
        }

        val kept = cur.filterKeys { !isJunkWord(it) && it !in nameFragments }
        val removed = cur.size - kept.size
        if (removed > 0) {
            _wordFreq.value = kept
            saveData(immediate = true)
        }
        return removed
    }

    fun clearWordFreq() {
        _wordFreq.value = emptyMap()
        saveData(immediate = true)
    }

    // 任何破壞性操作前先存一份完整快照到 App 內部空間，
    // 存檔案而不是 SharedPreferences —— 快照可達十幾 MB，塞進 prefs 會讓主檔案膨脹一倍
    private const val UNDO_FILE = "RelateStats_Undo.txt"

    val undoAvailable = MutableStateFlow(false)
    val undoLabel = MutableStateFlow("")

    /**
     * ⚠️ 狀態必須「同步」擷取。呼叫端會在這個函式回傳後立刻修改資料，
     * 若把讀取也丟進背景協程，快照可能拍到已經刪除後的狀態，復原就失效了。
     * 因此這裡先抓住不可變的引用，序列化與寫檔才交給背景。
     */
    fun snapshotForUndo(label: String) {
        val ctx = appContext ?: return
        val snapRecords = _interactions.value
        val snapAliases = _aliases.value
        val snapCats = _categories.value
        val snapAvail = _availableCategories.value
        undoLabel.value = label
        undoAvailable.value = true
        ioScope.launch {
            runCatching {
                val payload = buildBackupString(snapRecords, snapAliases, snapCats, snapAvail, "")
                File(ctx.filesDir, UNDO_FILE).writeText(payload)
            }.onFailure { undoAvailable.value = false }
        }
    }

    /** 復原上一次破壞性操作 */
    fun undoLastDestructive(): Boolean {
        val ctx = appContext ?: return false
        return runCatching {
            val f = File(ctx.filesDir, UNDO_FILE)
            if (!f.exists()) return false
            val ok = importBackupString(f.readText(), "")
            if (ok) { f.delete(); undoAvailable.value = false; undoLabel.value = "" }
            ok
        }.getOrDefault(false)
    }

    fun refreshUndoState() {
        val ctx = appContext ?: return
        undoAvailable.value = runCatching { File(ctx.filesDir, UNDO_FILE).exists() }.getOrDefault(false)
    }

    /** 依 id 刪除指定紀錄 */
    fun deleteRecordsByIds(ids: Set<String>): Int {
        if (ids.isEmpty()) return 0
        snapshotForUndo("刪除 ${ids.size} 筆紀錄")
        val before = _interactions.value.size
        _interactions.value = _interactions.value.filterNot { it.id in ids }
        saveData(immediate = true)
        return before - _interactions.value.size
    }

    /** 批次設定標籤，只寫一次檔 */
    fun updateCategories(names: Collection<String>, categoryId: String) {
        if (names.isEmpty()) return
        _categories.update { cur -> cur + names.associateWith { categoryId } }
        saveData(immediate = true)
    }

    /** 刪除指定聯絡人的所有紀錄，連同別名與標籤設定一起清掉。回傳刪除筆數 */
    /**
     * 改顯示名稱。
     * 作法是把所有指向舊名的別名（含舊名本身）改指向新名，
     * 互動紀錄不動 —— 紀錄存的是原始發話者名，歸戶一律走 aliases。
     */
    fun renameContact(oldName: String, newName: String) {
        val target = newName.trim()
        if (target.isEmpty() || target == oldName) return
        snapshotForUndo("修改顯示名稱")
        val raws = _interactions.value.map { it.rawSenderName }.distinct()
            .filter { (_aliases.value[it] ?: it) == oldName }
        val nextAliases = HashMap(_aliases.value)
        raws.forEach { nextAliases[it] = target }
        // 原本就指向舊名的別名也要一起搬過去
        _aliases.value.forEach { (k, v) -> if (v == oldName) nextAliases[k] = target }
        _aliases.value = nextAliases

        val cat = _categories.value[oldName]
        if (cat != null) {
            _categories.update { (it - oldName) + (target to cat) }
        }
        saveData(immediate = true)
    }

    fun deleteContacts(names: Set<String>): Int {
        if (names.isEmpty()) return 0
        snapshotForUndo("刪除 ${names.size} 位聯絡人")
        val before = _interactions.value.size
        _interactions.value = _interactions.value.filterNot {
            (_aliases.value[it.rawSenderName] ?: it.rawSenderName) in names
        }
        _aliases.update { m -> m.filterNot { (k, v) -> k in names || v in names } }
        _categories.update { m -> m.filterNot { (k, _) -> k in names } }
        saveData(immediate = true)
        return before - _interactions.value.size
    }

    /** 目前被判定為垃圾的聯絡人（非真人通知，或已被歸到系統通知） */
    fun listJunkContacts(): List<String> =
        _interactions.value.map { _aliases.value[it.rawSenderName] ?: it.rawSenderName }
            .distinct()
            .filter { isNonPersonContact(it) || _categories.value[it] == ContactCategory.SYSTEM.id }

    /** 預覽「整理系統通知」會影響哪些聯絡人（尚未歸類的） */
    fun previewRetagTargets(): List<String> =
        _interactions.value.map { _aliases.value[it.rawSenderName] ?: it.rawSenderName }.distinct()
            .filter { isNonPersonContact(it) && _categories.value[it] != ContactCategory.SYSTEM.id }

    /** 把現有聯絡人裡的非真人通知重新歸到「系統通知」，回傳筆數 */
    fun retagSystemContacts(): Int {
        val names = _interactions.value.map { _aliases.value[it.rawSenderName] ?: it.rawSenderName }.distinct()
        var n = 0
        val updates = HashMap<String, String>()
        names.forEach { name ->
            if (isNonPersonContact(name) && _categories.value[name] != ContactCategory.SYSTEM.id) {
                updates[name] = ContactCategory.SYSTEM.id
                n++
            }
        }
        if (n > 0) {
            _categories.update { it + updates }
            saveData(immediate = true)
        }
        return n
    }

    /**
     * 找出可能是同一個人的名稱群組。
     * 先比對「去掉 emoji/標點/空白」後完全相同的，再比對一方包含另一方的。
     */
    fun findSimilarGroups(names: List<String>): List<List<String>> {
        val valid = names.filter { it.isNotBlank() && !isNonPersonContact(it) }.distinct()
        val byNorm = LinkedHashMap<String, MutableList<String>>()
        valid.forEach { n ->
            val k = normalizeContactName(n)
            if (k.length >= 2) byNorm.getOrPut(k) { ArrayList() }.add(n)
        }
        val used = HashSet<String>()
        val groups = ArrayList<List<String>>()

        byNorm.values.forEach { list ->
            if (list.size > 1) {
                groups.add(list.toList())
                used.addAll(list)
            }
        }

        val remaining = byNorm.entries.filter { it.value.size == 1 && it.value[0] !in used }
        for (i in remaining.indices) {
            val (ka, la) = remaining[i]
            if (la[0] in used) continue
            val bucket = ArrayList<String>()
            for (j in i + 1 until remaining.size) {
                val (kb, lb) = remaining[j]
                if (lb[0] in used) continue
                val shortK = if (ka.length <= kb.length) ka else kb
                val longK = if (ka.length <= kb.length) kb else ka
                if (shortK.length >= 3 && longK.contains(shortK)) bucket.add(lb[0])
            }
            if (bucket.isNotEmpty()) {
                bucket.add(0, la[0])
                groups.add(bucket.toList())
                used.addAll(bucket)
            }
        }
        return groups
    }

    fun updateCategory(unifiedName: String, categoryId: String) { _categories.update { it + (unifiedName to categoryId) }; saveData(immediate = true) }
    /**
     * 帳號合併。
     * 目標解析到最終節點，並把原本指向來源的別名一起改指，支援鏈式合併。
     */
    fun setAlias(rawName: String, unifiedName: String) {
        if (rawName == unifiedName || rawName.isBlank() || unifiedName.isBlank()) return
        _aliases.update { current ->
            val target = resolveAlias(unifiedName, current)
            if (target == rawName) return@update current  // 會形成循環，放棄
            val next = HashMap(current)
            next[rawName] = target
            current.forEach { (k, v) -> if (v == rawName) next[k] = target }
            next
        }
        saveData(immediate = true)
    }

    private fun resolveAlias(name: String, map: Map<String, String>): String {
        var cur = name
        var guard = 0
        while (guard++ < 10) {
            val next = map[cur] ?: return cur
            if (next == cur) return cur
            cur = next
        }
        return cur
    }
    fun addCustomCategory(cat: ContactCategory) { _availableCategories.update { it + cat }; saveData(immediate = true) }

    /** 改名並換色；id 不變，已分類的聯絡人不受影響 */
    fun updateCategoryMeta(id: String, label: String, colorInt: Int, bgColorInt: Int) {
        val name = label.trim()
        if (name.isEmpty()) return
        _availableCategories.update { list ->
            list.map { if (it.id == id) it.copy(label = name, colorInt = colorInt, bgColorInt = bgColorInt) else it }
        }
        saveData(immediate = true)
    }

    /** 刪除標籤，原本屬於它的聯絡人退回「未分類」。OTHER / SYSTEM 不可刪 */
    fun deleteCategory(id: String): Boolean {
        if (id in ContactCategory.PROTECTED_IDS) return false
        if (_availableCategories.value.none { it.id == id }) return false
        _availableCategories.update { list -> list.filterNot { it.id == id } }
        _categories.update { m -> m.mapValues { (_, v) -> if (v == id) ContactCategory.OTHER.id else v } }
        saveData(immediate = true)
        return true
    }

    /** 調整順序（delta = -1 上移、+1 下移） */
    fun moveCategory(id: String, delta: Int) {
        _availableCategories.update { list ->
            val i = list.indexOfFirst { it.id == id }
            val j = i + delta
            if (i < 0 || j !in list.indices) list
            else list.toMutableList().apply { add(j, removeAt(i)) }
        }
        saveData(immediate = true)
    }

    fun clearInteractions(scope: String) {
        snapshotForUndo(when (scope) { "TODAY" -> "清除今日資料"; "WEEK" -> "清除本週資料"; else -> "清除所有資料" })
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
            _availableCategories.value = ContactCategory.seedCategories
        }
        saveData(immediate = true)
    }

    fun clearInteractionsByDate(timestamp: Long) {
        snapshotForUndo("清除指定日期資料")
        val targetCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val targetYear = targetCal.get(Calendar.YEAR)
        val targetDay = targetCal.get(Calendar.DAY_OF_YEAR)

        _interactions.update { list ->
            list.filterNot { record ->
                val rCal = Calendar.getInstance().apply { timeInMillis = record.timestamp }
                rCal.get(Calendar.YEAR) == targetYear && rCal.get(Calendar.DAY_OF_YEAR) == targetDay
            }
        }
        saveData(immediate = true)
    }

    /**
     * 異常紀錄判定。
     * 停留時間超過上限者為舊版計時錯誤的產物，無法還原只能剔除。
     * 訊息紀錄的 durationSeconds 是 0，不會被誤刪。
     */
    private fun isAbnormal(r: InteractionRecord): Boolean =
        r.durationSeconds < 0 || r.durationSeconds > MAX_STAY_SECONDS || r.timestamp <= 0L

    fun countAbnormalRecords(): Int = _interactions.value.count { isAbnormal(it) }

    fun removeAbnormalRecords(): Int {
        snapshotForUndo("清理異常紀錄")
        val before = _interactions.value.size
        _interactions.value = _interactions.value.filterNot { isAbnormal(it) }
        val removed = before - _interactions.value.size
        if (removed > 0) saveData(immediate = true)
        return removed
    }

    /** 升級後自動執行一次；清理前會先把原始資料另存一份到 App 內部空間 */
    private fun runOneTimeCleanupIfNeeded(context: Context) {
        val p = prefs ?: return
        if (p.getBoolean("cleanup_v2_done", false)) return
        val bad = countAbnormalRecords()
        if (bad > 0) {
            runCatching {
                File(context.filesDir, "RelateStats_PreCleanup_${System.currentTimeMillis()}.txt")
                    .writeText(exportBackupString(""))
            }
            removeAbnormalRecords()
        }
        retagSystemContacts()
        lastCleanupRemoved.value = bad
        p.edit().putBoolean("cleanup_v2_done", true).apply()
    }

    /**
     * 產生備份檔並交給系統分享選單。
     * 走 Share Intent，不需 Drive API/OAuth。
     * 用 FileProvider 傳檔：備份可達十幾 MB，塞 EXTRA_TEXT 會超過 Binder 上限。
     */
    fun buildShareBackupIntent(context: Context): Intent? = runCatching {
        val dir = File(context.cacheDir, "backups").apply { mkdirs() }
        dir.listFiles()?.forEach { runCatching { it.delete() } }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val file = File(dir, "RelateStats_Backup_$stamp.txt")
        file.writeText(exportBackupString(""))
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "RelateStats 備份 $stamp")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }.getOrNull()

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

    fun exportBackupString(key: String): String = buildBackupString(
        _interactions.value, _aliases.value, _categories.value, _availableCategories.value, key
    )

    private fun buildBackupString(
        recordList: List<InteractionRecord>,
        aliasMap: Map<String, String>,
        catMap: Map<String, String>,
        availCats: List<ContactCategory>,
        key: String
    ): String {
        val records = recordList.joinToString(";") { it.toCsvString() }
        val aliases = aliasMap.map { "${it.key}|=|${it.value}" }.joinToString(";")
        val categories = catMap.map { "${it.key}|=|${it.value}" }.joinToString(";")
        val customCats = availCats.joinToString(";") { it.toCsvString() }
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
            // 欄位數不足代表備份檔損毀
            if (parts.size < 5 || !parts[0].startsWith("V")) return false
            val newRecords = parts[1].split(";").filter { it.isNotBlank() }
                .mapNotNull { InteractionRecord.fromCsvString(it) }.sortedBy { it.timestamp }
            val newAliases = parsePairs(parts[2].split(";").filter { it.isNotBlank() }.toSet())
            val newCategories = parsePairs(parts[3].split(";").filter { it.isNotBlank() }.toSet())
            val newCustomCats = parts[4].split(";").filter { it.isNotBlank() }.mapNotNull { ContactCategory.fromCsvString(it) }
            if (newRecords.isEmpty() && newAliases.isEmpty() && newCategories.isEmpty()) return false
            _interactions.value = newRecords; _aliases.value = newAliases; _categories.value = newCategories; _availableCategories.value = ContactCategory.ensureEssential(
                // 舊備份只含自訂標籤（沒有 OTHER），要補回種子清單
                if (newCustomCats.none { it.id == ContactCategory.OTHER.id }) ContactCategory.seedCategories + newCustomCats
                else newCustomCats
            )
            saveData(immediate = true)
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

    // 只在「整則內容就是雜訊」或「以雜訊前綴開頭」時略過，
    // 用 contains 比對會把含這些字的真實訊息一起丟掉。
    private val noiseExact = setOf(
        "貼圖", "照片", "圖片", "影片", "語音訊息", "檔案", "位置資訊",
        "[貼圖]", "[照片]", "[影片]", "[語音訊息]",
        "Sent a sticker", "Sent a photo", "Sent an attachment", "串文"
    )
    private val noisePrefix = listOf("正在傳送", "傳送中", "收回了", "已收回", "sending", "sent a ")

    // 去重：LINE / IG 會為同一則訊息重複 post
    private val lastHandledLoaded = java.util.concurrent.atomic.AtomicBoolean(false)

    private val seenMessages = object : LinkedHashMap<String, Long>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?) = size > 400
    }
    // 每個對話已處理到哪個訊息時間點（避免同一則通知被重複 post 時重算）
    private val lastHandledTime = object : LinkedHashMap<String, Long>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?) = size > 300
    }

    override fun onCreate() { super.onCreate(); DataRepository.init(applicationContext) }
    override fun onListenerConnected() { super.onListenerConnected(); DataRepository.init(applicationContext) }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            handleNotification(sbn)
        } catch (_: Exception) {
            // 拋例外會被系統停用服務，之後全部收不到
        }
    }

    private fun handleNotification(sbn: StatusBarNotification) {
        val appName = targetApps[sbn.packageName] ?: return
        val n = sbn.notification ?: return

        // 群組摘要與常駐通知不是真實訊息
        val flags = n.flags
        if (flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        if (flags and Notification.FLAG_ONGOING_EVENT != 0 && appName != "電話") return

        val extras = n.extras ?: return
        val convTitle = extras.getString("android.conversationTitle")
        val baseTitle = extras.getString("android.title") ?: convTitle ?: return

        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val bigText = extras.getCharSequence("android.bigText")?.toString() ?: ""
        val fallbackContent = if (bigText.isNotBlank()) bigText else text

        // ---- 通話類優先處理 ----
        val callInfo = detectCall(appName, baseTitle, fallbackContent)
        if (callInfo != null) {
            val cleanName = stripCallSuffix(baseTitle)
            if (cleanName.isBlank()) return
            if (!markSeen(sbn.key, n.`when`, cleanName + "|" + fallbackContent)) return
            DataRepository.addInteraction(
                InteractionRecord(
                    rawSenderName = cleanName,
                    appName = appName,
                    isIncoming = true,
                    wordCount = 0,
                    durationSeconds = callInfo.second,
                    isGroup = false,
                    isMissedCall = callInfo.first,
                    isCall = true
                )
            )
            return
        }
        // 撥號 App 只產生通話紀錄，否則「通話中」通知會被當成訊息
        if (appName == "電話") return

        // ---- 一般訊息 ----
        // 一次多則訊息只會 post 一則通知，逐則計入才不會短少。
        // extractMessagingStyleFromNotification() 在公開 SDK 已隱藏，只能走 extras。
        val parsed = extractMessages(extras)
        // MessagingStyle 規格：sender 為 null 代表「這則是使用者本人發的」。
        // selfDisplayName 是本人在這個對話裡的顯示名稱，同樣要排除。
        val selfName = extras.getCharSequence("android.selfDisplayName")?.toString()?.trim()
        val incoming = parsed.filter { m ->
            val s = m.sender?.trim()
            !s.isNullOrBlank() && (selfName == null || !s.equals(selfName, ignoreCase = true))
        }

        // ⚠️ 群組判定「只」能信官方旗標 android.isGroupConversation。
        //  - 不能用 conversationTitle 是否存在：IG 連一對一私訊都會帶。
        //  - 也不能用「多位不同發言人」：Instagram 會把多個不同私訊串
        //    打包進同一則通知，發言人自然不只一位，但那是好幾段一對一對話，
        //    不是群組。誤判會讓不同的人被併成同一個「群組」。
        //    非群組時每則訊息各自歸戶到自己的發話者即可正確拆開。
        val isGroupFlag = extras.getBoolean("android.isGroupConversation", false)

        if (incoming.isNotEmpty()) {
            val convName = convTitle?.trim()?.takeIf { it.isNotBlank() }
            val convKey = sbn.key + "|" + (convName ?: baseTitle)
            if (lastHandledLoaded.compareAndSet(false, true)) {
                lastHandledTime.putAll(DataRepository.loadHandledTimes())
            }
            val since = lastHandledTime[convKey] ?: 0L
            var newest = since
            var handled = 0
            incoming.forEach { msg ->
                if (msg.time <= since) return@forEach
                if (msg.time > newest) newest = msg.time
                if (isNoise(msg.text)) return@forEach
                val speaker = msg.sender?.trim()?.takeIf { it.isNotBlank() }
                // 群組才用對話名當歸戶對象；一對一一律用實際發話者
                val senderName = if (isGroupFlag && convName != null) convName else (speaker ?: convName ?: baseTitle)
                val subSender = if (isGroupFlag) speaker else null
                addMessage(appName, senderName, subSender, msg.text, isGroupFlag, msg.time)
                handled++
            }
            lastHandledTime[convKey] = newest
            // ⚠️ 一定要存檔。一則通知會夾帶整段對話歷史，靠 lastHandledTime 擋重複；
            //    這張表只放在記憶體，服務一被系統殺掉重啟就歸零，
            //    整段歷史會被重新計入一次 —— 這就是「常用詞次數比實際多」的來源。
            DataRepository.saveHandledTime(convKey, newest)
            if (handled > 0) return
            if (since > 0L) return
        } else if (parsed.isNotEmpty()) {
            // 整則通知都是自己發的訊息，不計入
            return
        }

        // ---- 沒有 MessagingStyle 的後備路徑 ----
        var senderName = baseTitle.trim()
        var subSender: String? = null
        var isGroup = isGroupFlag
        var content = fallbackContent

        // 只有官方旗標說是群組時，才用對話名歸戶、把標題當發話者
        if (isGroup && !convTitle.isNullOrBlank() && convTitle.trim() != baseTitle.trim()) {
            senderName = convTitle.trim()
            subSender = baseTitle.trim()
        }

        if (appName == "Instagram") {
            // 「(你的帳號) 群組名」→ 群組；「帳號 : 朋友名」→ 私訊
            if (baseTitle.startsWith("(") && baseTitle.contains(") ")) {
                isGroup = true
                senderName = baseTitle.substringAfter(") ").trim()
                val sepIdx = content.indexOf(" : ").takeIf { it > 0 } ?: content.indexOf(": ")
                if (sepIdx > 0) {
                    subSender = content.substring(0, sepIdx).trim()
                    content = content.substring(sepIdx).removePrefix(" : ").removePrefix(": ")
                }
            } else if (baseTitle.contains(" : ")) {
                isGroup = false
                senderName = baseTitle.substringAfterLast(" : ").trim()
            }
        } else if (isGroup && subSender != null && content.contains(" : ")) {
            // LINE 群組：內文為「發言人 : 訊息」
            val speaker = content.substringBefore(" : ").trim()
            if (speaker.isNotBlank() && speaker.length <= 40) {
                subSender = speaker
                content = content.substringAfter(" : ")
            }
        }

        if (senderName.isBlank() || isNoise(content)) return
        if (!markSeen(sbn.key, n.`when`, senderName + "|" + subSender + "|" + content)) return
        addMessage(appName, senderName, subSender, content, isGroup, sbn.postTime)
    }

    private class ParsedMessage(val text: String, val time: Long, val sender: String?)

    /** 讀取 Notification.EXTRA_MESSAGES ("android.messages")：一則通知可包含多則訊息 */
    private fun extractMessages(extras: android.os.Bundle): List<ParsedMessage> {
        val arr = runCatching {
            @Suppress("DEPRECATION")
            extras.getParcelableArray("android.messages")
        }.getOrNull() ?: return emptyList()

        val out = ArrayList<ParsedMessage>(arr.size)
        arr.forEach { item ->
            val b = item as? android.os.Bundle ?: return@forEach
            val text = b.getCharSequence("text")?.toString()?.trim().orEmpty()
            if (text.isEmpty()) return@forEach
            val time = b.getLong("time", 0L)
            val sender = b.getCharSequence("sender")?.toString()
            out.add(ParsedMessage(text, time, sender))
        }
        return out.sortedBy { it.time }
    }

    private fun addMessage(appName: String, senderName: String, subSender: String?, body: String, isGroup: Boolean, ts: Long) {
        val name = senderName.trim()
        if (name.isBlank()) return
        // 非真人通知不累計詞頻
        // 連人名一起排除：通知本文有時會夾帶對方名字，切成 n-gram 就變成「常用詞」
        if (!isNonPersonContact(name)) DataRepository.addWords(tokenizeForWordFreq(body), name)
        DataRepository.addInteraction(
            InteractionRecord(
                rawSenderName = name,
                appName = appName,
                isIncoming = true,
                // 不含「發言人 :」前綴
                wordCount = body.trim().length,
                durationSeconds = 0,
                isGroup = isGroup,
                timestamp = if (ts in 1..System.currentTimeMillis() + 60000L) ts else System.currentTimeMillis(),
                isMissedCall = false,
                isCall = false,
                // 群組發言排行需要 subSender
                subSender = subSender?.trim()?.takeIf { it.isNotBlank() && it != name }
            )
        )
    }

    /** @return null = 不是通話；否則 (是否未接, 通話秒數) */
    private fun detectCall(appName: String, title: String, content: String): Pair<Boolean, Int>? {
        val blob = "$title $content"
        // ⚠️ 台灣原生撥號 App 的字樣是「未接電話」不是「未接來電」。
        // 只比對「未接來電」會讓所有未接來電偵測失敗，實機資料驗證過。
        val missed = listOf("未接來電", "未接電話", "未接的電話", "取消通話", "未接視訊", "Missed call")
            .any { blob.contains(it, ignoreCase = true) }
        val answered = listOf("通話時間", "通話時長", "已通話", "通話結束", "結束通話", "Call ended")
            .any { blob.contains(it, ignoreCase = true) }
        if (appName != "電話" && !missed && !answered) return null
        if (appName == "電話" && !missed && !answered) {
            // 通話中的常駐通知等結束後那則再算
            return null
        }
        if (missed) return true to 0
        return false to parseCallSeconds(content)
    }

    // 支援 1:02:03 / 1小時2分 / 12分34秒 / 90秒 等寫法
    private fun parseCallSeconds(content: String): Int {
        Regex("(\\d{1,2}):(\\d{2}):(\\d{2})").find(content)?.let {
            return it.groupValues[1].toInt() * 3600 + it.groupValues[2].toInt() * 60 + it.groupValues[3].toInt()
        }
        Regex("(\\d{1,3})\\s*小時\\s*(\\d{1,2})\\s*分").find(content)?.let {
            val sec = Regex("(\\d{1,2})\\s*秒").find(content)?.groupValues?.get(1)?.toInt() ?: 0
            return it.groupValues[1].toInt() * 3600 + it.groupValues[2].toInt() * 60 + sec
        }
        Regex("(\\d{1,3})\\s*[:：分]\\s*(\\d{1,2})\\s*秒?").find(content)?.let {
            return it.groupValues[1].toInt() * 60 + it.groupValues[2].toInt()
        }
        Regex("(\\d{1,4})\\s*秒").find(content)?.let { return it.groupValues[1].toInt() }
        return 0
    }

    private fun stripCallSuffix(title: String): String =
        listOf("未接來電", "未接電話", "未接的電話", "未接視訊通話", "未接視訊", "Missed call")
            .fold(title) { acc, w -> acc.replace(w, "", ignoreCase = true) }
            .replace("通話時間", "").trim().trim('-', '·', '•', ' ')

    private fun isNoise(content: String): Boolean {
        val c = content.trim()
        if (c.isEmpty()) return true
        if (c in noiseExact) return true
        return noisePrefix.any { c.startsWith(it, ignoreCase = true) }
    }

    /** 同一則訊息在短時間內重複 post 就略過 */
    private fun markSeen(key: String?, whenMs: Long, payload: String): Boolean {
        val id = (key ?: "") + "|" + whenMs + "|" + payload.hashCode()
        val now = System.currentTimeMillis()
        val prev = seenMessages[id]
        if (prev != null && now - prev < 60_000L) return false
        seenMessages[id] = now
        return true
    }
}

class ChatTrackerService : AccessibilityService() {

    private companion object {
        const val MAX_SESSION_SECONDS = MAX_STAY_SECONDS
        const val CONTENT_THROTTLE_MS = 800L
        const val MAX_TREE_NODES = 1500
        const val MAX_TREE_DEPTH = 20
        // 連續幾次抓不到標題才算真的離開。單次抓不到多半只是畫面重繪，
        // 立刻結算會把一段連續停留切成無數碎片，每片不足 1 秒就被丟掉。
        const val MISS_TOLERANCE = 3
        // 這段時間內回到同一個聊天室視為同一段停留，接續累加而不是重開一段
        const val RESUME_WINDOW_MS = 25_000L
    }

    private var currentChatName: String? = null
    private var currentAppName: String? = null
    private var chatStartTime: Long = 0
    private var lastContentScan: Long = 0
    private var missStreak = 0
    private var lastCandidate: String? = null
    private var screenReceiver: BroadcastReceiver? = null

    // 暫存的一段停留，等確定不會回來才寫入
    private var pendingName: String? = null
    private var pendingApp: String? = null
    private var pendingSeconds: Long = 0
    private val handler = Handler(Looper.getMainLooper())
    private val flushRunnable = Runnable { flushPending() }

    private val targetApps = mapOf(
        "jp.naver.line.android" to "LINE", "com.facebook.orca" to "Messenger",
        "com.instagram.android" to "Instagram", "com.instagram.barcelona" to "Threads",
        "com.whatsapp" to "WhatsApp", "org.telegram.messenger" to "Telegram",
        "com.google.android.dialer" to "電話", "com.samsung.android.incallui" to "電話"
    )

    // 用「view id 的後綴」比對，不寫死完整 id。
    // IG / Threads 幾乎每次改版都會換 id 前綴，寫死等於下次改版就失效。
    private val titleIdSuffixes = setOf(
        "header_title", "chathistory_title", "chat_title", "chat_ui_header_title",
        "thread_title", "thread_title_name", "thread_title_textview", "direct_thread_title",
        "action_bar_title", "action_bar_textview_title", "toolbar_title",
        "conversation_contact_name", "contactgrid_contact_name",
        "igds_headline_headline", "title", "name"
    )

    private val ignoreTitles = setOf(
        "LINE", "主頁", "聊天", "通話", "Instagram", "Messenger", "Threads", "WhatsApp", "Telegram",
        "搜尋", "設定", "動態", "貼文", "留言", "分享", "限時動態", "觀看完整 Reel",
        "更多選項", "應用程式圖示", "查看洞察報告", "新增", "相機", "通知", "訊息", "收件匣",
        "Direct", "Chats", "Search", "Home",
        // 使用者實際回報混進聯絡人清單的介面文字
        "返回", "關閉", "全部", "更多", "編輯", "取消", "確定", "完成", "刪除", "儲存",
        "新訊息", "串文", "探索", "個人檔案", "珍藏", "摘要", "訊息要求", "已傳送",
        "連續短片", "Reels", "reels", "Explore", "Profile", "Back", "Close", "All", "More"
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        DataRepository.init(applicationContext)
        DataRepository.beat()
        DataRepository.logDetect("無障礙服務已連線")
        // 關螢幕／解鎖時強制結算，避免整夜掛在聊天室
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) { endTrackingNow() }
        }
        screenReceiver = receiver
        runCatching { ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED) }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val appName = targetApps[event.packageName?.toString()]

        // 離開目標 App：先暫停並排程結算，短時間內切回來還能接續同一段
        if (appName == null) {
            pauseTracking()
            return
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> Unit
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                val now = SystemClock.elapsedRealtime()
                if (now - lastContentScan < CONTENT_THROTTLE_MS) return
                lastContentScan = now
            }
            else -> return
        }

        val root = rootInActiveWindow ?: return
        val hit = scanScreen(root)

        val name = hit.title?.trim()

        // 採信條件（任一成立）：
        //  1. 標題來自已知的 view id —— 高信賴，直接採信
        //  2. 畫面上有輸入框 —— 典型的聊天室特徵
        //  3. 連續兩次掃到同一個標題 —— 動態瀏覽（動態牆、探索）的頂端文字會一直變，
        //     穩定不變才可能是聊天室標題。這條是為了 IG/Threads：它們的輸入框常包在
        //     摺疊容器裡或未取得焦點時不回報 isEditable，只靠前兩條會永遠判定不在聊天室。
        val stable = name != null && name == lastCandidate
        val trustworthy = hit.fromViewId || appName == "電話" || hit.hasEditText || stable
        val valid = !name.isNullOrEmpty() && name.length <= 60 && name !in ignoreTitles && trustworthy
        lastCandidate = name

        if (!valid) {
            missStreak++
            DataRepository.logDetect(
                "$appName 未採用 · 標題=${name ?: "(抓不到)"} · viewId=${hit.fromViewId} · 輸入框=${hit.hasEditText}"
            )
            if (missStreak >= MISS_TOLERANCE) pauseTracking()
            return
        }

        missStreak = 0
        DataRepository.beat()
        DataRepository.logDetect("$appName 記錄中 · $name · viewId=${hit.fromViewId} · 輸入框=${hit.hasEditText}")
        enterChat(name!!, appName)
    }

    private class ScanResult(val title: String?, val hasEditText: Boolean, val fromViewId: Boolean)

    /**
     * 一次走訪就同時找出「聊天室標題」與「是否有輸入框」。
     * 標題優先用 view id 後綴比對；找不到就退而取畫面頂端 20% 內最上方的文字，
     * 這樣即使 App 改版換掉 id 仍然抓得到。
     */
    private fun scanScreen(root: AccessibilityNodeInfo): ScanResult {
        @Suppress("NAME_SHADOWING")
        val screen = Rect()
        root.getBoundsInScreen(screen)
        val topLimit = screen.top + (screen.height() * 0.20f).toInt()

        var byId: String? = null
        var fallback: String? = null
        var fallbackTop = Int.MAX_VALUE
        var hasEdit = false
        var visited = 0

        val bounds = Rect()
        // ⚠️ 必須用 BFS（removeFirst）。標題列在節點樹的淺層，訊息列表則又深又寬；
        // 用 DFS 會先鑽進訊息列表，節點預算在碰到標題前就耗盡 → 抓不到聊天室名稱。
        val queue = ArrayDeque<Pair<AccessibilityNodeInfo, Int>>()
        queue.addLast(root to 0)

        while (queue.isNotEmpty() && visited < MAX_TREE_NODES) {
            val (node, depth) = queue.removeFirst()
            visited++
            if (depth > MAX_TREE_DEPTH) continue

            if (!hasEdit && (node.isEditable || node.className?.toString()?.contains("EditText") == true)) {
                hasEdit = true
            }

            // 有些 App 的標題只放在 contentDescription
            val text = (node.text?.toString() ?: node.contentDescription?.toString())?.trim()
            if (!text.isNullOrBlank() && text.length <= 60) {
                if (byId == null) {
                    val suffix = node.viewIdResourceName?.substringAfterLast('/')
                    if (suffix != null && suffix in titleIdSuffixes && looksLikeTitle(text)) byId = text
                }
                if (byId == null && looksLikeTitle(text)) {
                    node.getBoundsInScreen(bounds)
                    if (bounds.top in screen.top..topLimit && bounds.height() > 0 && bounds.top < fallbackTop) {
                        fallbackTop = bounds.top
                        fallback = text
                    }
                }
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.addLast(it to depth + 1) }
            }
            if (byId != null && hasEdit) break
        }

        return ScanResult(byId ?: fallback, hasEdit, byId != null)
    }

    /** 排除時鐘、純數字、單一符號等明顯不是聊天室名稱的文字 */
    /**
     * 聊天室標題長什麼樣子。
     *
     * ⚠️ IG / Threads 的動態牆會把貼文內文、按讚數、大頭貼的 contentDescription
     * 一起丟進節點樹，這些以前全被當成聊天室名字建成聯絡人。
     * 只靠事後刪除沒有用（下次滑動態又會重建），必須在這裡擋掉。
     */
    private fun looksLikeTitle(text: String): Boolean {
        if (text.length < 2) return false
        if (text in ignoreTitles) return false
        // 共用同一套「非真人」規則，設定頁刪過的東西才不會一直長回來
        if (isNonPersonContact(text)) return false
        if (Regex("^\\d{1,2}:\\d{2}").containsMatchIn(text)) return false
        if (Regex("^[\\d\\s%+\\-.,]+$").matches(text)) return false
        // 按讚數／觀看數／留言數這類數字標籤
        if (Regex("^[\\d.,]+\\s*(萬|k|K|M)?\\s*(個)?\\s*(讚|留言|次|views?|likes?|comments?)", RegexOption.IGNORE_CASE)
                .containsMatchIn(text)) return false
        // 大頭貼／頭像的 contentDescription 幾乎都是「XXX 的大頭貼照」
        if (Regex("(的大頭貼|大頭貼照|個人檔案相片|profile picture|avatar)", RegexOption.IGNORE_CASE)
                .containsMatchIn(text)) return false
        // 貼文內文通常帶句號、換行或很長；聊天室標題是人名，不會這樣
        if (text.length > 28) return false
        if (text.contains('\n')) return false
        // 帶連結、hashtag 的一定是貼文
        if (text.contains("http", ignoreCase = true) || text.contains('#')) return false
        return true
    }

    private fun enterChat(chatName: String, appName: String) {
        if (currentChatName == chatName && currentAppName == appName) return

        stashCurrent()
        handler.removeCallbacks(flushRunnable)

        if (pendingName == chatName && pendingApp == appName) {
            // 短時間內回到同一個聊天室：接續先前累積的秒數
            chatStartTime = System.currentTimeMillis() - pendingSeconds * 1000L
            pendingName = null; pendingApp = null; pendingSeconds = 0
        } else {
            flushPending()
            chatStartTime = System.currentTimeMillis()
        }
        currentChatName = chatName
        currentAppName = appName
    }

    /** 把目前這段移到暫存區（還不寫入） */
    private fun stashCurrent() {
        val name = currentChatName ?: return
        val app = currentAppName
        val start = chatStartTime
        currentChatName = null; currentAppName = null; chatStartTime = 0
        if (start <= 0L) return

        val elapsed = (System.currentTimeMillis() - start) / 1000L
        if (elapsed < 0L) return

        if (pendingName == name && pendingApp == app) {
            pendingSeconds += elapsed
        } else {
            flushPending()
            pendingName = name; pendingApp = app; pendingSeconds = elapsed
        }
    }

    /** 暫停：移到暫存並排程結算，期間切回同一個聊天室可接續 */
    private fun pauseTracking() {
        stashCurrent()
        missStreak = 0
        if (pendingName != null) {
            handler.removeCallbacks(flushRunnable)
            handler.postDelayed(flushRunnable, RESUME_WINDOW_MS)
        }
    }

    /** 立即結束並寫入（關螢幕、服務停止） */
    private fun endTrackingNow() {
        stashCurrent()
        handler.removeCallbacks(flushRunnable)
        flushPending()
        missStreak = 0
    }

    private fun flushPending() {
        val name = pendingName
        val app = pendingApp
        val secs = pendingSeconds
        pendingName = null; pendingApp = null; pendingSeconds = 0
        if (name == null || secs < 1L) return
        // 超過上限視為背景掛機，直接丟棄（記成上限值一樣會虛胖）
        if (secs > MAX_SESSION_SECONDS) {
            DataRepository.logDetect("丟棄 · $name 累積 ${secs}s 超過上限")
            return
        }
        DataRepository.logDetect("寫入 · $name ${secs}s（$app）")

        DataRepository.addInteraction(
            InteractionRecord(
                rawSenderName = name,
                appName = app ?: "Unknown",
                isIncoming = false,
                wordCount = 0,
                durationSeconds = secs.toInt()
            )
        )
    }

    override fun onInterrupt() { endTrackingNow() }

    override fun onUnbind(intent: Intent?): Boolean {
        endTrackingNow()
        screenReceiver?.let { runCatching { unregisterReceiver(it) } }
        screenReceiver = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        endTrackingNow()
        screenReceiver?.let { runCatching { unregisterReceiver(it) } }
        screenReceiver = null
        super.onDestroy()
    }
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

    // 預設看「單週」。預設單日的話，只要今天還沒互動就整片空白，
    // 一開 App 什麼都看不到。
    var timeFilter = MutableStateFlow(DateFilter(FilterType.WEEK, System.currentTimeMillis()))
    // 預設依訊息數排序：停留時間需要無障礙服務，訊息數涵蓋面較廣
    var sortType = MutableStateFlow(SortType.MESSAGES)
    var categoryFilter = MutableStateFlow<ContactCategory?>(null)
    var appFilter = MutableStateFlow<String?>(null)
    var groupFilter = MutableStateFlow(GroupFilter.ALL)

    var contactsSearchQuery = MutableStateFlow("")
    var contactsSortType = MutableStateFlow(ContactSortType.INTERACTIONS)
    var contactsCatFilter = MutableStateFlow<ContactCategory?>(null)
    var contactsAppFilter = MutableStateFlow<String?>(null)

    val autoBackupEnabled = DataRepository.autoBackupEnabled
    val lastAutoBackupTime = DataRepository.lastAutoBackupTime

    fun completeOnboarding() { DataRepository.completeOnboarding() }
    fun setWeekStartDay(day: Int) { DataRepository.setWeekStartDay(day) }
    fun checkAutoBackup(context: Context) { DataRepository.performAutoBackupIfNeeded(context) }
    fun setAutoBackup(enabled: Boolean, pwd: String) { DataRepository.setAutoBackup(enabled, pwd) }
    fun restoreAutoBackup(context: Context): Boolean { return DataRepository.restoreAutoBackup(context) }

    private fun startOfDay(ts: Long): Long = Calendar.getInstance().apply {
        timeInMillis = ts
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun getFilteredRecords(records: List<InteractionRecord>, filter: DateFilter): List<InteractionRecord> {
        return when (filter.type) {
            FilterType.SPECIFIC_DAY -> {
                val start = startOfDay(filter.timestamp)
                val end = start + 86_399_999L
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

        val totalDurSecs = filtered.sumOf { it.durationSeconds.toLong() }
        val totalMsg = filtered.count { it.isIncoming }

        if (totalMsg == 0 && totalDurSecs > 60) return SocialPersona.ALL_TYPES[1]
        if (totalMsg == 0) return SocialPersona.ALL_TYPES[2]

        val avgSecsPerMsg = if (totalMsg > 0) totalDurSecs / totalMsg else totalDurSecs
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
            AppUsageStat(
                appName = appName,
                totalDurationSeconds = appRecords.sumOf { it.durationSeconds.toLong() },
                incomingCount = appRecords.count { it.isIncoming }
            )
        }.filter { it.totalDurationSeconds > 0 || it.incomingCount > 0 }
            .sortedByDescending { it.totalDurationSeconds + (it.incomingCount * 5L) }
    }

    fun getCategoryStats(records: List<InteractionRecord>, filter: DateFilter, catsMap: Map<String, String>, currentAliases: Map<String, String>, availableCats: List<ContactCategory>): List<CategoryStat> {
        return getFilteredRecords(records, filter).groupBy {
            val unifiedName = currentAliases[it.rawSenderName] ?: it.rawSenderName
            val catId = catsMap[unifiedName] ?: ContactCategory.OTHER.id
            availableCats.find { c -> c.id == catId } ?: ContactCategory.OTHER
        }.map { (category, catRecords) ->
            CategoryStat(
                category = category,
                totalDurationSeconds = catRecords.sumOf { it.durationSeconds.toLong() },
                incomingCount = catRecords.count { it.isIncoming }
            )
        }.filter { it.totalDurationSeconds > 0 || it.incomingCount > 0 }.sortedByDescending { it.totalDurationSeconds }
    }

    /**
     * 唯一的統計建構器。
     * 總覽與排行分頁共用同一套規則，數字才會一致。
     */
    private fun buildStats(
        records: List<InteractionRecord>,
        catsMap: Map<String, String>,
        currentAliases: Map<String, String>,
        availableCats: List<ContactCategory>
    ): List<SenderStat> {
        return records.groupBy { currentAliases[it.rawSenderName] ?: it.rawSenderName }
            .map { (unifiedName, userRecords) ->
                // 舊資料沒有 isCall 欄位，靠 appName 補判斷
                val calls = userRecords.filter { it.isCall || it.isMissedCall || it.appName == "電話" }
                val msgIncoming = userRecords.filter { it.isIncoming && !it.isCall && !it.isMissedCall && it.appName != "電話" }
                val avgWords = if (msgIncoming.isNotEmpty()) msgIncoming.sumOf { it.wordCount } / msgIncoming.size else 0
                val catId = catsMap[unifiedName] ?: ContactCategory.OTHER.id
                val categoryObj = availableCats.find { it.id == catId } ?: ContactCategory.OTHER

                val subSenderCounts = msgIncoming.mapNotNull { it.subSender }
                    .filter { it.isNotBlank() && it != unifiedName }
                    .groupingBy { it }.eachCount()

                SenderStat(
                    unifiedName = unifiedName,
                    recordCount = userRecords.size,
                    incomingCount = userRecords.count { it.isIncoming },
                    msgCount = msgIncoming.size,
                    callCount = calls.size,
                    incomingWords = msgIncoming.sumOf { it.wordCount },
                    avgWordsPerMessage = avgWords,
                    totalDurationSeconds = userRecords.sumOf { it.durationSeconds.toLong() },
                    callDurationSeconds = calls.sumOf { it.durationSeconds.toLong() },
                    appNames = userRecords.map { it.appName }.toSet(),
                    category = categoryObj,
                    isGroup = userRecords.any { it.isGroup },
                    subSenders = subSenderCounts.toList().sortedByDescending { it.second }.toMap()
                )
            }.filter { it.totalDurationSeconds > 0 || it.recordCount > 0 }
    }

    /**
     * 「互動對象」用的排行。
     *
     * ⚠️ 一定要排除系統通知與非真人來源：這份結果會被總覽的 Top 5、
     * 「訊息最多／停留最久」KPI、集中度、關係變化共用。以前沒濾，
     * 於是「新訊息」「串文」這種東西會擠進排行、甚至變成停留最久的第一名。
     *
     * 同時把「正規化後同名」的項目合併（差一個空白或 emoji 的重複聯絡人）。
     */
    fun getDynamicLeaderboard(baseRecords: List<InteractionRecord>, catsMap: Map<String, String>, currentAliases: Map<String, String>, availableCats: List<ContactCategory>): List<SenderStat> {
        val raw = buildStats(baseRecords, catsMap, currentAliases, availableCats)
            .filter { !isExcludedFromStats(it.unifiedName, catsMap) }
        return mergeDuplicateStats(raw)
            .sortedByDescending { it.totalDurationSeconds + (it.incomingCount * 60L) }
    }

    /** 名稱正規化後相同就視為同一人（「小明 」與「小明✨」） */
    fun mergeDuplicateStats(stats: List<SenderStat>): List<SenderStat> {
        val byNorm = stats.groupBy { normalizeContactName(it.unifiedName).ifBlank { it.unifiedName } }
        if (byNorm.size == stats.size) return stats
        return byNorm.values.map { group ->
            if (group.size == 1) group[0]
            else {
                // 保留互動最多的那個名字當代表
                val head = group.maxByOrNull { it.incomingCount } ?: group[0]
                head.copy(
                    recordCount = group.sumOf { it.recordCount },
                    incomingCount = group.sumOf { it.incomingCount },
                    msgCount = group.sumOf { it.msgCount },
                    callCount = group.sumOf { it.callCount },
                    incomingWords = group.sumOf { it.incomingWords },
                    totalDurationSeconds = group.sumOf { it.totalDurationSeconds },
                    callDurationSeconds = group.sumOf { it.callDurationSeconds },
                    appNames = group.flatMap { it.appNames }.toSet(),
                    isGroup = group.any { it.isGroup }
                )
            }
        }
    }

    fun getLeaderboard(
        records: List<InteractionRecord>, catsMap: Map<String, String>, currentAliases: Map<String, String>,
        availableCats: List<ContactCategory>, filter: DateFilter, sort: SortType,
        catFilter: ContactCategory?, appFlt: String?, groupFlt: GroupFilter = GroupFilter.ALL
    ): List<SenderStat> {
        val scoped = getFilteredRecords(records, filter).let { if (appFlt != null) it.filter { r -> r.appName == appFlt } else it }
        val baseStats = buildStats(scoped, catsMap, currentAliases, availableCats)

        val filteredByCategory = if (catFilter != null) {
            baseStats.filter { it.category.id == catFilter.id }
        } else {
            // 預設不顯示系統通知，避免佔滿排行
            baseStats.filter { it.category.id != ContactCategory.SYSTEM.id }
        }

        val filteredByGroup = when (groupFlt) {
            GroupFilter.ALL -> filteredByCategory
            GroupFilter.PERSON -> filteredByCategory.filter { !it.isGroup }
            GroupFilter.GROUP -> filteredByCategory.filter { it.isGroup }
        }

        return if (sort == SortType.TIME) {
            filteredByGroup.sortedByDescending { it.totalDurationSeconds }
        } else {
            // 互動頻率＝真正收到的訊息＋通話數
            filteredByGroup.sortedByDescending { it.msgCount + it.callCount }
        }
    }

    /** 星期 x 小時 的互動熱力圖（0=週日） */
    fun getWeekHourHeatmap(records: List<InteractionRecord>, filter: DateFilter): Array<IntArray> {
        val grid = Array(7) { IntArray(24) }
        val cal = Calendar.getInstance()
        getFilteredRecords(records, filter).forEach {
            if (!it.isIncoming && it.durationSeconds <= 0) return@forEach
            cal.timeInMillis = it.timestamp
            val d = cal.get(Calendar.DAY_OF_WEEK) - 1
            val h = cal.get(Calendar.HOUR_OF_DAY)
            if (d in 0..6 && h in 0..23) grid[d][h]++
        }
        return grid
    }

    // ══════════════ 深度分析 ══════════════

    /** 目前篩選對應的時間範圍；「全部」回 null */
    fun rangeOf(filter: DateFilter, weekStart: Int): Pair<Long, Long>? = when (filter.type) {
        FilterType.SPECIFIC_DAY -> startOfDay(filter.timestamp).let { it to it + 86_399_999L }
        FilterType.WEEK -> getStartOfWeekTimestamp(filter.timestamp, weekStart) to getEndOfWeekTimestamp(filter.timestamp, weekStart)
        FilterType.ALL -> null
    }

    /** 逐日趨勢。「全部」時取最近 30 天，避免圖表被拉到看不清。 */
    fun getDailyTrend(records: List<InteractionRecord>, filter: DateFilter, weekStart: Int): List<TrendPoint> {
        if (records.isEmpty()) return emptyList()
        val range = rangeOf(filter, weekStart)
        val end = range?.second ?: (records.maxOf { it.timestamp })
        val start = range?.first ?: maxOf(records.minOf { it.timestamp }, end - 29L * 86_400_000L)
        val dayStart = startOfDay(start)
        val days = (((startOfDay(end) - dayStart) / 86_400_000L) + 1).toInt().coerceIn(1, 92)

        val dur = LongArray(days)
        val msg = IntArray(days)
        records.forEach { r ->
            val idx = ((startOfDay(r.timestamp) - dayStart) / 86_400_000L).toInt()
            if (idx in 0 until days) {
                dur[idx] += r.durationSeconds.toLong()
                if (r.isIncoming) msg[idx]++
            }
        }
        val cal = Calendar.getInstance()
        return (0 until days).map { i ->
            val ts = dayStart + i * 86_400_000L
            cal.timeInMillis = ts
            TrendPoint("${cal.get(Calendar.MONTH) + 1}/${cal.get(Calendar.DAY_OF_MONTH)}", ts, dur[i], msg[i])
        }
    }

    /** 本期與「上一個同長度區間」的對比 */
    fun getPeriodDeltas(records: List<InteractionRecord>, filter: DateFilter, weekStart: Int, aliasMap: Map<String, String>): Triple<PeriodDelta, PeriodDelta, PeriodDelta>? {
        val range = rangeOf(filter, weekStart) ?: return null
        val span = range.second - range.first + 1
        val prevStart = range.first - span
        val prevEnd = range.first - 1

        var curDur = 0L; var curMsg = 0L
        var prevDur = 0L; var prevMsg = 0L
        val curPeople = HashSet<String>(); val prevPeople = HashSet<String>()
        records.forEach { r ->
            val name = aliasMap[r.rawSenderName] ?: r.rawSenderName
            when (r.timestamp) {
                in range.first..range.second -> { curDur += r.durationSeconds; if (r.isIncoming) curMsg++; curPeople.add(name) }
                in prevStart..prevEnd -> { prevDur += r.durationSeconds; if (r.isIncoming) prevMsg++; prevPeople.add(name) }
            }
        }
        return Triple(
            PeriodDelta(curDur, prevDur),
            PeriodDelta(curMsg, prevMsg),
            PeriodDelta(curPeople.size.toLong(), prevPeople.size.toLong())
        )
    }

    /**
     * 社交集中度。gini 為基尼係數 x100：0 = 時間平均分給每個人，
     * 100 = 全部集中在一個人身上。
     */
    fun getConcentration(stats: List<SenderStat>): ConcentrationStat {
        val vals = stats.map { it.totalDurationSeconds + it.msgCount * 60L }.filter { it > 0 }.sorted()
        val n = vals.size
        if (n == 0) return ConcentrationStat(0, 0, 0, 0)
        val total = vals.sum().toDouble()
        val desc = vals.reversed()
        val top1 = ((desc[0] / total) * 100).toInt()
        val top3 = ((desc.take(3).sum() / total) * 100).toInt()
        var cum = 0.0
        vals.forEachIndexed { i, v -> cum += (2.0 * (i + 1) - n - 1) * v }
        val gini = if (n > 1 && total > 0) ((cum / (n * total)) * 100).toInt().coerceIn(0, 100) else 0
        return ConcentrationStat(n, top1, top3, gini)
    }

    /** 本期新出現的人，以及已沉寂（超過 14 天沒互動）的人 */
    fun getChurn(records: List<InteractionRecord>, aliasMap: Map<String, String>, filter: DateFilter, weekStart: Int): ChurnStat {
        if (records.isEmpty()) return ChurnStat(emptyList(), emptyList())
        val range = rangeOf(filter, weekStart)
        val now = System.currentTimeMillis()

        val firstSeen = HashMap<String, Long>()
        val lastSeen = HashMap<String, Long>()
        records.forEach { r ->
            val name = aliasMap[r.rawSenderName] ?: r.rawSenderName
            // 「新朋友／久沒聯絡」裡混進系統通知完全沒有意義
            if (isNonPersonContact(name)) return@forEach
            firstSeen[name] = minOf(firstSeen[name] ?: Long.MAX_VALUE, r.timestamp)
            lastSeen[name] = maxOf(lastSeen[name] ?: 0L, r.timestamp)
        }

        val newcomers = if (range == null) emptyList() else
            firstSeen.filter { it.value in range.first..range.second }.keys.sortedBy { firstSeen[it] }.takeLast(20).reversed()

        val dormant = lastSeen
            .mapNotNull { (name, ts) ->
                val days = ((now - ts) / 86_400_000L).toInt()
                if (days in 14..365) name to days else null
            }
            .sortedByDescending { it.second }
            .take(20)

        return ChurnStat(newcomers, dormant)
    }

    /** 深夜社交佔比（22:00–04:59） */
    fun getNightOwlPct(records: List<InteractionRecord>, filter: DateFilter): Int {
        val f = getFilteredRecords(records, filter)
        if (f.isEmpty()) return 0
        val cal = Calendar.getInstance()
        val night = f.count { cal.timeInMillis = it.timestamp; cal.get(Calendar.HOUR_OF_DAY).let { h -> h >= 22 || h < 5 } }
        return (night * 100) / f.size
    }

    /** 每位聯絡人的連續互動天數（到今天為止） */
    fun getStreaks(records: List<InteractionRecord>, aliasMap: Map<String, String>): List<Pair<String, Int>> {
        if (records.isEmpty()) return emptyList()
        val byPerson = HashMap<String, MutableSet<Long>>()
        records.forEach { r ->
            byPerson.getOrPut(aliasMap[r.rawSenderName] ?: r.rawSenderName) { HashSet() }.add(startOfDay(r.timestamp))
        }
        val today = startOfDay(System.currentTimeMillis())
        return byPerson.mapNotNull { (name, days) ->
            if (!days.contains(today) && !days.contains(today - 86_400_000L)) return@mapNotNull null
            var streak = 0
            var cursor = if (days.contains(today)) today else today - 86_400_000L
            while (days.contains(cursor)) { streak++; cursor -= 86_400_000L }
            if (streak >= 2) name to streak else null
        }.sortedByDescending { it.second }.take(10)
    }

    /** 近 N 週的每日活躍度，用於貢獻圖 */
    fun getContributionMap(records: List<InteractionRecord>, weeks: Int = 17): List<Pair<Long, Int>> {
        val today = startOfDay(System.currentTimeMillis())
        val days = weeks * 7
        val start = today - (days - 1L) * 86_400_000L
        val counts = IntArray(days)
        records.forEach { r ->
            val idx = ((startOfDay(r.timestamp) - start) / 86_400_000L).toInt()
            if (idx in 0 until days) counts[idx]++
        }
        return (0 until days).map { (start + it * 86_400_000L) to counts[it] }
    }

    fun getCallSummary(records: List<InteractionRecord>, filter: DateFilter): CallSummary {
        val calls = getFilteredRecords(records, filter).filter { it.isCall || it.isMissedCall || it.appName == "電話" }
        val missed = calls.count { it.isMissedCall }
        val answeredCalls = calls.filter { !it.isMissedCall }
        // 只有真的帶秒數的才能拿來算平均
        val timedCalls = answeredCalls.filter { it.durationSeconds > 0 }
        return CallSummary(
            total = calls.size,
            answered = answeredCalls.size,
            missed = missed,
            totalSeconds = timedCalls.sumOf { it.durationSeconds.toLong() },
            timed = timedCalls.size
        )
    }

    /** 收到訊息的字數分佈 */
    fun getLengthBuckets(records: List<InteractionRecord>, filter: DateFilter): List<Triple<String, Int, Float>> {
        val msgs = getFilteredRecords(records, filter).filter { it.isIncoming && !it.isCall && it.wordCount > 0 }
        if (msgs.isEmpty()) return emptyList()
        val buckets = intArrayOf(0, 0, 0, 0, 0)
        msgs.forEach {
            val i = when {
                it.wordCount <= 5 -> 0
                it.wordCount <= 15 -> 1
                it.wordCount <= 40 -> 2
                it.wordCount <= 100 -> 3
                else -> 4
            }
            buckets[i]++
        }
        val labels = listOf("1-5 字", "6-15 字", "16-40 字", "41-100 字", "100+ 字")
        val total = msgs.size.toFloat()
        return labels.mapIndexed { i, l -> Triple(l, buckets[i], buckets[i] / total) }
    }

    /** 同時使用多個平台聯絡的人 */
    fun getCrossPlatform(records: List<InteractionRecord>, aliasMap: Map<String, String>): List<Pair<String, Int>> {
        val m = HashMap<String, MutableSet<String>>()
        records.forEach { r ->
            m.getOrPut(aliasMap[r.rawSenderName] ?: r.rawSenderName) { mutableSetOf() }.add(r.appName)
        }
        return m.filter { it.value.size >= 2 }.map { it.key to it.value.size }
            .sortedByDescending { it.second }.take(10)
    }

    /** 本期有互動的天數、最長與目前連續天數 */
    fun getCoverage(records: List<InteractionRecord>, filter: DateFilter, weekStart: Int): CoverageStat {
        if (records.isEmpty()) return CoverageStat(0, 0, 0, 0)
        val range = rangeOf(filter, weekStart)
        val scoped = if (range == null) records else records.filter { it.timestamp in range.first..range.second }
        val activeDays = scoped.map { startOfDay(it.timestamp) }.toSortedSet()
        val totalDays = if (range != null)
            (((startOfDay(range.second) - startOfDay(range.first)) / 86_400_000L) + 1).toInt()
        else if (records.isEmpty()) 0
        else (((startOfDay(records.maxOf { it.timestamp }) - startOfDay(records.minOf { it.timestamp })) / 86_400_000L) + 1).toInt()

        var longest = 0; var run = 0; var prev = -1L
        activeDays.forEach { d ->
            run = if (prev >= 0 && d - prev == 86_400_000L) run + 1 else 1
            if (run > longest) longest = run
            prev = d
        }
        val today = startOfDay(System.currentTimeMillis())
        var current = 0
        var cursor = if (activeDays.contains(today)) today else today - 86_400_000L
        while (activeDays.contains(cursor)) { current++; cursor -= 86_400_000L }
        return CoverageStat(activeDays.size, totalDays.coerceAtLeast(activeDays.size), longest, current)
    }

    fun getGroupSplit(records: List<InteractionRecord>, filter: DateFilter): GroupSplit {
        val f = getFilteredRecords(records, filter)
        var gd = 0L; var pd = 0L; var gm = 0; var pm = 0
        f.forEach {
            if (it.isGroup) { gd += it.durationSeconds; if (it.isIncoming) gm++ }
            else { pd += it.durationSeconds; if (it.isIncoming) pm++ }
        }
        return GroupSplit(gd, pd, gm, pm)
    }

    // ══════════════ 一次算完的資料包 ══════════════
    // 舊版在 composition 期間同步跑十幾次全表掃描（每次都重新 getFilteredRecords），
    // 資料量大時直接凍住畫面。改成一次算完並丟到背景執行緒。

    data class OverviewBundle(
        val appStats: List<AppUsageStat> = emptyList(),
        val timeStats: List<TimeOfDayStat> = emptyList(),
        val persona: SocialPersona = SocialPersona.ALL_TYPES[0],
        val leaderboard: List<SenderStat> = emptyList(),
        val catStats: List<CategoryStat> = emptyList(),
        val heatmap: Array<IntArray> = Array(7) { IntArray(24) },
        val scoped: List<InteractionRecord> = emptyList(),
        val ready: Boolean = false
    )

    fun buildOverview(
        records: List<InteractionRecord>, filter: DateFilter, weekStart: Int,
        catsMap: Map<String, String>, aliasMap: Map<String, String>, cats: List<ContactCategory>
    ): OverviewBundle {
        val scoped = getFilteredRecords(records, filter)
        val all = DateFilter(FilterType.ALL)
        return OverviewBundle(
            appStats = getAppUsageStats(scoped, all),
            timeStats = getTimeOfDayStats(scoped, all),
            persona = getSocialPersona(scoped, all),
            leaderboard = getDynamicLeaderboard(scoped, catsMap, aliasMap, cats),
            catStats = getCategoryStats(scoped, all, catsMap, aliasMap, cats),
            heatmap = getWeekHourHeatmap(scoped, all),
            scoped = scoped,
            ready = true
        )
    }

    data class ContactInsight(
        val msgCount: Int = 0,
        val words: Long = 0L,
        val avgWords: Int = 0,
        val callCount: Int = 0,
        val missedCalls: Int = 0,
        val callSeconds: Long = 0L,
        val staySeconds: Long = 0L,
        val firstSeen: Long = 0L,
        val lastSeen: Long = 0L,
        val activeDays: Int = 0,
        val spanDays: Int = 0,
        val longestStreak: Int = 0,
        val daysSinceLast: Int = 0,
        val sharePct: Int = 0,
        val rank: Int = 0,
        val totalContacts: Int = 0,
        val topHour: Int = -1,
        val topWeekday: Int = -1,
        val platforms: List<Pair<String, Int>> = emptyList(),
        val weekly: List<Pair<String, Int>> = emptyList(),
        val hourly: Array<HourlyStat> = Array(24) { HourlyStat(it) },
        val ready: Boolean = false
    )

    /**
     * 一次算完單一聯絡人的所有指標。
     * 必須在背景執行緒呼叫：這裡會掃兩次全表（全期間 + 本期）。
     */
    fun buildContactInsight(
        records: List<InteractionRecord>, aliasMap: Map<String, String>,
        unifiedName: String, filter: DateFilter
    ): ContactInsight {
        val mine = records.filter { (aliasMap[it.rawSenderName] ?: it.rawSenderName) == unifiedName }
        if (mine.isEmpty()) return ContactInsight(ready = true)

        val scoped = getFilteredRecords(mine, filter)
        val msgs = scoped.filter { it.isIncoming && !it.isCall && !it.isMissedCall }
        val calls = scoped.filter { it.isCall || it.isMissedCall }
        val words = msgs.sumOf { it.wordCount.toLong() }

        // 排名與佔比都只看目前篩選期間，才跟畫面上其他數字一致
        val allScoped = getFilteredRecords(records, filter)
            .filter { it.isIncoming && !it.isCall && !it.isMissedCall }
        val perContact = allScoped.groupingBy { aliasMap[it.rawSenderName] ?: it.rawSenderName }.eachCount()
        val ordered = perContact.entries.sortedByDescending { it.value }
        val rank = ordered.indexOfFirst { it.key == unifiedName } + 1
        val totalMsgs = allScoped.size

        val cal = Calendar.getInstance()
        fun dayKey(ts: Long): Long {
            cal.timeInMillis = ts
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        val days = scoped.map { dayKey(it.timestamp) }.distinct().sorted()
        var longest = 0
        var run = 0
        var prev = 0L
        days.forEach { d ->
            run = if (prev != 0L && d - prev == 86_400_000L) run + 1 else 1
            if (run > longest) longest = run
            prev = d
        }

        val hourly = Array(24) { HourlyStat(it) }
        val byWeekday = IntArray(7)
        scoped.forEach { r ->
            cal.timeInMillis = r.timestamp
            val h = cal.get(Calendar.HOUR_OF_DAY)
            if (r.isIncoming && !r.isCall) hourly[h].msgCount += 1
            hourly[h].durationSeconds += r.durationSeconds.toLong()
            if (r.isMissedCall) hourly[h].missedCalls += 1
            byWeekday[cal.get(Calendar.DAY_OF_WEEK) - 1] += 1
        }

        // 近 8 週趨勢刻意不受日期篩選影響，看的是長期關係變化
        val now = System.currentTimeMillis()
        val weekly = (7 downTo 0).map { back ->
            val end = now - back * 604_800_000L
            val start = end - 604_800_000L
            val label = if (back == 0) "本週" else "-" + back + "w"
            label to mine.count { it.timestamp in start..end && it.isIncoming && !it.isCall }
        }

        val firstTs = mine.minOf { it.timestamp }
        val lastTs = mine.maxOf { it.timestamp }
        return ContactInsight(
            msgCount = msgs.size,
            words = words,
            avgWords = if (msgs.isNotEmpty()) (words / msgs.size).toInt() else 0,
            callCount = calls.size,
            missedCalls = calls.count { it.isMissedCall },
            callSeconds = calls.filter { !it.isMissedCall }.sumOf { it.durationSeconds.toLong() },
            staySeconds = scoped.filter { !it.isCall && !it.isMissedCall }.sumOf { it.durationSeconds.toLong() },
            firstSeen = firstTs,
            lastSeen = lastTs,
            activeDays = days.size,
            spanDays = (((lastTs - firstTs) / 86_400_000L) + 1).toInt().coerceAtLeast(1),
            longestStreak = longest,
            daysSinceLast = ((dayKey(now) - dayKey(lastTs)) / 86_400_000L).toInt().coerceAtLeast(0),
            sharePct = if (totalMsgs > 0) (msgs.size * 100) / totalMsgs else 0,
            rank = rank,
            totalContacts = ordered.size,
            topHour = hourly.maxByOrNull { it.score }?.takeIf { it.score > 0 }?.hour ?: -1,
            topWeekday = byWeekday.indices.maxByOrNull { byWeekday[it] }?.takeIf { byWeekday[it] > 0 } ?: -1,
            platforms = scoped.groupingBy { it.appName }.eachCount().entries
                .sortedByDescending { it.value }.map { it.key to it.value },
            weekly = weekly,
            hourly = hourly,
            ready = true
        )
    }

    data class LeaderBundle(
        val list: List<SenderStat> = emptyList(),
        val apps: List<String> = emptyList(),
        val ready: Boolean = false
    )

    fun buildLeaderboard(
        records: List<InteractionRecord>, catsMap: Map<String, String>, aliasMap: Map<String, String>,
        cats: List<ContactCategory>, filter: DateFilter, sort: SortType,
        catFlt: ContactCategory?, appFlt: String?, groupFlt: GroupFilter
    ): LeaderBundle = LeaderBundle(
        list = getLeaderboard(records, catsMap, aliasMap, cats, filter, sort, catFlt, appFlt, groupFlt),
        apps = records.map { it.appName }.distinct().sorted(),
        ready = true
    )

    data class ContactsBundle(
        val unifiedNames: List<String> = emptyList(),
        val counts: Map<String, Int> = emptyMap(),
        val contactApps: Map<String, Set<String>> = emptyMap(),
        val apps: List<String> = emptyList(),
        val junk: List<String> = emptyList(),
        val similar: List<List<String>> = emptyList(),
        // 歸戶名 -> 被合併進來的原始名稱，給卡片上的「已合併」標示用。
        // 先算好成 Map，否則每張卡片都要 filter 一次全部名稱。
        val rawByUnified: Map<String, List<String>> = emptyMap(),
        val ready: Boolean = false
    )

    fun buildContacts(records: List<InteractionRecord>, aliasMap: Map<String, String>): ContactsBundle {
        val unified = (records.map { aliasMap[it.rawSenderName] ?: it.rawSenderName } + aliasMap.values).distinct()
        val appsOf = HashMap<String, MutableSet<String>>()
        records.forEach { r ->
            appsOf.getOrPut(aliasMap[r.rawSenderName] ?: r.rawSenderName) { mutableSetOf() }.add(r.appName)
        }
        return ContactsBundle(
            unifiedNames = unified,
            counts = records.groupingBy { aliasMap[it.rawSenderName] ?: it.rawSenderName }.eachCount(),
            contactApps = appsOf,
            apps = records.map { it.appName }.distinct().sorted(),
            junk = listJunkContacts(),
            similar = findSimilarGroups(unified),
            rawByUnified = records.map { it.rawSenderName }.distinct()
                .groupBy { aliasMap[it] ?: it },
            ready = true
        )
    }

    data class InsightsBundle(
        val trend: List<TrendPoint> = emptyList(),
        val deltas: Triple<PeriodDelta, PeriodDelta, PeriodDelta>? = null,
        val conc: ConcentrationStat = ConcentrationStat(0, 0, 0, 0),
        val churn: ChurnStat = ChurnStat(emptyList(), emptyList()),
        val nightPct: Int = 0,
        val streaks: List<Pair<String, Int>> = emptyList(),
        val contrib: List<Pair<Long, Int>> = emptyList(),
        val calls: CallSummary = CallSummary(0, 0, 0, 0L),
        val lengths: List<Triple<String, Int, Float>> = emptyList(),
        val cross: List<Pair<String, Int>> = emptyList(),
        val coverage: CoverageStat = CoverageStat(0, 0, 0, 0),
        val split: GroupSplit = GroupSplit(0L, 0L, 0, 0),
        val extras: ExtraStats = ExtraStats(),
        val persona: SocialPersona = SocialPersona.ALL_TYPES[0],
        val ready: Boolean = false
    )

    fun buildInsights(
        records: List<InteractionRecord>, filter: DateFilter, weekStart: Int,
        catsMap: Map<String, String>, aliasMap: Map<String, String>, cats: List<ContactCategory>
    ): InsightsBundle {
        val scoped = getFilteredRecords(records, filter)
        val stats = getDynamicLeaderboard(scoped, catsMap, aliasMap, cats)
        return InsightsBundle(
            trend = getDailyTrend(records, filter, weekStart),
            deltas = getPeriodDeltas(records, filter, weekStart, aliasMap),
            conc = getConcentration(stats),
            churn = getChurn(records, aliasMap, filter, weekStart),
            nightPct = nightPctOf(scoped),
            streaks = getStreaks(records, aliasMap),
            contrib = getContributionMap(records),
            calls = callSummaryOf(scoped),
            lengths = lengthBucketsOf(scoped),
            cross = getCrossPlatform(records, aliasMap),
            coverage = getCoverage(records, filter, weekStart),
            split = groupSplitOf(scoped),
            extras = extraStatsOf(scoped, records, filter, weekStart),
            persona = getSocialPersona(scoped, DateFilter(FilterType.ALL)),
            ready = true
        )
    }

    /** 額外的補充統計 */
    data class ExtraStats(
        val avgMsgPerActiveDay: Int = 0,
        val avgDurPerActiveDay: Long = 0L,
        val busiestDayLabel: String = "—",
        val busiestDayCount: Int = 0,
        val longestSessionSec: Long = 0L,
        val weekdayMsgs: Int = 0,
        val weekendMsgs: Int = 0,
        val avgWordsPerMsg: Int = 0,
        val openedChats: Int = 0
    )

    private fun extraStatsOf(scoped: List<InteractionRecord>, all: List<InteractionRecord>, filter: DateFilter, weekStart: Int): ExtraStats {
        if (scoped.isEmpty()) return ExtraStats()
        val cal = Calendar.getInstance()
        val perDayMsg = HashMap<Long, Int>()
        var weekday = 0; var weekend = 0
        var words = 0L; var msgs = 0
        var longest = 0L
        var opened = 0
        scoped.forEach { r ->
            if (r.isIncoming && !r.isCall) {
                val d = startOfDay(r.timestamp)
                perDayMsg[d] = (perDayMsg[d] ?: 0) + 1
                cal.timeInMillis = r.timestamp
                val dow = cal.get(Calendar.DAY_OF_WEEK)
                if (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) weekend++ else weekday++
                words += r.wordCount; msgs++
            }
            if (r.durationSeconds > 0) {
                opened++
                if (r.durationSeconds > longest) longest = r.durationSeconds.toLong()
            }
        }
        val activeDays = scoped.map { startOfDay(it.timestamp) }.distinct().size.coerceAtLeast(1)
        val busiest = perDayMsg.maxByOrNull { it.value }
        val label = busiest?.let {
            cal.timeInMillis = it.key
            "${cal.get(Calendar.MONTH) + 1}/${cal.get(Calendar.DAY_OF_MONTH)}"
        } ?: "—"
        return ExtraStats(
            avgMsgPerActiveDay = msgs / activeDays,
            avgDurPerActiveDay = scoped.sumOf { it.durationSeconds.toLong() } / activeDays,
            busiestDayLabel = label,
            busiestDayCount = busiest?.value ?: 0,
            longestSessionSec = longest,
            weekdayMsgs = weekday,
            weekendMsgs = weekend,
            avgWordsPerMsg = if (msgs > 0) (words / msgs).toInt() else 0,
            openedChats = opened
        )
    }

    private fun nightPctOf(scoped: List<InteractionRecord>): Int {
        if (scoped.isEmpty()) return 0
        val cal = Calendar.getInstance()
        val night = scoped.count { cal.timeInMillis = it.timestamp; cal.get(Calendar.HOUR_OF_DAY).let { h -> h >= 22 || h < 5 } }
        return (night * 100) / scoped.size
    }

    private fun callSummaryOf(scoped: List<InteractionRecord>): CallSummary {
        val calls = scoped.filter { it.isCall || it.isMissedCall || it.appName == "電話" }
        val missed = calls.count { it.isMissedCall }
        return CallSummary(calls.size, calls.size - missed, missed, calls.sumOf { it.durationSeconds.toLong() })
    }

    private fun lengthBucketsOf(scoped: List<InteractionRecord>): List<Triple<String, Int, Float>> {
        val msgs = scoped.filter { it.isIncoming && !it.isCall && it.wordCount > 0 }
        if (msgs.isEmpty()) return emptyList()
        val buckets = intArrayOf(0, 0, 0, 0, 0)
        msgs.forEach {
            val i = when {
                it.wordCount <= 5 -> 0
                it.wordCount <= 15 -> 1
                it.wordCount <= 40 -> 2
                it.wordCount <= 100 -> 3
                else -> 4
            }
            buckets[i]++
        }
        val labels = listOf("1-5 字", "6-15 字", "16-40 字", "41-100 字", "100+ 字")
        val total = msgs.size.toFloat()
        return labels.mapIndexed { i, l -> Triple(l, buckets[i], buckets[i] / total) }
    }

    private fun groupSplitOf(scoped: List<InteractionRecord>): GroupSplit {
        var gd = 0L; var pd = 0L; var gm = 0; var pm = 0
        scoped.forEach {
            if (it.isGroup) { gd += it.durationSeconds; if (it.isIncoming) gm++ }
            else { pd += it.durationSeconds; if (it.isIncoming) pm++ }
        }
        return GroupSplit(gd, pd, gm, pm)
    }

    fun getHourlyActivityForContact(records: List<InteractionRecord>, aliases: Map<String, String>, unifiedName: String, filter: DateFilter): Array<HourlyStat> {
        val hourlyData = Array(24) { HourlyStat(it) }
        getFilteredRecords(records, filter).filter { (aliases[it.rawSenderName] ?: it.rawSenderName) == unifiedName }.forEach {
            val hour = Calendar.getInstance().apply { timeInMillis = it.timestamp }.get(Calendar.HOUR_OF_DAY)
            hourlyData[hour].durationSeconds += it.durationSeconds.toLong()
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
    fun setGroupFilter(g: GroupFilter) { groupFilter.value = g }
    fun setContactsSearch(q: String) { contactsSearchQuery.value = q }
    fun setContactsSort(type: ContactSortType) { contactsSortType.value = type }
    fun setContactsCatFilter(cat: ContactCategory?) { contactsCatFilter.value = cat }
    fun setContactsAppFilter(app: String?) { contactsAppFilter.value = app }
    fun forceRefresh() { DataRepository.loadData() }
    fun clearInteractions(scope: String) { DataRepository.clearInteractions(scope) }
    fun clearInteractionsByDate(timestamp: Long) { DataRepository.clearInteractionsByDate(timestamp) }
    fun mergeContact(rawName: String, unifiedName: String) { DataRepository.setAlias(rawName, unifiedName) }
    fun renameContact(oldName: String, newName: String) = DataRepository.renameContact(oldName, newName)
    val lastCleanupRemoved = DataRepository.lastCleanupRemoved
    val wordFreq = DataRepository.wordFreq
    fun clearWordFreq() = DataRepository.clearWordFreq()
    fun purgeJunkWords() = DataRepository.purgeJunkWords()
    val lastWordPurge = DataRepository.lastWordPurge
    fun retagSystemContacts() = DataRepository.retagSystemContacts()
    fun previewRetagTargets() = DataRepository.previewRetagTargets()
    val debugLog = DataRepository.debugLog
    fun clearDetectLog() = DataRepository.clearDetectLog()
    val lastServiceBeat = DataRepository.lastServiceBeat
    fun readLastCrash() = DataRepository.readLastCrash()
    fun clearLastCrash() = DataRepository.clearLastCrash()
    fun setCategories(names: Collection<String>, categoryId: String) = DataRepository.updateCategories(names, categoryId)
    fun updateCategoryMeta(id: String, label: String, colorInt: Int, bgColorInt: Int) = DataRepository.updateCategoryMeta(id, label, colorInt, bgColorInt)
    fun deleteCategory(id: String) = DataRepository.deleteCategory(id)
    fun moveCategory(id: String, delta: Int) = DataRepository.moveCategory(id, delta)
    fun addCategory(label: String, colorInt: Int, bgColorInt: Int) {
        val name = label.trim()
        if (name.isEmpty()) return
        DataRepository.addCustomCategory(ContactCategory(UUID.randomUUID().toString(), name, colorInt, bgColorInt))
    }
    fun deleteContacts(names: Set<String>) = DataRepository.deleteContacts(names)
    fun listJunkContacts() = DataRepository.listJunkContacts()
    val undoAvailable = DataRepository.undoAvailable
    val undoLabel = DataRepository.undoLabel
    fun undoLastDestructive() = DataRepository.undoLastDestructive()
    fun refreshUndoState() = DataRepository.refreshUndoState()
    fun importCallLog(context: Context) = DataRepository.importCallLog(context)
    fun deleteRecordsByIds(ids: Set<String>) = DataRepository.deleteRecordsByIds(ids)
    fun findSimilarGroups(names: List<String>) = DataRepository.findSimilarGroups(names)
    fun countAbnormalRecords() = DataRepository.countAbnormalRecords()
    fun removeAbnormalRecords() = DataRepository.removeAbnormalRecords()
    fun buildShareBackupIntent(context: Context) = DataRepository.buildShareBackupIntent(context)
    fun exportBackup(key: String) = DataRepository.exportBackupString(key)
    fun importBackup(str: String, key: String) = DataRepository.importBackupString(str, key)
}

fun getDeviceUsageTimeSec(context: Context, filter: DateFilter, weekStartDay: Int, allRangeStart: Long = 0L): Long {
    if (!hasUsageStatsPermission(context)) return 0L
    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return 0L
    val (start, end) = when (filter.type) {
        FilterType.SPECIFIC_DAY -> {
            val s = Calendar.getInstance().apply {
                timeInMillis = filter.timestamp
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            s to (s + 86_399_999L)
        }
        FilterType.WEEK -> getStartOfWeekTimestamp(filter.timestamp, weekStartDay) to getEndOfWeekTimestamp(filter.timestamp, weekStartDay)
        // 「全部」以最早一筆紀錄為起點
        FilterType.ALL -> {
            if (allRangeStart <= 0L) return 0L
            allRangeStart to System.currentTimeMillis()
        }
    }
    if (end <= start) return 0L

    return runCatching {
        // 注意：`a ?: 0L / 1000L` 會因為 `/` 優先序較高而只套用在預設值上，
        // 導致回傳毫秒卻被當成秒。務必先取值再除。
        val aggregated = usm.queryAndAggregateUsageStats(start, end)
        val totalMs = aggregated?.values?.sumOf { it.totalTimeInForeground } ?: 0L
        totalMs / 1000L
    }.getOrDefault(0L)
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
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        DataRepository.installCrashReporter(applicationContext)
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
        LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.weight(1f)) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = AppColors.BrandPrimary), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
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
                Card(colors = CardDefaults.cardColors(containerColor = AppColors.Surface), border = BorderStroke(1.dp, AppColors.BorderLight), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)) {
                                Box(modifier = Modifier.background(AppColors.FamilyBg, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) { Text("家人", color = AppColors.FamilyText, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("老媽", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text("2h 15m", fontSize = 18.sp, fontWeight = FontWeight.Black, color = AppColors.TextPrimary, maxLines = 1, softWrap = false)
                        }
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(AppColors.BorderLight)) {
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
                Card(colors = CardDefaults.cardColors(containerColor = AppColors.Surface), border = BorderStroke(1.dp, AppColors.BorderLight), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)) {
                                Box(modifier = Modifier.background(AppColors.GroupBg, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) { Text("群組", color = AppColors.GroupText, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("大學死黨群", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text("1h 05m", fontSize = 18.sp, fontWeight = FontWeight.Black, color = AppColors.TextPrimary, maxLines = 1, softWrap = false)
                        }
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(AppColors.BorderLight)) {
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
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = D.Gutter, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(26.dp)
                            .background(AppColors.BrandPrimary, RoundedCornerShape(7.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("R", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1, softWrap = false)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("RelateStats", color = AppColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.4).sp)
                }
                HorizontalDivider(color = AppColors.BorderLight)
            }
        },
        bottomBar = {
            NavigationBar(containerColor = AppColors.Surface, tonalElevation = 8.dp) {
                NavigationBarItem(selected = activeTab == "overview", onClick = { setActiveTab("overview") }, icon = { Icon(Icons.Rounded.Home, "總覽") }, label = { Text("總覽", fontSize = 10.sp, maxLines = 1, softWrap = false) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = AppColors.ActiveBlue, selectedTextColor = AppColors.ActiveBlue))
                NavigationBarItem(selected = activeTab == "leaderboard", onClick = { setActiveTab("leaderboard") }, icon = { Icon(Icons.AutoMirrored.Rounded.List, "排行") }, label = { Text("排行", fontSize = 10.sp, maxLines = 1, softWrap = false) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = AppColors.ActiveBlue, selectedTextColor = AppColors.ActiveBlue))
                NavigationBarItem(selected = activeTab == "insights", onClick = { setActiveTab("insights") }, icon = { Icon(Icons.Rounded.Star, "洞察") }, label = { Text("洞察", fontSize = 10.sp, maxLines = 1, softWrap = false) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = AppColors.ActiveBlue, selectedTextColor = AppColors.ActiveBlue))
                NavigationBarItem(selected = activeTab == "contacts", onClick = { setActiveTab("contacts") }, icon = { Icon(Icons.Rounded.Person, "聯絡人") }, label = { Text("聯絡人", fontSize = 10.sp, maxLines = 1, softWrap = false) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = AppColors.ActiveBlue, selectedTextColor = AppColors.ActiveBlue))
                NavigationBarItem(selected = activeTab == "settings", onClick = { setActiveTab("settings") }, icon = { Icon(Icons.Rounded.Settings, "設定") }, label = { Text("設定", fontSize = 10.sp, maxLines = 1, softWrap = false) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = AppColors.ActiveBlue, selectedTextColor = AppColors.ActiveBlue))
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues).navigationBarsPadding()) {
            when (activeTab) {
                "overview" -> OverviewView(viewModel)
                "leaderboard" -> LeaderboardView(viewModel)
                "insights" -> InsightsView(viewModel)
                "contacts" -> ContactsCategoryView(viewModel)
                "settings" -> SettingsView(viewModel)
            }
        }
    }
}

@Composable
fun MinimalCalendar(selectedTime: Long, onDateSelected: (Long) -> Unit, weekStartDay: Int = Calendar.MONDAY) {
    val (currentMonthCal, setCurrentMonthCal) = remember(selectedTime) { mutableStateOf(Calendar.getInstance().apply { timeInMillis = selectedTime }) }

    val year = currentMonthCal.get(Calendar.YEAR)
    val month = currentMonthCal.get(Calendar.MONTH)

    val firstDayCal = Calendar.getInstance().apply {
        set(year, month, 1)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    // 依設定的一週起始日排列，否則日期會整排錯位
    val safeStart = if (weekStartDay in Calendar.SUNDAY..Calendar.SATURDAY) weekStartDay else Calendar.MONDAY
    val firstDayOfWeek = ((firstDayCal.get(Calendar.DAY_OF_WEEK) - safeStart) + 7) % 7
    val daysInMonth = firstDayCal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val allWeekdays = listOf("日", "一", "二", "三", "四", "五", "六")
    val weekdays = List(7) { allWeekdays[(safeStart - 1 + it) % 7] }

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
                                    val cal = Calendar.getInstance().apply {
                                        set(year, month, dayNumber)
                                        set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                                    }
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
        Row(modifier = Modifier.fillMaxWidth().background(AppColors.Surface, RoundedCornerShape(10.dp)).border(1.dp, AppColors.BorderLight, RoundedCornerShape(10.dp)).padding(3.dp)) {
            val types = listOf(FilterType.SPECIFIC_DAY to "單日", FilterType.WEEK to "單週", FilterType.ALL to "全部")
            types.forEach { (type, label) ->
                val isSelected = currentFilter.type == type
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(7.dp)).background(if (isSelected) AppColors.BrandPrimary else Color.Transparent)
                    .clickable {
                        // ⚠️ 保留目前停在哪一天。舊版一律帶 System.currentTimeMillis()，
                        //    所以往回翻到 8/10 再按「單週」會跳回本週，翻到一半就被拉走。
                        onFilterChange(DateFilter(type, currentFilter.timestamp))
                        setShowCalendar(false)
                    }.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                    Text(label, color = if (isSelected) Color.White else AppColors.TextSecondary, fontSize = T.Body, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                }
            }
        }

        if (currentFilter.type != FilterType.ALL) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    val days = if (currentFilter.type == FilterType.SPECIFIC_DAY) -1 else -7
                    val shifted = Calendar.getInstance().apply { timeInMillis = currentFilter.timestamp; add(Calendar.DATE, days) }.timeInMillis
                    onFilterChange(currentFilter.copy(timestamp = shifted))
                }, modifier = Modifier.background(AppColors.Surface, CircleShape).size(30.dp).border(1.dp, AppColors.BorderLight, CircleShape)) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "上一筆", tint = AppColors.TextPrimary, modifier = Modifier.size(18.dp))
                }

                Row(modifier = Modifier.clickable { setShowCalendar(!showCalendar) }.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.DateRange, contentDescription = null, modifier = Modifier.size(16.dp), tint = AppColors.ActiveBlue)
                    Spacer(modifier = Modifier.width(6.dp))

                    val dateText = if (currentFilter.type == FilterType.SPECIFIC_DAY) {
                        val cal = Calendar.getInstance().apply { timeInMillis = currentFilter.timestamp }
                        val isToday = DateFilter(FilterType.SPECIFIC_DAY, System.currentTimeMillis()).isSameDay(currentFilter.timestamp)
                        // 「今日」也要寫出是哪一天，否則截圖或回頭看都認不出來
                        val md = "${cal.get(Calendar.MONTH) + 1}/${cal.get(Calendar.DAY_OF_MONTH)}"
                        val wd = listOf("日", "一", "二", "三", "四", "五", "六")[cal.get(Calendar.DAY_OF_WEEK) - 1]
                        if (isToday) "今日 $md（$wd）" else "$md（$wd）"
                    } else {
                        val startMs = getStartOfWeekTimestamp(currentFilter.timestamp, weekStartDay)
                        val endMs = getEndOfWeekTimestamp(currentFilter.timestamp, weekStartDay)
                        val startCal = Calendar.getInstance().apply { timeInMillis = startMs }
                        val endCal = Calendar.getInstance().apply { timeInMillis = endMs }

                        val currentStartMs = getStartOfWeekTimestamp(System.currentTimeMillis(), weekStartDay)
                        // 一律顯示實際區間，才看得出「一週起始日」設定有沒有生效
                        val range = "${startCal.get(Calendar.MONTH) + 1}/${startCal.get(Calendar.DAY_OF_MONTH)} - ${endCal.get(Calendar.MONTH) + 1}/${endCal.get(Calendar.DAY_OF_MONTH)}"
                        if (startMs == currentStartMs) "本週 $range" else range
                    }

                    Text(dateText, color = AppColors.ActiveBlue, fontSize = T.Body, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = AppColors.ActiveBlue)
                }

                IconButton(onClick = {
                    val days = if (currentFilter.type == FilterType.SPECIFIC_DAY) 1 else 7
                    val shifted = Calendar.getInstance().apply { timeInMillis = currentFilter.timestamp; add(Calendar.DATE, days) }.timeInMillis
                    onFilterChange(currentFilter.copy(timestamp = shifted))
                }, modifier = Modifier.background(AppColors.Surface, CircleShape).size(30.dp).border(1.dp, AppColors.BorderLight, CircleShape)) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "下一筆", tint = AppColors.TextPrimary, modifier = Modifier.size(18.dp))
                }
            }
        }

        AnimatedVisibility(visible = showCalendar, enter = expandVertically(), exit = shrinkVertically()) {
            Box(modifier = Modifier.padding(top = 12.dp)) {
                MinimalCalendar(selectedTime = currentFilter.timestamp, onDateSelected = {
                    onFilterChange(currentFilter.copy(timestamp = it))
                    setShowCalendar(false)
                }, weekStartDay = weekStartDay)
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
                                    Text("💬 ${stat.msgCount} 則 · 📞 ${stat.callCount} 通 · ⏳ $mins 分 $secs 秒", fontSize = T.Label, color = AppColors.TextSecondary, modifier = Modifier.padding(top = 2.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Box(modifier = Modifier.background(stat.category.bgColor, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                    Text(stat.category.label, color = stat.category.color, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }

                            // 群組發言排行預設收起，點一下展開再點一下收合
                            if (stat.isGroup && stat.subSenders.isNotEmpty()) {
                                var expanded by remember(stat.unifiedName) { mutableStateOf(false) }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = AppColors.BorderLight)
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("群組發言排行（${stat.subSenders.size} 人）", fontSize = T.Label, fontWeight = FontWeight.Bold,
                                        color = AppColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    Icon(
                                        if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = if (expanded) "收合" else "展開",
                                        tint = AppColors.ActiveBlue, modifier = Modifier.size(18.dp)
                                    )
                                }
                                if (expanded) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val maxCount = stat.subSenders.values.maxOrNull()?.toFloat()?.coerceAtLeast(1f) ?: 1f
                                    stat.subSenders.entries.sortedByDescending { it.value }.take(8).forEach { (name, count) ->
                                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(name, fontSize = T.Label, color = AppColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(end = 8.dp))
                                                Text("$count 則", fontSize = T.Label, fontWeight = FontWeight.Bold, color = AppColors.ActiveBlue, maxLines = 1, softWrap = false)
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
            }
        },
        confirmButton = { Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = AppColors.BrandPrimary)) { Text("關閉") } }
    )
}

// ══════════════════════════════════════════
//  儀錶板共用元件
// ══════════════════════════════════════════

/** 通用的破壞性操作確認框，會列出實際會被影響的項目 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    details: List<String> = emptyList(),
    confirmLabel: String = "確認",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        containerColor = AppColors.Surface,
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, fontSize = T.Title, color = AppColors.TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column {
                Text(message, fontSize = T.Body, color = AppColors.TextSecondary, lineHeight = 18.sp)
                if (details.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(details.take(60)) {
                            Text("• $it", fontSize = T.Label, color = AppColors.TextPrimary,
                                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                    if (details.size > 60) {
                        Text("…等共 ${details.size} 項", fontSize = T.Micro, color = AppColors.TextTertiary, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = AppColors.WorkText)) {
                Text(confirmLabel, maxLines = 1, softWrap = false)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = AppColors.TextSecondary) } }
    )
}

/**
 * 服務關閉警示。
 * ⚠️ 每次重新安裝 App，Android 都會自動停用無障礙服務與通知存取權限，
 * 這是系統行為不是 bug。沒有明顯提示的話，使用者會誤以為是統計壞掉。
 */
@Composable
fun ServiceAlertBanner() {
    val context = LocalContext.current
    var accOn by remember { mutableStateOf(true) }
    var notifOn by remember { mutableStateOf(true) }
    val owner = LocalLifecycleOwner.current

    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) {
                accOn = isAccessibilityServiceEnabled(context, ChatTrackerService::class.java)
                notifOn = isNotificationServiceEnabled(context)
            }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    if (accOn && notifOn) return

    Column(
        modifier = Modifier.fillMaxWidth()
            .background(AppColors.WorkBg, RoundedCornerShape(D.Radius))
            .border(1.dp, AppColors.WorkText.copy(alpha = 0.25f), RoundedCornerShape(D.Radius))
            .padding(D.CardPad)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Warning, contentDescription = null, tint = AppColors.WorkText, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("目前沒有在記錄新資料", fontSize = T.Title, fontWeight = FontWeight.Bold, color = AppColors.WorkText, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(modifier = Modifier.height(5.dp))
        val lines = buildList {
            if (!accOn) add("「無障礙服務」已關閉 → 停留時間不會被記錄")
            if (!notifOn) add("「通知存取」已關閉 → 訊息不會被記錄")
        }
        lines.forEach {
            Text(it, fontSize = T.Label, color = AppColors.WorkText.copy(alpha = 0.9f), lineHeight = 17.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text("每次重新安裝 App，Android 都會自動關掉這些服務，需要手動重開。",
            fontSize = T.Micro, color = AppColors.WorkText.copy(alpha = 0.75f), lineHeight = 15.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (!accOn) {
                Button(
                    onClick = { runCatching { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.WorkText),
                    shape = RoundedCornerShape(9.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) { Text("開啟無障礙服務", fontSize = T.Label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
            if (!notifOn) {
                Button(
                    onClick = { runCatching { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) } },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.WorkText),
                    shape = RoundedCornerShape(9.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) { Text("開啟通知存取", fontSize = T.Label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
    }
}

/** 捲動一段距離後浮出的回頂端按鈕 */
@Composable
fun BoxScope.ScrollTopButton(listState: LazyListState) {
    val show by remember { derivedStateOf { listState.firstVisibleItemIndex > 2 } }
    val scope = rememberCoroutineScope()
    AnimatedVisibility(
        visible = show,
        enter = fadeIn(), exit = fadeOut(),
        modifier = Modifier.align(Alignment.BottomEnd).padding(end = D.Gutter, bottom = 16.dp)
    ) {
        Box(
            modifier = Modifier.size(42.dp).shadow(6.dp, CircleShape).clip(CircleShape)
                .background(AppColors.BrandPrimary)
                .clickable { scope.launch { listState.animateScrollToItem(0) } },
            contentAlignment = Alignment.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "回到頂端", tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
    }
}

/**
 * 未分類聯絡人的逐一快速分類。
 * 一次只問一位（依訊息數由多到少），避免一次面對幾百人不知從何開始。
 */
@Composable
fun QuickClassifyDialog(
    pending: List<SenderStat>,
    categories: List<ContactCategory>,
    onAssign: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var index by remember { mutableStateOf(0) }
    val current = pending.getOrNull(index)

    if (current == null) {
        AlertDialog(
            containerColor = AppColors.Surface,
            onDismissRequest = onDismiss,
            title = { Text("分類完成", fontWeight = FontWeight.Bold, fontSize = T.Title, color = AppColors.TextPrimary) },
            text = { Text("這批未分類的聯絡人都處理完了。", fontSize = T.Body, color = AppColors.TextSecondary) },
            confirmButton = { Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = AppColors.BrandPrimary)) { Text("關閉") } }
        )
        return
    }

    AlertDialog(
        containerColor = AppColors.Surface,
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("快速分類", fontWeight = FontWeight.Bold, fontSize = T.Title, color = AppColors.TextPrimary)
                Text("第 ${index + 1} / ${pending.size} 位", fontSize = T.Label, color = AppColors.TextSecondary)
            }
        },
        text = {
            Column {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .background(AppColors.SurfaceAlt, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (current.isGroup) {
                            Box(modifier = Modifier.background(AppColors.GroupBg, RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 1.dp)) {
                                Text("群", color = AppColors.GroupText, fontSize = T.Micro, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(5.dp))
                        }
                        Text(current.unifiedName, fontSize = T.Title, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${current.msgCount} 則 · ${formatDuration(current.totalDurationSeconds)}" +
                            (if (current.callCount > 0) " · ${current.callCount} 通" else "") +
                            " · " + current.appNames.joinToString("／"),
                        fontSize = T.Label, color = AppColors.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("選一個標籤", fontSize = T.Micro, color = AppColors.TextSecondary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                categories.filter { it.id != ContactCategory.OTHER.id }.chunked(2).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { cat ->
                            Box(
                                modifier = Modifier.weight(1f)
                                    .background(cat.bgColor, RoundedCornerShape(8.dp))
                                    .clickable { onAssign(current.unifiedName, cat.id); index++ }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(cat.label, color = cat.color, fontSize = T.Body, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        repeat(2 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { index++ }) { Text("跳過", color = AppColors.TextSecondary) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("稍後再說", color = AppColors.TextSecondary) } }
    )
}

/** 單一聯絡人的快速標籤選擇 */
@Composable
fun QuickTagDialog(name: String, categories: List<ContactCategory>, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        containerColor = AppColors.Surface,
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("設定標籤", fontWeight = FontWeight.Bold, fontSize = T.Title, color = AppColors.TextPrimary)
                Text(name, fontSize = T.Label, color = AppColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                categories.forEach { cat ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .background(cat.bgColor, RoundedCornerShape(8.dp))
                            .clickable { onPick(cat.id) }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(cat.color))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(cat.label, color = cat.color, fontSize = T.Body, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消", color = AppColors.TextSecondary) } }
    )
}

/** 多選時的底部操作列：批次設定標籤 */
@Composable
fun BoxScope.BatchTagBar(
    selectedCount: Int,
    totalCount: Int,
    categories: List<ContactCategory>,
    onSelectAll: () -> Unit,
    onApply: (String) -> Unit,
    onExit: () -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Surface(
        color = AppColors.Surface,
        shadowElevation = 12.dp,
        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = D.Gutter, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                if (selectedCount == 0) "點列選取" else "已選 $selectedCount",
                fontSize = T.Label, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary, maxLines = 1, softWrap = false
            )
            Text(
                if (selectedCount == totalCount && totalCount > 0) "取消全選" else "全選",
                fontSize = T.Label, fontWeight = FontWeight.Bold, color = AppColors.ActiveBlue, maxLines = 1, softWrap = false,
                modifier = Modifier.clickable { onSelectAll() }.padding(4.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            Box {
                Button(
                    onClick = { open = true },
                    enabled = selectedCount > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.BrandPrimary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) { Text("設定標籤", fontSize = T.Label, maxLines = 1, softWrap = false) }
                DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.background(AppColors.Surface)) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.label, color = cat.color, fontWeight = FontWeight.Bold) },
                            onClick = { open = false; onApply(cat.id) }
                        )
                    }
                }
            }
            TextButton(onClick = onExit, contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text("完成", fontSize = T.Label, color = AppColors.TextSecondary, maxLines = 1, softWrap = false)
            }
        }
    }
}

/** 區塊外框：白底、細框、小圓角 */
@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.fillMaxWidth()
            .background(AppColors.Surface, RoundedCornerShape(D.Radius))
            .border(1.dp, AppColors.BorderLight, RoundedCornerShape(D.Radius))
            .padding(D.CardPad),
        content = content
    )
}

/** 區塊標題列：左標題、右可放切換器或說明 */
@Composable
fun SectionHead(title: String, trailing: (@Composable RowScope.() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = T.Title, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        trailing?.let { Row(verticalAlignment = Alignment.CenterVertically, content = it) }
    }
}

/**
 * 指標說明的小 ⓘ。
 * 以前解釋文字直接印在卡片下面，字小又佔位置；改成統一用這顆點開。
 */
@Composable
fun InfoDot(title: String, body: String, tint: Color = AppColors.TextTertiary) {
    var open by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier.size(20.dp).clip(CircleShape).clickable { open = true },
        contentAlignment = Alignment.Center
    ) {
        Text("ⓘ", fontSize = 13.sp, color = tint, maxLines = 1, softWrap = false)
    }
    if (open) {
        AlertDialog(
            containerColor = AppColors.Surface,
            onDismissRequest = { open = false },
            title = { Text(title, fontWeight = FontWeight.Bold, fontSize = T.Title, color = AppColors.TextPrimary) },
            text = { Text(body, fontSize = T.Body, color = AppColors.TextSecondary, lineHeight = 19.sp) },
            confirmButton = {
                Button(onClick = { open = false }, colors = ButtonDefaults.buttonColors(containerColor = AppColors.BrandPrimary)) {
                    Text("知道了")
                }
            }
        )
    }
}

/**
 * 甜甜圈圖。用在「佔比」類的資料 —— 標籤分佈、平台分佈。
 *
 * 佔比用角度比用長條直覺：一眼看得出誰過半。
 * 但小切片在圓餅上讀不出來，所以右邊一定要配圖例與百分比。
 */
@Composable
fun DonutChart(
    slices: List<Triple<String, Long, Color>>,
    centerLabel: String,
    centerValue: String,
    onSliceClick: ((String) -> Unit)? = null
) {
    val total = slices.sumOf { it.second }.coerceAtLeast(1L)
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(116.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = size.minDimension * 0.22f
                val inset = stroke / 2f
                var startAngle = -90f
                slices.forEach { (_, v, c) ->
                    val sweep = (v.toFloat() / total) * 360f
                    if (sweep > 0f) {
                        drawArc(
                            color = c, startAngle = startAngle, sweepAngle = sweep - 1.2f, useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = Size(size.width - stroke, size.height - stroke),
                            style = Stroke(width = stroke, cap = StrokeCap.Butt)
                        )
                    }
                    startAngle += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(centerValue, fontSize = T.KpiSm, fontWeight = FontWeight.Black, color = AppColors.TextPrimary, maxLines = 1, softWrap = false)
                Text(centerLabel, fontSize = T.Micro, color = AppColors.TextTertiary, maxLines = 1, softWrap = false)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            slices.take(6).forEach { (name, v, c) ->
                val pct = ((v.toFloat() / total) * 100).toInt()
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .let { m -> if (onSliceClick != null) m.clip(RoundedCornerShape(6.dp)).clickable { onSliceClick(name) } else m }
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(c))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(name, fontSize = T.Label, color = AppColors.TextPrimary, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text("$pct%", fontSize = T.Label, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary, maxLines = 1, softWrap = false)
                }
            }
            if (slices.size > 6) {
                Text("＋其他 ${slices.size - 6} 項", fontSize = T.Micro, color = AppColors.TextTertiary)
            }
        }
    }
}

/** 小型分段切換器 */
@Composable
fun SegBar(options: List<Pair<String, String>>, current: String, onSelect: (String) -> Unit) {
    Row(modifier = Modifier.background(AppColors.Background, RoundedCornerShape(7.dp)).padding(2.dp)) {
        options.forEach { (key, label) ->
            val on = key == current
            Box(
                modifier = Modifier.clip(RoundedCornerShape(5.dp))
                    .background(if (on) AppColors.Surface else Color.Transparent)
                    .clickable { onSelect(key) }
                    .padding(horizontal = 9.dp, vertical = 3.dp)
            ) {
                Text(label, fontSize = T.Label, maxLines = 1, softWrap = false,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                    color = if (on) AppColors.TextPrimary else AppColors.TextSecondary)
            }
        }
    }
}

/** KPI 方塊 */
@Composable
fun KpiTile(label: String, value: String, sub: String?, accent: Color, modifier: Modifier = Modifier) {
    // ⚠️ 不可以寫死 .height()。方塊高度是 dp、裡面的字是 sp，
    //    使用者把系統字體放大時 sp 會變大、dp 不會，第三行就會被裁掉。
    //    改成內容決定高度；同列要等高，由外層 Row 的 IntrinsicSize.Min + fillMaxHeight 負責。
    Column(
        modifier = modifier
            .fillMaxHeight()
            .heightIn(min = 76.dp)
            .background(AppColors.Surface, RoundedCornerShape(D.Radius))
            .border(1.dp, AppColors.BorderLight, RoundedCornerShape(D.Radius))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(accent))
            Spacer(modifier = Modifier.width(5.dp))
            Text(label, fontSize = T.Micro, color = AppColors.TextSecondary, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Spacer(modifier = Modifier.weight(1f))
        // 這裡的值可能是聯絡人名稱，長度不可控，必須省略而不是溢出裁切。
        // 不指定 lineHeight：用字型自己的行高才含得下中文下緣，寫死反而會裁字。
        Text(value, fontSize = T.KpiSm, fontWeight = FontWeight.Black, color = AppColors.TextPrimary,
            maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
        Text(sub ?: "", fontSize = T.Micro, color = AppColors.TextTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** 表格資料列：色點 + 名稱 + 迷你長條 + 右側數值 */
@Composable
fun DataRow(
    name: String,
    dotColor: Color,
    progress: Float,
    primary: String,
    secondary: String? = null,
    badge: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    // 名稱靠左、長條置中、數值靠右的單列表格。
    // 舊版把長條放在名稱底下、數值另起一欄，中間留下大片空白，看起來像破版。
    Row(
        modifier = Modifier.fillMaxWidth()
            .let { if (onClick != null) it.clickable { onClick() } else it }
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(dotColor))
        Spacer(modifier = Modifier.width(7.dp))
        badge?.let { it(); Spacer(modifier = Modifier.width(4.dp)) }
        Text(
            name, fontSize = T.Body, fontWeight = FontWeight.Medium, color = AppColors.TextPrimary,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        )
        Box(modifier = Modifier.width(64.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(AppColors.Background)) {
            Box(modifier = Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight().background(dotColor))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.widthIn(min = 56.dp)) {
            Text(primary, fontSize = T.Body, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary, maxLines = 1, softWrap = false)
            if (secondary != null) {
                Text(secondary, fontSize = T.Micro, color = AppColors.TextTertiary, maxLines = 1, softWrap = false)
            }
        }
    }
}

/** 變化幅度徽章 */
@Composable
fun DeltaBadge(pct: Int?) {
    if (pct == null) {
        Text("—", fontSize = T.Micro, color = AppColors.TextTertiary, maxLines = 1, softWrap = false)
        return
    }
    val up = pct >= 0
    val c = if (up) AppColors.Positive else AppColors.WorkText
    Text(
        (if (up) "▲" else "▼") + "${kotlin.math.abs(pct)}%",
        fontSize = T.Micro, fontWeight = FontWeight.Bold, color = c, maxLines = 1, softWrap = false,
        modifier = Modifier.background(c.copy(alpha = 0.10f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 1.dp)
    )
}

/** 兩欄小統計 */
@Composable
fun StatPair(label: String, value: String, sub: String? = null, modifier: Modifier = Modifier) {
    Column(modifier = modifier.background(AppColors.SurfaceAlt, RoundedCornerShape(8.dp)).padding(horizontal = 9.dp, vertical = 7.dp)) {
        Text(label, fontSize = T.Micro, color = AppColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, fontSize = T.KpiSm, fontWeight = FontWeight.Black, color = AppColors.TextPrimary, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
        // ⚠️ 副標一定要畫，沒有就畫空字串。這個元件都是兩兩並排，
        //    只要一格有副標、另一格沒有，兩格高度就會差一行，看起來就是跑版。
        Text(sub ?: "", fontSize = T.Micro, color = AppColors.TextTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** 逐日趨勢長條圖 */
@Composable
fun TrendChart(points: List<TrendPoint>, showDuration: Boolean) {
    if (points.isEmpty()) {
        Text("尚無資料", fontSize = T.Label, color = AppColors.TextSecondary)
        return
    }
    val max = points.maxOf { if (showDuration) it.durationSeconds else it.msgCount.toLong() }.coerceAtLeast(1L)
    var picked by remember { mutableStateOf<TrendPoint?>(null) }
    val dayFmt = remember { SimpleDateFormat("M/d（E）", Locale.TAIWAN) }
    Column {
        // 選到的那天顯示在圖上方，不用另外跳對話框
        Box(modifier = Modifier.fillMaxWidth().heightIn(min = 26.dp), contentAlignment = Alignment.CenterStart) {
            val p = picked
            if (p != null) {
                Text(
                    dayFmt.format(Date(p.dayStart)) + " · " + p.msgCount + " 則 · 停留 " + formatDuration(p.durationSeconds),
                    fontSize = T.Label, fontWeight = FontWeight.Bold, color = AppColors.ActiveBlue, maxLines = 1, softWrap = false
                )
            } else {
                Text("點長條看當天數字", fontSize = T.Micro, color = AppColors.TextTertiary)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(84.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            points.forEach { pt ->
                val v = if (showDuration) pt.durationSeconds else pt.msgCount.toLong()
                val frac = (v.toFloat() / max).coerceIn(if (v > 0) 0.04f else 0.02f, 1f)
                val on = picked?.dayStart == pt.dayStart
                // 整根欄位都要可點：只有長條可點的話矮的那幾天根本點不到
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight()
                        .clickable { picked = if (on) null else pt },
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().fillMaxHeight(frac)
                            .background(
                                if (on) AppColors.BrandPrimary else if (v > 0) AppColors.ActiveBlue else AppColors.BorderLight,
                                RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
                            )
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(points.first().label, fontSize = T.Micro, color = AppColors.TextTertiary)
            Text("最高 " + (if (showDuration) formatDuration(max) else "$max 則"), fontSize = T.Micro, color = AppColors.TextTertiary)
            Text(points.last().label, fontSize = T.Micro, color = AppColors.TextTertiary)
        }
    }
}

/** 近數週每日活躍密度圖 */
@Composable
fun ContributionGrid(data: List<Pair<Long, Int>>) {
    if (data.isEmpty()) return
    val max = data.maxOf { it.second }.coerceAtLeast(1)
    val cal = remember { Calendar.getInstance() }

    // 對齊星期：第一欄可能不是完整的一週，前面補空格
    val firstDow = remember(data) {
        cal.timeInMillis = data.first().first
        (cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY + 7) % 7
    }
    val cells: List<Pair<Long, Int>?> = remember(data, firstDow) {
        List(firstDow) { null } + data
    }
    val weekLabels = listOf("日", "一", "二", "三", "四", "五", "六")

    var picked by remember { mutableStateOf<Pair<Long, Int>?>(null) }
    val dayFmt = remember { SimpleDateFormat("M/d（E）", Locale.TAIWAN) }

    Column {
        Text("每一格是一天，顏色越深代表互動越多；點格子看當天數字",
            fontSize = T.Micro, color = AppColors.TextTertiary, lineHeight = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        // ⚠️ 星期標籤與格子的邊長必須是「同一個數字」。
        //    舊寫法讓兩邊各自用 aspectRatio(1f) 推導高度：
        //    標籤區寬 20dp → 每格 20dp；格子區是 weight 平分 → 每格約 22dp。
        //    差 2dp，堆七列就差十幾 dp，看起來就是沒對齊。
        //    改成自己算邊長，兩邊都用 .size(cell)。
        val weeks = remember(cells) { cells.chunked(7) }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val labelW = 18.dp
            val gap = 2.dp
            val cell = ((maxWidth - labelW - 4.dp - gap * (weeks.size - 1).coerceAtLeast(0)) /
                weeks.size.coerceAtLeast(1)).coerceIn(6.dp, 28.dp)
            Row {
                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    weekLabels.forEach {
                        Box(modifier = Modifier.width(labelW).height(cell), contentAlignment = Alignment.CenterStart) {
                            Text(it, fontSize = 9.sp, color = AppColors.TextSecondary, maxLines = 1, softWrap = false)
                        }
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    weeks.forEach { week ->
                        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                            week.forEach { cellData ->
                                val c = cellData?.second
                                val on = cellData != null && picked?.first == cellData.first
                                val bg = when {
                                    cellData == null -> Color.Transparent
                                    c == 0 -> AppColors.BorderLight.copy(alpha = 0.5f)
                                    else -> AppColors.Positive.copy(alpha = 0.25f + 0.75f * (c!!.toFloat() / max))
                                }
                                Box(
                                    modifier = Modifier.size(cell)
                                        .background(bg, RoundedCornerShape(2.dp))
                                        .then(if (on) Modifier.border(1.5.dp, AppColors.BrandPrimary, RoundedCornerShape(2.dp)) else Modifier)
                                        .then(if (cellData != null) Modifier.clickable { picked = if (on) null else cellData } else Modifier)
                                )
                            }
                            repeat(7 - week.size) { Spacer(modifier = Modifier.size(cell)) }
                        }
                    }
                }
            }
        }
        picked?.let { (ts, count) ->
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().background(AppColors.SurfaceAlt, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                Text(dayFmt.format(Date(ts)), fontSize = T.Body, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary, maxLines = 1, softWrap = false)
                Text("$count 次互動", fontSize = T.Body, fontWeight = FontWeight.Bold, color = AppColors.Positive, maxLines = 1, softWrap = false)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Text("少", fontSize = T.Micro, color = AppColors.TextTertiary)
            Spacer(modifier = Modifier.width(4.dp))
            listOf(0f, 0.3f, 0.55f, 0.8f, 1f).forEach { a ->
                Box(modifier = Modifier.size(9.dp).background(
                    if (a == 0f) AppColors.BorderLight.copy(alpha = 0.5f) else AppColors.Positive.copy(alpha = 0.25f + 0.75f * a),
                    RoundedCornerShape(2.dp)
                ))
                Spacer(modifier = Modifier.width(2.dp))
            }
            Spacer(modifier = Modifier.width(2.dp))
            Text("多（最高 $max 次／天）", fontSize = T.Micro, color = AppColors.TextTertiary, maxLines = 1, softWrap = false)
        }
    }
}

/** 水平比例條（群組 vs 個人之類的二分佔比） */
@Composable
fun SplitBar(leftLabel: String, leftValue: Long, rightLabel: String, rightValue: Long, leftColor: Color, rightColor: Color, fmt: (Long) -> String) {
    val total = (leftValue + rightValue).coerceAtLeast(1L)
    Column {
        Row(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))) {
            Box(modifier = Modifier.weight(leftValue.toFloat().coerceAtLeast(0.001f)).fillMaxHeight().background(leftColor))
            Box(modifier = Modifier.weight(rightValue.toFloat().coerceAtLeast(0.001f)).fillMaxHeight().background(rightColor))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(leftColor))
                Spacer(modifier = Modifier.width(4.dp))
                Text("$leftLabel ${fmt(leftValue)} · ${(leftValue * 100 / total)}%", fontSize = T.Micro, color = AppColors.TextSecondary, maxLines = 1, softWrap = false)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(rightColor))
                Spacer(modifier = Modifier.width(4.dp))
                Text("$rightLabel ${fmt(rightValue)} · ${(rightValue * 100 / total)}%", fontSize = T.Micro, color = AppColors.TextSecondary, maxLines = 1, softWrap = false)
            }
        }
    }
}

// ══════════════════════════════════════════
//  洞察（深度分析）
// ══════════════════════════════════════════

@Composable
fun InsightsView(viewModel: RelateViewModel) {
    val records by viewModel.interactions.collectAsState()
    val aliases by viewModel.aliases.collectAsState()
    val categoriesMap by viewModel.categories.collectAsState()
    val availableCats by viewModel.availableCategories.collectAsState()
    val filter by viewModel.timeFilter.collectAsState()
    val weekStartDay by viewModel.weekStartDay.collectAsState()
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    // 十幾項統計一次算完並丟到背景執行緒；同步做會直接凍住畫面
    val b by produceState(
        RelateViewModel.InsightsBundle(),
        records, filter, weekStartDay, categoriesMap, aliases, availableCats
    ) {
        delay(300)
        if (value.ready) value = value.copy(ready = false)
        value = withContext(Dispatchers.Default) {
            viewModel.buildInsights(records, filter, weekStartDay, categoriesMap, aliases, availableCats)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(D.Gutter),
            verticalArrangement = Arrangement.spacedBy(D.Gap)
        ) {
            item { ServiceAlertBanner() }
            item { TimeFilterSelector(filter, viewModel::setTimeFilter, weekStartDay) }

            if (!b.ready) {
                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp, color = AppColors.ActiveBlue)
                    }
                }
                return@LazyColumn
            }

            if (records.isEmpty()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("還沒有資料可以分析", fontSize = T.Title, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                        Text("開始記錄後，這裡會出現趨勢、集中度、活躍度等分析", fontSize = T.Label, color = AppColors.TextSecondary,
                            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
                    }
                }
                return@LazyColumn
            }

            // ── 1. 與上期比較 ──
            b.deltas?.let { (dur, msg, ppl) ->
                item {
                    Panel {
                        SectionHead("與上期比較") {
                            Text("同長度的前一區間", fontSize = T.Micro, color = AppColors.TextTertiary, maxLines = 1, softWrap = false)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                Triple("訊息", "${msg.current} 則", msg.deltaPct),
                                Triple("停留時間", formatDuration(dur.current), dur.deltaPct),
                                Triple("互動對象", "${ppl.current} 位", ppl.deltaPct)
                            ).forEach { (l, v, d) ->
                                Column(modifier = Modifier.weight(1f).background(AppColors.SurfaceAlt, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 7.dp)) {
                                    Text(l, fontSize = T.Micro, color = AppColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(v, fontSize = T.KpiSm, fontWeight = FontWeight.Black, color = AppColors.TextPrimary, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    DeltaBadge(d)
                                }
                            }
                        }
                    }
                }
            }

            // ── 2. 逐日趨勢 ──
            item {
                val (mode, setMode) = remember { mutableStateOf("M") }
                Panel {
                    SectionHead("逐日趨勢") {
                        SegBar(listOf("M" to "訊息", "D" to "時長"), mode, setMode)
                    }
                    TrendChart(b.trend, mode == "D")
                }
            }

            // ── 3. 平均與極值 ──
            item {
                Panel {
                    SectionHead("每日平均")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatPair("平均每日訊息", "${b.extras.avgMsgPerActiveDay} 則", "以有活動的日子計", Modifier.weight(1f))
                        StatPair("平均每日停留", formatDuration(b.extras.avgDurPerActiveDay), null, Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatPair("最忙的一天", b.extras.busiestDayLabel, "${b.extras.busiestDayCount} 則", Modifier.weight(1f))
                        StatPair("最長單次停留", formatDuration(b.extras.longestSessionSec), null, Modifier.weight(1f))
                    }
                }
            }

            // ── 4. 活躍密度 ──
            item {
                Panel {
                    SectionHead("活躍密度") {
                        InfoDot(
                            "活躍密度怎麼看",
                            "橫軸是「週」，由左到右一週一欄；縱軸是「星期幾」，由上到下週日到週六。\n" +
                                "所以每一格代表某一天，顏色越深表示那天互動越多。\n\n" +
                                "點任一格會顯示那天的實際次數與聊天人數。\n" +
                                "整片偏白代表那段時間幾乎沒在用通訊軟體，或服務被關掉沒記錄到。"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("近 17 週", fontSize = T.Label, color = AppColors.TextSecondary, maxLines = 1, softWrap = false)
                    }
                    ContributionGrid(b.contrib)
                }
            }

            // ── 5. 活躍天數 ──
            item {
                Panel {
                    SectionHead("活躍天數")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatPair("本期活躍", "${b.coverage.activeDays} 天", "共 ${b.coverage.totalDays} 天", Modifier.weight(1f))
                        StatPair("覆蓋率", "${b.coverage.pct}%", "有互動的日子佔比", Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatPair("目前連續", "${b.coverage.currentStreak} 天", null, Modifier.weight(1f))
                        StatPair("最長連續", "${b.coverage.longestStreak} 天", null, Modifier.weight(1f))
                    }
                }
            }

            // ── 6. 社交集中度 ──
            item {
                Panel {
                    SectionHead("社交集中度") {
                        InfoDot(
                            "社交集中度怎麼看",
                            "集中指數用的是吉尼係數（Gini），衡量你的互動「有多不平均」。\n\n" +
                                "0 = 每個人分到的注意力完全一樣；100 = 全部集中在一個人身上。\n\n" +
                                "一般人大約落在 50-70：有幾個很常聯絡的人，其餘偶爾往來。\n" +
                                "數字偏高不代表不好，只代表你的社交圈很集中。\n\n" +
                                "第一名／前三名佔比則是直接看他們吃掉多少比例。"
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatPair("互動對象", "${b.conc.activeContacts} 位", null, Modifier.weight(1f))
                        StatPair("集中指數", "${b.conc.gini}", "0 平均 · 100 極度集中", Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatPair("第一名佔比", "${b.conc.top1Pct}%", null, Modifier.weight(1f))
                        StatPair("前三名佔比", "${b.conc.top3Pct}%", null, Modifier.weight(1f))
                    }
                }
            }

            // ── 7. 訊息 vs 停留（一體兩面） ──
            item {
                Panel {
                    SectionHead("訊息 vs 停留")
                    Text(
                        "兩者是互補的：訊息只有在你「沒開著聊天室」時才會跳通知，" +
                        "而停留時間只有在你「主動打開聊天室」時才會累積。加起來才是完整的注意力。",
                        fontSize = T.Label, color = AppColors.TextSecondary, lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatPair("被動接收", "${b.extras.weekdayMsgs + b.extras.weekendMsgs} 則", "通知攔截", Modifier.weight(1f))
                        StatPair("主動開啟", "${b.extras.openedChats} 次", "進入聊天室", Modifier.weight(1f))
                    }
                }
            }

            // ── 8. 平日 vs 週末 ──
            item {
                Panel {
                    SectionHead("平日 vs 週末")
                    SplitBar("平日", b.extras.weekdayMsgs.toLong(), "週末", b.extras.weekendMsgs.toLong(),
                        AppColors.ActiveBlue, AppColors.WorkText) { "$it 則" }
                }
            }

            // ── 9. 群組 vs 個人 ──
            item {
                Panel {
                    SectionHead("群組 vs 個人")
                    SplitBar("群組", b.split.groupMsgs.toLong(), "個人", b.split.personMsgs.toLong(),
                        AppColors.GroupText, AppColors.ActiveBlue) { "$it 則" }
                    Spacer(modifier = Modifier.height(10.dp))
                    SplitBar("群組停留", b.split.groupDuration, "個人停留", b.split.personDuration,
                        AppColors.GroupText.copy(alpha = 0.7f), AppColors.ActiveBlue.copy(alpha = 0.7f)) { formatDuration(it) }
                }
            }

            // ── 10. 深夜社交 ──
            item {
                Panel {
                    SectionHead("深夜社交指數") {
                        InfoDot(
                            "深夜社交指數怎麼看",
                            "統計 23:00-05:00 之間的互動佔全部的比例。\n\n" +
                                "比例高代表你的社交重心在深夜；這通常也跟睡眠時間有關。\n" +
                                "這個數字沒有好壞，只是讓你看見自己的作息樣貌。"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("22:00 – 04:59", fontSize = T.Label, color = AppColors.TextSecondary, maxLines = 1, softWrap = false)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${b.nightPct}%", fontSize = T.Kpi, fontWeight = FontWeight.Black, color = AppColors.TextPrimary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            when {
                                b.nightPct >= 40 -> "深夜是你的主場，注意作息"
                                b.nightPct >= 20 -> "有一定比例的深夜互動"
                                else -> "作息規律，深夜互動不多"
                            },
                            fontSize = T.Label, color = AppColors.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // ── 11. 通話統計 ──
            if (b.calls.total > 0) {
                item {
                    Panel {
                        SectionHead("通話統計")
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatPair("總通話", "${b.calls.total} 通", null, Modifier.weight(1f))
                            StatPair("接通", "${b.calls.answered} 通", null, Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatPair("未接", "${b.calls.missed} 通", "未接率 ${b.calls.missRate}%", Modifier.weight(1f))
                            // 沒抳到秒數就要說清楚，顯示 0s 會讓人以為是算錯
                            if (b.calls.hasDuration) {
                                StatPair("平均時長", formatDuration(b.calls.avgSeconds), "依 ${b.calls.timed} 通有秒數的通話", Modifier.weight(1f))
                            } else {
                                StatPair("平均時長", "—", "需匯入通話紀錄", Modifier.weight(1f))
                            }
                        }
                        if (!b.calls.hasDuration) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "原生撥號 App 接通後不會發通知，只靠通知拿不到通話秒數。" +
                                    "到「設定 → 匯入手機通話紀錄」可以把真實時長補進來。",
                                fontSize = T.Micro, color = AppColors.TextTertiary, lineHeight = 14.sp
                            )
                        }
                    }
                }
            }

            // ── 12. 訊息長度分佈 ──
            if (b.lengths.isNotEmpty()) {
                item {
                    Panel {
                        SectionHead("訊息長度分佈") {
                            Text("平均 ${b.extras.avgWordsPerMsg} 字／則", fontSize = T.Micro, color = AppColors.TextTertiary, maxLines = 1, softWrap = false)
                        }
                        b.lengths.forEach { (label, count, pct) ->
                            DataRow(
                                name = label,
                                dotColor = AppColors.ActiveBlue,
                                progress = pct,
                                primary = "$count 則",
                                secondary = "${(pct * 100).toInt()}%"
                            )
                        }
                    }
                }
            }

            // ── 13. 連續互動排行 ──
            if (b.streaks.isNotEmpty()) {
                item {
                    Panel {
                        SectionHead("連續互動天數") {
                            Text("到今天為止", fontSize = T.Label, color = AppColors.TextSecondary, maxLines = 1, softWrap = false)
                        }
                        val maxS = b.streaks.first().second.toFloat().coerceAtLeast(1f)
                        b.streaks.forEach { (name, days) ->
                            DataRow(name = name, dotColor = AppColors.Positive, progress = days / maxS, primary = "$days 天")
                        }
                    }
                }
            }

            // ── 14. 跨平台聯絡 ──
            if (b.cross.isNotEmpty()) {
                item {
                    Panel {
                        SectionHead("跨平台聯絡") {
                            Text("同時用多個 App", fontSize = T.Label, color = AppColors.TextSecondary, maxLines = 1, softWrap = false)
                        }
                        b.cross.forEach { (name, n) ->
                            DataRow(name = name, dotColor = AppColors.FamilyText, progress = n / 4f, primary = "$n 個平台")
                        }
                    }
                }
            }

            // ── 15. 社交人格 ──
            item {
                val persona = b.persona
                Panel {
                    SectionHead("社交人格")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(persona.emoji, fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(persona.title, fontSize = T.Title, fontWeight = FontWeight.Black, color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(persona.description.replace("\n", "　"), fontSize = T.Label, color = AppColors.TextSecondary, lineHeight = 17.sp)
                        }
                    }
                }
            }

            // ── 16. 關係變化 ──
            if (b.churn.newcomers.isNotEmpty() || b.churn.dormant.isNotEmpty()) {
                item {
                    Panel {
                        SectionHead("關係變化")
                        if (b.churn.newcomers.isNotEmpty()) {
                            Text("本期新出現 ${b.churn.newcomers.size} 位", fontSize = T.Label, fontWeight = FontWeight.Bold, color = AppColors.Positive)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(b.churn.newcomers.take(10).joinToString("、"), fontSize = T.Label, color = AppColors.TextSecondary, lineHeight = 17.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                        if (b.churn.dormant.isNotEmpty()) {
                            Text("已沉寂（14 天以上沒互動）", fontSize = T.Label, fontWeight = FontWeight.Bold, color = AppColors.WorkText)
                            Spacer(modifier = Modifier.height(4.dp))
                            b.churn.dormant.take(8).forEach { (name, days) ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(name, fontSize = T.Label, color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(end = 8.dp))
                                    Text("$days 天前", fontSize = T.Micro, color = AppColors.TextTertiary, maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }
                }
            }
        }
        ScrollTopButton(listState)
    }
}

// ══════════════════════════════════════════
//  總覽（儀錶板）
// ══════════════════════════════════════════

@Composable
fun OverviewView(viewModel: RelateViewModel) {
    val records by viewModel.interactions.collectAsState()
    val categoriesMap by viewModel.categories.collectAsState()
    val aliases by viewModel.aliases.collectAsState()
    val availableCats by viewModel.availableCategories.collectAsState()
    val filter by viewModel.timeFilter.collectAsState()
    val weekStartDay by viewModel.weekStartDay.collectAsState()
    val context = LocalContext.current

    // 全部統計一次算完並丟到背景執行緒。
    // 舊版在 composition 期間同步跑多次全表掃描，資料量大時會直接凍住畫面。
    val bundle by produceState(
        RelateViewModel.OverviewBundle(),
        records, filter, weekStartDay, categoriesMap, aliases, availableCats
    ) {
        // 先等一下再算：新訊息會讓 records 一直換新實例，
        // 不做合併的話每則訊息都會觸發一次全表重算，直接把 CPU 吃滿。
        delay(300)
        if (value.ready) value = value.copy(ready = false)
        value = withContext(Dispatchers.Default) {
            viewModel.buildOverview(records, filter, weekStartDay, categoriesMap, aliases, availableCats)
        }
    }
    val filteredRecords = bundle.scoped
    val appStats = bundle.appStats
    val timeStats = bundle.timeStats
    val persona = bundle.persona
    val leaderboard = bundle.leaderboard
    val catStats = bundle.catStats
    val heatmap = bundle.heatmap
    val wordFreq by viewModel.wordFreq.collectAsState()

    val totalTimeSec = appStats.sumOf { it.totalDurationSeconds }
    val totalNotifs = appStats.sumOf { it.incomingCount }
    val allRangeStart = remember(records) { records.minOfOrNull { it.timestamp } ?: 0L }
    val deviceTotalSecs = remember(filter, weekStartDay, allRangeStart) {
        getDeviceUsageTimeSec(context, filter, weekStartDay, allRangeStart)
    }
    val screenPct = if (deviceTotalSecs > 0L && totalTimeSec > 0L)
        ((totalTimeSec.toFloat() / deviceTotalSecs.toFloat()) * 100).toInt().coerceIn(0, 100) else 0

    val (showPersonaInfo, setShowPersonaInfo) = remember { mutableStateOf(false) }
    val (activeDetailDialog, setActiveDetailDialog) = remember { mutableStateOf<Pair<String, List<SenderStat>>?>(null) }
    val (showDemoUI, setShowDemoUI) = remember { mutableStateOf(false) }
    var quickTagTarget by remember { mutableStateOf<String?>(null) }
    var overviewDetailTarget by remember { mutableStateOf<SenderStat?>(null) }
    var showQuickClassify by remember { mutableStateOf(false) }
    var classifyDismissed by remember { mutableStateOf(false) }
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    if (showDemoUI) {
        Dialog(onDismissRequest = { setShowDemoUI(false) }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            DemoDashboardScreen(onClose = { setShowDemoUI(false) })
        }
        return
    }

    val isEmpty = bundle.ready && appStats.isEmpty() && leaderboard.isEmpty()

    Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(D.Gutter),
        verticalArrangement = Arrangement.spacedBy(D.Gap)
    ) {
        item { ServiceAlertBanner() }
        item { TimeFilterSelector(filter, viewModel::setTimeFilter, weekStartDay) }

        if (!bundle.ready) {
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp, color = AppColors.ActiveBlue)
                }
            }
            return@LazyColumn
        }

        if (isEmpty) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.DateRange, contentDescription = null, modifier = Modifier.size(48.dp), tint = AppColors.BorderStrong)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("這段期間沒有社交紀錄", fontSize = T.Title, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                    Text(
                        if (records.isEmpty()) "保持背景執行即可開始記錄" else "換個區間看看，或確認權限是否開啟",
                        fontSize = T.Label, color = AppColors.TextSecondary,
                        textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (records.isNotEmpty()) {
                            Button(
                                onClick = { viewModel.setTimeFilter(DateFilter(FilterType.ALL, System.currentTimeMillis())) },
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.BrandPrimary), shape = RoundedCornerShape(10.dp)
                            ) { Text("看全部 ${records.size} 筆", fontSize = T.Label, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) }
                        }
                        OutlinedButton(onClick = { setShowDemoUI(true) }, shape = RoundedCornerShape(10.dp)) {
                            Text("版面範例", fontSize = T.Label, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
            return@LazyColumn
        }

        // ── 未分類提示：主動邀請分類，而不是等使用者自己去聯絡人頁 ──
        run {
            val pending = leaderboard.filter { it.category.id == ContactCategory.OTHER.id && it.msgCount >= 3 }
            if (pending.isNotEmpty() && !classifyDismissed) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .background(AppColors.FamilyBg, RoundedCornerShape(D.Radius))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("有 ${pending.size} 位還沒分類", fontSize = T.Body, fontWeight = FontWeight.Bold,
                                color = AppColors.FamilyText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("分好類之後，標籤分佈才看得出你的時間花在哪種關係上",
                                fontSize = T.Micro, color = AppColors.FamilyText.copy(alpha = 0.8f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        Button(
                            onClick = { showQuickClassify = true },
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.FamilyText),
                            shape = RoundedCornerShape(9.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) { Text("開始分類", fontSize = T.Label, maxLines = 1, softWrap = false) }
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Rounded.Close, contentDescription = "先不要",
                            tint = AppColors.FamilyText.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp).clickable { classifyDismissed = true }
                        )
                    }
                }
            }
        }

        // ── KPI 兩列四格 ──
        item {
            Column(verticalArrangement = Arrangement.spacedBy(D.Gap)) {
                // IntrinsicSize.Min：先量兩格各自需要的高度，取較高者當整列高度。
                // 這樣字體放大時整列一起長高，既不裁字也不會兩格高低不齊。
                Row(modifier = Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(D.Gap)) {
                    KpiTile("接收訊息", "$totalNotifs 則", "來自 ${leaderboard.size} 位對象", AppColors.Positive, Modifier.weight(1f))
                    KpiTile("停留時間", formatDuration(totalTimeSec), if (screenPct > 0) "佔手機用量 $screenPct%" else "開啟聊天室的時間", AppColors.ActiveBlue, Modifier.weight(1f))
                }
                Row(modifier = Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(D.Gap)) {
                    val topByMsg = leaderboard.maxByOrNull { it.msgCount }
                    KpiTile("訊息最多", topByMsg?.unifiedName ?: "—", topByMsg?.let { "${it.msgCount} 則" } ?: "尚無資料",
                        AppColors.FamilyText, Modifier.weight(1f))
                    val topByTime = leaderboard.maxByOrNull { it.totalDurationSeconds }
                    KpiTile("停留最久", topByTime?.unifiedName ?: "—", topByTime?.let { formatDuration(it.totalDurationSeconds) } ?: "尚無資料",
                        AppColors.WorkText, Modifier.weight(1f))
                }
            }
        }

        // ── 平台分佈（表格） ──
        if (appStats.isNotEmpty()) {
            item {
                // 預設看訊息數：進到聊天室閱讀時不會有通知，兩個指標是互補的，
                // 但訊息數涵蓋面較廣（不需要開啟無障礙服務），當預設值比較不會是空的。
                val (mode, setMode) = remember { mutableStateOf("NOTIF") }
                var pie by rememberSaveable { mutableStateOf(true) }
                Panel {
                    SectionHead("平台分佈") {
                        SegBar(listOf("NOTIF" to "訊息", "TIME" to "時長"), mode, setMode)
                        Spacer(modifier = Modifier.width(4.dp))
                        SegBar(listOf("P" to "圓餅", "L" to "清單"), if (pie) "P" else "L") { pie = (it == "P") }
                    }
                    if (pie) {
                        val slices = appStats.map { st ->
                            Triple(
                                st.appName,
                                if (mode == "TIME") st.totalDurationSeconds else st.incomingCount.toLong(),
                                AppColors.AppBrands[st.appName] ?: AppColors.TextSecondary
                            )
                        }.filter { it.second > 0 }.sortedByDescending { it.second }
                        if (slices.isNotEmpty()) {
                            DonutChart(
                                slices = slices,
                                centerLabel = if (mode == "TIME") "總停留" else "總訊息",
                                centerValue = if (mode == "TIME") formatDuration(totalTimeSec) else "$totalNotifs",
                                onSliceClick = { appName ->
                                    val rs = filteredRecords.filter { it.appName == appName }
                                    setActiveDetailDialog("$appName 互動對象" to viewModel.getDynamicLeaderboard(rs, categoriesMap, aliases, availableCats))
                                }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                    val total = if (mode == "TIME") totalTimeSec.toFloat() else totalNotifs.toFloat()
                    if (!pie) appStats.forEach { stat ->
                        val v = if (mode == "TIME") stat.totalDurationSeconds.toFloat() else stat.incomingCount.toFloat()
                        DataRow(
                            name = stat.appName,
                            dotColor = AppColors.AppBrands[stat.appName] ?: AppColors.TextSecondary,
                            progress = if (total > 0f) v / total else 0f,
                            primary = if (mode == "TIME") formatDuration(stat.totalDurationSeconds) else "${stat.incomingCount} 則",
                            secondary = if (mode == "TIME") "${stat.incomingCount} 則" else formatDuration(stat.totalDurationSeconds),
                            onClick = {
                                val rs = filteredRecords.filter { it.appName == stat.appName }
                                setActiveDetailDialog("${stat.appName} 互動對象" to viewModel.getDynamicLeaderboard(rs, categoriesMap, aliases, availableCats))
                            }
                        )
                    }
                }
            }
        }

        // ── 標籤分佈（表格，取代原本的橫向卡片） ──
        if (catStats.isNotEmpty()) {
            item {
                val (showTime, setShowTime) = remember { mutableStateOf(false) }
                var catPie by rememberSaveable { mutableStateOf(true) }
                Panel {
                    SectionHead("標籤分佈") {
                        SegBar(listOf("M" to "訊息", "T" to "時長"), if (showTime) "T" else "M") { setShowTime(it == "T") }
                        Spacer(modifier = Modifier.width(4.dp))
                        SegBar(listOf("P" to "圓餅", "L" to "清單"), if (catPie) "P" else "L") { catPie = (it == "P") }
                    }
                    val maxV = catStats.maxOfOrNull { if (showTime) it.totalDurationSeconds.toFloat() else it.incomingCount.toFloat() }?.coerceAtLeast(1f) ?: 1f
                    val sorted = if (showTime) catStats.sortedByDescending { it.totalDurationSeconds } else catStats.sortedByDescending { it.incomingCount }
                    if (catPie) {
                        val slices = sorted.map { st ->
                            Triple(
                                st.category.label,
                                if (showTime) st.totalDurationSeconds else st.incomingCount.toLong(),
                                st.category.color
                            )
                        }.filter { it.second > 0 }
                        if (slices.isNotEmpty()) {
                            DonutChart(
                                slices = slices,
                                centerLabel = if (showTime) "總停留" else "總訊息",
                                centerValue = if (showTime) formatDuration(slices.sumOf { it.second }) else slices.sumOf { it.second }.toString(),
                                onSliceClick = { label ->
                                    val cat = sorted.firstOrNull { it.category.label == label }?.category
                                    if (cat != null) {
                                        val rs = filteredRecords.filter { r ->
                                            val u = aliases[r.rawSenderName] ?: r.rawSenderName
                                            (categoriesMap[u] ?: ContactCategory.OTHER.id) == cat.id
                                        }
                                        setActiveDetailDialog("${cat.label} 標籤內排行" to viewModel.getDynamicLeaderboard(rs, categoriesMap, aliases, availableCats))
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                    if (!catPie) sorted.forEach { stat ->
                        val v = if (showTime) stat.totalDurationSeconds.toFloat() else stat.incomingCount.toFloat()
                        DataRow(
                            name = stat.category.label,
                            dotColor = stat.category.color,
                            progress = v / maxV,
                            primary = if (showTime) formatDuration(stat.totalDurationSeconds) else "${stat.incomingCount} 則",
                            secondary = if (showTime) "${stat.incomingCount} 則" else formatDuration(stat.totalDurationSeconds),
                            onClick = {
                                val rs = filteredRecords.filter { r ->
                                    val u = aliases[r.rawSenderName] ?: r.rawSenderName
                                    (categoriesMap[u] ?: ContactCategory.OTHER.id) == stat.category.id
                                }
                                setActiveDetailDialog("${stat.category.label} 標籤內排行" to viewModel.getDynamicLeaderboard(rs, categoriesMap, aliases, availableCats))
                            }
                        )
                    }
                }
            }
        }

        // ── 時段分佈 ──
        if (timeStats.isNotEmpty()) {
            item {
                Panel {
                    SectionHead("時段分佈")
                    Row(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))) {
                        timeStats.forEach { s ->
                            Box(modifier = Modifier.weight(s.percentage.coerceAtLeast(0.01f)).fillMaxHeight().background(s.color))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        timeStats.forEach { s ->
                            val periodName = s.period.split(" ")[0]
                            Column(
                                modifier = Modifier.weight(1f)
                                    .background(AppColors.SurfaceAlt, RoundedCornerShape(8.dp))
                                    .clickable {
                                        val rs = filteredRecords.filter {
                                            val h = Calendar.getInstance().apply { timeInMillis = it.timestamp }.get(Calendar.HOUR_OF_DAY)
                                            when (periodName) { "早晨" -> h in 5..11; "下午" -> h in 12..17; else -> h !in 5..17 }
                                        }
                                        setActiveDetailDialog("$periodName 活躍對象" to viewModel.getDynamicLeaderboard(rs, categoriesMap, aliases, availableCats))
                                    }
                                    .padding(horizontal = 8.dp, vertical = 7.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(s.color))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(periodName, fontSize = T.Label, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary, maxLines = 1, softWrap = false)
                                }
                                Text("${(s.percentage * 100).toInt()}%", fontSize = T.KpiSm, fontWeight = FontWeight.Black, color = AppColors.TextPrimary, maxLines = 1, softWrap = false)
                                Text(s.explicitTimeRange, fontSize = T.Micro, color = AppColors.TextTertiary, maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }
            }
        }

        // ── 互動對象 Top 5（直接放總覽，不用切到排行） ──
        if (leaderboard.isNotEmpty()) {
            item {
                Panel {
                    SectionHead("互動對象 Top 5") {
                        Text("點名字看完整分析", fontSize = T.Micro, color = AppColors.TextTertiary, maxLines = 1, softWrap = false)
                    }
                    val byMsg = leaderboard.sortedByDescending { it.msgCount }
                    val maxD = byMsg.maxOfOrNull { it.msgCount }?.toFloat()?.coerceAtLeast(1f) ?: 1f
                    byMsg.take(5).forEachIndexed { i, stat ->
                        DataRow(
                            name = stat.unifiedName,
                            dotColor = if (stat.isGroup) AppColors.GroupText else stat.category.color,
                            progress = stat.msgCount / maxD,
                            primary = "${stat.msgCount} 則",
                            secondary = formatDuration(stat.totalDurationSeconds),
                            badge = {
                                Text("${i + 1}", fontSize = T.Micro, fontWeight = FontWeight.Black, color = AppColors.TextTertiary,
                                    modifier = Modifier.widthIn(min = 12.dp), textAlign = TextAlign.Center, maxLines = 1, softWrap = false)
                            },
                            // 點名字進詳情頁（設定標籤的入口移到詳情裡），
                            // 使用者要的是「點進去看到更多」，不是直接跳分類選單
                            onClick = { overviewDetailTarget = stat }
                        )
                    }
                }
            }
        }

        item { WeekHourHeatmapCard(heatmap) }
        item { TopWordsCard(wordFreq) }

    }
        ScrollTopButton(listState)
    }

    if (showQuickClassify) {
        // 清單必須凍結：每分類一位就會從 leaderboard 消失，
        // 清單即時縮短會讓 index 指到下一位，等於跳過人。
        val frozenPending = remember(showQuickClassify) {
            leaderboard.filter { it.category.id == ContactCategory.OTHER.id && it.msgCount >= 3 }
                .sortedByDescending { it.msgCount }
        }
        QuickClassifyDialog(
            pending = frozenPending,
            categories = availableCats,
            onAssign = { name, catId -> viewModel.setCategory(name, catId) },
            onDismiss = { showQuickClassify = false }
        )
    }

    overviewDetailTarget?.let { st ->
        val insight by produceState(RelateViewModel.ContactInsight(), records, aliases, st.unifiedName, filter) {
            value = withContext(Dispatchers.Default) {
                viewModel.buildContactInsight(records, aliases, st.unifiedName, filter)
            }
        }
        ContactDetailDialog(
            stat = st,
            insight = insight,
            onDismiss = { overviewDetailTarget = null },
            onSetTag = { quickTagTarget = st.unifiedName; overviewDetailTarget = null }
        )
    }

    quickTagTarget?.let { target ->
        QuickTagDialog(
            name = target,
            categories = availableCats,
            onPick = { catId -> viewModel.setCategory(target, catId); quickTagTarget = null },
            onDismiss = { quickTagTarget = null }
        )
    }

    if (showPersonaInfo) {
        AlertDialog(
            containerColor = AppColors.Surface, onDismissRequest = { setShowPersonaInfo(false) },
            title = { Text("社交人格全圖鑑", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AppColors.TextPrimary) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(SocialPersona.ALL_TYPES) { type ->
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(type.emoji, fontSize = 16.sp, modifier = Modifier.padding(end = 6.dp))
                                Text(type.title, fontWeight = FontWeight.Bold, fontSize = T.Body, color = AppColors.TextPrimary)
                            }
                            Text(type.description, fontSize = T.Label, color = AppColors.TextSecondary, modifier = Modifier.padding(top = 2.dp), lineHeight = 16.sp)
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
fun WeekHourHeatmapCard(grid: Array<IntArray>) {
    val max = (grid.maxOfOrNull { row -> row.maxOrNull() ?: 0 } ?: 0).coerceAtLeast(1)
    val days = listOf("日", "一", "二", "三", "四", "五", "六")
    val total = grid.sumOf { row -> row.sum() }

    Panel {
        SectionHead("活躍熱力圖") {
            Text("共 $total 次", fontSize = T.Label, color = AppColors.TextSecondary, maxLines = 1, softWrap = false)
        }
        run {
            if (total == 0) {
                Text("這段期間沒有互動紀錄", fontSize = T.Label, color = AppColors.TextSecondary)
                return@Panel
            }
            Spacer(modifier = Modifier.height(10.dp))
            // 標籤欄寬必須跟下方時間軸的 padding 用同一個值，否則兩排會對不齊
            val labelW = 20.dp
            grid.forEachIndexed { d, row ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                    Text(days[d], fontSize = T.Micro, color = AppColors.TextTertiary,
                        modifier = Modifier.widthIn(min = labelW), textAlign = TextAlign.Center, maxLines = 1, softWrap = false)
                    row.forEach { v ->
                        val alpha = if (v == 0) 0f else 0.2f + 0.8f * (v.toFloat() / max)
                        Box(
                            modifier = Modifier.weight(1f).aspectRatio(1f).padding(0.5.dp)
                                .background(
                                    if (v == 0) AppColors.BorderLight.copy(alpha = 0.5f) else AppColors.ActiveBlue.copy(alpha = alpha),
                                    RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(start = labelW, top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("0", "6", "12", "18", "23").forEach {
                    Text(it, fontSize = T.Micro, color = AppColors.TextTertiary)
                }
            }
        }
    }
}

/** 常用詞（只累計詞頻，不保留訊息原文） */
@Composable
fun TopWordsCard(wordFreq: Map<String, Int>) {
    // 預設看 3 字：2 字組合太容易切出「送了」「了貼」這種跨詞邊界的碎片
    var mode by remember { mutableStateOf("3") }
    val top = remember(wordFreq, mode) {
        wordFreq.entries.asSequence()
            .filter { e ->
                val w = e.key
                // 顯示前再擋一層：舊資料就算漏掉也不會出現在畫面上
                if (isJunkWord(w)) return@filter false
                val isCjk = w.all { it.code in 0x4e00..0x9fa5 }
                when (mode) {
                    "EN" -> !isCjk
                    else -> isCjk && w.length == mode.toInt()
                }
            }
            .sortedByDescending { it.value }
            .take(12).map { it.key to it.value }.toList()
    }

    Panel {
        SectionHead("常用詞") {
            SegBar(listOf("2" to "2字", "3" to "3字", "4" to "4字", "EN" to "英文"), mode) { mode = it }
        }
        run {
            if (top.isEmpty()) {
                Text(
                    if (wordFreq.isEmpty()) "接收訊息後會在這裡累積常用詞（只統計詞頻，不保留訊息內容）"
                    else "這個字數還沒有累積到資料，換一個看看",
                    fontSize = T.Label, color = AppColors.TextSecondary, lineHeight = 16.sp
                )
                return@Panel
            }
            val maxCount = top.first().second.coerceAtLeast(1)
            top.chunked(3).forEach { rowWords ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    rowWords.forEach { (word, count) ->
                        val ratio = count.toFloat() / maxCount
                        Column(
                            modifier = Modifier.weight(1f)
                                .background(AppColors.ActiveBlue.copy(alpha = 0.08f + 0.22f * ratio), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(word, fontSize = T.Body, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("$count 次", fontSize = T.Micro, color = AppColors.TextTertiary, maxLines = 1, softWrap = false)
                        }
                    }
                    repeat(3 - rowWords.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** 詳情頁的小標 */
@Composable
private fun DetailHead(text: String) {
    Text(text, fontSize = T.Label, fontWeight = FontWeight.Black, color = AppColors.TextSecondary,
        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp), maxLines = 1, softWrap = false)
}

/** 詳情頁的一列「標題 — 值」 */
@Composable
private fun DetailLine(label: String, value: String, accent: Color = AppColors.TextPrimary) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = T.Body, color = AppColors.TextSecondary, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(end = 8.dp))
        Text(value, fontSize = T.Body, fontWeight = FontWeight.Bold, color = accent, maxLines = 1, softWrap = false)
    }
}

@Composable
fun ContactDetailDialog(
    stat: SenderStat,
    insight: RelateViewModel.ContactInsight,
    onDismiss: () -> Unit,
    onSetTag: (() -> Unit)? = null
) {
    val (selectedHour, setSelectedHour) = remember { mutableStateOf<Int?>(null) }
    val hourly = insight.hourly
    val maxAct = (hourly.maxOfOrNull { it.score } ?: 0L).coerceAtLeast(1L)
    val avgAct = hourly.sumOf { it.score }.toFloat() / 24f
    val dateFmt = remember { SimpleDateFormat("yyyy/MM/dd", Locale.US) }
    val weekNames = listOf("週日", "週一", "週二", "週三", "週四", "週五", "週六")

    AlertDialog(
        containerColor = AppColors.Surface, onDismissRequest = onDismiss,
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.background(stat.category.bgColor, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text(stat.category.label, color = stat.category.color, fontSize = T.Label, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                    if (stat.isGroup) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(modifier = Modifier.background(AppColors.GroupBg, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 3.dp)) {
                            Text("群組", color = AppColors.GroupText, fontSize = T.Label, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(stat.unifiedName, fontWeight = FontWeight.Black, fontSize = 18.sp, lineHeight = 24.sp,
                    color = AppColors.TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (insight.ready && insight.rank > 0) {
                    Text(
                        "本期第 " + insight.rank + " 名／共 " + insight.totalContacts + " 位 · 佔你全部訊息 " + insight.sharePct + "%",
                        fontSize = T.Label, color = AppColors.ActiveBlue, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                if (!insight.ready) {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = AppColors.ActiveBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("計算中…", fontSize = T.Body, color = AppColors.TextSecondary)
                    }
                    return@Column
                }

                if (insight.missedCalls > 0) {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).background(AppColors.WorkBg, RoundedCornerShape(8.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Warning, contentDescription = null, tint = AppColors.WorkText, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("本期有 " + insight.missedCalls + " 通未接來電", color = AppColors.WorkText, fontSize = T.Body, fontWeight = FontWeight.Bold)
                    }
                }

                // ── 本期總覽 ──
                Row(modifier = Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatPair("收到訊息", insight.msgCount.toString() + " 則", insight.words.toString() + " 字", Modifier.weight(1f))
                    StatPair("停留時間", formatDuration(insight.staySeconds), "開啟聊天室", Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatPair("平均每則", insight.avgWords.toString() + " 字", "訊息長度", Modifier.weight(1f))
                    StatPair("通話", insight.callCount.toString() + " 通",
                        if (insight.callSeconds > 0) formatDuration(insight.callSeconds) else "無秒數資料", Modifier.weight(1f))
                }

                // ── 關係節奏 ──
                DetailHead("關係節奏")
                DetailLine("第一次互動", dateFmt.format(Date(insight.firstSeen)))
                DetailLine("最近一次互動",
                    dateFmt.format(Date(insight.lastSeen)) +
                        (if (insight.daysSinceLast == 0) "（今天）" else "（" + insight.daysSinceLast + " 天前）"),
                    if (insight.daysSinceLast > 30) AppColors.WorkText else AppColors.TextPrimary)
                DetailLine("認識長度", insight.spanDays.toString() + " 天")
                DetailLine("本期有互動的天數", insight.activeDays.toString() + " 天")
                DetailLine("最長連續互動", insight.longestStreak.toString() + " 天",
                    if (insight.longestStreak >= 7) AppColors.Positive else AppColors.TextPrimary)
                if (insight.topHour >= 0) {
                    DetailLine("最常聯絡時段", String.format(Locale.US, "%02d:00 - %02d:59", insight.topHour, insight.topHour))
                }
                if (insight.topWeekday in 0..6) DetailLine("最常聯絡星期", weekNames[insight.topWeekday])

                // ── 平台分佈 ──
                if (insight.platforms.isNotEmpty()) {
                    DetailHead("從哪裡來")
                    val maxP = insight.platforms.maxOf { it.second }.toFloat().coerceAtLeast(1f)
                    insight.platforms.forEach { (app, n) ->
                        DataRow(
                            name = app,
                            dotColor = AppColors.AppBrands[app] ?: AppColors.TextSecondary,
                            progress = n / maxP,
                            primary = n.toString() + " 筆"
                        )
                    }
                }

                // ── 近 8 週趨勢 ──
                if (insight.weekly.any { it.second > 0 }) {
                    DetailHead("近 8 週訊息數")
                    val maxW = insight.weekly.maxOf { it.second }.coerceAtLeast(1)
                    Row(modifier = Modifier.fillMaxWidth().height(72.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        insight.weekly.forEach { (_, n) ->
                            Box(modifier = Modifier.weight(1f).fillMaxHeight((n.toFloat() / maxW).coerceIn(0.03f, 1f))
                                .background(if (n > 0) AppColors.ActiveBlue else AppColors.BorderLight, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)))
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 3.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        insight.weekly.forEach { (label, _) ->
                            Text(label, fontSize = T.Micro, color = AppColors.TextTertiary,
                                modifier = Modifier.weight(1f), textAlign = TextAlign.Center, maxLines = 1, softWrap = false)
                        }
                    }
                    Text("最高 " + maxW + " 則／週", fontSize = T.Micro, color = AppColors.TextTertiary, modifier = Modifier.padding(top = 4.dp))
                }

                // ── 24 小時活躍 ──
                DetailHead("24 小時活躍")
                Box(modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp).background(AppColors.Background, RoundedCornerShape(8.dp)).padding(10.dp)) {
                    val h = selectedHour
                    if (h != null) {
                        val hs = hourly[h]
                        Column {
                            Text(String.format(Locale.US, "%02d:00 - %02d:59", hs.hour, hs.hour) + " · 活躍指數 " + hs.score,
                                color = AppColors.ActiveBlue, fontWeight = FontWeight.Bold, fontSize = T.Body)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(hs.msgCount.toString() + " 則訊息 · 停留 " + formatDuration(hs.durationSeconds) +
                                (if (hs.missedCalls > 0) " · " + hs.missedCalls + " 通未接" else ""),
                                color = AppColors.TextSecondary, fontSize = T.Label)
                        }
                    } else {
                        Text("點下方長條看該時段的數字", color = AppColors.TextSecondary, fontSize = T.Body)
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().height(100.dp).padding(top = 10.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val yPos = size.height - (avgAct / maxAct.toFloat()) * size.height
                        drawLine(
                            color = Color.Red.copy(alpha = 0.45f), start = Offset(0f, yPos), end = Offset(size.width, yPos),
                            strokeWidth = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                        )
                    }
                    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(1.dp), verticalAlignment = Alignment.Bottom) {
                        for (hour in 0..23) {
                            val value = hourly[hour].score
                            val pct = (value.toFloat() / maxAct.toFloat()).coerceIn(0.02f, 1f)
                            val barColor = when {
                                selectedHour == hour -> AppColors.BrandPrimary
                                value > 0 -> AppColors.ActiveBlue
                                else -> AppColors.BorderLight
                            }
                            Column(modifier = Modifier.weight(1f).fillMaxHeight().clickable { setSelectedHour(if (selectedHour == hour) null else hour) },
                                verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(modifier = Modifier.fillMaxWidth(0.85f).fillMaxHeight(pct)
                                    .background(barColor, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)))
                            }
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf(0, 6, 12, 18, 23).forEach {
                        Text(String.format(Locale.US, "%02d", it), fontSize = T.Micro, color = AppColors.TextTertiary)
                    }
                }
                Text("紅色虛線是這位聯絡人的平均值；活躍指數 = 訊息數 + 停留分鐘數",
                    fontSize = T.Micro, color = AppColors.TextTertiary, lineHeight = 14.sp, modifier = Modifier.padding(top = 6.dp))

                // ── 群組成員 ──
                if (stat.isGroup && stat.subSenders.isNotEmpty()) {
                    DetailHead("群組成員發言排行")
                    val maxSub = stat.subSenders.values.maxOrNull()?.toFloat()?.coerceAtLeast(1f) ?: 1f
                    stat.subSenders.toList().sortedByDescending { it.second }.take(15).forEach { (name, count) ->
                        DataRow(name = name, dotColor = AppColors.GroupText, progress = count / maxSub, primary = count.toString() + " 則")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = AppColors.BrandPrimary)) { Text("關閉") }
        },
        dismissButton = if (onSetTag != null) {
            { OutlinedButton(onClick = onSetTag) { Text("設定標籤", color = AppColors.TextPrimary) } }
        } else null
    )
}

/** 排行列：名次 + 名稱 + 標籤 + 迷你長條 + 雙欄數值。高度約為舊版卡片的三分之一。 */
@Composable
fun RankRow(rank: Int, stat: SenderStat, maxScore: Float, sortType: SortType, onClick: () -> Unit) {
    val score = if (sortType == SortType.TIME) stat.totalDurationSeconds.toFloat() else (stat.msgCount + stat.callCount).toFloat()
    val progress = if (maxScore > 0f) (score / maxScore).coerceIn(0f, 1f) else 0f
    val accent = if (stat.isGroup) AppColors.GroupText else stat.category.color

    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$rank", fontSize = T.Label, fontWeight = FontWeight.Black,
            color = if (rank <= 3) AppColors.ActiveBlue else AppColors.TextTertiary,
            // widthIn 而非 width：名次會到三位數，字體再放大就會被切掉
            modifier = Modifier.widthIn(min = 18.dp), textAlign = TextAlign.Center, maxLines = 1, softWrap = false)

        Column(modifier = Modifier.weight(1f).padding(start = 4.dp, end = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 標籤才是使用者要看的資訊；群組只用一個小記號附註，不再蓋掉標籤
                Box(modifier = Modifier.background(stat.category.bgColor, RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 1.dp)) {
                    Text(stat.category.label, color = stat.category.color, fontSize = T.Micro,
                        fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
                if (stat.isGroup) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Box(modifier = Modifier.background(AppColors.GroupBg, RoundedCornerShape(3.dp))
                        .padding(horizontal = 3.dp, vertical = 1.dp)) {
                        Text("群", color = AppColors.GroupText, fontSize = T.Micro, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                }
                Spacer(modifier = Modifier.width(5.dp))
                Text(stat.unifiedName, fontSize = T.Body, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(AppColors.Background)) {
                Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().background(accent))
            }
        }

        Column(horizontalAlignment = Alignment.End, modifier = Modifier.widthIn(min = 58.dp)) {
            Text("${stat.msgCount} 則", fontSize = T.Body, fontWeight = FontWeight.Black,
                color = AppColors.TextPrimary, maxLines = 1, softWrap = false)
            Text(
                buildString {
                    append(formatDuration(stat.totalDurationSeconds))
                    if (stat.callCount > 0) append(" · ${stat.callCount} 通")
                },
                fontSize = T.Micro, color = AppColors.TextTertiary, maxLines = 1, softWrap = false
            )
        }
    }
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
    val groupFilter by viewModel.groupFilter.collectAsState()
    val weekStartDay by viewModel.weekStartDay.collectAsState()

    val (selectedContact, setSelectedContact) = remember { mutableStateOf<SenderStat?>(null) }
    var tagTarget by remember { mutableStateOf<String?>(null) }
    var showFilters by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    val picked = remember { mutableStateListOf<String>() }
    // rememberSaveable：Activity 被系統回收再回來時，捲動位置要還在，
    // 不然每次切出去再切回來都被彈回最上面。
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    fun exitSelection() { selectionMode = false; picked.clear() }

    // ⚠️ 千萬不要在 remember{} 裡直接算排行。
    //    getLeaderboard 會掃過全部紀錄（可以到數萬筆），remember 是同步的，
    //    等於在主執行緒做全表運算；而 key 包含 records，
    //    每收到一則通知就重算一次，畫面直接凍住 → ANR。
    val lb by produceState(
        RelateViewModel.LeaderBundle(),
        records, categoriesMap, aliases, availableCats, filter, sort, catFilter, appFilter, groupFilter
    ) {
        delay(300)
        if (value.ready) value = value.copy(ready = false)
        value = withContext(Dispatchers.Default) {
            viewModel.buildLeaderboard(records, categoriesMap, aliases, availableCats, filter, sort, catFilter, appFilter, groupFilter)
        }
    }
    val leaderboard = lb.list
    val availableApps = lb.apps
    val maxScore = if (sort == SortType.TIME) {
        leaderboard.maxOfOrNull { it.totalDurationSeconds }?.toFloat()?.coerceAtLeast(1f) ?: 1f
    } else {
        leaderboard.maxOfOrNull { it.msgCount + it.callCount }?.toFloat()?.coerceAtLeast(1f) ?: 1f
    }
    val activeFilterCount = listOfNotNull(appFilter, catFilter?.label).size + (if (groupFilter != GroupFilter.ALL) 1 else 0)

    // 排行列彼此相連成一張表，間距由各 item 自行控制
    Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = D.Gutter, end = D.Gutter, top = D.Gutter, bottom = if (selectionMode) 76.dp else D.Gutter),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Column {
                TimeFilterSelector(filter, viewModel::setTimeFilter, weekStartDay)
                if (!lb.ready) {
                    Spacer(modifier = Modifier.height(D.Gap))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = AppColors.ActiveBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("計算中…", fontSize = T.Label, color = AppColors.TextSecondary)
                    }
                }
                Spacer(modifier = Modifier.height(D.Gap))
            }
        }

        // ── 標籤篩選：直接露出來。藏在「篩選」面板裡使用者找不到。
        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = D.Gap),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val allOn = catFilter == null
                Box(modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (allOn) AppColors.BrandPrimary else AppColors.Surface)
                    .border(1.dp, if (allOn) AppColors.BrandPrimary else AppColors.BorderLight, RoundedCornerShape(14.dp))
                    .clickable { viewModel.setCategoryFilter(null) }
                    .padding(horizontal = 12.dp, vertical = 5.dp)) {
                    Text("全部標籤", color = if (allOn) Color.White else AppColors.TextSecondary,
                        fontSize = T.Label, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
                availableCats.forEach { cat ->
                    val on = catFilter?.id == cat.id
                    Box(modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (on) cat.color else cat.bgColor)
                        .border(1.dp, if (on) cat.color else AppColors.BorderLight, RoundedCornerShape(14.dp))
                        .clickable { viewModel.setCategoryFilter(cat) }
                        .padding(horizontal = 12.dp, vertical = 5.dp)) {
                        Text(cat.label, color = if (on) Color.White else cat.color,
                            fontSize = T.Label, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                }
            }
        }

        // ── 控制列：排序 + 篩選收合，只佔一行 ──
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                SegBar(
                    listOf(SortType.MESSAGES.name to "訊息", SortType.TIME.name to "時長"),
                    sort.name
                ) { viewModel.setSortType(SortType.valueOf(it)) }

                Spacer(modifier = Modifier.weight(1f))

                Text("${leaderboard.size} 位", fontSize = T.Label, color = AppColors.TextSecondary, maxLines = 1, softWrap = false)

                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .background(if (selectionMode) AppColors.BrandPrimary else AppColors.Surface)
                        .border(1.dp, if (selectionMode) AppColors.BrandPrimary else AppColors.BorderLight, RoundedCornerShape(8.dp))
                        .clickable { if (selectionMode) exitSelection() else selectionMode = true }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(if (selectionMode) "完成" else "選取", fontSize = T.Label, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false,
                        color = if (selectionMode) Color.White else AppColors.TextPrimary)
                }

                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .background(if (activeFilterCount > 0) AppColors.ActiveBlue else AppColors.Surface)
                        .border(1.dp, if (activeFilterCount > 0) AppColors.ActiveBlue else AppColors.BorderLight, RoundedCornerShape(8.dp))
                        .clickable { showFilters = !showFilters }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        if (activeFilterCount > 0) "篩選 $activeFilterCount" else "篩選",
                        fontSize = T.Label, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false,
                        color = if (activeFilterCount > 0) Color.White else AppColors.TextPrimary
                    )
                }
            }
            Spacer(modifier = Modifier.height(D.Gap))
        }

        // ── 篩選面板（預設收合，不再佔滿三行） ──
        if (showFilters) {
            item {
                Column {
                Panel {
                    Text("類型", fontSize = T.Micro, color = AppColors.TextSecondary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(5.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GroupFilter.entries.forEach { g ->
                            val on = groupFilter == g
                            Box(modifier = Modifier
                                .background(if (on) AppColors.BrandPrimary else AppColors.Background, RoundedCornerShape(14.dp))
                                .clickable { viewModel.setGroupFilter(g) }
                                .padding(horizontal = 12.dp, vertical = 5.dp)) {
                                Text(g.label, color = if (on) Color.White else AppColors.TextSecondary, fontSize = T.Label, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("平台", fontSize = T.Micro, color = AppColors.TextSecondary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(5.dp))
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val allOn = appFilter == null
                        Box(modifier = Modifier
                            .background(if (allOn) AppColors.BrandPrimary else AppColors.Background, RoundedCornerShape(14.dp))
                            .clickable { viewModel.setAppFilter(null) }
                            .padding(horizontal = 12.dp, vertical = 5.dp)) {
                            Text("全平台", color = if (allOn) Color.White else AppColors.TextSecondary, fontSize = T.Label, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                        availableApps.forEach { app ->
                            val on = appFilter == app
                            val brand = AppColors.AppBrands[app] ?: AppColors.BrandPrimary
                            Box(modifier = Modifier
                                .background(if (on) brand else AppColors.Background, RoundedCornerShape(14.dp))
                                .clickable { viewModel.setAppFilter(app) }
                                .padding(horizontal = 12.dp, vertical = 5.dp)) {
                                Text(app, color = if (on) Color.White else AppColors.TextSecondary, fontSize = T.Label, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("標籤", fontSize = T.Micro, color = AppColors.TextSecondary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(5.dp))
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val allOn = catFilter == null
                        Box(modifier = Modifier
                            .background(if (allOn) AppColors.BrandPrimary else AppColors.Background, RoundedCornerShape(14.dp))
                            .clickable { viewModel.setCategoryFilter(null) }
                            .padding(horizontal = 12.dp, vertical = 5.dp)) {
                            Text("全標籤", color = if (allOn) Color.White else AppColors.TextSecondary, fontSize = T.Label, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                        availableCats.forEach { cat ->
                            val on = catFilter?.id == cat.id
                            Box(modifier = Modifier
                                .background(if (on) cat.color else AppColors.Background, RoundedCornerShape(14.dp))
                                .clickable { viewModel.setCategoryFilter(cat) }
                                .padding(horizontal = 12.dp, vertical = 5.dp)) {
                                Text(cat.label, color = if (on) Color.White else cat.color, fontSize = T.Label, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(D.Gap))
                }
            }
        }

        if (leaderboard.isEmpty() && lb.ready) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.DateRange, contentDescription = null, modifier = Modifier.size(40.dp), tint = AppColors.BorderStrong)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("沒有符合條件的資料", fontSize = T.Body, fontWeight = FontWeight.Bold, color = AppColors.TextSecondary)
                }
            }
        } else {
            // 逐列 item，維持惰性渲染。聯絡人可能有數百位，
            // 全部塞進單一 item 會一次組完所有節點，捲動直接卡死。
            itemsIndexed(leaderboard, key = { _, s -> s.unifiedName }) { i, stat ->
                val first = i == 0
                val last = i == leaderboard.size - 1
                val shape = RoundedCornerShape(
                    topStart = if (first) D.Radius else 0.dp, topEnd = if (first) D.Radius else 0.dp,
                    bottomStart = if (last) D.Radius else 0.dp, bottomEnd = if (last) D.Radius else 0.dp
                )
                val isSel = picked.contains(stat.unifiedName)
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .background(if (isSel) AppColors.ActiveBlue.copy(alpha = 0.07f) else AppColors.Surface, shape)
                        .border(1.dp, if (isSel) AppColors.ActiveBlue else AppColors.BorderLight, shape)
                        .padding(horizontal = D.CardPad)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (selectionMode) {
                            Checkbox(
                                checked = isSel,
                                onCheckedChange = { if (isSel) picked.remove(stat.unifiedName) else picked.add(stat.unifiedName) },
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            RankRow(i + 1, stat, maxScore, sort) {
                                if (selectionMode) {
                                    if (isSel) picked.remove(stat.unifiedName) else picked.add(stat.unifiedName)
                                } else setSelectedContact(stat)
                            }
                        }
                    }
                    if (!last) HorizontalDivider(color = AppColors.BorderLight.copy(alpha = 0.6f))
                }
            }
        }
    }

        ScrollTopButton(listState)

        if (selectionMode) {
            BatchTagBar(
                selectedCount = picked.size,
                totalCount = leaderboard.size,
                categories = availableCats,
                onSelectAll = {
                    if (picked.size == leaderboard.size) picked.clear()
                    else { picked.clear(); picked.addAll(leaderboard.map { it.unifiedName }) }
                },
                onApply = { catId -> viewModel.setCategories(picked.toList(), catId); exitSelection() },
                onExit = { exitSelection() }
            )
        }
    }

    tagTarget?.let { t ->
        QuickTagDialog(
            name = t,
            categories = availableCats,
            onPick = { catId -> viewModel.setCategory(t, catId); tagTarget = null },
            onDismiss = { tagTarget = null }
        )
    }

    if (selectedContact != null) {
        val target = selectedContact!!.unifiedName
        // 同樣不能在主執行緒掃全表：點一位聯絡人就卡一次。
        val insight by produceState(RelateViewModel.ContactInsight(), records, aliases, target, filter) {
            value = withContext(Dispatchers.Default) {
                viewModel.buildContactInsight(records, aliases, target, filter)
            }
        }
        ContactDetailDialog(
            stat = selectedContact!!,
            insight = insight,
            onDismiss = { setSelectedContact(null) },
            onSetTag = { tagTarget = target }
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun ContactsCategoryView(viewModel: RelateViewModel) {
    val context = LocalContext.current
    val records by viewModel.interactions.collectAsState()
    val categoriesMap by viewModel.categories.collectAsState()
    val aliases by viewModel.aliases.collectAsState()
    val availableCats by viewModel.availableCategories.collectAsState()

    val searchQuery by viewModel.contactsSearchQuery.collectAsState()
    val sortType by viewModel.contactsSortType.collectAsState()
    val catFilter by viewModel.contactsCatFilter.collectAsState()
    val appFilter by viewModel.contactsAppFilter.collectAsState()

    // ⚠️ 這裡本來有五次各自獨立的全表掃描（去重、計數、App 對應、
    //    垃圾聯絡人、相似分組）全部寫在 remember{}，也就是全部跑在主執行緒。
    //    findSimilarGroups 更是 O(n²)，800 位聯絡人就是 60 萬次比對。合併並丟到背景。
    val cb by produceState(RelateViewModel.ContactsBundle(), records, aliases) {
        delay(300)
        if (value.ready) value = value.copy(ready = false)
        value = withContext(Dispatchers.Default) { viewModel.buildContacts(records, aliases) }
    }
    val unifiedNamesList = cb.unifiedNames
    val interactionCounts = cb.counts
    val contactApps = cb.contactApps
    val availableApps = cb.apps

    val activeCatFilterId = catFilter?.id
    val filteredContacts = remember(unifiedNamesList, interactionCounts, searchQuery, activeCatFilterId, appFilter, contactApps, categoriesMap, sortType) {
        unifiedNamesList.filter { name ->
            val matchSearch = searchQuery.isBlank() || name.contains(searchQuery, ignoreCase = true)
            val catId = categoriesMap[name] ?: ContactCategory.OTHER.id
            val matchCat = activeCatFilterId == null || catId == activeCatFilterId
            val matchApp = appFilter == null || contactApps[name]?.contains(appFilter) == true
            matchSearch && matchCat && matchApp
        }.let { list ->
            if (sortType == ContactSortType.NAME) list.sortedBy { it }
            else list.sortedByDescending { interactionCounts[it] ?: 0 }
        }
    }

    val (showAddCategoryDialog, setShowAddCategoryDialog) = remember { mutableStateOf(false) }
    val (newCategoryName, setNewCategoryName) = remember { mutableStateOf("") }
    val (pendingAssignName, setPendingAssignName) = remember { mutableStateOf<String?>(null) }
    var mergeSource by remember { mutableStateOf<String?>(null) }
    var longPressTarget by remember { mutableStateOf<String?>(null) }
    var renameTarget by remember { mutableStateOf<String?>(null) }
    var showAutoMerge by remember { mutableStateOf(false) }
    val similarGroups = cb.similar

    var selectionMode by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<String>() }
    var showBatchCat by remember { mutableStateOf(false) }
    var showCategoryManager by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Set<String>?>(null) }
    val junkContacts = cb.junk

    fun toggleSelect(name: String) {
        if (selected.contains(name)) selected.remove(name) else selected.add(name)
    }
    fun exitSelection() { selectionMode = false; selected.clear() }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 14.dp, bottom = if (selectionMode) 92.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ⚠️ 「自動合併」以前跟標題擠在同一列。標題吃掉 weight(1f)，
            //    按鈕只拿得到剩餘寬度；名稱一長或字體一放大，按鈕就被推出父容器邊界。
            //    畫面上看得到（Row 不裁切子元件），但觸控事件不會派送到父容器外 ——
            //    這就是「看得到卻按不下去」。改成自己獨立一列、整列可按。
            if (similarGroups.isNotEmpty() && !selectionMode) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppColors.ActiveBlue.copy(alpha = 0.12f))
                            .clickable { showAutoMerge = true }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("偵測到 ${similarGroups.size} 組可能重複的聯絡人",
                            fontSize = T.Body, fontWeight = FontWeight.Bold, color = AppColors.ActiveBlue,
                            modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("查看並合併 ›", fontSize = T.Label, fontWeight = FontWeight.Bold,
                            color = AppColors.ActiveBlue, maxLines = 1, softWrap = false)
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("聯絡人整合", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = AppColors.TextPrimary, modifier = Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(10.dp))
                                .background(if (selectionMode) AppColors.BrandPrimary else AppColors.BorderLight)
                                .clickable { if (selectionMode) exitSelection() else selectionMode = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(if (selectionMode) "完成" else "選取", fontSize = T.Label, fontWeight = FontWeight.Bold,
                                color = if (selectionMode) Color.White else AppColors.TextPrimary,
                                maxLines = 1, softWrap = false)
                        }
                    }
                }
            }

            if (junkContacts.isNotEmpty() && !selectionMode) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = AppColors.WorkBg), shape = RoundedCornerShape(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text("${junkContacts.size} 個垃圾聯絡人", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppColors.WorkText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("讚、追蹤者、串文、推播等非真人通知", fontSize = 11.sp, color = AppColors.WorkText.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Button(
                                onClick = { pendingDelete = junkContacts.toSet() },
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.WorkText),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) { Text("一鍵清除", fontSize = 12.sp, maxLines = 1, softWrap = false) }
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier.fillMaxWidth().background(AppColors.Surface, RoundedCornerShape(14.dp)).border(1.dp, AppColors.BorderLight, RoundedCornerShape(14.dp)).padding(12.dp)) {
                    OutlinedTextField(
                        value = searchQuery, onValueChange = viewModel::setContactsSearch,
                        placeholder = { Text("搜尋名字...", fontSize = 14.sp) }, singleLine = true,
                        textStyle = TextStyle(fontSize = 14.sp),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppColors.BrandPrimary, unfocusedBorderColor = AppColors.BorderLight)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val allOn = appFilter == null
                        Box(modifier = Modifier
                            .background(if (allOn) AppColors.BrandPrimary else AppColors.Background, RoundedCornerShape(16.dp))
                            .clickable { viewModel.setContactsAppFilter(null) }
                            .padding(horizontal = 12.dp, vertical = 5.dp)) {
                            Text("全平台", color = if (allOn) Color.White else AppColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                        availableApps.forEach { app ->
                            val on = appFilter == app
                            val brand = AppColors.AppBrands[app] ?: AppColors.BrandPrimary
                            Box(modifier = Modifier
                                .background(if (on) brand else AppColors.Background, RoundedCornerShape(16.dp))
                                .clickable { viewModel.setContactsAppFilter(app) }
                                .padding(horizontal = 12.dp, vertical = 5.dp)) {
                                Text(app, color = if (on) Color.White else AppColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
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
                                HorizontalDivider(color = AppColors.BorderLight)
                                DropdownMenuItem(text = { Text("⚙ 管理標籤", color = AppColors.ActiveBlue, fontWeight = FontWeight.Bold) }, onClick = { showCatDropdown = false; showCategoryManager = true })
                            }
                        }
                    }
                }
            }

            if (filteredContacts.isEmpty() && cb.ready) {
                item { Text("沒有符合的聯絡人", color = AppColors.TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) }
            }

            items(filteredContacts, key = { it }) { unifiedName ->
                val catId = categoriesMap[unifiedName] ?: ContactCategory.OTHER.id
                val currentCategory = availableCats.find { it.id == catId } ?: ContactCategory.OTHER
                val (expandedCat, setExpandedCat) = remember { mutableStateOf(false) }
                val isSel = selected.contains(unifiedName)

                val rawNamesForThis = cb.rawByUnified[unifiedName] ?: listOf(unifiedName)

                Card(
                    colors = CardDefaults.cardColors(containerColor = if (isSel) AppColors.ActiveBlue.copy(alpha = 0.08f) else AppColors.Surface),
                    border = BorderStroke(1.dp, if (isSel) AppColors.ActiveBlue else AppColors.BorderLight),
                    shape = RoundedCornerShape(14.dp),
                    // 長按叫出操作選單（改名／綁定／刪除）；選取模式下維持單擊勾選
                    modifier = Modifier.combinedClickable(
                        onClick = { if (selectionMode) toggleSelect(unifiedName) },
                        onLongClick = { if (!selectionMode) longPressTarget = unifiedName }
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            if (selectionMode) {
                                Checkbox(checked = isSel, onCheckedChange = { toggleSelect(unifiedName) }, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                            }
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(unifiedName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (rawNamesForThis.isNotEmpty() && rawNamesForThis.first() != unifiedName) {
                                    Text("包含: ${rawNamesForThis.joinToString()}", fontSize = 11.sp, color = AppColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                contactApps[unifiedName]?.takeIf { it.isNotEmpty() }?.let { apps ->
                                    Row(modifier = Modifier.padding(top = 3.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        apps.take(4).forEach { a ->
                                            val brand = AppColors.AppBrands[a] ?: AppColors.TextSecondary
                                            Text(a, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = brand, maxLines = 1, softWrap = false,
                                                modifier = Modifier.background(brand.copy(alpha = 0.10f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 1.dp))
                                        }
                                    }
                                }
                            }
                            if (selectionMode) {
                                Box(modifier = Modifier.background(currentCategory.bgColor, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                    Text(currentCategory.label, color = currentCategory.color, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, softWrap = false)
                                }
                            } else {
                                Box {
                                    TextButton(onClick = { setExpandedCat(true) }, modifier = Modifier.background(currentCategory.bgColor, RoundedCornerShape(8.dp)), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                                        Text(currentCategory.label, color = currentCategory.color, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                        }
                        if (!selectionMode) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = AppColors.BorderLight)
                            Row(modifier = Modifier.fillMaxWidth().clickable { mergeSource = unifiedName }.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = AppColors.ActiveBlue, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("綁定其他帳號", fontSize = 12.sp, color = AppColors.ActiveBlue, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }

        // 多選模式的底部操作列
        if (selectionMode) {
            Surface(
                color = AppColors.Surface,
                shadowElevation = 12.dp,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (selected.isEmpty()) "點卡片選取" else "已選 ${selected.size} 位",
                            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary, maxLines = 1, softWrap = false
                        )
                        Text(
                            if (selected.size == filteredContacts.size && filteredContacts.isNotEmpty()) "取消全選" else "全選",
                            fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppColors.ActiveBlue, maxLines = 1, softWrap = false,
                            modifier = Modifier.clickable {
                                if (selected.size == filteredContacts.size) selected.clear()
                                else { selected.clear(); selected.addAll(filteredContacts) }
                            }.padding(6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            Button(
                                onClick = { showBatchCat = true },
                                enabled = selected.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.BrandPrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) { Text("設定標籤", fontSize = 13.sp, maxLines = 1, softWrap = false) }
                            DropdownMenu(expanded = showBatchCat, onDismissRequest = { showBatchCat = false }, modifier = Modifier.background(AppColors.Surface)) {
                                availableCats.forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(category.label, color = category.color, fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            viewModel.setCategories(selected.toList(), category.id)
                                            showBatchCat = false
                                            exitSelection()
                                        }
                                    )
                                }
                                HorizontalDivider(color = AppColors.BorderLight)
                                DropdownMenuItem(
                                    text = { Text("+ 新增自訂分類", color = AppColors.ActiveBlue, fontWeight = FontWeight.Bold) },
                                    onClick = { showBatchCat = false; setPendingAssignName(null); setShowAddCategoryDialog(true) }
                                )
                            }
                        }
                        Button(
                            onClick = { pendingDelete = selected.toSet() },
                            enabled = selected.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.WorkText),
                            shape = RoundedCornerShape(10.dp)
                        ) { Text("刪除", fontSize = 13.sp, maxLines = 1, softWrap = false) }
                    }
                }
            }
        }
    }

    pendingDelete?.let { targets ->
        AlertDialog(
            containerColor = AppColors.Surface,
            onDismissRequest = { pendingDelete = null },
            title = { Text("刪除 ${targets.size} 位聯絡人？", fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            text = {
                Column {
                    Text("將一併刪除這些聯絡人的所有互動紀錄、標籤與合併設定，此操作無法復原。", fontSize = 13.sp, color = AppColors.TextSecondary, lineHeight = 18.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 180.dp)) {
                        items(targets.toList().take(50), key = { it }) {
                            Text("• $it", fontSize = 12.sp, color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val removed = viewModel.deleteContacts(targets)
                    Toast.makeText(context, "已刪除 ${targets.size} 位聯絡人（$removed 筆紀錄）", Toast.LENGTH_SHORT).show()
                    pendingDelete = null
                    exitSelection()
                }, colors = ButtonDefaults.buttonColors(containerColor = AppColors.WorkText)) { Text("確認刪除", maxLines = 1) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消", color = AppColors.TextSecondary) } }
        )
    }

    mergeSource?.let { source ->
        MergeTargetDialog(
            source = source,
            candidates = unifiedNamesList.filter { it != source && (aliases[it] ?: it) != source },
            onPick = { target -> viewModel.mergeContact(source, target); mergeSource = null },
            onDismiss = { mergeSource = null }
        )
    }

    if (showCategoryManager) {
        CategoryManagerDialog(viewModel, availableCats) { showCategoryManager = false }
    }

    longPressTarget?.let { target ->
        AlertDialog(
            containerColor = AppColors.Surface,
            onDismissRequest = { longPressTarget = null },
            title = {
                Text(target, fontWeight = FontWeight.Bold, fontSize = T.Title,
                    color = AppColors.TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            },
            text = {
                Column {
                    listOf(
                        "\u270f\ufe0f 修改顯示名稱" to "rename",
                        "\ud83d\udd17 綁定其他帳號到這個人" to "merge",
                        "\ud83c\udff7\ufe0f 設定標籤" to "tag",
                        "\ud83d\uddd1\ufe0f 刪除這位聯絡人與其紀錄" to "delete"
                    ).forEach { (label, action) ->
                        Text(
                            label,
                            fontSize = T.Body,
                            color = if (action == "delete") AppColors.WorkText else AppColors.TextPrimary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    when (action) {
                                        "rename" -> renameTarget = target
                                        "merge" -> mergeSource = target
                                        "tag" -> setPendingAssignName(target)
                                        "delete" -> pendingDelete = setOf(target)
                                    }
                                    longPressTarget = null
                                }
                                .padding(vertical = 12.dp, horizontal = 6.dp)
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { longPressTarget = null }) { Text("取消", color = AppColors.TextSecondary) } }
        )
    }

    renameTarget?.let { target ->
        var newName by remember(target) { mutableStateOf(target) }
        AlertDialog(
            containerColor = AppColors.Surface,
            onDismissRequest = { renameTarget = null },
            title = { Text("修改顯示名稱", fontWeight = FontWeight.Bold, fontSize = T.Title, color = AppColors.TextPrimary) },
            text = {
                Column {
                    Text(
                        "只改這個 App 裡的顯示名稱，不會動到 LINE / IG 上的名稱。\n" +
                            "原本的所有紀錄會一起掛到新名稱底下。",
                        fontSize = T.Label, color = AppColors.TextSecondary, lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val nn = newName.trim()
                        if (nn.isNotEmpty() && nn != target) viewModel.renameContact(target, nn)
                        renameTarget = null
                    },
                    enabled = newName.trim().isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.BrandPrimary)
                ) { Text("儲存") }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("取消", color = AppColors.TextSecondary) } }
        )
    }

    if (showAutoMerge) {
        AutoMergeDialog(
            groups = similarGroups,
            counts = interactionCounts,
            onConfirm = { picks ->
                picks.forEach { (target, others) -> others.forEach { viewModel.mergeContact(it, target) } }
                showAutoMerge = false
            },
            onDismiss = { showAutoMerge = false }
        )
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

/** 可搜尋的合併對象選擇器 */
@Composable
fun MergeTargetDialog(source: String, candidates: List<String>, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, candidates) {
        if (query.isBlank()) candidates else candidates.filter { it.contains(query, ignoreCase = true) }
    }
    AlertDialog(
        containerColor = AppColors.Surface,
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("合併「$source」", fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("選擇要併入的主帳號", fontSize = 12.sp, color = AppColors.TextSecondary)
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    placeholder = { Text("搜尋名字...", fontSize = 13.sp) },
                    textStyle = TextStyle(fontSize = 14.sp),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppColors.BrandPrimary, unfocusedBorderColor = AppColors.BorderLight)
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (filtered.isEmpty()) {
                    Text("找不到符合的名字", fontSize = 13.sp, color = AppColors.TextSecondary, modifier = Modifier.padding(12.dp))
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                        items(filtered, key = { it }) { name ->
                            Row(modifier = Modifier.fillMaxWidth().clickable { onPick(name) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            HorizontalDivider(color = AppColors.BorderLight)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消", color = AppColors.TextSecondary) } }
    )
}

/** 自動合併相似帳號：列出建議群組讓使用者勾選確認 */
@Composable
fun AutoMergeDialog(
    groups: List<List<String>>,
    counts: Map<String, Int>,
    onConfirm: (List<Pair<String, List<String>>>) -> Unit,
    onDismiss: () -> Unit
) {
    val selected = remember { mutableStateMapOf<Int, Boolean>() }
    AlertDialog(
        containerColor = AppColors.Surface,
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("自動合併相似帳號", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("忽略 emoji、空白與標點後判定為同一人", fontSize = 12.sp, color = AppColors.TextSecondary)
            }
        },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 380.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(groups) { idx, group ->
                    val target = group.maxByOrNull { counts[it] ?: 0 } ?: group.first()
                    val on = selected[idx] ?: true
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .background(if (on) AppColors.ActiveBlue.copy(alpha = 0.06f) else AppColors.Background, RoundedCornerShape(8.dp))
                            .clickable { selected[idx] = !on }.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // 不要用 size() 壓縮 Checkbox：會讓觸控目標小於系統最小值，很難點中。
                        // 整列已經可以按，這裡只當狀態指示。
                        Checkbox(checked = on, onCheckedChange = { selected[idx] = it })
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("→ $target", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            group.filter { it != target }.forEach {
                                Text(it, fontSize = 11.sp, color = AppColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val picks = groups.mapIndexedNotNull { idx, group ->
                    if (selected[idx] ?: true) {
                        val target = group.maxByOrNull { counts[it] ?: 0 } ?: group.first()
                        target to group.filter { it != target }
                    } else null
                }
                onConfirm(picks)
            }, colors = ButtonDefaults.buttonColors(containerColor = AppColors.BrandPrimary)) { Text("合併") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = AppColors.TextSecondary) } }
    )
}

/**
 * 停留時間偵測診斷。
 * 顯示無障礙服務對每個畫面的判定結果，用來釐清某個 App 抓不到時長的原因：
 * 是服務沒開、抓不到標題、還是標題被判定為非聊天室。
 */
@Composable
fun DetectionDiagnosticsDialog(viewModel: RelateViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val log by viewModel.debugLog.collectAsState()
    val beat by viewModel.lastServiceBeat.collectAsState()
    val accOn = remember { isAccessibilityServiceEnabled(context, ChatTrackerService::class.java) }
    val crash = remember { viewModel.readLastCrash() }
    val beatFmt = remember { SimpleDateFormat("MM/dd HH:mm:ss", Locale.US) }

    AlertDialog(
        containerColor = AppColors.Surface,
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("停留時間偵測診斷", fontWeight = FontWeight.Bold, fontSize = T.Title, color = AppColors.TextPrimary)
                Text(
                    if (accOn) "無障礙服務執行中" else "無障礙服務已關閉，不會有任何紀錄",
                    fontSize = T.Label, color = if (accOn) AppColors.Positive else AppColors.WorkText, fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                if (crash != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .background(AppColors.WorkBg, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("上次閃退紀錄", fontSize = T.Body, fontWeight = FontWeight.Bold, color = AppColors.WorkText)
                            Text("清除", fontSize = T.Label, color = AppColors.WorkText, fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { viewModel.clearLastCrash() }.padding(4.dp))
                        }
                        Text(crash.lineSequence().take(8).joinToString("\n"),
                            fontSize = T.Micro, color = AppColors.WorkText.copy(alpha = 0.9f), lineHeight = 14.sp)
                        Text("按下方「複製紀錄」會一併複製完整堆疊",
                            fontSize = T.Micro, color = AppColors.WorkText.copy(alpha = 0.7f), modifier = Modifier.padding(top = 4.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(
                    "服務最後回報：" + (if (beat > 0) beatFmt.format(Date(beat)) else "本次啟動後尚未回報"),
                    fontSize = T.Label, fontWeight = FontWeight.Bold,
                    color = if (beat > 0) AppColors.Positive else AppColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "使用方式：先開著這個 App，切到 LINE / Instagram 打開一個聊天室停留幾秒，再回來看下方紀錄。",
                    fontSize = T.Label, color = AppColors.TextSecondary, lineHeight = 17.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (log.isEmpty()) {
                    Text("尚無偵測紀錄", fontSize = T.Label, color = AppColors.TextTertiary, modifier = Modifier.padding(vertical = 12.dp))
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(log.reversed()) { line ->
                            Text(
                                line, fontSize = T.Micro, color = AppColors.TextPrimary, lineHeight = 15.sp,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val text = buildString {
                        viewModel.readLastCrash()?.let { append("=== 上次閃退 ===\n").append(it).append("\n\n") }
                        append("=== 偵測紀錄 ===\n")
                        append(viewModel.debugLog.value.joinToString("\n"))
                    }
                    runCatching {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("RelateStats 診斷", text))
                        Toast.makeText(context, "已複製 ${viewModel.debugLog.value.size} 行", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.BrandPrimary)
            ) { Text("複製紀錄", maxLines = 1, softWrap = false) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { viewModel.clearDetectLog() }) { Text("清空", color = AppColors.TextSecondary) }
                TextButton(onClick = onDismiss) { Text("關閉", color = AppColors.TextSecondary) }
            }
        }
    )
}

/** 逐筆檢視與勾選刪除互動紀錄 */
@Composable
fun RecordManagerDialog(viewModel: RelateViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val records by viewModel.interactions.collectAsState()
    val aliases by viewModel.aliases.collectAsState()
    var query by remember { mutableStateOf("") }
    val selected = remember { mutableStateListOf<String>() }
    val fmt = remember { SimpleDateFormat("MM/dd HH:mm", Locale.US) }

    // 資料量可能上萬筆，只呈現最新的 400 筆並提供搜尋
    // 反轉 + 過濾上萬筆也不能在主執行緒做，打字搜尋時每按一鍵就卡一次
    val shown by produceState(emptyList<InteractionRecord>(), records, query, aliases) {
        delay(200)
        value = withContext(Dispatchers.Default) {
        val base = records.asReversed()
        val filtered = if (query.isBlank()) base else base.filter {
            val name = aliases[it.rawSenderName] ?: it.rawSenderName
            name.contains(query, ignoreCase = true) || it.appName.contains(query, ignoreCase = true)
        }
        filtered.take(400)
        }
    }

    AlertDialog(
        containerColor = AppColors.Surface,
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("紀錄管理", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("共 ${records.size} 筆，顯示最新 ${shown.size} 筆", fontSize = 11.sp, color = AppColors.TextSecondary)
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    placeholder = { Text("搜尋對象或平台…", fontSize = 13.sp) },
                    textStyle = TextStyle(fontSize = 13.sp),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppColors.BrandPrimary, unfocusedBorderColor = AppColors.BorderLight)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("已選 ${selected.size} 筆", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary, maxLines = 1, softWrap = false)
                    Text(
                        if (selected.size == shown.size && shown.isNotEmpty()) "取消全選" else "全選顯示中",
                        fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppColors.ActiveBlue, maxLines = 1, softWrap = false,
                        modifier = Modifier.clickable {
                            if (selected.size == shown.size) selected.clear()
                            else { selected.clear(); selected.addAll(shown.map { it.id }) }
                        }.padding(4.dp)
                    )
                }
                HorizontalDivider(color = AppColors.BorderLight)
                LazyColumn(modifier = Modifier.heightIn(max = 340.dp)) {
                    items(shown, key = { it.id }) { rec ->
                        val name = aliases[rec.rawSenderName] ?: rec.rawSenderName
                        val isSel = selected.contains(rec.id)
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable { if (isSel) selected.remove(rec.id) else selected.add(rec.id) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = isSel, onCheckedChange = { if (isSel) selected.remove(rec.id) else selected.add(rec.id) }, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                                Text(name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    buildString {
                                        append(rec.appName)
                                        append(" · ")
                                        append(fmt.format(Date(rec.timestamp)))
                                        if (rec.durationSeconds > 0) append(" · 停留 ${formatDuration(rec.durationSeconds)}")
                                        if (rec.wordCount > 0) append(" · ${rec.wordCount} 字")
                                        if (rec.isMissedCall) append(" · 未接")
                                        else if (rec.isCall) append(" · 通話")
                                        rec.subSender?.let { append(" · $it") }
                                    },
                                    fontSize = 10.sp, color = AppColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        HorizontalDivider(color = AppColors.BorderLight.copy(alpha = 0.5f))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val n = viewModel.deleteRecordsByIds(selected.toSet())
                    Toast.makeText(context, "已刪除 $n 筆（可在設定頁復原）", Toast.LENGTH_SHORT).show()
                    selected.clear()
                },
                enabled = selected.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.WorkText)
            ) { Text("刪除選取", maxLines = 1, softWrap = false) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("關閉", color = AppColors.TextSecondary) } }
    )
}

/** 標籤管理：改名、換色、排序、刪除 */
@Composable
fun CategoryManagerDialog(viewModel: RelateViewModel, categories: List<ContactCategory>, onDismiss: () -> Unit) {
    var editTarget by remember { mutableStateOf<ContactCategory?>(null) }
    var creating by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<ContactCategory?>(null) }

    AlertDialog(
        containerColor = AppColors.Surface,
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("標籤管理", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("可改名、換色、調整順序；刪除後成員退回未分類", fontSize = 11.sp, color = AppColors.TextSecondary, lineHeight = 15.sp)
            }
        },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 380.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                itemsIndexed(categories, key = { _, c -> c.id }) { idx, cat ->
                    val locked = cat.id in ContactCategory.PROTECTED_IDS
                    Row(
                        modifier = Modifier.fillMaxWidth().background(AppColors.Background, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.background(cat.bgColor, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text(cat.label, color = cat.color, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "上移",
                            tint = if (idx > 0) AppColors.TextPrimary else AppColors.BorderLight,
                            modifier = Modifier.size(22.dp).clickable(enabled = idx > 0) { viewModel.moveCategory(cat.id, -1) })
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "下移",
                            tint = if (idx < categories.size - 1) AppColors.TextPrimary else AppColors.BorderLight,
                            modifier = Modifier.size(22.dp).clickable(enabled = idx < categories.size - 1) { viewModel.moveCategory(cat.id, 1) })
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Rounded.Edit, contentDescription = "編輯", tint = AppColors.ActiveBlue,
                            modifier = Modifier.size(18.dp).clickable { editTarget = cat })
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(Icons.Rounded.Delete, contentDescription = "刪除",
                            tint = if (locked) AppColors.BorderLight else AppColors.WorkText,
                            modifier = Modifier.size(18.dp).clickable(enabled = !locked) { pendingDelete = cat })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { creating = true }, colors = ButtonDefaults.buttonColors(containerColor = AppColors.BrandPrimary)) {
                Text("新增標籤", maxLines = 1, softWrap = false)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("關閉", color = AppColors.TextSecondary) } }
    )

    if (creating || editTarget != null) {
        CategoryEditDialog(
            initial = editTarget,
            onConfirm = { label, textColor, bgColor ->
                val target = editTarget
                if (target == null) viewModel.addCategory(label, textColor, bgColor)
                else viewModel.updateCategoryMeta(target.id, label, textColor, bgColor)
                creating = false; editTarget = null
            },
            onDismiss = { creating = false; editTarget = null }
        )
    }

    pendingDelete?.let { cat ->
        AlertDialog(
            containerColor = AppColors.Surface,
            onDismissRequest = { pendingDelete = null },
            title = { Text("刪除標籤「${cat.label}」？", fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            text = { Text("原本使用這個標籤的聯絡人會退回「未分類」，互動紀錄不受影響。", fontSize = 13.sp, color = AppColors.TextSecondary, lineHeight = 18.sp) },
            confirmButton = {
                Button(onClick = { viewModel.deleteCategory(cat.id); pendingDelete = null },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.WorkText)) { Text("刪除", maxLines = 1) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消", color = AppColors.TextSecondary) } }
        )
    }
}

/** 標籤的新增／編輯：名稱 + 配色 */
@Composable
fun CategoryEditDialog(initial: ContactCategory?, onConfirm: (String, Int, Int) -> Unit, onDismiss: () -> Unit) {
    val palette = remember { AppColors.CategoryPalette }
    var label by remember { mutableStateOf(initial?.label ?: "") }
    var pick by remember {
        mutableStateOf(palette.indexOfFirst { it.first.toArgb() == initial?.colorInt }.takeIf { it >= 0 } ?: 0)
    }

    AlertDialog(
        containerColor = AppColors.Surface,
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "新增標籤" else "編輯標籤", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column {
                OutlinedTextField(
                    value = label, onValueChange = { label = it }, singleLine = true,
                    placeholder = { Text("例：另一半、健身房…", fontSize = 13.sp) },
                    textStyle = TextStyle(fontSize = 14.sp),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppColors.BrandPrimary, unfocusedBorderColor = AppColors.BorderLight)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("顏色", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppColors.TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                palette.chunked(6).forEachIndexed { rowIdx, row ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEachIndexed { colIdx, pair ->
                            val gi = rowIdx * 6 + colIdx
                            val on = gi == pick
                            Box(
                                modifier = Modifier.weight(1f).aspectRatio(1f)
                                    .background(pair.second, RoundedCornerShape(8.dp))
                                    .border(if (on) 2.dp else 1.dp, if (on) pair.first else AppColors.BorderLight, RoundedCornerShape(8.dp))
                                    .clickable { pick = gi },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("A", color = pair.first, fontSize = 13.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        repeat(6 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(label, palette[pick].first.toArgb(), palette[pick].second.toArgb()) },
                enabled = label.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.BrandPrimary)
            ) { Text("儲存", maxLines = 1) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = AppColors.TextSecondary) } }
    )
}

@Composable
fun AdvancedDataManagerDialog(viewModel: RelateViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val weekStartDay by viewModel.weekStartDay.collectAsState()
    var pendingClearScope by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        AlertDialog(
            containerColor = AppColors.Surface, onDismissRequest = { showDatePicker = false },
            title = { Text("請選擇要清除的日期", fontWeight = FontWeight.Bold) },
            text = {
                Box(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    MinimalCalendar(weekStartDay = weekStartDay, selectedTime = System.currentTimeMillis(), onDateSelected = { timestamp ->
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

    var callImportMsg by remember { mutableStateOf<String?>(null) }
    fun runCallImport() {
        val (added, skipped) = viewModel.importCallLog(context)
        callImportMsg = when {
            added > 0 -> "已匯入 $added 筆通話紀錄" + (if (skipped > 0) "（跳過 $skipped 筆重複）" else "")
            skipped > 0 -> "沒有新的紀錄，$skipped 筆已匯入過"
            else -> "手機裡沒有可匯入的通話紀錄"
        }
    }
    val callLogLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) runCallImport() else callImportMsg = "沒有通話紀錄權限，無法匯入"
    }

    val (showDataManager, setShowDataManager) = remember { mutableStateOf(false) }
    val (showGuideDialog, setShowGuideDialog) = remember { mutableStateOf(false) }
    val weekStartDay by viewModel.weekStartDay.collectAsState()

    val records by viewModel.interactions.collectAsState()
    val availableCats by viewModel.availableCategories.collectAsState()
    var showCategoryManager by remember { mutableStateOf(false) }
    var showRecordManager by remember { mutableStateOf(false) }
    var confirmAction by remember { mutableStateOf<String?>(null) }
    var showDiagnostics by remember { mutableStateOf(false) }
    val wordFreqNow by viewModel.wordFreq.collectAsState()
    val undoAvailable by viewModel.undoAvailable.collectAsState()
    val undoLabel by viewModel.undoLabel.collectAsState()
    LaunchedEffect(Unit) { viewModel.refreshUndoState() }
    val autoCleanedCount by viewModel.lastCleanupRemoved.collectAsState()
    var abnormalCount by remember { mutableStateOf(0) }
    LaunchedEffect(records) { abnormalCount = viewModel.countAbnormalRecords() }

    // 回到前景時重新檢查，否則開完權限回來仍顯示「去設定」
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                setHasNotif(isNotificationServiceEnabled(context))
                setHasAccel(isAccessibilityServiceEnabled(context, ChatTrackerService::class.java))
                setHasUsageStats(hasUsageStatsPermission(context))
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val ioScope = rememberCoroutineScope()

    // 檔案讀寫需在 IO 執行緒，否則資料量大會 ANR
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) {
            ioScope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        val payload = viewModel.exportBackup("").toByteArray()
                        context.contentResolver.openOutputStream(uri)?.use { it.write(payload) } ?: return@runCatching false
                        true
                    }.getOrDefault(false)
                }
                Toast.makeText(context, if (ok) "已成功匯出備份檔案！" else "匯出失敗，請換個儲存位置再試", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            ioScope.launch {
                val content = withContext(Dispatchers.IO) {
                    runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()
                }
                val ok = content != null && viewModel.importBackup(content, "")
                Toast.makeText(context, if (ok) "資料還原成功！" else "檔案無效或格式錯誤", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("設定與服務", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = AppColors.TextPrimary, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)) }

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
            Card(colors = CardDefaults.cardColors(containerColor = AppColors.Surface), border = BorderStroke(1.dp, AppColors.BorderLight), shape = RoundedCornerShape(14.dp), modifier = Modifier.clickable { showCategoryManager = true }) {
                Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("標籤管理", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("改名、換色、調整順序、新增或刪除", fontSize = 11.sp, color = AppColors.TextSecondary)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        availableCats.take(4).forEach { cat ->
                            Box(modifier = Modifier.size(14.dp).background(cat.bgColor, CircleShape).border(1.dp, cat.color, CircleShape))
                        }
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = AppColors.Surface), border = BorderStroke(1.dp, AppColors.BorderLight), shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("備份與還原", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Icon(Icons.Rounded.Lock, contentDescription = null, tint = AppColors.TextSecondary)
                    }
                    Text("所有資料只存在這支手機，換機或重灌前請務必先備份。", fontSize = 12.sp, color = AppColors.TextSecondary, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))

                    // 走系統分享選單，不需登入 Google 帳號
                    Button(
                        onClick = {
                            ioScope.launch {
                                val intent = withContext(Dispatchers.IO) { viewModel.buildShareBackupIntent(context) }
                                if (intent != null) {
                                    runCatching { context.startActivity(Intent.createChooser(intent, "備份到雲端")) }
                                        .onFailure { Toast.makeText(context, "找不到可接收的 App", Toast.LENGTH_SHORT).show() }
                                } else {
                                    Toast.makeText(context, "備份檔產生失敗", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.BrandPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("備份到 Google 雲端硬碟 / LINE Keep", fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { exportLauncher.launch("RelateStats_Backup.txt") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                            Text("存成檔案", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Button(onClick = { importLauncher.launch(arrayOf("text/plain")) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = AppColors.ActiveBlue), shape = RoundedCornerShape(12.dp)) {
                            Text("選擇檔案還原", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = AppColors.Surface), border = BorderStroke(1.dp, AppColors.BorderLight), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("資料管理", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("${records.size} 筆紀錄", color = AppColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }

                    if (autoCleanedCount > 0) {
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp).background(AppColors.FriendBg, RoundedCornerShape(8.dp)).padding(12.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Rounded.Info, contentDescription = null, tint = AppColors.FriendText, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("已自動清理 $autoCleanedCount 筆舊版計時錯誤留下的異常紀錄，清理前的原始資料已另存一份在 App 內部空間。", color = AppColors.FriendText, fontSize = 12.sp, lineHeight = 17.sp)
                        }
                    }

                    if (abnormalCount > 0) {
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp).background(AppColors.WorkBg, RoundedCornerShape(8.dp)).padding(12.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Rounded.Warning, contentDescription = null, tint = AppColors.WorkText, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("偵測到 $abnormalCount 筆異常紀錄：單筆停留時間超過 30 分鐘上限，是舊版計時錯誤的產物，會讓總時長暴增到不合理的數字。", color = AppColors.WorkText, fontSize = 12.sp, lineHeight = 17.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { confirmAction = "abnormal" },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.WorkText),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("清理 $abnormalCount 筆異常紀錄", fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG)
                                == PackageManager.PERMISSION_GRANTED
                            ) runCallImport() else callLogLauncher.launch(Manifest.permission.READ_CALL_LOG)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Phone, contentDescription = null, modifier = Modifier.size(16.dp), tint = AppColors.TextSecondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("匯入手機通話紀錄（補齊真實通話時長）", fontSize = 12.sp, color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    callImportMsg?.let {
                        Text(it, fontSize = T.Label, color = AppColors.FriendText, modifier = Modifier.padding(top = 6.dp), lineHeight = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = { setShowDataManager(true) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = AppColors.TextSecondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("清除資料（今日／本週／指定日期／全部）", fontSize = 12.sp, color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { confirmAction = "retag" },
                            modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)
                        ) { Text("整理系統通知", fontSize = T.Label, color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis) }

                        OutlinedButton(
                            onClick = { confirmAction = "words" },
                            modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)
                        ) { Text("清除常用詞", fontSize = T.Label, color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = {
                            val n = viewModel.purgeJunkWords()
                            Toast.makeText(
                                context,
                                if (n > 0) "已清掉 $n 個系統通知與人名碎片" else "沒有找到需要清的詞",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("只清常用詞裡的系統通知與人名（不歸零）",
                            fontSize = T.Label, color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = { showRecordManager = true },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                    ) { Text("逐筆檢視／勾選刪除紀錄", fontSize = T.Body, color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis) }

                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = { showDiagnostics = true },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                    ) { Text("停留時間偵測診斷", fontSize = T.Body, color = AppColors.ActiveBlue, maxLines = 1, overflow = TextOverflow.Ellipsis) }

                    if (undoAvailable) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().background(AppColors.FamilyBg, RoundedCornerShape(10.dp)).padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text("可復原上一次刪除", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppColors.FamilyText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (undoLabel.isNotBlank()) {
                                    Text(undoLabel, fontSize = 11.sp, color = AppColors.FamilyText.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            Button(
                                onClick = {
                                    val ok = viewModel.undoLastDestructive()
                                    Toast.makeText(context, if (ok) "已復原" else "復原失敗，快照可能已失效", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.FamilyText),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) { Text("復原", fontSize = 12.sp, maxLines = 1, softWrap = false) }
                        }
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = AppColors.FriendBg.copy(alpha = 0.5f)), border = BorderStroke(1.dp, AppColors.FriendText.copy(alpha = 0.2f)), shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("☁️ Google 帳號自動備份", color = AppColors.FriendText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("已啟用 Android 自動備份：資料會綁定這支手機登入的 Google 帳號，換機或重灌後重新安裝即可自動還原，不需手動操作。系統上限 25MB，超過時請改用上方的雲端備份。", fontSize = 11.sp, color = AppColors.FriendText.copy(alpha = 0.85f), lineHeight = 16.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }

        item {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("RelateStats v${BuildConfig.VERSION_NAME}", fontSize = 11.sp, color = AppColors.TextSecondary.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
                Text("© ${Calendar.getInstance().get(Calendar.YEAR)} RelateStats. All rights reserved.", fontSize = 10.sp, color = AppColors.TextSecondary.copy(alpha = 0.5f), modifier = Modifier.padding(top = 2.dp))
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    if (showCategoryManager) {
        CategoryManagerDialog(viewModel, availableCats) { showCategoryManager = false }
    }

    if (showRecordManager) {
        RecordManagerDialog(viewModel) { showRecordManager = false }
    }

    if (showDiagnostics) {
        DetectionDiagnosticsDialog(viewModel) { showDiagnostics = false }
    }

    when (confirmAction) {
        "retag" -> {
            val targets = remember { viewModel.previewRetagTargets() }
            ConfirmDialog(
                title = "整理系統通知",
                message = if (targets.isEmpty()) "目前沒有需要歸類的項目。"
                else "以下 ${targets.size} 個聯絡人會被歸類到「系統通知」標籤，排行榜將不再顯示。\n互動紀錄不會刪除，之後仍可自行改回其他標籤。",
                details = targets,
                confirmLabel = if (targets.isEmpty()) "知道了" else "歸類 ${targets.size} 個",
                onConfirm = {
                    val n = viewModel.retagSystemContacts()
                    Toast.makeText(context, if (n > 0) "已歸類 $n 個系統通知" else "沒有需要歸類的項目", Toast.LENGTH_SHORT).show()
                    confirmAction = null
                },
                onDismiss = { confirmAction = null }
            )
        }
        "words" -> ConfirmDialog(
            title = "清除常用詞統計",
            message = "會刪除目前累積的 ${wordFreqNow.size} 個詞的統計資料，總覽頁的「常用詞」會歸零重新累積。\n互動紀錄與訊息數不受影響，此操作無法復原。",
            confirmLabel = "清除",
            onConfirm = {
                viewModel.clearWordFreq()
                Toast.makeText(context, "已清除常用詞統計", Toast.LENGTH_SHORT).show()
                confirmAction = null
            },
            onDismiss = { confirmAction = null }
        )
        "abnormal" -> ConfirmDialog(
            title = "清理異常紀錄",
            message = "會刪除 $abnormalCount 筆停留時間超過 30 分鐘上限的紀錄，這些是舊版計時錯誤的產物。\n訊息紀錄不受影響，刪除後可在下方「復原」。",
            confirmLabel = "清理 $abnormalCount 筆",
            onConfirm = {
                val removed = viewModel.removeAbnormalRecords()
                abnormalCount = viewModel.countAbnormalRecords()
                Toast.makeText(context, "已清理 $removed 筆異常紀錄", Toast.LENGTH_SHORT).show()
                confirmAction = null
            },
            onDismiss = { confirmAction = null }
        )
    }

    if (showGuideDialog) {
        AlertDialog(
            containerColor = AppColors.Surface, onDismissRequest = { setShowGuideDialog(false) },
            title = { Text("📖 App 核心運作邏輯", fontWeight = FontWeight.Bold) },
            text = {
                // heightIn(max)：內容本來就可捲動，固定高度只會在小螢幕/大字體時把按鈕頂掉
                Column(modifier = Modifier.heightIn(max = 350.dp).padding(vertical = 8.dp)) {
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