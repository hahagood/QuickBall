# QuickBall

轻量级 Android 悬浮球快捷工具。一键调出常用功能，免翻多级菜单。

APK 约 37KB，纯 Android Framework API，零外部依赖。

## 功能

- **自定义快捷方式** — 添加任意 App 及其子功能（扫码、付款码等）
- **系统功能** — 切换流量卡、Wi-Fi、蓝牙、热点、手电筒、截屏等 12 项
- **已知 App 深度集成** — 微信（扫一扫、付款码）、支付宝（扫一扫、付款码、乘车码）、哈啰（扫码）等预置快捷方式
- **链式回退** — 每个快捷方式支持多种启动方式，按优先级依次尝试
- **深色/浅色模式** — 配置界面自动跟随系统主题

## 交互

- 点击半隐藏的球 → 滑出并弹出菜单
- 拖动 → 改变位置，松手自动吸附到最近边缘（隐藏状态也可拖动）
- 长按 0.8 秒 → 打开配置页面
- 3 秒无操作 → 自动滑到屏幕边缘半隐藏
- 点击菜单外 → 收起菜单并隐藏

## 配置页面

- 添加/删除/排序快捷方式
- 从已安装 App 列表中选择，支持搜索
- 自动发现 App 子功能（微信扫一扫、支付宝付款码等）
- 自定义名称和颜色
- 恢复默认配置
- 通知栏也可进入配置

## 常驻机制

- 前台服务 + `START_STICKY`
- `onTaskRemoved` / `onDestroy` 中 AlarmManager 定时重启
- 开机自启（BootReceiver）
- 首次启动请求忽略电池优化

## 构建

依赖命令行工具链，不需要 Gradle / Android Studio。

```bash
AAPT=$ANDROID_SDK/build-tools/35.0.1/aapt
D8=$ANDROID_SDK/build-tools/35.0.1/d8
APKSIGNER=$ANDROID_SDK/build-tools/35.0.1/apksigner
ANDROID_JAR=$ANDROID_SDK/platforms/android-34/android.jar

# 生成 R.java
$AAPT package -f -m -J src -M AndroidManifest.xml -S res -I $ANDROID_JAR

# 编译
javac -source 1.8 -target 1.8 -bootclasspath $ANDROID_JAR \
  -classpath $ANDROID_JAR -d obj src/com/quickball/*.java

# DEX
$D8 obj/com/quickball/*.class --output dex/ --lib $ANDROID_JAR

# 打包
$AAPT package -f -M AndroidManifest.xml -S res -I $ANDROID_JAR -F QuickBall.unsigned.apk
cd dex && zip -j ../QuickBall.unsigned.apk classes.dex && cd ..

# 签名
$APKSIGNER sign --ks debug.keystore --ks-pass pass:android \
  --ks-key-alias quickball --key-pass pass:android \
  --out QuickBall.apk QuickBall.unsigned.apk

# 安装
adb install -r QuickBall.apk
```

首次构建需用 `keytool` 生成 `debug.keystore`。

## 项目结构

```
QuickBall/
├── AndroidManifest.xml
├── res/
│   ├── drawable/
│   │   ├── ball_bg.xml                 # 圆形背景
│   │   └── ic_scan.xml                 # 图标
│   ├── values/
│   │   └── strings.xml
│   └── xml/
│       └── accessibility_config.xml    # 无障碍服务配置（截屏）
└── src/com/quickball/
    ├── MainActivity.java               # 权限引导 + 启动服务
    ├── FloatingBallService.java         # 悬浮球 + 菜单 + 拖动 + 隐藏
    ├── ConfigActivity.java              # 快捷方式配置界面
    ├── ShortcutConfig.java              # 数据模型 + 持久化 + 启动引擎
    ├── QuickBallAccessibilityService.java  # 截屏服务
    └── BootReceiver.java               # 开机自启
```

## 要求

- Android 8.0+（API 26）
- 悬浮窗权限
- 截屏功能需启用无障碍服务

## 许可

MIT
