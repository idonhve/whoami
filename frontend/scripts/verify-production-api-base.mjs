import { readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const frontendRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const distRoot = path.resolve(frontendRoot, '..', 'dist')
const html = readFileSync(path.join(distRoot, 'index.html'), 'utf8')
const entryScript = html.match(/<script[^>]+src="([^"]+\.js)"/i)?.[1]

if (!entryScript) {
  throw new Error('Production HTML does not reference a JavaScript entry bundle')
}

const entryPath = path.join(distRoot, entryScript.replace(/^\//, ''))
const entryBundle = readFileSync(entryPath, 'utf8')
const expectedApiOrigin = 'https://idonhve-whoami-api.onrender.com'

if (!entryBundle.includes(expectedApiOrigin)) {
  throw new Error(`Production entry bundle is missing the backend API origin: ${expectedApiOrigin}`)
}

console.log(`Production entry bundle targets ${expectedApiOrigin}`)
