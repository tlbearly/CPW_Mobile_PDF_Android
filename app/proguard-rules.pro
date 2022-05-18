# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\tammy\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# for pdfium when using TalbotGooday/AndroidPdfViewer
-keep class com.shockwave.**

# for iText added 5-18-22 didn't help still getting missing class errors!!!!!
#-keep class org.spongycastle.cert.**
#-keep class com.madgag.spongycastle.cms.**
#-keep class org.spongycastle.cms.CMSEnvelopedData