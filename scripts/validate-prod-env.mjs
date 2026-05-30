import fs from 'node:fs'
import path from 'node:path'
import process from 'node:process'
import { fileURLToPath } from 'node:url'

const currentFile = fileURLToPath(import.meta.url)
const root = path.resolve(path.dirname(currentFile), '..')

function read(filePath) {
  return fs.readFileSync(filePath, 'utf8')
}

function parseEnvFile(filePath) {
  const result = new Map()
  for (const rawLine of read(filePath).split(/\r?\n/)) {
    const line = rawLine.trim()
    if (!line || line.startsWith('#')) continue
    const idx = line.indexOf('=')
    if (idx === -1) continue
    const key = line.slice(0, idx).trim()
    const value = line.slice(idx + 1).trim()
    result.set(key, value)
  }
  return result
}

function collectPlaceholders(text) {
  const vars = new Set()
  const regex = /\$\{([A-Z0-9_]+)(?::[^}]*)?\}/g
  let match
  while ((match = regex.exec(text)) !== null) {
    vars.add(match[1])
  }
  return vars
}

function fail(message) {
  console.error(`ENV TEMPLATE VALIDATION FAILED: ${message}`)
  process.exitCode = 1
}

function warn(message) {
  console.warn(`ENV TEMPLATE VALIDATION WARNING: ${message}`)
}

const envFileArg = process.argv[2] ?? '.env.prod.example'
const envFilePath = path.isAbsolute(envFileArg)
  ? envFileArg
  : path.join(root, envFileArg)
const prodYamlPath = path.join(root, 'oaiss-chain-backend', 'src', 'main', 'resources', 'application-prod.yml')
const prodComposePath = path.join(root, 'docker-compose.prod.yml')
const releaseComposePath = path.join(root, 'docker-compose.release.yml')

const envMap = parseEnvFile(envFilePath)
const yamlText = read(prodYamlPath)
const composeText = read(prodComposePath)
const releaseComposeText = read(releaseComposePath)

const referencedVars = new Set([
  ...collectPlaceholders(yamlText),
  ...collectPlaceholders(composeText),
  ...collectPlaceholders(releaseComposeText),
])

const allowedMissing = new Set([
  'IMAGE_TAG',
  'HTTP_BIND_ADDRESS',
  'HTTP_PORT',
  'BACKEND_CPU_LIMIT',
  'BACKEND_MEMORY_LIMIT',
  'BACKEND_MEMORY_RESERVATION',
  'FRONTEND_CPU_LIMIT',
  'FRONTEND_MEMORY_LIMIT',
  'ML_CPU_LIMIT',
  'ML_MEMORY_LIMIT',
  'ML_MODEL_DIR',
  'SERVER_SHUTDOWN_TIMEOUT',
  'DB_POOL_IDLE_TIMEOUT_MS',
  'DB_POOL_MAX_LIFETIME_MS',
  'REDIS_TIMEOUT_MS',
  'MINIO_MAX_FILE_SIZE_BYTES',
  'FABRIC_CONNECT_TIMEOUT_SECONDS',
  'FABRIC_SUBMIT_TIMEOUT_SECONDS',
])

for (const variable of referencedVars) {
  if (!envMap.has(variable) && !allowedMissing.has(variable)) {
    fail(`${path.basename(envFilePath)} is missing required variable ${variable}`)
  }
}

const criticalNoPlaceholder = [
  'DB_PASSWORD',
  'REDIS_PASSWORD',
  'JWT_SECRET',
  'RSA_KEK',
  'MINIO_ACCESS_KEY',
  'MINIO_SECRET_KEY',
  'ML_SERVICE_SECRET',
]

for (const key of criticalNoPlaceholder) {
  const value = envMap.get(key)
  if (!value) {
    fail(`critical variable ${key} is blank`)
    continue
  }
  if (/^(change_me|replace_with|placeholder|todo|dummy)/i.test(value)) {
    warn(`critical variable ${key} still uses a placeholder value in ${path.basename(envFilePath)}`)
  }
}

const requiredProdHosts = [
  ['CORS_ALLOWED_ORIGINS', /localhost|127\.0\.0\.1/i],
  ['DB_URL', /localhost|127\.0\.0\.1/i],
  ['REDIS_HOST', /localhost|127\.0\.0\.1/i],
  ['MINIO_ENDPOINT', /localhost|127\.0\.0\.1/i],
]

for (const [key, pattern] of requiredProdHosts) {
  const value = envMap.get(key)
  if (value && pattern.test(value)) {
    fail(`${key} must not point to localhost/127.0.0.1 in ${path.basename(envFilePath)}`)
  }
}

const mlServiceUrl = envMap.get('ML_SERVICE_URL')
if (mlServiceUrl && !/^http:\/\/ml-service:8001$|^https?:\/\/[^/]+/i.test(mlServiceUrl)) {
  fail('ML_SERVICE_URL should be an internal service URL such as http://ml-service:8001 or a real hosted endpoint')
}

if (process.exitCode !== 1) {
  console.log(`Environment template validation passed for ${path.basename(envFilePath)}`)
}
