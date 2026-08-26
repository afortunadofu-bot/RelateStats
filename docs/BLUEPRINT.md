# RelateStats 架構與邏輯藍圖

> 原始規劃文件（社交app.docx，2026-03-30）轉存，作為需求與驗收基準。

---

我想用androind studio開發一款社交資料記錄應用程式除了可以自己觀看之外還具有病毒式散播的功用，初步構想是能夠記錄每個人在社群軟體上對應不同人的社交量，目前應用程式構想如下，幫我寫了androind studio程式碼，有想到的所有點子都可以更好地將其完善。
RelateStats 架構與邏輯藍圖 
📌 1. 專案核心概念與技術棧
App 核心目標：在不 Root 手機、不駭入資料庫的前提下，透過「通知」與「無障礙服務」，精準計算使用者在通訊軟體上的「注意力停留時間」與「訊息互動頻率」，並進行人格分析，並進行社交數據洞察分析。
技術棧：Android 原生開發 (Kotlin) + Jetpack Compose (100% 宣告式 UI) + ViewModel + StateFlow。
資料儲存：全本地端 SharedPreferences。採用自定義 CSV 格式字串儲存，並提供 Base64 加密的匯出/匯入功能。

🏛️ 2. 核心資料模型 (Data Models)
InteractionRecord (互動紀錄 - 最底層核心)：
包含：id, rawSenderName (原始抓到的名字), appName, isIncoming (收/發), wordCount (字數), durationSeconds (停留秒數), isGroup (是否為群組), timestamp, isMissedCall (未接來電), isCall (通話), subSender (群組內的具體發言人)。
時間單位是秒 (Int) 
SenderStat (UI 顯示用統計模型)：
將同一個 unifiedName (合併後的名稱) 的多筆 Record 聚合。包含總秒數、總字數、群組子成員發言次數字典 (Map<String, Int>)。
ContactCategory (聯絡人分類)：
內建 工作、家人、朋友、未分類。支援自訂標籤 (亂數分配特定色系)。

⚙️ 3. 雙引擎資料收集邏輯 (極度重要)
引擎 A：通知攔截器 (NotificationListenerService)
觸發時機：收到 LINE, IG, Messenger,threads, 電話等通知時。
核心邏輯：
抓取 android.title 與 android.text 計算字數。如果是通話，透過正則表達式抓出 (\d+)分(\d+)秒 並轉為秒數。
群組拆解與防呆：
判斷是否為群組的依據：系統標記 isGroupConversation 或 title 內含冒號 :。
Instagram 處理：遇到 (你的帳號) 群組名，切除括號；遇到 帳號 : 朋友名 (私訊)，切除冒號前方字串保留發言人。
LINE 處理：若標題與對話框名稱不同，視為群組；若內文格式為 發言人 : 訊息，抽出發言人存入 subSender。
引擎 B：畫面停留碼錶 (AccessibilityService)
觸發時機：偵測畫面視窗改變 (WINDOW_STATE_CHANGED)。
核心邏輯：
當畫面上存在「輸入框 (EditText)」或「特定 App 標題列 ID」時，視為進入聊天室，紀錄 chatStartTime。
離開畫面時，計算 System.currentTimeMillis() - chatStartTime，轉換為秒數存入 durationSeconds。
防呆限制：單次停留最高上限鎖定為 1800 秒 (半小時)，避免背景掛機造成異常數據。防禦性排除「貼文、留言、分享」等 IG 雜訊名稱。

🧠 4. ViewModel 與商業邏輯
狀態管理：使用 MutableStateFlow 存放 interactions, aliases (合併帳號對照表), categories。
時間過濾器 (TimeFilter)：支援「單日」、「單週」、「全部」。使用 Calendar 進行時間戳範圍過濾。
進階資料管理：提供針對 TODAY, WEEK, ALL 或「特定日期時間戳」的精準 filterNot 刪除功能。
自動備份引擎：每日開啟 App 時檢查，超過 24 小時未備份，則將 exportBackupString() 自動寫入本機 FilesDir 的隱藏 txt 中。

🎨 5. UI/UX 與 Compose 排版鐵律 (防跑版指南)
本 App 具備極高的 UI 細節要求，任何 AI 產出 UI 時必須遵守以下排版鐵律：
全域防擋機制 (System Insets)：
所有全螢幕視窗（包含 Scaffold、Onboarding、Dialog 內的滿版預覽），最外層必須加上 .systemBarsPadding().navigationBarsPadding()，確保按鈕絕對不會被導覽列遮擋。
文字防擠壓與置中鐵律 (Anti-Clipping)：
任何顯示「人名」、「標籤名」、「App 名稱」的 Text，必須加上 maxLines = 1, overflow = TextOverflow.Ellipsis。
在 Row 中排列時，左側的人名區塊必須加上 Modifier.weight(1f, fill = false).padding(end = 8.dp)，確保右側的時間或狀態數值有足夠空間顯示，字太長時由左側縮略 (...)。
多標籤橫向滾動 (LazyRow / ScrollState)：
排行榜卡片右下角的 App 來源標籤（若對方使用多個 App），外部的 Row 必須加上 .horizontalScroll(rememberScrollState())，且內部文字設定 softWrap = false，讓使用者可以滑動查看，絕不允許標籤被擠成直排。
閃退防護：
DemoDashboardScreen (展示用模擬假資料畫面) 必須包裝在 Dialog(properties = DialogProperties(usePlatformDefaultWidth = false)) 中，且內部的 LazyColumn 必須帶有 Modifier.weight(1f)，防止無限高度測量導致閃退。

📊 6. 重點互動視窗 (Dialogs)
熱點分析對話框 (ContactDetailDialog)：
24 小時活躍 Canvas：繪製 0-23 時的長條圖，點擊單一長條會顯示該小時的精確通話/停留分數。
群組成員發言排行：若該卡片為群組 (isGroup == true)，視窗下方必須使用 LazyColumn 結合長條圖進度條，列出該群組中 subSender 的發言次數排行。
🗂️ 7. 四大核心分頁模組 (Main Tabs)
7.1 總覽分頁 (OverviewView)
數位健康儀表板 (Digital Wellbeing)：
支援三種視角切換：TIME (時長)、NOTIF (通知數)、PERIOD (時段)。
手機總用量佔比：若有授權 UsageStatsManager，會讀取 Android 系統的總螢幕使用時間，並計算「通訊軟體停留時間 / 總螢幕時間」的百分比 (screenPct)。
動態比例條 (Animated Progress Bars)：
點擊不同的 App 或時段色塊，可直接呼叫 OverviewDetailDialog，並傳入該條件過濾後的 SenderStat 排行榜。
標籤互動分析 (CategoryStat)：
自動加總各標籤 (工作、家人等) 的總時長與訊息量，並提供橫向滾動 (LazyRow) 的卡片檢視。
7.2 排行分頁 (LeaderboardView)
多維度篩選器 (Multi-dimensional Filters)：
時間篩選：單日 / 單週 / 全部 (透過自訂日曆 MinimalCalendar 精確選擇特定日期)。
平台篩選：全平台 / 特定 App (如僅看 Instagram)。
標籤篩選：全標籤 / 特定自訂標籤。
排序切換：可隨時在「依停留時間 (SortType.TIME)」與「依互動頻率 (SortType.MESSAGES)」間切換。
手動強制重整 (Force Refresh)：提供防抖動 (Debounce) 的重整按鈕，點擊後會呼叫 viewModel.forceRefresh() 重新計算快取。
7.3 聯絡人分頁 (ContactsCategoryView)
帳號合併系統 (Alias Mapping Engine) - 核心功能：
痛點解決：同一個人在 LINE 叫「王小明」，在 IG 叫「Ming_Wang」。
邏輯：UI 提供「綁定其他帳號」下拉選單。當使用者將 A 合併至 B 時，會在 DataRepository.aliases 字典中寫入 A -> B 的對照。
查詢規則：所有排行榜在統計前，都會先經過 aliases[rawSenderName] ?: rawSenderName 的洗牌，確保數據完美歸戶到主帳號。
自訂標籤 (Custom Categories)：
除預設 4 種標籤外，允許使用者自創標籤。
創立時從 AppColors.CustomPalette 中隨機抽取一組互補色 (文字色與背景色)，確保 UI 美觀且不需使用者自選手機色碼。
搜尋與排序：支援即時字串比對 (contains(searchQuery)) 以及 依名稱/互動量排序。
7.4 設定與資料分頁 (SettingsView)
一週起始日自訂：可自由切換「週一」或「週日」作為一週的第一天，這會連帶影響所有 WEEK Filter 的計算區間 (getStartOfWeekTimestamp)。
權限監測 (Permission Trackers)：
即時監測 NotificationListener, Accessibility, UsageStats 權限狀態，提供一鍵跳轉系統設定的捷徑。
⚙️ 8. 進階資料管理與備份引擎
8.1 雙重備份機制 (Backup System)
手動匯出/匯入 (exportBackup / importBackup)：
將所有互動紀錄 (_interactions)、合併名單 (_aliases)、標籤分配 (_categories) 與自訂標籤 (_availableCategories) 轉為自定義的 CSV 格式，並以 Base64 編碼為 V11||... 開頭的安全字串。
透過 ActivityResultContracts.CreateDocument 呼叫系統原生檔案管理器儲存為 .txt。
自動備份引擎 (AutoBackup)：
觸發機制：每次開啟 App (init(context)) 時檢查 lastAutoBackupTime，若距離上次備份超過 24 小時，則自動在背景生成一份字串，寫入 Android 系統的本地安全資料夾 context.filesDir。
Share Intent (一鍵上傳雲端)：
將 Base64 備份字串塞入 Intent.EXTRA_TEXT，並呼叫 Intent.createChooser，讓使用者可以一鍵分享到 Google Drive 或 LINE Keep，繞過複雜的 Google API 認證。
8.2 精準資料淨化器 (Advanced Data Clear)
為防止誤觸「清除全部」，改為進階資料管理 Dialog。
分批清除邏輯：
TODAY: 過濾掉 Timestamp 大於「今日 00:00」的紀錄。
WEEK: 過濾掉 Timestamp 大於「本週第一天 00:00」的紀錄。
特定日期清除 (clearInteractionsByDate)：使用者透過日曆點選某日，系統會比對紀錄的 Calendar.YEAR 與 Calendar.DAY_OF_YEAR，僅刪除符合該日的精確資料。
🎭 9. 特殊 UX 體驗：視覺預覽引擎 (DemoDashboardScreen)
目的：解決使用者剛下載 App 時，因沒有數據導致的「空白畫面流失率 (Drop-off rate)」。
實作邏輯：
它是一個與真實資料完全隔離的假 UI 組件。
不使用寫入假資料進資料庫的髒方法，而是直接透過 Dialog(properties = DialogProperties(usePlatformDefaultWidth = false)) 蓋出一個全螢幕的排版範例。
展示了「深度交流家人格」、「群組長條圖排行榜」等最吸引人的功能，讓使用者產生期待感，點擊關閉後立即銷毀，確保真實資料庫永遠純淨 (isFirstLaunch 機制控制)。
