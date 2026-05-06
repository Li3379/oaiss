import http from 'node:http'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const distDir = path.join(__dirname, 'dist')
const port = Number(process.env.PORT || 80)

const mimeTypes = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.gif': 'image/gif',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.webp': 'image/webp',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
}

function serveFile(filePath, res) {
  fs.readFile(filePath, (err, data) => {
    if (err) {
      res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' })
      res.end('Not Found')
      return
    }

    const ext = path.extname(filePath).toLowerCase()
    const contentType = mimeTypes[ext] || 'application/octet-stream'

    const headers = { 'Content-Type': contentType }
    if (/(\.js|\.css|\.png|\.jpg|\.jpeg|\.gif|\.svg|\.ico|\.webp|\.woff|\.woff2)$/i.test(filePath)) {
      headers['Cache-Control'] = 'public, max-age=604800, immutable'
    }

    res.writeHead(200, headers)
    res.end(data)
  })
}

const server = http.createServer((req, res) => {
  const reqPath = decodeURIComponent((req.url || '/').split('?')[0])
  const safePath = reqPath === '/' ? '/index.html' : reqPath
  const filePath = path.normalize(path.join(distDir, safePath))

  if (!filePath.startsWith(distDir)) {
    res.writeHead(403, { 'Content-Type': 'text/plain; charset=utf-8' })
    res.end('Forbidden')
    return
  }

  fs.stat(filePath, (err, stat) => {
    if (!err && stat.isFile()) {
      serveFile(filePath, res)
      return
    }

    serveFile(path.join(distDir, 'index.html'), res)
  })
})

server.listen(port, '0.0.0.0', () => {
  console.log(`Static server running on http://0.0.0.0:${port}`)
})
