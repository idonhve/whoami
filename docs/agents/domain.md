# 领域文档

工程技能在探索代码库时，应如何使用本仓库的领域文档。

## 探索之前，先读这些

* 仓库根目录的 **`CONTEXT.md`**，或

* 仓库根目录的 **`CONTEXT-MAP.md`**（如果存在）：它指向每个上下文各自的 `CONTEXT.md`。把与当前话题相关的每一份都读一遍。

* **`docs/adr/`**：阅读与你即将工作的区域相关的 ADR（架构决策记录）。在多上下文仓库中，还要检查 `src/<context>/docs/adr/` 里上下文范围内的决策。

如果这些文件不存在，**静默继续**。不要提示它们缺失，也不要建议预先创建。`/domain-modeling` 技能（经由 `/grill-with-docs` 和 `/improve-codebase-architecture` 触达）会在术语或决策真正确定下来时按需创建它们。

## 文件结构

单上下文仓库（大多数仓库属于这种）：

```
/
├── CONTEXT.md
├── docs/adr/
│   ├── 0001-event-sourced-orders.md
│   └── 0002-postgres-for-write-model.md
└── src/
```

多上下文仓库（根目录存在 `CONTEXT-MAP.md` 时）：

```
/
├── CONTEXT-MAP.md
├── docs/adr/                          ← 全系统范围的决策
└── src/
    ├── ordering/
    │   ├── CONTEXT.md
    │   └── docs/adr/                  ← 上下文专属的决策
    └── billing/
        ├── CONTEXT.md
        └── docs/adr/
```

## 使用词汇表中的词汇

当你的输出提到某个领域概念时（无论是 issue 标题、重构提案、假设还是测试名），都使用 `CONTEXT.md` 里定义的术语。不要漂移到词汇表明确回避的同义词。

如果你需要的概念还不在词汇表里，这是一个信号：要么你在发明项目根本没在用的语言（请重新考虑），要么确实存在空缺（记下来，交给 `/domain-modeling` 处理）。

## 标记 ADR 冲突

如果你的输出与某个现有 ADR 相矛盾，要明确指出，而不是悄悄覆盖：

> _与 ADR-0007（事件溯源订单）矛盾，但值得重新讨论，因为……_

