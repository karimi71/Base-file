# Base-file

عمومی‌ترین مخزن فایل‌های مرجع — ابزارهای پایه بدون وابستگی به سرور دیگران.

---

## 📁 android-build/

زنجیرهٔ کامل ساخت APK اندروید (بدون Android SDK و Gradle). تمام ابزارهای مورد نیاز برای کامپایل، بسته‌بندی و امضای یک APK.

### فایل‌ها

| فایل | حجم | توضیح |
|------|------|-------|
| `android-34.jar` | ~26 MB | کلاس‌های فریم‌ورک Android 34 (API Level 34) — شامل تمام کلاس‌های `android.*` و `java.*` پایه |
| `aapt2` | ~6 MB | Android Asset Packaging Tool 2 — پردازش و بسته‌بندی منابع (layouts, strings, manifests) |
| `bundletool.jar` | ~32 MB | ابزار اصلی ساخت APK/AAB — شامل **D8** (کامپایلر DEX) و **apksig** (امضای APK) |
| `ecj.jar` | ~3 MB | Eclipse Java Compiler — کامپایلر Java مورد استفاده در فرایند ساخت |
| `openjdk25-linux-x64.tar.gz` | ~34 MB | Java Runtime (JDK 25) برای لینوکس ۶۴بیتی — بدون نیاز به نصب سیستمی |

### منابع

- **android-34.jar**: از مخزن `Sable/android-platforms` در GitHub — blob شماره `923bafccf73aef996495c559e919ad8be3cabd58`
- **aapt2**: از بستهٔ npm [`aaptjs3`](https://www.npmjs.com/package/aaptjs3) — فایل `package/bin/x64/linux/aapt2`
- **bundletool.jar**: از بستهٔ npm [`bundletoolheavy`](https://www.npmjs.com/package/bundletoolheavy) — فایل `bundletool-all-1.17.2.jar`
- **ecj.jar**: از بستهٔ npm [`coc-java`](https://www.npmjs.com/package/coc-java) — فایل `org.eclipse.jdt.core.compiler.batch_*.jar`
- **openjdk25-linux-x64.tar.gz**: از PyPI [`jdk4py`](https://pypi.org/project/jdk4py/) — پوشهٔ `java-runtime` از wheel بسته‌بندی شده

### نحوهٔ استفاده

```bash
# استخراج جاوا
tar -xzf android-build/openjdk25-linux-x64.tar.gz

# تنظیم متغیرهای محیطی
export JAVA_HOME="$PWD/jdk4py/java-runtime"
export PATH="$JAVA_HOME/bin:$PATH"

# بررسی نسخه‌ها
java -version        # باید JDK 25 را نشان دهد
java -jar android-build/bundletool.jar version  # نسخه bundletool
./android-build/aapt2 version                   # نسخه aapt2
```

### جریان ساخت APK

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  سورس کد    │────▶│   ecj.jar   │────▶│   فایل‌های  │
│  Java (.java)     │  (کامپایل)  │     │    .class   │
└─────────────┘     └─────────────┘     └──────┬──────┘
                                               │
                                               ▼
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  منابع      │────▶│   aapt2     │────▶│   resources.ap_      │
│  (res/)     │     │  (بسته‌بندی) │     │             │
└─────────────┘     └─────────────┘     └──────┬──────┘
                                               │
                                               ▼
                                      ┌─────────────┐
                                      │  D8 (بخشی   │
                                      │  از bundletool)
                                      └──────┬──────┘
                                             │
                                             ▼
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  AndroidManifest.xml  │────▶│   aapt2     │────▶│   AndroidManifest.xml     │
│                      │     │  (ادغام)    │     │   (کامپایل‌شده) │
└─────────────┘     └─────────────┘     └──────┬──────┘
                                               │
                                               ▼
                                      ┌─────────────┐
                                      │ apksigner   │
                                      │ (امضا)      │
                                      └──────┬──────┘
                                             │
                                             ▼
                                      ┌─────────────┐
                                      │    APK      │
                                      │  امضا‌شده   │
                                      └─────────────┘
```

---

## 🔤 fonts/

پک منتخب ۵۰ فونت انگلیسی و فارسی/عربی که از بسته‌های npm دریافت شده‌اند.

- `fonts/english/` — ۲۵ خانوادهٔ انگلیسی، از جمله Inter، Roboto، Montserrat، Poppins و فونت‌های monospace
- `fonts/persian/` — ۲۵ خانوادهٔ فارسی/عربیِ منتخب، از جمله Vazirmatn، Estedad، Shabnam، Sahel، IBM Plex Sans Arabic و Noto Arabic
- `fonts/README.md` — فهرست نسخه‌ها، حجم‌ها، منبع و نکات مجوز
- `fonts/manifest.json` — مسیر، اندازه و SHA-256 همهٔ فایل‌های فونت برای صحت‌سنجی

برای هر خانواده، فایل `SOURCE.md` و مجوز موجود از بستهٔ اصلی در همان پوشه نگه‌داری شده است.

---

## 🔒 امنیت

> ⚠️ **این مخزن عمومی است.**
> 
> هرگز فایل‌های زیر را در این مخزن قرار ندهید:
> - کلیدهای امضا (`*.keystore`, `*.jks`)
> - رمزهای عبور یا توکن‌ها
> - کد خصوصی یا API keys
> - اطلاعات حساس هر نوع

این ابزارها فقط برای ساخت APK عمومی مناسب هستند. امضای نهایی باید با کلیدهای خصوصی که در محیط امن نگهداری می‌شوند انجام شود.

---

## 📦 سایر پوشه‌ها

این مخزن برای افزودن سایر ابزارهای پایه نیز قابل استفاده است:
- ابزارهای CLI
- فایل‌های پیکربندی
- کتابخانه‌های مشترک
- Runtime های مختلف
