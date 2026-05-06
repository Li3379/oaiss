import { describe, it, expect, beforeEach } from 'vitest'
import {
  getAccessToken,
  getRefreshToken,
  setTokens,
  clearTokens,
  parseJwtPayload,
  isTokenExpired,
} from '../auth'

describe('auth utils', () => {
  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
    clearTokens()
  })

  describe('setTokens / getAccessToken / getRefreshToken', () => {
    it('stores and retrieves valid access token', () => {
      const header = btoa(JSON.stringify({ alg: 'HS256' }))
      const payload = btoa(JSON.stringify({ sub: '123', exp: 9999999999 }))
      const token = `${header}.${payload}.signature`
      setTokens(token, 'test-refresh')
      expect(getAccessToken()).toBe(token)
      expect(getRefreshToken()).toBe('test-refresh')
    })

    it('stores access token in sessionStorage', () => {
      const header = btoa(JSON.stringify({ alg: 'HS256' }))
      const payload = btoa(JSON.stringify({ sub: '123', exp: 9999999999 }))
      const token = `${header}.${payload}.signature`
      setTokens(token, null)
      expect(sessionStorage.getItem('access_token')).toBe(token)
    })

    it('stores refresh token in localStorage', () => {
      const header = btoa(JSON.stringify({ alg: 'HS256' }))
      const payload = btoa(JSON.stringify({ sub: '123', exp: 9999999999 }))
      const token = `${header}.${payload}.signature`
      setTokens(token, 'test-refresh')
      expect(localStorage.getItem('refresh_token')).toBe('test-refresh')
    })

    it('returns null for expired token', () => {
      const header = btoa(JSON.stringify({ alg: 'HS256' }))
      const payload = btoa(JSON.stringify({ exp: 1 }))
      const token = `${header}.${payload}.signature`
      setTokens(token, 'refresh')
      expect(getAccessToken()).toBeNull()
    })
  })

  describe('clearTokens', () => {
    it('removes all tokens', () => {
      const header = btoa(JSON.stringify({ alg: 'HS256' }))
      const payload = btoa(JSON.stringify({ sub: '123', exp: 9999999999 }))
      const token = `${header}.${payload}.signature`
      setTokens(token, 'refresh')
      clearTokens()
      expect(getAccessToken()).toBeNull()
      expect(getRefreshToken()).toBeNull()
    })
  })

  describe('parseJwtPayload', () => {
    it('parses valid JWT', () => {
      const header = btoa(JSON.stringify({ alg: 'HS256' }))
      const payload = btoa(JSON.stringify({ sub: '123', exp: 9999999999 }))
      const token = `${header}.${payload}.signature`
      const result = parseJwtPayload(token)
      expect(result.sub).toBe('123')
      expect(result.exp).toBe(9999999999)
    })

    it('returns null for empty input', () => {
      expect(parseJwtPayload(null)).toBeNull()
      expect(parseJwtPayload('')).toBeNull()
    })

    it('returns null for malformed token', () => {
      expect(parseJwtPayload('not-a-jwt')).toBeNull()
    })

    it('returns null for token with wrong number of parts', () => {
      expect(parseJwtPayload('a.b')).toBeNull()
      expect(parseJwtPayload('a.b.c.d')).toBeNull()
    })

    it('rejects alg:none tokens', () => {
      const header = btoa(JSON.stringify({ alg: 'none' }))
      const payload = btoa(JSON.stringify({ sub: '123' }))
      const token = `${header}.${payload}.signature`
      expect(parseJwtPayload(token)).toBeNull()
    })
  })

  describe('isTokenExpired', () => {
    it('returns true for expired token', () => {
      const header = btoa('{}')
      const payload = btoa(JSON.stringify({ exp: 1 }))
      const token = `${header}.${payload}.sig`
      expect(isTokenExpired(token)).toBe(true)
    })

    it('returns false for future token', () => {
      const header = btoa('{}')
      const payload = btoa(JSON.stringify({ exp: 9999999999 }))
      const token = `${header}.${payload}.sig`
      expect(isTokenExpired(token)).toBe(false)
    })

    it('returns true for null token', () => {
      expect(isTokenExpired(null)).toBe(true)
    })

    it('returns true for token without exp claim', () => {
      const header = btoa('{}')
      const payload = btoa(JSON.stringify({ sub: 'user' }))
      const token = `${header}.${payload}.sig`
      expect(isTokenExpired(token)).toBe(true)
    })
  })
})
