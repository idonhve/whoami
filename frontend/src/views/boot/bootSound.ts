/**
 * 开机打字音效：WebAudio 方波短促"咔嗒"声，无音频资源文件。
 * 浏览器自动播放策略下 AudioContext 可能处于 suspended，
 * 在首次用户交互（pointerdown/keydown）时恢复；静音偏好由 bootSession 持久化。
 */
export class BootSound {
  private ctx: AudioContext | null = null
  private unlockBound = false

  muted = false

  private ensureCtx(): AudioContext | null {
    if (this.ctx) return this.ctx
    try {
      this.ctx = new AudioContext()
    } catch {
      this.ctx = null
    }
    return this.ctx
  }

  /** 尝试恢复被自动播放策略挂起的上下文；并绑定一次性交互解锁 */
  unlock(): void {
    const ctx = this.ensureCtx()
    if (!ctx) return
    if (ctx.state === 'suspended') {
      void ctx.resume().catch(() => {})
    }
    if (!this.unlockBound) {
      this.unlockBound = true
      const onGesture = () => {
        if (this.ctx?.state === 'suspended') {
          void this.ctx.resume().catch(() => {})
        }
      }
      window.addEventListener('pointerdown', onGesture, { once: true })
      window.addEventListener('keydown', onGesture, { once: true })
    }
  }

  /** 单次打字咔嗒声 */
  blip(): void {
    if (this.muted) return
    const ctx = this.ensureCtx()
    if (!ctx || ctx.state !== 'running') return
    const osc = ctx.createOscillator()
    const gain = ctx.createGain()
    osc.type = 'square'
    osc.frequency.value = 1400 + Math.random() * 600
    gain.gain.setValueAtTime(0.018, ctx.currentTime)
    gain.gain.exponentialRampToValueAtTime(0.0001, ctx.currentTime + 0.04)
    osc.connect(gain)
    gain.connect(ctx.destination)
    osc.start()
    osc.stop(ctx.currentTime + 0.045)
  }

  /** 日志行完成的确认音（音调略高） */
  confirm(): void {
    if (this.muted) return
    const ctx = this.ensureCtx()
    if (!ctx || ctx.state !== 'running') return
    const osc = ctx.createOscillator()
    const gain = ctx.createGain()
    osc.type = 'square'
    osc.frequency.setValueAtTime(880, ctx.currentTime)
    osc.frequency.setValueAtTime(1320, ctx.currentTime + 0.05)
    gain.gain.setValueAtTime(0.02, ctx.currentTime)
    gain.gain.exponentialRampToValueAtTime(0.0001, ctx.currentTime + 0.1)
    osc.connect(gain)
    gain.connect(ctx.destination)
    osc.start()
    osc.stop(ctx.currentTime + 0.11)
  }

  dispose(): void {
    if (this.ctx) {
      void this.ctx.close().catch(() => {})
      this.ctx = null
    }
  }
}
