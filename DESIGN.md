---
version: alpha
name: exam-flow-gov-design
description: "A government / state-owned-enterprise style design system for exam-flow: authoritative crimson red (#C7000B) as the single brand accent on a white canvas, deep navy (#1F4E79) for the top rail and footer, gold (#C9A063) used sparingly for ceremonial dividers. Corners stay at 0–4px, shadows are nearly flat, and depth is carried by 1px hairlines and surface change. Display and heading type run Noto Sans SC (Heiti) at weight 700; body text runs Noto Serif SC (Songti) at weight 400 — the classic Chinese government-site pairing of bold heiti headlines over serif body. Layout is symmetric, grid-aligned, centered at a 1200px container, with full-width crimson banners, announcement lists, and hairline tables. The system reads as solemn, authoritative, and standardized."
colors:
  primary: "#C7000B"
  on-primary: "#FFFFFF"
  primary-hover: "#A60008"
  primary-pressed: "#8A0007"
  navy: "#1F4E79"
  on-navy: "#FFFFFF"
  gold: "#C9A063"
  ink: "#1A1A1A"
  ink-muted: "#555555"
  ink-subtle: "#8C8C8C"
  canvas: "#FFFFFF"
  surface-1: "#F5F5F5"
  surface-2: "#EDEDED"
  inverse-canvas: "#1F4E79"
  inverse-surface-1: "#2B5E8F"
  inverse-ink: "#FFFFFF"
  inverse-ink-muted: "#B8C8D9"
  hairline: "#E3E3E3"
  hairline-strong: "#C7000B"
  semantic-success: "#2E8B57"
  semantic-warning: "#C77400"
  semantic-error: "#D93025"
  semantic-info: "#1F4E79"

typography:
  display-lg:
    fontFamily: "Noto Sans SC, Microsoft YaHei, PingFang SC, sans-serif"
    fontSize: 44px
    fontWeight: 700
    lineHeight: 1.3
    letterSpacing: 2px
  display-md:
    fontFamily: "Noto Sans SC, Microsoft YaHei, PingFang SC, sans-serif"
    fontSize: 36px
    fontWeight: 700
    lineHeight: 1.3
    letterSpacing: 1px
  headline:
    fontFamily: "Noto Sans SC, Microsoft YaHei, PingFang SC, sans-serif"
    fontSize: 28px
    fontWeight: 700
    lineHeight: 1.35
    letterSpacing: 0.5px
  section-title:
    fontFamily: "Noto Sans SC, Microsoft YaHei, PingFang SC, sans-serif"
    fontSize: 24px
    fontWeight: 700
    lineHeight: 1.4
    letterSpacing: 0.5px
  card-title:
    fontFamily: "Noto Sans SC, Microsoft YaHei, PingFang SC, sans-serif"
    fontSize: 18px
    fontWeight: 700
    lineHeight: 1.4
    letterSpacing: 0.5px
  body:
    fontFamily: "Noto Serif SC, SimSun, serif"
    fontSize: 16px
    fontWeight: 400
    lineHeight: 1.75
    letterSpacing: 0.5px
  body-sm:
    fontFamily: "Noto Serif SC, SimSun, serif"
    fontSize: 14px
    fontWeight: 400
    lineHeight: 1.6
    letterSpacing: 0.5px
  body-emphasis:
    fontFamily: "Noto Serif SC, SimSun, serif"
    fontSize: 16px
    fontWeight: 600
    lineHeight: 1.6
    letterSpacing: 0.5px
  caption:
    fontFamily: "Noto Sans SC, Microsoft YaHei, PingFang SC, sans-serif"
    fontSize: 12px
    fontWeight: 400
    lineHeight: 1.5
    letterSpacing: 0.5px
  button:
    fontFamily: "Noto Sans SC, Microsoft YaHei, PingFang SC, sans-serif"
    fontSize: 16px
    fontWeight: 400
    lineHeight: 1.5
    letterSpacing: 1px
  eyebrow:
    fontFamily: "Noto Sans SC, Microsoft YaHei, PingFang SC, sans-serif"
    fontSize: 14px
    fontWeight: 400
    lineHeight: 1.5
    letterSpacing: 2px

rounded:
  none: 0px
  xs: 2px
  sm: 4px
  md: 6px
  lg: 8px
  pill: 9999px
  full: 9999px

spacing:
  xxs: 4px
  xs: 8px
  sm: 12px
  md: 16px
  lg: 24px
  xl: 32px
  xxl: 48px
  xxxl: 64px
  section: 80px

components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    typography: "{typography.button}"
    rounded: "{rounded.none}"
    padding: 12px 32px
    border: 1px solid "{colors.primary}"
  button-primary-hover:
    backgroundColor: "{colors.primary-hover}"
    textColor: "{colors.on-primary}"
    typography: "{typography.button}"
    rounded: "{rounded.none}"
    padding: 12px 32px
  button-primary-pressed:
    backgroundColor: "{colors.primary-pressed}"
    textColor: "{colors.on-primary}"
    typography: "{typography.button}"
    rounded: "{rounded.none}"
    padding: 12px 32px
  button-outline:
    backgroundColor: "{colors.canvas}"
    textColor: "{colors.primary}"
    typography: "{typography.button}"
    rounded: "{rounded.none}"
    padding: 12px 32px
    border: 1px solid "{colors.primary}"
  button-outline-hover:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    typography: "{typography.button}"
    rounded: "{rounded.none}"
    padding: 12px 32px
  button-plain:
    backgroundColor: "{colors.canvas}"
    textColor: "{colors.ink-muted}"
    typography: "{typography.button}"
    rounded: "{rounded.none}"
    padding: 12px 24px
  button-disabled:
    backgroundColor: "{colors.surface-2}"
    textColor: "{colors.ink-subtle}"
    typography: "{typography.button}"
    rounded: "{rounded.none}"
    padding: 12px 32px
  nav-link:
    backgroundColor: transparent
    textColor: "{colors.ink}"
    typography: "{typography.eyebrow}"
    rounded: "{rounded.none}"
    padding: 12px 16px
  nav-link-active:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    typography: "{typography.eyebrow}"
    rounded: "{rounded.none}"
    padding: 12px 16px
  nav-link-hover:
    backgroundColor: "{colors.surface-1}"
    textColor: "{colors.primary}"
    typography: "{typography.eyebrow}"
    rounded: "{rounded.none}"
    padding: 12px 16px
  top-rail:
    backgroundColor: "{colors.navy}"
    textColor: "{colors.inverse-ink-muted}"
    typography: "{typography.caption}"
    rounded: "{rounded.none}"
    height: 36px
  top-nav:
    backgroundColor: "{colors.canvas}"
    textColor: "{colors.ink}"
    typography: "{typography.eyebrow}"
    rounded: "{rounded.none}"
    height: 64px
  hero-banner:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    typography: "{typography.display-md}"
    rounded: "{rounded.none}"
    padding: 64px 32px
  card:
    backgroundColor: "{colors.canvas}"
    textColor: "{colors.ink}"
    typography: "{typography.body-sm}"
    rounded: "{rounded.none}"
    padding: 24px
    border: 1px solid "{colors.hairline}"
  card-hover:
    backgroundColor: "{colors.surface-1}"
    textColor: "{colors.ink}"
    typography: "{typography.body-sm}"
    rounded: "{rounded.none}"
    padding: 24px
    border: 1px solid "{colors.primary}"
  stat-card:
    backgroundColor: "{colors.canvas}"
    textColor: "{colors.ink}"
    typography: "{typography.card-title}"
    rounded: "{rounded.none}"
    padding: 24px
    border-top: 3px solid "{colors.primary}"
  text-input:
    backgroundColor: "{colors.canvas}"
    textColor: "{colors.ink}"
    typography: "{typography.body}"
    rounded: "{rounded.none}"
    padding: 10px 12px
    border: 1px solid "{colors.hairline}"
  text-input-focused:
    backgroundColor: "{colors.canvas}"
    textColor: "{colors.ink}"
    typography: "{typography.body}"
    rounded: "{rounded.none}"
    padding: 10px 12px
    border: 1px solid "{colors.primary}"
  text-input-error:
    backgroundColor: "{colors.canvas}"
    textColor: "{colors.ink}"
    typography: "{typography.body}"
    rounded: "{rounded.none}"
    padding: 10px 12px
    border: 1px solid "{colors.semantic-error}"
  tag:
    backgroundColor: "{colors.surface-1}"
    textColor: "{colors.ink-muted}"
    typography: "{typography.caption}"
    rounded: "{rounded.none}"
    padding: 2px 8px
    border: 1px solid "{colors.hairline}"
  tag-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    typography: "{typography.caption}"
    rounded: "{rounded.none}"
    padding: 2px 8px
  table-header:
    backgroundColor: "{colors.surface-1}"
    textColor: "{colors.ink}"
    typography: "{typography.body-sm}"
    rounded: "{rounded.none}"
    padding: 12px 16px
    fontWeight: 600
  table-cell:
    backgroundColor: "{colors.canvas}"
    textColor: "{colors.ink-muted}"
    typography: "{typography.body-sm}"
    rounded: "{rounded.none}"
    padding: 12px 16px
  table-row-hover:
    backgroundColor: "{colors.surface-1}"
    textColor: "{colors.ink-muted}"
    typography: "{typography.body-sm}"
    rounded: "{rounded.none}"
    padding: 12px 16px
  divider-title:
    backgroundColor: transparent
    textColor: "{colors.ink}"
    typography: "{typography.section-title}"
    rounded: "{rounded.none}"
    padding: 0 0 16px 0
    border-left: 4px solid "{colors.primary}"
  footer:
    backgroundColor: "{colors.inverse-canvas}"
    textColor: "{colors.inverse-ink-muted}"
    typography: "{typography.caption}"
    rounded: "{rounded.none}"
    padding: 48px 32px 24px 32px
---

## Overview

exam-flow 的设计系统是**中国政务 / 国企门户网站风格**的忠实呈现:以政务红 `{colors.primary}` (#C7000B) 为唯一品牌强调色,白底画布承载内容,深藏青 `{colors.navy}` (#1F4E79) 用于顶部通栏与页脚,金色 `{colors.gold}` 只出现在极少的仪式性分隔细节中。整体气质是**庄重、严谨、权威、规范**——信息可以密集,但秩序必须一目了然。

核心定义是**直角与细线**:所有按钮、卡片、输入框、容器使用 0px 直角(`{rounded.none}`)或至多 4px 的小圆角,以 1px 细线(`{colors.hairline}`)划分层级,基本不使用投影和渐变大色块。层级靠**表面色变化 + 1px 边框 + 红色强调**表达,而不是靠阴影。

字体组合沿用政企网站的经典搭配:**标题与按钮用黑体**(Noto Sans SC / 微软雅黑,字重 700),**正文用宋体**(Noto Serif SC / SimSun,字重 400)。黑体传达官方的坚决,宋体承载正文的可读性——这是中国政务门户的标志性排版。

**关键特征:**
- **政务红即品牌**:`{colors.primary}` #C7000B 承担所有链接、主按钮、横幅底色、当前导航项、标题左侧竖线。全系统只有这一个强调色。
- **直角、细线、扁平**:0–4px 圆角,1px 边框,几乎无阴影。任何渐变、大圆角、重阴影都是对风格的破坏。
- **黑体标题 + 宋体正文**:显示级与标题一律 Noto Sans SC 700;正文一律 Noto Serif SC 400。不要用西文无衬线替代中文正文。
- **顶部结构固定**:深蓝通栏(站内链接、日期)→ 白色主导航(当前项红色底白字)→ 全宽红色横幅。
- **对称与居中**:页面以 1200px 容器居中,模块对称排布,满行横幅,不出现不对称的漂浮元素。
- **深蓝页脚**:`{colors.inverse-canvas}` #1F4E79 深蓝底,浅灰蓝文字,含 ICP 备案号等法定信息。
- **表格化信息呈现**:考试计划、成绩、公告等以细线表格呈现,斑马纹不适用,悬停行为浅灰底。
- **页面节奏**:顶部通栏 → 主导航 → 红色横幅 → 公告/新闻列表 → 信息卡片网格 → 表格区 → 深蓝页脚。

## Colors

> 参考来源:中国政府网 (gov.cn) 风格门户、省市政府门户、国企官网的通用视觉语言。

### Brand & Accent
- **政务红** ({colors.primary}):全系统唯一强调色。链接、主按钮、横幅、当前导航项、标题装饰竖线、焦点描边。
- **红 Hover** ({colors.primary-hover}):主按钮与链接的悬停态。
- **红 Pressed** ({colors.primary-pressed}):主按钮按下态。

### Supporting Surfaces
- **深藏青** ({colors.navy}):顶部通栏、页脚底色、深色信息横幅。它是系统的"结构色",不是强调色——永远不与红色争夺视觉。
- **金色** ({colors.gold}):仅用于极少的仪式性装饰(如荣誉徽章、丝带分隔),使用频率低于 1% 的表面积。

### Surface
- **Canvas** ({colors.canvas}):默认页面背景,纯白。
- **Surface 1** ({colors.surface-1}):浅灰 #F5F5F5 —— 表头底、悬停行底、标签底、备用区块。
- **Surface 2** ({colors.surface-2}):#EDEDED —— 禁用底、更深的分离带。
- **Hairline** ({colors.hairline}):1px 边框,卡片、输入框、表格行分隔线。
- **Hairline Strong** ({colors.hairline-strong}):1px 政务红,焦点输入框的底线(政企风格的聚焦签名)。
- **Inverse Canvas** ({colors.inverse-canvas}):深藏青,页脚与深色横幅表面。

### Text
- **Ink** ({colors.ink}):所有标题与强调正文 #1A1A1A。
- **Ink Muted** ({colors.ink-muted}):次级文字 #555555 —— 元信息、表格正文、辅助说明。
- **Ink Subtle** ({colors.ink-subtle}):三级文字 #8C8C8C —— 禁用、占位符、脚注。
- **Inverse Ink** ({colors.inverse-ink}):深色表面的主文字,纯白。
- **Inverse Ink Muted** ({colors.inverse-ink-muted}):深色表面的次级文字 #B8C8D9 —— 页脚正文。

### Semantic
- **Success 绿** ({colors.semantic-success}):通过、成功状态。
- **Warning 琥珀** ({colors.semantic-warning}):警示、待办状态。
- **Error 红** ({colors.semantic-error}):错误、取消、不合格状态。注意:错误红比政务红更橙,二者必须可区分。
- **Info 蓝** ({colors.semantic-info}):与 navy 相同,信息性徽标。

## Typography

### Font Family

- **标题与 UI**:`Noto Sans SC`(思源黑体),回退 `Microsoft YaHei, PingFang SC, sans-serif`。字重 700 用于标题,400 用于按钮/导航。
- **正文**:`Noto Serif SC`(思源宋体),回退 `SimSun, serif`。字重 400,强调处 600。
- **数字与西文**:随字体族混排;不要在中文正文中整体切换为西文无衬线。

黑体与宋体的分工是这套系统的排版签名:**字重、字族的变化承担层级**,而不是靠字号跳变。

### Hierarchy

| Token | Size | Weight | Family | Line Height | Letter Spacing | Use |
|---|---|---|---|---|---|---|
| `{typography.display-lg}` | 44px | 700 | Heiti | 1.3 | 2px | 横幅大标题(如"全国统一考试报名系统") |
| `{typography.display-md}` | 36px | 700 | Heiti | 1.3 | 1px | 横幅主标题、一级页面标题 |
| `{typography.headline}` | 28px | 700 | Heiti | 1.35 | 0.5px | 页面标题、大区块标题 |
| `{typography.section-title}` | 24px | 700 | Heiti | 1.4 | 0.5px | 区块标题(带红色左侧竖线) |
| `{typography.card-title}` | 18px | 700 | Heiti | 1.4 | 0.5px | 卡片标题、统计数字标签 |
| `{typography.body}` | 16px | 400 | Songti | 1.75 | 0.5px | 默认正文 |
| `{typography.body-sm}` | 14px | 400 | Songti | 1.6 | 0.5px | 表格、列表、卡片正文 |
| `{typography.body-emphasis}` | 16px | 600 | Songti | 1.6 | 0.5px | 强调正文行 |
| `{typography.caption}` | 12px | 400 | Heiti | 1.5 | 0.5px | 通栏文字、页脚、标签 |
| `{typography.button}` | 16px | 400 | Heiti | 1.5 | 1px | 按钮文字(带字距,呈"官方感") |
| `{typography.eyebrow}` | 14px | 400 | Heiti | 1.5 | 2px | 导航项、小标题眉线 |

### Principles

- **黑体做骨、宋体做肉**:标题/按钮/导航一律黑体,正文一律宋体。这是政企风格区别于消费级产品的地方,不要混用。
- **字距是官方的签名**:`letter-spacing` 在标题与按钮上保持 0.5–2px 的正向字距,中文标题不加字距会显得拥挤轻浮。
- **正文行高 1.75**:宋体小字号必须靠行高保证可读性,不得压缩到 1.5 以下。
- **不使用全大写西文标题**:中文系统里没有 small-caps 的位置;英文单词按常规写法即可。
- **禁用装饰性字体**:书法体、手写体、圆体均不出现。系统只有黑体与宋体两种气质。

## Layout

### Spacing System

- **基准单位**:4px。所有间距必须是 4 的倍数。
- **Tokens (front matter)**:`{spacing.xxs}` 4px · `{spacing.xs}` 8px · `{spacing.sm}` 12px · `{spacing.md}` 16px · `{spacing.lg}` 24px · `{spacing.xl}` 32px · `{spacing.xxl}` 48px · `{spacing.xxxl}` 64px · `{spacing.section}` 80px。
- 卡片内边距 24px;横幅内边距 64px 上下;页脚 48px 上下。
- 按钮 12px 纵向 · 32px 横向;输入框 10px 纵向 · 12px 横向。

### Grid & Container

- 桌面端 12 列栅格,内容容器**最大宽度 1200px 居中**,两侧留白自然。这是政企门户的标志性版心宽度。
- 卡片网格:桌面 4 列 → 平板 2 列 → 移动 1 列。
- 横幅(banner)全宽铺满,内部文字仍对齐 1200px 版心。
- 列表与表格占满版心,不出现窄栏侧挂的杂志式布局。

### Whitespace Philosophy

政企网站用**秩序**而不是留白来表达权威:区块之间以 40–80px 分隔,区块内部信息密度高。列表项、表格行紧凑排布(行高 1.6)。页面总体是"信息充分、结构清晰",而不是"空气感设计"。留白服务于对称与对齐——上下左右对齐边界必须严格一致。

### Banner & Section Header 模式

- 区块标题统一为:**左侧 4px 政务红竖线 + 24px 黑体标题**(`{components.divider-title}`),竖线居中于文字,右侧可接"更多 >"链接(红色)。
- 横幅(hero)为**纯政务红底、白色标题与正文**,无插图或最多一枚素色徽章,不适用大渐变。

## Elevation & Depth

| Level | Treatment | Use |
|---|---|---|
| 0 (flat) | 无阴影、无边框 | 正文、横幅文字、页脚 |
| 1 (hairline) | 1px `{colors.hairline}` 边框 | 卡片、输入框、表格行分隔 |
| 2 (surface lift) | `{colors.surface-1}` 底 | 表头、悬停行、悬停卡片 |
| 3 (focus) | 1px `{colors.primary}` 边框 + 红色底线 | 聚焦输入框、聚焦按钮 |

政企风格几乎不用投影。卡片浮起只通过**边框 → 浅灰底 → 红色边框**三级表达。唯一的例外是下拉面板/弹窗,允许 `0 2px 8px rgba(0, 0, 0, 0.12)` 的轻投影以与页面分离——这是功能需求,不是装饰。

### Decorative Depth

- **红色横幅允许 5% 以内的同色系加深**(#C7000B → #A60008 的垂直微渐变),保持仪式感但不喧宾夺主。
- **禁止**:大面积渐变、发光效果、毛玻璃、大阴影卡片、拟物材质。

## Shapes

### Border Radius Scale

| Token | Value | Use |
|---|---|---|
| `{rounded.none}` | 0px | **默认值**——所有按钮、卡片、输入框、容器、横幅 |
| `{rounded.xs}` | 2px | 极少数徽章 |
| `{rounded.sm}` | 4px | 下拉菜单、弹窗(上限) |
| `{rounded.md}` | 6px | 不使用,记录完整性 |
| `{rounded.lg}` | 8px | 不使用,记录完整性 |
| `{rounded.pill}` | 9999px | 状态圆点(通过/未通过),仅用于直径小于 12px 的圆点 |

直角是政企风格的纪律。任何超过 4px 的圆角都会被改回。

## Components

### 按钮 (Buttons)

| 变体 | 底色 | 文字 | 边框 | 用途 |
|---|---|---|---|---|
| 主按钮 | `{colors.primary}` | 白 | 1px 红 | 首要操作:"立即报名"、"提交" |
| 主按钮 hover | `{colors.primary-hover}` | 白 | — | 悬停 |
| 主按钮 pressed | `{colors.primary-pressed}` | 白 | — | 按下 |
| 描边按钮 | 白 | `{colors.primary}` | 1px 红 | 次要操作:"查看公告" |
| 描边按钮 hover | `{colors.primary}` | 白 | — | 悬停反色填充 |
| 朴素按钮 | 白 | `{colors.ink-muted}` | 无 | 三级操作:"取消" |
| 禁用按钮 | `{colors.surface-2}` | `{colors.ink-subtle}` | — | 不可用 |

按钮统一:0px 圆角、16px 黑体、字距 1px、32px 横向内边距。按钮文案用动词短语("进入报名""查看详情"),不用问句。

### 导航 (Top Rail & Nav)

- **顶部通栏**:深藏青 `{colors.navy}` 底、36px 高,内容:左侧站点名称(如"国家考试信息网"),右侧日期与站内链接,12px 浅色文字。
- **主导航**:白色底 64px 高,左侧 Logo(单位名 + 徽标),右侧导航项 14px 黑体字距 2px。**当前页**为红色底白字(`{nav-link-active}`),悬停为浅灰底红字。导航项之间不用分隔符,靠间距区分。
- 导航项数量 5–8 个,超出即折叠到"更多"。

### 卡片 (Cards)

- 白底、1px `{colors.hairline}` 边框、24px 内边距、0px 圆角。
- 悬停:浅灰底 + 红色 1px 边框(`{card-hover}`),无位移无投影。
- 统计卡:白底 + **顶部 3px 政务红边线**(`{stat-card}`),大号黑体数字(如通过率 98.2%)+ 宋体说明。

### 表单 (Forms)

- 输入框:白底、1px 灰边、0px 圆角、10/12px 内边距,16px 宋体。
- 聚焦:1px 红色边框 + 2px 红色外光晕(仅焦点态允许极淡光晕 `0 0 0 3px rgba(199, 0, 11, 0.1)`)。
- 错误:1px `{colors.semantic-error}` 边框 + 红色错误文案(12px 黑体)。
- 校验提示为系统化表述:"身份证号格式不正确",不使用俏皮话。
- 必填项以红色 * 标记,置于标签左侧。

### 表格 (Tables)

- 表头:浅灰底 `{colors.surface-1}`、14px 黑体 600 字重、12/16px 内边距。
- 表体:白底、14px 宋体、行间 1px `{colors.hairline}` 分隔线,行高 40px。
- 悬停行:浅灰底;不使用斑马纹。
- 表格承载考试安排、成绩、报名名单等核心数据,信息密集、对齐严格(数字右对齐,中文左对齐)。

### 标签与状态 (Tags & Status)

- 标签:1px 灰边、浅灰底、12px 黑体、0px 圆角、2/8px 内边距。
- 状态用语义色圆点(直径 8px)+ 文字:"进行中"、"已结束"、"审核中"。
- 红色标签(`{tag-primary}`)仅用于"最新""重要"等极少数最高优先级标记。

### 页脚 (Footer)

- 深藏青底(`{colors.inverse-canvas}`)、浅灰蓝文字 12px。
- 内容:单位名称、联系方式、ICP 备案号、公安备案号、版权年份,分行居中排布。
- 顶部可有链接分组列,底部一行法定信息。

## Do's and Don'ts

### Do's

- **红色只用来强调**:链接、主按钮、当前导航、标题竖线、焦点。除此之外保持黑白灰。
- **直角、细线、扁平**:圆角 ≤ 4px,边框 1px,阴影几乎不用。
- **黑体标题 + 宋体正文**:严格遵守字族分工。
- **对称与对齐**:页面上下左右边界严格对齐,模块成组排列。
- **信息完整清晰**:日期、文号、状态等元信息要完整,如"报名截止:2026-08-31 17:00"。
- **中文优先**:所有界面文案用规范简体中文,术语准确(如"准考证""考场"而非"ticket""hall")。
- **官方语气**:文案庄重、准确、无网络流行语,如"请于规定时间内完成缴费"。
- **尊重法定信息**:页脚必须包含备案号等,不得省略。

### Don'ts

- **不用霓虹色与多彩渐变**:紫、粉、青绿等消费级配色不出现在界面中。
- **不用大圆角与重投影**:超过 4px 的圆角、超过 2px 的阴影都是违规。
- **不用 emoji 或卡通插画做图标**:用线性图标,描边 1.5px,颜色 `{colors.ink-muted}`。
- **不用手写体/书法体/圆体**:系统只有黑体与宋体。
- **不写全大写英文标题**:中文界面无 small-caps 位置。
- **不用非对称的漂浮布局**:卡片左对齐、居中铺满,不出现杂志式错落。
- **不用斑马纹表格**:政企表格用细线分隔 + 悬停高亮。
- **不让红色大面积铺满页面**:红色用于横幅与强调,正文区域必须留白为底。

## Responsive Behavior

- **断点**:桌面 ≥1200px(12 列) · 平板 768–1199px(8 列) · 移动 <768px(4 列)。
- **导航**:平板端保留横向滚动或压缩字距;移动端折叠为"≡ 菜单",展开后垂直列表,当前项仍为红色底。
- **横幅**:文字随断点缩至 28px 标题 + 16px 正文,内边距降至 32px,仍全宽。
- **表格**:移动端允许横向滚动容器(外层 `overflow-x: auto`),不做卡片化重组——保持数据表形态是政企规范的一部分。
- **卡片网格**:4 → 2 → 1 列。
- **触控目标**:按钮高度不低于 44px(含内边距),链接点击区不小于 24px。
- **顶部通栏**:移动端隐藏,内容并入主导航。

## Agent Prompt Guide

### 快速配色参考

| 用途 | 色值 |
|---|---|
| 主色(强调) | #C7000B |
| 主色悬停 | #A60008 |
| 深藏青(通栏/页脚) | #1F4E79 |
| 金色(仪式装饰) | #C9A063 |
| 画布 | #FFFFFF |
| 浅灰表面 | #F5F5F5 |
| 主文字 | #1A1A1A |
| 次级文字 | #555555 |
| 分隔线 | #E3E3E3 |
| 成功 / 警告 / 错误 | #2E8B57 / #C77400 / #D93025 |

### 字体速记

- 标题与按钮:`Noto Sans SC` 700 加粗,字距 0.5–2px。
- 正文:`Noto Serif SC` 400,行高 1.6–1.75。
- 禁用 emoji、书法体、圆体、全大写英文。

### 现成提示词

> 按 `DESIGN.md` 的政企风格实现这个页面:政务红 #C7000B 作为唯一强调色,0–4px 圆角,1px 细线边框,标题用思源黑体 700、正文用思源宋体 400,1200px 版心居中,区块标题带左侧红色竖线,不要渐变(横幅除外)、不要投影、不要 emoji。

> 按 `DESIGN.md` 检查这个页面的视觉一致性:颜色是否只使用了 palette 中的值?圆角是否 ≤4px?字族分工是否黑体标题/宋体正文?页脚是否含备案信息?

### 一致性自查清单(实现或修改 UI 后逐条核对)

- [ ] 所有颜色来自 front matter palette,未引入 palette 外的色值
- [ ] 圆角 ≤ 4px(状态圆点除外)
- [ ] 标题黑体 700 / 正文宋体 400
- [ ] 区块标题 = 4px 红竖线 + 黑体标题
- [ ] 主操作按钮为红色实心直角
- [ ] 表格有表头浅灰底与细线分隔,无斑马纹
- [ ] 页面无 emoji、无大面积渐变、无重阴影
- [ ] 页脚含备案号等法定信息
- [ ] 版心 1200px 居中,间距为 4px 倍数

## Iteration Guide

1. **新增组件前**:先检查 front matter 中是否已有可复用 token;新组件必须由既有 token 组合而成。
2. **改色**:只允许调整 `colors` 中现有 token 的取值,不允许新增色值。确需新增时,先更新本文件,再改代码。
3. **改排版**:调整 `typography` token,不在组件里写死 font-size / font-weight。
4. **跑自查清单**:每次 UI 改动后对照上面的清单核对,未通过不提交。

## Known Gaps

- **深色模式**:政企风格以浅色为唯一形态,暂不定义 dark 模式;如需夜间阅读,仅允许将画布换为 `{colors.inverse-canvas}` 系深藏青,红色与金色保持不变。
- **插画资产**:暂无定制插画,横幅与空状态使用线性图标 + 徽章,不生成插画。
- **无障碍对比度**:红色底白字(hover 态 #A60008 上白字)对比度 ≈ 4.6:1,仅用于 ≥14px 文字;12px 以下文字不得使用红底白字。
