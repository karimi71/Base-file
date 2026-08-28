# Keep the fixture's remotely-instantiated worker constructor.
-keep class dev.basefile.future.android.RemoteFixtureWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
# PDFBox discovers a small number of classes and resources dynamically.
-keep class com.tom_roush.pdfbox.** { *; }
-dontwarn org.bouncycastle.**
# JPEG 2000 support is an optional PDFBox-Android integration not shipped by this fixture.
-dontwarn com.gemalto.jp2.**
