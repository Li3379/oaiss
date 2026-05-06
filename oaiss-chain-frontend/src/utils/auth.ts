const TOKEN_KEY = 'access_token'
const REFRESH_TOKEN_KEY = 'refresh_token'

let memoryAccessToken: string | null = null

export function getAccessToken(): string | null {
  if (memoryAccessToken && !isTokenExpired(memoryAccessToken)) return memoryAccessToken
  const session = sessionStorage.getItem(TOKEN_KEY)
  if (session && !isTokenExpired(session)) {
    memoryAccessToken = session
    return memoryAccessToken
  }
  return null
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_KEY)
}

export function setTokens(access: string, refresh?: string): void {
  memoryAccessToken = access
  sessionStorage.setItem(TOKEN_KEY, access)
  if (refresh) {
    localStorage.setItem(REFRESH_TOKEN_KEY, refresh)
  }
}

export function clearTokens(): void {
  memoryAccessToken = null
  sessionStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
}

export interface JwtPayload {
  exp?: number
  sub?: string
  userId?: number
  userType?: number
  [key: string]: unknown
}

export function parseJwtPayload(token: string): JwtPayload | null {
  if (!token) return null
  try {
    const parts = token.split('.')
    if (parts.length !== 3) return null
    const header = JSON.parse(atob(parts[0]))
    if (header.alg === 'none') return null
    return JSON.parse(atob(parts[1]))
  } catch {
    return null
  }
}

export function isTokenExpired(token: string): boolean {
  const payload = parseJwtPayload(token)
  if (!payload || !payload.exp) return true
  return payload.exp * 1000 < Date.now()
}
