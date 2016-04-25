# Getting Started

Monkey Sample App on Android supports 4.0 (API 14) and above.

## Using Gradle + Maven

Monkey Sample App use two main dependencies: MonkeySDK and MonkeyUIKIT. In your app's build.gradle file add the following block to your repositories block:
```
repositories { 
    maven {
        url 'https://dl.bintray.com/criptext/maven'
    } 
}
```

Then add the following to your app's build.gradle file dependencies block:
```
dependencies {
    compile ('com.criptext:monkeyKit:1.2.2@aar') {
        transitive = true;
    }
    compile ('com.criptext.monkeykitui:monkeykitui:1.3@aar') {
        transitive = true;
    }
}
```
If you use compileSdkVersion 22 you can use this dependency for Monkey UIKIT:
```
dependencies {
    ...
    compile ('com.criptext.monkeykitui-v22:monkeykitui:1.0@aar') {
        transitive = true;
    }
}
```
## Edit your Manifest.xml
Monkey UIKit use a photoviewer to show the photos that you send and receive. If you want to use it you need declare PhotoViewActivity in your manifest:
```
<application
    ...
    <activity
        android:name="com.criptext.monkeykitui.photoview.PhotoViewActivity"
        android:theme="@style/Theme.CustomTranslucent"/>
    ...
</application>
```
Monkey SDK use a service to process the information in the background. You need to create a Class that extends the Class MonkeyKit and declare it in your manifest like this:
```
<application
    ...
    <service android:name=".MyServiceClass"/>
    ...
</application>
```
## Dependencies
MonkeySDK and Monkey UIKIT use libraries inside his projects. So when you declare these libraries as dependencies, you need to be aware of not declare the same dependencies that the libraries declare.
### Monkey SDK
- android-query-full.0.26.7.jar
- commons-io-2.4.jar
- gson-2.2.1.jar
- httpclient-4.1.1.jar
- httpmime-4.1.1.jar 

### Monkey UIKIT
- com.squareup.picasso:picasso:2.5.2

## Other Documentation
In order to understand the use of MonkeySDK you can follow the instructions here:
https://github.com/Criptext/MonkeyKitAndroid/

