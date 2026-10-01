import { cpSync, existsSync, rmSync } from 'node:fs'
import { spawn } from 'node:child_process'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const project = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const backendOrigin = process.env.VITE_API_BASE
if (!backendOrigin || new URL(backendOrigin).protocol !== 'https:') {
  throw new Error('Set VITE_API_BASE to the verified backend HTTPS origin before building Sites.')
}
const npmCli = process.env.npm_execpath || resolve(dirname(process.execPath), 'node_modules/npm/bin/npm-cli.js')
if (!existsSync(npmCli)) throw new Error('Cannot locate npm. Run this helper from the installed Node.js runtime.')
const build = await new Promise((accept, reject) => {
  const child = spawn(process.execPath, [npmCli, 'run', 'build'], {
    cwd: resolve(project, 'frontend'),
    env: process.env,
    stdio: 'inherit',
  })
  child.once('error', reject)
  child.once('close', (status, signal) => accept({ status, signal }))
})
if (build.status !== 0) {
  throw new Error(`Frontend build failed (status=${build.status}, signal=${build.signal}).`)
}
const output = resolve(project, 'dist')
if (dirname(output) !== project) throw new Error('Build output must stay directly inside this project.')
rmSync(output, { recursive: true, force: true })
cpSync(resolve(project, 'frontend/dist'), output, { recursive: true })
console.log('Sites static output ready in dist/.')
