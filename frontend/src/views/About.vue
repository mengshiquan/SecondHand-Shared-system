<template>
  <div class="about page-container">
    <div class="page-header">
      <h1 class="page-title">关于我们</h1>
      <p class="page-sub">了解校园二手共享平台的故事与愿景</p>
    </div>

    <div class="about-card">
      <div class="about-section">
        <div class="section-icon">
          <el-icon :size="28"><Shop /></el-icon>
        </div>
        <h2>我们的使命</h2>
        <p>
          校园二手共享平台致力于为高校学生提供一个安全、便捷的闲置物品交易社区。
          我们相信每一件闲置物品都有它的价值，通过循环利用减少浪费、降低学生生活成本，
          同时培养可持续的消费习惯。
        </p>
      </div>

      <el-divider />

      <div class="about-section">
        <div class="section-icon">
          <el-icon :size="28"><UserFilled /></el-icon>
        </div>
        <h2>面向群体</h2>
        <p>
          平台主要面向在校大学生群体，覆盖教材、数码产品、生活用品、服饰鞋包、运动器材、
          音乐乐器等校园常见闲置品类。无论是即将毕业的学长学姐、还是刚入学需要配置装备的新生，
          都能在这里找到所需。
        </p>
      </div>

      <el-divider />

      <div class="about-features">
        <h2>平台特色</h2>
        <div class="features-grid">
          <div class="feature-item">
            <el-icon :size="24" color="#10B981"><Checked /></el-icon>
            <span>实名认证，交易更安全</span>
          </div>
          <div class="feature-item">
            <el-icon :size="24" color="#10B981"><Checked /></el-icon>
            <span>订单追踪，流程透明</span>
          </div>
          <div class="feature-item">
            <el-icon :size="24" color="#10B981"><Checked /></el-icon>
            <span>评价体系，信誉可见</span>
          </div>
          <div class="feature-item">
            <el-icon :size="24" color="#10B981"><Checked /></el-icon>
            <span>分类清晰，快速找货</span>
          </div>
          <div class="feature-item">
            <el-icon :size="24" color="#10B981"><Checked /></el-icon>
            <span>免费发布，零手续费</span>
          </div>
          <div class="feature-item">
            <el-icon :size="24" color="#10B981"><Checked /></el-icon>
            <span>校内交易，方便面交</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// 平台介绍页：说明校园二手共享的定位、特色和保障。
import { Shop, UserFilled, Checked } from '@element-plus/icons-vue'
</script>

<style scoped>
.page-header { margin-bottom: 32px; text-align: center; }
.page-title { font-size: 28px; margin: 0 0 8px; justify-content: center; }
.page-sub { font-size: 15px; }

.about-card {
  max-width: 720px; margin: 0 auto;
  background: #fff; border-radius: 20px;
  padding: 40px 48px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  border: 1px solid #F3F4F6;
}

.about-section { text-align: center; padding: 8px 0 16px; }
.section-icon {
  display: inline-flex; align-items: center; justify-content: center;
  width: 56px; height: 56px; border-radius: 14px;
  background: linear-gradient(135deg, #ECFDF5, #D1FAE5);
  color: #10B981; margin-bottom: 16px;
}
.about-section h2 { font-size: 20px; font-weight: 700; color: #1F2937; margin: 0 0 12px; }
.about-section p {
  font-size: 15px; color: #4B5563; line-height: 1.9; margin: 0;
  max-width: 560px; margin: 0 auto;
}

.about-features { padding: 8px 0; }
.about-features h2 { text-align: center; font-size: 20px; font-weight: 700; color: #1F2937; margin: 0 0 20px; }
.features-grid {
  display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px;
}
.feature-item {
  display: flex; align-items: center; gap: 10px;
  padding: 14px 16px;
  background: #F9FAFB; border-radius: 10px;
  font-size: 14px; color: #374151;
}

@media (max-width: 768px) {
  .about-card { padding: 24px 20px; }
  .features-grid { grid-template-columns: 1fr; }
}
/* 手机端 */
@media (max-width: 480px) {
  .page-header { margin-bottom: 20px; }
  .page-title { font-size: 22px; }
  .about-card { padding: 20px 14px; border-radius: 14px; }
  .section-icon { width: 40px; height: 40px; border-radius: 10px; }
  .section-icon .el-icon { font-size: 22px; }
  .about-section h2 { font-size: 18px; }
  .about-section p { font-size: 14px; }
  .feature-item { padding: 10px 12px; font-size: 13px; }
}
</style>
