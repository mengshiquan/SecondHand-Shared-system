<template>
  <!-- 整页白板场景：薄荷纸底 + 网格 + 光晕 + 马克笔涂鸦（校园跳蚤市场） -->
  <div class="auth-bg" aria-hidden="true">
    <div class="auth-bg-grid"></div>
    <svg
      v-for="(it, i) in items"
      :key="i"
      class="float-item"
      :style="it.style + ';color:' + it.color"
      viewBox="0 0 64 64"
      fill="none"
      stroke="currentColor"
      stroke-width="3"
      stroke-linecap="round"
      stroke-linejoin="round"
    >
      <!-- 淡彩填充层：马克笔上色的感觉 -->
      <path v-for="(d, j) in it.fd" :key="'f' + j" :d="d" fill="currentColor" fill-opacity="0.10" stroke="none" />
      <circle v-for="(c, j) in it.cf" :key="'cf' + j" :cx="c[0]" :cy="c[1]" :r="c[2]" fill="currentColor" fill-opacity="0.10" stroke="none" />
      <path v-for="(d, j) in it.d" :key="j" :d="d" />
      <circle v-for="(c, j) in it.c" :key="'c' + j" :cx="c[0]" :cy="c[1]" :r="c[2]" />
    </svg>
  </div>
</template>

<script setup>
// 马克笔三色：翠绿=主笔、暖橙=价签/点缀、靛蓝=呼应 logo 手心
const G = 'var(--sh-primary)'
const A = 'var(--sh-accent)'
const I = '#6366F1'

const items = [
  { // 跳蚤市场摊位（蓬顶+柜台）
    d: ['M10 22l4-10h36l4 10', 'M10 22c4 6 9 6 13 0c4 6 9 6 13 0c4 6 9 6 13 0', 'M14 26v24h36V26', 'M14 38h36'],
    fd: ['M14 12h36l4 10H10z'],
    c: [],
    color: G,
    style: 'top:9%;left:34%;width:110px;animation-duration:28s'
  },
  { // 价签 ¥
    d: ['M30 6h26v22L38 46 20 28V6z', 'M29 16l5 7 5-7', 'M34 23v10', 'M30 26h8'],
    fd: ['M30 6h26v22L38 46 20 28V6z'],
    c: [[46, 12, 2]],
    color: A,
    style: 'top:5%;left:52%;width:72px;animation-duration:24s;animation-delay:-5s'
  },
  { // 自行车
    d: ['M16 44l10-20h12l10 20', 'M26 24l8 20H16', 'M40 20h-6'],
    c: [[16, 44, 9], [48, 44, 9]],
    color: I,
    style: 'bottom:24%;left:2%;width:104px;animation-duration:30s;animation-delay:-12s'
  },
  { // 翻开的书
    d: ['M8 16c8-4 16-4 24 0c8-4 16-4 24 0v32c-8-4-16-4-24 0c-8-4-16-4-24 0z', 'M32 16v32'],
    fd: ['M8 16c8-4 16-4 24 0c8-4 16-4 24 0v32c-8-4-16-4-24 0c-8-4-16-4-24 0z'],
    c: [],
    color: G,
    style: 'top:44%;left:3%;width:78px;animation-duration:26s;animation-delay:-8s'
  },
  { // 对话气泡
    d: ['M8 10h44v26H26L14 46V36H8z'],
    fd: ['M8 10h44v26H26L14 46V36H8z'],
    c: [[22, 23, 2], [30, 23, 2], [38, 23, 2]],
    color: I,
    style: 'top:6%;right:26%;width:76px;animation-duration:22s;animation-delay:-3s'
  },
  { // 耳机
    d: ['M12 42v-8a20 20 0 0 1 40 0v8', 'M10 42h9v12h-9z', 'M45 42h9v12h-9z'],
    c: [],
    color: A,
    style: 'top:34%;right:5%;width:70px;animation-duration:25s;animation-delay:-15s'
  },
  { // 星星
    d: ['M32 8l6 14 16 2-12 10 4 16-14-8-14 8 4-16-12-10 16-2z'],
    fd: ['M32 8l6 14 16 2-12 10 4 16-14-8-14 8 4-16-12-10 16-2z'],
    c: [],
    color: A,
    style: 'bottom:30%;left:38%;width:52px;animation-duration:21s;animation-delay:-9s'
  },
  { // 箭头涂鸦
    d: ['M8 44c12-18 28-24 44-16', 'M44 22l9 5-5 9'],
    c: [],
    color: G,
    style: 'top:64%;left:47%;width:84px;animation-duration:27s;animation-delay:-18s'
  },
  { // 台灯
    d: ['M20 54h24', 'M32 54V40', 'M32 40L22 26a12 12 0 0 1 20 0z', 'M42 26l6-6'],
    c: [],
    color: I,
    style: 'bottom:8%;right:8%;width:68px;animation-duration:24s;animation-delay:-6s'
  },
  { // 吉他
    d: ['M44 34L56 12', 'M54 10l6-2'],
    c: [[34, 42, 12], [34, 42, 4]],
    color: G,
    style: 'top:40%;left:50%;width:74px;animation-duration:29s;animation-delay:-11s'
  },
  { // 相机
    d: ['M10 22h12l4-6h12l4 6h12v26H10z'],
    c: [[32, 34, 8]],
    color: A,
    style: 'top:12%;right:6%;width:66px;animation-duration:23s;animation-delay:-14s'
  },
  { // 篮球
    d: ['M12 32h40', 'M32 12v40', 'M18 18c8 8 20 8 28 0', 'M18 46c8-8 20-8 28 0'],
    cf: [[32, 32, 20]],
    c: [[32, 32, 20]],
    color: I,
    style: 'bottom:10%;right:26%;width:60px;animation-duration:26s;animation-delay:-4s'
  },
  { // 爱心
    d: ['M32 52C12 36 12 20 24 16c6-2 8 4 8 4s2-6 8-4c12 4 12 20-8 32z'],
    fd: ['M32 52C12 36 12 20 24 16c6-2 8 4 8 4s2-6 8-4c12 4 12 20-8 32z'],
    c: [],
    color: G,
    style: 'top:30%;left:26%;width:48px;animation-duration:20s;animation-delay:-7s'
  }
]
</script>

<style scoped>
.auth-bg {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
  /* 三色氛围渐变：左薄荷 / 右上暖橙 / 右下靖蓝 + 薄荷纸底 */
  background:
    radial-gradient(42rem 32rem at 10% 40%, rgba(16, 185, 129, 0.16), transparent 62%),
    radial-gradient(36rem 28rem at 90% 8%, rgba(245, 158, 11, 0.13), transparent 60%),
    radial-gradient(40rem 30rem at 85% 92%, rgba(99, 102, 241, 0.10), transparent 60%),
    linear-gradient(180deg, #FBFDFB 0%, #EDF5F0 100%);
}
.auth-bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(16, 185, 129, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(16, 185, 129, 0.05) 1px, transparent 1px);
  background-size: 28px 28px;
}
.float-item {
  position: absolute;
  opacity: 0.2;
  animation: floatDrift 24s ease-in-out infinite alternate;
}
@keyframes floatDrift {
  from { transform: translateY(0) rotate(-2deg); }
  to   { transform: translateY(-14px) rotate(3deg); }
}
</style>
