# ungstagram

## 소개
```
Kotlin, 앱개발에대한 학습을 위해 강의를 들으면서 개발한 인스타그램 클론 프로젝트입니다.

강의 : 인프런-하울의 안드로이드 인스타그램 클론 만들기(https://www.inflearn.com/course/%EC%9D%B8%EC%8A%A4%ED%83%80%EA%B7%B8%EB%9E%A8%EB%A7%8C%EB%93%A4%EA%B8%B0-%EC%95%88%EB%93%9C%EB%A1%9C%EC%9D%B4%EB%93%9C)
프로젝트 기간 : 2020.09.17 ~ 2020.12.08
프로젝트 인원 : 1인
```
![웅스타그램 이미지](https://user-images.githubusercontent.com/45528487/101637775-88c08a80-3a70-11eb-9ba9-16a7c6e87203.jpg)

## 결과물

ungstagram/app/release/ungstagram.apk

## 목표

```
1. 앱에 대한 이해
2. 파이어베이스에 대환 이해와 활용
3. Kotlin에 대한 이해와 활용
```
## 개발환경

```
- kotlin : 1.3.61
- gradle : 3.4.1
- compileSdkVersion : 29
- buildToolsVersion : 29.0.3
- minSdkVersion : 16
- targetSdkVersion : 29
- 개발 tool : Android Studio 3.6.1
- 형상관리 : Git / Github
```

## 프로젝트 트리
```
┌── .idea
│   ├── codeStyles
│   ├── ├── Project.xml
│   ├── └── codeStyleConfig.xml
│   ├── gradle.xml
│   ├── misc.xml
│   ├── runConfigurations.xml
│   ├── vcs.xml
├── app
│   ├── release
│   ├── ├── output.json
│   ├── └── ungstagram.apk
│   ├── src/main
│   ├── ├── java/com/hyeung/ungstagram
│   ├── ├── ├── Navigation
│   ├── ├── ├── ├── Model
│   ├── ├── ├── ├── ├── AlarmDTO.kt
│   ├── ├── ├── ├── ├── ContentDTO.kt
│   ├── ├── ├── ├── ├── FollowDTO.kt
│   ├── ├── ├── ├── └── PushDTO.kt
│   ├── ├── ├── ├── util
│   ├── ├── ├── ├── └── FcmPush.kt
│   ├── ├── ├── ├── AddPhotoActivity.kt
│   ├── ├── ├── ├── AlarmFragment.kt
│   ├── ├── ├── ├── CommentActivity.kt
│   ├── ├── ├── ├── DetailViewFragment.kt
│   ├── ├── ├── ├── GridFragment.kt
│   ├── ├── ├── └── UserFragment.kt
│   ├── ├── ├── LoginActivity.kt
│   ├── ├── └── MainActivity.kt
│   ├── ├── res 
│   ├── ├── └── ...
│   ├── ├── AndroidManifest.xml
│   ├── └── ic_launcher_ung-playstore.png
├── gradle/wrapper
│   ├── gradle-wrapper.jar
│   ├── gradle-wrapper.properties
├── .gitignore
├── README.md
├── build.gradle
├── google-services.json
└── proguard-rules.pro
```

## 사용한 모듈

```
"firebase-auth" : "18.0.0"
"firebase-storage" : "18.0.0"
"firebase-firestore" : "18.0.0"
"firebase-messaging" : "12.0.0"
"facebook-android-sdk" : "4.5"
"okhttp3" : "3.4.1"
"gson" : "2.8.6"
"glide" : "4.9.0"
```
