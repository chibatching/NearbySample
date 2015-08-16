# NearbySample
Nearby Messages API sample chat application for Kotlin

## Setup

### Enable Nearby Messages

Setup your Nearby Messagees API at [Google Developers Console](https://console.developers.google.com/)
Follow official instruction below.
https://developers.google.com/nearby/messages/android/get-started

### Put API KEY in AndroidManifest

Put your API KEY in `android:value` of `meta-data`.


```xml
    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/AppTheme" >
        <meta-data
            android:name="com.google.android.nearby.messages.API_KEY"
            android:value="YOUR API KEY" />
```
