#!/usr/bin/env bash
# M1 冒烟测试：起栈 → 健康检查 → 管理员登录 → 停栈
# 用法：在仓库根目录执行  ./scripts/smoke.sh
# 前置：本机已安装 Docker 且已启动；.env 已按 .env.example 配好
set -euo pipefail

ADMIN_USER="${1:-admin}"
ADMIN_PASSWORD="${2:-Admin@whoami2026}"
BASE_URL="${BASE_URL:-http://localhost}"

step() { echo "==> $1"; }

step "构建并启动全栈（docker compose up -d --build，首次较慢）"
docker compose up -d --build

cleanup() {
  step "停栈（docker compose down，数据卷保留）"
  docker compose down
}
trap cleanup EXIT

step "等待后端健康检查通过"
healthy=0
for i in $(seq 1 72); do
  if body=$(curl -fsS "$BASE_URL/admin/api/health" 2>/dev/null) \
     && echo "$body" | grep -q '"status":"up"'; then
    healthy=1
    break
  fi
  sleep 5
done
if [ "$healthy" -ne 1 ]; then
  echo "健康检查超时：GET /admin/api/health 未在 6 分钟内返回 up" >&2
  exit 1
fi
echo "    health = up"

step "管理员登录（$ADMIN_USER）"
login=$(curl -fsS -X POST "$BASE_URL/admin/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASSWORD\"}")
token=$(echo "$login" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
expires=$(echo "$login" | grep -o '"expiresIn":[0-9]*' | cut -d: -f2)
if [ -z "$token" ]; then
  echo "登录失败：$login" >&2
  exit 1
fi
echo "    登录成功，token 有效期 ${expires} 秒"

step "带 token 调用 /admin/api/auth/me"
me=$(curl -fsS "$BASE_URL/admin/api/auth/me" -H "Authorization: Bearer $token")
echo "    me = $me"

step "未登录访问受保护接口应返回 401"
status=$(curl -s -o /dev/null -w '%{http_code}' "$BASE_URL/admin/api/auth/me")
if [ "$status" != "401" ]; then
  echo "预期 401，实际 $status" >&2
  exit 1
fi
echo "    401 校验通过"

echo ""
echo "SMOKE OK：全栈一键起 + 管理员登录 + JWT 鉴权全部通过"
