# تغییرات ادغام ماژول WireGuard در SFDNS Pro

تاریخ: ۲۰۲۶-۰۹-۰۶

## خلاصه

ماژول WireGuard به صورت **مستقل** به اپلیکیشن SFDNS Pro اضافه شده است.
کاربر می‌تواند بین دو حالت **DNS** و **WireGuard** سوییچ کند (دقیقاً شبیه تصویر مرجع).
فقط یکی از دو سرویس در هر لحظه فعال می‌شود چون اندروید اجازهٔ یک VpnService فعال را می‌دهد.

## روش ادغام انتخاب‌شده

- **بهترین و کم‌ریسک‌ترین روش**: استفاده از کتابخانهٔ رسمی و embeddable  
  `com.wireguard.android:tunnel:1.0.20260102` از Maven Central  
  (نه کپی کل سورس wireguard-android و نه کار روی Smali).

## فایل‌های تغییر یافته / اضافه شده

### جدید
- `app/src/main/java/com/sfdnsapp/pro/data/AppMode.kt`  
  enum حالت اپ (DNS / WIREGUARD)

- `app/src/main/java/com/sfdnsapp/pro/wireguard/WireGuardManager.kt`  
  Wrapper سبک روی GoBackend رسمی. پشتیبانی از connect / disconnect با متن کانفیگ استاندارد WireGuard.

- `app/src/main/java/com/sfdnsapp/pro/ui/components/ModeToggle.kt`  
  کامپوننت سوییچ UI دقیقاً شبیه تصویر (WireGuard | DNS)

- `CHANGES_WIREGUARD.md` (همین فایل)

### ویرایش‌شده
- `gradle/libs.versions.toml`  
  اضافه شدن `wireguardTunnel` و `desugarJdkLibs`

- `app/build.gradle.kts`  
  - `compileOptions` به Java 17 + `coreLibraryDesugaringEnabled = true`  
  - dependency `libs.wireguard.tunnel` + desugar

- `app/src/main/java/com/sfdnsapp/pro/viewmodel/DnsViewModel.kt`  
  - stateهای `appMode`, `wgState`, `wgConfigName`, `wgConfigText`  
  - متدهای `setAppMode()`, `setWireGuardConfigText()`, `toggleWireGuard()`, `disconnectWireGuard()`  
  - بارگذاری/ذخیرهٔ حالت و کانفیگ از SharedPreferences

- `app/src/main/java/com/sfdnsapp/pro/ui/screens/MainScreen.kt`  
  - نمایش `ModeToggle`  
  - رفتار دکمهٔ اتصال وابسته به حالت فعلی  
  - کارت سرور در حالت WireGuard نام کانفیگ را نشان می‌دهد

- `app/src/main/java/com/sfdnsapp/pro/MainActivity.kt`  
  - `toggleVpnConnection()` حالا mode را چک می‌کند

## نحوهٔ استفاده فعلی

1. سوییچ را روی **WireGuard** بگذارید.
2. فعلاً کانفیگ باید از قبل در SharedPreferences (`wg_config_text`) ذخیره شده باشد  
   (یا می‌توانید از طریق کد/دیباگ `viewModel.setWireGuardConfigText(confText)` را صدا بزنید).
3. دکمهٔ پاور را بزنید → تونل بالا می‌آید.

## کارهای باقی‌مانده برای کامل شدن (پیشنهادی)

- دیالوگ وارد کردن کانفیگ (Paste / Import .conf / QR)
- لیست تونل‌های ذخیره‌شده
- نمایش وضعیت واقعی peer (transfer, handshake)
- نوتیفیکیشن اختصاصی برای حالت WireGuard
- توقف تمیز DnsVpnService هنگام سوییچ به WireGuard (و برعکس)

این اسکلت طوری طراحی شده که این موارد را بدون شکستن DNS فعلی بتوان اضافه کرد.

## ساخت APK

در محیط فعلی Android SDK وجود ندارد.  
پروژه را در Android Studio باز کنید و:

```bash
./gradlew assembleDebug
# یا
./gradlew assembleRelease
```

اولین بار Gradle dependencyهای جدید (tunnel + desugar) را دانلود می‌کند.

## نکات مهم

- هر دو قابلیت **مستقل** هستند و با سوییچ حالت از هم جدا می‌شوند.
- از کتابخانهٔ رسمی استفاده شده → احتمال باگ و ناسازگاری بسیار کمتر از کپی سورس یا Smali است.
- DNS قبلی بدون تغییر رفتار کار می‌کند.
