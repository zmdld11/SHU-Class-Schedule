# 应用主题

在「设置 → 应用主题」中选择主题，即时生效并通过 DataStore 保存在本机。

- **默认**：上大蓝，跟随系统深浅色，Android 12+ 可动态取色（完整版与纯净版均内置）。
- **明日方舟**（仅完整版）：固定深色，罗德岛终端配色、切角面板、工业背景与独立八色课程色板。素材来自 [mashirozx/arknights-ui]，版权归原权利人，仅供学习。
- **主题包**（仅完整版）：从 `.shutheme` 文件导入第三方主题，见下文。

自选课表图片优先于主题内置背景；清除自选图片后恢复主题背景。课程数据与 `colorIndex` 不随主题切换改写。

## 主题包（.shutheme）

主题包 = zip 压缩包，包含：

| 文件 | 说明 |
| --- | --- |
| `theme.json` | 必需。主题元数据 + 配色定义 |
| `background.jpg|png|webp` | 可选。课表内置背景 |

`theme.json` 字段（未知字段忽略；颜色格式 `#RRGGBB` 或 `#AARRGGBB`）：

```json
{
  "id": "my-theme",            // 必需：^[a-z0-9_-]{1,32}$，重复导入同 id = 覆盖更新
  "name": "我的主题",           // 必需
  "description": "…", "version": 1, "author": "…",
  "fixedDark": true,           // true=固定深色（忽略系统深浅色）；false=浅色基线
  "colors": {                  // 全部可选：Material 3 槽位，缺省回落内置基线
    "primary": "#…", "onPrimary": "#…", "primaryContainer": "#…", "onPrimaryContainer": "#…",
    "secondary": "#…", "onSecondary": "#…", "secondaryContainer": "#…", "onSecondaryContainer": "#…",
    "tertiary": "#…", "onTertiary": "#…", "tertiaryContainer": "#…", "onTertiaryContainer": "#…",
    "background": "#…", "onBackground": "#…", "surface": "#…", "onSurface": "#…",
    "surfaceVariant": "#…", "onSurfaceVariant": "#…", "outline": "#…",
    "error": "#…", "onError": "#…"
  },
  "courseColors": [["#容器色", "#文字色"], …],  // 4-16 组取模；缺省用默认八色
  "shape": { "type": "rounded|cut", "radius": 0-24 },
  "widget": {                  // 可选：桌面小组件配色
    "rootBg": "#…", "itemBg": "#…", "textPrimary": "#…", "textSecondary": "#…"
  }
}
```

**安全模型**：主题包是纯数据（颜色/形状/一张背景图），应用不加载其中任何代码；导入时只接收 `theme.json` 与 `background.*` 两类条目。

**导入**：设置 → 应用主题 → 「导入主题包（.shutheme）」→ 选择文件。已导入的主题出现在列表中（标记「导入」），**长按可删除**；正被使用的主题删除后自动回落默认。

示例包见 [`samples/demo-ocean.shutheme`](samples/demo-ocean.shutheme)。

## 桌面小组件主题

主题定义包含小组件配色（`widget` 节）：根背景、列表项背景、主/次文字色。文字颜色全版本生效；根/项背景在 **Android 12+** 通过 backgroundTint 生效（保留圆角），Android 8-10 保持默认观感。切换主题后小组件随下一次刷新（打开应用即触发）换色。

## 代码扩展（内置主题）

1. 在 `data/settings/AppTheme.kt` 添加稳定 ID 与元信息（持久化使用 ID，不能改名）。
2. 实现一个 `ScheduleThemeDefinition`（配色、形状、`ScheduleStyle`、小组件色），登记进 `ThemeCatalog`（full 变体在 `ui/theme/ThemeSource.kt` 的 `builtInThemes()`；纯净版不登记额外内置主题）。
3. 图片资源放 `src/full/res/drawable-nodpi`，经 `backgroundRes`/`logoRes` 提供给渲染层。
4. 增补对比度测试（`ThemeContrastTest`）。

渲染管线（`ShuScheduleTheme` → `ThemePicker` → `ScheduleScaffold` → 小组件 `WidgetTheming`）只认 `ThemeCatalog`，不感知具体主题。

## 双包分发

| 变体 | 内容 |
| --- | --- |
| `full`（默认版 APK `shu-schedule-vX.Y.Z.apk`） | 默认 + 明日方舟主题、`.shutheme` 导入（主题商店） |
| `pure`（纯净版 APK `shu-schedule-pure-vX.Y.Z.apk`） | 仅默认主题，无导入入口，体积更小 |

两包同包名同签名，可互相覆盖安装；应用内更新检查按当前安装的变体自动匹配对应 APK。

## 主题商店路线（未实现）

`ThemeCatalog.register()` 是在线商店的接入点：未来从 GitHub 上的主题目录（index.json + 包直链）下载 `.shutheme` 后走与本地导入完全相同的管线，无需自建服务器。当前刻意未实现下载/在线列表，等有真实主题再上。

## 素材来源与版权

明日方舟主题素材来自 [mashirozx/arknights-ui]，固定版本 `8fb68d35992467c0cca9de953c5bd6227c316b97`。

| 本地文件 | 上游文件 | 处理 |
| --- | --- | --- |
| `app/src/full/res/drawable-nodpi/arknights_background.webp` | `img/UI_HOME_FRONT_BKG.png` | Pillow 等比缩小至 1280×720 范围，WebP quality=85 / method=6 |
| `app/src/full/res/drawable-nodpi/arknights_rhodes_island.png` | `img/UI_HOME.png` | 裁剪 `(486,1230,690,1408)`；亮度用作 alpha，白色前景，移除黑色面板 |

上游代码采用 MIT，许可证副本见 `third_party/arknights-ui-LICENSE.txt`。**游戏贴图不因此变为 MIT 授权素材**。上游 README 明确注明：「界面贴图素材都是游戏逆向出来的，仅供学习使用，请勿商用。」贴图、标识及相关角色版权归原权利人；本主题为非官方学习用途，无官方关联或背书。应用设置中同时展示来源和用途说明。

## 验证

```sh
./gradlew testFullDebugUnitTest testPureDebugUnitTest assembleFullDebug assemblePureDebug
```

人工回归：两款内置主题切换 → 重启保留 → 动态取色偏好恢复；导入 `samples/demo-ocean.shutheme` → 切换 → 重启 → 长按删除回落；小组件换色；纯净版无导入入口无明日方舟。

[mashirozx/arknights-ui]: https://github.com/mashirozx/arknights-ui
