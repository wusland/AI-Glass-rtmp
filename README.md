# AI-Glass-rtmp Android Client

这是一个基于pedroSG94的RTMP库开发的Android RTMP直播客户端应用。

## 功能特性

- 调用设备摄像头进行视频采集
- 实时音频录制
- RTMP协议推流到服务器
- 支持视频录制到本地
- 支持前后摄像头切换
- 支持横屏/竖屏模式切换
- 实时码率显示和自适应调节

## 开发环境要求

- Android API 21+ (Android 5.0+)
- Kotlin 2.1.0
- Gradle 8.2+

## 构建和运行

1. 克隆或下载项目代码
2. 使用Android Studio打开项目
3. 连接Android设备或启动模拟器
4. 点击运行按钮

## 使用说明

1. 启动应用后，输入RTMP服务器地址
2. 点击播放按钮开始推流
3. 点击录制按钮可以同时录制本地视频
4. 点击摄像头切换按钮可以在前后摄像头之间切换

## 依赖库

- pedroSG94/rtmp-rtsp-stream-client-java: RTMP推流库
- AndroidX: Android Jetpack组件

## 权限要求

- 相机权限 (CAMERA)
- 录音权限 (RECORD_AUDIO)
- 存储权限 (WRITE_EXTERNAL_STORAGE)
- 网络权限 (INTERNET)

## 许可证

本项目基于Apache License 2.0许可证开源。

