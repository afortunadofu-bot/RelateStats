# RelateStats 上架 Google Play 規劃

> 更新：2026-09-15（對應 v3.28）
> ⚠️ Play 政策會變。每一步送出前，以 Play Console 當下顯示的要求為準。
> 送審要貼的文字全部在 [play/SUBMISSION.md](play/SUBMISSION.md)。

---

## 0. 已確定的決策

| 項目 | 決定 |
|------|------|
| 套件名稱 | `com.relatestats.app`（⚠️ 第一次上傳 AAB 之後就永遠不能改） |
| 上架名稱 | RelateStats：社交時間統計 |
| 匯入手機通話紀錄 | **移除**（`READ_CALL_LOG` 是受限權限，社交統計不在允許用途內） |
| 停留時間偵測 | **保留並申請無障礙 API 聲明**（審核資料已備妥，但仍有被拒風險，見第 5 節） |
| 備份 | 新增「綁定 Google 雲端硬碟」每晚自動備份；**不加網路權限**（見第 2 節） |
| 收費 | 免費 |
| 目標年齡 | 18 歲以上 |

---

## 1. 進度

### ✅ 程式（v3.28 已完成）

- [x] `applicationId` 改成 `com.relatestats.app`
- [x] 移除「匯入手機通話紀錄」功能與 `READ_CALL_LOG`
- [x] 明確揭露畫面 `PermissionDisclosureDialog`：通知存取、無障礙服務、使用紀錄存取，所有入口都先經過它
- [x] 無障礙服務設定 `isAccessibilityTool="false"`，系統設定頁說明改寫
- [x] 設定頁「隱私權政策」入口
- [x] 雲端備份（綁定、每晚自動、立即備份、更換位置、解除綁定）
- [x] release 開啟 R8（`isMinifyEnabled`、`isShrinkResources`）
- [x] 簽署設定讀 `keystore.properties`（已列入 `.gitignore`，範本 `keystore.properties.example`）
- [x] 使用說明、常見問題同步更新

### ✅ 文件（已完成）

- [x] 隱私權政策 `docs/privacy-policy.html`
- [x] 商店文字、表單答案、無障礙聲明、影片腳本、測試招募文案 `docs/play/SUBMISSION.md`

### ⬜ 需要你本人做的事

依順序：

1. [ ] **把現有資料搬到新版**（見第 3 節）
2. [ ] **註冊 Play Console 開發者帳號**（US$25，身分驗證要等幾天，越早送越好）
3. [ ] **產生 upload key**（見第 4 節）
4. [ ] **開 GitHub Pages 讓隱私權政策上線**（見第 6 節）
5. [ ] **隱私權政策與 SUBMISSION.md 填入聯絡信箱**（建議另開一個專用信箱，這會公開）
6. [ ] **截圖與主題圖片**（用範例資料，規格見 SUBMISSION.md 第 1 節）
7. [ ] **錄無障礙聲明影片**，上傳 YouTube 設為不公開（腳本見 SUBMISSION.md 第 3 節）
8. [ ] **在 Console 建立 App、填所有表單**（照 SUBMISSION.md 貼上）
9. [ ] **內部測試**：自己從 Play 裝一次，完整跑過
10. [ ] **封閉測試：至少 12 人連續 14 天**（招募文案見 SUBMISSION.md 第 5 節）
11. [ ] **申請正式版存取權 → 送審 → 分階段發布**

---

## 2. 雲端備份為什麼不用「Google 登入」

直覺做法是「登入 Google 帳號 → 用 Drive API 上傳」，但那樣要：

- 加 `INTERNET` 權限 → 「App 沒有網路、資料傳不出去」這個最強的隱私保證就沒了
- 串 OAuth、到 Google Cloud 申請憑證，Drive 權限範圍還可能要另外審核
- 資料安全性表單要申報收集與傳輸

實際做法：用 Android 系統的檔案選擇器。使用者在儲存位置選「雲端硬碟」建立一個檔案，
App 取得那個檔案的長期寫入權限，每晚覆寫它，**上傳由 Google 雲端硬碟 App 處理**。
使用者的操作只有「按綁定 → 選雲端硬碟 → 儲存」，效果一樣是每晚自動備份。

⚠️ 未實機驗證：少數手機的雲端硬碟 App 版本可能不給長期權限，這時會提示「這個位置不允許自動更新」。
上架前請在自己的手機實測：綁定 → 隔天看雲端硬碟裡的檔案時間有沒有更新。

---

## 3. 資料搬家（換套件名稱）

`com.relatestats.app` 對手機來說是**全新的 App**，舊版（`com.example.relatestats`）的資料不會自動帶過去。
兩個版本可以同時存在手機上。

1. 開**舊版** → 設定 → 備份 →「存成檔案」→ 儲存位置選「雲端硬碟」
2. 安裝**新版** → 設定 → 備份 →「從檔案還原」→ 到雲端硬碟選剛剛的檔案
3. 新版重新開啟通知存取、無障礙服務、使用紀錄存取（舊版的要先關掉，否則兩個會重複記錄）
4. 新版 →「☁ 綁定 Google 雲端硬碟」
5. 確認新版資料正確後，解除安裝舊版

---

## 4. 產生 upload key 與建置

1. Android Studio → **Build → Generate Signed App Bundle or APK** → Android App Bundle → **Create new**
   - Key store path：放在專案根目錄，檔名 `upload-keystore.jks`（已被 `.gitignore` 排除）
   - Alias：`upload`
   - 有效期：25 年以上
2. 把 `keystore.properties.example` 複製成 `keystore.properties`，填入路徑與密碼
3. ⚠️ `upload-keystore.jks` 與密碼**另外備份兩份**（隨身碟＋密碼管理器）。遺失可以向 Google 申請重設，但要等好幾天
4. 建置：

```
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat bundleRelease
```

產出：`app/build/outputs/bundle/release/app-release.aab`

5. Play Console 建立 App 時使用 **Play App Signing**（預設），真正的簽署金鑰由 Google 保管

---

## 5. 無障礙服務的風險

審核資料已照規定備妥（揭露畫面、系統說明、聲明文字、影片腳本），但這項政策是 Play 最常退件的原因之一，
**即使全部照做仍可能被拒**。

被拒時的處理：

1. 看退件信寫的具體理由，照著補（常見：影片沒拍到揭露畫面、聲明與實際行為不符）
2. 補完重新送審；確實有誤判時用 Console 內的申訴表單
3. 若確定不會過：出一個不含無障礙服務的版本先上架，停留時間改用 `UsageStatsManager` 只算 App 層級

---

## 6. 隱私權政策上線（GitHub Pages）

檔案已在 `docs/privacy-policy.html`，網址預定為：

```
https://afortunadofu-bot.github.io/RelateStats/privacy-policy.html
```

1. GitHub → RelateStats repo → **Settings → Pages**
2. Source 選 **Deploy from a branch**，Branch 選 `master`、資料夾選 `/docs` → Save
3. 等幾分鐘，用手機開上面的網址確認看得到
4. ⚠️ 免費帳號的 GitHub Pages **需要 repo 是公開的**。若不想公開原始碼：另開一個公開 repo（例如 `relatestats-privacy`）只放這個 HTML，
   然後把 `MainActivity.kt` 的 `PRIVACY_POLICY_URL` 與 SUBMISSION.md 的網址一起改掉

---

## 7. 時程

```
第 1 週     資料搬家、註冊開發者帳號、upload key、隱私權政策上線
第 2 週     截圖、影片、Console 表單、內部測試
第 2–4 週   封閉測試（至少 12 人、連續 14 天，無法縮短）
第 4–5 週   申請正式版存取權 → 送審（含無障礙聲明，可能超過一週）
第 5–6 週   分階段發布 20% → 50% → 100%（先只開台灣）
```

---

## 8. 上架後

- **Android Vitals**：當機率 > 1.09%、ANR > 0.47% 會影響曝光
- **回覆評論**：尤其「權限太多」「不敢開」類，說明資料不上傳
- **每年目標 API 更新**：Google 每年 8 月底左右要求提高 `targetSdk`
- **政策信件**：開發者帳號信箱的政策通知通常有 30 天修正期
- **通知與畫面格式**：LINE、IG 改版可能讓偵測失效，要持續維護
- **帳號活躍**：長期不更新、不登入，帳號可能被標記為不活躍
