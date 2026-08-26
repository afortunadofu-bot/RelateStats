# 修復紀錄

## v1.1（2026-08-26）— 系統性除錯

針對「訊息統計不進去 / 閃退卡頓 / 跑版 / 統計數據不實」做的全面盤查與修復。
以下依照使用者感受到的症狀分類，每一項都標示根因。

---

### A. 訊息統計不進去

| # | 根因 | 修法 |
|---|------|------|
| A1 | 通知服務算出 `isCall` / `isMissedCall` / `subSender` / `finalWordCount`，**卻沒有放進 `InteractionRecord`**。 | 全部正確帶入紀錄。 |
| A2 | `InteractionRecord.toCsvString()` 只序列化 8 個欄位，`isMissedCall` / `isCall` / `subSender` **存檔即遺失**。重開 App 後通話數、未接來電、群組發言排行永遠是 0。 | 擴充為 11 欄位（V12），並保留讀取舊 8 欄位資料的相容性。 |
| A3 | 雜訊過濾用 `content.contains("正在")`、`contains("上傳")`、`contains("影片")` 等寬鬆比對，「我正在路上」「影片我看過了」這類**真實訊息被整句丟掉**。 | 改為「整則內容等於雜訊」或「以雜訊前綴開頭」才略過。 |
| A4 | 一次收到多則訊息時系統只 post 一則通知，舊版只記 1 則。 | 解析 `android.messages`（Notification.EXTRA_MESSAGES），逐則計入並記錄處理進度避免重複。 |
| A5 | 群組判斷讀 `android.isGroup`，但正確的 key 是 `android.isGroupConversation`，該判斷**永遠是 false**。 | 兩個 key 都讀。 |
| A6 | 服務先於 Activity 啟動時（例如開機後）`prefs` 尚未建立，`saveData()` 靜靜地什麼都沒寫。 | `addInteraction` 明確擋掉未初始化狀態，並在 `onListenerConnected()` 補呼叫 `init()`。 |
| A7 | 通知服務任何一次例外都會被系統停用，之後全部收不到。 | 整個處理流程包在 try/catch 內。 |

### B. 停留時間統計不實（畫面出現 742246 小時）

| # | 根因 | 修法 |
|---|------|------|
| B1 | `getDeviceUsageTimeSec()` 寫成 `stats?.sumOf{...} ?: 0L / 1000L`。Kotlin 中 `/` 優先序高於 `?:`，**`/1000` 只套用在預設值 `0L` 上**，真正的加總完全沒除以 1000 → 回傳毫秒卻當秒用。 | 先取值再除，並改用 `queryAndAggregateUsageStats`，回傳型別改 Long。 |
| B2 | 無障礙服務遇到非目標 App 直接 `return`，**碼錶永遠不會停**。切去別的 App、回桌面、關螢幕，時間都繼續累加到最後一個聊天室頭上。 | 非目標 App 一律先 `leaveChat()` 結算。 |
| B3 | 沒有處理關螢幕。整夜掛在聊天室 → 隔天一開就是好幾小時。 | 註冊 `ACTION_SCREEN_OFF` / `ACTION_USER_PRESENT` 廣播強制結算，並在 `onUnbind` / `onDestroy` / `onInterrupt` 一併結算。 |
| B4 | 單次上限寫成 3600 秒（藍圖規定 1800），且超過上限時**記成上限值**，一樣虛胖。 | 上限改回 1800 秒，超過視為背景掛機直接丟棄。 |
| B5 | 所有加總用 `Int`，資料量一大就整數溢位變負數或天文數字。 | `SenderStat` / `AppUsageStat` / `CategoryStat` / `HourlyStat` 的時長欄位全面改 `Long`。 |

### C. 抓不到聊天室名稱、出現假聯絡人

| # | 根因 | 修法 |
|---|------|------|
| C1 | `accessibility_service_config.xml` 的 `accessibilityFlags` 只有 `flagDefault`，**缺少 `flagReportViewIds`**。依 Android 規格，沒有這個 flag 時 `findAccessibilityNodeInfosByViewId()` 不會回傳任何節點 → `extractChatName()` 永遠是 null。 | 補上 `flagReportViewIds｜flagIncludeNotImportantViews｜flagRetrieveInteractiveWindows`。 |
| C2 | 因為 C1 抓不到標題，舊碼退而用 `event.text.firstOrNull()`——那通常就是**訊息內容本身**，於是訊息文字被當成聯絡人名稱，建出一堆假聯絡人並瓜分停留時間。 | 移除該退路，只信任明確的標題 view id。 |
| C3 | `accessibilityEventTypes` 只宣告 `typeWindowStateChanged`，但程式碼在判斷 `TYPE_WINDOW_CONTENT_CHANGED`——該事件根本收不到。 | 兩種事件都宣告，並在程式端節流。 |
| C4 | 聊天室名稱 `length < 25` 的限制會把長群組名整個濾掉。 | 放寬到 60 字。 |

### D. 卡頓 / ANR

| # | 根因 | 修法 |
|---|------|------|
| D1 | 每收到一則通知就把**整份紀錄重新序列化並寫檔**，而且是同步執行。 | 改為背景執行緒 + 1.5 秒去抖動合併寫入；使用者操作（合併、標籤、清除、匯入）則立即寫入。 |
| D2 | `TYPE_WINDOW_CONTENT_CHANGED` 滑動時每秒觸發數十次，每次都**走訪整棵節點樹**。 | 1.2 秒節流，並限制遞迴深度 12 層／節點數 400。 |
| D3 | 聯絡人分頁對每位聯絡人都掃一次完整紀錄（O(聯絡人×紀錄)），且沒有 `remember`，每次重組重算。 | 改用 `groupingBy().eachCount()` 一次算完並加上 `remember`。 |
| D4 | 「標籤互動分析」與聯絡人詳情的 24 小時統計都沒有 `remember`，對話框開著就持續重算。 | 全部加上 `remember`。 |
| D5 | 匯出／匯入在主執行緒讀寫檔案。 | 移到 `Dispatchers.IO`。 |
| D6 | 紀錄數無上限成長，存檔越來越慢。 | 上限 200,000 筆，超過保留最新的。 |

### E. 閃退

| # | 根因 | 修法 |
|---|------|------|
| E1 | `loadData()` 中 `catSet.associate { it.split("|=|").let { it[0] to it[1] } }`，任何一筆資料格式壞掉就拋 `IndexOutOfBoundsException`——**一開 App 就閃退**。 | 每段獨立防護，壞掉的那筆丟棄，其餘照常載入。 |
| E2 | 名稱中若含 `|#|`、`|=|`、`;`、`\|\|` 會讓 CSV 解析錯位，整批紀錄讀不回來。 | 存檔前消毒分隔符號。 |
| E3 | `importBackupString` 沒檢查欄位數，備份檔略有損毀就 `parts[4]` 越界。 | 檢查 `parts.size` 並驗證解析結果。 |
| E4 | `getStartOfWeekTimestamp` 的 `while` 迴圈在 `startDay` 值異常時**無限迴圈直接 ANR**。 | 加上合法值檢查與最多 7 次的迴圈上限。 |
| E5 | 進度條 `currentScore / maxScore` 在 maxScore 為 0 時產生 NaN。 | 防護並 `coerceIn(0f, 1f)`。 |

### F. 跑版

| # | 根因 | 修法 |
|---|------|------|
| F1 | App 用量列的數值 `Modifier.width(60.dp)` 固定寬度又沒鎖單行，`"250888h 41m"` 這種長字串被折成三行（截圖中的破版）。 | 改 `widthIn(min=60.dp)` + `maxLines=1, softWrap=false`。 |
| F2 | 時段百分比同樣的固定 `width(40.dp)` 問題。 | 同上。 |
| F3 | 「平台數位健康」的大數字在 Row 中沒有溢位保護，數字一大整排擠爆。 | 改為單行 + 橫向捲動。 |
| F4 | `ContactDetailDialog` 內容在小螢幕會被裁掉。 | 加上垂直捲動與最大高度。 |
| F5 | 「清除特定日期」的日曆被固定 `height(300.dp)` 切掉。 | 改 `heightIn(max=420.dp)` + 可捲動。 |
| F6 | `MinimalCalendar` 固定以「週日」開頭，和設定頁的「一週起始日」不一致，日期整排錯位。 | 依設定值旋轉星期標題與起始位移。 |
| F7 | 內容區沒有導覽列避讓。 | 補 `navigationBarsPadding()`。 |

### G. 統計邏輯不一致

| # | 根因 | 修法 |
|---|------|------|
| G1 | `getLeaderboard` 與 `getDynamicLeaderboard` **各寫一份規則且互相衝突**（一個把未接來電算進訊息、另一個不算），總覽點進去的排行與排行分頁對不起來。 | 合併為單一 `buildStats()`。 |
| G2 | `SenderStat.incomingCount` 實際存的是「全部紀錄筆數」，「依互動頻率」排序其實在排停留紀錄筆數。 | 拆成 `recordCount`（全部）與 `incomingCount`（真正收到的），排序改用 `msgCount + callCount`。 |
| G3 | 排行榜長條圖的基準與排序基準不一致，第一名的長條不一定最長。 | 統一基準。 |
| G4 | 帳號合併只跳一層：「A 併到 B，再把 B 併到 C」之後 A 仍停留在 B，同一人被拆成兩筆。 | 目標解析到最終節點，並把原本指向 A 的別名一起改指；加上循環偵測。 |
| G5 | 聯絡人清單是 `aliases.values + 所有原始名稱`，**被合併掉的舊名稱仍留在清單上**，同一人合併後反而變成兩張卡片。 | 一律先歸戶再去重。 |
| G6 | 通話時長正則只認得「12分34秒」，其他格式（`1:02:03`、`1小時2分`）一律算 0。 | 支援多種格式。 |
| G7 | IG 群組訊息的字數把「發言人 :」前綴也算進去。 | 先剝除前綴再計字數。 |
| G8 | 同一則訊息被重複 post（已讀、摘要）就重複計次，訊息數虛胖。 | 依 `key + when + 內容` 去重，並排除 `FLAG_GROUP_SUMMARY` 與常駐通知。 |
| G9 | 撥號 App 的「通話中」常駐通知會被當成一則訊息記進去。 | 撥號 App 只產生通話紀錄。 |
| G10 | 「全部」時間範圍下手機總用量直接回 0，佔比分析永遠不出現。 | 以最早一筆紀錄為起點查詢。 |

### H. 其他

- 設定頁的權限狀態只在進入分頁那一刻檢查一次，去系統設定開完權限回來仍顯示「去設定」→ 改為監聽 `ON_RESUME` 即時刷新。
- `app/build.gradle.kts` 的 `buildFeatures` / `composeOptions` 被誤寫在 `compileSdk { }` 區塊內部，外層又重複宣告；`dependencies` 重複引入兩個不同版本的 Compose BOM → 整理並改用 version catalog。
- 「徹底清除並重置所有資料」沒有重置自訂標籤 → 補上。
- 日期前後翻頁用 `± 86400000L` 硬加毫秒 → 改用 `Calendar.add(DATE, n)`。

---

### 已知未完成（非本次範圍）

- **病毒式分享卡片（ShareCardScreen）尚未整合**。藍圖第 1 章的擴散核心，舊版 `社交APP 0410/files/ShareCardScreen.kt` 有完整實作（NEON / MINIMAL / GLASSMORPHISM 三種風格），但 package 為 `com.relatestats.ui.screen`，需移植並補上 `FileProvider` 與 `res/xml/file_paths.xml` 才能分享圖片。
- 自動備份的密碼欄位（`autoBackupPassword`）目前 UI 未開放設定，匯出一律不加密。
