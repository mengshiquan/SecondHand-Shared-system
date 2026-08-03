<template>
  <div class="help page-container">
    <div class="page-header">
      <h1 class="page-title">帮助中心</h1>
      <p class="page-sub">常见问题与使用指南</p>
    </div>

    <div class="help-card">
      <el-collapse>
        <el-collapse-item title="如何发布商品？" name="1">
          <p>登录账号后，点击导航栏中的「发布」按钮或在首页点击「发布闲置」，填写商品标题、选择分类、设置价格、上传图片并撰写描述后提交即可。</p>
        </el-collapse-item>

        <el-collapse-item title="如何购买商品？" name="2">
          <p>在商品详情页点击「立即购买」按钮，填写收货信息（姓名、电话、地址），确认后生成订单。请在 30 分钟内完成付款，超时订单将自动取消。</p>
        </el-collapse-item>

        <el-collapse-item title="订单状态是什么意思？" name="3">
          <div class="status-guide">
            <div class="status-row"><el-tag type="warning" size="small">待付款</el-tag><span>买家已下单，尚未确认付款</span></div>
            <div class="status-row"><el-tag type="primary" size="small">已付款</el-tag><span>买家已付款，等待卖家发货</span></div>
            <div class="status-row"><el-tag type="success" size="small">已发货</el-tag><span>卖家已发货，等待买家收货</span></div>
            <div class="status-row"><el-tag type="success" size="small">已完成</el-tag><span>买家已确认收货，交易完成</span></div>
            <div class="status-row"><el-tag type="info" size="small">已取消</el-tag><span>订单已取消，商品重新上架</span></div>
          </div>
        </el-collapse-item>

        <el-collapse-item title="如何联系卖家？" name="4">
          <p>在商品详情页点击卖家信息卡片中的「联系卖家」按钮，可以给卖家留言。下单成功后，在订单详情页可以查看卖家的联系方式。</p>
        </el-collapse-item>

        <el-collapse-item title="如何收藏商品？" name="5">
          <p>在商品详情页点击「收藏」按钮即可将商品加入收藏列表。再次点击可取消收藏。收藏列表可在个人中心的「我的收藏」中查看。</p>
        </el-collapse-item>

        <el-collapse-item title="忘记密码了怎么办？" name="6">
          <p>目前可以通过重新注册或联系管理员重置密码。后续版本将支持自助找回密码功能。</p>
        </el-collapse-item>

        <el-collapse-item title="交易安全有保障吗？" name="7">
          <p>平台要求用户实名注册，每次交易生成独立订单便于追溯。建议在校内公共场所当面交易，并核对订单信息无误后再完成付款。</p>
        </el-collapse-item>
      </el-collapse>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.page-header { margin-bottom: 32px; text-align: center; }
.page-title { font-size: 28px; font-weight: 800; color: #1F2937; margin: 0 0 8px; }
.page-sub { font-size: 15px; color: #9CA3AF; margin: 0; }

.help-card {
  max-width: 760px; margin: 0 auto;
  background: #fff; border-radius: 20px;
  padding: 28px 36px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  border: 1px solid #F3F4F6;
}

.help-card p {
  color: #4B5563; font-size: 14px; line-height: 1.8; margin: 0;
  padding: 6px 0 10px;
}

.status-guide { display: flex; flex-direction: column; gap: 10px; padding: 6px 0 10px; }
.status-row { display: flex; align-items: center; gap: 12px; font-size: 14px; color: #4B5563; }

/* 平板端 */
@media (max-width: 768px) {
  .help-card { max-width: 100%; padding: 20px 18px; border-radius: 14px; }
  .page-title { font-size: 22px; }
  .page-sub { font-size: 14px; }
}
/* 手机端 */
@media (max-width: 480px) {
  .page-header { margin-bottom: 20px; }
  .page-title { font-size: 20px; }
  .help-card { padding: 16px 14px; border-radius: 12px; }
  .help-card :deep(.el-collapse-item__header) { font-size: 14px; }
  .help-card p { font-size: 13px; }
  .status-row { font-size: 13px; }
}
</style>
