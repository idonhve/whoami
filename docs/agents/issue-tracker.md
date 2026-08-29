# Issue 跟踪：GitHub

本仓库的 Issue 和规格文档以 GitHub issue 的形式管理。所有操作均使用 `gh` CLI。

## 约定

* **创建 issue**：`gh issue create --title "..." --body "..."`。多行正文使用 heredoc 写法。

* **查看 issue**：`gh issue view <number> --comments`，用 `jq` 过滤评论，并一并获取标签。

* **列出 issue**：`gh issue list --state open --json number,title,body,labels,comments --jq '[.[] | {number, title, body, labels: [.labels[].name], comments: [.comments[].body]}]'`，按需加 `--label` 和 `--state` 过滤条件。

* **给 issue 写评论**：`gh issue comment <number> --body "..."`

* **添加 / 移除标签**：`gh issue edit <number> --add-label "..."` / `--remove-label "..."`

* **关闭 issue**：`gh issue close <number> --comment "..."`

仓库名可从 `git remote -v` 推断；在克隆目录里运行 `gh` 时它会自动识别。

## PR 是否作为分诊入口

**PRs as a request surface: no**（PR 作为请求入口：否）。_（如果本仓库把外部 PR 当作功能请求处理，请把_ _`no`_ _改成_ _`yes`；`/triage`_ _技能会读取这个开关。）_

设为 `yes` 时，PR 会走和 issue 相同的标签与状态流转，命令换成 `gh pr` 等价形式：

* **查看 PR**：`gh pr view <number> --comments`，改动内容用 `gh pr diff <number>`。

* **列出待分诊的外部 PR**：`gh pr list --state open --json number,title,body,labels,author,authorAssociation,comments`，只保留 `authorAssociation` 为 `CONTRIBUTOR`、`FIRST_TIME_CONTRIBUTOR` 或 `NONE` 的（丢弃 `OWNER`/`MEMBER`/`COLLABORATOR`）。

* **评论 / 打标签 / 关闭**：`gh pr comment`、`gh pr edit --add-label`/`--remove-label`、`gh pr close`。

GitHub 的 issue 和 PR 共用同一套编号，所以 `#42` 可能是两者之一：先用 `gh pr view 42` 查，查不到再退回 `gh issue view 42`。

## 当技能说"发布到 issue 跟踪系统"时

创建一个 GitHub issue。

## 当技能说"获取相关工单"时

运行 `gh issue view <number> --comments`。

## Wayfinder（探路）操作

供 `/wayfinder` 使用。**地图（map）是一个单独的 issue，它的**子 issue 充当工单。

* **地图**：一个带 `wayfinder:map` 标签的 issue，正文包含"笔记 / 目前的决策 / 迷雾"三部分。用 `gh issue create --label wayfinder:map` 创建。

* **子工单**：以 GitHub sub-issue 形式挂到地图下面的 issue（通过 `gh api` 调用 sub-issues 端点）。如果没启用 sub-issues，就把子工单加进地图正文的任务列表，并在子工单正文开头写 `Part of #<map>`。标签：`wayfinder:<type>`（`research`/`prototype`/`grilling`/`task`）。工单一旦被认领，就指派给主导该工作的开发者。

* **阻塞关系**：使用 GitHub **原生 issue 依赖**，这是权威且在界面上可见的表示方式。添加依赖：`gh api --method POST repos/<owner>/<repo>/issues/<child>/dependencies/blocked_by -F issue_id=<blocker-db-id>`，其中 `<blocker-db-id>` 是阻塞方的数字**数据库 id**（`gh api repos/<owner>/<repo>/issues/<n> --jq .id`，不是 `#编号`，也不是 `node_id`）。GitHub 通过 `issue_dependencies_summary.blocked_by` 报告阻塞情况（只统计未关闭的阻塞方，这就是实时门槛）。如果依赖功能不可用，退而使用子工单正文开头的 `Blocked by: #<n>, #<n>` 一行。所有阻塞方都关闭后，工单才算解除阻塞。

* **前沿查询**：列出地图下所有未关闭的子工单（`gh issue list --state open`，限定在地图的 sub-issues / 任务列表范围内），剔除那些还有未关闭阻塞方的（`issue_dependencies_summary.blocked_by > 0`，或 `Blocked by` 行里有未关闭的 issue）以及已有指派人的；按地图顺序，排最前面的优先。

* **认领**：`gh issue edit <n> --add-assignee @me`，这是本会话的第一次写操作。

* **解决**：`gh issue comment <n> --body "<answer>"`，然后 `gh issue close <n>`，最后在地图的"目前的决策"末尾追加一条上下文指引（gist + 链接）。

