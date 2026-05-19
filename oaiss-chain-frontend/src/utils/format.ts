/**
 * Format ISO date string to friendly display format
 * "2026-05-14T17:22:28" → "2026-05-14 17:22:28"
 */
export function formatDateTime(value: string | null | undefined): string {
  if (!value) return ''
  return value.replace('T', ' ').substring(0, 19)
}
