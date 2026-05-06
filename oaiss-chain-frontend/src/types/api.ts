/** 通用分页信息 */
export interface PaginationInfo {
  page: number
  size: number
  total: number
  totalPages: number
}

/** 通用响应元数据 */
export interface ResponseMeta {
  requestId: string
  timestamp: string
  pagination: PaginationInfo | null
}

/** 通用API响应包装 */
export interface ApiResponse<T> {
  code: number
  message: string
  data: T | null
  meta: ResponseMeta | null
}

/** Spring Data Page 响应 */
export interface SpringPage<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
  empty: boolean
}

/** 通用分页请求参数 */
export interface PageRequest {
  pageNum?: number
  pageSize?: number
  sortBy?: string
  sortOrder?: string
  keyword?: string
  startTime?: string
  endTime?: string
}

/** 通用分页响应 */
export interface PageResponse<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
  hasPrevious: boolean
  hasNext: boolean
  isFirst: boolean
  isLast: boolean
}
