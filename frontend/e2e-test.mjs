// End-to-end test: 用户 + 管理员 全流程测试
import { chromium } from 'playwright';
import { execSync } from 'node:child_process';

// 验证码存于 Redis，本地测试直接读取当前验证码填入
function readCaptchaCode() {
  try {
    const keys = execSync('D:\\tool\\Redis\\redis-cli.exe --scan --pattern "captcha:*"')
      .toString().trim().split(/\r?\n/).filter(Boolean);
    if (!keys.length) return '';
    return execSync(`D:\\tool\\Redis\\redis-cli.exe GET "${keys[keys.length - 1]}"`).toString().trim();
  } catch { return ''; }
}

const BASE = 'http://localhost:5173';
const OUT = 'd:/code/SecondHand-Shared-system/frontend/test-screenshots';
import { mkdirSync } from 'fs';
mkdirSync(OUT, { recursive: true });

let passed = 0, failed = 0;

function log(tag, msg, ok) {
  const icon = ok === true ? '✅' : ok === false ? '❌' : '';
  console.log(`  ${icon} ${msg}`);
  if (ok === true) passed++; else if (ok === false) failed++;
}

async function run() {
  const browser = await chromium.launch({ headless: true });
  const ctx = await browser.newContext({ viewport: { width: 1920, height: 1080 } });
  const page = await ctx.newPage();

  // ================================================================
  // 一、未登录用户
  // ================================================================
  console.log('\n═══ 一、未登录游客 ═══');

  await page.goto(BASE, { waitUntil: 'networkidle', timeout: 30000 });
  await page.waitForTimeout(1500);
  let body = await page.textContent('body');
  log('H', '首页加载', body.includes('校园二手') && body.includes('下午好'));
  await page.screenshot({ path: `${OUT}/01-homepage.png`, fullPage: true });

  await page.goto(`${BASE}/products`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1000);
  body = await page.textContent('body');
  log('H', '商品列表页加载', body.includes('全部商品'));
  await page.screenshot({ path: `${OUT}/02-products.png`, fullPage: true });

  await page.goto(`${BASE}/about`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(800);
  body = await page.textContent('body');
  log('H', '关于我们页加载', body.includes('关于我们'));
  await page.screenshot({ path: `${OUT}/03-about.png`, fullPage: true });

  await page.goto(`${BASE}/help`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(800);
  body = await page.textContent('body');
  log('H', '帮助中心页加载', body.includes('帮助中心'));
  await page.screenshot({ path: `${OUT}/04-help.png`, fullPage: true });

  // 点击"更多"下拉
  await page.goto(BASE, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1000);
  const moreBtn = page.locator('.nav-more');
  if (await moreBtn.isVisible()) {
    await moreBtn.hover();
    await page.waitForTimeout(500);
    await page.screenshot({ path: `${OUT}/05-nav-more-dropdown.png` });
    log('H', '导航"更多"下拉菜单', true);
  } else {
    log('H', '导航"更多"下拉菜单', false);
  }

  // 未登录访问发布页 → 应该重定向到登录页
  await page.goto(`${BASE}/publish`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1000);
  const redirected = page.url().includes('/login');
  log('H', '未登录访问发布页 → 重定向到登录', redirected);

  // ================================================================
  // 二、登录为学生
  // ================================================================
  console.log('\n═══ 二、登录普通用户（student/123456）═══');

  await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(800);
  await page.locator('input[type="text"]').first().fill('student');
  await page.locator('input[type="password"]').fill('123456');
  await page.waitForTimeout(600);
  await page.locator('input[placeholder="验证码"]').fill(readCaptchaCode());
  await page.screenshot({ path: `${OUT}/06-login-filled.png` });
  await page.locator('button[type="button"]').first().click();
  await page.waitForTimeout(2000);

  body = await page.textContent('body');
  const loggedIn = body.includes('校园同学') || body.includes('我的订单') || body.includes('退出登录');
  log('L', 'student 登录成功', loggedIn);

  if (!loggedIn) {
    // 查看状态
    console.log('  Login response body preview:', body.substring(0, 300));
  }

  await page.goto(`${BASE}/publish`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1000);
  body = await page.textContent('body');
  log('H', '登录后访问发布页', body.includes('发布闲置商品') && !page.url().includes('/login'));
  await page.screenshot({ path: `${OUT}/07-publish-page.png`, fullPage: true });

  await page.goto(`${BASE}/orders`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1000);
  body = await page.textContent('body');
  log('H', '我的订单页', body.includes('我的订单'));
  await page.screenshot({ path: `${OUT}/08-orders.png`, fullPage: true });

  await page.goto(`${BASE}/profile`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1000);
  body = await page.textContent('body');
  log('H', '个人中心页', body.includes('个人资料') || body.includes('我的发布'));
  await page.screenshot({ path: `${OUT}/09-profile.png`, fullPage: true });

  // 测试商品详情 - 找到第一个商品
  await page.goto(`${BASE}/products`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1500);
  const firstCard = page.locator('.product-card').first();
  let productDetailOK = false;
  if (await firstCard.isVisible().catch(() => false)) {
    await firstCard.click();
    await page.waitForTimeout(1500);
    body = await page.textContent('body');
    productDetailOK = body.includes('次浏览') || body.includes('商品描述');
    log('H', '商品详情页', productDetailOK);

    // 测试联系卖家
    const contactBtn = page.locator('.btn-contact');
    if (await contactBtn.isVisible().catch(() => false)) {
      await contactBtn.click();
      await page.waitForTimeout(600);
      await page.screenshot({ path: `${OUT}/10-contact-seller.png` });
      log('H', '联系卖家弹窗', true);
      // 关闭弹窗
      await page.keyboard.press('Escape');
      await page.waitForTimeout(500);
    } else {
      log('H', '联系卖家按钮（可能为自己商品）', null);
    }

    // 测试投诉按钮
    const complaintBtn = page.locator('.btn-complaint');
    if (await complaintBtn.isVisible().catch(() => false)) {
      await complaintBtn.click();
      await page.waitForTimeout(600);
      await page.screenshot({ path: `${OUT}/11-complaint-dialog.png` });
      log('H', '投诉弹窗', true);
      await page.keyboard.press('Escape');
      await page.waitForTimeout(500);
    } else {
      log('H', '投诉按钮（可能为自己商品）', null);
    }

    // 检查卖家其他商品
    const sellerSection = page.locator('.seller-products');
    log('H', '卖家其他在售区块', await sellerSection.isVisible().catch(() => false));

    // 检查分享按钮
    const shareBtn = page.locator('.btn-share');
    log('H', '分享按钮', await shareBtn.isVisible().catch(() => false));
  } else {
    log('H', '商品列表无商品（数据库为空）', null);
  }

  // 测试投诉提交
  await page.goto(`${BASE}/product/1`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1500);
  body = await page.textContent('body');
  if (body.includes('商品描述') || body.includes('卖家')) {
    const cBtn = page.locator('.btn-complaint');
    if (await cBtn.isVisible().catch(() => false)) {
      await cBtn.click();
      await page.waitForTimeout(500);
      // 选择投诉原因
      const select = page.locator('.el-select').first();
      if (await select.isVisible()) {
        await select.click();
        await page.waitForTimeout(400);
        await page.locator('.el-select-dropdown__item').first().click();
        await page.waitForTimeout(300);
      }
      await page.locator('textarea').first().fill('测试投诉：该用户行为可疑');
      await page.screenshot({ path: `${OUT}/12-complaint-form-filled.png` });
      // 找提交投诉按钮（在 el-dialog footer 中的 primary/danger 按钮）
      const submitBtn = page.locator('.el-dialog__footer button.el-button--danger, .el-dialog__footer button.el-button--primary').last();
      if (await submitBtn.isVisible().catch(() => false)) {
        await submitBtn.click();
        await page.waitForTimeout(1500);
      }
      body = await page.textContent('body');
      body = await page.textContent('body');
      log('H', '提交投诉', body.includes('已提交') || body.includes('已对该用户提交过'));
    }
  }

  // 测试联系卖家弹窗
  await page.goto(`${BASE}/product/1`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1500);
  const sellerContact = page.locator('.btn-contact');
  if (await sellerContact.isVisible().catch(() => false)) {
    await sellerContact.click();
    await page.waitForTimeout(500);
    await page.screenshot({ path: `${OUT}/13-contact-seller-dialog.png` });
    log('H', '联系卖家弹窗（含留言和购买入口）', true);
    await page.keyboard.press('Escape');
    await page.waitForTimeout(500);
  }

  // ================================================================
  // 三、登出，登录为管理员
  // ================================================================
  console.log('\n═══ 三、登录管理员（admin/admin）═══');

  // 登出
  await page.goto(BASE, { waitUntil: 'networkidle' });
  await page.waitForTimeout(800);
  const nickname = page.locator('.nickname').first();
  if (await nickname.isVisible().catch(() => false)) {
    await nickname.click();
    await page.waitForTimeout(600);
    const logoutItem = page.locator('.el-dropdown-menu__item:has-text("退出登录")');
    if (await logoutItem.isVisible().catch(() => false)) {
      await logoutItem.click();
      await page.waitForTimeout(1500);
      log('H', '登出成功', true);
    } else {
      log('H', '退出登录菜单项不可见', false);
    }
  } else {
    log('H', '用户昵称不可见，可能已登出', null);
  }

  // 管理员登录
  await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(800);
  await page.locator('input[type="text"]').first().fill('admin');
  await page.locator('input[type="password"]').fill('admin');
  await page.waitForTimeout(600);
  await page.locator('input[placeholder="验证码"]').fill(readCaptchaCode());
  await page.locator('button[type="button"]').first().click();
  await page.waitForTimeout(2000);

  body = await page.textContent('body');
  log('L', 'admin 登录成功', body.includes('管理员') || body.includes('后台管理'));

  // 访问后台
  await page.goto(`${BASE}/admin`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(2000);
  body = await page.textContent('body');
  log('H', '后台管理-数据概览', body.includes('数据概览') || body.includes('用户总数'));
  await page.screenshot({ path: `${OUT}/14-admin-dashboard.png`, fullPage: true });

  // 用户管理
  const userTab = page.locator('.el-tabs__item:has-text("用户管理")');
  if (await userTab.isVisible().catch(() => false)) { await userTab.click(); await page.waitForTimeout(1000); }
  body = await page.textContent('body');
  log('H', '用户管理标签页', body.includes('用户名') || body.includes('暂无数据'));
  await page.screenshot({ path: `${OUT}/15-admin-users.png`, fullPage: true });

  // 小黑屋
  const blTab = page.locator('.el-tabs__item:has-text("小黑屋")');
  if (await blTab.isVisible().catch(() => false)) {
    await blTab.click();
    await page.waitForTimeout(1000);
    body = await page.textContent('body');
    log('H', '小黑屋标签页', body.includes('受限用户') || body.includes('暂无数据'));
    await page.screenshot({ path: `${OUT}/16-admin-blacklist.png`, fullPage: true });

    // 手动扫描
    const scanBtn = page.locator('button:has-text("手动扫描")');
    if (await scanBtn.isVisible().catch(() => false)) {
      await scanBtn.click();
      await page.waitForTimeout(2000);
      body = await page.textContent('body');
      log('H', '手动触发扫描', body.includes('扫描完成') || body.includes('受限用户'));
    }
  }

  // 投诉/申诉
  const reportTab = page.locator('.el-tabs__item:has-text("投诉")');
  if (await reportTab.isVisible().catch(() => false)) {
    await reportTab.click();
    await page.waitForTimeout(1000);
    body = await page.textContent('body');
    log('H', '投诉/申诉标签页', body.includes('投诉列表') || body.includes('申诉列表') || body.includes('暂无数据'));
    await page.screenshot({ path: `${OUT}/17-admin-reports.png`, fullPage: true });
  }

  // ================================================================
  // 四、响应式测试
  // ================================================================
  console.log('\n═══ 四、响应式 ═══');
  for (const [label, w, h] of [['tablet', 768, 1024], ['mobile', 375, 812]]) {
    await page.setViewportSize({ width: w, height: h });
    await page.goto(BASE, { waitUntil: 'networkidle' });
    await page.waitForTimeout(1000);
    await page.screenshot({ path: `${OUT}/18-${label}.png`, fullPage: true });
    log('H', `${label} (${w}px)`, true);
  }

  // ================================================================
  // 总结
  // ================================================================
  console.log(`\n\n══════════════════════════════`);
  console.log(`  测试完成: ✅ ${passed} 通过  ❌ ${failed} 失败`);
  console.log(`  截图: ${OUT}/`);
  console.log(`══════════════════════════════`);

  await browser.close();
}

run().catch(err => { console.error('TEST FAILED:', err); process.exit(1); });
