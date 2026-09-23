# 引力核心运行动画预览

直接打开index.html即可操作，无外部脚本或网络依赖。拖动改变视角，滚轮缩放；运行/停机按钮和负载滑条演示加减速、反向转环、呼吸浮动与六向汇聚。

mesh.js由 `python MekGravity/tools/generate_motion_preview.py` 从游戏实际core_energy/core_ring_0/core_ring_1.obj导出。motion.js与CoreRenderer/CoreMotion使用相同速度、幅度和指数过渡参数，但本预览是Canvas简化着色和深度排序，不复现游戏材质、光照或玻璃遮挡。

几何和预览代码为本项目原创，沿用仓库MIT许可证。未使用新生成位图。alpha.7游戏实现已编译并通过服务端逻辑/同步测试；浏览器已检查满载、25%负载、停机相位静止和重新启动。实际视觉由用户游戏内验收。
