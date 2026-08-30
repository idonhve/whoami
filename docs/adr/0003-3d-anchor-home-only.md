# 3D 锚点仅首页一处，内容展示页动效一律 DOM/CSS

前台各内容展示页（/works、/tech、/experience、/awards、/about）的重动效一律用 DOM + CSS 实现（GSAP 仅做编排辅助），Three.js 3D 锚点全站只保留首页 HeroScene3D 一处。原因：站主明确反感重特效带来的卡顿感，内容页的核心价值是信息可读性与交互响应速度；且 MASTER.md 约定"每页有且仅有一个 3D 锚点"，若内容页各自配 3D 场景会造成 GPU 常驻负载与维护成本翻倍。钉子照片墙（/awards）因此采用 CSS transform 摆动（transform-origin 设在钉子处）而非物理引擎或 3D 场景。

**Considered Options**：Three.js 3D 挂墙（真物理摆动、可拖拽视角）——视觉更炫但占用 3D 锚点名额、低配设备降级成本高、与"轻快不卡"的体验目标冲突，被否。
