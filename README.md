# RelateStats

Android 社交數據記錄 App。不 root、不碰任何資料庫，只靠 **通知監聽** 與 **無障礙服務** 兩個引擎，
統計你在通訊軟體上對每個人投入的「注意力停留時間」與「互動頻率」，並做社交人格與數位健康分析。

- **目前版本**：v1.6（versionCode 6）
- **技術棧**：Kotlin + Jetpack Compose + ViewModel / StateFlow
- **資料儲存**：全本地 SharedPreferences（`RelateStatsData`），不上傳任何伺服器
- **原始規劃**：[docs/BLUEPRINT.md](docs/BLUEPRINT.md)
- **完整修復與變更紀錄**：[docs/CHANGELOG.md](docs/CHANGELOG.md)

---

## 快速開始

在 Android Studio 直接按 Run 即可。若要用命令列建置，**這台機器的 `java` 不在 PATH**，必須先指定 `JAVA_HOME`：

```bash
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug
```

產出的 APK 在 `app/build/outputs/apk/debug/app-debug.apk`。

安裝到實機：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

首次安裝後必須到 **設定分頁** 手動開啟三項權限，否則不會有任何數據：

| 權限 | 用途 | 沒開會怎樣 |
|---|---|---|
| 通知存取 | 攔截訊息、計算字數與訊息量 | 完全沒有訊息紀錄 |
| 無障礙服務 | 計算聊天室真實停留時間 | 停留時間全部是 0 |
| 使用紀錄存取 | 讀取手機總螢幕時間，算佔比 | 佔比分析不顯示 |

> Samsung 等機型若在系統設定裡找不到本 App，需先到「應用程式 → 本 App → 右上角 → 允許受限制的設定」。

---

## 目前功能

**四大分頁**

- **總覽**：社交人格解讀、平台數位健康（時長／通知／時段三種視角）、標籤互動分析、活躍熱力圖、常用詞
- **排行**：時間／平台／標籤／個人群組 四維篩選，可依停留時間或互動頻率排序，點卡片看 24 小時活躍分析與群組發言排行
- **聯絡人**：跨平台帳號合併（可搜尋、可自動合併相似）、多選批次分組、依平台篩選、垃圾聯絡人一鍵清除
- **設定**：權限監測、標籤管理、一週起始日、備份還原、資料管理

**資料管理**

- 一鍵清除垃圾聯絡人（讚／追蹤者／串文推播等非真人通知）
- 逐筆檢視／勾選刪除紀錄
- **所有刪除操作皆可復原**（設定頁「資料管理」）
- 多選批次設定標籤與刪除
- 清除今日／本週／指定日期／全部
- 自動清理異常紀錄（升級時執行一次）

**標籤**

預設 家人／另一半／朋友／工作／未分類／系統通知，全部可改名、換色、排序、刪除、新增。

**備份**

- Android 自動備份（綁定手機的 Google 帳號，換機自動還原）
- 分享備份檔到 Google 雲端硬碟 / LINE Keep / Gmail（走系統分享選單）
- 匯出成檔案 / 選擇檔案還原

---

## ⚠️ 注意事項

### 資料相關

- **所有資料只存在這支手機**，沒有伺服器。換機或重灌前務必先備份。
- **v1.1 以前累積的停留時間資料全部是錯的，且無法還原**。舊版把毫秒當秒存、碼錶又不會停，
  單筆紀錄出現過 999,000,000 秒（約 31 年）。升級到 v1.2 時會自動清掉這些異常紀錄
  （清理前會先另存原始資料到 App 內部空間）。**訊息數與字數的歷史資料是好的，不受影響。**
- **常用詞從 v1.3 才開始累積**。舊資料只存了字數沒存內容，無法回推。剛升級時這張卡是空的。
- 常用詞是**全期間累計**，不隨日期篩選變動；只存詞頻，不保留訊息原文。
- 單筆紀錄上限 200,000 筆，超過會保留最新的。
- 單次聊天室停留上限 **1800 秒**（30 分鐘），超過視為背景掛機直接丟棄而非記成上限值。

### 標籤相關

- **「未分類」與「系統通知」可以改名換色，但不能刪除**。前者是分類的 fallback，
  後者用於排行降噪，程式內部有依賴，刪除鍵會是灰色的。
- 刪除其他標籤時，原本屬於它的聯絡人會退回「未分類」，互動紀錄不受影響。

### 垃圾聯絡人判定

- 用規則比對辨識非真人通知（`1000+ 個新的讚`、`11 位新追蹤者`、`XXX 的串文`、促銷推播等）。
- 這些名稱把數字寫進名稱裡，所以同一種通知會被拆成數十個獨立「聯絡人」，是清單爆量的主因。
- **規則有可能誤判**（例如群組真的叫「推薦好物」）。因此預設是**歸類到「系統通知」而非直接刪除**，
  誤判時可自行改標籤救回來。要永久刪除請按聯絡人頁的「一鍵清除」，該操作**無法復原**。

### 備份相關

- **Android 自動備份有 25MB 系統上限**。資料量大時請改用「備份到雲端」手動備份。
- **完整的 Google 帳號登入 + Drive API 同步尚未實作**。需要先到 Google Cloud Console
  建立專案、申請 OAuth Client ID 並註冊 App 簽章 SHA-1 —— 這部分只有專案擁有者能做。
  目前以 Android 自動備份與 Share Intent 替代。
- 備份檔可達十幾 MB，因此是透過 FileProvider 傳檔，不是塞進 Intent 的 EXTRA_TEXT
  （那樣會超過 Binder 傳輸上限直接閃退）。

### 開發相關

- 主要邏輯目前全在單一檔案 `app/src/main/java/com/example/relatestats/MainActivity.kt`（約 3,600 行）。
- `getDeviceUsageTimeSec()` 裡有一段警告註解務必保留：Kotlin 的 `/` 優先序高於 `?:`，
  寫成 `a ?: 0L / 1000L` 會讓 `/1000` 只套用在預設值上，導致回傳毫秒卻被當成秒。
  這個 bug 曾讓畫面顯示 742,246 小時。
- 無障礙服務設定 `accessibility_service_config.xml` 的 `flagReportViewIds` **不可移除**，
  少了它 `findAccessibilityNodeInfosByViewId()` 永遠回傳空，會導致抓不到聊天室名稱。
- **聊天室標題不可用寫死的完整 view id 比對**。IG / Threads 幾乎每次改版都會換 id 前綴，
  寫死等於下次改版就失效（v1.6 前 Threads 完全偵測不到時長就是這個原因）。
  現在是「view id 後綴比對 + 畫面頂端文字」兩層策略。
- **停留時間偵測需要容錯**。單次抓不到標題多半只是畫面重繪，立刻結算會把連續停留
  切成大量不足 1 秒的碎片而被丟棄。現在是連續 3 次抓不到才算離開，且 25 秒內回到
  同一聊天室會接續累加。
- **`snapshotForUndo()` 的狀態必須同步擷取**，不能整段丟進背景協程 ——
  呼叫端會在回傳後立刻改資料，否則快照會拍到已刪除後的狀態。
- `Notification.MessagingStyle` 規格：**`sender` 為 null 代表訊息由使用者本人發出**，
  必須排除，否則自己發的訊息會被算成收到的。
- **群組判定不可用 `conversationTitle` 是否存在來推斷** —— IG 連一對一私訊都會帶，
  會導致所有私訊被併成同一個「群組」。只能信 `android.isGroupConversation` 或多發言人。
- 修改後請務必跑一次 `assembleDebug` 確認可建置，再 commit。
- **`versionName` 現在會直接顯示在 App 設定頁最下方**（`BuildConfig.VERSION_NAME`）。改版號只需要改 `app/build.gradle.kts` 一處，App 內畫面與 CHANGELOG 版本號要盡量保持一致。

### 版控

- Repo 掛在 `afortunadofu-bot` 帳號下，但 git `user.email` 是個人信箱，push 時注意憑證。

---

## 版本紀錄

| 版本 | 日期 | 重點 |
|---|---|---|
| v1.6 | 2026-08-27 | 時長偵測重寫、IG 歸戶修正、刪除可復原、勾選刪除紀錄、聯絡人分軟體 |
| v1.5.1 | 2026-08-27 | 設定頁新增版本號與 copyright 標示 |
| v1.5 | 2026-08-27 | 標籤完全可自訂（改名／換色／排序／刪除／新增），預設加入「另一半」 |
| v1.4 | 2026-08-27 | 垃圾聯絡人一鍵清除、多選批次分組 |
| v1.3 | 2026-08-27 | 通知降噪（系統通知標籤）、合併搜尋與自動合併、活躍熱力圖、常用詞、介面瘦身、Android 自動備份 |
| v1.2 | 2026-08-26 | 清理舊版異常資料、資料管理改為可見功能、雲端備份、FileProvider |
| v1.1 | 2026-08-26 | 系統性除錯 40+ 項：統計錯誤、閃退、卡頓、跑版 |
| v1.0 | 2026-04-08 | 初版（雙引擎、四大分頁、帳號合併、備份） |

各版本的完整根因分析與逐項修法見 [docs/CHANGELOG.md](docs/CHANGELOG.md)。

---

## 專案結構

```
app/src/main/
├── java/com/example/relatestats/
│   └── MainActivity.kt          # 資料模型 / Repository / 雙引擎服務 / ViewModel / 全部 UI
├── res/xml/
│   ├── accessibility_service_config.xml   # 無障礙服務設定（flagReportViewIds 不可移除）
│   ├── file_paths.xml                     # FileProvider 路徑
│   ├── backup_rules.xml                   # Android 11 以下自動備份規則
│   └── data_extraction_rules.xml          # Android 12 以上備份／轉移規則
└── AndroidManifest.xml
docs/
├── BLUEPRINT.md    # 原始架構與邏輯藍圖
└── CHANGELOG.md    # 完整修復與變更紀錄
```

---

## 已知限制與待辦

- [ ] **病毒式分享卡片（ShareCardScreen）尚未整合** —— 藍圖第 1 章的擴散核心。
      舊版 `桌面/工作/9私/000 relate app/社交APP 0410/files/ShareCardScreen.kt` 有完整實作
      （NEON / MINIMAL / GLASSMORPHISM 三種風格），需改 package 並接上現有的 FileProvider。
- [ ] Google 帳號登入 + Drive API 同步（卡在需要專案擁有者申請 OAuth 憑證）
- [ ] 自動備份的密碼欄位 `autoBackupPassword` 已存在但 UI 未開放，匯出目前一律不加密
- [ ] 常用詞尚未支援依聯絡人拆分（目前只有全域統計）
- [ ] MainActivity.kt 已達 3,600 行，後續可考慮拆檔
