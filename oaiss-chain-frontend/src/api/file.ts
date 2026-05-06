import request from './request'
import type { UploadResult, FileInfo, FileListResult } from '../types'
import type { AxiosProgressEvent } from 'axios'

const MAX_FILE_SIZE = 50 * 1024 * 1024 // 50MB

export function uploadFile(
  file: File,
  folder = '',
  onProgress?: (event: AxiosProgressEvent) => void,
): Promise<UploadResult> {
  if (!file) return Promise.reject(new Error('请选择文件'))
  if (file.size > MAX_FILE_SIZE) return Promise.reject(new Error('文件大小不能超过50MB'))

  const formData = new FormData()
  formData.append('file', file)
  if (folder) {
    formData.append('folder', folder)
  }
  return request.post('/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: onProgress,
  })
}

export function batchUploadFiles(
  files: File[],
  folder = '',
  onProgress?: (event: AxiosProgressEvent) => void,
): Promise<UploadResult[]> {
  if (!Array.isArray(files) || files.length === 0) return Promise.reject(new Error('请选择文件'))
  for (const file of files) {
    if (file.size > MAX_FILE_SIZE) return Promise.reject(new Error(`${file.name} 超过50MB限制`))
  }

  const formData = new FormData()
  files.forEach((file) => formData.append('files', file))
  if (folder) {
    formData.append('folder', folder)
  }
  return request.post('/file/upload/batch', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: onProgress,
  })
}

export function downloadFile(objectName: string): Promise<Blob> {
  return request.get('/file/download', {
    params: { objectName },
    responseType: 'blob',
  })
}

export function deleteFile(objectName: string): Promise<void> {
  return request.delete('/file', { params: { objectName } })
}

export function batchDeleteFiles(objectNames: string[]): Promise<void> {
  return request.delete('/file/batch', { data: { objectNames } })
}

export function getFileInfo(objectName: string): Promise<FileInfo> {
  return request.get('/file/info', { params: { objectName } })
}

export function fileExists(objectName: string): Promise<boolean> {
  return request.get('/file/exists', { params: { objectName } })
}

export function getPresignedUrl(objectName: string): Promise<string> {
  return request.get('/file/presigned-url', { params: { objectName } })
}

export function getPresignedUploadUrl(objectName: string): Promise<string> {
  return request.get('/file/presigned-upload-url', { params: { objectName } })
}

export function listFiles(params?: Record<string, unknown>): Promise<FileListResult> {
  return request.get('/file/list', { params })
}

export function copyFile(sourceObjectName: string, destinationObjectName: string): Promise<void> {
  return request.post('/file/copy', { sourceObjectName, destinationObjectName })
}
