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
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.text.TextUtils
import android.util.Base64
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.foundation.verticalScroll
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
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
/**
 * 一組圖表配色。
 *
 * ⚠️ 這個只換**圖表**的顏色，不換 App 本身。
 *    App 的灰白藍是介面骨架（背景、卡片、按鈕、連結），換掉會變成另一個 App；
 *    使用者要挑的是資料的顏色 —— 圓餅圖切片、長條、格子色階。
 *
 * colors[0] 同時是單色圖表（長條圖、色階格子）的主色，所以每組的第一個
 * 都必須是「單獨用也夠深、看得清楚」的顏色。
 *
 * 不進配色的東西：
 *  · 標籤色（家人／同事…）—— 使用者自己挑的，覆蓋掉等於把設定弄丟
 *  · 「上升／下降」的綠紅 —— 那是語意，不是裝飾
 */
/**
 * 一個標籤 chip 的樣式。
 *
 * ⚠️ bg 與 text 是**各自獨立的色值**，不是從彼此算出來的。
 *    舊版是「text 壓到 15% 透明度當 bg」，那在同一組裡會讓所有標籤的底色
 *    深淺一模一樣，只剩文字色有差 —— 小色塊上根本分不出來。
 *    現在兩個都明寫，底色也刻意做出深淺差。
 * @param dashed 透明底＋虛線框。留給「未分類」——它代表「還沒分」，
 *               不該長得像一個真正的標籤。
 */
data class TagStyle(val bg: Color, val text: Color, val dashed: Boolean = false)

data class ChartPalette(
    val id: String,
    /**
     * 介面強調色：連結、被選中的日期、導覽列高亮、輸入框游標。
     * ⚠️ 這個一定要跟著配色走。只換圖表與標籤的話，
     *    選了暖色配色卻看到藍色的「本週」與藍色的分頁高亮，整個畫面就是散的。
     */
    val accent: Color,
    /** 深色：填色按鈕、被選中的分頁、App 標記。配色的最深階。 */
    val brand: Color,
    /**
     * 圖表用色（6 色）。colors[0] 同時是單色圖表（長條、色階格子）的主色，必須夠深。
     * ⚠️ 設定頁色盤選擇畫面上方的預覽色條吃的就是這 6 色。
     */
    val colors: List<Color>,
    /**
     * 標籤 chip 用色（7 組），固定順序：
     *   0 伴侶／男友　1 家人　2 朋友　3 工作　4 其他　5 系統通知　6 未分類
     * ⚠️ 跟 colors 是**兩份獨立的資料**，不互相衍生，只要求色相彼此呼應。
     * ⚠️ 0–4 要靠明度差分開，不能只差色相 —— chip 很小，只有明度差看得出來。
     * ⚠️ 索引 6（未分類）是 dashed 的，所以自訂標籤配色只能挑 0–5，
     *    不然使用者會挑到一個「看起來沒分類」的樣式。
     */
    val tags: List<TagStyle>
) {
    val primary: Color get() = colors[0]
    fun at(i: Int): Color = colors[((i % colors.size) + colors.size) % colors.size]
    fun tagStyle(i: Int): TagStyle = tags[((i % tags.size) + tags.size) % tags.size]
    fun tag(i: Int): Color = tagStyle(i).text
    fun tagBg(i: Int): Color = tagStyle(i).bg
}

/**
 * 配色。
 *
 * 換的是「強調色 + 圖表色 + 標籤色」，**不換中性色** ——
 * 背景、卡片、文字、分隔線永遠是那套灰白，所以不管選哪一組，
 * App 看起來都還是同一個 App，只是換了個調性。
 */
val CHART_PALETTES = listOf(
    // A 霧灰藍
    ChartPalette(
        "mistblue",
        accent = Color(0xFF4A6373), brand = Color(0xFF3E4A54),
        colors = listOf(
            Color(0xFF3E4A54), Color(0xFF6E8695), Color(0xFFC08A85),
            Color(0xFFD3A45E), Color(0xFF6E8078), Color(0xFFDEDEDC)
        ),
        tags = listOf(
            TagStyle(Color(0xFFF4E3E1), Color(0xFF9A554F)),   // 伴侶：霧玫瑰
            TagStyle(Color(0xFFE3ECF3), Color(0xFF3C5B6E)),   // 家人：霧藍
            TagStyle(Color(0xFFE7EEEA), Color(0xFF4A6459)),   // 朋友：霧綠
            TagStyle(Color(0xFFF8EEDB), Color(0xFF8A6418)),   // 工作：赭黃
            TagStyle(Color(0xFFDFE3E7), Color(0xFF39454F)),   // 其他：深板岩
            TagStyle(Color(0xFFEFEFEE), Color(0xFF7C8287)),   // 系統通知：安靜灰
            TagStyle(Color.Transparent, Color(0xFF8C939A), dashed = true) // 未分類
        )
    ),
    // B 酒紅暖調
    ChartPalette(
        "wine",
        accent = Color(0xFFA8483C), brand = Color(0xFF6B322C),
        colors = listOf(
            Color(0xFF6B322C), Color(0xFFC0685C), Color(0xFFCC7C9C),
            Color(0xFFB49B6C), Color(0xFF6E9A97), Color(0xFFE6DCD6)
        ),
        tags = listOf(
            TagStyle(Color(0xFFF8E2DD), Color(0xFFA8483C)),   // 伴侶：赤陶
            TagStyle(Color(0xFFF4DCE5), Color(0xFF9C4466)),   // 家人：玫瑰
            TagStyle(Color(0xFFDFEDEC), Color(0xFF3C6E6A)),   // 朋友：霧青
            TagStyle(Color(0xFFF3ECD9), Color(0xFF7A652C)),   // 工作：卡其
            TagStyle(Color(0xFFEBDBD8), Color(0xFF5E2A25)),   // 其他：酒紅
            TagStyle(Color(0xFFF0EBE8), Color(0xFF877C75)),   // 系統通知
            TagStyle(Color.Transparent, Color(0xFF9C918A), dashed = true)
        )
    ),
    // C 抹茶灰綠
    ChartPalette(
        "matcha",
        accent = Color(0xFF5F7A2E), brand = Color(0xFF46463E),
        colors = listOf(
            Color(0xFF46463E), Color(0xFF84847C), Color(0xFFC79290),
            Color(0xFF92B356), Color(0xFF93A38F), Color(0xFFDCDCD4)
        ),
        tags = listOf(
            TagStyle(Color(0xFFF6E4E2), Color(0xFF9E5B57)),   // 伴侶：霧粉
            TagStyle(Color(0xFFE9F1DA), Color(0xFF587522)),   // 家人：抹茶
            TagStyle(Color(0xFFE6EDE5), Color(0xFF48634C)),   // 朋友：灰綠
            TagStyle(Color(0xFFF1EEDC), Color(0xFF6B5F30)),   // 工作：橄欖
            TagStyle(Color(0xFFE4E4DF), Color(0xFF3F3F37)),   // 其他：深橄欖
            TagStyle(Color(0xFFF1F1ED), Color(0xFF87877D)),   // 系統通知
            TagStyle(Color.Transparent, Color(0xFF95958C), dashed = true)
        )
    ),
    // D 大地棕調
    ChartPalette(
        "earth",
        accent = Color(0xFFA85F14), brand = Color(0xFF3A2E2C),
        colors = listOf(
            Color(0xFF3A2E2C), Color(0xFF6E5F5A), Color(0xFFBC8CA8),
            Color(0xFFDD8C34), Color(0xFF7E8E80), Color(0xFFE0D6CC)
        ),
        tags = listOf(
            TagStyle(Color(0xFFF5E2EC), Color(0xFF8A4A6C)),   // 伴侶：藕紫
            TagStyle(Color(0xFFFBEAD4), Color(0xFFA05A10)),   // 家人：琥珀
            TagStyle(Color(0xFFE7EDE7), Color(0xFF4C6350)),   // 朋友：灰綠
            TagStyle(Color(0xFFEDE7E4), Color(0xFF5C4E49)),   // 工作：灰褐
            TagStyle(Color(0xFFE5DDD9), Color(0xFF352A28)),   // 其他：深棕
            TagStyle(Color(0xFFF2EDEA), Color(0xFF8C817B)),   // 系統通知
            TagStyle(Color.Transparent, Color(0xFF9E938D), dashed = true)
        )
    ),
    // E 山巒暮色
    ChartPalette(
        "dusk",
        accent = Color(0xFF3A554E), brand = Color(0xFF22332E),
        colors = listOf(
            Color(0xFF22332E), Color(0xFF4A625C), Color(0xFFE0AE86),
            Color(0xFF92A896), Color(0xFF6E6A5E), Color(0xFFDCDAD2)
        ),
        tags = listOf(
            TagStyle(Color(0xFFFBEADA), Color(0xFFA4663A)),   // 伴侶：杏
            TagStyle(Color(0xFFE1ECE8), Color(0xFF35534C)),   // 家人：松綠
            TagStyle(Color(0xFFE8EFE9), Color(0xFF4C6A56)),   // 朋友：霧綠
            TagStyle(Color(0xFFDFE6E2), Color(0xFF1E2F2A)),   // 工作：深林
            TagStyle(Color(0xFFEEEBE2), Color(0xFF5C584C)),   // 其他：暖灰
            TagStyle(Color(0xFFF2F1EB), Color(0xFF8A8778)),   // 系統通知
            TagStyle(Color.Transparent, Color(0xFF929182), dashed = true)
        )
    )
)

object AppColors {
    /**
     * ⚠️ 必須是 Compose 的 MutableState，不能是普通 var。
     *    Chart 寫成 get()，讀取時等於讀了這個 state ——
     *    這樣換配色才會自動觸發重組。改成普通 var 的話畫面不會更新。
     */
    val chartPaletteState = mutableStateOf(CHART_PALETTES[0])
    val Chart: ChartPalette get() = chartPaletteState.value

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

    // ── 強調（跟著配色走）──
    // ⚠️ 中性色（Background / Surface / Text / Border）**不跟著變**，那是骨架。
    //    但強調色一定要跟 —— 選了粉色配色卻看到藍色的「本週」跟藍色分頁高亮，
    //    畫面就是散的。這是 v3.16 的判斷失誤，v3.21 修正。
    // ⚠️ 畫資料還是請用 AppColors.Chart，不要用 ActiveBlue。
    //    兩者在預設配色下同色，但換了配色就不一樣了。
    val BrandPrimary: Color get() = Chart.brand
    val ActiveBlue: Color get() = Chart.accent
    /** ⚠️ 語意色：專門表示「上升／正向」。不要拿它來畫圖表。 */
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

// 標籤可挑選的配色。⚠️ 直接來自目前的圖表配色 ——
    // 標籤與圖表是同一批資料的兩種呈現，配色分家會很怪。
    // ⚠️ 只給 0..5，不給 6（未分類的虛線樣式）
    val CategoryPalette: List<Pair<Color, Color>> get() =
        (0..5).map { Chart.tag(it) to Chart.tagBg(it) }

    /** 舊版寫死的標籤色，只用於把舊資料轉成索引 */
    val LegacyTagColors = listOf(
        Color(0xFF1D4ED8), Color(0xFFBE185D), Color(0xFF047857), Color(0xFFB91C1C),
        Color(0xFFB45309), Color(0xFF4B5563), Color(0xFF9333EA), Color(0xFFC026D3),
        Color(0xFFE11D48), Color(0xFF0D9488), Color(0xFF0284C7), Color(0xFF4F46E5),
        Color(0xFF059669)
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

/**
 * 標籤。
 *
 * ⚠️ colorInt / bgColorInt 有兩種意義：
 *   · 0..15  → 配色裡的第幾個顏色（跟著「圖表配色」一起變）
 *   · 其他    → 舊資料留下的絕對色值，照原樣用
 * 真正的 ARGB 一定帶 alpha（0xFF……），轉成 Int 是負數，不可能落在 0..15，
 * 所以兩者不會撞在一起。舊資料會在 loadData 時一次性轉成索引。
 */
data class ContactCategory(val id: String, val label: String, val colorInt: Int, val bgColorInt: Int) {
    val color: Color get() = if (colorInt in 0..15) AppColors.Chart.tag(colorInt) else Color(colorInt)
    val bgColor: Color get() = if (bgColorInt in 0..15) AppColors.Chart.tagBg(bgColorInt) else Color(bgColorInt)
    /** 透明底＋虛線框（只有「未分類」是這樣）。舊資料用絕對色值的一律不是。 */
    val dashed: Boolean get() = colorInt in 0..15 && AppColors.Chart.tagStyle(colorInt).dashed
    fun toCsvString() = listOf(id, label, colorInt.toString(), bgColorInt.toString()).joinToString("|cat|")
    companion object {
// 數字是配色索引，不是色值 —— 換配色時這些標籤會跟著換色。
        // ⚠️ 索引對應 ChartPalette.tags 的固定順序：
        //    0 伴侶　1 家人　2 朋友　3 工作　4 其他　5 系統通知　6 未分類
        val PARTNER = ContactCategory("PARTNER", "伴侶", 0, 0)
        val FAMILY = ContactCategory("FAMILY", "家人", 1, 1)
        val FRIEND = ContactCategory("FRIEND", "朋友", 2, 2)
        val WORK = ContactCategory("WORK", "工作", 3, 3)
        // IG 讚／追蹤者／串文推播等非真人通知統一歸到這裡，排行預設不顯示
        val SYSTEM = ContactCategory("SYSTEM", "系統通知", 5, 5)
        val OTHER = ContactCategory("OTHER", "未分類", 6, 6)

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

enum class FilterType { SPECIFIC_DAY, WEEK, RANGE, ALL }

/**
 * @param timestamp SPECIFIC_DAY/WEEK 是「這一天」；RANGE 是區間起點。
 * @param endTimestamp 只有 RANGE 會用到，區間終點（含）。
 */
data class DateFilter(
    val type: FilterType,
    val timestamp: Long = System.currentTimeMillis(),
    val endTimestamp: Long = System.currentTimeMillis()
) {
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
    "reels", "explore", "profile", "story", "stories", "notifications",
    // 以下是實際在使用者機器上被誤收成「聯絡人」的介面字，逐一補進來。
    // ⚠️ 只放「不可能有人叫這個名字」的字。像「回覆」「社群」這種兩字詞很安全，
    //    但不要放「小明」「阿姨」這類可能是暱稱的字。
    "回覆", "社群", "返回首頁", "關閉瀏覽器", "功能表", "相片預覽", "洞察報告",
    "更多選項", "上一頁", "下一頁", "開啟", "展開", "收合", "選取", "全選",
    "播放", "暫停", "靜音", "新增", "草稿", "已讀", "輸入中", "表情符號",
    "相簿", "檔案", "位置資訊", "搜尋訊息", "封鎖", "檢舉", "說明", "隱私權", "服務條款"
)

private val NON_PERSON_EXTRA = listOf(
    // 「1.2萬 個讚」「按讚數 345」這種數字型標籤
    Regex("^[\\d.,]+\\s*(萬|k|K|M)?\\s*(個)?\\s*(讚|留言|次觀看|views|likes)"),
    Regex("^(按讚數|觀看次數|留言數|分享次數)"),
    // Reels / 貼文殘骸
    Regex("(reels?|限時動態|連續短片)\\s*$", RegexOption.IGNORE_CASE),
    // 機構／官方帳號：這些字出現在名稱裡幾乎都是推播來源。
    // ⚠️ 這條規則比較寬，理論上會誤傷名字裡有「銀行」的群組。
    //    可接受的原因是判定結果只是「歸到系統通知標籤」，不是刪除，隨時可以改回來。
    Regex("(禮物通知|系統通知|安全通知|驗證碼|一次性密碼|銀行|人壽|產險|證券|信用卡|客服中心|官方)")
)

/**
 * 判斷是否為非真人通知（讚、追蹤、推播、促銷、介面文字）。
 *
 * ⚠️ 這個函式是統計的熱點：活躍密度、新出現的人、連續互動都會**逐筆紀錄**呼叫它，
 *    五萬多筆就是五萬多次、每次跑十幾條 Regex —— 這是「切日期要跑很久」的主因之一。
 *    名稱其實只有幾百種，所以結果直接記憶化。
 *    ConcurrentHashMap：統計跑在背景執行緒，通知服務也會呼叫，兩邊會同時進來。
 */
private val nonPersonCache = java.util.concurrent.ConcurrentHashMap<String, Boolean>()

fun isNonPersonContact(name: String): Boolean {
    // 名稱數量正常是幾百個；真的爆掉就整個丟掉重算，不要無限長大
    if (nonPersonCache.size > 8000) nonPersonCache.clear()
    return nonPersonCache.getOrPut(name) {
        val n = name.trim()
        when {
            n.isEmpty() -> true
            n.lowercase() in UI_LABEL_NAMES -> true
            NON_PERSON_PATTERNS.any { it.containsMatchIn(n) } -> true
            else -> NON_PERSON_EXTRA.any { it.containsMatchIn(n) }
        }
    }
}

/**
 * 這個名稱為什麼被判成非真人 —— 給使用者看的白話版。
 *
 * ⚠️ 順序必須跟 isNonPersonContact 完全一致，否則理由會對不上實際命中的規則。
 * 規則本來就有可能誤判（群組真的叫「推薦好物」就會中），
 * 所以一定要把理由攤開來，使用者才有辦法判斷要不要放它一馬。
 */
fun junkReason(name: String): String {
    val n = name.trim()
    if (n.isEmpty()) return "名稱是空的"
    if (n.lowercase() in UI_LABEL_NAMES) return "這是 App 介面上的字，不是聊天室"
    if (NON_PERSON_PATTERNS.any { it.containsMatchIn(n) }) return "名稱符合系統通知格式（讚／追蹤／貼文／推播）"
    if (NON_PERSON_EXTRA.any { it.containsMatchIn(n) }) return "名稱是數字型標籤（讚數、觀看次數等）"
    return "手動標記"
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
    "來電", "通話時", "則新", "新訊息", "邀請", "加入了", "退出了", "拍了拍", "分享了", "回覆了",
    // ⚠️ 以下是實際回報漏掉的。整句過濾（isSystemGeneratedText）本來就擋得住這些句子，
    //    但**已經存進詞頻的是 n-gram 碎片**：「標註您的訊息內容」會被切成
    //    「標註」「註您」「您的」「訊息」「內容」…… 沒有一個含有上面舊的標記字串，
    //    所以清理完全清不到。清理用的標記必須跟得上過濾規則。
    "標註", "傳達", "按了", "回覆", "回應", "提及", "動態", "訊息", "內容", "留言",
    "貼上", "撤銷", "已傳", "新增了", "查看全部", "訊息要求"
)
private val JUNK_WORD_EXACT: Set<String> by lazy {
    // 這份語料要涵蓋 SYSTEM_PHRASE / SYSTEM_HINT 擋得住的每一種句型，
    // 否則「過濾擋得住、清理清不掉」，使用者看到的還是舊資料。
    val corpus = listOf(
        "傳送了一則貼圖", "傳送了一張照片", "傳送了一個檔案", "傳送了一段影片",
        "傳送了語音訊息", "傳送了一則訊息", "收回了訊息", "分享了一則貼文",
        "你有一則新訊息", "已傳送", "正在輸入中", "加入了聊天", "退出了聊天",
        // 使用者實際點名的五種
        "標註您的訊息內容", "標註你的訊息內容", "已傳送貼圖", "對你的訊息按了讚",
        "對您的訊息按了", "傳達了", "已傳達了", "已回覆了你的限時動態",
        "回覆了你的限時動態", "回應了你的限時動態", "提及了你", "在留言中提到你",
        "拍了拍你", "邀請你加入", "開始追蹤你", "查看全部訊息", "訊息要求"
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
    val subSender: String? = null,
    /**
     * 通話方向：0 不知道、1 對方打來、2 我打出去。
     * ⚠️ 只有真的抓得到證據才標（先看到來電鈴響＝對方打來），抓不到就留 0 顯示「通話」，不要猜。
     */
    val callDirection: Int = 0
) {
    // 12 個欄位；仍相容舊的 8 欄位資料
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
        subSender?.let { sanitizeForCsv(it) } ?: "",
        callDirection.toString()
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
                    subSender = parts.getOrNull(10)?.takeIf { it.isNotBlank() },
                    callDirection = parts.getOrNull(11)?.toIntOrNull() ?: 0
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

/**
 * 釘在列表頂端的日期列。
 *
 * 往下滑之後還是要看得出「現在在看哪一天」，否則捲到一半根本忘了範圍。
 * 背景必須不透明並帶下緣分隔線，不然捲動的內容會從底下透出來。
 */
@Composable
fun StickyDateBar(
    filter: DateFilter,
    onChange: (DateFilter) -> Unit,
    weekStartDay: Int,
    trailing: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(AppColors.Background)
            // 上方留白：頂端標題列的分隔線緊貼著分頁列，看起來很擠。
            // 背景寫在 padding 之前，捲動時內容才不會從留白處透出來。
            .padding(top = 5.dp, bottom = 8.dp)
    ) {
        TimeFilterSelector(filter, onChange, weekStartDay, trailing)
        Spacer(modifier = Modifier.height(6.dp))
        HorizontalDivider(color = AppColors.BorderLight)
    }
}

/**
 * 左右滑動切換日期／週。
 *
 * ⚠️ 掛在整頁的容器上，不是掛在日期列上。
 *    掛在日期列的話，分頁鈕與箭頭都有自己的 clickable，會先吃掉手勢，滑起來時靈時不靈。
 * ⚠️ 垂直捲動不受影響：LazyColumn 只消費垂直方向的拖曳，
 *    水平方向沒人消費，事件才會冒泡到這裡。頁面內用了 horizontalScroll 的橫向清單
 *    （標籤列、平台列）會自己消費掉，滑那幾條不會誤觸換日期，這是對的。
 * 「全部」與「區間」沒有上一期／下一期可言，直接不掛。
 */
fun Modifier.dateSwipe(filter: DateFilter, onChange: (DateFilter) -> Unit): Modifier =
    if (filter.type == FilterType.ALL || filter.type == FilterType.RANGE) this
    else this.pointerInput(filter) {
        var total = 0f
        detectHorizontalDragGestures(
            onDragStart = { total = 0f },
            onDragEnd = {
                val step = if (filter.type == FilterType.SPECIFIC_DAY) 1 else 7
                // 門檻要夠大，不然直向捲動時手指稍微歪掉就換日期
                if (total <= -90f) onChange(filter.copy(timestamp = shiftDays(filter.timestamp, step)))
                else if (total >= 90f) onChange(filter.copy(timestamp = shiftDays(filter.timestamp, -step)))
            }
        ) { _, dragAmount -> total += dragAmount }
    }

private val shiftCal = ThreadLocal.withInitial { Calendar.getInstance() }

fun shiftDays(ts: Long, days: Int): Long {
    val c = shiftCal.get()!!
    c.timeInMillis = ts
    c.add(Calendar.DATE, days)
    return c.timeInMillis
}

/** 頂層版的當日零點；ViewModel 內另有一份私有實作 */
private val topDayCal = ThreadLocal.withInitial { Calendar.getInstance() }

fun startOfDayMs(ts: Long): Long {
    val c = topDayCal.get()!!
    c.timeInMillis = ts
    c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

private val dowCal = ThreadLocal.withInitial { Calendar.getInstance() }

/** ⚠️ 會被逐筆呼叫，不能在裡面 new Calendar */
fun isWeekend(ts: Long): Boolean {
    val c = dowCal.get()!!
    c.timeInMillis = ts
    val d = c.get(Calendar.DAY_OF_WEEK)
    return d == Calendar.SATURDAY || d == Calendar.SUNDAY
}

/**
 * 洞察頁的一個可排序區塊。
 * body 是 @Composable，先收集起來、依使用者存的順序再輸出。
 */
class Sec(val id: String, val label: String, val body: @Composable () -> Unit)

/**
 * 洞察區塊的完整清單（id 與顯示名稱）。
 * ⚠️ 排序對話框吃這份常數，不是吃「這次畫了哪些」——
 *    有些區塊在沒資料時不會畫出來（例如通話統計），
 *    但使用者還是應該能先幫它排好位置。
 *    新增區塊時這裡要一起加，順序即預設順序。
 */
val INSIGHT_SECTIONS: List<Pair<String, String>> = listOf(
    "delta" to "與上期比較",
    "trend" to "逐日趨勢",
    "avg" to "每日平均",
    // ⚠️ 這兩張都是格子圖，長得像但問的問題完全不同。
    //    名稱一定要直接寫出「它回答什麼」，不然第一次看只看得到兩片色塊：
    //      哪幾天在聊 = 看**日曆**（近 17 週，哪幾天熱）
    //      幾點在聊   = 看**時鐘**（一天 24 小時，哪幾個鐘頭熱）
    "contrib" to "哪幾天在聊",
    "timeofday" to "時段分佈",
    "heatmap" to "幾點在聊",
    "words" to "常用詞",
    "conc" to "社交集中度",
    "weekday" to "平日 vs 週末",
    "night" to "深夜社交",
    "streaks" to "連續互動排行",
    "cross" to "跨平台聯絡",
    "persona" to "社交人格",
    "churn" to "新出現的人"
)

/** 活躍密度的一格：某一天的互動次數與聊天人數 */
data class ContribDay(val dayStart: Long, val count: Int, val people: Int)

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
    val totalSeconds: Long, val timed: Int = 0,
    /** 對方打來（含未接）、我打出去、分不出方向的通數 */
    val incoming: Int = 0, val outgoing: Int = 0, val unknownDir: Int = 0
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
data class CategoryStat(
    val category: ContactCategory,
    val totalDurationSeconds: Long,
    val incomingCount: Int,
    /** 這個標籤底下有幾位聯絡人（本期有互動的）*/
    val peopleCount: Int = 0
)
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
    val chartPaletteId = MutableStateFlow(CHART_PALETTES[0].id)
    val recapEnabled = MutableStateFlow(true)
    /** 從回顧通知點進來時設成 true，主畫面看到就開概要彈窗 */
    val pendingRecap = MutableStateFlow(false)

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

    /**
     * 記錄「有接住、但不該發生」的例外。
     *
     * 統計算到一半丟例外時，如果讓它往上冒就是整個 App 閃退，
     * 而且沒有裝置在手邊根本無從得知是哪一行。改成接住 + 寫檔，
     * 使用者只要到設定頁按「複製」就能把堆疊給我。
     */
    fun recordError(tag: String, err: Throwable) {
        val ctx = appContext ?: return
        runCatching {
            val sw = java.io.StringWriter()
            err.printStackTrace(java.io.PrintWriter(sw))
            val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            File(ctx.filesDir, CRASH_FILE).writeText(
                "時間：$stamp\n版本：${BuildConfig.VERSION_NAME}\n位置：$tag（已接住，未閃退）\n\n$sw"
            )
        }
        lastErrorTag.value = tag
    }

    val lastErrorTag = MutableStateFlow<String?>(null)

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
        // ⚠️ 每次補強 JUNK_WORD_MARKERS 或語料就要換一次旗標名稱，
        //    否則升級後不會重跑，使用者看到的還是舊的髒資料。
        if (p.getBoolean("word_purge_v30_done", false)) return
        lastWordPurge.value = purgeJunkWords()
        p.edit().putBoolean("word_purge_v30_done", true).apply()
    }

    fun init(context: Context) {
        if (prefs != null) return
        synchronized(this) {
            if (prefs != null) return
            appContext = context.applicationContext
            prefs = context.getSharedPreferences("RelateStatsData", Context.MODE_PRIVATE)
        }
        loadData()
        loadCloudBackupState()
        loadGroupState()
        val appContext = context.applicationContext
        ioScope.launch {
            runOneTimeCleanupIfNeeded(appContext)
            runWordPurgeIfNeeded()
            purgeExpiredTrash()
            performAutoBackupIfNeeded(appContext)
        }
    }

    // 每段獨立防護，單筆資料損毀不影響其餘載入
    fun loadData() {
        val p = prefs ?: return
        isFirstLaunch.value = p.getBoolean("is_first_launch", true)
        weekStartDay.value = p.getInt("week_start_day", Calendar.MONDAY)
        applyChartPalette(p.getString("chart_palette", null) ?: CHART_PALETTES[0].id)
        recapEnabled.value = p.getBoolean("recap_enabled", true)
        autoBackupEnabled.value = p.getBoolean("auto_backup_enabled", false)
        autoBackupPassword.value = p.getString("auto_backup_pwd", "") ?: ""
        lastAutoBackupTime.value = p.getLong("last_auto_backup_time", 0L)

        // 先讀新的檔案；沒有就從舊版的 SharedPreferences 搬過來（只會發生一次）
        val fromFile = readRecordsFile()
        if (fromFile != null) {
            _interactions.value = fromFile.sortedBy { it.timestamp }
            // 剛從檔案讀進來，內容跟檔案一致，不需要立刻回寫
            lastWrittenRecords = _interactions.value
        } else {
            _interactions.value = runCatching {
                (p.getStringSet("interactions", emptySet()) ?: emptySet())
                    .mapNotNull { InteractionRecord.fromCsvString(it) }
                    .sortedBy { it.timestamp }
            }.getOrDefault(emptyList())
            if (_interactions.value.isNotEmpty()) {
                writeRecordsFile()
                // 搬完就把舊鍵拿掉，否則每次啟動都要多解析一次幾萬筆
                runCatching { p.edit().remove("interactions").apply() }
            }
        }

        _aliases.value = runCatching { parsePairs(p.getStringSet("aliases", emptySet())) }.getOrDefault(emptyMap())
        _blockedNames.value = runCatching { p.getStringSet("blocked_names", emptySet())?.toSet() ?: emptySet() }.getOrDefault(emptySet())
        _junkIgnored.value = runCatching { p.getStringSet("junk_ignored", emptySet())?.toSet() ?: emptySet() }.getOrDefault(emptySet())
        _sectionOrder.value = runCatching {
            p.getString("section_order", "")?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        }.getOrDefault(emptyList())
        _hiddenSections.value = runCatching { p.getStringSet("hidden_sections", emptySet())?.toSet() ?: emptySet() }.getOrDefault(emptySet())
        _deletedNames.value = runCatching {
            (p.getStringSet("deleted_names", emptySet()) ?: emptySet()).mapNotNull { line ->
                val i = line.lastIndexOf("|=|")
                if (i > 0) line.substring(0, i) to (line.substring(i + 3).toLongOrNull() ?: 0L) else null
            }.toMap()
        }.getOrDefault(emptyMap())
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

        // ⚠️ 必須在 _availableCategories 讀完之後才跑。
        //    放在前面的話會對著舊值（或空清單）做轉換，然後把旗標設成已完成 ——
        //    既沒轉到東西，之後也不會再跑。
        migrateTagColorsToIndex(p)
        migrateTagSlots(p)

        _wordSenders.value = runCatching {
            val out = HashMap<String, HashMap<String, Int>>()
            (p.getStringSet("word_senders", emptySet()) ?: emptySet()).forEach { line ->
                val parts = line.split("\u0001")
                if (parts.size == 3) {
                    val c = parts[2].toIntOrNull() ?: return@forEach
                    out.getOrPut(parts[0]) { HashMap() }[parts[1]] = c
                }
            }
            out as Map<String, Map<String, Int>>
        }.getOrDefault(emptyMap())

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

    /**
     * 把舊資料的標籤色（絕對 ARGB）轉成配色索引，讓它們跟著配色走。
     *
     * ⚠️ 只跑一次，用旗標擋住。跑第二次沒有壞處（索引不在 LegacyTagColors 裡就不動），
     * 但每次啟動都掃一遍沒有意義。
     * ⚠️ 對不上舊色票的（使用者從別版本帶過來的怪色）保留原樣，
     * 不要硬塞一個索引 —— 那會讓他挑好的顏色無聲無息被換掉。
     */
    private fun migrateTagColorsToIndex(p: SharedPreferences) {
        if (p.getBoolean("tag_palette_v317_done", false)) return
        val legacy = AppColors.LegacyTagColors.map { it.toArgb() }
        val migrated = _availableCategories.value.map { c ->
            val idx = legacy.indexOf(c.colorInt)
            if (c.colorInt in 0..15 || idx < 0) c else c.copy(colorInt = idx, bgColorInt = idx)
        }
        if (migrated != _availableCategories.value) {
            _availableCategories.value = migrated
            saveData(immediate = true)
        }
        p.edit().putBoolean("tag_palette_v317_done", true).apply()
    }

    /**
     * 把舊的標籤色索引搬到新的七格順序。
     *
     * 舊順序：家人 0、伴侶 1、朋友 2、工作 3、未分類 6、系統通知 7
     * 新順序：伴侶 0、家人 1、朋友 2、工作 3、其他 4、系統通知 5、未分類 6
     *
     * ⚠️ 不搬的話會出兩個問題：
     *   1. 家人與伴侶的顏色對調（純外觀，但看起來像壞掉）
     *   2. 系統通知停在索引 7 —— 新的 tags 只有 0..6，取模之後會拿到索引 0，
     *      「系統通知」會長得跟「伴侶」一模一樣。
     * ⚠️ 自訂標籤如果落在 5 或 6，也要拉回來 ——
     *    那兩格是系統通知與未分類的保留樣式（尤其 6 是虛線，代表「還沒分類」）。
     */
    private fun migrateTagSlots(p: SharedPreferences) {
        if (p.getBoolean("tag_slots_v323_done", false)) return
        val fixed = mapOf(
            ContactCategory.PARTNER.id to 0, ContactCategory.FAMILY.id to 1,
            ContactCategory.FRIEND.id to 2, ContactCategory.WORK.id to 3,
            ContactCategory.SYSTEM.id to 5, ContactCategory.OTHER.id to 6
        )
        val next = _availableCategories.value.map { c ->
            val slot = fixed[c.id]
            when {
                slot != null -> c.copy(colorInt = slot, bgColorInt = slot)
                c.colorInt in 5..15 -> c.copy(colorInt = 4, bgColorInt = 4)
                else -> c
            }
        }
        if (next != _availableCategories.value) {
            _availableCategories.value = next
            saveData(immediate = true)
        }
        p.edit().putBoolean("tag_slots_v323_done", true).apply()
    }

    private fun applyChartPalette(id: String) {
        val p = CHART_PALETTES.find { it.id == id } ?: CHART_PALETTES[0]
        chartPaletteId.value = p.id
        AppColors.chartPaletteState.value = p
    }

    fun setRecapEnabled(on: Boolean) {
        recapEnabled.value = on
        prefs?.edit()?.putBoolean("recap_enabled", on)?.apply()
    }

    fun setChartPalette(id: String) {
        applyChartPalette(id)
        prefs?.edit()?.putString("chart_palette", chartPaletteId.value)?.apply()
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
                // 4 秒：訊息常常是一串一串進來，合併成一次寫入
                delay(4000)
                writeToDisk()
            }
        }
    }

    /**
     * ⚠️ 互動紀錄**不能**放 SharedPreferences。
     *
     * 舊版每次存檔都把全部紀錄（實測 55,000+ 筆）轉成 CSV、丟進 HashSet、
     * 再交給 SharedPreferences 寫 XML —— 光序列化就要好幾秒，而通知一來
     * 就觸發一次（1.5 秒防抖），所以「每次通知跳出來 App 都會 LAG」。
     *
     * 改成單一純文字檔：一行一筆，順序寫入，沒有 XML 跳脫也沒有 HashSet。
     * 另外加 dirty 旗標，紀錄沒變動時連檔案都不碰。
     */
    private const val RECORDS_FILE = "RelateStats_Records.txt"

    /**
     * 上次寫進檔案的那份清單「本身」。
     *
     * ⚠️ 用參考比對而不是手動的 dirty 旗標：紀錄有十幾個地方會被改
     * （新增、批次匯入、刪除、還原、清理、匯入備份…），
     * 漏標任何一個就是資料沒存到。每次修改都會產生新的 List 實例，
     * 所以 `!==` 一定抓得到，而且是 O(1)。
     */
    private var lastWrittenRecords: List<InteractionRecord>? = null

    private fun writeRecordsFile() {
        val ctx = appContext ?: return
        val snapshot = _interactions.value
        runCatching {
            File(ctx.filesDir, RECORDS_FILE).bufferedWriter().use { w ->
                snapshot.forEach { w.write(it.toCsvString()); w.newLine() }
            }
            lastWrittenRecords = snapshot
        }
    }

    private fun readRecordsFile(): List<InteractionRecord>? {
        val ctx = appContext ?: return null
        return runCatching {
            val f = File(ctx.filesDir, RECORDS_FILE)
            if (!f.exists()) return null
            f.readLines().mapNotNull { InteractionRecord.fromCsvString(it) }
        }.getOrNull()
    }

    private fun writeToDisk() {
        val p = prefs ?: return
        if (_interactions.value !== lastWrittenRecords) writeRecordsFile()
        runCatching {
            val aliases = _aliases.value.map { "${it.key}|=|${it.value}" }.toSet()
            val cats = _categories.value.map { "${it.key}|=|${it.value}" }.toSet()
            val allCats = _availableCategories.value.joinToString("|;|") { it.toCsvString() }
            val words = _wordFreq.value.entries.map { "${it.key}|w|${it.value}" }.toSet()
            p.edit().apply {
                putStringSet("aliases", aliases)
                putStringSet("categories", cats)
                putString("all_categories", allCats)
                putStringSet("word_freq", words)
                // word \u0001 sender \u0001 count —— 用控制字元當分隔，名字裡不可能出現
                putStringSet("word_senders", _wordSenders.value.flatMap { (w, senders) ->
                    senders.map { (n, c) -> w + "\u0001" + n + "\u0001" + c }
                }.toSet())
                putStringSet("blocked_names", _blockedNames.value)
                putString("section_order", _sectionOrder.value.joinToString(","))
                putStringSet("hidden_sections", _hiddenSections.value)
                putStringSet("deleted_names", _deletedNames.value.map { it.key + "|=|" + it.value }.toSet())
            }.apply()
        }
    }

    // ── 群組判定 ──────────────────────────────────────────
    /** 使用者手動指定：名稱 → true 群組／false 個人。比自動判斷優先。 */
    val groupOverrides = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    /**
     * 曾經被判定為群組的名稱。
     * ⚠️ 同一個 LINE 群組有時帶群組訊號、有時沒有（例如停留時間紀錄一律不帶），
     *    只看單筆紀錄會讓同一個群組一半算群組、一半算個人。只要有一次是群組，整個對話都算群組。
     */
    private val knownGroups = java.util.Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap<String, Boolean>())

    fun isGroupName(unified: String): Boolean = groupOverrides.value[unified] ?: (unified in knownGroups)

    private fun resolveGroup(r: InteractionRecord): InteractionRecord {
        val unified = _aliases.value[r.rawSenderName] ?: r.rawSenderName
        val o = groupOverrides.value[unified]
        val g = o ?: (r.isGroup || unified in knownGroups)
        if (g && o == null) knownGroups.add(unified)
        return if (g == r.isGroup) r else r.copy(isGroup = g)
    }

    private fun loadGroupState() {
        val p = prefs ?: return
        groupOverrides.value = (p.getStringSet("group_overrides", emptySet()) ?: emptySet()).mapNotNull { e ->
            val parts = e.split("|=|")
            if (parts.size == 2) parts[0] to (parts[1] == "1") else null
        }.toMap()
        val aliases = _aliases.value
        _interactions.value.forEach { if (it.isGroup) knownGroups.add(aliases[it.rawSenderName] ?: it.rawSenderName) }
        applyGroupState()
    }

    /** 把群組判定套回所有既有紀錄，讓統計、篩選、群組 vs 個人卡片看到的是同一個答案 */
    private fun applyGroupState() {
        val aliases = _aliases.value
        val ov = groupOverrides.value
        var changed = false
        val next = _interactions.value.map { r ->
            val u = aliases[r.rawSenderName] ?: r.rawSenderName
            val g = ov[u] ?: (r.isGroup || u in knownGroups)
            if (g != r.isGroup) { changed = true; r.copy(isGroup = g) } else r
        }
        if (changed) {
            _interactions.value = next
            saveData()
        }
    }

    /** 使用者在聯絡人選單手動改：true 群組、false 個人 */
    fun setGroupOverride(unified: String, isGroup: Boolean) {
        groupOverrides.update { it + (unified to isGroup) }
        if (isGroup) knownGroups.add(unified) else knownGroups.remove(unified)
        prefs?.edit()?.putStringSet(
            "group_overrides",
            groupOverrides.value.map { it.key + "|=|" + (if (it.value) "1" else "0") }.toSet()
        )?.apply()
        applyGroupState()
    }

    fun addInteraction(incoming: InteractionRecord) {
        // 服務可能先於 Activity 啟動，prefs 未就緒時不能靜靜丟資料
        if (prefs == null) return
        val record = resolveGroup(incoming)

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

    // ── 紀錄垃圾桶 ──────────────────────────────────────────
    // snapshotForUndo 只有「上一步」一份。刪錯東西常常過幾天才發現，
    // 所以刪掉的紀錄再另存一份，保留 7 天，可個別還原。
    private const val TRASH_PREFIX = "RelateStats_Trash_"
    private const val TRASH_KEEP_MS = 7L * 86_400_000L

    data class TrashBatch(val file: String, val deletedAt: Long, val label: String, val count: Int)

    private fun trashDir(): File? = appContext?.filesDir

    /** 刪除前呼叫：把即將消失的紀錄存進垃圾桶 */
    fun moveToTrash(label: String, records: List<InteractionRecord>) {
        if (records.isEmpty()) return
        val dir = trashDir() ?: return
        runCatching {
            val now = System.currentTimeMillis()
            // 檔名自帶時間與標籤，列表不用另外存索引
            val safe = label.replace(Regex("[^\\u4e00-\\u9fa5A-Za-z0-9]"), "")
            File(dir, TRASH_PREFIX + now + "_" + safe + ".txt")
                .writeText(records.joinToString("\n") { it.toCsvString() })
        }
    }

    fun listTrash(): List<TrashBatch> {
        val dir = trashDir() ?: return emptyList()
        return runCatching {
            (dir.listFiles { f -> f.name.startsWith(TRASH_PREFIX) } ?: emptyArray()).mapNotNull { f ->
                val body = f.name.removePrefix(TRASH_PREFIX).removeSuffix(".txt")
                val ts = body.substringBefore('_').toLongOrNull() ?: return@mapNotNull null
                val label = body.substringAfter('_', "").ifBlank { "刪除紀錄" }
                TrashBatch(f.name, ts, label, f.readLines().count { it.isNotBlank() })
            }.sortedByDescending { it.deletedAt }
        }.getOrDefault(emptyList())
    }

    /** @return 還原了幾筆 */
    fun restoreTrash(fileName: String): Int {
        val dir = trashDir() ?: return 0
        return runCatching {
            val f = File(dir, fileName)
            if (!f.exists()) return 0
            val recs = f.readLines().mapNotNull { InteractionRecord.fromCsvString(it) }
            // 用 id 去重，重複按還原不會變成兩份
            val existing = _interactions.value.mapTo(HashSet()) { it.id }
            val fresh = recs.filter { it.id !in existing }
            if (fresh.isNotEmpty()) {
                _interactions.update { (it + fresh).sortedBy { r -> r.timestamp } }
                saveData(immediate = true)
            }
            f.delete()
            fresh.size
        }.getOrDefault(0)
    }

    /** 某一批垃圾桶裡有哪些對象、各幾筆、時間範圍 —— 不還原也要看得到內容 */
    fun peekTrash(fileName: String): List<Triple<String, Int, String>> {
        val dir = trashDir() ?: return emptyList()
        return runCatching {
            val f = File(dir, fileName)
            if (!f.exists()) return emptyList()
            val recs = f.readLines().mapNotNull { InteractionRecord.fromCsvString(it) }
            val fmt = SimpleDateFormat("M/d", Locale.US)
            recs.groupBy { _aliases.value[it.rawSenderName] ?: it.rawSenderName }
                .map { (name, list) ->
                    val from = list.minOf { it.timestamp }
                    val to = list.maxOf { it.timestamp }
                    val span = if (fmt.format(Date(from)) == fmt.format(Date(to))) fmt.format(Date(from))
                    else fmt.format(Date(from)) + "-" + fmt.format(Date(to))
                    Triple(name, list.size, span)
                }
                .sortedByDescending { it.second }
        }.getOrDefault(emptyList())
    }

    /**
     * 只還原某一批裡的特定對象，其餘留在垃圾桶。
     *
     * ⚠️ 還原完要把檔案「重寫成剩下的」而不是整個刪掉，
     *    否則還原一個人等於順手永久刪掉同一批的其他人。
     */
    fun restoreTrashNames(fileName: String, names: Set<String>): Int {
        val dir = trashDir() ?: return 0
        return runCatching {
            val f = File(dir, fileName)
            if (!f.exists()) return 0
            val recs = f.readLines().mapNotNull { InteractionRecord.fromCsvString(it) }
            val (take, keep) = recs.partition { (_aliases.value[it.rawSenderName] ?: it.rawSenderName) in names }
            if (take.isEmpty()) return 0
            val existing = _interactions.value.mapTo(HashSet()) { it.id }
            val fresh = take.filter { it.id !in existing }
            if (fresh.isNotEmpty()) {
                _interactions.update { (it + fresh).sortedBy { r -> r.timestamp } }
                saveData(immediate = true)
            }
            if (keep.isEmpty()) f.delete() else f.writeText(keep.joinToString("\n") { it.toCsvString() })
            fresh.size
        }.getOrDefault(0)
    }

    fun purgeTrash(fileName: String) {
        val dir = trashDir() ?: return
        runCatching { File(dir, fileName).delete() }
    }

    /** 每次啟動清掉超過 7 天的批次 */
    fun purgeExpiredTrash() {
        val dir = trashDir() ?: return
        val cutoff = System.currentTimeMillis() - TRASH_KEEP_MS
        runCatching {
            listTrash().filter { it.deletedAt < cutoff }.forEach { File(dir, it.file).delete() }
        }
    }

    /** 批次新增：匯入時用，避免每筆都觸發一次存檔與分類建立 */
    fun addInteractions(incoming: List<InteractionRecord>) {
        if (incoming.isEmpty() || prefs == null) return
        val records = incoming.map { resolveGroup(it) }
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

    /**
     * 每個常用詞是誰傳的。
     *
     * ⚠️ 只記「前幾位發話者與次數」，不記訊息內容 —— 那個前提不能破。
     * ⚠️ 兩層上限是必要的：詞最多 400 個、每個詞最多 6 位發話者。
     *    不設上限的話這份資料會變成「詞數 × 聯絡人數」，比詞頻本身大一個量級。
     * ⚠️ 這份資料只能從現在開始累積 —— 過去的訊息早就沒留內容了，回不去。
     */
    private const val MAX_WORD_SENDER_WORDS = 400
    private const val MAX_SENDERS_PER_WORD = 6

    private val _wordSenders = MutableStateFlow<Map<String, Map<String, Int>>>(emptyMap())
    val wordSenders: StateFlow<Map<String, Map<String, Int>>> = _wordSenders

    private fun recordWordSenders(words: List<String>, senderName: String) {
        _wordSenders.update { cur ->
            val m = HashMap(cur)
            words.distinct().forEach { w ->
                val inner = HashMap(m[w] ?: emptyMap())
                inner[senderName] = (inner[senderName] ?: 0) + 1
                m[w] = if (inner.size > MAX_SENDERS_PER_WORD)
                    inner.entries.sortedByDescending { it.value }.take(MAX_SENDERS_PER_WORD)
                        .associate { it.key to it.value }
                else inner
            }
            if (m.size > MAX_WORD_SENDER_WORDS)
                m.entries.sortedByDescending { e -> e.value.values.sum() }
                    .take(MAX_WORD_SENDER_WORDS).associate { it.key to it.value }
            else m
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
        if (!senderName.isNullOrBlank()) recordWordSenders(clean, senderName)
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
    /** 所有聯絡人名字的 2~4 字子字串，用來認出被切碎的人名 */
    private fun nameFragments(): HashSet<String> {
        val out = HashSet<String>()
        val names = (_interactions.value.map { _aliases.value[it.rawSenderName] ?: it.rawSenderName } +
            _aliases.value.keys + _aliases.value.values).toHashSet()
        names.forEach { raw ->
            val nm = raw.trim()
            if (nm.length in 2..24) {
                for (n in 2..4) for (i in 0..nm.length - n) out.add(nm.substring(i, i + n))
            }
        }
        return out
    }

    /**
     * 這次會被清掉哪些詞 —— 按出現次數由多到少。
     *
     * ⚠️ 一定要能先看再刪。這個動作一次會清掉幾百個詞而且無法復原，
     *    只丟一句「已清掉 N 個」使用者根本不知道自己失去了什麼。
     */
    fun previewJunkWords(): List<Pair<String, Int>> {
        val cur = _wordFreq.value
        if (cur.isEmpty()) return emptyList()
        val frags = nameFragments()
        return cur.filterKeys { isJunkWord(it) || it in frags }
            .toList().sortedByDescending { it.second }
    }

    /** 只清掉指定的詞。⚠️ 使用者一定要能挑，整批清掉是不可復原的。 */
    fun purgeWords(words: Set<String>): Int {
        if (words.isEmpty()) return 0
        val cur = _wordFreq.value
        val kept = cur.filterKeys { it !in words }
        val removed = cur.size - kept.size
        if (removed > 0) {
            _wordFreq.value = kept
            saveData(immediate = true)
        }
        return removed
    }

    fun purgeJunkWords(): Int {
        val cur = _wordFreq.value
        if (cur.isEmpty()) return 0
        val frags = nameFragments()
        val kept = cur.filterKeys { !isJunkWord(it) && it !in frags }
        val removed = cur.size - kept.size
        if (removed > 0) {
            _wordFreq.value = kept
            saveData(immediate = true)
        }
        return removed
    }

    fun clearWordFreq() {
        _wordFreq.value = emptyMap()
        _wordSenders.value = emptyMap()
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

    fun deleteContactsWithTrash(names: Set<String>): Int {
        val doomed = _interactions.value.filter { (_aliases.value[it.rawSenderName] ?: it.rawSenderName) in names }
        moveToTrash("刪除聯絡人", doomed)
        return deleteContacts(names)
    }

    // ── 刪除過的對象 ────────────────────────────────────────
    // 刪掉之後對方又傳訊息，紀錄就會重建、聯絡人又冒出來，
    // 使用者得一直重刪。所以記住刪過誰、什麼時候刪的，
    // 之後偵測到又有新紀錄就主動問一次。
    private val _deletedNames = MutableStateFlow<Map<String, Long>>(emptyMap())
    val deletedNames: StateFlow<Map<String, Long>> = _deletedNames

    /** 永久忽略名單：通知服務直接不收，不會再產生任何紀錄 */
    // 洞察區塊的顯示順序與隱藏清單。只存 id，新版新增的區塊會自動接在後面。
    private val _sectionOrder = MutableStateFlow<List<String>>(emptyList())
    val sectionOrder: StateFlow<List<String>> = _sectionOrder
    private val _hiddenSections = MutableStateFlow<Set<String>>(emptySet())
    val hiddenSections: StateFlow<Set<String>> = _hiddenSections

    fun setSectionOrder(ids: List<String>) {
        _sectionOrder.value = ids
        saveData(immediate = true)
    }

    fun toggleSectionHidden(id: String) {
        _hiddenSections.update { if (id in it) it - id else it + id }
        saveData(immediate = true)
    }

    fun resetSections() {
        _sectionOrder.value = emptyList()
        _hiddenSections.value = emptySet()
        saveData(immediate = true)
    }

    /**
     * 「這些不是垃圾，別再問我」的名單。
     *
     * ⚠️ 沒有這個的話使用者會被卡死：規則判定某個名字是非真人，
     *    但它其實是真人（群組名撞到關鍵字就會發生）——
     *    使用者既不想標成系統通知、也不想刪，於是橫幅每次開 App 都跳出來。
     *    這是第三個出口：記住它是真人，不再列入提醒。
     */
    private val _junkIgnored = MutableStateFlow<Set<String>>(emptySet())
    val junkIgnored: StateFlow<Set<String>> = _junkIgnored

    fun ignoreJunk(names: Collection<String>) {
        if (names.isEmpty()) return
        _junkIgnored.update { it + names }
        prefs?.edit()?.putStringSet("junk_ignored", _junkIgnored.value)?.apply()
    }

    fun unignoreJunk(name: String) {
        _junkIgnored.update { it - name }
        prefs?.edit()?.putStringSet("junk_ignored", _junkIgnored.value)?.apply()
    }

    private val _blockedNames = MutableStateFlow<Set<String>>(emptySet())
    val blockedNames: StateFlow<Set<String>> = _blockedNames

    fun isBlocked(name: String): Boolean = name.trim() in _blockedNames.value

    fun blockName(name: String) {
        _blockedNames.update { it + name }
        _deletedNames.update { it - name }
        saveData(immediate = true)
    }

    /** 使用者說「這個人要留著」：從刪除名單移除，之後不再詢問 */
    fun forgetDeleted(name: String) {
        _deletedNames.update { it - name }
        _blockedNames.update { it - name }
        saveData(immediate = true)
    }

    fun unblockName(name: String) {
        _blockedNames.update { it - name }
        saveData(immediate = true)
    }

    /** 刪除過、而且刪除之後又有新紀錄的對象 */
    fun resurrectedNames(): List<Pair<String, Int>> {
        val del = _deletedNames.value
        if (del.isEmpty()) return emptyList()
        val counts = HashMap<String, Int>()
        _interactions.value.forEach { r ->
            val nm = _aliases.value[r.rawSenderName] ?: r.rawSenderName
            val since = del[nm] ?: return@forEach
            if (r.timestamp > since) counts[nm] = (counts[nm] ?: 0) + 1
        }
        return counts.entries.sortedByDescending { it.value }.map { it.key to it.value }
    }

    fun deleteContacts(names: Set<String>): Int {
        if (names.isEmpty()) return 0
        snapshotForUndo("刪除 ${names.size} 位聯絡人")
        val now = System.currentTimeMillis()
        _deletedNames.update { m -> m + names.associateWith { now } }
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
    /**
     * 還沒處理的非真人來源。
     *
     * ⚠️ 條件是「是非真人」**而且**「還沒被標成系統通知」。
     *    舊版寫成 `isNonPersonContact(it) || categories[it] == SYSTEM`：
     *    用 or 又把「已標為系統通知」的算進來，等於標記完數字一個都不會少，
     *    橫幅永遠關不掉 —— 使用者按了「標為系統通知」看起來完全沒反應。
     *    定義跟 previewRetagTargets 保持一致。
     */
    fun listJunkContacts(): List<String> =
        _interactions.value.map { _aliases.value[it.rawSenderName] ?: it.rawSenderName }
            .distinct()
            .filter {
                isNonPersonContact(it) &&
                    _categories.value[it] != ContactCategory.SYSTEM.id &&
                    it !in _junkIgnored.value
            }

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

    /**
     * 取消綁定：把某個原始帳號從合併裡拆出來，變回獨立的聯絡人，它的紀錄跟著它走。
     * ⚠️ 只刪別名，不動紀錄 —— 綁錯了要能無損拆回來。
     */
    fun unmergeAccount(rawName: String) {
        if (!_aliases.value.containsKey(rawName)) return
        snapshotForUndo("取消綁定帳號")
        _aliases.update { it - rawName }
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

    /**
     * 依「範圍 + 要保留的對象」清除紀錄。
     * 使用者常常只想清掉某幾個對象在某段時間的資料，全刪太粗暴。
     * @param keep 這些歸戶名稱的紀錄不動
     * @return 實際刪掉幾筆
     */
    fun clearInteractionsInRange(start: Long, end: Long, keep: Set<String>): Int {
        snapshotForUndo("清除區間資料")
        val before = _interactions.value.size
        val doomed = _interactions.value.filter { r ->
            val nm = _aliases.value[r.rawSenderName] ?: r.rawSenderName
            r.timestamp in start..end && nm !in keep
        }
        moveToTrash("清除區間", doomed)
        _interactions.update { list ->
            list.filterNot { r ->
                val nm = _aliases.value[r.rawSenderName] ?: r.rawSenderName
                r.timestamp in start..end && nm !in keep
            }
        }
        val removed = before - _interactions.value.size
        if (removed > 0) saveData(immediate = true)
        return removed
    }

    /** 預覽某個區間內、每位對象各有幾筆紀錄 */
    fun previewRange(start: Long, end: Long): List<Pair<String, Int>> =
        _interactions.value.filter { it.timestamp in start..end }
            .groupingBy { _aliases.value[it.rawSenderName] ?: it.rawSenderName }
            .eachCount().entries.sortedByDescending { it.value }.map { it.key to it.value }

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

    // ── 雲端備份 ──────────────────────────────────────────
    /**
     * 綁定 Google 雲端硬碟備份。
     *
     * ⚠️ 刻意不走「Google 登入＋Drive API」：那要加 INTERNET 權限、串 OAuth，
     *    「App 沒有網路權限、資料傳不出去」這個最重要的隱私保證就沒了，
     *    上架的資料安全性表單也會多出一堆要申報的項目。
     *    改用系統檔案選擇器（SAF）：使用者在選擇器裡挑「雲端硬碟」建立一個檔案，
     *    App 拿到那個檔案的長期寫入權限，之後每晚覆寫它，上傳由雲端硬碟 App 自己處理。
     * ⚠️ 雲端硬碟的上傳是非同步的，手機離線時會等連上網才傳。
     */
    val cloudBackupUri = MutableStateFlow<String?>(null)
    val cloudBackupLast = MutableStateFlow(0L)
    /** 自動備份週期（天）。0 = 不自動，只在按「立即備份」時更新 */
    val cloudBackupInterval = MutableStateFlow(1)
    /** 給人看的位置，例如「Google 雲端硬碟 › RelateStats_雲端備份.txt」 */
    val cloudBackupLocation = MutableStateFlow<String?>(null)

    private fun loadCloudBackupState() {
        val p = prefs ?: return
        cloudBackupUri.value = p.getString("cloud_backup_uri", null)
        cloudBackupLast.value = p.getLong("cloud_backup_last", 0L)
        cloudBackupInterval.value = p.getInt("cloud_backup_interval", 1)
        cloudBackupLocation.value = p.getString("cloud_backup_label", null)
    }

    fun setCloudBackupInterval(days: Int) {
        cloudBackupInterval.value = days
        prefs?.edit()?.putInt("cloud_backup_interval", days)?.apply()
    }

    /**
     * 每晚鬧鐘呼叫：距離上次備份滿週期才寫。
     * ⚠️ 容許 3 小時誤差 —— 鬧鐘給了一小時的窗，每天觸發時間不固定，
     *    嚴格比「滿 24 小時」會常常差幾分鐘沒到，變成隔一天才備份。
     */
    fun cloudBackupIfDue(context: Context): Boolean {
        val days = cloudBackupInterval.value
        if (cloudBackupUri.value == null || days <= 0) return false
        if (System.currentTimeMillis() - cloudBackupLast.value < days * 86_400_000L - 3 * 3_600_000L) return false
        return writeCloudBackup(context)
    }

    /**
     * 把 SAF 的 content:// 網址翻成人看得懂的位置。
     * ⚠️ SAF 拿不到資料夾路徑，只拿得到「哪個提供者」＋「檔名」，所以只顯示這兩段。
     * ⚠️ 綁定當下算好存進偏好設定，不要每次開設定頁都去查 —— 查雲端硬碟可能要等網路。
     */
    private fun describeCloudLocation(ctx: Context, uri: Uri): String {
        val auth = uri.authority.orEmpty()
        val app = when {
            auth.startsWith("com.google.android.apps.docs") -> "Google 雲端硬碟"
            auth == "com.android.externalstorage.documents" -> "手機儲存空間"
            auth == "com.android.providers.downloads.documents" -> "手機「下載」資料夾"
            auth == "com.android.providers.media.documents" -> "手機媒體資料夾"
            else -> runCatching {
                ctx.packageManager.resolveContentProvider(auth, 0)?.loadLabel(ctx.packageManager)?.toString()
            }.getOrNull()
        }
        val name = runCatching {
            ctx.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        }.getOrNull()
        return listOfNotNull(app?.takeIf { it.isNotBlank() }, name?.takeIf { it.isNotBlank() })
            .joinToString(" › ").ifEmpty { "已選擇的位置" }
    }

    private val uriRw = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

    /** @return 0 成功且之後會自動更新；1 這次寫入成功，但該位置不給長期權限（無法自動）；-1 寫入失敗 */
    fun bindCloudBackup(context: Context, uri: Uri): Int {
        val ctx = context.applicationContext
        val persisted = runCatching { ctx.contentResolver.takePersistableUriPermission(uri, uriRw) }.isSuccess
        if (!writeBackupTo(ctx, uri)) return -1
        if (!persisted) return 1
        // 換位置時放掉舊檔案的權限 —— 系統對每個 App 能保留的長期權限數量有上限
        cloudBackupUri.value?.takeIf { it != uri.toString() }?.let { old ->
            runCatching { ctx.contentResolver.releasePersistableUriPermission(Uri.parse(old), uriRw) }
        }
        val label = describeCloudLocation(ctx, uri)
        cloudBackupUri.value = uri.toString()
        cloudBackupLocation.value = label
        prefs?.edit()?.putString("cloud_backup_uri", uri.toString())?.putString("cloud_backup_label", label)?.apply()
        return 0
    }

    fun unbindCloudBackup(context: Context) {
        cloudBackupUri.value?.let { old ->
            runCatching { context.applicationContext.contentResolver.releasePersistableUriPermission(Uri.parse(old), uriRw) }
        }
        cloudBackupUri.value = null
        cloudBackupLocation.value = null
        prefs?.edit()?.remove("cloud_backup_uri")?.remove("cloud_backup_label")?.apply()
    }

    /** 覆寫已綁定的雲端備份檔。沒綁定或寫入失敗（檔案被移動、刪除）回 false。 */
    fun writeCloudBackup(context: Context): Boolean {
        val u = cloudBackupUri.value ?: return false
        return writeBackupTo(context.applicationContext, Uri.parse(u))
    }

    private fun writeBackupTo(ctx: Context, uri: Uri): Boolean {
        val payload = runCatching { exportBackupString("").toByteArray() }.getOrNull() ?: return false
        // ⚠️ 先用 "wt"（先清空再寫）。部分文件提供者不認 "wt"，才退回 "w"。
        //    "w" 在某些提供者不會截斷，新檔比舊檔短時尾巴會殘留舊內容，所以只當備援。
        val ok = listOf("wt", "w").any { mode ->
            runCatching {
                ctx.contentResolver.openOutputStream(uri, mode)?.use { it.write(payload); it.flush() } != null
            }.getOrDefault(false)
        }
        if (ok) {
            val now = System.currentTimeMillis()
            cloudBackupLast.value = now
            prefs?.edit()?.putLong("cloud_backup_last", now)?.apply()
        }
        return ok
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
/**
 * 每晚回顧＋服務異常提醒。
 *
 * ⚠️ 用 AlarmManager 而不是 WorkManager：這個 App 沒有其他背景工作，
 *    為了一天一次的鬧鐘多拉一個函式庫不划算。
 * ⚠️ 必須用 setExactAndAllowWhileIdle 之外的方式（setWindow），
 *    因為精確鬧鐘在 Android 12+ 要另外的權限，而「晚上九點左右」本來就不需要精確。
 * ⚠️ 每次觸發後要自己排下一次 —— setWindow 是一次性的。
 */
object DailyRecap {
    const val CHANNEL_RECAP = "relate_recap"
    const val CHANNEL_ALERT = "relate_alert"
    private const val REQ = 8801
    /** 晚上 9 點 */
    private const val HOUR = 21

    fun ensureChannels(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_RECAP, "每日回顧", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "每天晚上整理今天的互動" }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERT, "服務提醒", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "偵測服務被系統關掉時通知你" }
        )
    }

    private fun pending(ctx: Context): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, REQ, Intent(ctx, RecapReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    fun schedule(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val c = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, HOUR); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DATE, 1)
        }
        runCatching {
            // 給一小時的窗，系統可以挑省電的時機發，不需要分秒不差
            am.setWindow(AlarmManager.RTC_WAKEUP, c.timeInMillis, 3_600_000L, pending(ctx))
        }
    }

    fun cancel(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        runCatching { am.cancel(pending(ctx)) }
    }

    /** 回顧用的數字：今天與昨天並排，才能比 */
    data class RecapStat(
        val msgs: Int, val people: Int, val dur: Long,
        val yMsgs: Int, val yPeople: Int, val yDur: Long,
        /** 聊最多的三個人：名字、筆數、主要在哪個 App */
        val top: List<Triple<String, Int, String>>,
        /** 今天每小時的互動筆數（24 格） */
        val hourly: List<Int>,
        /** 依標籤分的互動對象人數，由多到少 */
        val catShare: List<Pair<ContactCategory, Int>>
    )

    /** 今天沒有真人互動就回 null —— 沒事就不要吵人。 */
    fun buildRecap(now: Long = System.currentTimeMillis()): RecapStat? {
        val today = startOfDayMs(now)
        // ⚠️ 看過去某一天時一定要有結束時間，不然「那天」會把之後每一天都算進來
        val dayEnd = today + 86_400_000L
        val yStart = today - 86_400_000L
        val cats = DataRepository.categories.value
        val aliases = DataRepository.aliases.value
        fun nameOf(r: InteractionRecord) = aliases[r.rawSenderName] ?: r.rawSenderName
        val all = DataRepository.interactions.value
        val t = all.filter { it.timestamp in today until dayEnd && !isExcludedFromStats(nameOf(it), cats) }
        if (t.isEmpty()) return null
        val y = all.filter { it.timestamp in yStart until today && !isExcludedFromStats(nameOf(it), cats) }

        val cal = Calendar.getInstance()
        val hourly = IntArray(24)
        t.forEach { cal.timeInMillis = it.timestamp; hourly[cal.get(Calendar.HOUR_OF_DAY)]++ }

        val catList = DataRepository.availableCategories.value
        val people = t.mapTo(HashSet()) { nameOf(it) }
        val catShare = people
            .groupingBy { n -> catList.firstOrNull { it.id == cats[n] } ?: ContactCategory.OTHER }
            .eachCount().entries.sortedByDescending { it.value }.map { it.key to it.value }

        val byName = t.groupBy { nameOf(it) }
        return RecapStat(
            msgs = t.count { it.isIncoming && !it.isCall },
            people = people.size,
            dur = t.sumOf { it.durationSeconds.toLong() },
            yMsgs = y.count { it.isIncoming && !it.isCall },
            yPeople = y.mapTo(HashSet()) { nameOf(it) }.size,
            yDur = y.sumOf { it.durationSeconds.toLong() },
            top = byName.entries.sortedByDescending { it.value.size }.take(3).map { (n, rs) ->
                Triple(n, rs.size, rs.groupingBy { it.appName }.eachCount().maxByOrNull { it.value }?.key ?: "")
            },
            hourly = hourly.toList(),
            catShare = catShare
        )
    }

    /**
     * ⚠️ 通知只負責「叫你點進來」，不要把數字塞進通知裡。
     *    以前把「189 則 · 54 位 · 停留 22m 23s」塞在通知內文，
     *    不同手機的通知寬度不一樣，全形空白一換行就整個歪掉，也看不出跟昨天比如何。
     *    數字留給 App 裡的概要彈窗，那裡排版是我們自己控制的。
     */
    fun notifyRecap(ctx: Context) {
        if (buildRecap() == null) return
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        ensureChannels(ctx)
        val open = PendingIntent.getActivity(
            ctx, 0,
            Intent(ctx, MainActivity::class.java)
                .putExtra(EXTRA_OPEN_RECAP, true)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = Notification.Builder(ctx, CHANNEL_RECAP)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle("今日互動報告")
            .setContentText("點開看今天聊了誰，跟昨天比起來如何")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { nm.notify(9001, n) }
    }

    const val EXTRA_OPEN_RECAP = "open_recap"

    /**
     * 無障礙服務被關掉時提醒。
     * ⚠️ Android 每次重新安裝 App 都會自動停用它，使用者通常不會發現 ——
     *    等到發現「停留時間都是 0」已經漏掉好幾天了。
     */
    fun notifyServiceOff(ctx: Context) {
        if (isAccessibilityServiceEnabled(ctx, ChatTrackerService::class.java)) {
            runCatching {
                (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.cancel(9002)
            }
            return
        }
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        ensureChannels(ctx)
        val open = PendingIntent.getActivity(
            ctx, 1, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = Notification.Builder(ctx, CHANNEL_ALERT)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("停留時間沒有在記錄")
            .setContentText("無障礙服務被關掉了，點這裡重新開啟")
            .setStyle(Notification.BigTextStyle().bigText(
                "「停留時間偵測」目前是關閉的，這段期間不會累積任何停留時間。\n" +
                    "Android 在每次重新安裝 App 後都會自動停用它。"
            ))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { nm.notify(9002, n) }
    }
}

/** 重開機後重新排程 —— AlarmManager 的錳鐘不會跨重啟保留 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) DailyRecap.schedule(context)
    }
}

class RecapReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        runCatching {
            DataRepository.init(context.applicationContext)
            if (DataRepository.recapEnabled.value) DailyRecap.notifyRecap(context)
            DailyRecap.notifyServiceOff(context)
        }
        // 每晚順便更新雲端備份。備份檔可能有好幾 MB，不能在主執行緒寫。
        if (DataRepository.cloudBackupUri.value != null) {
            val pr = goAsync()
            Thread {
                runCatching { DataRepository.cloudBackupIfDue(context) }
                pr.finish()
            }.start()
        }
        // setWindow 是一次性的，排下一次
        DailyRecap.schedule(context)
    }
}

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

    /**
     * 進行中的通話：通知 key → (對象, App, 開始時間)。
     *
     * LINE、Messenger 這類 App 通話時會掛一則「通話中」的常駐通知，
     * 通話結束那則通知就會被移除。所以「post 到 remove」的時間差就是通話長度 ——
     * 這是不讀取任何內容、也不需要額外權限就能拿到秒數的方法。
     * ⚠️ 用 putIfAbsent：通話中通知每秒都會重新 post（上面的計時在跳），只能記第一次。
     */
    private data class CallStart(val name: String, val app: String, val start: Long, val dir: Int)
    private val ongoingCalls = java.util.concurrent.ConcurrentHashMap<String, CallStart>()
    /**
     * 最近看到「來電鈴響中」通知的對象 → 時間。
     * 對方打來時會先跳鈴響通知，接著才變成「通話中」；我自己撥出去則只有「通話中」。
     * 這是不讀通話紀錄、也沒有權限可問的情況下，唯一分得出方向的線索。
     */
    private val ringingCalls = java.util.concurrent.ConcurrentHashMap<String, Long>()
    /** 對象 → 最近一次靠「通知移除」算出通話的時間，用來避免跟「通話結束」通知重複計算 */
    private val recentEndedCalls = java.util.concurrent.ConcurrentHashMap<String, Long>()

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

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        try {
            val start = ongoingCalls.remove(sbn.key) ?: return
            val secs = ((System.currentTimeMillis() - start.start) / 1000L).toInt()
            // 3 秒以下多半是沒接通就掛掉；超過 6 小時一定是通知卡住沒被移除
            if (secs < 3 || secs > 6 * 3600) return
            recentEndedCalls[start.name] = System.currentTimeMillis()
            ringingCalls.remove(start.name)
            DataRepository.addInteraction(
                InteractionRecord(
                    rawSenderName = start.name,
                    appName = start.app,
                    isIncoming = start.dir != 2,
                    wordCount = 0,
                    durationSeconds = secs,
                    isGroup = false,
                    isMissedCall = false,
                    isCall = true,
                    callDirection = start.dir
                )
            )
        } catch (_: Exception) {
            // 拋例外會被系統停用服務
        }
    }

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
        if (flags and Notification.FLAG_ONGOING_EVENT != 0) {
            // 常駐通知不是訊息，但「通話中」那則要記下開始時間。
            // ⚠️ 手機原生電話也要走這裡：撥號 App 接通後不會發一般通知，
            //    只靠通知偵測的話，「我打出去的電話」永遠不會被記錄，
            //    使用者看到的就只有別人打給他的未接來電。
            val ex = n.extras
            val title = ex?.getString("android.title")?.trim().orEmpty()
            val body = ex?.getCharSequence("android.text")?.toString().orEmpty()
            val callWords = listOf("通話中", "語音通話", "視訊通話", "正在通話", "Ongoing call", "voice call", "video call")
            val isCallNotif = n.category == Notification.CATEGORY_CALL ||
                callWords.any { title.contains(it, ignoreCase = true) || body.contains(it, ignoreCase = true) }
            if (isCallNotif) {
                val name = stripCallSuffix(title)
                    .replace(Regex("(語音通話|視訊通話|通話中|正在通話|LINE|Messenger)"), "")
                    .trim().trim('-', '·', '•', ' ')
                if (name.isNotBlank() && !isNonPersonContact(name)) {
                    val blob = "$title $body"
                    // ⚠️ 撥號 App 從「撥號中」就掛著常駐通知，要等真的接通才開始計時，
                    //    否則響很久沒人接也會被記成一通。接通的證據＝通知上開始跳 0:07 這種計時。
                    if (appName == "電話" &&
                        !Regex("\\d{1,2}:\\d{2}").containsMatchIn(blob) &&
                        !blob.contains("通話中")
                    ) {
                        if (listOf("來電", "Incoming call").any { blob.contains(it, ignoreCase = true) }) {
                            ringingCalls[name] = System.currentTimeMillis()
                        }
                        return
                    }
                    // ⚠️ 鈴響中的通知不是通話本身，只拿來記「這通是對方打來的」——
                    //    接通後會換成「通話中」通知，那時才開始計時。
                    val ringing = appName != "電話" &&
                        listOf("來電", "通話邀請", "邀請你", "正在響鈴", "Incoming call", "is calling")
                            .any { blob.contains(it, ignoreCase = true) }
                    if (ringing) {
                        ringingCalls[name] = System.currentTimeMillis()
                    } else {
                        val ring = ringingCalls[name]
                        val dir = if (ring != null && System.currentTimeMillis() - ring < 120_000L) 1 else 2
                        ongoingCalls.putIfAbsent(sbn.key, CallStart(name, appName, System.currentTimeMillis(), dir))
                    }
                }
            }
            return
        }

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
            // 同一通電話剛剛已經用「通話中通知被移除」算過秒數了，不要再記一次
            if (!callInfo.first && appName != "電話") {
                val ended = recentEndedCalls[cleanName]
                if (ended != null && System.currentTimeMillis() - ended < 120_000L) return
            }
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
                    isCall = true,
                    // 未接一定是對方打來；已接的結束通知看不出方向，除非剛剛有鈴響過
                    callDirection = if (callInfo.first) 1
                    else ringingCalls[cleanName]?.takeIf { System.currentTimeMillis() - it < 300_000L }?.let { 1 } ?: 0
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
        // ⚠️ LINE 的群組通知常常「沒有」帶官方群組旗標，只看旗標會把大部分 LINE 群組當成個人。
        //    LINE 群組的通知：對話名＝群組名、發言人＝群組裡的某個人，兩者不同；
        //    一對一則沒有對話名、或對話名就是對方名字。群組名後面帶「(人數)」也一定是群組。
        //    這個判斷只套在 LINE —— IG 一對一私訊也會帶對話名，不能沿用。
        fun lineGroup(conv: String?, speaker: String?): Boolean {
            if (appName != "LINE") return false
            val c = conv?.trim()?.takeIf { it.isNotBlank() } ?: return false
            if (Regex("[（(]\\s*\\d{1,4}\\s*[)）]\\s*$").containsMatchIn(c)) return true
            val sp = speaker?.trim()?.takeIf { it.isNotBlank() } ?: return false
            return !c.equals(sp, ignoreCase = true)
        }

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
                val grp = isGroupFlag || lineGroup(convName, speaker)
                // 群組才用對話名當歸戶對象；一對一一律用實際發話者
                val senderName = if (grp && convName != null) convName else (speaker ?: convName ?: baseTitle)
                val subSender = if (grp) speaker else null
                addMessage(appName, senderName, subSender, msg.text, grp, msg.time)
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
        var isGroup = isGroupFlag || lineGroup(convTitle, baseTitle)
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
        // 使用者說過「這個永遠不要記」——在最源頭擋掉，不產生任何紀錄
        if (DataRepository.isBlocked(name)) return
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
    val chartPaletteId = DataRepository.chartPaletteId
    fun setChartPalette(id: String) = DataRepository.setChartPalette(id)
    val recapEnabled = DataRepository.recapEnabled
    val pendingRecap = DataRepository.pendingRecap
    fun setRecapEnabled(on: Boolean) = DataRepository.setRecapEnabled(on)

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

    /**
     * ⚠️ 這個函式是分析頁效能的關鍵路徑。
     *
     * 舊版每次呼叫都 `Calendar.getInstance()` —— 那會做時區與地區設定查詢並配置物件。
     * 分析頁一次重算會經過 getStreaks、getContributionMap、extraStatsOf、
     * getDailyTrend、getCoverage 等好幾條全表掃描，每條都對五萬多筆各呼叫一次，
     * 等於一次產生數十萬個 Calendar，這就是「分析要跑很久才出來」。
     *
     * 改法有二：
     *  1. Calendar 放 ThreadLocal 重複使用，不再配置（背景執行緒各持一份，安全）
     *  2. 加一格「上一天」快取 —— 紀錄依時間排序，相鄰筆數大多同一天，
     *     絕大部分呼叫會在第一個 if 就回傳
     */
    private val dayCal = ThreadLocal.withInitial { Calendar.getInstance() }
    private val dayLo = ThreadLocal.withInitial { 0L }
    private val dayHi = ThreadLocal.withInitial { -1L }

    private fun startOfDay(ts: Long): Long {
        val lo = dayLo.get()!!
        if (ts >= lo && ts < dayHi.get()!!) return lo
        val c = dayCal.get()!!
        c.timeInMillis = ts
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        val start = c.timeInMillis
        c.add(Calendar.DATE, 1)
        dayLo.set(start)
        dayHi.set(c.timeInMillis)
        return start
    }

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
            FilterType.RANGE -> {
                val start = startOfDay(minOf(filter.timestamp, filter.endTimestamp))
                val end = startOfDay(maxOf(filter.timestamp, filter.endTimestamp)) + 86_399_999L
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
                incomingCount = catRecords.count { it.isIncoming },
                // 「這個標籤有幾個人」是使用者最常想知道的一件事，但以前完全沒算
                peopleCount = catRecords.mapTo(HashSet()) { currentAliases[it.rawSenderName] ?: it.rawSenderName }.size
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
        FilterType.RANGE -> startOfDay(minOf(filter.timestamp, filter.endTimestamp)) to
            (startOfDay(maxOf(filter.timestamp, filter.endTimestamp)) + 86_399_999L)
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

    /**
     * 「每個人的第一次／最後一次／活躍過哪些天」——三份跟日期篩選**完全無關**的索引。
     *
     * ⚠️ 這是效能的關鍵。新出現的人與連續互動各自掃過全表一次，
     *    切一次日期就是兩趟五萬筆。但這三份索引只跟「紀錄」和「別名」有關，
     *    換日期根本不會變，所以用引用比對（===）快取起來共用。
     *    新紀錄進來時 _interactions 會換成新的 List 物件，引用自然對不上，快取就失效重算——
     *    不需要另外維護任何 dirty flag。
     * ⚠️ 統計跑在背景執行緒，洞察頁與排行頁可能同時進來，所以要同步。
     */
    private class FullScan(
        val firstSeen: HashMap<String, Long>,
        val lastSeen: HashMap<String, Long>,
        val daysByPerson: HashMap<String, MutableSet<Long>>
    )

    private var scanRecords: List<InteractionRecord>? = null
    private var scanAlias: Map<String, String>? = null
    private var scanResult: FullScan? = null

    @Synchronized
    private fun fullScan(records: List<InteractionRecord>, aliasMap: Map<String, String>): FullScan {
        val cached = scanResult
        if (cached != null && records === scanRecords && aliasMap === scanAlias) return cached
        val firstSeen = HashMap<String, Long>()
        val lastSeen = HashMap<String, Long>()
        val days = HashMap<String, MutableSet<Long>>()
        records.forEach { r ->
            val name = aliasMap[r.rawSenderName] ?: r.rawSenderName
            val ts = r.timestamp
            val f = firstSeen[name]
            if (f == null || ts < f) firstSeen[name] = ts
            val l = lastSeen[name]
            if (l == null || ts > l) lastSeen[name] = ts
            days.getOrPut(name) { HashSet() }.add(startOfDay(ts))
        }
        val res = FullScan(firstSeen, lastSeen, days)
        scanRecords = records; scanAlias = aliasMap; scanResult = res
        return res
    }

    /** 本期新出現的人，以及已沉寂（超過 14 天沒互動）的人 */
    fun getChurn(records: List<InteractionRecord>, aliasMap: Map<String, String>, filter: DateFilter, weekStart: Int): ChurnStat {
        if (records.isEmpty()) return ChurnStat(emptyList(), emptyList())
        val range = rangeOf(filter, weekStart)
        // ⚠️ 基準是「區間結束日」不是今天。翻到上個月時，
        //    「沉寂了幾天」要以那個月底來算，否則所有人看起來都沉寂很久。
        val now = range?.second ?: System.currentTimeMillis()
        val scan = fullScan(records, aliasMap)
        // ⚠️ 這裡要用 isExcludedFromStats 而不是 isNonPersonContact。
        //    規則認不出的推播來源（銀行、官方帳號…）是靠使用者手動標成「系統通知」處理的，
        //    只查規則的話，標記完它們照樣會出現在「新出現的人」裡。
        val cats = categories.value

        val newcomers = if (range == null) emptyList() else
            scan.firstSeen.entries.asSequence()
                .filter { it.value in range.first..range.second && !isExcludedFromStats(it.key, cats) }
                .sortedBy { it.value }
                .map { it.key }
                .toList().takeLast(20).reversed()

        val dormant = scan.lastSeen.entries
            .mapNotNull { (name, ts) ->
                if (ts > now || isExcludedFromStats(name, cats)) return@mapNotNull null
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
    /**
     * 連續互動天數。
     *
     * ⚠️ 連續的「天」必須回頭看完整歷史（否則跨出區間的那幾天會被截斷），
     *    但**基準日要跟著使用者選的區間走**，而且只列出「最後一次互動落在區間內」的人。
     *    舊版一律以今天為基準，所以翻到上個月看到的還是今天的連續天數，完全對不上。
     */
    fun getStreaks(
        records: List<InteractionRecord>, aliasMap: Map<String, String>,
        filter: DateFilter, weekStart: Int
    ): List<Pair<String, Int>> {
        if (records.isEmpty()) return emptyList()
        val range = rangeOf(filter, weekStart)
        val anchor = startOfDay(range?.second ?: System.currentTimeMillis())
        val rangeStart = range?.first ?: 0L

        // 跟「新出現的人」共用同一份索引，不再各掃一次全表
        val byPerson = fullScan(records, aliasMap).daysByPerson
        return byPerson.mapNotNull { (name, days) ->
            // 非真人、或被歸到「系統通知」標籤的都不該出現在連續互動排行
            if (isExcludedFromStats(name, DataRepository.categories.value)) return@mapNotNull null
            // 最後一次互動必須落在這個區間裡，否則這個人跟本期無關
            val last = days.max()
            if (last < startOfDay(rangeStart) || last > anchor) return@mapNotNull null
            var streak = 0
            var cursor = last
            while (days.contains(cursor)) { streak++; cursor -= 86_400_000L }
            if (streak >= 2) name to streak else null
        }.sortedByDescending { it.second }.take(10)
    }

    /** 近 N 週的每日活躍度，用於貢獻圖 */
    /**
     * 活躍密度用的資料。除了次數，也要帶當天「跟幾個人聊過」——
     * 一天 50 則全來自同一個人，跟來自 10 個人，意義完全不同。
     */
    // 活躍密度固定看最近數月，跟日期篩選無關 —— 切日期不該重算。
    // 快取鍵除了紀錄與別名，還要帶「今天」：跨日之後格子要往前推一格。
    private var contribKey: Triple<List<InteractionRecord>?, Map<String, String>?, Long> = Triple(null, null, -1L)
    private var contribResult: List<ContribDay> = emptyList()

    @Synchronized
    fun getContributionMap(
        records: List<InteractionRecord>, aliasMap: Map<String, String>, weeks: Int = 17
    ): List<ContribDay> {
        val today = startOfDay(System.currentTimeMillis())
        val key = Triple(records, aliasMap, today)
        if (contribResult.isNotEmpty() &&
            contribKey.first === records && contribKey.second === aliasMap && contribKey.third == today
        ) return contribResult
        val days = weeks * 7
        val start = today - (days - 1L) * 86_400_000L
        val counts = IntArray(days)
        val people = Array(days) { HashSet<String>() }
        records.forEach { r ->
            val idx = ((startOfDay(r.timestamp) - start) / 86_400_000L).toInt()
            if (idx in 0 until days) {
                counts[idx]++
                val nm = aliasMap[r.rawSenderName] ?: r.rawSenderName
                if (!isNonPersonContact(nm)) people[idx].add(nm)
            }
        }
        val out = (0 until days).map { ContribDay(start + it * 86_400_000L, counts[it], people[it].size) }
        contribKey = key; contribResult = out
        return out
    }

    fun getCallSummary(records: List<InteractionRecord>, filter: DateFilter): CallSummary {
        val calls = getFilteredRecords(records, filter).filter { it.isCall || it.isMissedCall || it.appName == "電話" }
        val missed = calls.count { it.isMissedCall }
        val answeredCalls = calls.filter { !it.isMissedCall }
        // 只有真的帶秒數的才能拿來算平均
        val timedCalls = answeredCalls.filter { it.durationSeconds > 0 }
        // 未接一定是對方打來；舊資料沒有方向欄位，算進「分不出方向」而不是硬塞給某一邊
        val incoming = calls.count { it.callDirection == 1 || (it.callDirection == 0 && it.isMissedCall) }
        val outgoing = calls.count { it.callDirection == 2 }
        return CallSummary(
            total = calls.size,
            answered = answeredCalls.size,
            missed = missed,
            totalSeconds = timedCalls.sumOf { it.durationSeconds.toLong() },
            timed = timedCalls.size,
            incoming = incoming,
            outgoing = outgoing,
            unknownDir = calls.size - incoming - outgoing
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
    /**
     * ⚠️ 活躍天數必須排除系統通知與非真人來源。
     *
     * 舊版是「這天有任何一筆紀錄就算活躍」—— 但一天裡只要有一則 IG 推播、
     * 一個「11 位新追蹤者」，那天就被算成有互動。
     * 結果就是使用者說的「我的總聊天天數沒那麼多」：數字是對的，但算的不是聊天。
     * 現在只算真人的互動。
     */
    fun getCoverage(
        records: List<InteractionRecord>, filter: DateFilter, weekStart: Int,
        aliasMap: Map<String, String> = emptyMap(), catsMap: Map<String, String> = emptyMap()
    ): CoverageStat {
        if (records.isEmpty()) return CoverageStat(0, 0, 0, 0)
        val range = rangeOf(filter, weekStart)
        val human = records.filter { r ->
            val n = aliasMap[r.rawSenderName] ?: r.rawSenderName
            !isExcludedFromStats(n, catsMap)
        }
        if (human.isEmpty()) return CoverageStat(0, 0, 0, 0)
        val scoped = if (range == null) human else human.filter { it.timestamp in range.first..range.second }
        val activeDays = scoped.map { startOfDay(it.timestamp) }.toSortedSet()
        val totalDays = if (range != null)
            (((startOfDay(range.second) - startOfDay(range.first)) / 86_400_000L) + 1).toInt()
        else if (records.isEmpty()) 0
        else (((startOfDay(human.maxOf { it.timestamp }) - startOfDay(human.minOf { it.timestamp })) / 86_400_000L) + 1).toInt()

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
        // 「構成」類卡片從洞察搬過來，資料也一起搬
        val calls: CallSummary = CallSummary(0, 0, 0, 0L),
        val lengths: List<Triple<String, Int, Float>> = emptyList(),
        val split: GroupSplit = GroupSplit(0L, 0L, 0, 0),
        val extras: ExtraStats = ExtraStats(),
        val ready: Boolean = false
    )

    // 算好的結果留一份在 ViewModel 裡。分頁切走再切回來時，
    // produceState 用它當初始值，畫面直接是上次的內容，不會先空一下再重畫。
    // ⚠️ 只是「不閃空白」，資料仍然會在背景重算並覆蓋，不會拿到過期數字。
    var cachedOverview = OverviewBundle()
        private set
    var cachedInsights = InsightsBundle()
        private set
    var cachedLeader = LeaderBundle()
        private set
    var cachedContacts = ContactsBundle()
        private set

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
            calls = callSummaryOf(scoped),
            lengths = lengthBucketsOf(scoped),
            split = groupSplitOf(scoped),
            extras = extraStatsOf(scoped, records, filter, weekStart),
            ready = true
        ).also { cachedOverview = it }
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
    ).also { cachedLeader = it }

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

    /** @param catsMap 只是拿來當重算依據，內容由 listJunkContacts 直接讀 Repository */
    fun buildContacts(
        records: List<InteractionRecord>, aliasMap: Map<String, String>,
        @Suppress("UNUSED_PARAMETER") catsMap: Map<String, String> = emptyMap()
    ): ContactsBundle {
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
        ).also { cachedContacts = it }
    }

    data class InsightsBundle(
        val trend: List<TrendPoint> = emptyList(),
        val deltas: Triple<PeriodDelta, PeriodDelta, PeriodDelta>? = null,
        val conc: ConcentrationStat = ConcentrationStat(0, 0, 0, 0),
        val churn: ChurnStat = ChurnStat(emptyList(), emptyList()),
        val nightPct: Int = 0,
        val streaks: List<Pair<String, Int>> = emptyList(),
        val contrib: List<ContribDay> = emptyList(),
        val calls: CallSummary = CallSummary(0, 0, 0, 0L),
        val lengths: List<Triple<String, Int, Float>> = emptyList(),
        val cross: List<Pair<String, Int>> = emptyList(),
        val coverage: CoverageStat = CoverageStat(0, 0, 0, 0),
        val split: GroupSplit = GroupSplit(0L, 0L, 0, 0),
        val extras: ExtraStats = ExtraStats(),
        val persona: SocialPersona = SocialPersona.ALL_TYPES[0],
        // 「時間模式」類卡片從總覽搬過來
        val timeStats: List<TimeOfDayStat> = emptyList(),
        val heatmap: Array<IntArray> = Array(7) { IntArray(24) },
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
            streaks = getStreaks(records, aliasMap, filter, weekStart),
            contrib = getContributionMap(records, aliasMap),
            calls = callSummaryOf(scoped),
            lengths = lengthBucketsOf(scoped),
            // 「本期有幾個人從多個平台找你」是一個期間問題，要跟隨篩選
            cross = getCrossPlatform(scoped, aliasMap),
            coverage = getCoverage(records, filter, weekStart, aliasMap, catsMap),
            split = groupSplitOf(scoped),
            extras = extraStatsOf(scoped, records, filter, weekStart),
            persona = getSocialPersona(scoped, DateFilter(FilterType.ALL)),
            timeStats = getTimeOfDayStats(scoped, DateFilter(FilterType.ALL)),
            heatmap = getWeekHourHeatmap(scoped, DateFilter(FilterType.ALL)),
            ready = true
        ).also { cachedInsights = it }
    }

    /** 額外的補充統計 */
    data class ExtraStats(
        val avgMsgPerActiveDay: Int = 0,
        val avgDurPerActiveDay: Long = 0L,
        val busiestDayLabel: String = "—",
        val busiestDayCount: Int = 0,
        // 下鑽需要實際日期，label 只是「8/12」這種字串，回推不回去
        val busiestDayStart: Long = 0L,
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
            busiestDayStart = busiest?.key ?: 0L,
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
        // 存的是配色索引，不是色值 —— 換配色時這個新標籤才會跟著變
        // ⚠️ 不能挑到 6 —— 那是「未分類」的虛線樣式，自訂標籤長成那樣會很怪
        val slot = (0..4).random()
        val newCat = ContactCategory(id = UUID.randomUUID().toString(), label = label, colorInt = slot, bgColorInt = slot)
        DataRepository.addCustomCategory(newCat)
        if (targetUnifiedName != null) DataRepository.updateCategory(targetUnifiedName, newCat.id)
    }

    fun setCategory(name: String, categoryId: String) { DataRepository.updateCategory(name, categoryId) }
    fun isGroupName(name: String) = DataRepository.isGroupName(name)
    fun setGroupOverride(name: String, isGroup: Boolean) = DataRepository.setGroupOverride(name, isGroup)
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
    fun previewRange(start: Long, end: Long) = DataRepository.previewRange(start, end)
    fun clearInteractionsInRange(start: Long, end: Long, keep: Set<String>) =
        DataRepository.clearInteractionsInRange(start, end, keep)
    fun mergeContact(rawName: String, unifiedName: String) { DataRepository.setAlias(rawName, unifiedName) }
    fun renameContact(oldName: String, newName: String) = DataRepository.renameContact(oldName, newName)
    fun unmergeAccount(rawName: String) = DataRepository.unmergeAccount(rawName)
    val lastCleanupRemoved = DataRepository.lastCleanupRemoved
    val wordFreq = DataRepository.wordFreq
    fun clearWordFreq() = DataRepository.clearWordFreq()
    fun purgeJunkWords() = DataRepository.purgeJunkWords()
    fun previewJunkWords() = DataRepository.previewJunkWords()
    fun purgeWords(words: Set<String>) = DataRepository.purgeWords(words)
    val lastWordPurge = DataRepository.lastWordPurge
    val wordSenders = DataRepository.wordSenders
    fun retagSystemContacts() = DataRepository.retagSystemContacts()
    fun previewRetagTargets() = DataRepository.previewRetagTargets()
    val debugLog = DataRepository.debugLog
    fun clearDetectLog() = DataRepository.clearDetectLog()
    val lastServiceBeat = DataRepository.lastServiceBeat
    fun readLastCrash() = DataRepository.readLastCrash()
    val lastErrorTag = DataRepository.lastErrorTag
    fun recordError(tag: String, e: Throwable) = DataRepository.recordError(tag, e)
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
    fun deleteContacts(names: Set<String>) = DataRepository.deleteContactsWithTrash(names)
    val blockedNames = DataRepository.blockedNames
    val sectionOrder = DataRepository.sectionOrder
    val hiddenSections = DataRepository.hiddenSections
    fun setSectionOrder(ids: List<String>) = DataRepository.setSectionOrder(ids)
    fun toggleSectionHidden(id: String) = DataRepository.toggleSectionHidden(id)
    fun resetSections() = DataRepository.resetSections()
    fun resurrectedNames() = DataRepository.resurrectedNames()
    fun blockName(n: String) = DataRepository.blockName(n)
    fun forgetDeleted(n: String) = DataRepository.forgetDeleted(n)
    fun unblockName(n: String) = DataRepository.unblockName(n)
    fun listTrash() = DataRepository.listTrash()
    fun restoreTrashNames(file: String, names: Set<String>) = DataRepository.restoreTrashNames(file, names)
    fun restoreTrash(f: String) = DataRepository.restoreTrash(f)
    fun purgeTrash(f: String) = DataRepository.purgeTrash(f)
    fun peekTrash(f: String) = DataRepository.peekTrash(f)
    fun listJunkContacts() = DataRepository.listJunkContacts()
    val junkIgnored = DataRepository.junkIgnored
    fun ignoreJunk(names: Collection<String>) = DataRepository.ignoreJunk(names)
    fun unignoreJunk(name: String) = DataRepository.unignoreJunk(name)
    val undoAvailable = DataRepository.undoAvailable
    val undoLabel = DataRepository.undoLabel
    fun undoLastDestructive() = DataRepository.undoLastDestructive()
    fun refreshUndoState() = DataRepository.refreshUndoState()
    val cloudBackupUri = DataRepository.cloudBackupUri
    val cloudBackupLast = DataRepository.cloudBackupLast
    val cloudBackupInterval = DataRepository.cloudBackupInterval
    val cloudBackupLocation = DataRepository.cloudBackupLocation
    fun setCloudBackupInterval(days: Int) = DataRepository.setCloudBackupInterval(days)
    fun bindCloudBackup(context: Context, uri: Uri) = DataRepository.bindCloudBackup(context, uri)
    fun unbindCloudBackup(context: Context) = DataRepository.unbindCloudBackup(context)
    fun backupToCloudNow(context: Context) = DataRepository.writeCloudBackup(context)
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
        FilterType.RANGE -> {
            val cal = Calendar.getInstance()
            fun dayStart(t: Long) = cal.apply {
                timeInMillis = t
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            dayStart(minOf(filter.timestamp, filter.endTimestamp)) to
                (dayStart(maxOf(filter.timestamp, filter.endTimestamp)) + 86_399_999L)
        }
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
    // ⚠️ App 已經開著時點通知不會走 onCreate，只會進這裡 ——
    //    少了這個，App 在背景時點回顧通知只會把 App 叫到前景，彈窗不會出現。
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(DailyRecap.EXTRA_OPEN_RECAP, false)) {
            DataRepository.pendingRecap.value = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 每晚回顧與「服務被關掉」提醒。
        // ⚠️ 排程要每次啟動都做 —— AlarmManager 的錳鐘會被「強制停止」清掉，
        //    只靠開機廣播補不回來。重複排同一個 PendingIntent 不會累加。
        DailyRecap.ensureChannels(this)
        DailyRecap.schedule(this)
        if (Build.VERSION.SDK_INT >= 33) {
            runCatching {
                if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 7701)
                }
            }
        }

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
        if (intent?.getBooleanExtra(DailyRecap.EXTRA_OPEN_RECAP, false) == true) {
            DataRepository.pendingRecap.value = true
        }
        setContent {
            // ⚠️ 以前是空的 MaterialTheme{}，等於整包吃 Material3 預設的紫色配色 ——
            //    導覽列選中的膠囊、Checkbox、TextButton 文字、輸入框游標全都是紫的，
            //    跟 App 自己的藍完全對不起來。這就是「色調不統一」最大的來源。
            MaterialTheme(colorScheme = appColorScheme()) {
                Surface(color = AppColors.Background, modifier = Modifier.fillMaxSize()) {
                    RelateStatsApp()
                }
            }
        }
    }
}

/** 把 App 自己的色票交給 Material3，內建元件才不會用預設的紫 */
@Composable
fun appColorScheme(): ColorScheme {
    val accent = AppColors.ActiveBlue
    val brand = AppColors.BrandPrimary
    return lightColorScheme(
        primary = accent, onPrimary = Color.White,
        primaryContainer = accent.copy(alpha = 0.14f), onPrimaryContainer = accent,
        secondary = brand, onSecondary = Color.White,
        // secondaryContainer 就是導覽列選中膠囊的底色
        secondaryContainer = accent.copy(alpha = 0.14f), onSecondaryContainer = accent,
        tertiary = accent, onTertiary = Color.White,
        background = AppColors.Background, onBackground = AppColors.TextPrimary,
        surface = AppColors.Surface, onSurface = AppColors.TextPrimary,
        surfaceVariant = AppColors.SurfaceAlt, onSurfaceVariant = AppColors.TextSecondary,
        outline = AppColors.BorderStrong, outlineVariant = AppColors.BorderLight,
        error = AppColors.WorkText, onError = Color.White
    )
}

/** 這天 vs 前一天的差：(文字, 方向) 方向 1 多、-1 少、0 一樣或前一天沒有 */
private fun recapDelta(cur: Long, prev: Long, prevLabel: String, fmt: (Long) -> String): Pair<String, Int> = when {
    prev == 0L && cur == 0L -> "跟${prevLabel}一樣" to 0
    prev == 0L -> "${prevLabel}沒有" to 0
    cur == prev -> "跟${prevLabel}一樣" to 0
    cur > prev -> ("+" + fmt(cur - prev)) to 1
    else -> ("−" + fmt(prev - cur)) to -1
}

/** 標頭那句話：用訊息量跟前一天比，給一個有溫度的總結，而不是再重複一次數字 */
private fun recapHeadline(r: DailyRecap.RecapStat, isToday: Boolean): String {
    val d = if (isToday) "今天" else "這天"
    val p = if (isToday) "昨天" else "前一天"
    val y = r.yMsgs
    return when {
        y == 0 -> "${d}跟 ${r.people} 個人有互動"
        r.msgs >= y * 1.3 -> "${d}比${p}熱鬧不少"
        r.msgs >= y * 1.05 -> "${d}比${p}多聊了一些"
        r.msgs <= y * 0.7 -> "${d}比${p}安靜許多"
        r.msgs <= y * 0.95 -> "${d}比${p}少聊了一點"
        else -> "${d}跟${p}差不多熱絡"
    }
}

/**
 * 每日報告的概要。
 *
 * 版面由上而下：彩色標頭（日期切換＋總結一句＋主數字）→ 三張數字卡（各自跟前一天比）→
 * 這天的節奏（24 小時長條）→ 聊天對象的關係分布 → 聊最多的三個人。
 * ⚠️ 顏色全部取自目前的圖表配色（brand／accent／colors／tags），換配色時這裡會一起換。
 * ⚠️ 高度上限是螢幕的 78%，內容區自己捲動 —— 不設上限時，資料多的日子會把卡片撐到螢幕底，
 *    「看完整報告」就跟系統導覽列疊在一起。
 * ⚠️ 關掉（✕、點外面、按返回、按「看完整報告」）一律帶到分析頁的「這一天」。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecapDialog(day: Long, onDayChange: (Long) -> Unit, onClose: () -> Unit) {
    val pal = AppColors.Chart
    val todayStart = remember { startOfDayMs(System.currentTimeMillis()) }
    val isToday = day >= todayStart
    val earliest = remember {
        DataRepository.interactions.value.minOfOrNull { it.timestamp }?.let { startOfDayMs(it) } ?: todayStart
    }
    val recap = remember(day) { DailyRecap.buildRecap(day + 12 * 3_600_000L) }
    val dateText = remember(day) { SimpleDateFormat("M 月 d 日 EEEE", Locale.TAIWAN).format(Date(day)) }
    val maxH = (LocalConfiguration.current.screenHeightDp * 0.78f).dp
    val prevLabel = if (isToday) "昨天" else "前一天"
    val d = if (isToday) "今天" else "這天"
    fun shift(days: Int) {
        val c = Calendar.getInstance().apply { timeInMillis = day; add(Calendar.DATE, days) }
        onDayChange(startOfDayMs(c.timeInMillis))
    }
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            // ⚠️ decorFitsSystemWindows = false 才拿得到系統列的邊界；再加 safeDrawingPadding()
            //    與額外的下邊距，卡片底緣一定會停在導覽列上方。
            modifier = Modifier.fillMaxSize().safeDrawingPadding()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
            contentAlignment = Alignment.Center
        ) {
        Column(
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth().heightIn(max = maxH)
                .clip(RoundedCornerShape(28.dp)).background(AppColors.Surface)
        ) {
            // ── 標頭 ──
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(pal.brand, pal.accent)))
                    .padding(start = 18.dp, end = 10.dp, top = 12.dp, bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(end = 40.dp)) {
                    Text(if (isToday) "今日互動報告" else "每日互動報告", fontSize = T.Label, fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.85f), letterSpacing = 1.sp)
                    // 日期切換：‹ 前一天　日期　後一天 ›
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                        val canPrev = day > earliest
                        Text("‹", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = if (canPrev) 0.95f else 0.3f),
                            modifier = Modifier.clip(CircleShape).clickable(enabled = canPrev) { shift(-1) }
                                .padding(horizontal = 8.dp))
                        Text(dateText, fontSize = T.Label, fontWeight = FontWeight.Bold, color = Color.White,
                            maxLines = 1, softWrap = false)
                        Text("›", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = if (!isToday) 0.95f else 0.3f),
                            modifier = Modifier.clip(CircleShape).clickable(enabled = !isToday) { shift(1) }
                                .padding(horizontal = 8.dp))
                        if (!isToday) {
                            Text("回今天", fontSize = T.Micro, fontWeight = FontWeight.Bold, color = pal.brand,
                                maxLines = 1, softWrap = false,
                                modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.9f))
                                    .clickable { onDayChange(todayStart) }.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (recap == null) {
                        Text("${d}很安靜", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                    } else {
                        Text(recapHeadline(recap, isToday), fontSize = 18.sp, fontWeight = FontWeight.Black,
                            color = Color.White, lineHeight = 24.sp)
                        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 2.dp)) {
                            Text("${recap.msgs}", fontSize = 34.sp, fontWeight = FontWeight.Black,
                                color = Color.White, maxLines = 1, softWrap = false)
                            Text(" 則訊息", fontSize = T.Body, fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.85f), modifier = Modifier.padding(bottom = 6.dp))
                        }
                    }
                }
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).size(34.dp).clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f)).clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Close, contentDescription = "關閉", tint = Color.White, modifier = Modifier.size(18.dp)) }
            }

            Column(
                modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (recap == null) {
                    Text("${d}沒有跟真人的互動紀錄。", fontSize = T.Body, color = AppColors.TextSecondary)
                } else {
                    // ── 三張數字卡 ──
                    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RecapTile("訊息", "${recap.msgs}", "則",
                            recapDelta(recap.msgs.toLong(), recap.yMsgs.toLong(), prevLabel) { "$it" }, pal.tagStyle(1), Modifier.weight(1f))
                        RecapTile("對象", "${recap.people}", "位",
                            recapDelta(recap.people.toLong(), recap.yPeople.toLong(), prevLabel) { "$it" }, pal.tagStyle(2), Modifier.weight(1f))
                        RecapTile("停留", formatDuration(recap.dur), "",
                            recapDelta(recap.dur, recap.yDur, prevLabel) { formatDuration(it) }, pal.tagStyle(3), Modifier.weight(1f))
                    }

                    // ── 這天的節奏 ──
                    val maxHour = recap.hourly.maxOrNull() ?: 0
                    if (maxHour > 0) {
                        val peak = recap.hourly.indexOf(maxHour)
                        RecapSectionTitle("${d}的節奏", "最熱絡 $peak:00–${peak + 1}:00")
                        Row(
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            recap.hourly.forEachIndexed { h, c ->
                                val frac = if (c == 0) 0.04f else 0.12f + 0.88f * c / maxHour
                                Box(
                                    modifier = Modifier.weight(1f).fillMaxHeight(frac)
                                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                        .background(
                                            when {
                                                h == peak -> pal.accent
                                                c == 0 -> AppColors.BorderLight
                                                else -> pal.primary.copy(alpha = 0.35f)
                                            }
                                        )
                                )
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 3.dp)) {
                            listOf("0", "6", "12", "18", "24").forEachIndexed { i, l ->
                                Text(l, fontSize = T.Micro, color = AppColors.TextTertiary, maxLines = 1, softWrap = false,
                                    textAlign = when (i) { 0 -> TextAlign.Start; 4 -> TextAlign.End; else -> TextAlign.Center },
                                    modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    // ── 關係分布 ──
                    val shares = recap.catShare.filter { it.second > 0 }
                    if (shares.isNotEmpty()) {
                        RecapSectionTitle("跟誰聊", "${recap.people} 位")
                        val total = shares.sumOf { it.second }.toFloat()
                        Row(modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            shares.forEach { (cat, n) ->
                                Box(modifier = Modifier.weight(n / total).fillMaxHeight()
                                    .background(if (cat.dashed) AppColors.BorderLight else cat.color))
                            }
                        }
                        FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            shares.forEach { (cat, n) ->
                                Text("${cat.label} $n", fontSize = T.Micro, fontWeight = FontWeight.Bold, color = cat.color,
                                    maxLines = 1, softWrap = false,
                                    modifier = Modifier.tagChip(cat, 8.dp).padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                        }
                    }

                    // ── 聊最多 ──
                    if (recap.top.isNotEmpty()) {
                        RecapSectionTitle("${d}聊最多", null)
                        val topMax = recap.top.first().second.toFloat()
                        recap.top.forEachIndexed { i, (name, cnt, app) ->
                            val c = pal.at(i)
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(c.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center) {
                                    Text(name.trim().take(1).ifEmpty { "?" }, fontSize = T.Body, fontWeight = FontWeight.Black,
                                        color = c, maxLines = 1, softWrap = false)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(name, fontSize = T.Body, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                                        if (app.isNotBlank()) {
                                            Text(" · $app", fontSize = T.Micro, color = AppColors.TextTertiary, maxLines = 1, softWrap = false)
                                        }
                                    }
                                    Box(modifier = Modifier.padding(top = 4.dp).fillMaxWidth().height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)).background(AppColors.BorderLight.copy(alpha = 0.6f))) {
                                        Box(modifier = Modifier.fillMaxWidth(cnt / topMax).fillMaxHeight()
                                            .clip(RoundedCornerShape(3.dp)).background(c))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("$cnt 筆", fontSize = T.Label, fontWeight = FontWeight.Bold, color = AppColors.TextSecondary,
                                    maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }
            }

            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = pal.brand),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 14.dp).heightIn(min = 46.dp)
            ) { Text("看${d}的完整報告  →", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) }
        }
        }
    }
}

@Composable
private fun RecapSectionTitle(title: String, trailing: String?) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = T.Label, fontWeight = FontWeight.Black, color = AppColors.TextPrimary, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, fontSize = T.Micro, color = AppColors.TextTertiary, maxLines = 1, softWrap = false)
    }
}

/** 回顧的數字卡：底色吃標籤配色，差值用小膠囊標出多或少 */
@Composable
private fun RecapTile(label: String, value: String, unit: String, delta: Pair<String, Int>, style: TagStyle, modifier: Modifier) {
    val bg = if (style.dashed) AppColors.BorderLight.copy(alpha = 0.4f) else style.bg
    Column(
        modifier = modifier.fillMaxHeight().clip(RoundedCornerShape(16.dp)).background(bg)
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        Text(label, fontSize = T.Micro, fontWeight = FontWeight.Bold, color = style.text.copy(alpha = 0.85f), maxLines = 1)
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 2.dp)) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = style.text,
                maxLines = 1, softWrap = false, overflow = TextOverflow.Visible)
            if (unit.isNotEmpty()) Text(" $unit", fontSize = T.Micro, fontWeight = FontWeight.Bold, color = style.text,
                modifier = Modifier.padding(bottom = 3.dp))
        }
        Spacer(modifier = Modifier.weight(1f))
        val (txt, dir) = delta
        Text(
            (when (dir) { 1 -> "▲ "; -1 -> "▼ "; else -> "" }) + txt,
            fontSize = T.Micro, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false,
            color = style.text,
            modifier = Modifier.padding(top = 6.dp).clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.6f)).padding(horizontal = 6.dp, vertical = 2.dp)
        )
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
        "需要「通知存取」與「無障礙服務」才能開始記錄。開啟前會先說明讀取哪些資料；所有資料只存在手機，App 沒有網路權限。",
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
    /**
     * 分頁的返回堆疊。
     *
     * ⚠️ 沒有這個，按實體返回鍵會直接退出 App —— 不管你在第幾層都一樣。
     * 也不要改成「一律回概況」：那樣從排行→分析按返回會跳到概況，
     * 使用者要的是回到剛剛那一頁。
     * 切到已經在堆疊裡的分頁時先移除舊的再推入，堆疊才不會長出一堆重複項。
     */
    val tabStack = rememberSaveable(saver = listSaver(
        save = { it.toList() },
        restore = { it.toMutableStateList() }
    )) { mutableStateListOf("overview") }
    val activeTab = tabStack.last()
    val setActiveTab: (String) -> Unit = { t ->
        if (t != tabStack.last()) {
            tabStack.remove(t)
            tabStack.add(t)
            // 只是為了「回上一頁」，不需要保留整段瀏覽史
            while (tabStack.size > 6) tabStack.removeAt(0)
        }
    }
    BackHandler(enabled = tabStack.size > 1) { tabStack.removeAt(tabStack.lastIndex) }

    // 每日報告：通知點進來、或上方「每日報告」按鈕 → 概要彈窗（可翻前後天）→ 關掉就看那天的完整分析
    val pendingRecap by viewModel.pendingRecap.collectAsState()
    var recapDay by rememberSaveable { mutableStateOf<Long?>(null) }
    LaunchedEffect(pendingRecap) {
        if (pendingRecap) {
            recapDay = startOfDayMs(System.currentTimeMillis())
            viewModel.pendingRecap.value = false
        }
    }
    recapDay?.let { day ->
        RecapDialog(day, onDayChange = { recapDay = it }) {
            recapDay = null
            viewModel.setTimeFilter(DateFilter(FilterType.SPECIFIC_DAY, day + 12 * 3_600_000L))
            setActiveTab("insights")
        }
    }

    Scaffold(
        containerColor = AppColors.Background,
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().background(AppColors.Surface).statusBarsPadding().padding(top = 8.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = D.Gutter, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    // 跟 App icon 同一個記號：三個遞增、彼此交疊的圓
                    Canvas(modifier = Modifier.size(26.dp)) {
                        val corner = size.minDimension * 0.26f
                        // ⚠️ logo 用固定色，不跟著配色走 —— 換配色是換「資料的顏色」，
                        //    App 的識別記號換掉就像換了一個 App。
                        drawRoundRect(color = Color(0xFF0F172A), cornerRadius = CornerRadius(corner, corner))
                        val cy = size.height * 0.5f
                        // 比例與 ic_launcher_foreground.xml 一致（108 畫布上的 30/48/70 與 9/13/18）
                        val spec = listOf(
                            Triple(0.278f, 0.083f, 0.55f),
                            Triple(0.444f, 0.120f, 0.85f),
                            Triple(0.648f, 0.167f, -1f)
                        )
                        spec.forEach { (cx, rr, alpha) ->
                            drawCircle(
                                color = if (alpha < 0f) Color(0xFF2563EB) else Color.White.copy(alpha = alpha),
                                radius = size.minDimension * rr,
                                center = Offset(size.width * cx, cy)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    // 隨時都能打開每日報告，不必等晚上的通知；打開後可以往前翻
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(50))
                            .border(1.dp, AppColors.BorderLight, RoundedCornerShape(50))
                            .clickable { recapDay = startOfDayMs(System.currentTimeMillis()) }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.DateRange, contentDescription = null, tint = AppColors.ActiveBlue, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("每日報告", fontSize = T.Label, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary,
                            maxLines = 1, softWrap = false)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text("RelateStats", color = AppColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.4).sp)
                }
                HorizontalDivider(color = AppColors.BorderLight)
            }
        },
        bottomBar = {
            // ⚠️ tonalElevation 必須是 0。
            //    containerColor 跟 colorScheme.surface 相等時，M3 會拿 primary 色疊一層「色調膜」上去，
            //    整條分頁列就變成淡藍的 —— 跟白底的內容區對不起來。
            //    改成平的白底，跟內容的分隔靠上緣的細線。
            Column {
            HorizontalDivider(color = AppColors.BorderLight)
            NavigationBar(containerColor = AppColors.Surface, tonalElevation = 0.dp) {
                NavigationBarItem(selected = activeTab == "overview", onClick = { setActiveTab("overview") }, icon = { Icon(Icons.Rounded.Home, "概況") }, label = { Text("概況", fontSize = 10.sp, maxLines = 1, softWrap = false) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = AppColors.ActiveBlue, selectedTextColor = AppColors.ActiveBlue))
                NavigationBarItem(selected = activeTab == "leaderboard", onClick = { setActiveTab("leaderboard") }, icon = { Icon(Icons.AutoMirrored.Rounded.List, "排行") }, label = { Text("排行", fontSize = 10.sp, maxLines = 1, softWrap = false) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = AppColors.ActiveBlue, selectedTextColor = AppColors.ActiveBlue))
                NavigationBarItem(selected = activeTab == "insights", onClick = { setActiveTab("insights") }, icon = { Icon(Icons.Rounded.Star, "分析") }, label = { Text("分析", fontSize = 10.sp, maxLines = 1, softWrap = false) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = AppColors.ActiveBlue, selectedTextColor = AppColors.ActiveBlue))
                NavigationBarItem(selected = activeTab == "contacts", onClick = { setActiveTab("contacts") }, icon = { Icon(Icons.Rounded.Person, "聯絡人") }, label = { Text("聯絡人", fontSize = 10.sp, maxLines = 1, softWrap = false) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = AppColors.ActiveBlue, selectedTextColor = AppColors.ActiveBlue))
                NavigationBarItem(selected = activeTab == "settings", onClick = { setActiveTab("settings") }, icon = { Icon(Icons.Rounded.Settings, "設定") }, label = { Text("設定", fontSize = 10.sp, maxLines = 1, softWrap = false) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = AppColors.ActiveBlue, selectedTextColor = AppColors.ActiveBlue))
            }
            }
        }
    ) { paddingValues ->
        // ⚠️ when(activeTab) 會把上一個分頁整個移出組合樹，
        //    裡面的 rememberSaveable（含捲動位置）就跟著被丟掉 ——
        //    所以切回來永遠是最上面。SaveableStateProvider 會按 key 保存並還原。
        val stateHolder = rememberSaveableStateHolder()
        // ⚠️ 不要再加 navigationBarsPadding()。
        //    paddingValues 已經含了「導覽列高度 + 系統手勢列」的內距，
        //    再加一次等於多墊一條系統列的高度，中間就露出 Scaffold 的底色 ——
        //    那就是分頁列上方那條固定的灰帶。
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            stateHolder.SaveableStateProvider(activeTab) {
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
}

@Composable
fun MinimalCalendar(selectedTime: Long, onDateSelected: (Long) -> Unit, weekStartDay: Int = Calendar.MONDAY) {
    val (currentMonthCal, setCurrentMonthCal) = remember(selectedTime) { mutableStateOf(Calendar.getInstance().apply { timeInMillis = selectedTime }) }

    val year = currentMonthCal.get(Calendar.YEAR)
    val month = currentMonthCal.get(Calendar.MONTH)
    val todayCal = remember { Calendar.getInstance() }

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
                        // 今天永遠要看得出來，不然翻到別的月份就失去方位感
                        val isToday = todayCal.get(Calendar.YEAR) == year &&
                            todayCal.get(Calendar.MONTH) == month &&
                            todayCal.get(Calendar.DAY_OF_MONTH) == dayNumber
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) AppColors.ActiveBlue else Color.Transparent)
                                .then(
                                    if (isToday && !isSelected)
                                        Modifier.border(1.5.dp, AppColors.ActiveBlue, CircleShape)
                                    else Modifier
                                )
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
fun TimeFilterSelector(
    currentFilter: DateFilter,
    onFilterChange: (DateFilter) -> Unit,
    weekStartDay: Int,
    /**
     * 頁面專屬的按鈕，畫在「單日／單週／區間／全部」這一列的右邊。
     *
     * ⚠️ 為什麼要有這個插槽：像洞察頁的「排序」這種一頁只有一顆的入口，
     *    自己站一整列會多吃掉一個列高（大字體下更明顯），但畫面上其實整列都是空的。
     *    塞進本來就一定會出現的日期列，等於不花任何垂直空間。
     *    不要改放在「‹ 日期 ›」那一列 —— 選「全部」時那一列根本不會出現，按鈕會跟著消失。
     */
    trailing: (@Composable () -> Unit)? = null
) {
    val (showCalendar, setShowCalendar) = remember { mutableStateOf(false) }
    // 選區間時要記住現在在挑起點還是終點
    var pickingEnd by remember { mutableStateOf(false) }
    val mdFmt = remember { SimpleDateFormat("M/d", Locale.US) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // height(IntrinsicSize.Min) + fillMaxHeight()：讓右邊的按鈕自動長到跟分頁列一樣高。
        // 寫死 dp 高度在大字體下一定會對不齊 —— 分頁列會被 sp 撐高，按鈕不會。
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
        Row(modifier = Modifier.weight(1f).background(AppColors.Surface, RoundedCornerShape(10.dp)).border(1.dp, AppColors.BorderLight, RoundedCornerShape(10.dp)).padding(3.dp)) {
            val types = listOf(
                FilterType.SPECIFIC_DAY to "單日",
                FilterType.WEEK to "單週",
                FilterType.RANGE to "區間",
                FilterType.ALL to "全部"
            )
            types.forEach { (type, label) ->
                val isSelected = currentFilter.type == type
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(7.dp)).background(if (isSelected) AppColors.BrandPrimary else Color.Transparent)
                    .clickable {
                        // ⚠️ 保留目前停在哪一天。舊版一律帶 System.currentTimeMillis()，
                        //    所以往回翻到 8/10 再按「單週」會跳回本週，翻到一半就被拉走。
                        if (type == FilterType.RANGE) {
                            // 進區間模式時給一個合理預設：從 7 天前到今天
                            val end = currentFilter.timestamp
                            val start = end - 6L * 86_400_000L
                            onFilterChange(DateFilter(type, start, end))
                            pickingEnd = false
                            setShowCalendar(true)
                        } else {
                            onFilterChange(DateFilter(type, currentFilter.timestamp, currentFilter.timestamp))
                            setShowCalendar(false)
                        }
                    }.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                    Text(label, color = if (isSelected) Color.White else AppColors.TextSecondary, fontSize = T.Body, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                }
            }
        }
            if (trailing != null) {
                Spacer(modifier = Modifier.width(6.dp))
                trailing()
            }
        }

        if (currentFilter.type == FilterType.RANGE) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf(false to "起", true to "迄").forEach { (isEnd, tag) ->
                    val active = pickingEnd == isEnd
                    val ts = if (isEnd) currentFilter.endTimestamp else currentFilter.timestamp
                    Row(
                        modifier = Modifier.weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (active) AppColors.ActiveBlue.copy(alpha = 0.12f) else AppColors.Surface)
                            .border(1.dp, if (active) AppColors.ActiveBlue else AppColors.BorderLight, RoundedCornerShape(8.dp))
                            .clickable { pickingEnd = isEnd; setShowCalendar(true) }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tag, fontSize = T.Micro, color = AppColors.TextSecondary, maxLines = 1, softWrap = false)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(mdFmt.format(Date(ts)), fontSize = T.Body, fontWeight = FontWeight.Bold,
                            color = AppColors.ActiveBlue, maxLines = 1, softWrap = false)
                    }
                }
            }
            val days = (((startOfDayMs(maxOf(currentFilter.timestamp, currentFilter.endTimestamp)) -
                startOfDayMs(minOf(currentFilter.timestamp, currentFilter.endTimestamp))) / 86_400_000L) + 1).toInt()
            Text("共 $days 天　·　點上面的日期再從下方月曆挑",
                fontSize = T.Micro, color = AppColors.TextTertiary, modifier = Modifier.padding(top = 4.dp))
        } else if (currentFilter.type != FilterType.ALL) {
            Spacer(modifier = Modifier.height(6.dp))
            // 現在停的是不是「今天／本週」。翻走了才需要「回今天」那顆鈕。
            val onCurrent = remember(currentFilter, weekStartDay) {
                val now = System.currentTimeMillis()
                if (currentFilter.type == FilterType.SPECIFIC_DAY)
                    DateFilter(FilterType.SPECIFIC_DAY, now).isSameDay(currentFilter.timestamp)
                else getStartOfWeekTimestamp(currentFilter.timestamp, weekStartDay) ==
                    getStartOfWeekTimestamp(now, weekStartDay)
            }
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

                // ⚠️ 翻到第五週之後要回到本週，只能一直按箭頭 —— 這顆是唯一的捷徑。
                //    只在「不在今天／本週」時出現，平常不佔位置。
                if (!onCurrent) {
                    Text(
                        if (currentFilter.type == FilterType.SPECIFIC_DAY) "今天" else "本週",
                        fontSize = T.Micro, fontWeight = FontWeight.Bold, color = Color.White,
                        maxLines = 1, softWrap = false,
                        modifier = Modifier.padding(end = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppColors.BrandPrimary)
                            .clickable { onFilterChange(currentFilter.copy(timestamp = System.currentTimeMillis())) }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    )
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
                MinimalCalendar(selectedTime = if (pickingEnd) currentFilter.endTimestamp else currentFilter.timestamp, onDateSelected = {
                    onFilterChange(
                        if (currentFilter.type == FilterType.RANGE && pickingEnd) currentFilter.copy(endTimestamp = it)
                        else if (currentFilter.type == FilterType.RANGE) currentFilter.copy(timestamp = it)
                        else currentFilter.copy(timestamp = it, endTimestamp = it)
                    )
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
                                Box(modifier = Modifier.tagChip(stat.category, 4.dp).padding(horizontal = 6.dp, vertical = 2.dp)) {
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
        var disclose by remember { mutableStateOf<String?>(null) }
        disclose?.let { k ->
            PermissionDisclosureDialog(k, onAgree = { disclose = null; openPermissionSettings(context, k) },
                onDismiss = { disclose = null })
        }
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
                    onClick = { disclose = "access" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.WorkText),
                    shape = RoundedCornerShape(9.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) { Text("開啟無障礙服務", fontSize = T.Label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
            if (!notifOn) {
                Button(
                    onClick = { disclose = "notif" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.WorkText),
                    shape = RoundedCornerShape(9.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) { Text("開啟通知存取", fontSize = T.Label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
    }
}

/**
 * 長清單右側的捲動指示條，可以直接拖。
 *
 * ⚠️ 位置用「第幾筆」估算，不是像素。LazyColumn 的每一列高度不一定一樣，
 *    要拿到精確的像素位移得把所有列都量過，那就失去 lazy 的意義了。
 *    對「快速跳到中間」這個用途，用索引估算已經夠準。
 * ⚠️ 只有夠長的清單才畫。二十筆以內出現捲動條只是多一條線在旁邊晃。
 */
@Composable
fun BoxScope.ListScrollbar(listState: LazyListState, minItems: Int = 20) {
    val total = listState.layoutInfo.totalItemsCount
    if (total < minItems) return
    val scope = rememberCoroutineScope()
    val visible = listState.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
    val maxIndex = (total - visible).coerceAtLeast(1)
    val progress = (listState.firstVisibleItemIndex.toFloat() / maxIndex).coerceIn(0f, 1f)
    val thumbFrac = (visible.toFloat() / total).coerceIn(0.06f, 1f)

    BoxWithConstraints(
        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(20.dp)
            .pointerInput(total, maxIndex) {
                detectVerticalDragGestures { change, _ ->
                    val f = (change.position.y / size.height.toFloat()).coerceIn(0f, 1f)
                    scope.launch { listState.scrollToItem((f * maxIndex).toInt()) }
                }
            }
    ) {
        val trackH = maxHeight
        val thumbH = trackH * thumbFrac
        Box(
            modifier = Modifier.align(Alignment.TopEnd)
                .offset(y = (trackH - thumbH) * progress)
                .padding(end = 3.dp)
                .width(4.dp).height(thumbH)
                .clip(RoundedCornerShape(2.dp))
                .background(AppColors.BorderStrong.copy(alpha = 0.75f))
        )
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
                                    .tagChip(cat, 8.dp)
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
fun QuickTagDialog(
    name: String,
    categories: List<ContactCategory>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
    currentId: String? = null,
    onAddNew: (() -> Unit)? = null
) {
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
            // 標籤可能不少，超過畫面高度時要能捲
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())
            ) {
                categories.forEach { cat ->
                    val on = currentId == cat.id
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .tagChip(cat, 8.dp)
                            .then(if (on) Modifier.border(2.dp, cat.color, RoundedCornerShape(8.dp)) else Modifier)
                            .clickable { onPick(cat.id) }
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(cat.color))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(cat.label, color = cat.color, fontSize = T.Body, fontWeight = FontWeight.Bold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        if (on) Text("目前", color = cat.color, fontSize = T.Micro, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                }
                if (onAddNew != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppColors.Background)
                            .clickable { onAddNew() }
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = AppColors.ActiveBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("新增自訂標籤", color = AppColors.ActiveBlue, fontSize = T.Body, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
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

/**
 * 提醒卡：需要使用者處理、但還沒到「出事了」等級的通知。
 *
 * ⚠️ 不要再用「整塊粉紅底 + 兩顆整寬紅按鈕」。
 *    那是警告等級的視覺語言，但這些其實只是待辦提醒；
 *    滿版紅色一進聯絡人頁就搶走全部注意力，看起來像 App 壞了。
 *    改成白底卡 + 左側一條細色帶 + 文字型動作，跟銀行 App 的通知列同一個調性。
 * ⚠️ 左色帶用 height(IntrinsicSize.Min) + fillMaxHeight()，
 *    不要寫死高度 —— 卡片高度會隨字體縮放改變。
 */
@Composable
fun NoticeCard(
    title: String,
    subtitle: String? = null,
    accent: Color = AppColors.WorkText,
    onOpen: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    body: (@Composable ColumnScope.() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.BorderLight, RoundedCornerShape(12.dp))
            .height(IntrinsicSize.Min)
    ) {
        Box(modifier = Modifier.width(3.dp).fillMaxHeight().background(accent))
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp, end = 8.dp, top = 11.dp, bottom = 11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    modifier = Modifier.weight(1f).padding(end = 6.dp)
                        .let { m -> if (onOpen != null) m.clip(RoundedCornerShape(6.dp)).clickable { onOpen() } else m }
                ) {
                    Text(title, fontSize = T.Body, fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (subtitle != null) {
                        Text(subtitle, fontSize = T.Micro, color = AppColors.TextTertiary,
                            lineHeight = 15.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                }
                if (onOpen != null) {
                    Text("\u203a", fontSize = 17.sp, color = AppColors.TextTertiary, maxLines = 1, softWrap = false)
                }
                if (onDismiss != null) {
                    Text("\u2715", fontSize = T.Label, color = AppColors.TextTertiary,
                        maxLines = 1, softWrap = false,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { onDismiss() }.padding(6.dp))
                }
            }
            body?.let { Spacer(modifier = Modifier.height(8.dp)); it() }
        }
    }
}

/** 提醒卡上的文字型動作。填色的只留給主要動作，其他一律描邊。 */
@Composable
fun NoticeAction(label: String, tint: Color, filled: Boolean = false, onClick: () -> Unit) {
    Text(
        label, fontSize = T.Label, fontWeight = FontWeight.Bold,
        color = if (filled) Color.White else tint, maxLines = 1, softWrap = false,
        modifier = Modifier.clip(RoundedCornerShape(8.dp))
            .background(if (filled) tint else Color.Transparent)
            .let { m -> if (filled) m else m.border(1.dp, AppColors.BorderLight, RoundedCornerShape(8.dp)) }
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp)
    )
}

/**
 * 標籤 chip 的底。
 *
 * ⚠️ 一般標籤是實心底色；「未分類」是透明底＋虛線框 ——
 *    它代表「還沒分」，不該長得像一個真正的標籤。
 *    全 App 的 chip 都走這裡，不要再各自寫 .background(cat.bgColor, ...)，
 *    不然新增樣式時會漏掉幾處。
 */
fun Modifier.tagChip(cat: ContactCategory, radius: Dp): Modifier =
    if (cat.dashed) this.drawBehind {
        val r = radius.toPx()
        drawRoundRect(
            color = cat.color.copy(alpha = 0.55f),
            cornerRadius = CornerRadius(r, r),
            style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)))
        )
    } else this.background(cat.bgColor, RoundedCornerShape(radius))

/**
 * 全部的社交人格類型。
 * 只告訴使用者「你是 X」卻不說還有哪些，那個結果就沒有參照點，看了等於沒看。
 */
@Composable
fun PersonaListDialog(current: SocialPersona, onDismiss: () -> Unit) {
    AlertDialog(
        containerColor = AppColors.Surface,
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("社交人格", fontWeight = FontWeight.Black, fontSize = 18.sp, color = AppColors.TextPrimary)
                Text("依本期的訊息量、停留時間、平均訊息長度判定；換日期範圍可能換人格",
                    fontSize = T.Micro, color = AppColors.TextTertiary, lineHeight = 15.sp)
            }
        },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 440.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(SocialPersona.ALL_TYPES, key = { it.title }) { p ->
                    val on = p.title == current.title
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (on) AppColors.ActiveBlue.copy(alpha = 0.08f) else AppColors.SurfaceAlt)
                            .then(if (on) Modifier.border(1.5.dp, AppColors.ActiveBlue, RoundedCornerShape(10.dp)) else Modifier)
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(p.emoji, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(p.title, fontSize = T.Body, fontWeight = FontWeight.Bold,
                                    color = AppColors.TextPrimary, maxLines = 1, softWrap = false)
                                if (on) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("你目前", fontSize = T.Micro, fontWeight = FontWeight.Bold, color = Color.White,
                                        maxLines = 1, softWrap = false,
                                        modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(AppColors.ActiveBlue)
                                            .padding(horizontal = 5.dp, vertical = 1.dp))
                                }
                            }
                            Text(p.description.substringBefore("\n"), fontSize = T.Label,
                                color = AppColors.TextSecondary, lineHeight = 17.sp)
                            val rule = p.description.substringAfter("\n", "").trim('(', ')', '（', '）')
                            if (rule.isNotBlank()) {
                                Text("判定：$rule", fontSize = T.Micro, color = AppColors.TextTertiary, lineHeight = 14.sp,
                                    modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("關閉", color = AppColors.TextSecondary) } }
    )
}

/**
 * 讓文字塞得進甜甜圈的空心。
 *
 * ⚠️ 空心內徑只有約 65dp。「55123」「6h 57m」這種值用固定 17sp，
 *    系統字體一放大就溢出圓環（「總停留時數跑版」就是這個）。
 *    依字數與 fontScale 反推字級：中日文字算一個字寬，數字與英文算 0.6。
 */
fun fitInHoleSp(text: String, fontScale: Float, maxSp: Float, usableDp: Float = 56f): Float {
    val units = text.sumOf { c -> if (c.code >= 0x2E80) 1.0 else 0.6 }.toFloat().coerceAtLeast(1f)
    return (usableDp / units / fontScale.coerceAtLeast(1f)).coerceIn(8f, maxSp)
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
    // ⚠️ 不要寫死 size()。以前是 20.dp 的圓框裝 13.sp 的字 ——
    //    系統字體放大之後字比框大，ⓘ 就被切掉一半。
    //    改成讓框跟著字長，點擊範圍靠 padding 撐開。
    Box(
        modifier = Modifier.clip(CircleShape).clickable { open = true }.padding(3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("ⓘ", fontSize = T.Label, color = tint, maxLines = 1, softWrap = false)
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
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DonutChart(
    slices: List<Triple<String, Long, Color>>,
    centerLabel: String,
    centerValue: String,
    onSliceClick: ((String) -> Unit)? = null
) {
    val total = slices.sumOf { it.second }.coerceAtLeast(1L)
    // 點扇形＝預覽（中心顯示該項數字），長按＝下鑽。
    // 只有圖例能點的話，圖本身看起來像裝飾品。
    var picked by remember(slices.size) { mutableStateOf<Int?>(null) }

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(116.dp)
                .pointerInput(slices) {
                    fun hit(offset: Offset): Int? {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val dx = offset.x - cx
                        val dy = offset.y - cy
                        val r = kotlin.math.sqrt(dx * dx + dy * dy)
                        val outer = minOf(size.width, size.height) / 2f
                        // 圓環內外緣之外的點擊不算（中間的空心與圓外都略過）
                        if (r > outer || r < outer * 0.45f) return null
                        // 從 12 點鐘方向順時針量角度，跟繪圖的 startAngle = -90 對齊
                        var deg = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                        if (deg < 0f) deg += 360f
                        var acc = 0f
                        slices.forEachIndexed { i, sl ->
                            val sweep = (sl.second.toFloat() / total) * 360f
                            if (deg >= acc && deg < acc + sweep) return i
                            acc += sweep
                        }
                        return null
                    }
                    detectTapGestures(
                        onTap = { off -> hit(off)?.let { i -> picked = if (picked == i) null else i } },
                        onLongPress = { off -> hit(off)?.let { i -> onSliceClick?.invoke(slices[i].first) } }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = size.minDimension * 0.22f
                val inset = stroke / 2f
                var startAngle = -90f
                slices.forEachIndexed { i, sl ->
                    val sweep = (sl.second.toFloat() / total) * 360f
                    if (sweep > 0f) {
                        val on = picked == i
                        drawArc(
                            color = if (picked == null || on) sl.third else sl.third.copy(alpha = 0.35f),
                            startAngle = startAngle, sweepAngle = sweep - 1.2f, useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = Size(size.width - stroke, size.height - stroke),
                            style = Stroke(width = if (on) stroke * 1.25f else stroke, cap = StrokeCap.Butt)
                        )
                    }
                    startAngle += sweep
                }
            }
            val p = picked
            val fsc = LocalDensity.current.fontScale
            // 寬度鎖在空心裡面，字級依內容縮放
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(max = 62.dp)) {
                if (p != null && p in slices.indices) {
                    val sl = slices[p]
                    val pctText = "${((sl.second.toFloat() / total) * 100).toInt()}%"
                    Text(pctText, fontSize = fitInHoleSp(pctText, fsc, 17f).sp,
                        fontWeight = FontWeight.Black, color = sl.third, maxLines = 1, softWrap = false)
                    Text(sl.first, fontSize = fitInHoleSp(sl.first, fsc, 9f).sp, color = AppColors.TextSecondary,
                        maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                } else {
                    Text(centerValue, fontSize = fitInHoleSp(centerValue, fsc, 17f).sp, fontWeight = FontWeight.Black,
                        color = AppColors.TextPrimary, maxLines = 1, softWrap = false)
                    Text(centerLabel, fontSize = fitInHoleSp(centerLabel, fsc, 9f).sp,
                        color = AppColors.TextTertiary, maxLines = 1, softWrap = false)
                }
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        // ⚠️ 「＋其他 N 項」以前只是一行灰字，看起來像按鈕卻按不下去。現在點了會展開。
        var showAllSlices by remember(slices.size) { mutableStateOf(false) }
        Column(modifier = Modifier.weight(1f)) {
            (if (showAllSlices) slices else slices.take(6)).forEachIndexed { i, sl ->
                val (name, v, c) = sl
                val pct = ((v.toFloat() / total) * 100).toInt()
                val on = picked == i
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .let { m ->
                            if (onSliceClick != null) m.combinedClickable(
                                onClick = { picked = if (on) null else i },
                                onLongClick = { onSliceClick(name) }
                            ) else m
                        }
                        .padding(vertical = 3.dp, horizontal = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(c))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(name, fontSize = T.Label,
                        color = if (on) c else AppColors.TextPrimary,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text("$pct%", fontSize = T.Label, fontWeight = FontWeight.Bold,
                        color = if (on) c else AppColors.TextPrimary, maxLines = 1, softWrap = false)
                }
            }
            if (slices.size > 6) {
                Text(
                    if (showAllSlices) "收合" else "＋其他 ${slices.size - 6} 項",
                    fontSize = T.Label, fontWeight = FontWeight.Bold, color = AppColors.ActiveBlue,
                    maxLines = 1, softWrap = false,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp))
                        .clickable { showAllSlices = !showAllSlices }
                        .padding(horizontal = 3.dp, vertical = 5.dp)
                )
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
fun KpiTile(
    label: String, value: String, sub: String?, accent: Color,
    modifier: Modifier = Modifier, onClick: (() -> Unit)? = null
) {
    // ⚠️ 不可以寫死 .height()。方塊高度是 dp、裡面的字是 sp，
    //    使用者把系統字體放大時 sp 會變大、dp 不會，第三行就會被裁掉。
    //    改成內容決定高度；同列要等高，由外層 Row 的 IntrinsicSize.Min + fillMaxHeight 負責。
    Column(
        modifier = modifier
            .fillMaxHeight()
            .heightIn(min = 76.dp)
            .clip(RoundedCornerShape(D.Radius))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.BorderLight, RoundedCornerShape(D.Radius))
            .let { m -> if (onClick != null) m.clickable { onClick() } else m }
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
@OptIn(ExperimentalFoundationApi::class)
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
    // ⚠️ 這個元件被平台分佈、標籤分佈、訊息長度、Top 5、聯絡人詳情共用，
    //    高度改一次全部跟著改。主／次數值以前上下疊成兩行、外加 7dp 上下留白，
    //    一列就要 ~39dp，五六列就吃掉半個螢幕。改成同一行並排 + 4dp 留白，
    //    一列 ~24dp，資訊完全沒少。
    Row(
        modifier = Modifier.fillMaxWidth()
            .let { if (onClick != null) it.clickable { onClick() } else it }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(dotColor))
        Spacer(modifier = Modifier.width(6.dp))
        badge?.let { it(); Spacer(modifier = Modifier.width(4.dp)) }
        Text(
            name, fontSize = T.Body, fontWeight = FontWeight.Medium, color = AppColors.TextPrimary,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(end = 6.dp)
        )
        Box(modifier = Modifier.width(48.dp).height(5.dp).clip(RoundedCornerShape(3.dp)).background(AppColors.Background)) {
            Box(modifier = Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight().background(dotColor))
        }
        Spacer(modifier = Modifier.width(8.dp))
        // ⚠️ 主／次數值各自給固定欄寬並靠右，整欄的數字才會對齊。
        //    只用 Row 排在一起的話，名稱長度不同就會參差不齊。
        Text(primary, fontSize = T.Body, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary,
            maxLines = 1, softWrap = false, textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = 52.dp))
        if (secondary != null) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(secondary, fontSize = T.Micro, color = AppColors.TextTertiary,
                maxLines = 1, softWrap = false, textAlign = TextAlign.End,
                modifier = Modifier.widthIn(min = 46.dp))
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
@OptIn(ExperimentalFoundationApi::class)
fun StatPair(
    label: String, value: String, sub: String? = null,
    modifier: Modifier = Modifier, onClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(AppColors.SurfaceAlt)
            .let { m ->
                if (onClick != null) m.combinedClickable(onClick = onClick, onLongClick = onClick) else m
            }
            .padding(horizontal = 9.dp, vertical = 7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = T.Micro, color = AppColors.TextSecondary, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            // 可以點的要看得出來，否則使用者不會知道還有東西
            if (onClick != null) {
                Spacer(modifier = Modifier.width(3.dp))
                Text("›", fontSize = T.Body, color = AppColors.ActiveBlue, maxLines = 1, softWrap = false)
            }
        }
        Text(value, fontSize = T.KpiSm, fontWeight = FontWeight.Black, color = AppColors.TextPrimary, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
        // ⚠️ 副標一定要畫，沒有就畫空字串。這個元件都是兩兩並排，
        //    只要一格有副標、另一格沒有，兩格高度就會差一行，看起來就是跑版。
        Text(sub ?: "", fontSize = T.Micro, color = AppColors.TextTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** 逐日趨勢長條圖 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
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
                    fontSize = T.Label, fontWeight = FontWeight.Bold, color = AppColors.Chart.primary, maxLines = 1, softWrap = false
                )
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
                    // ⚠️ 長按要跟點擊做同一件事。
                    //    手指停在長條上等它出現說明是很自然的動作，
                    //    只綁 onClick 的話長按等於什麼都沒發生，使用者會以為圖表壞了。
                    modifier = Modifier.weight(1f).fillMaxHeight()
                        .combinedClickable(
                            onClick = { picked = if (on) null else pt },
                            onLongClick = { picked = pt }
                        ),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().fillMaxHeight(frac)
                            .background(
                                if (on) AppColors.BrandPrimary else if (v > 0) AppColors.Chart.primary else AppColors.BorderLight,
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
@OptIn(ExperimentalFoundationApi::class)
fun ContributionGrid(data: List<ContribDay>, weekStartDay: Int = Calendar.MONDAY) {
    if (data.isEmpty()) return
    val max = data.maxOf { it.count }.coerceAtLeast(1)
    val cal = remember { Calendar.getInstance() }

    // 對齊星期：第一欄可能不是完整的一週，前面補空格
    // ⚠️ 這裡以前寫死從週日開始，設定頁的「一週起始日」對它完全沒作用 ——
    //    使用者選週一，貢獻格還是週日開頭，兩邊對不起來。
    val firstDow = remember(data, weekStartDay) {
        cal.timeInMillis = data.first().dayStart
        (cal.get(Calendar.DAY_OF_WEEK) - weekStartDay + 7) % 7
    }
    val cells: List<ContribDay?> = remember(data, firstDow) {
        List(firstDow) { null } + data
    }
    val weekLabels = remember(weekStartDay) { rotatedWeekLabels(weekStartDay) }

    var picked by remember { mutableStateOf<ContribDay?>(null) }
    val dayFmt = remember { SimpleDateFormat("M/d（E）", Locale.TAIWAN) }

    Column {

        // ⚠️ 星期標籤與格子的邊長必須是「同一個數字」。
        //    舊寫法讓兩邊各自用 aspectRatio(1f) 推導高度：
        //    標籤區寬 20dp → 每格 20dp；格子區是 weight 平分 → 每格約 22dp。
        //    差 2dp，堆七列就差十幾 dp，看起來就是沒對齊。
        //    改成自己算邊長，兩邊都用 .size(cell)。
        val allWeeks = remember(cells) { cells.chunked(7) }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val labelW = 18.dp
            val gap = 2.dp
            // ⚠️ 格子邊長同時也是「星期」標籤那一列的高度。
            //    塞滿 17 週時每格只剩約 16dp，使用者把系統字體放大後
            //    9sp 的中文連行高都不夠，字就被切掉 —— 這是「字還是被卡掉」的原因。
            //    所以先訂下最小格子邊長，再回推畫得下幾週，多的週直接不顯示。
            val minCell = 20.dp
            val avail = maxWidth - labelW - 4.dp
            val fitWeeks = ((avail + gap) / (minCell + gap)).toInt().coerceIn(4, 26)
            val weeks = if (allWeeks.size > fitWeeks) allWeeks.takeLast(fitWeeks) else allWeeks
            val cell = ((avail - gap * (weeks.size - 1).coerceAtLeast(0)) /
                weeks.size.coerceAtLeast(1)).coerceIn(minCell, 32.dp)

            Column {
                // 月份刻度：橫軸到底是哪段時間，光看格子看不出來
                Row(modifier = Modifier.padding(start = labelW + 4.dp, bottom = 3.dp), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    val mcal = Calendar.getInstance()
                    var lastMonth = -1
                    weeks.forEach { week ->
                        val firstDay = week.firstOrNull { it != null }
                        val m = if (firstDay != null) {
                            mcal.timeInMillis = firstDay.dayStart
                            mcal.get(Calendar.MONTH)
                        } else -1
                        val showLabel = m >= 0 && m != lastMonth
                        if (showLabel) lastMonth = m
                        Box(modifier = Modifier.width(cell)) {
                            if (showLabel) {
                                Text("${m + 1}月", fontSize = 8.sp, color = AppColors.TextTertiary,
                                    maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }
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
                                val c = cellData?.count
                                val on = cellData != null && picked?.dayStart == cellData.dayStart
                                // ⚠️ 圖表一律用 ActiveBlue（主題色），不要用 Positive。
                                //    Positive 是「上升／正向」的語意色，拿來畫格子會讓
                                //    這張圖跟隔壁那張藍色的熱力圖看起來像兩個 App。
                                val bg = when {
                                    cellData == null -> Color.Transparent
                                    c == 0 -> AppColors.BorderLight.copy(alpha = 0.5f)
                                    else -> AppColors.Chart.primary.copy(alpha = 0.25f + 0.75f * (c!!.toFloat() / max))
                                }
                                Box(
                                    modifier = Modifier.size(cell)
                                        .background(bg, RoundedCornerShape(2.dp))
                                        .then(if (on) Modifier.border(1.5.dp, AppColors.BrandPrimary, RoundedCornerShape(2.dp)) else Modifier)
                                        .then(
                                            if (cellData != null) Modifier.combinedClickable(
                                                onClick = { picked = if (on) null else cellData },
                                                onLongClick = { picked = cellData }
                                            ) else Modifier
                                        )
                                )
                            }
                            repeat(7 - week.size) { Spacer(modifier = Modifier.size(cell)) }
                        }
                    }
                }
            }
            }
        }
        picked?.let { d ->
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().background(AppColors.SurfaceAlt, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                Text(dayFmt.format(Date(d.dayStart)), fontSize = T.Body, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary, maxLines = 1, softWrap = false)
                Text(
                    // 次數＋人數一起看才有意義：50 則全來自同一人 vs 來自 10 個人差很多
                    "${d.count} 次 · ${d.people} 人",
                    fontSize = T.Body, fontWeight = FontWeight.Bold, color = AppColors.Chart.primary, maxLines = 1, softWrap = false
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        // 圖例那一行本來右邊是空的 —— 把「一欄＝一週」塞在左邊，
        // 不用另外開一行小字就能講清楚橫軸是什麼。
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("每一欄＝一週", fontSize = T.Micro, color = AppColors.TextTertiary, maxLines = 1, softWrap = false)
            Row(verticalAlignment = Alignment.CenterVertically) {
            Text("少", fontSize = T.Micro, color = AppColors.TextTertiary)
            Spacer(modifier = Modifier.width(4.dp))
            listOf(0f, 0.3f, 0.55f, 0.8f, 1f).forEach { a ->
                Box(modifier = Modifier.size(9.dp).background(
                    if (a == 0f) AppColors.BorderLight.copy(alpha = 0.5f) else AppColors.Chart.primary.copy(alpha = 0.25f + 0.75f * a),
                    RoundedCornerShape(2.dp)
                ))
                Spacer(modifier = Modifier.width(2.dp))
            }
            Spacer(modifier = Modifier.width(2.dp))
            Text("多（最高 $max）", fontSize = T.Micro, color = AppColors.TextTertiary, maxLines = 1, softWrap = false)
            }
        }
        val avgPeople = data.filter { it.count > 0 }.let { d -> if (d.isEmpty()) 0 else d.sumOf { it.people } / d.size }
        if (avgPeople > 0) {
            Text("平均每天 $avgPeople 人", fontSize = T.Micro, color = AppColors.TextTertiary,
                modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/** 水平比例條（群組 vs 個人之類的二分佔比） */
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun SplitBar(
    leftLabel: String, leftValue: Long, rightLabel: String, rightValue: Long,
    leftColor: Color, rightColor: Color,
    // ⚠️ fmt 必須留在最後一個參數：呼叫端都是用尾隨 lambda 傳它的，
    //    把別的參數排在它後面會讓尾隨 lambda 改綁到那個參數上。
    onLeftClick: (() -> Unit)? = null, onRightClick: (() -> Unit)? = null,
    fmt: (Long) -> String
) {
    val total = (leftValue + rightValue).coerceAtLeast(1L)
    // ⚠️ 某一邊是 0 的時候也要留一截顏色（4%）。
    //    寬度給 0 的話那一邊會整個消失，看起來像圖表壞了 ——
    //    使用者分不出「這一邊是 0」跟「這張圖沒畫出來」。
    //    也順便讓那一截可以點，行為跟有值時一致。
    val leftFrac = when {
        leftValue == 0L && rightValue == 0L -> 0.5f
        else -> (leftValue.toFloat() / total).coerceIn(0.04f, 0.96f)
    }
    // 長條與底下的圖例都可點：使用者想點哪邊看哪邊，兩處都要有反應
    Column {
        Row(modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))) {
            Box(
                modifier = Modifier.weight(leftFrac).fillMaxHeight()
                    .background(leftColor)
                    .let { m -> if (onLeftClick != null) m.combinedClickable(onClick = onLeftClick, onLongClick = onLeftClick) else m }
            )
            Box(
                modifier = Modifier.weight(1f - leftFrac).fillMaxHeight()
                    .background(rightColor)
                    .let { m -> if (onRightClick != null) m.combinedClickable(onClick = onRightClick, onLongClick = onRightClick) else m }
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clip(RoundedCornerShape(6.dp))
                    .let { m -> if (onLeftClick != null) m.clickable { onLeftClick() } else m }
                    .padding(vertical = 3.dp, horizontal = 2.dp)
            ) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(leftColor))
                Spacer(modifier = Modifier.width(4.dp))
                Text("$leftLabel ${fmt(leftValue)} · ${(leftValue * 100 / total)}%", fontSize = T.Micro, color = AppColors.TextSecondary, maxLines = 1, softWrap = false)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clip(RoundedCornerShape(6.dp))
                    .let { m -> if (onRightClick != null) m.clickable { onRightClick() } else m }
                    .padding(vertical = 3.dp, horizontal = 2.dp)
            ) {
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
@OptIn(ExperimentalFoundationApi::class)
fun InsightsView(viewModel: RelateViewModel) {
    val records by viewModel.interactions.collectAsState()
    val aliases by viewModel.aliases.collectAsState()
    val categoriesMap by viewModel.categories.collectAsState()
    val availableCats by viewModel.availableCategories.collectAsState()
    val filter by viewModel.timeFilter.collectAsState()
    val weekStartDay by viewModel.weekStartDay.collectAsState()
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    // 圖表點下去要看得到「是哪些人」，不能只給一個總數
    var drill by remember { mutableStateOf<Pair<String, List<SenderStat>>?>(null) }
    val wordFreq by viewModel.wordFreq.collectAsState()
    val wordSenders by viewModel.wordSenders.collectAsState()
    val secOrder by viewModel.sectionOrder.collectAsState()
    val secHidden by viewModel.hiddenSections.collectAsState()
    var showSectionEditor by remember { mutableStateOf(false) }
    // 「新出現的人」點下去就能改標籤，不用記住名字再切去聯絡人頁找
    var tagTarget by remember { mutableStateOf<String?>(null) }
    // 依存檔順序排好的完整區塊清單；沒排到的接在後面
    val orderedMeta = remember(secOrder) {
        val byId = INSIGHT_SECTIONS.associateBy { it.first }
        secOrder.mapNotNull { byId[it] } + INSIGHT_SECTIONS.filter { it.first !in secOrder }
    }

    /** 從目前篩選範圍內挑出符合條件的紀錄，算成排行後開對話框 */
    fun drillInto(title: String, predicate: (InteractionRecord) -> Boolean) {
        val rs = viewModel.getFilteredRecords(records, filter).filter(predicate)
        drill = title to viewModel.getDynamicLeaderboard(rs, categoriesMap, aliases, availableCats)
    }

    // 十幾項統計一次算完並丟到背景執行緒；同步做會直接凍住畫面
    val b by produceState(
        viewModel.cachedInsights,
        records, filter, weekStartDay, categoriesMap, aliases, availableCats
    ) {
        delay(300)
        // ⚠️ 不要在這裡把 value 標成 ready = false。
        //    那會讓畫面上的內容整個消失換成轉圈圈，捲動位置跟著歸零 ——
        //    每來一則通知、每切一次日期都跳回最上面，就是這一行造成的。
        //    舊資料先留著，算完再換掉；只有「從來沒算過」時才會是 ready = false。
        val next = withContext(Dispatchers.Default) {
            runCatching {
                viewModel.buildInsights(records, filter, weekStartDay, categoriesMap, aliases, availableCats)
            }
        }
        next.getOrNull()?.let { value = it }
            ?: next.exceptionOrNull()?.let { viewModel.recordError("buildInsights", it) }
    }

    drill?.let { (title, stats) ->
        OverviewDetailDialog(title = title, stats = stats) { drill = null }
    }

    if (showSectionEditor) {
        SectionOrderDialog(
            sections = orderedMeta,
            hidden = secHidden,
            onMove = { id, delta ->
                val cur = orderedMeta.map { it.first }.toMutableList()
                val i = cur.indexOf(id)
                val j = i + delta
                if (i >= 0 && j in cur.indices) {
                    cur[i] = cur[j].also { cur[j] = cur[i] }
                    viewModel.setSectionOrder(cur)
                }
            },
            onToggle = { viewModel.toggleSectionHidden(it) },
            onReset = { viewModel.resetSections() },
            onDismiss = { showSectionEditor = false }
        )
    }

    tagTarget?.let { t ->
        QuickTagDialog(
            name = t,
            categories = availableCats,
            currentId = categoriesMap[t],
            onPick = { catId -> viewModel.setCategory(t, catId); tagTarget = null },
            onDismiss = { tagTarget = null }
        )
    }

    Box(modifier = Modifier.fillMaxSize()
        // 左右滑動換上一期／下一期，跟日期列上的 ‹ › 同一件事
        .dateSwipe(filter, viewModel::setTimeFilter)) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(D.Gutter),
            verticalArrangement = Arrangement.spacedBy(D.Gap)
        ) {
            item { ServiceAlertBanner() }
            // 排序入口以前埋在整頁最下面（要滑到底才看得到），
            // 後來拉上來自己站一列，但整列只有右邊一顆按鈕，白白吃掉一個列高。
            // 現在掛在日期列右邊，不佔任何額外垂直空間。
            stickyHeader {
                StickyDateBar(filter, viewModel::setTimeFilter, weekStartDay) {
                    Row(
                        modifier = Modifier.fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppColors.Surface)
                            .border(1.dp, AppColors.BorderLight, RoundedCornerShape(10.dp))
                            .clickable { showSectionEditor = true }
                            .padding(horizontal = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("☰", fontSize = T.Label, color = AppColors.TextSecondary, maxLines = 1, softWrap = false)
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("排序", fontSize = T.Label, fontWeight = FontWeight.Bold,
                            color = AppColors.TextSecondary, maxLines = 1, softWrap = false)
                    }
                }
            }

            // ⚠️ 只有「這輩子第一次算」才整頁轉圈。
            //    重算期間照樣顯示上一份結果，使用者才不會每切一次日期就看到空白頁。
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

            // ⚠️ 這些區塊不是直接 item{} 出去，而是先收集成清單，
            //    再依使用者存的順序輸出，並跳過被隱藏的。
            //    收集與輸出都在同一個 content lambda 裡，收集在前、輸出在後，
            //    所以順序是對的；容器宣告在 lambda 內，每次重組都是新的，不會累加。
            val sections = ArrayList<Sec>()

            // ── 1. 與上期比較 ──
            b.deltas?.let { (dur, msg, ppl) ->
                sections.add(Sec("delta", "與上期比較") {
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
                })
            }

            // ── 2. 逐日趨勢 ──
            // ⚠️ 只有一天的時候不要畫。一根長條會撐滿整張圖（寬度平分、高度必然是 100%），
            //    看起來就是一大塊色塊，而且「趨勢」本來就需要至少兩個點才成立。
            //    單日想看細節，下面的「幾點在聊」與「時段分佈」才是對的圖。
            if (b.trend.size >= 2) {
                sections.add(Sec("trend", "逐日趨勢") {
                    val (mode, setMode) = remember { mutableStateOf("M") }
                    Panel {
                        SectionHead("逐日趨勢") {
                            SegBar(listOf("M" to "訊息", "D" to "時長"), mode, setMode)
                        }
                        TrendChart(b.trend, mode == "D")
                    }
                })
            }

            // ── 3. 平均與極值 ──
            sections.add(Sec("avg", "每日平均") {
                Panel {
                    SectionHead("每日平均")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatPair("平均每日訊息", "${b.extras.avgMsgPerActiveDay} 則", "以有活動的日子計", Modifier.weight(1f))
                        StatPair("平均每日停留", formatDuration(b.extras.avgDurPerActiveDay), null, Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatPair("最忙的一天", b.extras.busiestDayLabel, "${b.extras.busiestDayCount} 則", Modifier.weight(1f)) {
                            val day = b.extras.busiestDayStart
                            drillInto("${b.extras.busiestDayLabel} 的互動對象") { r ->
                                startOfDayMs(r.timestamp) == day
                            }
                        }
                        StatPair("最長單次停留", formatDuration(b.extras.longestSessionSec), null, Modifier.weight(1f))
                    }
                }
            })

            // ── 4. 哪幾天在聊（日曆格子圖）──
            sections.add(Sec("contrib", "哪幾天在聊") {
                Panel {
                    SectionHead("哪幾天在聊") {
                        InfoDot(
                            "哪幾天在聊",
                            "一格＝一天。橫向由左到右**一欄就是一週**，縱向由上到下是星期幾。\n" +
                                "顏色越深表示那天互動越多，點任一格會顯示當天的實際次數與聊天人數。\n\n" +
                                "跟「幾點在聊」的差別：\n" +
                                "這張看的是**日曆** —— 最近幾個月裡，哪幾天熱、哪幾天整片空白。\n" +
                                "那張看的是**時鐘** —— 一天二十四小時裡，哪幾個鐘頭熱。\n\n" +
                                "整片偏白代表那段時間幾乎沒在用通訊軟體，或服務被關掉沒記錄到。"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("近 17 週", fontSize = T.Label, color = AppColors.TextSecondary, maxLines = 1, softWrap = false)
                    }
                    ContributionGrid(b.contrib, weekStartDay)
                }
            })

            // ── 5. 活躍天數 ──
            sections.add(Sec("timeofday", "時段分佈") {
                TimeOfDayCard(b.timeStats) { title, pred -> drillInto(title, pred) }
            })

            sections.add(Sec("heatmap", "幾點在聊") {
                WeekHourHeatmapCard(b.heatmap, weekStartDay)
            })

            sections.add(Sec("words", "常用詞") {
                TopWordsCard(wordFreq, wordSenders)
            })

            // 「活躍天數」區塊已拿掉（v3.24）：多數人天天都在聊，
            // 覆蓋率幾乎永遠接近 100%，看不出任何東西。

            // ── 6. 社交集中度 ──
            sections.add(Sec("conc", "社交集中度") {
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
            })

            // ── 7. 訊息 vs 停留（一體兩面） ──

            // ── 8. 平日 vs 週末 ──
            sections.add(Sec("weekday", "平日 vs 週末") {
                Panel {
                    SectionHead("平日 vs 週末")
                    SplitBar(
                        "平日", b.extras.weekdayMsgs.toLong(), "週末", b.extras.weekendMsgs.toLong(),
                        AppColors.ActiveBlue, AppColors.WorkText,
                        onLeftClick = { drillInto("平日的互動對象") { r -> !isWeekend(r.timestamp) } },
                        onRightClick = { drillInto("週末的互動對象") { r -> isWeekend(r.timestamp) } }
                    ) { "$it 則" }
                }
            })

            // ── 9. 群組 vs 個人 ──

            // ── 10. 深夜社交 ──
            sections.add(Sec("night", "深夜社交") {
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
            })

            // ── 11. 通話統計 ──
            if (b.calls.total > 0) {
            }

            // ── 12. 訊息長度分佈 ──
            if (b.lengths.isNotEmpty()) {
            }

            // ── 13. 連續互動排行 ──
            if (b.streaks.isNotEmpty()) {
                sections.add(Sec("streaks", "連續互動排行") {
                    Panel {
                        SectionHead("連續互動天數") {
                            // 連續天數本質上是長期指標，切到單日看「這天的連續天數」沒有意義
                            ScopeTag("全期間 · 到今天為止")
                        }
                        val maxS = b.streaks.first().second.toFloat().coerceAtLeast(1f)
                        b.streaks.forEach { (name, days) ->
                            DataRow(name = name, dotColor = AppColors.Positive, progress = days / maxS, primary = "$days 天")
                        }
                    }
                })
            }

            // ── 14. 跨平台聯絡 ──
            if (b.cross.isNotEmpty()) {
                sections.add(Sec("cross", "跨平台聯絡") {
                    Panel {
                        SectionHead("跨平台聯絡") {
                            Text("同時用多個 App", fontSize = T.Label, color = AppColors.TextSecondary, maxLines = 1, softWrap = false)
                        }
                        b.cross.forEach { (name, n) ->
                            DataRow(name = name, dotColor = AppColors.FamilyText, progress = n / 4f, primary = "$n 個平台")
                        }
                    }
                })
            }

            // ── 15. 社交人格 ──
            sections.add(Sec("persona", "社交人格") {
                val persona = b.persona
                var showAllPersona by remember { mutableStateOf(false) }
                if (showAllPersona) {
                    PersonaListDialog(current = persona) { showAllPersona = false }
                }
                Panel {
                    SectionHead("社交人格") {
                        InfoDot(
                            "社交人格怎麼判定",
                            "依據本期的四個比例算出來：訊息量、停留時間、平均訊息長度、對象集中程度。\n\n" +
                                "換一個日期範圍就可能換一個人格 —— 它描述的是「你在這段期間的樣子」，不是固定標籤。"
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .clickable { showAllPersona = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(persona.emoji, fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(persona.title, fontSize = T.Title, fontWeight = FontWeight.Black, color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(persona.description.substringBefore("\n"), fontSize = T.Label, color = AppColors.TextSecondary, lineHeight = 17.sp)
                        }
                        Text("›", fontSize = 17.sp, color = AppColors.TextTertiary, maxLines = 1, softWrap = false)
                    }
                }
            })

            // ── 16. 關係變化 ──
            if (b.churn.newcomers.isNotEmpty()) {
                sections.add(Sec("churn", "新出現的人") {
                    var showAllNew by remember { mutableStateOf(false) }
                    Panel {
                        // 「已沉寂」拿掉了：那份名單動輒數十人、只是提醒你很久沒聯絡誰，
                        // 對「這段期間發生什麼」沒有幫助，反而把卡片撐得又長又亂。
                        SectionHead("新出現的人") {
                            Text("${b.churn.newcomers.size} 位", fontSize = T.Label, fontWeight = FontWeight.Bold,
                                color = AppColors.ActiveBlue, maxLines = 1, softWrap = false)
                            Spacer(modifier = Modifier.width(4.dp))
                            InfoDot(
                                "怎麼算「新出現」",
                                "本期第一次出現在紀錄裡的人。\n\n" +
                                    "判斷「新不新」必須回頭看完整歷史 —— 一個人是不是本期才第一次出現，" +
                                    "只有看過去所有紀錄才知道；但列出來的範圍跟著上方的日期走。"
                            )
                        }
                        // 新出現的人最常要做的動作就是「分個標籤」——
                        // 以前點下去只能看互動明細，還得記住名字再切到聯絡人頁去找，
                        // 所以現在膠囊上直接標出目前的標籤，點一下改標籤，長按才看明細。
                        (if (showAllNew) b.churn.newcomers else b.churn.newcomers.take(12)).chunked(2).forEach { pairRow ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                pairRow.forEach { nm ->
                                    val catId = categoriesMap[nm] ?: ContactCategory.OTHER.id
                                    val cat = availableCats.find { it.id == catId } ?: ContactCategory.OTHER
                                    Row(
                                        modifier = Modifier.weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(AppColors.ActiveBlue.copy(alpha = 0.07f))
                                            .combinedClickable(
                                                onClick = { tagTarget = nm },
                                                onLongClick = {
                                                    drillInto("$nm 的互動") { r -> (aliases[r.rawSenderName] ?: r.rawSenderName) == nm }
                                                }
                                            )
                                            .padding(start = 10.dp, end = 6.dp, top = 7.dp, bottom = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(nm, fontSize = T.Label, fontWeight = FontWeight.Medium,
                                            color = AppColors.TextPrimary, maxLines = 1,
                                            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(cat.label, fontSize = T.Micro, fontWeight = FontWeight.Bold,
                                            color = cat.color, maxLines = 1, softWrap = false,
                                            modifier = Modifier.tagChip(cat, 5.dp)
                                                .padding(horizontal = 5.dp, vertical = 1.dp))
                                    }
                                }
                                repeat(2 - pairRow.size) { Spacer(modifier = Modifier.weight(1f)) }
                            }
                        }
                        Text("點膠囊改標籤　·　長按看互動明細", fontSize = T.Micro,
                            color = AppColors.TextTertiary, modifier = Modifier.padding(top = 6.dp))
                        if (b.churn.newcomers.size > 12) {
                            Text(
                                if (showAllNew) "收合" else "＋其他 ${b.churn.newcomers.size - 12} 位",
                                fontSize = T.Label, fontWeight = FontWeight.Bold, color = AppColors.ActiveBlue,
                                maxLines = 1, softWrap = false,
                                modifier = Modifier.padding(top = 4.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { showAllNew = !showAllNew }
                                    .padding(horizontal = 4.dp, vertical = 5.dp)
                            )
                        }
                    }
                })
            }

            // 依存檔順序排；沒排到的（新版新增的區塊）接在後面
            val byId = sections.associateBy { it.id }
            val ordered = (secOrder.mapNotNull { byId[it] } + sections.filter { it.id !in secOrder })
            ordered.filter { it.id !in secHidden }.forEach { sc -> item(key = sc.id) { sc.body() } }


        }
        ScrollTopButton(listState)
    }
}

// ══════════════════════════════════════════
//  總覽（儀錶板）
// ══════════════════════════════════════════

@Composable
@OptIn(ExperimentalFoundationApi::class)
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
        viewModel.cachedOverview,
        records, filter, weekStartDay, categoriesMap, aliases, availableCats
    ) {
        // 先等一下再算：新訊息會讓 records 一直換新實例，
        // 不做合併的話每則訊息都會觸發一次全表重算，直接把 CPU 吃滿。
        delay(300)
        // ⚠️ 不要在這裡把 value 標成 ready = false。
        //    那會讓畫面上的內容整個消失換成轉圈圈，捲動位置跟著歸零 ——
        //    每來一則通知、每切一次日期都跳回最上面，就是這一行造成的。
        //    舊資料先留著，算完再換掉；只有「從來沒算過」時才會是 ready = false。
        // ⚠️ 統計丟例外不能讓整個 App 掛掉。接住、記下堆疊、保留上一份資料，
        //    使用者才有機會把原因回報出來（設定頁最上方會出現紅色區塊）。
        val next = withContext(Dispatchers.Default) {
            runCatching {
                viewModel.buildOverview(records, filter, weekStartDay, categoriesMap, aliases, availableCats)
            }
        }
        next.getOrNull()?.let { value = it }
            ?: next.exceptionOrNull()?.let { viewModel.recordError("buildOverview", it) }
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

    // 構成卡片共用的下鑽：挑出符合條件的紀錄，算成排行後開對話框
    val overviewDrill: (String, (InteractionRecord) -> Boolean) -> Unit = { title, pred ->
        val rs = filteredRecords.filter(pred)
        setActiveDetailDialog(title to viewModel.getDynamicLeaderboard(rs, categoriesMap, aliases, availableCats))
    }
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

    Box(modifier = Modifier.fillMaxSize()
        // 左右滑動換上一期／下一期，跟日期列上的 ‹ › 同一件事
        .dateSwipe(filter, viewModel::setTimeFilter)) {
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(D.Gutter),
        verticalArrangement = Arrangement.spacedBy(D.Gap)
    ) {
        item { ServiceAlertBanner() }
        stickyHeader { StickyDateBar(filter, viewModel::setTimeFilter, weekStartDay) }
        // 兩頁的分工要寫出來：使用者問過「洞察跟總覽的用意差在哪」
        item {

        }

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
                    KpiTile("接收訊息", "$totalNotifs 則", "來自 ${leaderboard.size} 位對象", AppColors.Positive, Modifier.weight(1f)) {
                        setActiveDetailDialog("依訊息數排序" to leaderboard.sortedByDescending { it.msgCount })
                    }
                    KpiTile("停留時間", formatDuration(totalTimeSec), if (screenPct > 0) "佔手機用量 $screenPct%" else "開啟聊天室的時間", AppColors.ActiveBlue, Modifier.weight(1f)) {
                        setActiveDetailDialog("依停留時間排序" to leaderboard.sortedByDescending { it.totalDurationSeconds })
                    }
                }
                Row(modifier = Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(D.Gap)) {
                    val topByMsg = leaderboard.maxByOrNull { it.msgCount }
                    KpiTile("訊息最多", topByMsg?.unifiedName ?: "—", topByMsg?.let { "${it.msgCount} 則" } ?: "尚無資料",
                        AppColors.FamilyText, Modifier.weight(1f)) {
                        topByMsg?.let { overviewDetailTarget = it }
                    }
                    val topByTime = leaderboard.maxByOrNull { it.totalDurationSeconds }
                    KpiTile("停留最久", topByTime?.unifiedName ?: "—", topByTime?.let { formatDuration(it.totalDurationSeconds) } ?: "尚無資料",
                        AppColors.WorkText, Modifier.weight(1f)) {
                        topByTime?.let { overviewDetailTarget = it }
                    }
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
                        // ⚠️ 平台圓餅改吃 ChartPalette 而不是各家品牌色。
                        //    品牌色（LINE 綠、IG 粉）彼此明度差很大，湊成一張圖本來就不成套；
                        //    而且使用者要挑的就是這張圖的顏色。平台名稱寫在圖例上，不靠顏色辨認。
                        val slices = appStats
                            .map { st ->
                                st.appName to (if (mode == "TIME") st.totalDurationSeconds else st.incomingCount.toLong())
                            }
                            .filter { it.second > 0 }.sortedByDescending { it.second }
                            .mapIndexed { i, (name, v) -> Triple(name, v, AppColors.Chart.at(i)) }
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
                    if (!pie) appStats.forEachIndexed { i, stat ->
                        val v = if (mode == "TIME") stat.totalDurationSeconds.toFloat() else stat.incomingCount.toFloat()
                        DataRow(
                            name = stat.appName,
                            dotColor = AppColors.Chart.at(i),
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
                // ⚠️ 三個模式共用一個字串，不要用 boolean —— 加第三個選項時 boolean 會整段要重寫。
                var catMode by rememberSaveable { mutableStateOf("M") }
                var catPie by rememberSaveable { mutableStateOf(true) }
                Panel {
                    SectionHead("標籤分佈") {
                        SegBar(listOf("M" to "訊息", "T" to "時長", "P" to "人數"), catMode) { catMode = it }
                        Spacer(modifier = Modifier.width(4.dp))
                        SegBar(listOf("P" to "圓餅", "L" to "清單"), if (catPie) "P" else "L") { catPie = (it == "P") }
                    }
                    val showTime = catMode == "T"
                    val showPeople = catMode == "P"
                    fun valueOf(st: CategoryStat): Long = when {
                        showTime -> st.totalDurationSeconds
                        showPeople -> st.peopleCount.toLong()
                        else -> st.incomingCount.toLong()
                    }
                    fun textOf(v: Long): String = when {
                        showTime -> formatDuration(v)
                        showPeople -> "$v 位"
                        else -> "$v 則"
                    }
                    val maxV = catStats.maxOfOrNull { valueOf(it).toFloat() }?.coerceAtLeast(1f) ?: 1f
                    val sorted = catStats.sortedByDescending { valueOf(it) }
                    if (catPie) {
                        val slices = sorted.map { st ->
                            Triple(st.category.label, valueOf(st), st.category.color)
                        }.filter { it.second > 0 }
                        if (slices.isNotEmpty()) {
                            DonutChart(
                                slices = slices,
                                centerLabel = when {
                                    showTime -> "總停留"
                                    showPeople -> "總人數"
                                    else -> "總訊息"
                                },
                                centerValue = if (showTime) formatDuration(slices.sumOf { it.second })
                                else slices.sumOf { it.second }.toString(),
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
                        DataRow(
                            name = stat.category.label,
                            dotColor = stat.category.color,
                            progress = valueOf(stat).toFloat() / maxV,
                            primary = textOf(valueOf(stat)),
                            // 次要欄位固定顯示人數 —— 就算主指標在看訊息數，
                            // 「這個標籤有幾個人」也是一直想知道的事
                            secondary = if (showPeople) "${stat.incomingCount} 則" else "${stat.peopleCount} 位",
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

        // ── 構成：這段期間裡誰／什麼佔多少 ──
        item { MsgVsTimeCard(bundle.extras.weekdayMsgs + bundle.extras.weekendMsgs, bundle.extras.openedChats, overviewDrill) }
        item { GroupSplitCard(bundle.split, overviewDrill) }
        if (bundle.lengths.isNotEmpty()) {
            item { LengthsCard(bundle.lengths, bundle.extras.avgWordsPerMsg, overviewDrill) }
        }
        if (bundle.calls.total > 0) {
            item { CallsCard(bundle.calls, overviewDrill) }
        }

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
fun WeekHourHeatmapCard(grid: Array<IntArray>, weekStartDay: Int = Calendar.MONDAY) {
    val max = (grid.maxOfOrNull { row -> row.maxOrNull() ?: 0 } ?: 0).coerceAtLeast(1)
    // grid 的索引固定是 0=週日（資料層），顯示時才依設定旋轉
    val days = remember(weekStartDay) { rotatedWeekLabels(weekStartDay) }
    val rowOrder = remember(weekStartDay) { (0..6).map { (it + (weekStartDay - Calendar.SUNDAY)) % 7 } }
    val total = grid.sumOf { row -> row.sum() }
    val cellColor = AppColors.Chart.primary
    val emptyColor = AppColors.BorderLight.copy(alpha = 0.5f)
    val markColor = AppColors.BrandPrimary

    // 尖峰時段：這張圖最想回答的就是「我都幾點在聊」，
    // 沒選格子時就把答案直接寫出來，不用等使用者自己找最深的那一格。
    val peak = remember(grid) {
        var bd = -1; var bh = -1; var bv = 0
        for (d in 0..6) for (h in 0..23) if (grid[d][h] > bv) { bv = grid[d][h]; bd = d; bh = h }
        if (bd < 0) null else Triple(bd, bh, bv)
    }

    Panel {
        SectionHead("幾點在聊") {
            InfoDot(
                "幾點在聊",
                "一格＝某個星期幾的某一個小時。橫向是一天二十四小時（最左 0 點、最右 23 點），" +
                    "縱向是星期幾，顏色越深表示那個時段互動越多。\n" +
                    "點任一格會顯示該時段的實際次數。\n\n" +
                    "跟「哪幾天在聊」的差別：\n" +
                    "那張看的是**日曆** —— 最近幾個月裡哪幾天熱。\n" +
                    "這張看的是**時鐘** —— 不管哪一天，一天當中哪幾個鐘頭熱。"
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("共 $total 次", fontSize = T.Label, color = AppColors.TextSecondary, maxLines = 1, softWrap = false)
        }
        run {
            if (total == 0) {
                Text("這段期間沒有互動紀錄", fontSize = T.Label, color = AppColors.TextSecondary)
                return@Panel
            }
            // 選中的格子（資料列索引, 小時）
            var picked by remember { mutableStateOf<Pair<Int, Int>?>(null) }
            Box(modifier = Modifier.fillMaxWidth().heightIn(min = 24.dp), contentAlignment = Alignment.CenterStart) {
                val p = picked
                if (p != null) {
                    val displayIdx = rowOrder.indexOf(p.first).coerceAtLeast(0)
                    Text(
                        days[displayIdx] + " " + String.format(Locale.US, "%02d:00", p.second) +
                            " · " + grid[p.first][p.second] + " 次",
                        fontSize = T.Label, fontWeight = FontWeight.Bold, color = AppColors.Chart.primary, maxLines = 1, softWrap = false
                    )
                } else if (peak != null) {
                    val (pd, ph, pv) = peak
                    val displayIdx = rowOrder.indexOf(pd).coerceAtLeast(0)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("最常互動", fontSize = T.Micro, color = AppColors.TextTertiary,
                            maxLines = 1, softWrap = false)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            days[displayIdx] + " " + String.format(Locale.US, "%02d:00", ph) + " · $pv 次",
                            fontSize = T.Label, fontWeight = FontWeight.Bold,
                            color = AppColors.Chart.primary, maxLines = 1, softWrap = false
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            // ⚠️ 星期標籤與「小時」都是 sp 的字，裝在寫死 dp 的框裡 ——
            //    使用者把系統字體放大之後字比框大，兩邊都被切掉。
            //    格高同時也是 Canvas 的列高，所以兩者必須用同一個數字，
            //    直接讓它跟著 fontScale 長大。
            val fs = LocalDensity.current.fontScale.coerceIn(1f, 2f)
            val labelW = (23 * fs).dp
            val rowH = (15 * fs).dp
            val measurer = rememberTextMeasurer()
            val labelStyle = TextStyle(fontSize = T.Micro, color = AppColors.TextTertiary)
            // ⚠️ 星期標籤直接畫在同一張 Canvas 裡，不要再拆成「左邊一欄 Box ＋ 右邊一張 Canvas」。
            //    兩邊各自把 dp 換成 px 再四捨五入，七列累積下來就差好幾個像素，
            //    字體放大後更明顯 —— 看起來就是星期幾對不齊格子。
            //    畫在同一張 Canvas 裡，標籤和格子共用同一個列高 ch，不可能對不齊。
            // ⚠️ 這裡以前是 7×24＝168 個各自帶 clickable 的 Box（近千個 modifier 節點，捲動會卡），
            //    所以格子一直是一張 Canvas ＋ 一個 pointerInput 做命中測試。
            Canvas(
                modifier = Modifier.fillMaxWidth().height(rowH * 7)
                    .pointerInput(rowOrder, labelW) {
                        fun cellAt(offset: Offset): Pair<Int, Int>? {
                            val lw = labelW.toPx()
                            if (offset.x < lw) return null
                            val cw = (size.width - lw) / 24f
                            val ch = size.height / 7f
                            val h = ((offset.x - lw) / cw).toInt().coerceIn(0, 23)
                            val rowIdx = (offset.y / ch).toInt().coerceIn(0, 6)
                            return rowOrder[rowIdx] to h
                        }
                        // 長按與點擊同效果；長按時直接選取，不做「再按一次取消」
                        detectTapGestures(
                            onLongPress = { o -> cellAt(o)?.let { picked = it } },
                            onTap = { o -> cellAt(o)?.let { c -> picked = if (picked == c) null else c } }
                        )
                    }
            ) {
                val lw = labelW.toPx()
                val cw = (size.width - lw) / 24f
                val ch = size.height / 7f
                val pad = 1f
                days.forEachIndexed { rowIdx, label ->
                    val layout = measurer.measure(label, labelStyle)
                    drawText(layout, topLeft = Offset(0f, rowIdx * ch + (ch - layout.size.height) / 2f))
                }
                rowOrder.forEachIndexed { rowIdx, d ->
                    for (h in 0..23) {
                        val v = grid[d][h]
                        val color = if (v == 0) emptyColor
                        else cellColor.copy(alpha = 0.2f + 0.8f * (v.toFloat() / max))
                        val tl = Offset(lw + h * cw + pad, rowIdx * ch + pad)
                        val sz = Size(cw - pad * 2, ch - pad * 2)
                        drawRoundRect(color = color, topLeft = tl, size = sz, cornerRadius = CornerRadius(3f, 3f))
                        if (picked?.first == d && picked?.second == h) {
                            drawRoundRect(color = markColor, topLeft = tl, size = sz,
                                cornerRadius = CornerRadius(3f, 3f), style = Stroke(width = 3f))
                        }
                    }
                }
            }
            // 橫軸只寫 0 6 12 18 23 五個數字，第一次看不出那是小時 ——
            // 左邊的星期標籤欄本來就是空的，把「小時」兩個字放進去。
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(labelW)) {
                    Text("時", fontSize = T.Micro, color = AppColors.TextTertiary, maxLines = 1, softWrap = false)
                }
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("0", "6", "12", "18", "23").forEach {
                        Text(it, fontSize = T.Micro, color = AppColors.TextTertiary)
                    }
                }
            }
            // 色階圖例：跟「哪幾天在聊」那張同一個寫法，兩張圖才讀得起來像同一套
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("左邊是星期　·　橫向是 0–23 時", fontSize = T.Micro, color = AppColors.TextTertiary, maxLines = 1, softWrap = false)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("少", fontSize = T.Micro, color = AppColors.TextTertiary)
                    Spacer(modifier = Modifier.width(4.dp))
                    listOf(0f, 0.3f, 0.55f, 0.8f, 1f).forEach { a ->
                        Box(modifier = Modifier.size(9.dp).background(
                            if (a == 0f) emptyColor else cellColor.copy(alpha = 0.2f + 0.8f * a),
                            RoundedCornerShape(2.dp)
                        ))
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("多（最高 $max）", fontSize = T.Micro, color = AppColors.TextTertiary, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

/** 常用詞（只累計詞頻，不保留訊息原文） */
@Composable
fun TopWordsCard(wordFreq: Map<String, Int>, wordSenders: Map<String, Map<String, Int>> = emptyMap()) {
    // 預設看 3 字：2 字組合太容易切出「送了」「了貼」這種跨詞邊界的碎片
    var mode by remember { mutableStateOf("3") }
    var picked by remember { mutableStateOf<Pair<String, Int>?>(null) }
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
            // 只存詞頻不存原文，事後無法重新切詞，所以只能是全期間累計。
            // 不跟隨日期篩選就一定要標明，否則使用者會以為數字算錯。
            ScopeTag("全期間")
            Spacer(modifier = Modifier.width(4.dp))
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
            picked?.let { (w, c) ->
                val share = (c * 100f / top.sumOf { it.second }.coerceAtLeast(1)).let { "%.1f".format(it) }
                val senders = wordSenders[w].orEmpty().entries.sortedByDescending { it.value }
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                        .background(AppColors.SurfaceAlt, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("「$w」", fontSize = T.Body, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary, maxLines = 1, softWrap = false)
                        Text("$c 次 · 佔榜上 $share%", fontSize = T.Label, fontWeight = FontWeight.Bold, color = AppColors.Chart.primary, maxLines = 1, softWrap = false)
                    }
                    // 誰傳的。⚠️ 只有 v3.20 之後收到的訊息才有這份資料 ——
                    //    過去的訊息沒有留內容，發話者回推不出來，所以舊詞會是空的。
                    if (senders.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        senders.forEach { (name, cnt) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(name, fontSize = T.Label, color = AppColors.TextPrimary,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                Text("$cnt 則", fontSize = T.Micro, fontWeight = FontWeight.Bold,
                                    color = AppColors.TextSecondary, maxLines = 1, softWrap = false)
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("這個詞還沒有發話者紀錄（只記錄 v3.20 之後收到的訊息）",
                            fontSize = T.Micro, color = AppColors.TextTertiary, lineHeight = 14.sp)
                    }
                }
            }
            top.chunked(3).forEach { rowWords ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    rowWords.forEach { (word, count) ->
                        val ratio = count.toFloat() / maxCount
                        val rank = top.indexOfFirst { it.first == word } + 1
                        Column(
                            modifier = Modifier.weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AppColors.Chart.primary.copy(alpha = 0.08f + 0.22f * ratio))
                                .clickable { picked = if (picked?.first == word) null else (word to count) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(word, fontSize = T.Body, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("$count 次 · 第 $rank", fontSize = T.Micro, color = AppColors.TextTertiary, maxLines = 1, softWrap = false)
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
    // ⚠️ 這個彈窗有五、六個小標與十幾列，每列多 2dp 就是整整多一段。
    //    間距一律走小數值，密度靠字重與顏色撐，不要靠留白。
    Text(text, fontSize = T.Label, fontWeight = FontWeight.Black, color = AppColors.TextSecondary,
        modifier = Modifier.padding(top = 10.dp, bottom = 3.dp), maxLines = 1, softWrap = false)
}

/** 詳情頁的一列「標題 — 值」 */
@Composable
private fun DetailLine(label: String, value: String, accent: Color = AppColors.TextPrimary) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
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
                    Box(modifier = Modifier.tagChip(stat.category, 6.dp).padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text(stat.category.label, color = stat.category.color, fontSize = T.Label, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                    if (stat.isGroup) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(modifier = Modifier.background(AppColors.GroupBg, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 3.dp)) {
                            Text("群組", color = AppColors.GroupText, fontSize = T.Label, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(stat.unifiedName, fontWeight = FontWeight.Black, fontSize = 18.sp, lineHeight = 22.sp,
                    color = AppColors.TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (insight.ready && insight.rank > 0) {
                    Text(
                        "本期第 " + insight.rank + " 名／共 " + insight.totalContacts + " 位 · 佔你全部訊息 " + insight.sharePct + "%",
                        fontSize = T.Label, color = AppColors.ActiveBlue, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                if (!insight.ready) {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = AppColors.ActiveBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("計算中…", fontSize = T.Body, color = AppColors.TextSecondary)
                    }
                    return@Column
                }

                if (insight.missedCalls > 0) {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).background(AppColors.WorkBg, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Warning, contentDescription = null, tint = AppColors.WorkText, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("本期有 " + insight.missedCalls + " 通未接來電", color = AppColors.WorkText, fontSize = T.Label, fontWeight = FontWeight.Bold)
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
                        if (insight.callSeconds > 0) formatDuration(insight.callSeconds) else null, Modifier.weight(1f))
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
                    // 跟其他圖表一致：點下去看得到那一週的實際數字
                    var pickedWeek by remember { mutableStateOf<Int?>(null) }
                    Box(modifier = Modifier.fillMaxWidth().heightIn(min = 20.dp), contentAlignment = Alignment.CenterStart) {
                        val pw = pickedWeek
                        if (pw != null && pw in insight.weekly.indices) {
                            val (lbl, n) = insight.weekly[pw]
                            Text("$lbl · $n 則", fontSize = T.Label, fontWeight = FontWeight.Bold,
                                color = AppColors.ActiveBlue, maxLines = 1, softWrap = false)
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth().height(72.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        insight.weekly.forEachIndexed { wi, pair ->
                            val n = pair.second
                            val on = pickedWeek == wi
                            // 整根欄位可點，矮的那幾週才點得到
                            Column(
                                modifier = Modifier.weight(1f).fillMaxHeight()
                                    .clickable { pickedWeek = if (on) null else wi },
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                Box(modifier = Modifier.fillMaxWidth().fillMaxHeight((n.toFloat() / maxW).coerceIn(0.03f, 1f))
                                    .background(
                                        if (on) AppColors.BrandPrimary else if (n > 0) AppColors.ActiveBlue else AppColors.BorderLight,
                                        RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                                    ))
                            }
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
@OptIn(ExperimentalFoundationApi::class)
fun RankRow(
    rank: Int,
    stat: SenderStat,
    maxScore: Float,
    sortType: SortType,
    onLongClick: () -> Unit = {},
    onClick: () -> Unit
) {
    val score = if (sortType == SortType.TIME) stat.totalDurationSeconds.toFloat() else (stat.msgCount + stat.callCount).toFloat()
    val progress = if (maxScore > 0f) (score / maxScore).coerceIn(0f, 1f) else 0f
    val accent = if (stat.isGroup) AppColors.GroupText else stat.category.color

    Row(
        // ⚠️ 長按要寫在這裡，不能包在外面。
        //    這一列自己就吃掉點擊事件了，外層再包 combinedClickable 收不到手勢。
        modifier = Modifier.fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$rank", fontSize = T.Label, fontWeight = FontWeight.Black,
            color = if (rank <= 3) AppColors.ActiveBlue else AppColors.TextTertiary,
            // widthIn 而非 width：名次會到三位數，字體再放大就會被切掉
            modifier = Modifier.widthIn(min = 18.dp), textAlign = TextAlign.Center, maxLines = 1, softWrap = false)

        Column(modifier = Modifier.weight(1f).padding(start = 4.dp, end = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 標籤才是使用者要看的資訊；群組只用一個小記號附註，不再蓋掉標籤
                Box(modifier = Modifier.tagChip(stat.category, 4.dp)
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
@OptIn(ExperimentalFoundationApi::class)
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

    // 多選中按返回＝取消多選，而不是離開這一頁。
    BackHandler(enabled = selectionMode) { exitSelection() }

    // ⚠️ 千萬不要在 remember{} 裡直接算排行。
    //    getLeaderboard 會掃過全部紀錄（可以到數萬筆），remember 是同步的，
    //    等於在主執行緒做全表運算；而 key 包含 records，
    //    每收到一則通知就重算一次，畫面直接凍住 → ANR。
    val lb by produceState(
        viewModel.cachedLeader,
        records, categoriesMap, aliases, availableCats, filter, sort, catFilter, appFilter, groupFilter
    ) {
        delay(300)
        // ⚠️ 不要在這裡把 value 標成 ready = false。
        //    那會讓畫面上的內容整個消失換成轉圈圈，捲動位置跟著歸零 ——
        //    每來一則通知、每切一次日期都跳回最上面，就是這一行造成的。
        //    舊資料先留著，算完再換掉；只有「從來沒算過」時才會是 ready = false。
        val next = withContext(Dispatchers.Default) {
            runCatching {
                viewModel.buildLeaderboard(records, categoriesMap, aliases, availableCats, filter, sort, catFilter, appFilter, groupFilter)
            }
        }
        next.getOrNull()?.let { value = it }
            ?: next.exceptionOrNull()?.let { viewModel.recordError("buildLeaderboard", it) }
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
    Box(modifier = Modifier.fillMaxSize()
        // 左右滑動換上一期／下一期，跟日期列上的 ‹ › 同一件事
        .dateSwipe(filter, viewModel::setTimeFilter)) {
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = D.Gutter, end = D.Gutter, top = D.Gutter, bottom = if (selectionMode) 76.dp else D.Gutter),
        modifier = Modifier.fillMaxSize()
    ) {
        stickyHeader {
            Column(modifier = Modifier.background(AppColors.Background)) {
                StickyDateBar(filter, viewModel::setTimeFilter, weekStartDay)
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
                        .then(if (on) Modifier.background(cat.color) else Modifier.tagChip(cat, 14.dp))
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
                            RankRow(
                                i + 1, stat, maxScore, sort,
                                // 長按直接進多選並勾起這一位 —— 跟聯絡人頁同一個手勢。
                                // 先按上面的「選取」再一個個勾，多了一步而且不直覺。
                                onLongClick = {
                                    if (!selectionMode) selectionMode = true
                                    if (!picked.contains(stat.unifiedName)) picked.add(stat.unifiedName)
                                }
                            ) {
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

        ListScrollbar(listState)
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

/**
 * 常用詞雜訊清理：逐個勾選。
 *
 * ⚠️ 不要做成「全部清掉」的一鍵動作。判定是靠規則與人名比對，一定會有誤判，
 *    而且清掉之後累積的次數就回不來了。預設全勾，使用者只要取消想留的那幾個。
 */
@Composable
fun WordPurgeDialog(
    words: List<Pair<String, Int>>,
    onConfirm: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val unchecked = remember { mutableStateListOf<String>() }
    var query by remember { mutableStateOf("") }
    val shown = remember(words, query) {
        if (query.isBlank()) words else words.filter { it.first.contains(query, true) }
    }
    val picked = words.map { it.first }.filter { it !in unchecked }

    AlertDialog(
        containerColor = AppColors.Surface,
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("清掉常用詞裡的雜訊", fontWeight = FontWeight.Black, fontSize = 18.sp,
                    letterSpacing = (-0.3).sp, color = AppColors.TextPrimary)
                Text("系統自動產生的字，以及被切碎的人名。取消勾選的會保留。",
                    fontSize = T.Micro, color = AppColors.TextTertiary, lineHeight = 15.sp)
            }
        },
        text = {
            Column {
                if (words.isEmpty()) {
                    Text("目前沒有偵測到需要清掉的詞。", fontSize = T.Body, color = AppColors.TextSecondary)
                    return@Column
                }
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    placeholder = { Text("搜尋詞…", fontSize = T.Label) },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = T.Body),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.BrandPrimary,
                        unfocusedBorderColor = AppColors.BorderLight
                    )
                )
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("已勾選 ${picked.size} / ${words.size}", fontSize = T.Label,
                        fontWeight = FontWeight.Bold, color = AppColors.TextSecondary,
                        modifier = Modifier.weight(1f), maxLines = 1, softWrap = false)
                    Text(if (unchecked.isEmpty()) "全部不選" else "全選",
                        fontSize = T.Label, fontWeight = FontWeight.Bold, color = AppColors.ActiveBlue,
                        maxLines = 1, softWrap = false,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp))
                            .clickable {
                                if (unchecked.isEmpty()) { unchecked.clear(); unchecked.addAll(words.map { it.first }) }
                                else unchecked.clear()
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp))
                }
                if (shown.isEmpty()) {
                    Text("找不到「$query」", fontSize = T.Body, color = AppColors.TextSecondary,
                        modifier = Modifier.padding(vertical = 16.dp))
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                        items(shown, key = { it.first }) { (w, n) ->
                            val on = w !in unchecked
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clickable { if (on) unchecked.add(w) else unchecked.remove(w) }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = on, onCheckedChange = {
                                    if (on) unchecked.add(w) else unchecked.remove(w)
                                })
                                Text(w, fontSize = T.Body, fontWeight = FontWeight.Bold,
                                    color = AppColors.TextPrimary, maxLines = 1,
                                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                Text("$n 次", fontSize = T.Micro, color = AppColors.TextTertiary,
                                    maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }
                Text("勾選的會從常用詞統計中移除，其他詞的累積不受影響。此操作無法復原。",
                    fontSize = T.Micro, color = AppColors.TextTertiary, lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 8.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(picked.toSet()) },
                enabled = picked.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.BrandPrimary),
                shape = RoundedCornerShape(10.dp)
            ) { Text("清掉 ${picked.size} 個", fontSize = T.Label, maxLines = 1, softWrap = false) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = AppColors.TextSecondary) }
        }
    )
}

/** 逐筆確認畫面上的一列 */
data class JunkEntry(
    val name: String,
    val count: Int,
    val lastTs: Long,
    val apps: Set<String>,
    val rawNames: List<String>,
    val reason: String
)

/**
 * 非真人來源逐筆確認。
 *
 * ⚠️ 為什麼一定要有這一頁：橫幅只寫「202 個非真人來源」，
 *    使用者無從判斷裡面有沒有誤判的真人，只能整批賭下去。
 *    規則會誤判（群組名稱撞到關鍵字就會中），所以要能一列一列看、一列一列取消勾選。
 *
 * ⚠️ 本 App 不保存訊息原文，所以這裡能給的「是哪則通知」就是**通知標題本身**
 *    （也就是聯絡人名稱）、平台、筆數、最近時間，以及命中的規則。
 *    不要為了做這頁去存訊息內容 —— 那會直接破壞「不讀取訊息內容」這個前提。
 */
@Composable
fun JunkReviewDialog(
    names: List<String>,
    records: List<InteractionRecord>,
    aliases: Map<String, String>,
    onApply: (List<String>) -> Unit,
    onDelete: (List<String>) -> Unit,
    /** 記住這些其實是真人，之後不再列入提醒 */
    onKeep: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val dateFmt = remember { SimpleDateFormat("M/d HH:mm", Locale.US) }
    // 預設全勾：多數情況判定是對的，使用者只要取消少數誤判。
    val unchecked = remember { mutableStateListOf<String>() }

    // ⚠️ 全表掃描，不能放在 remember{} 直接算 —— 那是主執行緒。
    val entries by produceState(emptyList<JunkEntry>(), names, records, aliases) {
        value = withContext(Dispatchers.Default) {
            val wanted = names.toHashSet()
            val counts = HashMap<String, Int>()
            val last = HashMap<String, Long>()
            val apps = HashMap<String, MutableSet<String>>()
            val raws = HashMap<String, LinkedHashSet<String>>()
            records.forEach { r ->
                val u = aliases[r.rawSenderName] ?: r.rawSenderName
                if (u in wanted) {
                    counts[u] = (counts[u] ?: 0) + 1
                    if (r.timestamp > (last[u] ?: 0L)) last[u] = r.timestamp
                    apps.getOrPut(u) { HashSet() }.add(r.appName)
                    raws.getOrPut(u) { LinkedHashSet() }.add(r.rawSenderName)
                }
            }
            names.map { n ->
                JunkEntry(
                    n, counts[n] ?: 0, last[n] ?: 0L,
                    apps[n] ?: emptySet(),
                    (raws[n] ?: LinkedHashSet()).toList().ifEmpty { listOf(n) },
                    junkReason(n)
                )
            }.sortedByDescending { it.count }
        }
    }
    val picked = names.filter { it !in unchecked }

    AlertDialog(
        containerColor = AppColors.Surface,
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("確認非真人來源", fontWeight = FontWeight.Black, fontSize = 18.sp,
                    letterSpacing = (-0.3).sp, color = AppColors.TextPrimary)
                Text("下面是通知上顯示的名稱。取消勾選代表它其實是真人。",
                    fontSize = T.Micro, color = AppColors.TextTertiary, lineHeight = 15.sp)
            }
        },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("已勾選 ${picked.size} / ${names.size}", fontSize = T.Label,
                        fontWeight = FontWeight.Bold, color = AppColors.TextSecondary,
                        modifier = Modifier.weight(1f), maxLines = 1, softWrap = false)
                    Text(if (unchecked.isEmpty()) "全部不選" else "全選",
                        fontSize = T.Label, fontWeight = FontWeight.Bold, color = AppColors.ActiveBlue,
                        maxLines = 1, softWrap = false,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp))
                            .clickable {
                                if (unchecked.isEmpty()) { unchecked.clear(); unchecked.addAll(names) }
                                else unchecked.clear()
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp))
                }
                if (entries.isEmpty()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp,
                            color = AppColors.ActiveBlue)
                    }
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        itemsIndexed(entries, key = { _, e -> e.name }) { idx, e ->
                            val on = e.name !in unchecked
                            if (idx > 0) HorizontalDivider(color = AppColors.BorderLight.copy(alpha = 0.6f))
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clickable {
                                        if (on) unchecked.add(e.name) else unchecked.remove(e.name)
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = on, onCheckedChange = {
                                    if (on) unchecked.add(e.name) else unchecked.remove(e.name)
                                })
                                Column(modifier = Modifier.weight(1f).padding(start = 2.dp)) {
                                    Text(e.name, fontSize = T.Body, fontWeight = FontWeight.Bold,
                                        color = AppColors.TextPrimary, maxLines = 2,
                                        overflow = TextOverflow.Ellipsis)
                                    Text(
                                        buildString {
                                            if (e.apps.isNotEmpty()) {
                                                append(e.apps.joinToString(" · ") { shortAppName(it) })
                                                append("　")
                                            }
                                            append("${e.count} 則")
                                            if (e.lastTs > 0L) append("　·　最近 ${dateFmt.format(Date(e.lastTs))}")
                                        },
                                        fontSize = T.Micro, color = AppColors.TextTertiary,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                    Text(e.reason, fontSize = T.Micro, color = AppColors.WorkText,
                                        lineHeight = 14.sp, modifier = Modifier.padding(top = 2.dp))
                                    // 合併過的人，通知上的原名跟顯示名稱會不一樣，要一起攤開
                                    if (e.rawNames.size > 1 || e.rawNames.firstOrNull() != e.name) {
                                        Text("通知原名：${e.rawNames.joinToString("、")}",
                                            fontSize = T.Micro, color = AppColors.TextTertiary,
                                            lineHeight = 14.sp, maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(top = 2.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                Text(
                    "標為系統通知：紀錄留著但統計不算，也不會再出現在清單上。\n" +
                        "直接刪除：對方下次推播還是會重建。\n" +
                        "取消勾選的會被記成真人，之後不再列入提醒。",
                    fontSize = T.Micro, color = AppColors.TextTertiary, lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
                // ⚠️ 一定要有「什麼都不做」這個出口。
                //    不標成系統通知、也不刪，橫幅就會每次開 App 都跳出來，
                //    使用者等於被逼著在兩個不想要的選項裡挑一個。
                Text("這些都是真人，全部保留不再提醒",
                    fontSize = T.Label, fontWeight = FontWeight.Bold, color = AppColors.ActiveBlue,
                    modifier = Modifier.padding(top = 8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onKeep(names) }
                        .padding(vertical = 4.dp))
            }
        },
        confirmButton = {
            Button(
                // 取消勾選的＝使用者判定為真人，順手記起來，下次不要再問
                onClick = { if (unchecked.isNotEmpty()) onKeep(unchecked.toList()); onApply(picked) },
                enabled = picked.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.BrandPrimary),
                shape = RoundedCornerShape(10.dp)
            ) { Text("標為系統通知 (${picked.size})", fontSize = T.Label, maxLines = 1, softWrap = false) }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = { if (unchecked.isNotEmpty()) onKeep(unchecked.toList()); onDelete(picked) },
                    enabled = picked.isNotEmpty()
                ) {
                    Text("刪除", color = AppColors.WorkText, fontSize = T.Label, maxLines = 1, softWrap = false)
                }
                TextButton(onClick = onDismiss) {
                    Text("取消", color = AppColors.TextSecondary, fontSize = T.Label, maxLines = 1, softWrap = false)
                }
            }
        }
    )
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
    // ⚠️ key 一定要含 categoriesMap：垃圾來源清單、分類篩選都吃標籤，
    //    少了它，標籤改完畫面不會更新（按「標為系統通知」像沒反應）。
    val cb by produceState(viewModel.cachedContacts, records, aliases, categoriesMap) {
        delay(300)
        // ⚠️ 不要在這裡把 value 標成 ready = false。
        //    那會讓畫面上的內容整個消失換成轉圈圈，捲動位置跟著歸零 ——
        //    每來一則通知、每切一次日期都跳回最上面，就是這一行造成的。
        //    舊資料先留著，算完再換掉；只有「從來沒算過」時才會是 ready = false。
        val next = withContext(Dispatchers.Default) { runCatching { viewModel.buildContacts(records, aliases, categoriesMap) } }
        next.getOrNull()?.let { value = it }
            ?: next.exceptionOrNull()?.let { viewModel.recordError("buildContacts", it) }
    }
    val unifiedNamesList = cb.unifiedNames
    val interactionCounts = cb.counts
    val contactApps = cb.contactApps
    val availableApps = cb.apps

    val activeCatFilterId = catFilter?.id
    // 每個標籤底下有幾位 —— 以前只能一個個切篩選自己數
    val tagCounts = remember(unifiedNamesList, categoriesMap) {
        unifiedNamesList.groupingBy { categoriesMap[it] ?: ContactCategory.OTHER.id }.eachCount()
    }
    val filteredContacts = remember(unifiedNamesList, interactionCounts, searchQuery, activeCatFilterId, appFilter, contactApps, categoriesMap, sortType) {
        unifiedNamesList.filter { name ->
            val matchSearch = searchQuery.isBlank() || name.contains(searchQuery, ignoreCase = true)
            val catId = categoriesMap[name] ?: ContactCategory.OTHER.id
            // 預設不顯示「系統通知」—— 跟排行頁的規則一致。
            // 標為系統通知就是為了讓它們消失，留在清單上等於沒處理。
            // 想看的話把標籤篩選切到「系統通知」即可。
            val matchCat = if (activeCatFilterId == null) catId != ContactCategory.SYSTEM.id
                           else catId == activeCatFilterId
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
    // 「帳號與名稱」對話框開給誰
    var accountsTarget by remember { mutableStateOf<String?>(null) }
    var longPressTarget by remember { mutableStateOf<String?>(null) }
    var renameTarget by remember { mutableStateOf<String?>(null) }
    var quickTagTarget by remember { mutableStateOf<String?>(null) }
    // 刪過的人又冒出來：不自動處理，問過使用者再決定
    var resurrected by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    LaunchedEffect(cb.ready, records.size) {
        if (cb.ready) resurrected = viewModel.resurrectedNames()
    }
    var showAutoMerge by remember { mutableStateOf(false) }
    // 收起來就不再提醒，直到重開 App
    var junkDismissed by remember { mutableStateOf(false) }
    var showJunkReview by remember { mutableStateOf(false) }
    // 「刪除並封鎖」會刪紀錄又永久擋掉，不能一按就執行
    var pendingBlock by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var pendingBlockMany by remember { mutableStateOf<List<String>?>(null) }
    // 預設全勾：這張卡多數情況是整批想做同一件事
    val resurrectPicked = remember { mutableStateListOf<String>() }
    LaunchedEffect(resurrected) {
        resurrectPicked.clear()
        resurrectPicked.addAll(resurrected.map { it.first })
    }
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

    // 多選中按返回＝取消多選，而不是離開這一頁。
    BackHandler(enabled = selectionMode) { exitSelection() }

    // 聯絡人可能有幾百位，沒有 listState 就沒辦法做捲動條與回頂鈕。
    // rememberSaveable：切出去再切回來不要彈回最上面。
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 14.dp, bottom = if (selectionMode) 92.dp else 14.dp),
            // 0dp：聯絡人列彼此相連成一張卡，間距由各列自己的分隔線負責
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            if (resurrected.isNotEmpty()) {
                item {
                    NoticeCard(
                        title = "刪除過的對象又有新紀錄",
                        subtitle = "你刪掉他們之後對方又傳了訊息，紀錄被重新建立。",
                        onDismiss = { resurrected = emptyList() }
                    ) {
                        // ⚠️ 這張卡可能列出十幾個人，一個一個按兩下太慢。
                        //    勾選 + 批次處理，而且預設全勾 —— 多數情況是整批想做同一件事。
                        val allNames = resurrected.map { it.first }
                        val allOn = resurrectPicked.size == allNames.size && allNames.isNotEmpty()
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("已選 ${resurrectPicked.size} / ${allNames.size}", fontSize = T.Micro,
                                color = AppColors.TextTertiary, modifier = Modifier.weight(1f))
                            Text(if (allOn) "取消全選" else "全選",
                                fontSize = T.Label, fontWeight = FontWeight.Bold, color = AppColors.ActiveBlue,
                                maxLines = 1, softWrap = false,
                                modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        if (allOn) resurrectPicked.clear()
                                        else { resurrectPicked.clear(); resurrectPicked.addAll(allNames) }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                        resurrected.forEach { (name, count) ->
                            val on = resurrectPicked.contains(name)
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { if (on) resurrectPicked.remove(name) else resurrectPicked.add(name) }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = on, onCheckedChange = {
                                    if (on) resurrectPicked.remove(name) else resurrectPicked.add(name)
                                })
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(name, fontSize = T.Label, fontWeight = FontWeight.Bold,
                                        color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("新增 $count 筆", fontSize = T.Micro, color = AppColors.TextTertiary)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            NoticeAction("保留 (${resurrectPicked.size})", AppColors.TextSecondary) {
                                if (resurrectPicked.isNotEmpty()) {
                                    resurrectPicked.forEach { viewModel.forgetDeleted(it) }
                                    resurrectPicked.clear()
                                    resurrected = viewModel.resurrectedNames()
                                }
                            }
                            // ⚠️ 這一顆會刪紀錄又封鎖，是不可逆的，一定要二次確認
                            NoticeAction("刪除並封鎖 (${resurrectPicked.size})", AppColors.WorkText) {
                                if (resurrectPicked.isNotEmpty()) pendingBlockMany = resurrectPicked.toList()
                            }
                        }
                    }
                }
            }

            // ⚠️ 垃圾聯絡人大多是系統通知，「刪除」沒有用 —— 對方下次再推播就會重建，
            //    使用者等於每隔幾天就要清一次。所以主要動作改成**標為系統通知**：
            //    紀錄留著、但排行與統計都不算它，而且不會再冒出來。
            //    刪除降為次要選項，整張卡也可以直接收起來不看。
            if (junkContacts.isNotEmpty() && !selectionMode && !junkDismissed) {
                item {
                    NoticeCard(
                        title = "${junkContacts.size} 個非真人來源",
                        subtitle = "讚、追蹤、推播這類來源。點開可逐筆確認，取消誤判的。",
                        onOpen = { showJunkReview = true },
                        onDismiss = { junkDismissed = true }
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            NoticeAction("逐筆確認", AppColors.BrandPrimary, filled = true) { showJunkReview = true }
                            NoticeAction("標為系統通知", AppColors.TextSecondary) {
                                viewModel.setCategories(junkContacts, ContactCategory.SYSTEM.id)
                                Toast.makeText(context, "已標為系統通知，統計不再算入", Toast.LENGTH_SHORT).show()
                            }
                        }
                        // 「直接刪除」刻意收進逐筆確認裡：刪了對方再推播就會重建，
                        // 放在最外層等於鼓勵使用者選錯的那一個。
                        Text("刪除沒有用，對方下次推播還是會重建", fontSize = T.Micro,
                            color = AppColors.TextTertiary, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }

            item {
                // ⚠️ LazyColumn 的 spacedBy 已改成 0（讓聯絡人列黏成一張卡），
                //    所以頭部這幾張卡要自己帶下方留白。
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                    .background(AppColors.Surface, RoundedCornerShape(12.dp))
                    .border(1.dp, AppColors.BorderLight, RoundedCornerShape(12.dp)).padding(12.dp)) {
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
                    }
                    // ⚠️ 標籤篩選從下拉選單改成一排膠囊，每顆直接寫人數。
                    //    下拉選單看不到數量，要知道「家人有幾位」得一個個切。
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val allOn = catFilter == null
                        // 「全部」不含系統通知，跟清單預設的顯示規則一致
                        val allCount = unifiedNamesList.size - (tagCounts[ContactCategory.SYSTEM.id] ?: 0)
                        Row(
                            modifier = Modifier.clip(RoundedCornerShape(14.dp))
                                .background(if (allOn) AppColors.BrandPrimary else AppColors.Surface)
                                .border(1.dp, if (allOn) AppColors.BrandPrimary else AppColors.BorderLight, RoundedCornerShape(14.dp))
                                .clickable { viewModel.setContactsCatFilter(null) }
                                .padding(horizontal = 11.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("全部", fontSize = T.Label, fontWeight = FontWeight.Bold,
                                color = if (allOn) Color.White else AppColors.TextSecondary, maxLines = 1, softWrap = false)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("$allCount", fontSize = T.Micro, fontWeight = FontWeight.Bold,
                                color = if (allOn) Color.White.copy(alpha = 0.85f) else AppColors.TextTertiary, maxLines = 1, softWrap = false)
                        }
                        availableCats.forEach { cat ->
                            val on = catFilter?.id == cat.id
                            val n = tagCounts[cat.id] ?: 0
                            Row(
                                modifier = Modifier.clip(RoundedCornerShape(14.dp))
                                    .then(if (on) Modifier.background(cat.color) else Modifier.tagChip(cat, 14.dp))
                                    .clickable { viewModel.setContactsCatFilter(cat) }
                                    .padding(horizontal = 11.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(cat.label, fontSize = T.Label, fontWeight = FontWeight.Bold,
                                    color = if (on) Color.White else cat.color, maxLines = 1, softWrap = false)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("$n", fontSize = T.Micro, fontWeight = FontWeight.Bold,
                                    color = if (on) Color.White.copy(alpha = 0.85f) else cat.color.copy(alpha = 0.7f),
                                    maxLines = 1, softWrap = false)
                            }
                        }
                        Text("⚙ 管理", fontSize = T.Label, fontWeight = FontWeight.Bold, color = AppColors.TextSecondary,
                            maxLines = 1, softWrap = false,
                            modifier = Modifier.clip(RoundedCornerShape(14.dp))
                                .border(1.dp, AppColors.BorderLight, RoundedCornerShape(14.dp))
                                .clickable { showCategoryManager = true }
                                .padding(horizontal = 11.dp, vertical = 5.dp))
                    }
                }
            }

            if (filteredContacts.isEmpty() && cb.ready) {
                item { Text("沒有符合的聯絡人", color = AppColors.TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) }
            }

            // ⚠️ 舊版每位聯絡人是一張獨立的圓角外框卡，幾百張堆起來全是框線；
            //    平台又用四個彩色膠囊呈現，使用者把系統字體放大後（實測約 1.8 倍）
            //    9sp 變成 16sp，四顆膠囊直接撐爆父容器，Row 不裁切所以溢出被切掉。
            //
            //    改成銀行 App 那種做法：整份清單是一張白卡，列與列之間用細分隔線，
            //    平台改成一行灰字（LINE · IG · Messenger），顏色只留給標籤。
            //    密度高、框線少、放大字體也不會爆。
            itemsIndexed(filteredContacts, key = { _, it -> it }) { idx, unifiedName ->
                val catId = categoriesMap[unifiedName] ?: ContactCategory.OTHER.id
                val currentCategory = availableCats.find { it.id == catId } ?: ContactCategory.OTHER
                val isSel = selected.contains(unifiedName)
                val rawNamesForThis = cb.rawByUnified[unifiedName] ?: listOf(unifiedName)
                val isFirst = idx == 0
                val isLast = idx == filteredContacts.lastIndex
                val shape = RoundedCornerShape(
                    topStart = if (isFirst) 12.dp else 0.dp, topEnd = if (isFirst) 12.dp else 0.dp,
                    bottomStart = if (isLast) 12.dp else 0.dp, bottomEnd = if (isLast) 12.dp else 0.dp
                )

                Column(
                    modifier = Modifier.fillMaxWidth()
                        .clip(shape)
                        .background(if (isSel) AppColors.ActiveBlue.copy(alpha = 0.06f) else AppColors.Surface)
                        // 手勢與排行頁一致：長按 = 進多選，點一下 = 開這個人的操作選單。
                        // ⚠️ 以前長按開選單、點一下什麼都不做，
                        //    結果捲到一半想批次分類時，唯一的入口（上方「選取」鈕）早就滑掉了。
                        .combinedClickable(
                            onClick = {
                                if (selectionMode) toggleSelect(unifiedName)
                                else longPressTarget = unifiedName
                            },
                            onLongClick = {
                                if (!selectionMode) selectionMode = true
                                if (!selected.contains(unifiedName)) selected.add(unifiedName)
                            }
                        )
                ) {
                    if (!isFirst) HorizontalDivider(color = AppColors.BorderLight, modifier = Modifier.padding(start = 14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectionMode) {
                            Checkbox(checked = isSel, onCheckedChange = { toggleSelect(unifiedName) })
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                            Text(unifiedName, fontWeight = FontWeight.Bold, fontSize = T.Body,
                                color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            // 平台與合併來源合成一行灰字，字體再大也只是換行不會爆版
                            val apps = contactApps[unifiedName].orEmpty()
                            val merged = rawNamesForThis.size > 1
                            if (apps.isNotEmpty() || merged) {
                                Text(
                                    buildString {
                                        append(apps.joinToString(" · ") { shortAppName(it) })
                                        if (merged) {
                                            if (isNotEmpty()) append("　")
                                            append("已合併 ${rawNamesForThis.size} 個帳號 ›")
                                        }
                                    },
                                    fontSize = T.Micro, color = if (merged) AppColors.ActiveBlue else AppColors.TextTertiary,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    // 合併過的：點這行直接看綁了哪些帳號、可以拆
                                    modifier = Modifier.padding(top = 2.dp)
                                        .then(if (merged) Modifier.clickable { accountsTarget = unifiedName } else Modifier)
                                )
                            }
                        }
                        if (selectionMode) {
                            Text(currentCategory.label, color = currentCategory.color, fontWeight = FontWeight.Bold,
                                fontSize = T.Micro, maxLines = 1, softWrap = false,
                                modifier = Modifier.tagChip(currentCategory, 6.dp)
                                    .padding(horizontal = 8.dp, vertical = 4.dp))
                        } else {
                            Row(
                                modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                    .tagChip(currentCategory, 6.dp)
                                    .clickable { quickTagTarget = unifiedName }
                                    .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(currentCategory.label, color = currentCategory.color, fontWeight = FontWeight.Bold,
                                    fontSize = T.Micro, maxLines = 1, softWrap = false)
                                Icon(Icons.Rounded.ArrowDropDown, contentDescription = "更換標籤",
                                    tint = currentCategory.color, modifier = Modifier.size(14.dp))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("›", fontSize = 17.sp, color = AppColors.TextTertiary, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }

        ListScrollbar(listState)
        ScrollTopButton(listState)

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
                        // ⚠️ 以前是一行 12sp 的藍字，跟旁邊的說明文字看起來一樣，使用者根本沒發現那是按鈕。
                        //    做成有框、有圖示的按鈕。
                        val allOn = selected.size == filteredContacts.size && filteredContacts.isNotEmpty()
                        Row(
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                .background(if (allOn) AppColors.ActiveBlue else AppColors.Surface)
                                .border(1.dp, if (allOn) AppColors.ActiveBlue else AppColors.BorderStrong, RoundedCornerShape(8.dp))
                                .clickable {
                                    if (allOn) selected.clear()
                                    else { selected.clear(); selected.addAll(filteredContacts) }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Check, contentDescription = null,
                                tint = if (allOn) Color.White else AppColors.TextPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (allOn) "取消全選" else "全選 ${filteredContacts.size}",
                                fontSize = T.Label, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false,
                                color = if (allOn) Color.White else AppColors.TextPrimary
                            )
                        }
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
        // ⚠️ 刪除本身擋不住對方再傳訊息 —— 紀錄會被重新建立，使用者以為沒刪掉。
        //    所以「永不記錄」要在刪除的當下就能勾，不要等它復活了才在提醒卡上補救。
        //    預設不勾：多數刪除是整理資料，不是要封鎖一個人。
        var alsoBlock by remember(targets) { mutableStateOf(false) }
        AlertDialog(
            containerColor = AppColors.Surface,
            onDismissRequest = { pendingDelete = null },
            title = { Text("刪除 ${targets.size} 位聯絡人？", fontWeight = FontWeight.Bold, fontSize = T.Title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            text = {
                Column {
                    Text("將一併刪除這些聯絡人的所有互動紀錄、標籤與合併設定。",
                        fontSize = T.Body, color = AppColors.TextSecondary, lineHeight = 18.sp)
                    Text("紀錄會先進「最近刪除」保留 7 天，期間可以還原。",
                        fontSize = T.Micro, color = AppColors.TextTertiary, lineHeight = 15.sp,
                        modifier = Modifier.padding(top = 2.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 160.dp)) {
                        items(targets.toList().take(50), key = { it }) {
                            Text("• $it", fontSize = T.Label, color = AppColors.TextPrimary, maxLines = 1,
                                overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(vertical = 1.dp))
                        }
                        if (targets.size > 50) {
                            item { Text("＋其他 ${targets.size - 50} 位", fontSize = T.Micro, color = AppColors.TextTertiary) }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppColors.SurfaceAlt)
                            .clickable { alsoBlock = !alsoBlock }
                            .padding(end = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = alsoBlock, onCheckedChange = { alsoBlock = it })
                        Column(modifier = Modifier.weight(1f)) {
                            Text("同時加入永不記錄名單", fontSize = T.Label, fontWeight = FontWeight.Bold,
                                color = AppColors.TextPrimary)
                            Text("之後對方再傳訊息也不會被記下來，可在設定裡解除",
                                fontSize = T.Micro, color = AppColors.TextTertiary, lineHeight = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val removed = viewModel.deleteContacts(targets)
                    if (alsoBlock) targets.forEach { viewModel.blockName(it) }
                    Toast.makeText(
                        context,
                        "已刪除 ${targets.size} 位（$removed 筆紀錄）" + if (alsoBlock) "並加入永不記錄" else "",
                        Toast.LENGTH_SHORT
                    ).show()
                    pendingDelete = null
                    exitSelection()
                }, colors = ButtonDefaults.buttonColors(containerColor = AppColors.WorkText)) { Text("確認刪除", maxLines = 1) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消", color = AppColors.TextSecondary) } }
        )
    }

    accountsTarget?.let { t ->
        // cb.rawByUnified 是背景算的，剛合併完還沒更新；補上 aliases 才看得到剛綁進來的帳號
        val raws = ((cb.rawByUnified[t] ?: emptyList()) + aliases.filterValues { it == t }.keys + t).distinct()
        ContactAccountsDialog(
            unifiedName = t,
            rawNames = raws,
            onUseName = { raw ->
                viewModel.renameContact(t, raw)
                Toast.makeText(context, "顯示名稱改成「$raw」", Toast.LENGTH_SHORT).show()
                accountsTarget = raw
            },
            onCustomName = { renameTarget = t; accountsTarget = null },
            onUnmerge = { raw ->
                viewModel.unmergeAccount(raw)
                Toast.makeText(context, "已取消綁定「$raw」，它變回獨立的聯絡人", Toast.LENGTH_SHORT).show()
            },
            onAddMerge = { mergeSource = t; accountsTarget = null },
            onDismiss = { accountsTarget = null }
        )
    }

    mergeSource?.let { source ->
        MergeTargetDialog(
            source = source,
            candidates = unifiedNamesList.filter { it != source && (aliases[it] ?: it) != source },
            // 合併完直接打開「帳號與名稱」，讓使用者當場挑要用哪個名字
            onPick = { target -> viewModel.mergeContact(source, target); mergeSource = null; accountsTarget = target },
            onDismiss = { mergeSource = null }
        )
    }

    if (showCategoryManager) {
        CategoryManagerDialog(viewModel, availableCats) { showCategoryManager = false }
    }

    quickTagTarget?.let { target ->
        QuickTagDialog(
            name = target,
            categories = availableCats,
            currentId = categoriesMap[target] ?: ContactCategory.OTHER.id,
            onPick = { catId -> viewModel.setCategory(target, catId); quickTagTarget = null },
            onDismiss = { quickTagTarget = null },
            onAddNew = {
                setPendingAssignName(target)
                quickTagTarget = null
                setShowAddCategoryDialog(true)
            }
        )
    }

    pendingBlockMany?.let { names ->
        ConfirmDialog(
            title = "刪除並封鎖 ${names.size} 位",
            message = "會刪除這些對象目前的新紀錄，並把名字加入永不記錄名單 —— " +
                "之後對方再傳訊息也不會被記下來。\n\n刪掉的紀錄會進「最近刪除」保留 7 天；" +
                "封鎖可在「設定 → 永不記錄名單」解除。",
            details = names,
            confirmLabel = "刪除並封鎖",
            onConfirm = {
                viewModel.deleteContacts(names.toSet())
                names.forEach { viewModel.blockName(it) }
                resurrected = viewModel.resurrectedNames()
                Toast.makeText(context, "已刪除並封鎖 ${names.size} 位", Toast.LENGTH_SHORT).show()
                pendingBlockMany = null
            },
            onDismiss = { pendingBlockMany = null }
        )
    }

    pendingBlock?.let { (name, count) ->
        ConfirmDialog(
            title = "刪除並封鎖",
            message = "會刪除「$name」目前的 $count 筆新紀錄，並把這個名字加入永不記錄名單 —— " +
                "之後對方再傳訊息也不會被記下來。\n\n刪掉的紀錄會進「最近刪除」保留 7 天；" +
                "封鎖可在「設定 → 永不記錄名單」解除。",
            confirmLabel = "刪除並封鎖",
            onConfirm = {
                viewModel.deleteContacts(setOf(name))
                viewModel.blockName(name)
                resurrected = viewModel.resurrectedNames()
                Toast.makeText(context, "已刪除並封鎖「$name」", Toast.LENGTH_SHORT).show()
                pendingBlock = null
            },
            onDismiss = { pendingBlock = null }
        )
    }

    if (showJunkReview) {
        JunkReviewDialog(
            names = junkContacts,
            records = records,
            aliases = aliases,
            onApply = { list ->
                viewModel.setCategories(list, ContactCategory.SYSTEM.id)
                Toast.makeText(context, "已標記 ${list.size} 個，統計不再算入", Toast.LENGTH_SHORT).show()
                showJunkReview = false
            },
            onDelete = { list ->
                pendingDelete = list.toSet()
                showJunkReview = false
            },
            onKeep = { list ->
                viewModel.ignoreJunk(list)
                Toast.makeText(context, "已保留 ${list.size} 個，不再列入提醒", Toast.LENGTH_SHORT).show()
                showJunkReview = false
            },
            onDismiss = { showJunkReview = false }
        )
    }

    longPressTarget?.let { target ->
        val isGrp = remember(target) { viewModel.isGroupName(target) }
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
                        // 改名、合併、看綁了哪些、取消綁定 —— 全部在「帳號與名稱」裡
                        "\ud83d\udc65 帳號與名稱（改名、合併、取消綁定）" to "accounts",
                        "\ud83c\udff7\ufe0f 設定標籤" to "tag",
                        // 自動判斷難免出錯（LINE 群組常被當成個人），一定要能手動改
                        (if (isGrp) "\ud83d\udc64 改成個人（不是群組）" else "\ud83d\udcac 改成群組聊天室") to "group",
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
                                        "accounts" -> accountsTarget = target
                                        // ⚠️ 以前接到 pendingAssignName，但那個狀態只是
                                        //    「新增自訂分類」時用來記住要套用給誰，
                                        //    本身不會開任何對話框 —— 所以按了完全沒反應。
                                        "tag" -> quickTagTarget = target
                                        "group" -> {
                                            viewModel.setGroupOverride(target, !isGrp)
                                            Toast.makeText(context,
                                                if (isGrp) "「$target」已改成個人" else "「$target」已改成群組",
                                                Toast.LENGTH_SHORT).show()
                                        }
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

/**
 * 帳號與名稱：一個人底下綁了哪些帳號、要叫什麼名字、綁錯了怎麼拆。
 *
 * ⚠️ 以前這三件事分散在長按選單的兩個項目裡，而且：
 *    · 看不到「到底合併了哪幾個帳號」（卡片上只寫「已合併 5 個帳號」）
 *    · 綁錯了沒有任何方法拆回來
 *    · 合併後顯示名稱固定是主帳號，不能挑
 *    全部收進這一個地方。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactAccountsDialog(
    unifiedName: String,
    rawNames: List<String>,
    onUseName: (String) -> Unit,
    onCustomName: () -> Unit,
    onUnmerge: (String) -> Unit,
    onAddMerge: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        containerColor = AppColors.Surface,
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("帳號與名稱", fontWeight = FontWeight.Black, fontSize = 18.sp, color = AppColors.TextPrimary)
                Text(unifiedName, fontSize = T.Label, color = AppColors.TextSecondary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        text = {
            Column {
                Text("顯示名稱", fontSize = T.Label, fontWeight = FontWeight.Black, color = AppColors.TextSecondary)
                var nameExpanded by remember { mutableStateOf(false) }
                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(unifiedName, fontSize = T.Body, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary,
                        maxLines = if (nameExpanded) Int.MAX_VALUE else 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(6.dp))
                            .combinedClickable(onClick = { nameExpanded = !nameExpanded }, onLongClick = { nameExpanded = !nameExpanded }))
                    // ⚠️ 以前只是一行藍字，跟旁邊的名字混在一起看不出是按鈕 —— 改成實心膠囊＋筆的圖示。
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(AppColors.BrandPrimary)
                            .clickable { onCustomName() }.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("自訂暱稱", fontSize = T.Label, fontWeight = FontWeight.Bold, color = Color.White,
                            maxLines = 1, softWrap = false)
                    }
                }

                Text("已綁定的帳號（${rawNames.size}）", fontSize = T.Label, fontWeight = FontWeight.Black, color = AppColors.TextSecondary)
                Text("名字太長被切掉時，長按名字看全名。點「用這個名字」把它設成顯示名稱；綁錯了按「取消綁定」，它會變回獨立的聯絡人，紀錄跟著它走。",
                    fontSize = T.Micro, color = AppColors.TextTertiary, lineHeight = 14.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 4.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                    items(rawNames, key = { it }) { raw ->
                        val isCurrent = raw == unifiedName
                        // ⚠️ 群組帳號常常開頭都一樣（「(lavender_0806) …」），截斷後八個看起來一模一樣，
                        //    長按（或點一下）名字就展開全名，再按一次收回。
                        var expanded by remember(raw) { mutableStateOf(false) }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(raw, fontSize = T.Body, color = AppColors.TextPrimary,
                                maxLines = if (expanded) Int.MAX_VALUE else 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(end = 6.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .combinedClickable(onClick = { expanded = !expanded }, onLongClick = { expanded = !expanded }))
                            if (isCurrent) {
                                Text("目前顯示", fontSize = T.Micro, fontWeight = FontWeight.Bold, color = AppColors.TextTertiary,
                                    maxLines = 1, softWrap = false)
                            } else {
                                Text("用這個名字", fontSize = T.Micro, fontWeight = FontWeight.Bold, color = AppColors.ActiveBlue,
                                    maxLines = 1, softWrap = false,
                                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { onUseName(raw) }
                                        .padding(horizontal = 6.dp, vertical = 5.dp))
                                if (rawNames.size > 1) {
                                    Text("取消綁定", fontSize = T.Micro, fontWeight = FontWeight.Bold, color = AppColors.WorkText,
                                        maxLines = 1, softWrap = false,
                                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { onUnmerge(raw) }
                                            .padding(horizontal = 6.dp, vertical = 5.dp))
                                }
                            }
                        }
                        HorizontalDivider(color = AppColors.BorderLight.copy(alpha = 0.6f))
                    }
                }
                Text("＋ 綁定其他帳號到這個人", fontSize = T.Body, fontWeight = FontWeight.Bold, color = AppColors.ActiveBlue,
                    modifier = Modifier.padding(top = 10.dp).clip(RoundedCornerShape(8.dp))
                        .clickable { onAddMerge() }.padding(vertical = 6.dp, horizontal = 2.dp))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成", color = AppColors.TextSecondary) } }
    )
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
                Spacer(modifier = Modifier.height(8.dp))
                // ⚠️ 這一頁最常見的問題是「我不知道它要幹嘛」。
                //    先寫清楚它回答的是哪個問題，再給操作步驟。
                Text(
                    "這一頁只回答一件事：停留時間為什麼沒有累積。",
                    fontSize = T.Label, fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary, lineHeight = 18.sp
                )
                Text(
                    "停留時間靠無障礙服務判斷「你正開著哪個聊天室」，中間有三個可能卡住的環節：" +
                    "服務沒開、抓不到聊天室標題、或標題被判定成不是聊天室。" +
                    "下方紀錄會逐筆寫出每個畫面的判定結果，看得出卡在哪一步。",
                    fontSize = T.Label, color = AppColors.TextSecondary, lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "測法：開著這個 App，切到 LINE / Instagram 打開一個聊天室停留幾秒，再回來看下方紀錄。" +
                    "看不懂也沒關係 —— 按「複製紀錄」整包貼給開發者即可。",
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
    // ⚠️ 沒有這個的話「刪除選取」是按下去立刻生效的 ——
    //    全 App 只剩這一個破壞性動作沒有二次確認。
    var confirmDelete by remember { mutableStateOf(false) }

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
                                        if (rec.isMissedCall) append(" · 未接來電")
                                        else if (rec.isCall) append(" · " + callDirectionLabel(rec))
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
                onClick = { confirmDelete = true },
                enabled = selected.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.WorkText)
            ) { Text("刪除選取 (${selected.size})", maxLines = 1, softWrap = false) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("關閉", color = AppColors.TextSecondary) } }
    )

    if (confirmDelete) {
        // 按對象分組列出來，比列一堆時間戳看得懂
        val byName = remember(selected.size) {
            val ids = selected.toHashSet()
            records.filter { it.id in ids }
                .groupBy { aliases[it.rawSenderName] ?: it.rawSenderName }
                .map { (n, list) -> "$n　${list.size} 筆" }
                .sortedByDescending { it.length }
        }
        ConfirmDialog(
            title = "刪除 ${selected.size} 筆紀錄？",
            message = "這些紀錄會從統計中移除。" +
                "\n刪掉之後可以到「設定 → 最近刪除」復原。",
            details = byName,
            confirmLabel = "刪除 ${selected.size} 筆",
            onConfirm = {
                val n = viewModel.deleteRecordsByIds(selected.toSet())
                Toast.makeText(context, "已刪除 $n 筆（可在設定頁復原）", Toast.LENGTH_SHORT).show()
                selected.clear()
                confirmDelete = false
            },
            onDismiss = { confirmDelete = false }
        )
    }
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
                        Box(modifier = Modifier.tagChip(cat, 6.dp).padding(horizontal = 8.dp, vertical = 4.dp)) {
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
    // ⚠️ 不要 remember 住色票：換配色之後這裡要跟著變。
    val palette = AppColors.CategoryPalette
    var label by remember { mutableStateOf(initial?.label ?: "") }
    // 存的是「配色的第幾個顏色」而不是色值，所以換配色時標籤會一起換
    var pick by remember { mutableStateOf(initial?.colorInt?.takeIf { it in palette.indices } ?: 0) }

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
                palette.chunked(4).forEachIndexed { rowIdx, row ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEachIndexed { colIdx, pair ->
                            val gi = rowIdx * 4 + colIdx
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
                        repeat(4 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(label, pick, pick) },
                enabled = label.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.BrandPrimary)
            ) { Text("儲存", maxLines = 1) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = AppColors.TextSecondary) } }
    )
}

/**
 * 刪除前的逐項確認。
 *
 * 直接「確定 → 全刪」太危險：使用者常常只是想清掉某幾個對象，
 * 卻連想留的一起沒了，而且刪完才發現。這裡先把範圍內每位對象與筆數列出來，
 * 預設全勾（＝全刪），取消勾選的那幾位會被保留。
 */
@Composable
fun ClearPreviewDialog(
    title: String,
    rows: List<Pair<String, Int>>,
    onConfirm: (keep: Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    // 這裡存的是「要保留的人」，取消勾選就加進來
    val keep = remember { mutableStateListOf<String>() }
    val total = rows.sumOf { it.second }
    val willDelete = rows.filter { it.first !in keep }.sumOf { it.second }

    AlertDialog(
        containerColor = AppColors.Surface,
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = T.Title, color = AppColors.TextPrimary)
                Text("將刪除 $willDelete 筆／共 $total 筆　·　取消勾選就保留",
                    fontSize = T.Label, color = if (willDelete > 0) AppColors.WorkText else AppColors.TextSecondary,
                    fontWeight = FontWeight.Bold)
            }
        },
        text = {
            if (rows.isEmpty()) {
                Text("這個範圍內沒有任何紀錄。", fontSize = T.Body, color = AppColors.TextSecondary)
            } else {
                Column {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("全部勾選" to true, "全部取消" to false).forEach { (lbl, checkAll) ->
                            Box(
                                modifier = Modifier.weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AppColors.Background)
                                    .clickable {
                                        keep.clear()
                                        if (!checkAll) keep.addAll(rows.map { it.first })
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(lbl, fontSize = T.Label, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary, maxLines = 1, softWrap = false)
                            }
                        }
                    }
                    LazyColumn(modifier = Modifier.heightIn(max = 340.dp)) {
                        items(rows, key = { it.first }) { (name, count) ->
                            val checked = name !in keep
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { if (checked) keep.add(name) else keep.remove(name) }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = checked, onCheckedChange = { on -> if (on) keep.remove(name) else keep.add(name) })
                                Text(name, fontSize = T.Body, color = if (checked) AppColors.TextPrimary else AppColors.TextTertiary,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                Text("$count 筆", fontSize = T.Label,
                                    color = if (checked) AppColors.WorkText else AppColors.TextTertiary,
                                    fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(keep.toSet()) },
                enabled = willDelete > 0,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.WorkText)
            ) { Text("刪除 $willDelete 筆", maxLines = 1) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = AppColors.TextSecondary) } }
    )
}

/**
 * 封鎖名單。
 * 「刪除並封鎖」是單向門，一定要有地方看得到封鎖了誰、能解除，
 * 否則封錯人就永遠救不回來。
 */
@Composable
fun BlockedNamesDialog(viewModel: RelateViewModel, onDismiss: () -> Unit) {
    val blocked by viewModel.blockedNames.collectAsState()
    AlertDialog(
        containerColor = AppColors.Surface,
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("永不記錄名單", fontWeight = FontWeight.Bold, fontSize = T.Title, color = AppColors.TextPrimary)
                Text("這些對象的通知會被直接忽略，不產生任何紀錄",
                    fontSize = T.Label, color = AppColors.TextSecondary, lineHeight = 16.sp)
            }
        },
        text = {
            if (blocked.isEmpty()) {
                Text("目前沒有封鎖任何對象。", fontSize = T.Body, color = AppColors.TextSecondary)
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(blocked.toList().sorted(), key = { it }) { name ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(name, fontSize = T.Body, color = AppColors.TextPrimary,
                                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                    .background(AppColors.Background)
                                    .border(1.dp, AppColors.BorderLight, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.unblockName(name) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("解除", fontSize = T.Label, fontWeight = FontWeight.Bold,
                                    color = AppColors.ActiveBlue, maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("關閉", color = AppColors.TextSecondary) } }
    )
}

/**
 * 配色選擇。
 *
 * ⚠️ 做成彈窗而不是設定頁裡的一列，是因為**要看得到實際套用的樣子**。
 *    設定頁上只有一條色帶，使用者根本不知道套下去的圓餅圖與標籤長什麼樣。
 *    這裡每一組都直接畫出「圖表會用的顏色」與「標籤會長的樣子」。
 * ⚠️ 點下去立刻套用（不做「確定」按鈕）——
 *    整個 App 會即時跟著變，關掉彈窗就能看到真正的圖表。
 *    後悔就再點另一組，沒有破壞性，不需要二次確認。
 */
@Composable
fun PaletteDialog(currentId: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    // ⚠️ 預覽要排滿七個，順序跟 ChartPalette.tags 一致。
    //    只放幾個的話看不出「標籤彼此分不分得開」，而那正是挑配色最想知道的事。
    val sampleTags = listOf("伴侶", "家人", "朋友", "工作", "其他", "系統通知", "未分類")
    AlertDialog(
        containerColor = AppColors.Surface,
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("圖表與標籤配色", fontWeight = FontWeight.Black, fontSize = 18.sp,
                    letterSpacing = (-0.3).sp, color = AppColors.TextPrimary)
                Text("圖表、標籤、按鈕與連結都會跟著換；背景與文字維持原本的灰白",
                    fontSize = T.Micro, color = AppColors.TextTertiary, lineHeight = 15.sp)
            }
        },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 460.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CHART_PALETTES, key = { it.id }) { p ->
                    val on = p.id == currentId
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (on) AppColors.SurfaceAlt else Color.Transparent)
                            .border(
                                if (on) 2.dp else 1.dp,
                                if (on) AppColors.BrandPrimary else AppColors.BorderLight,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onPick(p.id) }
                            .padding(10.dp)
                    ) {
                        // 圖表會長的樣子：一條由六個顏色組成的色帶
                        Row(modifier = Modifier.fillMaxWidth().height(18.dp).clip(RoundedCornerShape(4.dp))) {
                            p.colors.forEach { c ->
                                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(c))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        // 標籤會長的樣子：直接畫成標籤
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            sampleTags.forEachIndexed { i, label ->
                                Text(
                                    label, fontSize = T.Micro, fontWeight = FontWeight.Bold,
                                    color = p.tagStyle(i).text, maxLines = 1, softWrap = false,
                                    modifier = Modifier
                                        .then(
                                            // 未分類：透明底＋虛線框，預覽裡也要看得到
                                            p.tagStyle(i).let { st ->
                                                if (st.dashed) Modifier.drawBehind {
                                                    val r = 5.dp.toPx()
                                                    drawRoundRect(
                                                        color = st.text.copy(alpha = 0.55f),
                                                        cornerRadius = CornerRadius(r, r),
                                                        style = Stroke(
                                                            width = 1.dp.toPx(),
                                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
                                                        )
                                                    )
                                                } else Modifier.background(st.bg, RoundedCornerShape(5.dp))
                                            }
                                        )
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        // 介面會長的樣子：這組配色的按鈕與連結
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("按鈕", fontSize = T.Micro, fontWeight = FontWeight.Bold, color = Color.White,
                                maxLines = 1, softWrap = false,
                                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(p.brand)
                                    .padding(horizontal = 10.dp, vertical = 4.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("連結文字", fontSize = T.Micro, fontWeight = FontWeight.Bold,
                                color = p.accent, maxLines = 1, softWrap = false)
                            Spacer(modifier = Modifier.weight(1f))
                            if (on) {
                                Text("✓", fontSize = T.Title, fontWeight = FontWeight.Black,
                                    color = p.brand, maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成", color = AppColors.TextSecondary) } }
    )
}

/** 已保留的非真人來源。
 *
 * ⚠️ 「保留不處理」是永久的，所以必須有地方看得到並且改得回來 ——
 *    按錯了卻沒有出口，那個來源就永遠不會再被提醒。
 */
@Composable
fun KeptSourcesDialog(viewModel: RelateViewModel, onDismiss: () -> Unit) {
    val kept by viewModel.junkIgnored.collectAsState()
    AlertDialog(
        containerColor = AppColors.Surface,
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("已保留的來源", fontWeight = FontWeight.Bold, fontSize = T.Title, color = AppColors.TextPrimary)
                Text("規則把這些當成非真人，但你確認過它們是真人，所以不再提醒",
                    fontSize = T.Label, color = AppColors.TextSecondary, lineHeight = 16.sp)
            }
        },
        text = {
            if (kept.isEmpty()) {
                Text("目前沒有保留任何來源。", fontSize = T.Body, color = AppColors.TextSecondary)
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(kept.toList().sorted(), key = { it }) { name ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(name, fontSize = T.Body, color = AppColors.TextPrimary,
                                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                    .background(AppColors.Background)
                                    .border(1.dp, AppColors.BorderLight, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.unignoreJunk(name) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("取消保留", fontSize = T.Label, fontWeight = FontWeight.Bold,
                                    color = AppColors.ActiveBlue, maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("關閉", color = AppColors.TextSecondary) } }
    )
}

/**
 * 洞察區塊的排序 / 隱藏。
 *
 * 用上下箭頭而不是拖曳：拖曳在對話框裡跟外層的捲動會互搶手勢，
 * 而且要多帶一個排序函式庫；箭頭一次移一格，行為明確也按得準。
 */
@Composable
fun SectionOrderDialog(
    sections: List<Pair<String, String>>,
    hidden: Set<String>,
    onMove: (String, Int) -> Unit,
    onToggle: (String) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        containerColor = AppColors.Surface,
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("區塊順序", fontWeight = FontWeight.Bold, fontSize = T.Title, color = AppColors.TextPrimary)
                Text("用箭頭上下移動；點眼睛可以隱藏不想看的區塊",
                    fontSize = T.Label, color = AppColors.TextSecondary, lineHeight = 16.sp)
            }
        },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                itemsIndexed(sections, key = { _, it -> it.first }) { idx, (id, label) ->
                    val isHidden = id in hidden
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            label,
                            fontSize = T.Body,
                            fontWeight = FontWeight.Medium,
                            color = if (isHidden) AppColors.TextTertiary else AppColors.TextPrimary,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            if (isHidden) "隱藏中" else "顯示",
                            fontSize = T.Micro,
                            color = if (isHidden) AppColors.TextTertiary else AppColors.Positive,
                            fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                .clickable { onToggle(id) }
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                        )
                        Text(
                            "↑", fontSize = 17.sp, textAlign = TextAlign.Center,
                            color = if (idx > 0) AppColors.ActiveBlue else AppColors.BorderStrong,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                .clickable(enabled = idx > 0) { onMove(id, -1) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        Text(
                            "↓", fontSize = 17.sp, textAlign = TextAlign.Center,
                            color = if (idx < sections.size - 1) AppColors.ActiveBlue else AppColors.BorderStrong,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                .clickable(enabled = idx < sections.size - 1) { onMove(id, 1) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = AppColors.BrandPrimary)) {
                Text("完成")
            }
        },
        dismissButton = { TextButton(onClick = onReset) { Text("回到預設", color = AppColors.TextSecondary) } }
    )
}

/**
 * 「構成」類的卡片：講的是「這段期間裡誰／什麼佔多少」。
 *
 * ⚠️ 這四張以前只長在洞察頁，但它們跟總覽的平台分佈／標籤分佈是同一種東西
 *    （都是本期構成），分在兩頁才會讓人分不清兩頁的差別。
 *    抽出來讓總覽用，洞察頁只留「模式與變化」。
 *
 * drill 統一由呼叫端提供：兩頁各自有自己的下鑽對話框。
 */
@Composable
fun CallsCard(calls: CallSummary, drill: (String, (InteractionRecord) -> Boolean) -> Unit) {
    // 舊資料沒有 isCall 欄位，靠 appName 補判斷（跟 getCallSummary 的條件一致）
    fun isCallRec(r: InteractionRecord) = r.isCall || r.isMissedCall || r.appName == "電話"
    Panel {
        // ⚠️ 不要再解釋「為什麼沒有通話時長」。
        //    沒有秒數時就整格不畫（見下方），畫面上根本不會有空欄位，
        //    沒有東西缺席就不需要道歉 —— 多一個 ⓘ 反而是在提醒使用者這裡少了東西。
        SectionHead("通話統計")
        // ⚠️ 每一格都要點得進去看是「誰」——只給一個總數，使用者看不出這幾通是跟誰通的。
        Row(modifier = Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatPair("總通話", "${calls.total} 通", "點看是誰", Modifier.weight(1f)) {
                drill("通話對象") { r -> isCallRec(r) }
            }
            StatPair("接通", "${calls.answered} 通", null, Modifier.weight(1f)) {
                drill("接通的通話對象") { r -> isCallRec(r) && !r.isMissedCall }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        // 誰打給誰：只有「對方打來」沒有「我打出去」的話，看起來會像從來沒主動找過人
        Row(modifier = Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatPair("對方打來", "${calls.incoming} 通", "點看是誰", Modifier.weight(1f)) {
                drill("打電話給我的人") { r ->
                    isCallRec(r) && (r.callDirection == 1 || (r.callDirection == 0 && r.isMissedCall))
                }
            }
            StatPair("我打出去", "${calls.outgoing} 通",
                if (calls.unknownDir > 0) "另有 ${calls.unknownDir} 通分不出方向" else "點看是誰", Modifier.weight(1f)) {
                drill("我打電話給誰") { r -> isCallRec(r) && r.callDirection == 2 }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatPair("未接", "${calls.missed} 通", "未接率 ${calls.missRate}%", Modifier.weight(1f)) {
                drill("未接來電是誰打的") { r -> r.isMissedCall }
            }
            // ⚠️ 沒有秒數時**整格拿掉**，不要留一個「—　需匯入通話紀錄」的空欄位。
            //    永遠是空的欄位只會佔位置又讓人以為壞了；
            //    「為什麼沒有時長」的說明已經在標題旁的 ⓘ 裡了。
            if (calls.hasDuration) {
                StatPair("平均時長", formatDuration(calls.avgSeconds), "依 ${calls.timed} 通有秒數的通話", Modifier.weight(1f))
            }
        }

    }
}

@Composable
fun LengthsCard(
    lengths: List<Triple<String, Int, Float>>,
    avgWords: Int,
    drill: (String, (InteractionRecord) -> Boolean) -> Unit
) {
    Panel {
        SectionHead("訊息長度分佈") {
            Text("平均 $avgWords 字／則", fontSize = T.Micro, color = AppColors.TextTertiary, maxLines = 1, softWrap = false)
        }
        lengths.forEachIndexed { i, triple ->
            val (label, count, pct) = triple
            // 五個級距對應的字數區間，點下去看是誰在傳這種長度的訊息
            val range = when (i) {
                0 -> 1..5
                1 -> 6..15
                2 -> 16..40
                3 -> 41..100
                else -> 101..Int.MAX_VALUE
            }
            DataRow(
                name = label,
                dotColor = AppColors.Chart.at(i),
                progress = pct,
                primary = "$count 則",
                secondary = "${(pct * 100).toInt()}%",
                onClick = {
                    drill("$label 的訊息來自") { r -> r.isIncoming && !r.isCall && r.wordCount in range }
                }
            )
        }
    }
}

@Composable
fun GroupSplitCard(split: GroupSplit, drill: (String, (InteractionRecord) -> Boolean) -> Unit) {
    Panel {
        SectionHead("群組 vs 個人")
        SplitBar(
            "群組", split.groupMsgs.toLong(), "個人", split.personMsgs.toLong(),
            AppColors.Chart.at(0), AppColors.Chart.at(2),
            onLeftClick = { drill("群組") { r -> r.isGroup } },
            onRightClick = { drill("個人") { r -> !r.isGroup } }
        ) { "$it 則" }
        Spacer(modifier = Modifier.height(10.dp))
        SplitBar(
            "群組停留", split.groupDuration, "個人停留", split.personDuration,
            AppColors.Chart.at(0).copy(alpha = 0.7f), AppColors.Chart.at(2).copy(alpha = 0.7f),
            onLeftClick = { drill("群組停留") { r -> r.isGroup && r.durationSeconds > 0 } },
            onRightClick = { drill("個人停留") { r -> !r.isGroup && r.durationSeconds > 0 } }
        ) { formatDuration(it) }
    }
}

@Composable
fun MsgVsTimeCard(msgs: Int, opened: Int, drill: (String, (InteractionRecord) -> Boolean) -> Unit) {
    Panel {
        // 長段說明一律收進 i，卡片上只留數字
        SectionHead("訊息 vs 停留") {
            InfoDot(
                "訊息 vs 停留",
                "兩者是互補的：訊息只有在你「沒開著聊天室」時才會跳通知，" +
                    "而停留時間只有在你「主動打開聊天室」時才會累積。\n\n加起來才是完整的注意力。"
            )
        }
        Row(modifier = Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatPair("被動接收", "$msgs 則", "通知攔截", Modifier.weight(1f)) {
                drill("被動接收（通知攔截）") { r -> r.isIncoming && !r.isCall }
            }
            StatPair("主動開啟", "$opened 次", "進入聊天室", Modifier.weight(1f)) {
                drill("主動開啟（進入聊天室）") { r -> r.durationSeconds > 0 }
            }
        }
    }
}

/**
 * 時段分佈（早晨 / 下午 / 夜晚）。
 * 這是「時間模式」，跟平台分佈那種「構成」不同類，所以放在洞察頁。
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun TimeOfDayCard(timeStats: List<TimeOfDayStat>, drill: (String, (InteractionRecord) -> Boolean) -> Unit) {
    Panel {
        SectionHead("時段分佈")
        // 顏色走 ChartPalette，跟其他圖表同一套；st.color 是舊的寫死色，不再使用
        Row(modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))) {
            timeStats.forEachIndexed { i, st ->
                val name = st.period.split(" ")[0]
                Box(
                    modifier = Modifier.weight(st.percentage.coerceAtLeast(0.02f)).fillMaxHeight()
                        .background(AppColors.Chart.at(i))
                        .combinedClickable(
                            onClick = { drill("$name 活躍對象", periodPredicate(name)) },
                            onLongClick = { drill("$name 活躍對象", periodPredicate(name)) }
                        )
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            timeStats.forEachIndexed { i, st ->
                val name = st.period.split(" ")[0]
                Column(
                    modifier = Modifier.weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppColors.SurfaceAlt)
                        .clickable { drill("$name 活躍對象", periodPredicate(name)) }
                        .padding(horizontal = 8.dp, vertical = 7.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(AppColors.Chart.at(i)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(name, fontSize = T.Label, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary, maxLines = 1, softWrap = false)
                    }
                    Text("${(st.percentage * 100).toInt()}%", fontSize = T.KpiSm, fontWeight = FontWeight.Black,
                        color = AppColors.TextPrimary, maxLines = 1, softWrap = false)
                    Text(st.explicitTimeRange, fontSize = T.Micro, color = AppColors.TextTertiary, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

/**
 * 依「一週起始日」設定旋轉星期標籤。
 * 資料層一律以週日為索引 0，只有顯示時旋轉，這樣改設定不用重算任何統計。
 */
fun rotatedWeekLabels(weekStartDay: Int): List<String> {
    val base = listOf("日", "一", "二", "三", "四", "五", "六")
    val offset = (weekStartDay - Calendar.SUNDAY + 7) % 7
    return (0..6).map { base[(it + offset) % 7] }
}

/** 平台名縮寫：清單列的空間有限，Instagram/Messenger 全名會把名字擠掉 */
fun shortAppName(name: String): String = when (name) {
    "Instagram" -> "IG"
    "Messenger" -> "Messenger"
    "Facebook" -> "FB"
    "WhatsApp" -> "WA"
    "Telegram" -> "TG"
    else -> name
}

/** 早晨 05-11 / 下午 12-17 / 夜晚 18-04 */
fun periodPredicate(name: String): (InteractionRecord) -> Boolean = { r ->
    val c = dowCal.get()!!
    c.timeInMillis = r.timestamp
    val h = c.get(Calendar.HOUR_OF_DAY)
    when (name) {
        "早晨" -> h in 5..11
        "下午" -> h in 12..17
        else -> h !in 5..17
    }
}

/**
 * 標明這張卡的時間範圍不跟隨上方的日期篩選。
 * 使用者問過「洞察裡有很多非本週的東西也被算進去」——
 * 有些指標確實刻意看全期間，但一定要在卡片上講清楚，不能讓人猜。
 */
@Composable
fun ScopeTag(text: String) {
    Box(
        modifier = Modifier.background(AppColors.Background, RoundedCornerShape(5.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, fontSize = T.Micro, color = AppColors.TextTertiary, maxLines = 1, softWrap = false)
    }
}

/**
 * 設定頁的區段標題。
 * 卡片一張接一張沒有層次時，使用者只能一路往下掃；
 * 分成幾個區段之後才看得出「這一塊是什麼」。
 */
@Composable
fun SettingsSection(title: String) {
    Text(
        title,
        fontSize = T.Micro,
        fontWeight = FontWeight.Black,
        color = AppColors.TextTertiary,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp),
        maxLines = 1, softWrap = false
    )
}

/**
 * 設定頁唯一的容器。
 *
 * 整頁只用這一種卡：Surface 底、1dp 邊框、14dp 圓角、無陰影、內容自己控制內距。
 * 以前每張卡各自寫 `Card(colors=…, border=…, shape=…)`，圓角 14/16 混用、
 * 內距 14/16 混用，看起來就是拼湊出來的。
 */
@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.BorderLight, RoundedCornerShape(14.dp)),
        content = content
    )
}

/** 卡片內兩列之間的分隔線；縮排讓它從文字起點開始，不要貼滿整張卡 */
@Composable
fun SettingsDivider() {
    HorizontalDivider(color = AppColors.BorderLight, modifier = Modifier.padding(start = 14.dp))
}

/**
 * 設定頁的標準一列：標題 + 說明 + 右側箭號。
 * 說明是必填的 —— 只有標題的話，「整理系統通知」這種名字猜不出會發生什麼事，
 * 使用者就不敢按。
 */
@Composable
fun SettingsRow(
    title: String,
    subtitle: String? = null,
    danger: Boolean = false,
    accent: Boolean = false,
    onClick: () -> Unit
) {
    val tint = when {
        danger -> AppColors.WorkText
        accent -> AppColors.ActiveBlue
        else -> AppColors.TextPrimary
    }
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, fontSize = T.Body, fontWeight = FontWeight.Bold, color = tint,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            // 副標只寫在「光看名字猜不出會發生什麼」的項目上。
            // 每一列都掛一句說明，整頁會變成一面文字牆，反而更難掃。
            if (subtitle != null) {
                Text(subtitle, fontSize = T.Micro, color = AppColors.TextTertiary, lineHeight = 14.sp)
            }
        }
        Text("›", fontSize = 17.sp, color = AppColors.TextTertiary, maxLines = 1, softWrap = false)
    }
}
/** 使用說明的一個章節 */
private class GuideSection(val no: String, val title: String, val paragraphs: List<String>)

/**
 * 使用說明。
 *
 * 寫成正式的說明文件而不是幾句提示：這個 App 的數字有不少「為什麼是這樣」
 * 需要交代（訊息數與停留時間為何互補、哪些指標跟隨日期篩選、常用詞為何只能全期間），
 * 使用者看不到解釋就會以為是算錯。
 * 章節資料與畫面分開，之後要加一節只要往清單裡加一筆。
 */
private val USER_GUIDE = listOf(
    GuideSection(
        "1", "資料從哪裡來",
        listOf(
            "本 App 不讀取任何訊息內容，也不會連上伺服器。所有統計都由三項系統權限產生，缺一項就少一類資料。",
            "通知存取：對方傳訊息跳出通知時，在背景取得發話者與字數。這是訊息數與字數的唯一來源。",
            "無障礙服務：偵測你何時開啟、何時離開某個聊天室，據此計算停留時間。單次上限 30 分鐘，超過視為背景掛機不計入。",
            "使用紀錄存取：取得手機總使用時間，用來換算「聊天佔手機用量的百分比」。"
        )
    ),
    GuideSection(
        "2", "訊息數與停留時間為什麼是兩回事",
        listOf(
            "訊息數只有在你「沒開著聊天室」時才會累積——你人在聊天室裡，系統就不會跳通知。它代表被動接收的量。",
            "停留時間只有在你「主動打開聊天室」時才會累積。它代表主動投入的注意力。兩者相加才是完整的注意力。"
        )
    ),
    GuideSection(
        "3", "日期範圍",
        listOf(
            "頁面頂端的日期列決定統計範圍，可選單日、單週、自訂區間或全部。左右滑動畫面也能換到上一天／上一週。",
            "翻到別的日期後，日期列會出現「今天」或「本週」按鈕，一鍵回到現在；月曆上的今天會有外框。",
            "常用詞固定是全期間累計（不保留訊息原文，事後無法分期）；「哪幾天在聊」固定看最近數月。"
        )
    ),
    GuideSection(
        "4", "聯絡人、合併與名稱",
        listOf(
            "同一個人在不同平台會被當成不同帳號。點聯絡人 →「帳號與名稱」可以把其他帳號綁到這個人身上，之後統計會合併計算。",
            "在「帳號與名稱」裡看得到這個人綁了哪些帳號，可以挑其中一個按「用這個名字」，或按顯示名稱旁的藍色「✎ 自訂暱稱」自己打。改名只影響本 App，不會動到 LINE 或 IG。",
            "帳號名稱太長被切掉時，長按（或點一下）名字就會展開全名，再按一次收回。",
            "綁錯了按「取消綁定」，那個帳號會變回獨立的聯絡人，紀錄跟著它走，不會遺失。",
            "卡片上寫「已合併 N 個帳號 ›」的那一行也可以直接點開。長按聯絡人可以多選，批次設定標籤或刪除。"
        )
    ),
    GuideSection(
        "5", "標籤與系統通知",
        listOf(
            "聯絡人頁的標籤列會直接顯示每個標籤有幾位，點一下就只看那個標籤。「全部」不含系統通知。",
            "讚、追蹤者、推播這類非真人來源會被歸到「系統通知」，不列入排行與統計。聯絡人頁的提醒卡可以逐筆確認：標為系統通知、刪除，或確認是真人後保留不再提醒。",
            "刪除這類來源沒有用——對方下次推播就會重建。刪除聯絡人時可以勾「同時加入永不記錄名單」，之後就不會再記下來。",
            "「未分類」是虛線框，代表還沒分；分好類之後圖表才看得出你的時間花在哪種關係上。"
        )
    ),
    GuideSection(
        "6", "通話",
        listOf(
            "未接來電由通知取得。",
            "LINE、Messenger 等 App 的通話：從「通話中」通知出現到消失的時間會記成通話長度。部分 App 或機型不會掛這則通知，就無法取得秒數。",
            "手機原生電話：接通後系統會掛一則通話中的常駐通知，從它出現到消失的時間就是秒數。撥號等待的那段不算，但掛斷後通知晚幾秒才消失時會多算幾秒。",
            "「通話統計」上的每個數字都可以點，會列出是跟誰通的：總通話、接通、對方打來、我打出去、未接各自一份名單。",
            "「逐筆檢視紀錄」裡，通話那一列會寫「對方打來」或「我打出去」：對方打來時會先跳來電通知，我方撥出則不會，靠這個分辨。分辨不出來時只寫「通話」。"
        )
    ),
    GuideSection(
        "7", "圖表與配色",
        listOf(
            "圖表上的長條、格子、圓餅扇形，點一下或長按都會顯示那一格的數字；圓餅長按可以看是哪些人。",
            "「設定 → 圖表與標籤配色」有五組色盤，會一起換掉圖表、標籤、按鈕與連結的顏色；背景、文字與 App 圖示維持原樣。",
            "社交人格點進去可以看到全部類型與判定方式。"
        )
    ),
    GuideSection(
        "8", "常用詞",
        listOf(
            "只統計詞頻，不保留任何訊息原文。系統自動產生的文字（傳送了貼圖、回覆了限時動態等）與聯絡人名稱都會排除。",
            "點一個詞可以看到是誰傳的。這份資料是 v3.20 之後才開始記錄，之前累積的詞看不到發話者。"
        )
    ),
    GuideSection(
        "9", "每晚回顧",
        listOf(
            "每天晚上九點左右會發一則通知。點開後先看概要：一句今天跟昨天比的總結、訊息／對象／停留三張數字卡（▲▼ 標出比昨天多或少）、今天幾點聊最多的節奏圖、聊天對象的標籤分布，以及聊最多的三個人。",
            "關掉概要（✕ 或「看完整報告」）會直接帶你到分析頁的那一天。今天沒有互動就不會發通知；可以在「設定 → 偏好」關掉。",
            "不必等通知：畫面上方的「每日報告」隨時打開今天的報告，用日期旁的 ‹ › 翻看過去每一天，「回今天」一鍵回來。",
            "如果停留時間偵測（無障礙服務）被系統關掉，也會另外發通知提醒你重新開啟。"
        )
    ),
    GuideSection(
        "10", "刪除、還原與備份",
        listOf(
            "「設定 → 清除資料」只會刪互動紀錄（訊息、停留、通話），標籤、合併的帳號、改過的名稱、常用詞都不會動。刪之前會列出清單讓你逐人取消。",
            "刪掉的紀錄會進「最近刪除」保留 7 天，可以搜尋、整批還原，或只還原其中一個人。",
            "所有資料只存在這支手機。到「設定 → 備份」按「☁ 綁定 Google 雲端硬碟」，在儲存位置選雲端硬碟。綁定後可以看到備份存在哪裡、上次備份時間，並把自動備份調成每天、每 3 天、每週或不自動；隨時也可以按「立即備份」。",
            "換機時，在新手機裝好 App 後按「從檔案還原」，到雲端硬碟選那份備份檔即可。沒綁定的話，換機前請手動「存成檔案」。",
            "開啟通知存取、無障礙服務、使用紀錄存取之前，App 會先說明讀取哪些資料。完整內容見「設定 → 隱私權政策」。"
        )
    )
)

/**
 * 常見問題。跟 USER_GUIDE 分開放：說明是「這個 App 怎麼運作」，
 * 常見問題是「東西不對的時候怎麼辦」，兩者的查閱時機完全不同。
 */
private val USER_FAQ = listOf(
    "停留時間都是 0，怎麼辦？" to
        "多半是無障礙服務被關掉了。Android 每次重新安裝 App 都會自動停用它，需要手動重開。" +
        "到「設定 → 服務」把「停留時間偵測」打開；服務被關掉時 App 也會發通知提醒。",
    "LINE 通話的時間有算嗎？" to
        "有。LINE、Messenger 等 App 通話時會掛一則「通話中」通知，從它出現到消失的時間就記成通話長度。" +
        "有。手機原生電話也一樣，接通後的通話中通知消失時就算出秒數。少數 App 或機型不會掛這則通知，那就拿不到秒數。",
    "合併錯人了，怎麼拆開？" to
        "點那位聯絡人 →「帳號與名稱」，在綁錯的帳號旁按「取消綁定」。它會變回獨立的聯絡人，紀錄跟著它走，不會遺失。",
    "合併後想用另一個名字？" to
        "同樣在「帳號與名稱」裡，挑一個帳號按「用這個名字」，或按顯示名稱旁的藍色「✎ 自訂暱稱」自己打。" +
        "帳號名稱被切掉分不出來時，長按名字可以看全名。",
    "為什麼有一堆不是人的「聯絡人」？" to
        "讚、追蹤者、推播這類通知也會被系統送出來，名稱還會把數字寫進去，所以同一種通知會被拆成很多筆。" +
        "聯絡人頁的提醒卡點開可以逐筆確認。不要用刪除的 —— 對方下次推播就會重建。",
    "清除資料會不會把標籤跟合併也刪掉？" to
        "不會。清除資料只刪互動紀錄，標籤、合併的帳號、改過的名稱、常用詞都保留。刪掉的紀錄會進「最近刪除」保留 7 天。",
    "刪錯了可以救回來嗎？" to
        "可以。到「設定 → 最近刪除」，保留 7 天，可以整批還原，也可以只還原其中一個人。",
    "換手機資料會跟著走嗎？" to
        "先綁定雲端備份：「設定 → 備份 → ☁ 綁定 Google 雲端硬碟」，之後依你設定的週期自動更新（預設每天）。" +
        "新手機裝好 App 後按「從檔案還原」，到雲端硬碟選那份備份檔。沒綁定的話，換機前請手動「存成檔案」。",
    "App 會把我的資料傳出去嗎？" to
        "不會。App 沒有網路權限，無法把任何資料傳出手機，開發者也看不到。" +
        "唯一的例外是你自己綁定的雲端備份：備份檔由 Google 雲端硬碟 App 上傳到你自己的雲端硬碟。",
    "常用詞為什麼看不到是誰傳的？" to
        "發話者是 v3.20 之後才開始記錄的。之前累積的詞沒有這份資料，新收到的訊息才會有。",
    "怎麼知道是誰打給我的？" to
        "到總覽的「通話統計」，點「對方打來」或「未接」，會列出這段期間打給你的人與通數；" +
        "點「我打出去」則是你主動打給誰。要看每一通的時間，到「設定 → 逐筆檢視紀錄」。",
    "為什麼只看得到別人打給我的電話？" to
        "舊版只認得未接來電的通知，所以自己打出去的不會被記錄。v3.31 起，只要通話中會掛常駐通知（LINE、Messenger 與手機原生電話都會），" +
        "打出去的也會記下來，通話統計裡可以看到「對方打來」與「我打出去」各幾通。更新前的舊紀錄補不回來，會算在「分不出方向」。",
    "LINE 群組被當成個人了？" to
        "LINE 的群組通知常常沒有帶群組標記，App 會用「對話名稱跟發言人不同」「名稱後面有人數」來判斷，還是可能漏掉。" +
        "長按那個聯絡人 →「改成群組聊天室」即可，之後這個名稱的所有紀錄都算群組；判斷錯成群組的也能用同一處改回個人。",
    "想回報問題或給建議？" to
        "到「設定 → 意見回饋」，選類型、寫幾句話，App 會把手機型號、Android 版本與 App 版本一起帶進郵件草稿，" +
        "用你自己的郵件 App 寄給開發者。不會附上你的聊天紀錄或任何統計資料。",
    "晚上沒收到回顧通知？" to
        "今天沒有跟真人的互動就不會發。另外請確認通知權限有開，並把本 App 從省電限制中排除，否則系統可能延後或擋掉。"
)

/** 說明頁裡可展開的一段 */
@Composable
private fun GuideExpandable(no: String, title: String, body: @Composable ColumnScope.() -> Unit) {
    var open by rememberSaveable(no + title) { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                .clickable { open = !open }
                .padding(vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (no.isNotBlank()) {
                Text(no, fontSize = T.Label, fontWeight = FontWeight.Black, color = AppColors.TextTertiary,
                    maxLines = 1, softWrap = false, modifier = Modifier.widthIn(min = 20.dp))
            }
            Text(title, fontSize = T.Body, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary,
                lineHeight = 20.sp, modifier = Modifier.weight(1f).padding(end = 8.dp))
            Text(if (open) "\u2013" else "+", fontSize = T.Title, fontWeight = FontWeight.Bold,
                color = AppColors.TextTertiary, maxLines = 1, softWrap = false)
        }
        if (open) {
            Column(modifier = Modifier.fillMaxWidth().padding(start = if (no.isBlank()) 0.dp else 20.dp, bottom = 10.dp)) { body() }
        }
    }
}

/**
 * 使用說明（整頁）。
 *
 * ⚠️ 這裡以前是 AlertDialog。八個章節塞進一個彈窗，內容被壓在 460dp 的框裡，
 *    大字體下一次只看得到一段，而且看不到「總共有哪些主題」。
 *    改成整頁 + 每段預設收合：一眼掃完所有標題，要看哪段再展開。
 * ⚠️ 展開狀態用 rememberSaveable：翻到一半轉螢幕或被系統回收，不該全部收回去。
 */
@Composable
fun UserGuideScreen(onBack: () -> Unit) {
    BackHandler { onBack() }
    Column(modifier = Modifier.fillMaxSize().background(AppColors.Background)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(AppColors.Surface)
                .padding(horizontal = 6.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("\u2039", fontSize = 24.sp, color = AppColors.TextPrimary, maxLines = 1, softWrap = false,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onBack() }
                    .padding(horizontal = 12.dp, vertical = 2.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("使用說明", fontWeight = FontWeight.Black, fontSize = 18.sp,
                    letterSpacing = (-0.3).sp, color = AppColors.TextPrimary)
                Text("RelateStats v${BuildConfig.VERSION_NAME}", fontSize = T.Micro, color = AppColors.TextTertiary)
            }
        }
        HorizontalDivider(color = AppColors.BorderLight)

        LazyColumn(
            contentPadding = PaddingValues(D.Gutter),
            verticalArrangement = Arrangement.spacedBy(D.Gap)
        ) {
            item {
                Text("常見問題", fontSize = T.Title, fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary, modifier = Modifier.padding(top = 2.dp, bottom = 2.dp))
            }
            item {
                SettingsCard {
                    USER_FAQ.forEachIndexed { i, (q, a) ->
                        if (i > 0) SettingsDivider()
                        Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                            GuideExpandable("", q) {
                                Text(a, fontSize = T.Label, color = AppColors.TextSecondary, lineHeight = 19.sp)
                            }
                        }
                    }
                }
            }
            item {
                Text("運作原理", fontSize = T.Title, fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary, modifier = Modifier.padding(top = 10.dp, bottom = 2.dp))
            }
            item {
                SettingsCard {
                    USER_GUIDE.forEachIndexed { i, sec ->
                        if (i > 0) SettingsDivider()
                        Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                            GuideExpandable(sec.no, sec.title) {
                                sec.paragraphs.forEach { p ->
                                    Text(p, fontSize = T.Label, color = AppColors.TextSecondary,
                                        lineHeight = 19.sp, modifier = Modifier.padding(bottom = 6.dp))
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
fun UserGuideDialog(onDismiss: () -> Unit) {
    AlertDialog(
        containerColor = AppColors.Surface,
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("使用說明", fontWeight = FontWeight.Black, fontSize = 18.sp,
                    letterSpacing = (-0.3).sp, color = AppColors.TextPrimary)
                Text("RelateStats v${BuildConfig.VERSION_NAME}",
                    fontSize = T.Micro, color = AppColors.TextTertiary)
            }
        },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 460.dp)) {
                itemsIndexed(USER_GUIDE, key = { _, sec -> sec.no }) { idx, sec ->
                    if (idx > 0) {
                        HorizontalDivider(color = AppColors.BorderLight,
                            modifier = Modifier.padding(vertical = 14.dp))
                    }
                    Row(verticalAlignment = Alignment.Top) {
                        Text(sec.no, fontSize = T.Label, fontWeight = FontWeight.Black,
                            color = AppColors.TextTertiary, maxLines = 1, softWrap = false,
                            modifier = Modifier.widthIn(min = 18.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(sec.title, fontSize = T.Body, fontWeight = FontWeight.Bold,
                                color = AppColors.TextPrimary, lineHeight = 20.sp)
                            sec.paragraphs.forEach { p ->
                                Text(p, fontSize = T.Label, color = AppColors.TextSecondary,
                                    lineHeight = 19.sp, modifier = Modifier.padding(top = 6.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.BrandPrimary),
                shape = RoundedCornerShape(10.dp)) { Text("關閉") }
        }
    )
}

/** 最近刪除：保留 7 天，可個別還原或立刻永久刪除 */
@Composable
fun TrashDialog(viewModel: RelateViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var batches by remember { mutableStateOf(viewModel.listTrash()) }
    val undoAvailable by viewModel.undoAvailable.collectAsState()
    val undoLabel by viewModel.undoLabel.collectAsState()
    LaunchedEffect(Unit) { viewModel.refreshUndoState() }
    val fmt = remember { SimpleDateFormat("M/d HH:mm", Locale.US) }
    val now = System.currentTimeMillis()
    var query by remember { mutableStateOf("") }
    // ⚠️ 搜尋時才把每一批的內容讀進來。垃圾桶最多只留 7 天，批次數量有限，
    //    但一批可能上千筆，沒在搜尋就不要讀。
    val searching = query.isNotBlank()
    var pendingPurge by remember { mutableStateOf<DataRepository.TrashBatch?>(null) }
    val allDetail = remember(batches, searching) {
        if (searching) batches.associate { it.file to viewModel.peekTrash(it.file) } else emptyMap()
    }

    AlertDialog(
        containerColor = AppColors.Surface,
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("最近刪除", fontWeight = FontWeight.Bold, fontSize = T.Title, color = AppColors.TextPrimary)
                Text("保留 7 天後自動永久移除　·　可整批或單獨還原某個人",
                    fontSize = T.Label, color = AppColors.TextSecondary, lineHeight = 16.sp)
            }
        },
        text = {
            Column {
            // 「復原上一次刪除」跟垃圾桶是同一件事的兩種粒度（整批快照 vs 逐批檔案），
            // 分散在兩個地方使用者根本找不到。跟 Windows 一樣統一收在這裡。
            if (undoAvailable) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                        .background(AppColors.FamilyBg, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("復原上一次操作", fontSize = T.Label, fontWeight = FontWeight.Bold, color = AppColors.FamilyText,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(undoLabel.ifBlank { "回到最近一次刪除之前" }, fontSize = T.Micro,
                            color = AppColors.FamilyText.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text("復原", fontSize = T.Micro, fontWeight = FontWeight.Bold, color = Color.White,
                        maxLines = 1, softWrap = false,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp))
                            .background(AppColors.FamilyText)
                            .clickable {
                                val ok = viewModel.undoLastDestructive()
                                Toast.makeText(context, if (ok) "已復原" else "復原失敗，快照可能已失效", Toast.LENGTH_SHORT).show()
                                batches = viewModel.listTrash()
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp))
                }
            }
            if (batches.isEmpty()) {
                Text("目前沒有可還原的紀錄。", fontSize = T.Body, color = AppColors.TextSecondary)
            } else {
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    placeholder = { Text("搜尋刪掉的對象…", fontSize = T.Label) },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = T.Body),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.BrandPrimary,
                        unfocusedBorderColor = AppColors.BorderLight
                    )
                )
                // 搜尋時只留下「批次名稱命中」或「裡面有人命中」的批次
                val shown = if (!searching) batches else batches.filter { b ->
                    b.label.contains(query, true) ||
                        allDetail[b.file].orEmpty().any { it.first.contains(query, true) }
                }
                if (shown.isEmpty()) {
                    Text("找不到「$query」", fontSize = T.Body, color = AppColors.TextSecondary,
                        modifier = Modifier.padding(vertical = 16.dp))
                }
                LazyColumn(modifier = Modifier.heightIn(max = 380.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(shown, key = { it.file }) { b ->
                        val daysLeft = (7 - ((now - b.deletedAt) / 86_400_000L)).coerceAtLeast(0L)
                        // 展開才去讀檔：一批可能上千筆，全部先讀會拖慢開啟
                        var expanded by remember(b.file) { mutableStateOf(false) }
                        // 搜尋中一律展開，並且只顯示命中的對象 —— 不然搜到了還要自己點開找
                        val open = expanded || searching
                        val detail = remember(b.file, open, query, batches) {
                            when {
                                searching -> allDetail[b.file].orEmpty().filter { it.first.contains(query, true) }
                                expanded -> viewModel.peekTrash(b.file)
                                else -> emptyList()
                            }
                        }
                        Column(
                            modifier = Modifier.fillMaxWidth()
                                .background(AppColors.Background, RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                                    .clickable { expanded = !expanded },
                                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(b.label, fontSize = T.Body, fontWeight = FontWeight.Bold,
                                    color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f))
                                Text("${b.count} 筆", fontSize = T.Label, fontWeight = FontWeight.Bold,
                                    color = AppColors.ActiveBlue, maxLines = 1, softWrap = false)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (open) "▲" else "▼", fontSize = T.Micro, color = AppColors.TextTertiary, maxLines = 1, softWrap = false)
                            }
                            Text(fmt.format(Date(b.deletedAt)) + " 刪除　·　剩 $daysLeft 天",
                                fontSize = T.Micro, color = AppColors.TextTertiary)
                            if (open) {
                                Spacer(modifier = Modifier.height(6.dp))
                                if (detail.isEmpty()) {
                                    Text(if (searching) "這一批裡沒有符合的對象" else "讀不到這一批的內容（檔案可能已損毀）",
                                        fontSize = T.Micro, color = AppColors.TextTertiary)
                                } else {
                                    detail.take(30).forEach { (name, cnt, span) ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                                                Text(name, fontSize = T.Label, color = AppColors.TextPrimary,
                                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text("$span　·　$cnt 筆", fontSize = T.Micro,
                                                    color = AppColors.TextTertiary, maxLines = 1, softWrap = false)
                                            }
                                            // 單獨還原這個人：整批還原常常會把不想要的一起帶回來
                                            Text("還原", fontSize = T.Micro, fontWeight = FontWeight.Bold,
                                                color = AppColors.ActiveBlue, maxLines = 1, softWrap = false,
                                                modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                                    .background(AppColors.ActiveBlue.copy(alpha = 0.12f))
                                                    .clickable {
                                                        val n = viewModel.restoreTrashNames(b.file, setOf(name))
                                                        batches = viewModel.listTrash()
                                                        Toast.makeText(context, "已還原「$name」$n 筆", Toast.LENGTH_SHORT).show()
                                                    }
                                                    .padding(horizontal = 9.dp, vertical = 5.dp))
                                        }
                                    }
                                    if (detail.size > 30) {
                                        Text("＋其他 ${detail.size - 30} 位", fontSize = T.Micro, color = AppColors.TextTertiary)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier.weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AppColors.ActiveBlue.copy(alpha = 0.12f))
                                        .clickable {
                                            val n = viewModel.restoreTrash(b.file)
                                            batches = viewModel.listTrash()
                                            Toast.makeText(context, "已還原 $n 筆", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("整批還原", fontSize = T.Label, fontWeight = FontWeight.Bold, color = AppColors.ActiveBlue, maxLines = 1, softWrap = false)
                                }
                                Box(
                                    modifier = Modifier.weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AppColors.Background)
                                        .border(1.dp, AppColors.BorderLight, RoundedCornerShape(8.dp))
                                        // ⚠️ 這一顆是真的救不回來了，必須二次確認並寫明刪的是什麼
                                        .clickable { pendingPurge = b }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("永久刪除", fontSize = T.Label, fontWeight = FontWeight.Bold, color = AppColors.TextSecondary, maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }
                }
            }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("關閉", color = AppColors.TextSecondary) } }
    )

    pendingPurge?.let { b ->
        ConfirmDialog(
            title = "永久刪除這一批",
            message = "「${b.label}」共 ${b.count} 筆會被永久移除，之後無法還原。",
            // ⚠️ remember 住：peekTrash 會讀檔，一批可能上千筆，不能每次重組都跑一遍
            details = remember(b.file) { viewModel.peekTrash(b.file).take(30).map { "${it.first}　${it.third}　${it.second} 筆" } },
            confirmLabel = "永久刪除",
            onConfirm = {
                viewModel.purgeTrash(b.file)
                batches = viewModel.listTrash()
                pendingPurge = null
            },
            onDismiss = { pendingPurge = null }
        )
    }
}

@Composable
fun AdvancedDataManagerDialog(viewModel: RelateViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val weekStartDay by viewModel.weekStartDay.collectAsState()
    var pendingClearScope by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    // 選好範圍後先給預覽：標題、起、迄
    var previewRange by remember { mutableStateOf<Triple<String, Long, Long>?>(null) }

    fun scopeRange(scope: String, day: Long = System.currentTimeMillis()): Triple<String, Long, Long> = when (scope) {
        "TODAY" -> Triple("清除今日資料", startOfDayMs(day), startOfDayMs(day) + 86_399_999L)
        "WEEK" -> Triple(
            "清除本週資料",
            getStartOfWeekTimestamp(day, weekStartDay),
            getEndOfWeekTimestamp(day, weekStartDay)
        )
        "DAY" -> Triple("清除該日資料", startOfDayMs(day), startOfDayMs(day) + 86_399_999L)
        else -> Triple("清除全部的互動紀錄", 0L, Long.MAX_VALUE)
    }

    if (showDatePicker) {
        AlertDialog(
            containerColor = AppColors.Surface, onDismissRequest = { showDatePicker = false },
            title = { Text("請選擇要清除的日期", fontWeight = FontWeight.Bold) },
            text = {
                Box(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    // ⚠️ 以前點下去就直接刪，沒有任何確認。改成先進預覽。
                    MinimalCalendar(weekStartDay = weekStartDay, selectedTime = System.currentTimeMillis(), onDateSelected = { timestamp ->
                        previewRange = scopeRange("DAY", timestamp)
                        showDatePicker = false
                    })
                }
            },
            confirmButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消", color = AppColors.TextSecondary) } }
        )
    } else if (previewRange != null) {
        val (title, start, end) = previewRange!!
        val rows = remember(start, end) { viewModel.previewRange(start, end) }
        ClearPreviewDialog(
            title = title,
            rows = rows,
            onConfirm = { keep ->
                val n = viewModel.clearInteractionsInRange(start, end, keep)
                Toast.makeText(context, "已刪除 $n 筆（可在設定頁復原）", Toast.LENGTH_SHORT).show()
                previewRange = null
                onDismiss()
            },
            onDismiss = { previewRange = null }
        )
    } else if (pendingClearScope != null) {
        LaunchedEffect(pendingClearScope) {
            previewRange = scopeRange(pendingClearScope!!)
            pendingClearScope = null
        }
    } else {
        AlertDialog(
            containerColor = AppColors.Surface, onDismissRequest = onDismiss,
            // ⚠️ 使用者不敢點的原因是「不知道會刪到什麼」。
            //    舊版寫「淨化」「徹底重置所有標籤與連線資料」—— 聽起來像會把標籤跟合併全部清掉，
            //    但實際上只刪互動紀錄，而且會先預覽、會進最近刪除。事實比文案安全得多，要照實寫。
            title = {
                Column {
                    Text("清除互動紀錄", fontWeight = FontWeight.Black, fontSize = 18.sp, color = AppColors.TextPrimary)
                    Text("先選範圍，下一步會列出清單讓你確認，還不會刪",
                        fontSize = T.Micro, color = AppColors.TextTertiary, lineHeight = 15.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .background(AppColors.SurfaceAlt, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text("會刪掉：範圍內的訊息數、停留時間、通話紀錄", fontSize = T.Label,
                            fontWeight = FontWeight.Bold, color = AppColors.TextPrimary, lineHeight = 17.sp)
                        Text("不會動：標籤、合併的帳號、改過的名稱、常用詞", fontSize = T.Label,
                            color = AppColors.TextSecondary, lineHeight = 17.sp, modifier = Modifier.padding(top = 2.dp))
                        Text("刪掉的會進「最近刪除」保留 7 天，期間可整批或單人還原。", fontSize = T.Micro,
                            color = AppColors.TextTertiary, lineHeight = 14.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                    ClearScopeOption("今天", "今天 0:00 到現在的紀錄") { pendingClearScope = "TODAY" }
                    ClearScopeOption("本週", "這一週的紀錄（依你設定的一週起始日）") { pendingClearScope = "WEEK" }
                    ClearScopeOption("指定某一天", "從月曆挑一天") { showDatePicker = true }
                    ClearScopeOption("全部的紀錄", "所有日期的互動紀錄；標籤與合併仍然保留", danger = true) { pendingClearScope = "ALL" }
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("關閉", color = AppColors.TextSecondary) } }
        )
    }
}

/** 清除資料的一個範圍選項：標題＋一句說明範圍，右邊 › 表示還有下一步 */
@Composable
private fun ClearScopeOption(title: String, sub: String, danger: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, if (danger) AppColors.WorkText.copy(alpha = 0.35f) else AppColors.BorderLight, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, fontSize = T.Body, fontWeight = FontWeight.Bold,
                color = if (danger) AppColors.WorkText else AppColors.TextPrimary)
            Text(sub, fontSize = T.Micro, color = AppColors.TextTertiary, lineHeight = 14.sp)
        }
        Text("›", fontSize = 17.sp, color = AppColors.TextTertiary, maxLines = 1, softWrap = false)
    }
}

@Composable
fun SettingsView(viewModel: RelateViewModel) {
    val context = LocalContext.current
    val (hasNotif, setHasNotif) = remember { mutableStateOf(isNotificationServiceEnabled(context)) }
    val (hasAccel, setHasAccel) = remember { mutableStateOf(isAccessibilityServiceEnabled(context, ChatTrackerService::class.java)) }
    val (hasUsageStats, setHasUsageStats) = remember { mutableStateOf(hasUsageStatsPermission(context)) }

    // 開權限一律先經過明確揭露（見 PermissionDisclosureDialog）
    var disclosureKind by remember { mutableStateOf<String?>(null) }
    disclosureKind?.let { k ->
        PermissionDisclosureDialog(k, onAgree = { disclosureKind = null; openPermissionSettings(context, k) },
            onDismiss = { disclosureKind = null })
    }
    var showFeedback by remember { mutableStateOf(false) }
    if (showFeedback) FeedbackDialog { showFeedback = false }
    val cloudUri by viewModel.cloudBackupUri.collectAsState()
    val cloudLast by viewModel.cloudBackupLast.collectAsState()
    val cloudInterval by viewModel.cloudBackupInterval.collectAsState()
    val cloudLocation by viewModel.cloudBackupLocation.collectAsState()
    var confirmUnbindCloud by remember { mutableStateOf(false) }
    if (confirmUnbindCloud) {
        AlertDialog(
            containerColor = AppColors.Surface,
            onDismissRequest = { confirmUnbindCloud = false },
            title = { Text("解除雲端備份？", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
            text = {
                Text("之後不會再自動更新雲端上的備份檔。已經在雲端硬碟裡的檔案不會被刪除，之後仍然可以用來還原。",
                    fontSize = T.Body, color = AppColors.TextSecondary, lineHeight = 20.sp)
            },
            confirmButton = {
                TextButton(onClick = { viewModel.unbindCloudBackup(context); confirmUnbindCloud = false }) {
                    Text("解除綁定", color = AppColors.WorkText, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { confirmUnbindCloud = false }) { Text("取消", color = AppColors.TextSecondary) } }
        )
    }

    val (showDataManager, setShowDataManager) = remember { mutableStateOf(false) }
    val (showGuideDialog, setShowGuideDialog) = remember { mutableStateOf(false) }

    val weekStartDay by viewModel.weekStartDay.collectAsState()
    val chartPaletteId by viewModel.chartPaletteId.collectAsState()

    val records by viewModel.interactions.collectAsState()
    val availableCats by viewModel.availableCategories.collectAsState()
    var showCategoryManager by remember { mutableStateOf(false) }
    var showRecordManager by remember { mutableStateOf(false) }
    var confirmAction by remember { mutableStateOf<String?>(null) }
    var showDiagnostics by remember { mutableStateOf(false) }
    var showTrash by remember { mutableStateOf(false) }
    var showBlocked by remember { mutableStateOf(false) }
    var showKept by remember { mutableStateOf(false) }
    var showPalette by remember { mutableStateOf(false) }
    val recapEnabled by viewModel.recapEnabled.collectAsState()
    val keptNow by viewModel.junkIgnored.collectAsState()
    // 每次進設定頁重讀一次；統計出錯時 lastErrorTag 會變動，跟著重讀
    val errTag by viewModel.lastErrorTag.collectAsState()
    var crashReport by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(errTag) { crashReport = viewModel.readLastCrash() }
    val blockedNow by viewModel.blockedNames.collectAsState()
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

    // 綁定雲端備份：在選擇器的儲存位置挑「雲端硬碟」建立檔案
    val cloudBindLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) {
            ioScope.launch {
                val r = withContext(Dispatchers.IO) { viewModel.bindCloudBackup(context, uri) }
                Toast.makeText(context, when (r) {
                    0 -> "已綁定，預設每天自動備份，可以在設定頁調整週期"
                    1 -> "已備份這一次，但這個位置不允許自動更新，請改選 Google 雲端硬碟"
                    else -> "備份失敗，請換個位置再試"
                }, Toast.LENGTH_LONG).show()
            }
        }
    }

    // 版面規則（整頁一致，不要再各寫各的）：
    //   · 容器一律 SettingsCard —— Surface 底、1dp 邊框、14dp 圓角、無陰影
    //   · 群組之間用 SettingsSection 的小標分隔，不靠留白硬撐
    //   · 每一列 = 標題(14sp) + 說明(11sp) + 右側狀態或 ›
    //   · 破壞性動作一律紅字並排在該區最後
    // ⚠️ 使用說明是整頁不是彈窗，但**不要**用「早退」的寫法把設定頁換掉 ——
    //    早退等於整棵子樹被丟掉，回來時設定頁的捲動位置與所有本地狀態都會重置。
    //    改成蓋在上面：說明頁自己有不透明底色，設定頁還在下面原封不動。
    Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            // ⚠️ 標題與第一個小標之間本來疊了三層留白：
            //    標題 bottom 6 + LazyColumn spacedBy 8 + SettingsSection top 12 = 26dp。
            //    標題自己不再帶下留白，小標的上留白也收到 8。
            Text("設定", fontWeight = FontWeight.Black, fontSize = 24.sp, letterSpacing = (-0.6).sp,
                color = AppColors.TextPrimary, modifier = Modifier.padding(top = 4.dp))
        }

        // ── 服務狀態：這是全 App 最重要的一張卡，關掉就什麼都不會記錄 ──
        item { SettingsSection("服務") }
        item {
            SettingsCard {
                PermissionRow("通知存取", "計算訊息數與常用詞", hasNotif) { disclosureKind = "notif" }
                SettingsDivider()
                PermissionRow("無障礙服務", "計算聊天室停留時間", hasAccel) { disclosureKind = "access" }
                SettingsDivider()
                PermissionRow("使用紀錄存取", "計算手機總用量佔比", hasUsageStats) { disclosureKind = "usage" }
                if (!hasUsageStats) {
                    Text(
                        "找不到本 App 的話，到系統「應用程式」裡點右上角「允許受限制的設定」",
                        fontSize = T.Micro, color = AppColors.WorkText, lineHeight = 14.sp,
                        modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp)
                    )
                }
                SettingsDivider()
                SettingsRow("使用說明", "常見問題與運作原理") { setShowGuideDialog(true) }
                SettingsDivider()
                SettingsRow("隱私權政策", "讀取哪些資料、存在哪裡") {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL))) }
                }
                SettingsDivider()
                SettingsRow("意見回饋", "回報問題或提建議") { showFeedback = true }
            }
        }

        // ── 偏好 ──
        item { SettingsSection("偏好") }
        item {
            SettingsCard {
                // 設定頁上只放一列「現在是哪組」，挑色在彈窗裡做 —— 那裡才看得到實際套用的樣子
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clickable { showPalette = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                        Text("圖表與標籤配色", fontSize = T.Body, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                        Text("App 介面維持原本的配色", fontSize = T.Micro, color = AppColors.TextTertiary, lineHeight = 14.sp)
                    }
                    Row(modifier = Modifier.width(64.dp).height(18.dp).clip(RoundedCornerShape(4.dp))) {
                        AppColors.Chart.colors.forEach { c ->
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(c))
                        }
                    }
                    Text("›", fontSize = 17.sp, color = AppColors.TextTertiary,
                        maxLines = 1, softWrap = false, modifier = Modifier.padding(start = 6.dp))
                }
                SettingsDivider()
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clickable { viewModel.setRecapEnabled(!recapEnabled) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("每晚回顧通知", fontSize = T.Body, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                        Text("晚上九點左右整理今天的互動；今天沒互動就不發",
                            fontSize = T.Micro, color = AppColors.TextTertiary, lineHeight = 14.sp)
                    }
                    Switch(checked = recapEnabled, onCheckedChange = { viewModel.setRecapEnabled(it) })
                }
                SettingsDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("一週起始日", fontSize = T.Body, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                        Text("影響單週範圍、「哪幾天在聊」與「幾點在聊」", fontSize = T.Micro, color = AppColors.TextTertiary, lineHeight = 14.sp)
                    }
                    Row(modifier = Modifier.background(AppColors.Background, RoundedCornerShape(8.dp)).padding(2.dp)) {
                        listOf(Calendar.MONDAY to "週一", Calendar.SUNDAY to "週日").forEach { (day, label) ->
                            val on = weekStartDay == day
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                    .background(if (on) AppColors.BrandPrimary else Color.Transparent)
                                    .clickable { viewModel.setWeekStartDay(day) }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(label, fontSize = T.Label,
                                    color = if (on) Color.White else AppColors.TextSecondary,
                                    fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }
                SettingsDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showCategoryManager = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("標籤管理", fontSize = T.Body, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                        Text("改名、換色、調整順序、新增或刪除", fontSize = T.Micro, color = AppColors.TextTertiary, lineHeight = 14.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        availableCats.take(4).forEach { cat ->
                            Box(modifier = Modifier.size(12.dp).background(cat.bgColor, CircleShape).border(1.dp, cat.color, CircleShape))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("›", fontSize = 18.sp, color = AppColors.TextTertiary, maxLines = 1, softWrap = false)
                }
            }
        }

        // ── 備份 ──
        item { SettingsSection("備份") }
        item {
            SettingsCard {
                Column(modifier = Modifier.padding(14.dp)) {
                    if (cloudUri != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AppColors.FriendText))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("雲端備份已綁定", fontSize = T.Body, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                        }

                        // 綁在哪裡：使用者要能確認自己真的綁到雲端，而不是手機裡的某個資料夾
                        val cloudAuth = remember(cloudUri) { runCatching { Uri.parse(cloudUri).authority.orEmpty() }.getOrDefault("") }
                        val isOnPhone = cloudAuth.startsWith("com.android.externalstorage") ||
                            cloudAuth.startsWith("com.android.providers.downloads") ||
                            cloudAuth.startsWith("com.android.providers.media")
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(10.dp))
                                .background(AppColors.Background).padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text("備份位置", fontSize = T.Micro, color = AppColors.TextTertiary)
                            Text(cloudLocation ?: "已選擇的位置", fontSize = T.Body, fontWeight = FontWeight.Bold,
                                color = AppColors.TextPrimary, lineHeight = 20.sp)
                            Text(
                                if (cloudLast > 0) "上次備份 " + SimpleDateFormat("yyyy/M/d HH:mm", Locale.TAIWAN).format(Date(cloudLast))
                                else "尚未備份",
                                fontSize = T.Micro, color = AppColors.TextTertiary, modifier = Modifier.padding(top = 2.dp)
                            )
                            if (isOnPhone) {
                                Text("⚠ 這個位置在手機裡，換機或重灌時不會跟著走。建議按「更換位置」改選雲端硬碟。",
                                    fontSize = T.Micro, color = AppColors.WorkText, lineHeight = 15.sp,
                                    modifier = Modifier.padding(top = 6.dp))
                            }
                        }

                        // 自動備份週期
                        Text("自動備份", fontSize = T.Label, fontWeight = FontWeight.Black, color = AppColors.TextSecondary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().background(AppColors.Background, RoundedCornerShape(8.dp)).padding(2.dp)
                        ) {
                            listOf(1 to "每天", 3 to "每 3 天", 7 to "每週", 0 to "不自動").forEach { (days, label) ->
                                val on = cloudInterval == days
                                Box(
                                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(6.dp))
                                        .background(if (on) AppColors.BrandPrimary else Color.Transparent)
                                        .clickable { viewModel.setCloudBackupInterval(days) }
                                        .padding(vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(label, fontSize = T.Label, maxLines = 1, softWrap = false,
                                        color = if (on) Color.White else AppColors.TextSecondary,
                                        fontWeight = if (on) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                        Text(
                            if (cloudInterval == 0) "不會自動更新，只在你按「立即備份」時更新。"
                            else "每晚 9 點左右檢查，距離上次備份滿 ${if (cloudInterval == 1) "1 天" else if (cloudInterval == 7) "7 天" else "$cloudInterval 天"}就自動更新。",
                            fontSize = T.Micro, color = AppColors.TextTertiary, lineHeight = 14.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    ioScope.launch {
                                        val ok = withContext(Dispatchers.IO) { viewModel.backupToCloudNow(context) }
                                        Toast.makeText(context,
                                            if (ok) "已更新雲端備份" else "備份失敗：檔案可能被移動或刪除，請按「更換位置」重新綁定",
                                            Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.BrandPrimary),
                                shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(vertical = 10.dp)
                            ) { Text("立即備份", fontSize = T.Label, maxLines = 1, softWrap = false) }
                            OutlinedButton(
                                onClick = { cloudBindLauncher.launch("RelateStats_雲端備份.txt") },
                                modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) { Text("更換位置", fontSize = T.Label, color = AppColors.TextPrimary, maxLines = 1, softWrap = false) }
                        }
                        Text("解除綁定", fontSize = T.Label, fontWeight = FontWeight.Bold, color = AppColors.WorkText,
                            maxLines = 1, softWrap = false,
                            modifier = Modifier.padding(top = 6.dp).clip(RoundedCornerShape(6.dp))
                                .clickable { confirmUnbindCloud = true }.padding(horizontal = 4.dp, vertical = 6.dp))
                    } else {
                        Text("所有資料只存在這支手機。綁定 Google 雲端硬碟後會定期自動把備份更新到雲端（週期可調），換機或重灌都不怕。",
                            fontSize = T.Label, color = AppColors.TextSecondary, lineHeight = 17.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { cloudBindLauncher.launch("RelateStats_雲端備份.txt") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.BrandPrimary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) { Text("☁  綁定 Google 雲端硬碟", fontSize = T.Body, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) }
                        Text("按下後，在儲存位置選「雲端硬碟」再按儲存。",
                            fontSize = T.Micro, color = AppColors.TextTertiary, lineHeight = 14.sp,
                            modifier = Modifier.padding(top = 6.dp))
                    }

                    HorizontalDivider(color = AppColors.BorderLight, modifier = Modifier.padding(vertical = 12.dp))
                    Text("手動備份", fontSize = T.Label, fontWeight = FontWeight.Black, color = AppColors.TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { exportLauncher.launch("RelateStats_Backup.txt") },
                            modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) { Text("存成檔案", fontSize = T.Label, color = AppColors.TextPrimary, maxLines = 1, softWrap = false) }
                        OutlinedButton(
                            onClick = { importLauncher.launch(arrayOf("text/plain")) },
                            modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) { Text("從檔案還原", fontSize = T.Label, color = AppColors.ActiveBlue, maxLines = 1, softWrap = false) }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    // ⚠️ 這裡要講清楚「App 自己沒有網路」，否則使用者會以為綁了雲端就代表 App 會上網。
                    Text("換機時在新手機按「從檔案還原」，到雲端硬碟選那份備份檔。App 本身沒有網路權限，上傳由 Google 雲端硬碟 App 代為處理。",
                        fontSize = T.Micro, color = AppColors.TextTertiary, lineHeight = 14.sp)
                }
            }
        }

        // ── 資料 ──
        item { SettingsSection("資料") }

        // 「上次發生錯誤」卡片已拿掉（v3.24）：一般使用者看不懂堆疊，
        // 那張紅卡只會讓人以為 App 壞了。錯誤仍然會記在內部，需要時再開回來。
        if (autoCleanedCount > 0 || abnormalCount > 0) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .background(if (abnormalCount > 0) AppColors.WorkBg else AppColors.FriendBg, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    if (abnormalCount > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text("$abnormalCount 筆異常紀錄", fontSize = T.Label, fontWeight = FontWeight.Bold, color = AppColors.WorkText)
                                Text("單筆停留超過 30 分鐘上限", fontSize = T.Micro,
                                    color = AppColors.WorkText.copy(alpha = 0.8f), lineHeight = 14.sp)
                            }
                            Text("清理", fontSize = T.Micro, fontWeight = FontWeight.Bold, color = Color.White,
                                maxLines = 1, softWrap = false,
                                modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                    .background(AppColors.WorkText)
                                    .clickable { confirmAction = "abnormal" }
                                    .padding(horizontal = 10.dp, vertical = 6.dp))
                        }
                    } else {
                        Text("已自動清理 $autoCleanedCount 筆舊版計時錯誤的異常紀錄",
                            fontSize = T.Micro, color = AppColors.FriendText, lineHeight = 15.sp)
                    }
                }
            }
        }

        item {
            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("紀錄總數", fontSize = T.Body, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                    Text("${records.size} 筆", fontSize = T.Body, fontWeight = FontWeight.Bold,
                        color = AppColors.TextSecondary, maxLines = 1, softWrap = false)
                }
                SettingsDivider()
                SettingsRow("整理系統通知", "把讚、追蹤、推播歸到「系統通知」") { confirmAction = "retag" }
                SettingsDivider()
                SettingsRow("清掉常用詞裡的雜訊", "移除系統通知與人名碎片") { confirmAction = "purgeWords" }
            }
        }

        item {
            SettingsCard {
                SettingsRow(
                    "最近刪除",
                    if (undoAvailable) "保留 7 天　·　有可復原的操作" else "保留 7 天，可還原"
                ) { showTrash = true }
                SettingsDivider()
                SettingsRow("逐筆檢視紀錄") { showRecordManager = true }
                if (blockedNow.isNotEmpty()) {
                    SettingsDivider()
                    SettingsRow("永不記錄名單", "${blockedNow.size} 位") { showBlocked = true }
                }
                if (keptNow.isNotEmpty()) {
                    SettingsDivider()
                    SettingsRow("已保留的來源", "${keptNow.size} 個，不再列入非真人提醒") { showKept = true }
                }
                SettingsDivider()
                SettingsRow("清除資料", "今日／本週／指定日期／全部", danger = true) {
                    setShowDataManager(true)
                }
                SettingsDivider()
                SettingsRow("清除常用詞", "${wordFreqNow.size} 個詞全部歸零", danger = true) {
                    confirmAction = "words"
                }
            }
        }

        item {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("RelateStats v${BuildConfig.VERSION_NAME}", fontSize = T.Micro,
                    color = AppColors.TextTertiary, fontWeight = FontWeight.Medium)
                Text("© ${Calendar.getInstance().get(Calendar.YEAR)} RelateStats",
                    fontSize = T.Micro, color = AppColors.TextTertiary.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 2.dp))
            }
        }
    }

    if (showCategoryManager) {
        CategoryManagerDialog(viewModel, availableCats) { showCategoryManager = false }
    }

    // ⚠️ 以前只有 setShowDataManager(true)，卻沒有任何地方讀這個旗標 ——
    //    「清除資料」按了完全沒反應。
    if (showDataManager) {
        AdvancedDataManagerDialog(viewModel) { setShowDataManager(false) }
    }

    if (showTrash) {
        TrashDialog(viewModel) { showTrash = false }
    }

    if (showPalette) {
        PaletteDialog(
            currentId = chartPaletteId,
            onPick = { viewModel.setChartPalette(it) },
            onDismiss = { showPalette = false }
        )
    }

    if (showKept) {
        KeptSourcesDialog(viewModel) { showKept = false }
    }

    if (showBlocked) {
        BlockedNamesDialog(viewModel) { showBlocked = false }
    }

    if (showRecordManager) {
        RecordManagerDialog(viewModel) { showRecordManager = false }
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
        "purgeWords" -> {
            // ⚠️ remember 住，不然對話框每次重組都重掃一次詞頻表
            val preview = remember { viewModel.previewJunkWords() }
            WordPurgeDialog(
                words = preview,
                onConfirm = { picked ->
                    val n = viewModel.purgeWords(picked)
                    Toast.makeText(context, if (n > 0) "已清掉 $n 個詞" else "沒有清掉任何詞", Toast.LENGTH_SHORT).show()
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
        UserGuideScreen { setShowGuideDialog(false) }
    }
    }
}

/**
 * 權限狀態列。
 * 狀態用「色點 + 短字」而不是一行純文字 —— 三個權限並排時，
 * 有沒有開必須一眼掃完，不能逐行讀。
 */
@Composable
fun PermissionRow(title: String, subtitle: String, isGranted: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, fontSize = T.Body, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, fontSize = T.Micro, color = AppColors.TextTertiary, lineHeight = 14.sp)
        }
        // ⚠️ 不要用「綠色膠囊 + 色點」表示正常狀態。
        //    正常本來就該安靜 —— 三個權限都開著時，三顆綠膠囊反而變成畫面上最搶眼的東西。
        //    已開啟＝低調灰字；未開啟＝橘字加箭號，把注意力留給真正需要動作的那一行。
        if (isGranted) {
            Text("已開啟", color = AppColors.TextTertiary, fontSize = T.Label,
                maxLines = 1, softWrap = false)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("未開啟", color = AppColors.WorkText, fontWeight = FontWeight.Bold,
                    fontSize = T.Label, maxLines = 1, softWrap = false)
                Text("›", fontSize = 17.sp, color = AppColors.WorkText,
                    maxLines = 1, softWrap = false, modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
}

/**
 * 隱私權政策網址（GitHub Pages，原始檔在 docs/privacy-policy.html）。
 * ⚠️ Play Console 填的網址必須跟這裡一樣。
 */
const val PRIVACY_POLICY_URL = "https://afortunadofu-bot.github.io/RelateStats/privacy-policy.html"

/** 開系統權限頁。⚠️ 一律先經過 PermissionDisclosureDialog，不要直接呼叫。 */
fun openPermissionSettings(context: Context, kind: String) {
    val action = when (kind) {
        "notif" -> Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
        "access" -> Settings.ACTION_ACCESSIBILITY_SETTINGS
        else -> Settings.ACTION_USAGE_ACCESS_SETTINGS
    }
    runCatching { context.startActivity(Intent(action)) }
}

/**
 * 權限的明確揭露（Google Play「使用者資料」政策要求的 Prominent Disclosure）。
 *
 * ⚠️ 規定：把使用者送去系統設定開權限**之前**，必須在 App 內用專門的畫面講清楚
 *    讀什麼、拿來做什麼、存在哪、會不會分享，而且要使用者主動按「同意」。
 *    只寫在隱私權政策、或引導頁的一句話都不算數。
 * ⚠️ 所有開權限的入口都要走這裡（設定頁、概況頁的警告卡）。新增入口時不要直接 startActivity。
 * ⚠️ 文字要跟 docs/play/SUBMISSION.md 的申報內容、docs/privacy-policy.html 一致 —— 審核會對照。
 */
@Composable
fun PermissionDisclosureDialog(kind: String, onAgree: () -> Unit, onDismiss: () -> Unit) {
    val (title, items) = when (kind) {
        "notif" -> "通知存取" to listOf(
            "會讀取" to "LINE、Messenger、Instagram、WhatsApp、Telegram、Threads 與電話 App 的通知：寄件人名稱、App 名稱、時間與訊息文字。",
            "用來" to "計算你跟每個人的訊息數、聊天時段與常用詞。",
            "訊息文字" to "只拆成詞並累計次數，不保存訊息原文。",
            "其他 App" to "的通知會直接略過，不記錄。"
        )
        "access" -> "無障礙服務" to listOf(
            "會讀取" to "你開著 LINE、Messenger、Instagram、WhatsApp、Telegram、Threads 或電話 App 時，畫面頂端的聊天室名稱。",
            "用來" to "計算你在每個聊天室停留多久。",
            "不會" to "保存訊息內容，也不會操作手機、代替你點擊或輸入。在其他 App 裡不運作。",
            "說明" to "本 App 不是身障輔助工具，只借用無障礙服務辨識你正在看哪個聊天室。"
        )
        else -> "使用紀錄存取" to listOf(
            "會讀取" to "各 App 在前景使用的時間長度。",
            "用來" to "計算聊天 App 佔你手機總使用時間的比例。"
        )
    }
    AlertDialog(
        containerColor = AppColors.Surface,
        onDismissRequest = onDismiss,
        title = { Text("開啟「$title」前請先了解", fontWeight = FontWeight.Black, fontSize = 17.sp, color = AppColors.TextPrimary) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                items.forEach { (k, v) ->
                    Row(modifier = Modifier.padding(bottom = 10.dp)) {
                        Text(k, fontSize = T.Label, fontWeight = FontWeight.Black, color = AppColors.TextSecondary,
                            modifier = Modifier.width(64.dp))
                        Text(v, fontSize = T.Body, color = AppColors.TextPrimary, lineHeight = 20.sp, modifier = Modifier.weight(1f))
                    }
                }
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp).clip(RoundedCornerShape(10.dp))
                        .background(AppColors.FriendBg).padding(10.dp)
                ) {
                    Text("資料只存在這支手機", fontSize = T.Label, fontWeight = FontWeight.Black, color = AppColors.FriendText)
                    Text("本 App 沒有網路權限，無法把資料傳出手機，開發者也看不到。除非你自己綁定雲端備份，資料不會離開手機。隨時可以到系統設定關閉權限，或在 App 內清除資料。",
                        fontSize = T.Micro, color = AppColors.FriendText, lineHeight = 16.sp)
                }
            }
        },
        confirmButton = {
            Button(onClick = onAgree, colors = ButtonDefaults.buttonColors(containerColor = AppColors.BrandPrimary),
                shape = RoundedCornerShape(10.dp)) { Text("同意並前往設定", maxLines = 1, softWrap = false) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("不同意", color = AppColors.TextSecondary) } }
    )
}

const val FEEDBACK_EMAIL = "celine.fu.work@gmail.com"

/**
 * 意見回饋。
 *
 * ⚠️ App 沒有網路權限，所以不可能自己送出表單 ——
 *    改成幫使用者把內容與裝置資訊寫好，交給他自己的郵件 App 寄出，
 *    沒有郵件 App 時退而求其次複製到剪貼簿。
 *    不要為了做表單去加 INTERNET，那會毀掉整個隱私說法與資料安全性申報。
 */
@Composable
fun FeedbackDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val kinds = listOf("問題回報", "功能建議", "數字不對", "其他")
    var kind by remember { mutableStateOf(kinds[0]) }
    var text by remember { mutableStateOf("") }
    val deviceInfo = remember {
        "裝置：" + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL +
            "\nAndroid：" + android.os.Build.VERSION.RELEASE + "（API " + android.os.Build.VERSION.SDK_INT + "）" +
            "\nApp：v" + BuildConfig.VERSION_NAME
    }
    fun send() {
        val body = text.trim() + "\n\n———\n" + deviceInfo
        val mail = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + FEEDBACK_EMAIL)).apply {
            putExtra(Intent.EXTRA_SUBJECT, "RelateStats 回饋：$kind")
            putExtra(Intent.EXTRA_TEXT, body)
        }
        val ok = runCatching { context.startActivity(mail) }.isSuccess
        if (!ok) {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            cm?.setPrimaryClip(android.content.ClipData.newPlainText("RelateStats 回饋", "[$kind] " + body))
            Toast.makeText(context, "找不到郵件 App，內容已複製，請貼給 $FEEDBACK_EMAIL", Toast.LENGTH_LONG).show()
        }
        onDismiss()
    }
    AlertDialog(
        containerColor = AppColors.Surface,
        onDismissRequest = onDismiss,
        title = { Text("意見回饋", fontWeight = FontWeight.Black, fontSize = 18.sp, color = AppColors.TextPrimary) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(modifier = Modifier.fillMaxWidth().background(AppColors.Background, RoundedCornerShape(8.dp)).padding(2.dp)) {
                    kinds.forEach { k ->
                        val on = kind == k
                        Box(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(6.dp))
                                .background(if (on) AppColors.BrandPrimary else Color.Transparent)
                                .clickable { kind = k }.padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(k, fontSize = T.Micro, maxLines = 1, softWrap = false,
                                color = if (on) Color.White else AppColors.TextSecondary,
                                fontWeight = if (on) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
                OutlinedTextField(
                    value = text, onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp).heightIn(min = 120.dp),
                    placeholder = { Text("發生什麼事？在哪個畫面？", fontSize = T.Body, color = AppColors.TextTertiary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.BrandPrimary, unfocusedBorderColor = AppColors.BorderLight),
                    shape = RoundedCornerShape(12.dp)
                )
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(10.dp))
                        .background(AppColors.Background).padding(10.dp)
                ) {
                    Text("會一起附上", fontSize = T.Micro, fontWeight = FontWeight.Black, color = AppColors.TextSecondary)
                    Text(deviceInfo, fontSize = T.Micro, color = AppColors.TextTertiary, lineHeight = 16.sp)
                    Text("不會附上你的聊天紀錄、聯絡人或任何統計資料。",
                        fontSize = T.Micro, color = AppColors.TextTertiary, lineHeight = 16.sp,
                        modifier = Modifier.padding(top = 4.dp))
                }
            }
        },
        confirmButton = {
            Button(onClick = { send() }, enabled = text.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.BrandPrimary),
                shape = RoundedCornerShape(10.dp)) { Text("用郵件送出", maxLines = 1, softWrap = false) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = AppColors.TextSecondary) } }
    )
}

/** 通話那一列要寫「誰打的」。抓不到方向就只寫「通話」，不要猜。 */
fun callDirectionLabel(r: InteractionRecord): String = when (r.callDirection) {
    1 -> "對方打來"
    2 -> "我打出去"
    else -> "通話"
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