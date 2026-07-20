# Flowday Widget APK

原生 Android 桌面小组件，与 Flowday PWA 通过本地 HTTP 同步数据。

## 工作原理

```
Flowday PWA (Samsung Internet)  ──POST──▶  localhost:18765/update  ──▶  SharedPreferences  ──▶  Widget
```

1. 安装此 APK 后，打开一次 Flowday Widget App 启动本地同步服务
2. 在桌面添加小组件：长按桌面 → 小组件 → Flowday Widget
3. 打开 Flowday PWA，每次添加/完成任务，自动推送到小组件
4. 小组件实时显示今日进度和任务列表

## 构建方法

### 方式一：Android Studio（推荐）
1. 下载 [Android Studio](https://developer.android.com/studio)
2. 打开 FlowdayWidget 目录
3. 等待 Gradle 同步完成
4. Build → Build Bundle(s) / APK(s) → Build APK(s)
5. 在 `app/build/outputs/apk/debug/` 找到 APK

### 方式二：命令行
```bash
# 确保安装了 Android SDK 和 Java 17+
export ANDROID_HOME=/path/to/android/sdk
./gradlew assembleDebug
```

## 安装到手机

1. 将 APK 传输到三星 S25+
2. 打开 APK 文件，允许「未知来源」安装
3. 安装后打开 Flowday Widget App（仅需一次，启动同步服务）
4. 桌面添加小组件：长按 → 小组件 → Flowday Widget

## 项目结构

```
app/src/main/java/com/flowday/widget/
├── FlowdayWidget.java        # AppWidgetProvider - 小组件渲染
├── LocalServerService.java   # 前台服务 - 本地 HTTP 服务器
├── WidgetUpdateService.java  # 小组件刷新服务
└── SettingsActivity.java     # 设置页 - 启动服务
```

## 技术说明

- 本地 HTTP 服务器监听 `localhost:18765`，仅接收本机请求
- PWA 通过 `fetch()` POST 任务 JSON 数据
- 小组件每 30 分钟自动刷新一次
- 点击小组件跳转到 Flowday PWA
