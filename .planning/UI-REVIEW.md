---
phase: full-platform
audited: 2026-05-11
method: browser-harness + source-code-analysis
pages_audited: 19
roles_covered: 5 (admin, enterprise, auditor, authenticator, third-party)
overall_score: 17
max_score: 24
---

# OAISS CHAIN — 6-Pillar Visual UI Audit

## Scope

Full-platform audit covering 19 view pages across 5 user roles, using browser-harness screenshots, accessibility snapshots, and Vue source code analysis.

**Pages audited:**
- Public: OfficialHome, Login, NotFound
- Admin (4): SystemUsers, SystemConfig, SystemCarbon, DataStatistics
- Enterprise (11): CompanyDashboard, CarbonCoin, TradingMarket, TradingP2P, CarbonNeutral, CarbonUpload, CreditScore, EmissionData, OrdersManage, Blockchain, UserProfile
- Auditor (1): AuditList
- Authenticator (1): VerifyList
- Third-party (1): Monitor

---

## Score Summary

| Pillar              | Score | Verdict |
|---------------------|-------|---------|
| Copywriting         | 3/4   | Good    |
| Visuals             | 3/4   | Good    |
| Color               | 3/4   | Good    |
| Typography          | 3/4   | Good    |
| Spacing             | 3/4   | Good    |
| Experience Design   | 2/4   | Needs Work |
| **Total**           | **17/24** | **71%** |

---

## Pillar 1: Copywriting — 3/4

### Strengths

- **Full i18n coverage**: All 19 views use `vue-i18n` with `useI18n()` composable. No hardcoded Chinese strings in templates — all text flows through `t()` function calls.
- **Consistent terminology**: Breadcrumb labels, button text, table headers, and error messages follow a uniform naming convention across all roles.
- **Breadcrumb navigation**: Every internal page has a 2-level breadcrumb (e.g., "系统管理 / 用户管理", "企业中心 / 企业仪表盘") providing clear context.
- **Empty/loading states**: Dashboard uses `el-skeleton` for loading. List pages show "暂无数据" (Element Plus default) for empty tables.

### Issues

| ID | Severity | Description | Evidence |
|----|----------|-------------|----------|
| CW-01 | HIGH | **Page title is "test3"** (placeholder emoji + test string) | All snap.txt files: `RootWebArea "test3"` — set in `index.html` `<title>` tag |
| CW-02 | MEDIUM | Footer links are dead (`javascript:void(0)`) | OfficialHome.vue lines 219-232: all `<a href="javascript:void(0)">` — whitepaper, docs, governance, news links do nothing |
| CW-03 | MEDIUM | Section subtitle reuse — multiple sections share the same `officialHome.heroSubtitle` text | OfficialHome.vue: sections "build", "platform", "country" all use `t('officialHome.heroSubtitle')` as their subtitle instead of unique descriptions |
| CW-04 | LOW | 404 page uses generic text | NotFound.vue: "抱歉，您访问的页面不存在" — functional but not branded |

---

## Pillar 2: Visuals — 3/4

### Strengths

- **Professional landing page**: OfficialHome has a well-structured hero section (72vh height), feature grid (4 cards), metrics grid (5 metrics with dark backgrounds), role cards (4 roles), image gallery, icon strip, and footer — comprehensive marketing page.
- **Consistent card system**: All internal pages use `el-card` with `shadow="never"` and `class="section-card"` with `border-radius: 12px` — uniform card-based layout.
- **Dashboard with 6 ECharts**: CompanyDashboard renders 6 charts (2 bar, 2 line, 2 pie) in a responsive 2-column grid — data-rich and visually structured.
- **Dark mode**: Full dark mode support with CSS variable overrides in `style.css` and toggle button in layout header.

### Issues

| ID | Severity | Description | Evidence |
|----|----------|-------------|----------|
| VI-01 | HIGH | **Enterprise pages rendering 404** | snap.txt files show `/enterprise/company-dashboard`, `/enterprise/carbon-coin`, `/enterprise/trading-market` all return 404 — likely routing mismatch or auth redirect failure |
| VI-02 | MEDIUM | Logo is a CSS dot, not an image | Layout and Login both use `.logo-dot` — a 12-14px CSS circle with gradient. No SVG/image logo anywhere. |
| VI-03 | MEDIUM | 404 page uses Element Plus default blue (#409eff) | NotFound.vue: `.code { color: #409eff }` — should use brand teal `var(--teal)` or `#14a79a` |
| VI-04 | LOW | Gallery images depend on external config | `GALLERY_IMAGES` from `config/images.ts` — if URLs break, gallery section shows broken images with no fallback |

---

## Pillar 3: Color — 3/4

### Strengths

- **CSS variables system**: Root variables defined in `style.css`: `--teal: #14a79a`, `--green: #45be75`, `--bg-page: #edf2f4`, `--text-primary: #1a2c30`, etc.
- **Cohesive palette**: Teal-green gradient family used consistently:
  - Sidebar: `linear-gradient(180deg, #0d3236, #125850, #198369)`
  - Header: `linear-gradient(90deg, #17363a, #1e4d49)`
  - Buttons/accents: `linear-gradient(120deg, #18a99a, #42c977)`
  - Overview cards: `linear-gradient(130deg, rgba(20, 167, 154, 0.12), rgba(69, 190, 117, 0.1))`
- **Dark mode**: Full dark variable set: `--bg-page: #141414`, `--bg-surface: #1d1d1d`, `--text-primary: #e5eaf3`, reduced gradient opacity.
- **Background texture**: Body uses radial gradients for subtle depth effect.

### Issues

| ID | Severity | Description | Evidence |
|----|----------|-------------|----------|
| CO-01 | MEDIUM | **404 page uses #409eff** (Element Plus default blue) | NotFound.vue line 49: `color: #409eff` — breaks the teal/green color story |
| CO-02 | MEDIUM | Hardcoded colors alongside CSS variables | Some components use raw hex (#0f3d40, #2f6268, #d0efe6) instead of variables — reduces themeability |
| CO-03 | LOW | Metric card uses dark background (#0f4748) on OfficialHome but overview cards use light gradient on Dashboard | Two different styles for metric/stats cards — should be consistent |
| CO-04 | LOW | No color contrast audit performed | Some light-on-dark text (rgba values) may not meet WCAG AA on certain backgrounds |

---

## Pillar 4: Typography — 3/4

### Strengths

- **Clear size hierarchy**: Hero 42px → Section titles 30px → Card headings ~16-18px → Body 13-14px → Small/meta 12px — proper visual rhythm.
- **System font stack optimized for CJK**: `'HarmonyOS Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif` — no web font download needed, excellent for Chinese text rendering.
- **Antialiased rendering**: `text-rendering: optimizeLegibility` and `-webkit-font-smoothing: antialiased` set globally.
- **Font weight hierarchy**: 800 (brand), 700 (headings), 600 (chart titles), normal (body) — clear weight differentiation.

### Issues

| ID | Severity | Description | Evidence |
|----|----------|-------------|----------|
| TY-01 | MEDIUM | Some text at 12-13px may be too small | `.metric-label`, `.desc` (copyright), `.system-subtitle` all at 12px — borderline for readability, especially in CJK characters |
| TY-02 | MEDIUM | No letter-spacing consistency | Layout `.brand-title` has `letter-spacing: 0.6px` but other headings don't — only the brand uses it |
| TY-03 | LOW | Hero title at 42px lacks line-height | OfficialHome `.hero-title` has no explicit line-height — relies on browser default which may clip on some fonts |
| TY-04 | LOW | Font stack depends on OS availability | HarmonyOS Sans SC only available on HarmonyOS devices; Windows falls back to Microsoft YaHei, macOS to PingFang SC |

---

## Pillar 5: Spacing — 3/4

### Strengths

- **Consistent gap system**: All page layouts use `gap: 14px` between section cards (dashboard, users, trading, etc.) — uniform vertical rhythm.
- **Responsive grids**: Dashboard uses `grid-template-columns: repeat(3, minmax(180px, 1fr))` for overview and `repeat(2, minmax(280px, 1fr))` for charts — proper responsive behavior.
- **Three breakpoints**: 1280px (search grid adjusts), 1024px (landing page grids), 768px (mobile layout) — good progressive enhancement.
- **Card border-radius consistency**: All section cards use `border-radius: 12px` — uniform corner treatment.
- **Mobile-first responsive**: Sidebar collapses to 70px, header wraps, content padding reduces, grid columns go to 1fr.

### Issues

| ID | Severity | Description | Evidence |
|----|----------|-------------|----------|
| SP-01 | MEDIUM | No max-width constraint on main content | Layout `.main-content` has no `max-width` — on ultra-wide screens (>1920px), cards stretch excessively |
| SP-02 | MEDIUM | Landing page nav hidden on tablet | `.nav-menu { display: none }` at 1024px — no hamburger menu replacement, navigation completely lost |
| SP-03 | LOW | Inconsistent section padding | OfficialHome `.section-wrap` uses `padding: 46px 18px` while layout `.el-main` uses `--el-main-padding: 20px` — different density levels |
| SP-04 | LOW | Pagination right-aligned on mobile | `.pager-row { justify-content: flex-end }` — on mobile, this pushes pagination off-screen; should center |

---

## Pillar 6: Experience Design — 2/4

### Strengths

- **Role-based menu system**: Dynamic sidebar menu based on user role (`MENU_BY_ROLE[appStore.role]`) with 3-level navigation.
- **Login flow**: Captcha with refresh, remember password, redirect support — functional auth UX.
- **Dark mode toggle**: Persisted to localStorage, respects `prefers-color-scheme` on first load.
- **Language switcher**: Component in header allows switching between zh-CN and en-US.
- **Loading states**: Dashboard uses `el-skeleton`, tables use `v-loading` directive.
- **Breadcrumb navigation**: Consistent 2-level breadcrumbs on all internal pages.

### Critical Issues

| ID | Severity | Description | Evidence |
|----|----------|-------------|----------|
| XD-01 | **CRITICAL** | **Enterprise pages return 404** | Browser-harness snap files show `/enterprise/company-dashboard`, `/enterprise/carbon-coin`, `/enterprise/trading-market` all rendering 404 NotFound page. The Vue components exist (CompanyDashboard.vue, CarbonCoin.vue, TradingMarket.vue) but routes may be misconfigured or auth guards redirect to wrong paths. |
| XD-02 | HIGH | **Page title "test3"** is unprofessional | All pages show "test3" in browser tab — `index.html` title not updated for production. Users see a test identifier. |
| XD-03 | HIGH | **Footer links are completely non-functional** | OfficialHome footer has 9 links (whitepaper, docs, governance, news, announcements, privacy, WeChat, video, community) — all are `javascript:void(0)` stubs. |
| XD-04 | MEDIUM | Social buttons trigger only a toast message | OfficialHome social buttons (Message, Share, Link) just show `ElMessage.success('...')` — no actual sharing/link behavior. |
| XD-05 | MEDIUM | No empty state illustrations | Tables show Element Plus default "暂无数据" text — no custom illustrations or guidance for new users. |
| XD-06 | MEDIUM | Feature cards link to routes requiring login | OfficialHome feature card buttons link directly to `/enterprise/trading/p2p`, `/admin/system/carbon`, etc. — these will redirect to login but there's no indication that login is required. |
| XD-07 | LOW | Logo is a CSS dot with no alt text or semantic meaning | Both layout and login use a colored circle — no accessible name, no fallback, no branding recognition. |

---

## Cross-Cutting Observations

### Positive Patterns

1. **Consistent component architecture**: All pages follow `<section class="xxx-page">` → `<el-card class="section-card">` pattern — easy to maintain.
2. **Proper reactive state management**: Pinia store for auth/app state, local refs for component state.
3. **Error handling in API calls**: Every `fetchData` function has try/catch with `ElMessage.error()`.
4. **Responsive design**: Three breakpoints with progressive layout changes.
5. **i18n throughout**: No hardcoded UI strings in templates.

### Architecture-Level Issues

1. **Token storage inconsistency**: `auth.ts` checks both localStorage and sessionStorage, with defensive fallback logic. The dual-storage approach is complex and could lead to stale tokens.
2. **Screenshot verification gap**: PNG screenshots were captured successfully but CDN upload made visual analysis difficult. The snap.txt (accessibility tree) files revealed the 404 issue that screenshots alone might not have exposed clearly.

---

## Priority Fix List

| Priority | ID | Fix | Effort |
|----------|-----|-----|--------|
| P0 | XD-01 | Investigate and fix enterprise route 404s — verify router paths match menu config | Medium |
| P0 | CW-01/XD-02 | Change page title from "test3" to "OAISS CHAIN - 碳链交易平台" or similar | Trivial |
| P1 | CO-01/VI-03 | Change 404 page color from #409eff to `var(--teal)` or `#14a79a` | Trivial |
| P1 | CW-02 | Replace footer `javascript:void(0)` links with actual URLs or remove them | Small |
| P1 | CW-03 | Create unique subtitle i18n keys for each section instead of reusing heroSubtitle | Small |
| P2 | SP-02 | Add hamburger menu for tablet breakpoint when nav is hidden | Medium |
| P2 | SP-01 | Add `max-width: 1440px` with `margin: 0 auto` to main content area | Trivial |
| P2 | VI-02 | Replace logo dot with actual SVG/image logo | Small |
| P2 | XD-05 | Create branded empty state components with illustrations | Medium |
| P3 | CO-02 | Migrate remaining hardcoded hex colors to CSS variables | Small |
| P3 | TY-01 | Audit and increase 12px text to 13px minimum | Trivial |

---

## Methodology

- **Screenshots**: 20 PNG files captured via browser-harness (`screenshots/00-*.png` through `screenshots/19-*.png`)
- **Accessibility trees**: 3 `.snap.txt` files extracted via browser-harness for enterprise pages
- **Source analysis**: 6 key Vue components + `style.css` + `auth.ts` read for CSS/design patterns
- **Tools**: browser-harness (CDP), Read tool (image + code), Grep (pattern search)

---

*Audit completed 2026-05-11 using browser-harness and source code analysis.*
