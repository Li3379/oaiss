<script setup lang="ts">
import {
  Connection,
  DataAnalysis,
  EditPen,
  Link,
  Lock,
  Message,
  Monitor,
  Notification,
  OfficeBuilding,
  Operation,
  SetUp,
  Share,
  User,
  CircleCheck,
} from '@element-plus/icons-vue'
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { HERO_BG, INTRO_BG, GALLERY_IMAGES } from '../config/images'

const { t } = useI18n()

const router = useRouter()

const mobileMenuOpen = ref(false)

const navItems = computed(() => [
  { key: 'hero', label: 'OAISS' },
  { key: 'build', label: t('officialHome.navBuild') },
  { key: 'join', label: t('officialHome.navParticipate') },
  { key: 'research', label: t('officialHome.navResearch') },
  { key: 'platform', label: t('officialHome.navPlatform') },
  { key: 'country', label: t('officialHome.navCountry') },
  { key: 'about', label: t('officialHome.navAbout') },
])

const featureCards = computed(() => [
  {
    title: t('officialHome.featureP2P'),
    description: t('officialHome.featureP2PDesc'),
    icon: Connection,
    route: '/enterprise/trading/p2p',
  },
  {
    title: t('officialHome.featureBlockchain'),
    description: t('officialHome.featureBlockchainDesc'),
    icon: Link,
    route: '/enterprise/trading/market',
  },
  {
    title: t('officialHome.featureAI'),
    description: t('officialHome.featureAIDesc'),
    icon: DataAnalysis,
    route: '/enterprise/company/dashboard',
  },
  {
    title: t('officialHome.featureSignature'),
    description: t('officialHome.featureSignatureDesc'),
    icon: EditPen,
    route: '/admin/system/carbon',
  },
])

const roleCards = computed(() => [
  {
    title: t('officialHome.roleEnterprise'),
    description: t('officialHome.roleEnterpriseDesc'),
    icon: OfficeBuilding,
    route: '/enterprise/carbon/upload',
  },
  {
    title: t('officialHome.roleAdmin'),
    description: t('officialHome.roleAdminDesc'),
    icon: SetUp,
    route: '/admin/system/users',
  },
  {
    title: t('officialHome.roleAuditor'),
    description: t('officialHome.roleAuditorDesc'),
    icon: CircleCheck,
    route: '/auditor/audit/list',
  },
  {
    title: t('officialHome.roleThirdParty'),
    description: t('officialHome.roleThirdPartyDesc'),
    icon: Monitor,
    route: '/admin/data/statistics',
  },
])

const metricItems = computed(() => [
  { label: t('officialHome.metricDistributed'), value: '99.96%' },
  { label: t('officialHome.metricCentralized'), value: 'A+' },
  { label: t('officialHome.metricNodes'), value: '3,248' },
  { label: t('officialHome.metricLatency'), value: '11ms' },
  { label: t('officialHome.metricMaxLatency'), value: '49ms' },
])

const galleryImages = GALLERY_IMAGES

const bottomIcons = [Operation, DataAnalysis, Connection, Link, Lock, Notification]

const onNavClick = (key) => {
  const el = document.getElementById(key)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
  mobileMenuOpen.value = false
}

const jumpToRoute = (path) => {
  router.push(path)
}

const onSocialClick = () => {
  ElMessage.success(t('officialHome.socialTriggered'))
}
</script>

<template>
  <div class="site-page">
    <header class="top-nav">
      <div class="brand" @click="onNavClick('hero')">OAISS</div>
      <nav class="nav-menu">
        <button
          v-for="item in navItems"
          :key="item.key"
          class="nav-item"
          type="button"
          @click="onNavClick(item.key)"
        >
          {{ item.label }}
        </button>
      </nav>
      <button class="hamburger-btn" type="button" @click="mobileMenuOpen = !mobileMenuOpen">
        <span class="hamburger-line" />
        <span class="hamburger-line" />
        <span class="hamburger-line" />
      </button>
      <div v-if="mobileMenuOpen" class="mobile-menu-overlay" @click="mobileMenuOpen = false">
        <div class="mobile-menu-panel" @click.stop>
          <button
            v-for="item in navItems"
            :key="item.key"
            class="mobile-menu-item"
            type="button"
            @click="onNavClick(item.key)"
          >
            {{ item.label }}
          </button>
        </div>
      </div>
      <div class="social-links">
        <button type="button" class="social-btn" @click="onSocialClick"><el-icon><Message /></el-icon></button>
        <button type="button" class="social-btn" @click="onSocialClick"><el-icon><Share /></el-icon></button>
        <button type="button" class="social-btn" @click="onSocialClick"><el-icon><Link /></el-icon></button>
      </div>
    </header>

    <section id="hero" class="hero" :style="{ backgroundImage: `linear-gradient(rgba(7, 40, 45, 0.55), rgba(7, 40, 45, 0.62)), url(${HERO_BG})` }">
      <div class="hero-overlay">
        <div class="hero-logo">OAISS</div>
        <h1 class="hero-title">{{ t('officialHome.heroTitle') }}</h1>
        <p class="hero-description">
          {{ t('officialHome.heroSubtitle') }}
        </p>
      </div>
    </section>

    <section id="build" class="section-wrap">
      <div class="section-title-wrap">
        <h2 class="section-title">{{ t('officialHome.sectionCoreFeatures') }}</h2>
        <p class="section-subtitle">{{ t('officialHome.sectionCoreFeaturesSub') }}</p>
      </div>
      <div class="feature-grid">
        <article v-for="item in featureCards" :key="item.title" class="feature-card">
          <div class="feature-icon"><el-icon><component :is="item.icon" /></el-icon></div>
          <h3>{{ item.title }}</h3>
          <p>{{ item.description }}</p>
          <el-button type="primary" plain @click="jumpToRoute(item.route)">{{ t('officialHome.btnEnter') }}</el-button>
        </article>
      </div>
    </section>

    <section id="research" class="section-wrap intro-section">
      <div class="intro-left">
        <h2 class="section-title">{{ t('officialHome.sectionSystemIntro') }}</h2>
        <p class="intro-text">
          {{ t('officialHome.sectionSystemIntroSub') }}
        </p>
        <div class="metrics-grid">
          <div v-for="item in metricItems" :key="item.label" class="metric-card">
            <div class="metric-value">{{ item.value }}</div>
            <div class="metric-label">{{ item.label }}</div>
          </div>
        </div>
      </div>
      <div class="intro-image" :style="{ backgroundImage: `url(${INTRO_BG})` }" />
    </section>

    <section id="platform" class="section-wrap">
      <div class="section-title-wrap">
        <h2 class="section-title">{{ t('officialHome.sectionUserRoles') }}</h2>
        <p class="section-subtitle">{{ t('officialHome.sectionUserRolesSub') }}</p>
      </div>
      <div class="roles-grid">
        <article v-for="role in roleCards" :key="role.title" class="role-card">
          <div class="role-icon"><el-icon><component :is="role.icon" /></el-icon></div>
          <h3>{{ role.title }}</h3>
          <p>{{ role.description }}</p>
          <el-button text type="primary" @click="jumpToRoute(role.route)">{{ t('officialHome.btnAccessRole') }}</el-button>
        </article>
      </div>
    </section>

    <section id="country" class="section-wrap">
      <div class="section-title-wrap">
        <h2 class="section-title">{{ t('officialHome.sectionEcosystem') }}</h2>
        <p class="section-subtitle">{{ t('officialHome.sectionEcosystemSub') }}</p>
      </div>
      <div class="gallery-grid">
        <img v-for="url in galleryImages" :key="url" :src="url" :alt="t('officialHome.altEcosystem')" />
      </div>
    </section>

    <section id="join" class="bottom-icon-strip">
      <div v-for="(icon, idx) in bottomIcons" :key="idx" class="strip-icon">
        <el-icon><component :is="icon" /></el-icon>
      </div>
    </section>

    <footer id="about" class="footer">
      <div class="footer-columns">
        <div>
          <h4>{{ t('officialHome.footerMoreLinks') }}</h4>
          <router-link to="/official-home#build">{{ t('officialHome.footerWhitepaper') }}</router-link>
          <router-link to="/official-home#research">{{ t('officialHome.footerDevDocs') }}</router-link>
          <router-link to="/official-home#platform">{{ t('officialHome.footerGovernance') }}</router-link>
        </div>
        <div>
          <h4>{{ t('officialHome.footerInfoLinks') }}</h4>
          <router-link to="/official-home#hero">{{ t('officialHome.footerNews') }}</router-link>
          <router-link to="/official-home#build">{{ t('officialHome.footerAnnouncement') }}</router-link>
          <router-link to="/official-home#about">{{ t('officialHome.footerPrivacy') }}</router-link>
        </div>
        <div>
          <h4>{{ t('officialHome.footerFollowUs') }}</h4>
          <router-link to="/official-home#about">{{ t('officialHome.footerWechat') }}</router-link>
          <router-link to="/official-home#about">{{ t('officialHome.footerVideo') }}</router-link>
          <router-link to="/official-home#about">{{ t('officialHome.footerCommunity') }}</router-link>
        </div>
      </div>
      <div class="copyright">© 2026 OAISS. All rights reserved.</div>
    </footer>
  </div>
</template>

<style scoped>
.site-page {
  min-height: 100vh;
  background: #f0f4f5;
  color: #143639;
}

.top-nav {
  position: sticky;
  top: 0;
  z-index: 100;
  height: 68px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  background: rgba(11, 45, 48, 0.88);
  backdrop-filter: blur(8px);
}

.brand {
  color: #d6fff4;
  font-weight: 800;
  font-size: 20px;
  cursor: pointer;
}

.nav-menu {
  display: flex;
  gap: 16px;
}

.nav-item {
  border: none;
  background: transparent;
  color: #def8f1;
  font-size: 14px;
  cursor: pointer;
  padding: 6px 8px;
}

.nav-item:hover {
  color: #83f2d5;
}

.social-links {
  display: flex;
  gap: 8px;
}

.social-btn {
  border: 1px solid rgba(189, 243, 230, 0.32);
  background: rgba(255, 255, 255, 0.08);
  color: #d7f9f2;
  width: 34px;
  height: 34px;
  border-radius: 8px;
  cursor: pointer;
}

.hamburger-btn {
  display: none;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
  width: 34px;
  height: 34px;
  border: 1px solid rgba(189, 243, 230, 0.32);
  background: rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  cursor: pointer;
  padding: 6px;
}

.hamburger-line {
  display: block;
  width: 100%;
  height: 2px;
  background: #d7f9f2;
  border-radius: 1px;
}

.mobile-menu-overlay {
  position: fixed;
  inset: 0;
  z-index: 200;
  background: rgba(0, 0, 0, 0.45);
}

.mobile-menu-panel {
  position: absolute;
  top: 0;
  right: 0;
  width: 240px;
  height: 100%;
  background: #0d2f33;
  padding: 68px 0 20px;
  display: flex;
  flex-direction: column;
}

.mobile-menu-item {
  display: block;
  width: 100%;
  padding: 14px 24px;
  border: none;
  background: transparent;
  color: #def8f1;
  font-size: 15px;
  text-align: left;
  cursor: pointer;
}

.mobile-menu-item:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #83f2d5;
}

.hero {
  height: 72vh;
  min-height: 520px;
  background-size: cover;
  background-position: center;
}

.hero-overlay {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 0 20px;
}

.hero-logo {
  width: 110px;
  height: 110px;
  border-radius: 50%;
  border: 2px solid rgba(202, 255, 241, 0.75);
  color: #e5fff8;
  display: grid;
  place-items: center;
  font-size: 24px;
  font-weight: 800;
  background: rgba(10, 63, 62, 0.35);
}

.hero-title {
  margin: 24px 0 8px;
  color: #f3fffb;
  font-size: 42px;
}

.hero-description {
  margin: 0;
  color: #d0efe6;
  font-size: 16px;
  max-width: 760px;
}

.section-wrap {
  max-width: 1200px;
  margin: 0 auto;
  padding: 46px 18px;
}

.section-title-wrap {
  margin-bottom: 20px;
}

.section-title {
  margin: 0;
  font-size: 30px;
  color: #0e3940;
}

.section-subtitle {
  margin: 8px 0 0;
  color: #4f7276;
}

.feature-grid,
.roles-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(180px, 1fr));
  gap: 12px;
}

.feature-card,
.role-card {
  background: linear-gradient(160deg, #ffffff, #f2fbf9);
  border: 1px solid #d3e6e0;
  border-radius: 14px;
  padding: 18px;
}

.feature-icon,
.role-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  background: linear-gradient(120deg, #24b39f, #67d083);
  color: #ffffff;
  font-size: 18px;
}

.feature-card h3,
.role-card h3 {
  margin: 14px 0 8px;
}

.feature-card p,
.role-card p {
  margin: 0 0 14px;
  color: #5a767a;
  line-height: 1.7;
  font-size: 13px;
}

.intro-section {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 16px;
}

.intro-left {
  background: linear-gradient(160deg, #ffffff, #f1fbf8);
  border: 1px solid #d2e6df;
  border-radius: 14px;
  padding: 18px;
}

.intro-text {
  color: #557378;
  line-height: 1.9;
  margin: 10px 0 18px;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(90px, 1fr));
  gap: 10px;
}

.metric-card {
  background: #0f4748;
  color: #d6fbf3;
  border-radius: 10px;
  padding: 10px;
  text-align: center;
}

.metric-value {
  font-size: 18px;
  font-weight: 700;
}

.metric-label {
  font-size: 12px;
  margin-top: 4px;
  opacity: 0.86;
}

.intro-image {
  border-radius: 14px;
  min-height: 320px;
  background-size: cover;
  background-position: center;
}

.gallery-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(180px, 1fr));
  gap: 10px;
}

.gallery-grid img {
  width: 100%;
  height: 190px;
  object-fit: cover;
  border-radius: 10px;
}

.bottom-icon-strip {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 18px 24px;
  display: grid;
  grid-template-columns: repeat(6, minmax(80px, 1fr));
  gap: 8px;
}

.strip-icon {
  height: 54px;
  border-radius: 10px;
  border: 1px dashed #8cc4b4;
  color: #1d6e69;
  background: #ecf8f4;
  display: grid;
  place-items: center;
  font-size: 22px;
}

.footer {
  background: #0d2f33;
  color: #d0efea;
  padding: 34px 20px 20px;
}

.footer-columns {
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(3, minmax(180px, 1fr));
  gap: 12px;
}

.footer h4 {
  margin: 0 0 12px;
}

.footer a {
  display: block;
  color: #a8d8cf;
  text-decoration: none;
  margin-bottom: 8px;
  font-size: 13px;
}

.copyright {
  max-width: 1200px;
  margin: 22px auto 0;
  border-top: 1px solid rgba(196, 239, 229, 0.15);
  padding-top: 12px;
  font-size: 12px;
  color: #95c0b8;
}

@media (max-width: 1024px) {
  .nav-menu {
    display: none;
  }

  .hamburger-btn {
    display: flex;
  }

  .feature-grid,
  .roles-grid {
    grid-template-columns: repeat(2, minmax(180px, 1fr));
  }

  .intro-section {
    grid-template-columns: 1fr;
  }

  .metrics-grid {
    grid-template-columns: repeat(3, minmax(90px, 1fr));
  }
}

@media (max-width: 768px) {
  .hero-title {
    font-size: 30px;
  }

  .feature-grid,
  .roles-grid,
  .gallery-grid,
  .footer-columns {
    grid-template-columns: 1fr;
  }

  .bottom-icon-strip {
    grid-template-columns: repeat(3, minmax(70px, 1fr));
  }

  .metrics-grid {
    grid-template-columns: repeat(2, minmax(90px, 1fr));
  }
}
</style>
