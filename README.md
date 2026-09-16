# 上大课表 SHU-Class-Schedule

上海大学课程表 Android App：从教务系统（正方 `jwxt.shu.edu.cn`）一键导入课表，无广告、无跟踪、数据全在本地。

[![CI](https://github.com/zmdld11/SHU-Class-Schedule/actions/workflows/ci.yml/badge.svg)](https://github.com/zmdld11/SHU-Class-Schedule/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/zmdld11/SHU-Class-Schedule)](https://github.com/zmdld11/SHU-Class-Schedule/releases)

## 功能

### 课表导入
- **SSO 登录抓取**：WebView 打开学校统一身份认证，登录后自动进入教务课表查询页，读取教务自己的学年/学期下拉框（真实学期编码，不做硬编码假设——上大教务改版后学期编码非标准且会变，实测 秋=3、春=16、夏=32）
- **预览核对**：抓取后先列出全部课程与时段，确认无误再写入；开学日期可修改
- **调课处理**：教务的调课记录（单周换教师）按周精确展示——平时显示原教师，调课那一周显示调课教师并带「调课」标记
- 密码只进学校官方登录页，本应用不保存任何凭据

### 周视图
- 同课同色、今日高亮、周次切换（箭头 / 点击跳转 / **左右滑动手势 + 滑动动画**）
- 默认只显示工作日 5 列（上大绝大多数周末无课），可在设置中打开周六周日
- 「显示非本周课程」：本周有时段课的槽位不干扰，无课槽位置灰显示最近一次未来上课安排
- 课程块内 课名 / @教室 / 教师 / 校区 分行展示；点击查看详情（学分、教学班、各时段本周有无课）

### 课程编辑
- 手动添加课程（教务里没有的活动、补录往期课表）
- 编辑任意时段的 星期/节次/周次/教室/教师/校区（教务信息与实际不符时自己改）
- 周次文本支持 `1-16周`、`2-16周(双)`、`1,5,9周` 等写法，实时预览解析结果
- 删除时段 / 删除整门课（删光最后一个时段自动连课程删除）

### 学期管理
- 顶栏下拉快捷切换学期，回看任意学期
- 手动添加空白学期（补录查不到的往期课表）
- 冬季学期默认隐藏（教务系统目前查不到冬季数据），设置中可开

### 桌面小组件
- 2×2 下一节课卡片 / 4×2 / 4×4 今日课程列表，点击任意位置打开应用

### 其他
- 12 节默认作息可编辑、可一键恢复默认；节次结束时间显示开关
- JSON 全量备份 / 恢复，换机不丢数据
- Material 3 动态取色，支持深色模式
- 主题：默认上大蓝 / 明日方舟（默认版），支持导入 `.shutheme` 主题包，桌面小组件配色随主题（[主题与主题包说明](docs/themes.md)）

## 下载安装

从 [Releases](https://github.com/zmdld11/SHU-Class-Schedule/releases) 下载，两个安装包同签名可互相覆盖安装：

| 安装包 | 说明 |
| --- | --- |
| `shu-schedule-vX.Y.Z.apk`（默认版） | 全功能：内置明日方舟主题 + `.shutheme` 主题包导入 |
| `shu-schedule-pure-vX.Y.Z.apk`（纯净版） | 仅默认主题，无主题导入，体积更小 |

- 系统要求：Android 8.0（API 26）及以上
- **v0.3.2 起使用固定签名**：从 v0.3.2+ 升级可直接覆盖安装；从更早版本升级因签名切换需先卸载（卸载前请在 设置 → 备份与恢复 导出 JSON，装好后恢复）
- 应用内检查更新按当前安装的变体自动匹配下载对应 APK（GitHub 直链，国内网络可能较慢）

## 已知限制

- 教务系统目前查不到冬季学期课表（只留接口），冬季需手动补录
- 教务深路径在未登录状态下会跳转到不可用的登录页，因此导入流程全程不离开受控 WebView
- 调课识别基于教务「第X周」写法，教务若改格式需跟进适配（可在导入预览页「复制原始数据」反馈）

## 开发

```bash
# 需 JDK 17 与 Android SDK（platforms;android-35, build-tools;35.0.0）
./gradlew assembleDebug   # 产物在 app/build/outputs/apk/debug/
./gradlew test            # JVM 单元测试（35 个）
```

**技术栈**：Kotlin · Jetpack Compose · Material 3 · MVVM · Room · DataStore · Hilt · 单模块，minSdk 26 / target & compileSdk 35 / JDK 17

**关键模块**：

| 路径 | 职责 |
|---|---|
| `data/jwxk/` | 教务导入契约（SSO 入口、同源抓取脚本、学期编码发现） |
| `data/parser/` | 正方 kbList / 周次文本解析、学期编码、默认作息 |
| `data/db/` | Room 实体与 DAO（学期/课程/时段/节次） |
| `data/backup/` | JSON 备份编解码（向前兼容） |
| `ui/schedule/` | 周视图、课程编辑表单 |
| `ui/importer/` | WebView 登录导入流程 |
| `widget/` | 三档桌面小组件 |

## 路线图

- [x] 课表导入与周视图（v0.1.0）
- [x] 小组件 / 显示优化 / 课程编辑 / 学期切换（v0.2–v0.3）
- [ ] 课前提醒（[#6](https://github.com/zmdld11/SHU-Class-Schedule/issues/6)）
- [ ] ICS 日历导出（[#7](https://github.com/zmdld11/SHU-Class-Schedule/issues/7)）

## 贡献

仓库采用 issue 驱动工作流，提交前请阅读 [CONTRIBUTING.md](CONTRIBUTING.md)。

导入接口的口径参考了同作者的 [SHU-jwxk-assistant](https://github.com/zmdld11/SHU-jwxk-assistant)（选课助手）的长期实践。

## 许可

[MIT](LICENSE)
