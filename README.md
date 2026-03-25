# 🍳 食见 (Shi Jian) - Android

> 🚀 **让 AI 为你的每一餐注入灵感！**  
> 这是一款基于 **Neo-Brutalism** 设计风格，深度整合 AI 能力的智能菜谱创作助手。
> 
> 💡 **灵感来源**：本项目灵感源于 Web 开源项目 <a href="https://github.com/liu-ziting/what-to-eat" target="_blank">一饭封神(what-to-eat)</a>。

[![License](https://img.shields.io/badge/License-CC_BY--NC--SA_4.0-lightgrey.svg)](https://github.com/RemotePinee/Shi-Jian/blob/main/LICENSE)
[![JDK](https://img.shields.io/badge/JDK-21-blue.svg)](https://www.oracle.com/java/technologies/downloads/#java21)
[![Platform](https://img.shields.io/badge/Platform-Android_12%2B-green.svg)](https://developer.android.com/)
[![Compose](https://img.shields.io/badge/UI-Jetpack_Compose-4285F4.svg)](https://developer.android.com/jetpack/compose)

---

## ⚠️ 版权与商用声明

**本项目严禁任何形式的未经授权商务用途，严禁以此项目为基础进行二次开发后用于营利。**

- **允许**：个人学习、技术研究、非盈利性质的社区分享。
- **禁止**：
    - 将其作为付费产品销售或作为商业服务的补充。
    - **修改代码、更换 UI 或提取核心逻辑后进行二次封装，并以营利为目的分发。**
    - 在未获得作者授权的情况下用于任何盈利性活动。
- **授权**：如需商用或基于本项目进行商业性质的定制开发，请务必联系作者获取正式授权。

---

## ✨ 核心特性

- **🎨 视觉美学**：采用大胆的 **Neo-Brutalism (新丑风)** 界面设计，极高对比度、粗边框、鲜亮色彩，打破传统沉闷的 UI 布局。
- **🎤 多模态食材输入**：
    - **文字录入**：快速手动添加现有食材。
    - **语音识别**：对着手机说话，AI 自动解析食材。
    - **图库/相机识别**：通过拍照或相册选择，快速识别已有食材。
- **🪄 AI 菜谱创作**：基于您提供的食材和选定的菜系，AI 将为您量身定制详细的烹饪步骤、营养分析及配酒建议。
- **🖼️ AI 菜品图鉴**：不仅有文字，还能一键生成极具艺术感的菜品渲染图，让“脑补”化为现实。
- **🧪 酱汁实验室**：独家酱汁设计功能，为您解决“调料怎么放”的世纪难题。
- **📦 收藏与探索**：保存您的每一次灵感创作，随时回顾“一饭封神”的瞬间。

---

## 🛠️ 技术架构

- **语言**：[Kotlin](https://kotlinlang.org/) (最新稳定版)
- **UI 框架**：[Jetpack Compose](https://developer.android.com/jetpack/compose) (声明式 UI，完全符合最新 Material 3 标准)
- **编译/目标 SDK**：**Android 36**
- **运行时环境**：**JDK 21**
- **架构模式**：MVVM + Clean Architecture 思想
- **依赖注入**：手动依赖注入 (Manual DI)，通过接口与构造函数传递依赖，保证解耦且易于测试
- **数据持久化**：[Room](https://developer.android.com/training/data-storage/room) (本地数据库)
- **图像处理**：Coil (图片加载)

---

## 🚀 快速开始

### 开发环境要求

1. **Android Studio**: 建议使用最新版 (Ladybug 2024.2.1 或更高)。
2. **JDK**: 必须配置为 **JDK 21**。
3. **Android 设备**: 建议系统版本 Android 12 (API 31) 或以上，以获得最佳视觉效果。

### 编译运行

```bash
# 1. 克隆项目
git clone https://github.com/RemotePinee/Shi-Jian.git

# 2. 用 Android Studio 打开项目
```

---

## 📁 目录结构

```text
app/src/main/java/com/eatwhat/
├── data/               # 数据层
│   ├── api/            # Retrofit 接口定义
│   ├── local/          # Room 数据库与 DAO
│   ├── model/          # 数据实体模型
│   └── repository/     # 业务仓库层 (处理数据逻辑)
├── ui/                 # UI 层
│   ├── components/     # 全局通用的 Neo-Brutalism 特色组件
│   ├── screens/        # 各个功能页面 (Home, AiChat, Discovery, SauceDesign 等)
│   ├── theme/          # 颜色、字体及 Neo-Brutalism 主题定义
│   └── viewmodel/      # Jetpack ViewModel (持有 UI 状态)
├── util/               # 辅助工具类
├── MainActivity.kt     # 唯一 Activity 入口，处理导航逻辑
└── MainApplication.kt  # Application 全局初始化
```

---

## 🤝 贡献指南

我们欢迎所有形式的贡献（只要不涉及商用协议的违反）：
1. 提交 Bug 报告或功能建议。
2. 参与 UI 细节优化（我们非常注重 Neo-Brutalism 风格的纯粹性）。
3. 改进 AI Prompt 逻辑。

详情请参考 [CONTRIBUTING.md](./CONTRIBUTING.md)。

---

## 🙏 致谢

- 感谢 <a href="https://github.com/liu-ziting" target="_blank">liuziting</a> 提供的优秀灵感与 Web 端参考。
- 感谢 <a href="https://developer.android.com/jetpack/compose" target="_blank">Jetpack Compose</a> 让现代 Android 开发如此优雅。
