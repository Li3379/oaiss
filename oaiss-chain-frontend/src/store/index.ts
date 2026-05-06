import { createPinia, defineStore } from 'pinia'
import { ROLE, ROLE_HOME, ROLE_LABEL, type RoleType } from '../config/menu'
import { getAccessToken, setTokens, clearTokens, isTokenExpired, parseJwtPayload } from '../utils/auth'

export const pinia = createPinia()

interface UserState {
  loggedIn: boolean
  role: RoleType | null
  username: string
  userId: number | null
  enterpriseId: number | null
}

function extractUserFromToken(token: string): UserState | null {
  const payload = parseJwtPayload(token)
  if (!payload) return null

  const roles = (payload.roles as string[]) || []
  const role = Array.isArray(roles) && roles.length > 0 ? (roles[0] as RoleType) : null
  const validRole = role && Object.values(ROLE).includes(role)

  return {
    loggedIn: true,
    role: validRole ? role : null,
    username: (payload.sub as string) || (payload.username as string) || '',
    userId: (payload.userId as number) || null,
    enterpriseId: (payload.enterpriseId as number) || null,
  }
}

function resolveInitialState(): UserState {
  const token = getAccessToken()
  if (!token || isTokenExpired(token)) {
    clearTokens()
    return { loggedIn: false, role: null, username: '', userId: null, enterpriseId: null }
  }

  const user = extractUserFromToken(token)
  if (!user) {
    clearTokens()
    return { loggedIn: false, role: null, username: '', userId: null, enterpriseId: null }
  }

  return user
}

export const useAppStore = defineStore('app', {
  state: () => ({
    sidebarCollapsed: false,
    systemTitle: 'layout.title',
    ...resolveInitialState(),
  }),
  getters: {
    roleLabel: (state) => (state.role ? ROLE_LABEL[state.role] : 'layout.notLoggedIn'),
    homePath: (state) => (state.role ? ROLE_HOME[state.role] : '/enterprise/carbon/upload'),
  },
  actions: {
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
    },
    login({ accessToken, refreshToken }: { accessToken: string; refreshToken?: string }) {
      setTokens(accessToken, refreshToken)
      const user = extractUserFromToken(accessToken)
      if (user) {
        this.loggedIn = user.loggedIn
        this.role = user.role
        this.username = user.username
        this.userId = user.userId
        this.enterpriseId = user.enterpriseId
      } else {
        this.loggedIn = false
        this.role = null
        this.username = ''
      }
    },
    logout() {
      clearTokens()
      this.loggedIn = false
      this.role = null
      this.username = ''
      this.userId = null
      this.enterpriseId = null
      this.sidebarCollapsed = false
    },
  },
})
