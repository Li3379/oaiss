/** 用户信息响应 */
export interface UserInfoResponse {
  userId: number
  username: string
  realName: string
  phone: string
  email: string
  avatar: string
  userType: number
  userTypeDesc: string
  status: number
  lastLoginAt: string
  lastLoginIp: string
  createdAt: string
}

/** 用户资料更新请求 */
export interface UserProfileUpdateRequest {
  realName?: string
  phone?: string
  email?: string
  avatar?: string
}

/** 用户实体 (Admin接口返回) */
export interface User {
  id: number
  username: string
  phone: string
  email: string
  realName: string
  userType: number
  status: number
  allowedIps: string
  lastLoginAt: string
  lastLoginIp: string
  avatar: string
  createdAt: string
  updatedAt: string
  deleted: boolean
}
