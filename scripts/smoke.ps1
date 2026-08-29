# M1 冒烟测试：起栈 → 健康检查 → 管理员登录 → 停栈
# 用法：在仓库根目录执行  .\scripts\smoke.ps1
# 前置：本机已安装 Docker Desktop 且已启动；.env 已按 .env.example 配好
param(
    [string]$AdminUser = "admin",
    [string]$AdminPassword = "Admin@whoami2026"
)

$ErrorActionPreference = "Stop"
$BaseUrl = "http://localhost"

function Step($msg) { Write-Host "==> $msg" -ForegroundColor Cyan }

Step "构建并启动全栈（docker compose up -d --build，首次较慢）"
docker compose up -d --build
if ($LASTEXITCODE -ne 0) { throw "docker compose up 失败" }

try {
    Step "等待后端健康检查通过"
    $deadline = (Get-Date).AddMinutes(6)
    $healthy = $false
    while ((Get-Date) -lt $deadline) {
        try {
            $health = Invoke-RestMethod -Uri "$BaseUrl/admin/api/health" -TimeoutSec 5
            if ($health.code -eq 0 -and $health.data.status -eq "up") { $healthy = $true; break }
        } catch { Start-Sleep -Seconds 5 }
    }
    if (-not $healthy) { throw "健康检查超时：GET /admin/api/health 未在 6 分钟内返回 up" }
    Write-Host "    health = up"

    Step "管理员登录（$AdminUser）"
    $login = Invoke-RestMethod -Method Post -Uri "$BaseUrl/admin/api/auth/login" `
        -ContentType "application/json" `
        -Body (@{ username = $AdminUser; password = $AdminPassword } | ConvertTo-Json)
    if ($login.code -ne 0 -or -not $login.data.token) { throw "登录失败：$($login | ConvertTo-Json -Compress)" }
    Write-Host "    登录成功，token 有效期 $($login.data.expiresIn) 秒"

    Step "带 token 调用 /admin/api/auth/me"
    $me = Invoke-RestMethod -Uri "$BaseUrl/admin/api/auth/me" -Headers @{ Authorization = "Bearer $($login.data.token)" }
    if ($me.code -ne 0 -or $me.data.username -ne $AdminUser) { throw "me 接口校验失败：$($me | ConvertTo-Json -Compress)" }
    Write-Host "    me = { id: $($me.data.id), username: $($me.data.username) }"

    Step "未登录访问受保护接口应返回 401"
    try {
        Invoke-RestMethod -Uri "$BaseUrl/admin/api/auth/me" | Out-Null
        throw "预期 401，实际却成功"
    } catch {
        $status = $_.Exception.Response.StatusCode.value__
        if ($status -ne 401) { throw "预期 401，实际 $status" }
        Write-Host "    401 校验通过"
    }

    Write-Host ""
    Write-Host "SMOKE OK：全栈一键起 + 管理员登录 + JWT 鉴权全部通过" -ForegroundColor Green
    exit 0
}
finally {
    Step "停栈（docker compose down，数据卷保留）"
    docker compose down
}
