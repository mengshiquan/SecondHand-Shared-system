<template>
  <div class="admin page-container">
    <div class="page-header">
      <h1 class="page-title">后台管理</h1>
      <p class="page-sub">管理平台用户、商品、订单和分类</p>
    </div>

    <el-tabs v-model="activeTab" class="admin-tabs">
      <!-- 仪表盘 -->
      <el-tab-pane name="dashboard">
        <template #label><el-icon><DataAnalysis /></el-icon> 数据概览</template>
        <el-row :gutter="20" v-if="dashboard">
          <el-col :xs="12" :sm="6">
            <div class="stat-card stat-users" shadow="hover">
              <div class="stat-icon-bg"><el-icon :size="24"><User /></el-icon></div>
              <div class="stat-value">{{ dashboard.userCount }}</div>
              <div class="stat-label">用户总数</div>
            </div>
          </el-col>
          <el-col :xs="12" :sm="6">
            <div class="stat-card stat-products">
              <div class="stat-icon-bg"><el-icon :size="24"><Goods /></el-icon></div>
              <div class="stat-value">{{ dashboard.productCount }}</div>
              <div class="stat-label">商品总数</div>
            </div>
          </el-col>
          <el-col :xs="12" :sm="6">
            <div class="stat-card stat-orders">
              <div class="stat-icon-bg"><el-icon :size="24"><Document /></el-icon></div>
              <div class="stat-value">{{ dashboard.orderCount }}</div>
              <div class="stat-label">订单总数</div>
            </div>
          </el-col>
          <el-col :xs="12" :sm="6">
            <div class="stat-card stat-today">
              <div class="stat-icon-bg"><el-icon :size="24"><TrendCharts /></el-icon></div>
              <div class="stat-value">{{ dashboard.todayOrderCount }}</div>
              <div class="stat-label">今日订单</div>
            </div>
          </el-col>
        </el-row>

        <el-row v-if="activeTab === 'dashboard'" :gutter="20" style="margin-top: 20px">
          <el-col :xs="24" :lg="12">
            <div class="chart-card">
              <div class="chart-card-header">
                <span class="chart-title">商品分类分布</span>
              </div>
              <div id="pieChartBox" class="chart-box"></div>
            </div>
          </el-col>
          <el-col :xs="24" :lg="12">
            <div class="chart-card">
              <div class="chart-card-header">
                <span class="chart-title">订单状态分布</span>
              </div>
              <div id="barChartBox" class="chart-box"></div>
            </div>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- 用户管理：下设普通用户管理 / 管理员管理两个分类 -->
      <el-tab-pane name="users">
        <template #label><el-icon><User /></el-icon> 用户管理</template>
        <el-tabs v-model="userSubTab" type="card" style="margin-top:0">
          <el-tab-pane name="normal" label="普通用户管理" />
          <el-tab-pane name="admins" label="管理员管理" />
        </el-tabs>

        <!-- 普通用户管理 -->
        <div v-if="userSubTab === 'normal'" class="table-card" style="margin-top:12px">
          <div class="table-header">
            <span>共 <strong>{{ userTotal }}</strong> 个用户</span>
            <div class="table-header-actions">
              <el-button size="small" :type="pendingFilter ? 'warning' : 'default'" plain @click="togglePendingFilter">
                {{ pendingFilter ? '查看全部' : '仅看待审核' }}
              </el-button>
              <el-button v-if="selectedVerifyUsers.length" size="small" type="success" @click="handleBatchVerify('APPROVE')">
                批量通过（{{ selectedVerifyUsers.length }}）
              </el-button>
              <el-button v-if="selectedVerifyUsers.length" size="small" type="danger" plain @click="handleBatchVerify('REJECT')">
                批量拒绝（{{ selectedVerifyUsers.length }}）
              </el-button>
              <el-button size="small" :icon="Download" @click="handleExport('users')">导出</el-button>
              <el-button type="primary" size="small" :icon="Plus" @click="openUserDialog()">新增用户</el-button>
            </div>
          </div>
          <el-table :data="users" stripe @selection-change="rows => selectedVerifyUsers = rows">
            <el-table-column type="selection" width="44" :selectable="row => row.verifyStatus === 'PENDING'" />
            <el-table-column label="ID" width="70">
              <template #default="{ $index }">{{ $index + 1 }}</template>
            </el-table-column>
            <el-table-column prop="username" label="用户名" min-width="110" />
            <el-table-column prop="nickname" label="昵称" min-width="110" />
            <el-table-column prop="role" label="角色" width="110">
              <template #default="{ row }">
                <el-tag v-if="row.role === 'SUPER_ADMIN'" type="danger" size="small" effect="plain">超级管理员</el-tag>
                <el-tag v-else-if="row.role === 'ADMIN'" type="warning" size="small" effect="plain">管理员</el-tag>
                <el-tag v-else size="small" effect="plain">用户</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="认证状态" width="110">
              <template #default="{ row }">
                <el-tag :type="verifyStatusType(row.verifyStatus)" size="small" effect="plain">
                  {{ verifyStatusText(row.verifyStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="学号 / 学校" min-width="150">
              <template #default="{ row }">
                <span v-if="row.studentId || row.schoolName">{{ row.schoolName || '-' }} · {{ row.studentId || '-' }}</span>
                <span v-else style="color:#D1D5DB">未提交</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="plain">
                  {{ row.status === 1 ? '正常' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" min-width="420">
              <template #default="{ row }">
                <el-button v-if="row.verifyStatus === 'PENDING'" size="small" type="success" @click="handleVerify(row, 'APPROVE')">通过</el-button>
                <el-button v-if="row.verifyStatus === 'PENDING'" size="small" type="danger" plain @click="handleVerify(row, 'REJECT')">拒绝</el-button>
                <el-button size="small" :disabled="row.role === 'SUPER_ADMIN'" @click="openUserEditDialog(row)">编辑</el-button>
                <el-button size="small" type="warning" plain :disabled="row.role === 'SUPER_ADMIN'" @click="handleResetPassword(row)">重置密码</el-button>
                <el-button
                  v-if="isSuperAdmin && row.role === 'USER'"
                  size="small"
                  type="success"
                  plain
                  @click="handleSetRole(row, 'ADMIN')"
                >设为管理员</el-button>
                <el-button
                  v-if="isSuperAdmin && row.role === 'ADMIN'"
                  size="small"
                  plain
                  @click="handleSetRole(row, 'USER')"
                >取消管理员</el-button>
                <el-button
                  size="small"
                  :type="row.status === 1 ? 'danger' : 'success'"
                  :disabled="row.role === 'SUPER_ADMIN'"
                  @click="toggleUserStatus(row)"
                >{{ row.status === 1 ? '禁用' : '启用' }}</el-button>
                <el-button size="small" type="danger" plain :disabled="row.role === 'SUPER_ADMIN'" @click="handleDeleteUser(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 管理员管理：普通管理员可查看全部，仅超级管理员可操作 -->
        <div v-if="userSubTab === 'admins'" class="table-card" style="margin-top:12px">
          <div class="table-header">
            <span>共 <strong>{{ admins.length }}</strong> 个管理员</span>
            <el-button v-if="isSuperAdmin" type="primary" size="small" :icon="Plus" @click="openAdminDialog()">新增管理员</el-button>
          </div>
          <el-table :data="admins" stripe>
            <el-table-column label="ID" width="70">
              <template #default="{ $index }">{{ $index + 1 }}</template>
            </el-table-column>
            <el-table-column prop="username" label="用户名" min-width="120" />
            <el-table-column prop="nickname" label="昵称" min-width="120" />
            <el-table-column label="角色" width="110">
              <template #default="{ row }">
                <el-tag v-if="row.role === 'SUPER_ADMIN'" type="danger" size="small" effect="plain">超级管理员</el-tag>
                <el-tag v-else type="warning" size="small" effect="plain">管理员</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="plain">
                  {{ row.status === 1 ? '正常' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="创建时间" width="165">
              <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="360">
              <template #default="{ row }">
                <template v-if="isSuperAdmin && row.role !== 'SUPER_ADMIN'">
                  <el-button size="small" type="primary" plain @click="openAdminEditDialog(row)">编辑</el-button>
                  <el-button
                    v-if="row.status === 1"
                    size="small"
                    type="warning"
                    plain
                    @click="toggleAdminStatus(row)"
                  >禁用</el-button>
                  <el-button
                    v-else
                    size="small"
                    type="success"
                    @click="toggleAdminStatus(row)"
                  >启用</el-button>
                  <el-button size="small" type="danger" @click="handleDeleteAdmin(row)">删除</el-button>
                  <el-button size="small" type="warning" @click="handleRevokeAdmin(row)">取消管理员</el-button>
                </template>
                <span v-else class="text-muted">{{ isSuperAdmin ? '-' : '仅查看，无操作权限' }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 商品管理：默认全部，支持按分类/状态筛选 -->
      <el-tab-pane name="products">
        <template #label><el-icon><Goods /></el-icon> 商品管理</template>
        <div class="table-card">
          <div class="table-header">
            <span>共 <strong>{{ adminProducts.length }}</strong> 件商品</span>
            <div class="table-header-actions">
              <el-cascader
                v-model="productCategoryFilter"
                :options="categoryOptions"
                :props="{ checkStrictly: true }"
                clearable
                placeholder="按分类筛选"
                size="small"
                style="width: 200px"
                @change="loadAdminProducts"
              />
              <el-select v-model="productStatusFilter" clearable placeholder="按状态筛选" size="small" style="width: 130px" @change="loadAdminProducts">
                <el-option label="在售" value="ON_SALE" />
                <el-option label="已下架" value="OFF_SHELF" />
                <el-option label="已售出" value="SOLD" />
              </el-select>
              <el-button size="small" @click="loadAdminProducts">刷新</el-button>
              <el-button size="small" :icon="Download" @click="handleExport('products')">导出</el-button>
            </div>
          </div>
          <el-table :data="adminProducts" stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="title" label="标题" show-overflow-tooltip />
            <el-table-column prop="categoryName" label="分类" width="110" />
            <el-table-column prop="price" label="价格" width="100" />
            <el-table-column prop="sellerName" label="卖家" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="productStatusType(row.status)" effect="plain">
                  {{ productStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="190">
              <template #default="{ row }">
                <el-button
                  v-if="row.status === 'ON_SALE'"
                  size="small" type="warning" plain
                  @click="toggleProductStatus(row, 'OFF_SHELF')"
                >下架</el-button>
                <el-button
                  v-else-if="row.status === 'OFF_SHELF'"
                  size="small" type="success" plain
                  @click="toggleProductStatus(row, 'ON_SALE')"
                >上架</el-button>
                <el-button size="small" type="danger" plain @click="handleDeleteProduct(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 订单管理 -->
      <el-tab-pane name="orders">
        <template #label><el-icon><Document /></el-icon> 订单管理
          <span v-if="adminNotify.pendingArbitrations > 0" class="tab-badge-dot">{{ adminNotify.pendingArbitrations }}</span>
        </template>
        <div class="table-card">
          <div class="table-header">
            <span>共 <strong>{{ adminOrders.length }}</strong> 笔订单</span>
            <div class="table-header-actions">
              <el-select v-model="adminOrderStatusFilter" clearable placeholder="按状态筛选" size="small" style="width: 130px" @change="loadAdminOrders">
                <el-option label="待付款" value="PENDING" />
                <el-option label="已付款" value="PAID" />
                <el-option label="已发货" value="SHIPPED" />
                <el-option label="已完成" value="COMPLETED" />
                <el-option label="已取消" value="CANCELLED" />
              </el-select>
              <el-checkbox v-model="arbitrationOnly" @change="loadAdminOrders">仅看待仲裁</el-checkbox>
              <el-button size="small" @click="loadAdminOrders">刷新</el-button>
              <el-button size="small" :icon="Download" @click="handleExport('orders')">导出</el-button>
            </div>
          </div>
          <el-table :data="adminOrders" stripe>
            <el-table-column prop="orderNo" label="订单号" min-width="150" show-overflow-tooltip />
            <el-table-column prop="productTitle" label="商品" min-width="150" show-overflow-tooltip />
            <el-table-column prop="price" label="金额" width="90" />
            <el-table-column prop="buyerNickname" label="买家" width="100" />
            <el-table-column prop="sellerName" label="卖家" width="100" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="orderStatusType(row.status)" effect="plain">
                  {{ orderStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="退款状态" width="130">
              <template #default="{ row }">
                <el-tag v-if="refundStatusText(row.refundStatus)" :type="refundStatusType(row.refundStatus)" size="small" effect="plain">
                  {{ refundStatusText(row.refundStatus) }}
                </el-tag>
                <span v-else class="text-muted">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="165" />
            <el-table-column label="操作" width="260" fixed="right">
              <template #default="{ row }">
                <template v-if="row.refundStatus === 'ARBITRATION'">
                  <el-button size="small" type="danger" @click="handleArbitrate(row, true)">退款</el-button>
                  <el-button size="small" @click="handleArbitrate(row, false)">维持</el-button>
                </template>
                <template v-else>
                  <el-button
                    v-if="row.status !== 'COMPLETED' && row.status !== 'CANCELLED'"
                    size="small" type="success" plain
                    @click="forceOrderStatus(row, 'COMPLETED')"
                  >强制完成</el-button>
                  <el-button
                    v-if="row.status !== 'COMPLETED' && row.status !== 'CANCELLED'"
                    size="small" type="warning" plain
                    @click="forceOrderStatus(row, 'CANCELLED')"
                  >强制取消</el-button>
                  <el-button
                    v-if="row.status === 'COMPLETED' || row.status === 'CANCELLED'"
                    size="small" type="danger" plain
                    @click="handleDeleteAdminOrder(row.id)"
                  >删除</el-button>
                </template>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 分类管理 -->
      <el-tab-pane name="categories">
        <template #label><el-icon><Grid /></el-icon> 分类管理</template>
        <div class="table-card">
          <div class="table-header">
            <span>共 <strong>{{ filteredCategories.length }}</strong> 个分类</span>
            <div class="table-header-actions">
              <el-select v-model="categoryParentFilter" clearable placeholder="按上级分类筛选" size="small" style="width: 160px">
                <el-option v-for="m in mainCategories" :key="m.id" :label="m.name" :value="m.id" />
              </el-select>
              <el-button type="primary" size="small" :icon="Plus" @click="openCategoryDialog()">新增分类</el-button>
            </div>
          </div>
          <el-table :data="filteredCategories" stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="name" label="名称" />
            <el-table-column label="上级分类" width="140">
              <template #default="{ row }">
                <el-tag v-if="!row.parentId" size="small" type="success" effect="plain">一级分类</el-tag>
                <span v-else>{{ categoryNameMap[row.parentId] || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="sort" label="排序" width="80" />
            <el-table-column label="操作" width="160">
              <template #default="{ row }">
                <el-button size="small" @click="openCategoryDialog(row)">编辑</el-button>
                <el-button size="small" type="danger" plain @click="handleDeleteCategory(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 小黑屋 -->
      <el-tab-pane name="blacklist">
        <template #label>
          <el-icon><WarningFilled /></el-icon> 小黑屋
          <span v-if="adminNotify.blacklistCount" class="tab-badge-dot">{{ adminNotify.blacklistCount }}</span>
        </template>
        <div class="table-card">
          <div class="table-header">
            <span>共 <strong>{{ blacklistTotal }}</strong> 个受限用户</span>
            <div style="display:flex;gap:8px">
              <el-button size="small" @click="loadBlacklist">刷新</el-button>
              <el-button size="small" type="primary" @click="triggerScan" :loading="scanning">手动扫描</el-button>
            </div>
          </div>
          <el-table :data="blacklistUsers" stripe>
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="nickname" label="昵称" width="120" />
            <el-table-column label="拉黑方式" width="100">
              <template #default="{ row }">
                <el-tag :type="row.blacklistStatus === 'AUTO' ? 'warning' : 'danger'" size="small">
                  {{ row.blacklistStatus === 'AUTO' ? '系统自动' : '管理员' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="blacklistReason" label="拉黑原因" min-width="200" show-overflow-tooltip />
            <el-table-column label="解封时间" width="170">
              <template #default="{ row }">{{ formatTime(row.blacklistUntil) }}</template>
            </el-table-column>
            <el-table-column label="历史次数" width="80" prop="blacklistCount" />
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button size="small" type="success" @click="handleUnblacklist(row.id)">解封</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 投诉 / 申诉 -->
      <el-tab-pane name="reports">
        <template #label>
          <el-icon><Bell /></el-icon> 投诉 / 申诉
          <span v-if="adminNotify.pendingComplaints + adminNotify.pendingAppeals" class="tab-badge-dot">{{ adminNotify.pendingComplaints + adminNotify.pendingAppeals }}</span>
        </template>
        <el-tabs v-model="reportSubTab" type="card" style="margin-top:0">
          <el-tab-pane name="complaints" label="投诉列表" />
          <el-tab-pane name="appeals" label="申诉列表" />
        </el-tabs>

        <!-- 投诉列表 -->
        <div v-if="reportSubTab === 'complaints'" class="table-card" style="margin-top:12px">
          <el-table :data="complaints" stripe>
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="targetUserId" label="被投诉人ID" width="100" />
            <el-table-column prop="reporterId" label="投诉人ID" width="100" />
            <el-table-column prop="reason" label="原因" width="120" />
            <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="reportStatusType(row.status)" size="small">{{ reportStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160">
              <template #default="{ row }">
                <template v-if="row.status === 'PENDING'">
                  <el-button size="small" type="primary" @click="handleReport(row, 'complaint', true)">通过</el-button>
                  <el-button size="small" @click="handleReport(row, 'complaint', false)">驳回</el-button>
                </template>
                <span v-else class="text-muted">已处理</span>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 申诉列表 -->
        <div v-if="reportSubTab === 'appeals'" class="table-card" style="margin-top:12px">
          <el-table :data="appeals" stripe>
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="userId" label="申诉人ID" width="100" />
            <el-table-column prop="reason" label="申诉理由" min-width="200" show-overflow-tooltip />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="reportStatusType(row.status)" size="small">{{ reportStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160">
              <template #default="{ row }">
                <template v-if="row.status === 'PENDING'">
                  <el-button size="small" type="primary" @click="handleReport(row, 'appeal', true)">通过</el-button>
                  <el-button size="small" @click="handleReport(row, 'appeal', false)">驳回</el-button>
                </template>
                <span v-else class="text-muted">已处理</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 销售统计 -->
      <el-tab-pane name="sales">
        <template #label><el-icon><TrendCharts /></el-icon> 销售统计</template>
        <div class="sales-toolbar">
          <el-radio-group v-model="statsPeriod" @change="loadSalesStats">
            <el-radio-button value="day">日</el-radio-button>
            <el-radio-button value="week">周</el-radio-button>
            <el-radio-button value="month">月</el-radio-button>
          </el-radio-group>
          <el-date-picker
            v-model="statsRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
          />
          <el-button type="primary" :loading="statsLoading" @click="loadSalesStats">查询</el-button>
          <el-button :icon="Download" :loading="exporting" @click="handleExportSales">导出 Excel</el-button>
        </div>

        <el-row :gutter="20" v-loading="statsLoading">
          <el-col :xs="12" :sm="6">
            <div class="stat-card stat-orders">
              <div class="stat-icon-bg"><el-icon :size="24"><Document /></el-icon></div>
              <div class="stat-value">{{ statsData?.totalCount ?? 0 }}</div>
              <div class="stat-label">成交笔数</div>
            </div>
          </el-col>
          <el-col :xs="12" :sm="6">
            <div class="stat-card stat-today">
              <div class="stat-icon-bg"><el-icon :size="24"><Money /></el-icon></div>
              <div class="stat-value">¥{{ formatAmount(statsData?.totalAmount) }}</div>
              <div class="stat-label">成交总额</div>
            </div>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :xs="24" :lg="12">
            <div class="chart-card">
              <div class="chart-card-header"><span class="chart-title">分类销售额占比</span></div>
              <div id="salesPieBox" class="chart-box"></div>
            </div>
          </el-col>
          <el-col :xs="24" :lg="12">
            <div class="chart-card">
              <div class="chart-card-header"><span class="chart-title">卖家销量排行</span></div>
              <div id="salesBarBox" class="chart-box"></div>
            </div>
          </el-col>
        </el-row>

        <div class="table-card">
          <div class="table-header">
            <span>成交明细 共 <strong>{{ statsData?.rows?.length || 0 }}</strong> 条</span>
          </div>
          <el-table :data="pagedSalesRows" stripe>
            <el-table-column label="成交时间" width="165">
              <template #default="{ row }">{{ formatTime(row.dealTime) }}</template>
            </el-table-column>
            <el-table-column prop="orderNo" label="订单编号" min-width="150" show-overflow-tooltip />
            <el-table-column prop="productTitle" label="商品标题" min-width="160" show-overflow-tooltip />
            <el-table-column prop="categoryName" label="分类" width="110" />
            <el-table-column prop="sellerNickname" label="卖家" width="110" />
            <el-table-column prop="buyerNickname" label="买家" width="110" />
            <el-table-column label="成交额" width="100">
              <template #default="{ row }">¥{{ formatAmount(row.price) }}</template>
            </el-table-column>
          </el-table>
          <div class="sales-pager" v-if="(statsData?.rows?.length || 0) > salesPageSize">
            <el-pagination
              v-model:current-page="salesPage"
              :page-size="salesPageSize"
              :total="statsData.rows.length"
              layout="prev, pager, next"
              background
              small
            />
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
    <el-dialog v-model="categoryDialogVisible" :title="categoryForm.id ? '编辑分类' : '新增分类'" width="400px">
      <el-form :model="categoryForm" label-width="80px">
        <el-form-item label="上级分类">
          <el-select v-model="categoryForm.parentId" clearable placeholder="不选则为一级大分类" style="width: 100%">
            <el-option
              v-for="m in mainCategories"
              :key="m.id"
              :label="m.name"
              :value="m.id"
              :disabled="m.id === categoryForm.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="categoryForm.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="categoryForm.sort" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCategoryForm">保存</el-button>
      </template>
    </el-dialog>

    <!-- 新增用户弹窗 -->
    <el-dialog v-model="userDialogVisible" title="新增用户" width="420px" :close-on-click-modal="false">
      <el-form ref="userFormRef" :model="userForm" :rules="userRules" label-width="70px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" placeholder="3-20字符" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="userForm.password" type="password" show-password placeholder="6-20字符" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="userForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="userForm.role" style="width: 100%">
            <el-option label="普通用户" value="USER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="userSaving" @click="saveUser">保存</el-button>
      </template>
    </el-dialog>

    <!-- 编辑用户弹窗 -->
    <el-dialog v-model="userEditDialogVisible" title="编辑用户" width="420px" :close-on-click-modal="false">
      <el-form :model="userEditForm" label-width="70px">
        <el-form-item label="昵称">
          <el-input v-model="userEditForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="userEditForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="userEditForm.email" placeholder="请输入邮箱" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userEditDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="userSaving" @click="saveUserEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 新增管理员弹窗 -->
    <el-dialog v-model="adminDialogVisible" title="新增管理员" width="420px" :close-on-click-modal="false">
      <el-form ref="adminFormRef" :model="adminForm" :rules="adminRules" label-width="70px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="adminForm.username" placeholder="登录用户名" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="adminForm.nickname" placeholder="显示昵称" />
        </el-form-item>
      </el-form>
      <div class="contact-hint">
        <el-icon><InfoFilled /></el-icon>
        创建成功后系统将自动生成初始密码，请及时告知本人。
      </div>
      <template #footer>
        <el-button @click="adminDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="adminSaving" @click="saveAdmin">创建</el-button>
      </template>
    </el-dialog>

    <!-- 编辑管理员弹窗 -->
    <el-dialog v-model="adminEditDialogVisible" title="编辑管理员" width="420px" :close-on-click-modal="false">
      <el-form :model="adminEditForm" label-width="70px">
        <el-form-item label="昵称">
          <el-input v-model="adminEditForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adminEditDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="adminSaving" @click="saveAdminEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
// 后台管理页：集中维护仪表盘、用户、商品、订单、分类、投诉申诉和小黑屋。
import { ref, reactive, computed, onMounted, watch, nextTick, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DataAnalysis, Grid, Plus, WarningFilled, Bell, Download } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import {
  getDashboard, getAdminUsers, updateUserStatus,
  getAdminProducts, getAdminOrders, saveCategory, deleteCategory,
  getBlacklist, unblacklistUser, triggerBlacklistScan,
  getAdminComplaints, handleComplaint,
  getAdminAppeals, handleAppeal,
  getAdminList, createAdmin, updateAdmin, deleteAdmin, updateAdminStatus,
  createUser, updateUser, deleteUser, resetUserPassword, updateUserRole, verifyUsers,
  exportUsers, exportProducts, exportOrders,
  updateProductStatus, deleteAdminProduct,
  updateAdminOrderStatus, deleteAdminOrder, arbitrateOrder,
  getNotifications
} from '@/api/admin'
import { getSalesStats, exportSales } from '@/api/stats'
import { getCategoryList } from '@/api/category'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const isSuperAdmin = computed(() => userStore.userInfo?.role === 'SUPER_ADMIN')

const activeTab = ref('dashboard')
// 用户管理子分类：普通用户管理 / 管理员管理（仅超管可见后者）
const userSubTab = ref('normal')
const dashboard = ref(null)
const users = ref([])
const userTotal = ref(0)
const adminProducts = ref([])
const adminOrders = ref([])
const categories = ref([])

// 商品/订单/分类管理筛选状态（默认全部，筛选后实时刷新列表）
const productCategoryFilter = ref([])
const productStatusFilter = ref('')
const adminOrderStatusFilter = ref('')
const categoryParentFilter = ref(null)

const mainCategories = computed(() => categories.value.filter(c => !c.parentId))
const categoryNameMap = computed(() => Object.fromEntries(categories.value.map(c => [c.id, c.name])))
const filteredCategories = computed(() => categoryParentFilter.value == null
  ? categories.value
  : categories.value.filter(c => c.parentId === categoryParentFilter.value))
// 商品分类筛选级联选项：一级分类可直接选（含其全部子类），也可选到子类
const categoryOptions = computed(() => mainCategories.value.map(m => ({
  value: m.id,
  label: m.name,
  children: categories.value
    .filter(c => c.parentId === m.id)
    .map(s => ({ value: s.id, label: s.name }))
})))

// 用户管理：待审核筛选 + 批量审核 + 新增/编辑/删除/重置密码
const pendingFilter = ref(false)
const selectedVerifyUsers = ref([])
const userDialogVisible = ref(false)
const userEditDialogVisible = ref(false)
const userSaving = ref(false)
const userFormRef = ref(null)
const userForm = reactive({ username: '', password: '', nickname: '', role: 'USER' })
const userRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度为3-20个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度为6-20个字符', trigger: 'blur' }
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }]
}
const userEditForm = reactive({ id: null, nickname: '', phone: '', email: '' })

// 订单管理：仲裁筛选 + 强制状态 + 删除 + 仲裁处理
const arbitrationOnly = ref(false)

// 管理员管理
const admins = ref([])
const adminDialogVisible = ref(false)
const adminEditDialogVisible = ref(false)
const adminSaving = ref(false)
const adminFormRef = ref(null)
const adminForm = reactive({ username: '', nickname: '' })
const adminRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度为3-20个字符', trigger: 'blur' }
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }]
}
const adminEditForm = reactive({ id: null, nickname: '' })

// 销售统计
const statsPeriod = ref('day')
const statsRange = ref(null)
const statsData = ref(null)
const statsLoading = ref(false)
const exporting = ref(false)
const salesPage = ref(1)
const salesPageSize = 10
const pagedSalesRows = computed(() =>
  (statsData.value?.rows || []).slice((salesPage.value - 1) * salesPageSize, salesPage.value * salesPageSize)
)
let salesPieChart = null
let salesBarChart = null

const categoryDialogVisible = ref(false)
const categoryForm = reactive({ id: null, parentId: null, name: '', sort: 0 })

// 小黑屋
const blacklistUsers = ref([])
const blacklistTotal = ref(0)
const scanning = ref(false)

// 后台待处理事项计数：与顶栏角标同源，进入后台后在对应 Tab 标签上标红，指明消息在哪个模块
const adminNotify = reactive({ pendingComplaints: 0, pendingAppeals: 0, blacklistCount: 0, pendingArbitrations: 0 })
/** 加载后台通知汇总和角标数量。 */
async function loadAdminNotify() {
  try {
    const res = await getNotifications()
    Object.assign(adminNotify, res.data)
  } catch {}
}

// 投诉/申诉
const reportSubTab = ref('complaints')
const complaints = ref([])
const appeals = ref([])

let pieChart = null
let barChart = null

/** 从后端拉取仪表盘统计。 */
async function loadDashboard() {
  const res = await getDashboard()
  dashboard.value = res.data
}

/** 等待数据、渲染和图表容器就绪后刷新图表。 */
async function loadDashboardData() {
  // 先等数据：仪表盘 + 订单 + 商品列表（用于提取分类统计）
  await Promise.all([loadDashboard(), loadAdminOrders(), loadAdminProducts()])
  // 等 Vue 完成渲染（v-if + el-tabs 需要更多时间）
  await nextTick()
  // 用 setTimeout 确保 DOM 元素已挂载且有尺寸
  await new Promise(r => setTimeout(r, 100))
  renderCharts()
}

/** 初始化或刷新仪表盘图表。 */
function renderCharts() {
  const pieBox = document.getElementById('pieChartBox')
  const barBox = document.getElementById('barChartBox')

  if (pieBox) initPieChart(pieBox)
  if (barBox) initBarChart(barBox)
}

/** 渲染分类商品数量饼图。 */
/** 渲染分类商品数量饼图。 */
function initPieChart(dom) {
  if (pieChart) pieChart.dispose()
  if (!dom || dom.offsetParent === null) {
    console.warn('pieChartBox not visible, retrying...')
    setTimeout(() => renderCharts(), 200)
    return
  }

  // 从商品列表中提取各分类数量（无需额外后端接口）
  const catMap = {}
  for (const p of (adminProducts.value || [])) {
    const name = p.categoryName || '未分类'
    catMap[name] = (catMap[name] || 0) + 1
  }
  const data = Object.entries(catMap).map(([name, value]) => ({ name, value }))

  pieChart = echarts.init(dom)
  if (data.length === 0) {
    pieChart.setOption({
      title: { text: '暂无商品数据', left: 'center', top: 'center', textStyle: { color: '#9CA3AF', fontSize: 14 } }
    })
    return
  }

  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} 件 ({d}%)' },
    legend: { bottom: 0 },
    color: ['#10B981', '#059669', '#34D399', '#6EE7B7', '#A7F3D0', '#D1FAE5'],
    series: [{
      type: 'pie',
      radius: ['45%', '75%'],
      center: ['50%', '45%'],
      roseType: 'area',
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 3 },
      label: { formatter: '{b}\n{d}%' },
      data
    }]
  })
}

/** 渲染核心业务数据柱状图。 */
/** 渲染核心业务数据柱状图。 */
function initBarChart(dom) {
  if (barChart) barChart.dispose()
  if (!dom || dom.offsetParent === null) {
    console.warn('barChartBox not visible, retrying...')
    setTimeout(() => renderCharts(), 200)
    return
  }

  barChart = echarts.init(dom)
  const orderStatusMap = { 'PENDING': '待付款', 'PAID': '已付款', 'SHIPPED': '已发货', 'COMPLETED': '已完成', 'CANCELLED': '已取消' }
  const colorMap = { 'PENDING': '#F59E0B', 'PAID': '#3B82F6', 'SHIPPED': '#8B5CF6', 'COMPLETED': '#10B981', 'CANCELLED': '#9CA3AF' }
  const statusCount = {}
  for (const o of (adminOrders.value || [])) {
    const s = o.status || 'PENDING'
    statusCount[s] = (statusCount[s] || 0) + 1
  }
  const data = Object.entries(statusCount).map(([k, v]) => ({ name: orderStatusMap[k] || k, value: v, itemStyle: { color: colorMap[k] || '#6B7280' } }))

  if (data.length === 0) {
    barChart.setOption({
      title: { text: '暂无订单数据', left: 'center', top: 'center', textStyle: { color: '#9CA3AF', fontSize: 14 } }
    })
    return
  }

  barChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 40, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: data.map(d => d.name), axisLabel: { fontSize: 12 } },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      type: 'bar',
      data: data.map(d => ({ value: d.value, itemStyle: d.itemStyle })),
      barMaxWidth: 48,
      itemStyle: { borderRadius: [6, 6, 0, 0] }
    }]
  })
}

/** 窗口尺寸变化时同步调整图表。 */
function handleResize() {
  pieChart?.resize()
  barChart?.resize()
  salesPieChart?.resize()
  salesBarChart?.resize()
}

/** 分页加载普通用户列表。 */
async function loadUsers() {
  const res = await getAdminUsers({ pageNum: 1, pageSize: 50, verifyStatus: pendingFilter.value ? 'PENDING' : undefined })
  users.value = res.data.records
  userTotal.value = res.data.total
}

/** 切换只看待审核用户的过滤条件。 */
function togglePendingFilter() {
  pendingFilter.value = !pendingFilter.value
  loadUsers()
}

/** 认证状态转中文标签。 */
function verifyStatusText(s) {
  const map = { PENDING: '待审核', APPROVED: '已认证', REJECTED: '已拒绝' }
  return map[s] || '未提交'
}
/** 认证状态转标签颜色类型。 */
function verifyStatusType(s) {
  const map = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }
  return map[s] || 'info'
}

/** 批量通过或拒绝校园认证。 */
async function handleBatchVerify(action) {
  const ids = selectedVerifyUsers.value.map(u => u.id)
  const tip = action === 'APPROVE' ? '通过' : '拒绝'
  await ElMessageBox.confirm(`确认${tip}选中的 ${ids.length} 个用户的认证申请？`, '批量审核', { type: 'warning' })
  await verifyUsers(ids, action)
  ElMessage.success(`已批量${tip}`)
  loadUsers()
}

/** 审核单个用户的校园认证。 */
async function handleVerify(row, action) {
  const tip = action === 'APPROVE' ? '通过' : '拒绝'
  const info = row.studentId || row.schoolName ? `（${row.schoolName || '-'} · ${row.studentId || '-'}）` : ''
  await ElMessageBox.confirm(`确认${tip}用户「${row.username}」的认证申请${info}？`, '认证审核', { type: 'warning' })
  await verifyUsers([row.id], action)
  ElMessage.success(`已${tip}`)
  loadUsers()
}

/** 打开后台创建用户弹窗。 */
function openUserDialog() {
  Object.assign(userForm, { username: '', password: '', nickname: '', role: 'USER' })
  userDialogVisible.value = true
}

/** 保存后台新建用户。 */
async function saveUser() {
  await userFormRef.value.validate()
  userSaving.value = true
  try {
    await createUser(userForm)
    ElMessage.success('用户创建成功')
    userDialogVisible.value = false
    loadUsers()
  } finally { userSaving.value = false }
}

/** 打开用户资料编辑弹窗。 */
function openUserEditDialog(row) {
  Object.assign(userEditForm, { id: row.id, nickname: row.nickname || '', phone: row.phone || '', email: row.email || '' })
  userEditDialogVisible.value = true
}

/** 保存用户资料修改。 */
async function saveUserEdit() {
  userSaving.value = true
  try {
    await updateUser(userEditForm.id, {
      nickname: userEditForm.nickname,
      phone: userEditForm.phone,
      email: userEditForm.email
    })
    ElMessage.success('保存成功')
    userEditDialogVisible.value = false
    loadUsers()
  } finally { userSaving.value = false }
}

/** 重置用户密码并提示新密码。 */
async function handleResetPassword(row) {
  let newPassword = ''
  try {
    const { value } = await ElMessageBox.prompt(
      `为用户「${row.username}」设置新密码；留空则重置为默认密码 123456`,
      '重置密码',
      {
        confirmButtonText: '确认重置',
        cancelButtonText: '取消',
        inputPlaceholder: '自定义新密码（6-20位，可留空）',
        inputValidator: v => !v || (v.length >= 6 && v.length <= 20) || '密码长度需为6-20位'
      }
    )
    newPassword = value || ''
  } catch { return }
  const res = await resetUserPassword(row.id, newPassword)
  ElMessageBox.alert(
    `用户「${row.username}」的密码已重置为：${res.data}，请及时告知该用户。`,
    '重置成功',
    { confirmButtonText: '知道了', type: 'success' }
  )
}

/** 调整用户角色为普通用户或管理员。 */
async function handleSetRole(row, role) {
  const tip = role === 'ADMIN' ? '设为管理员' : '取消管理员权限'
  await ElMessageBox.confirm(`确认将用户「${row.username}」${tip}？`, '角色调整', { type: 'warning' })
  await updateUserRole(row.id, role)
  ElMessage.success(role === 'ADMIN' ? '已设为管理员' : '已取消管理员权限')
  loadUsers()
}

/** 删除用户并级联清理关联数据。 */
async function handleDeleteUser(row) {
  await ElMessageBox.confirm(`确认删除用户「${row.username}」？其商品将下架，未完成订单将取消。`, '删除用户', { type: 'warning' })
  await deleteUser(row.id)
  ElMessage.success('删除成功')
  loadUsers()
}

/** 分页加载后台商品列表。 */
async function loadAdminProducts() {
  // 分类筛选依赖分类数据，首次进入商品管理时自动加载
  if (!categories.value.length) loadCategories()
  const sel = productCategoryFilter.value || []
  const res = await getAdminProducts({
    pageNum: 1,
    pageSize: 50,
    parentCategoryId: sel.length === 1 ? sel[0] : undefined,
    categoryId: sel.length === 2 ? sel[1] : undefined,
    status: productStatusFilter.value || undefined
  })
  adminProducts.value = res.data.records
}

/** 分页加载后台订单列表。 */
async function loadAdminOrders() {
  const res = await getAdminOrders({
    pageNum: 1,
    pageSize: 50,
    status: adminOrderStatusFilter.value || undefined,
    refundStatus: arbitrationOnly.value ? 'ARBITRATION' : undefined
  })
  adminOrders.value = res.data.records
}

/** 加载分类树供筛选和编辑使用。 */
async function loadCategories() {
  const res = await getCategoryList()
  categories.value = res.data
}

/** 启用或禁用用户。 */
async function toggleUserStatus(row) {
  const newStatus = row.status === 1 ? 0 : 1
  await updateUserStatus(row.id, newStatus)
  ElMessage.success('操作成功')
  loadUsers()
}

/** 上架或下架后台商品。 */
async function toggleProductStatus(row, status) {
  await updateProductStatus(row.id, status)
  ElMessage.success(status === 'ON_SALE' ? '已上架' : '已下架')
  loadAdminProducts()
}

/** 删除后台商品。 */
/** 删除后台商品。 */
async function handleDeleteProduct(id) {
  await ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' })
  await deleteAdminProduct(id)
  ElMessage.success('删除成功')
  loadAdminProducts()
}

// === 订单管理：强制状态/删除/仲裁 ===
/** 退款状态转中文标签。 */
function refundStatusText(s) {
  const map = {
    REQUESTED: '退款待处理', SELLER_AGREED: '卖家已同意', SELLER_REJECTED: '卖家已拒绝',
    ARBITRATION: '仲裁中', ARBITRATION_REFUND: '仲裁退款', ARBITRATION_MAINTAIN: '仲裁维持'
  }
  return s && s !== 'NONE' ? (map[s] || s) : ''
}
/** 退款状态转标签颜色类型。 */
function refundStatusType(s) {
  const map = {
    REQUESTED: 'warning', SELLER_AGREED: 'success', SELLER_REJECTED: 'danger',
    ARBITRATION: 'danger', ARBITRATION_REFUND: 'warning', ARBITRATION_MAINTAIN: 'info'
  }
  return map[s] || 'info'
}

/** 管理员强制修改订单状态。 */
async function forceOrderStatus(row, status) {
  const tip = status === 'COMPLETED' ? '强制完成' : '强制取消'
  await ElMessageBox.confirm(`确认${tip}订单 ${row.orderNo}？`, '管理员操作', { type: 'warning' })
  await updateAdminOrderStatus(row.id, status)
  ElMessage.success(`已${tip}`)
  loadAdminOrders()
}

/** 删除已完成或已取消订单。 */
async function handleDeleteAdminOrder(id) {
  await ElMessageBox.confirm('确认删除该订单？', '提示', { type: 'warning' })
  await deleteAdminOrder(id)
  ElMessage.success('删除成功')
  loadAdminOrders()
}

/** 处理订单退款仲裁。 */
async function handleArbitrate(row, refund) {
  const tip = refund ? '判定退款（订单取消，商品恢复在售）' : '维持交易（订单继续）'
  await ElMessageBox.confirm(`确认对订单 ${row.orderNo} ${tip}？`, '仲裁处理', { type: 'warning' })
  await arbitrateOrder(row.id, refund)
  ElMessage.success(refund ? '已判定退款' : '已维持交易')
  loadAdminOrders()
}

// === 管理员管理 ===
/** 分页加载管理员列表。 */
async function loadAdmins() {
  const res = await getAdminList({ pageNum: 1, pageSize: 50 })
  admins.value = res.data.records
}

/** 打开创建管理员弹窗。 */
function openAdminDialog() {
  Object.assign(adminForm, { username: '', nickname: '' })
  adminDialogVisible.value = true
}

/** 保存新建管理员账号。 */
async function saveAdmin() {
  await adminFormRef.value.validate()
  adminSaving.value = true
  try {
    const res = await createAdmin(adminForm)
    adminDialogVisible.value = false
    ElMessageBox.alert(`管理员创建成功，初始密码：${res.data.password}`, '初始密码', {
      confirmButtonText: '我已记录',
      type: 'success'
    })
    loadAdmins()
  } finally { adminSaving.value = false }
}

/** 打开管理员编辑弹窗。 */
function openAdminEditDialog(row) {
  Object.assign(adminEditForm, { id: row.id, nickname: row.nickname || '' })
  adminEditDialogVisible.value = true
}

/** 保存管理员资料修改。 */
async function saveAdminEdit() {
  adminSaving.value = true
  try {
    await updateAdmin(adminEditForm.id, { nickname: adminEditForm.nickname })
    ElMessage.success('保存成功')
    adminEditDialogVisible.value = false
    loadAdmins()
  } finally { adminSaving.value = false }
}

/** 启用或禁用管理员账号。 */
async function toggleAdminStatus(row) {
  const newStatus = row.status === 1 ? 0 : 1
  await updateAdminStatus(row.id, newStatus)
  ElMessage.success('操作成功')
  loadAdmins()
}

/** 删除管理员账号。 */
async function handleDeleteAdmin(row) {
  await ElMessageBox.confirm(`确认删除管理员「${row.username}」？`, '删除管理员', { type: 'warning' })
  await deleteAdmin(row.id)
  ElMessage.success('删除成功')
  loadAdmins()
}

/** 将管理员降级为普通用户。 */
async function handleRevokeAdmin(row) {
  await ElMessageBox.confirm(`确认取消「${row.username}」的管理员权限？取消后该账号将变为普通用户。`, '取消管理员', { type: 'warning' })
  await updateUserRole(row.id, 'USER')
  ElMessage.success('已取消管理员权限')
  loadAdmins()
}

// === 销售统计 ===
/** 格式化金额展示。 */
function formatAmount(v) {
  return Number(v || 0).toFixed(2)
}

/** 按统计周期和时间范围加载销售数据。 */
async function loadSalesStats() {
  statsLoading.value = true
  try {
    const params = { period: statsPeriod.value }
    if (statsRange.value?.length === 2) {
      params.startDate = statsRange.value[0]
      params.endDate = statsRange.value[1]
    }
    const res = await getSalesStats(params)
    statsData.value = res.data
    salesPage.value = 1
    await nextTick()
    await new Promise(r => setTimeout(r, 100))
    renderSalesCharts()
  } finally { statsLoading.value = false }
}

/** 初始化或刷新销售统计图表。 */
function renderSalesCharts() {
  const pieBox = document.getElementById('salesPieBox')
  const barBox = document.getElementById('salesBarBox')
  if (!pieBox || !barBox || pieBox.offsetParent === null) {
    setTimeout(renderSalesCharts, 200)
    return
  }
  initSalesPieChart(pieBox)
  initSalesBarChart(barBox)
}

/** 渲染销售汇总饼图。 */
function initSalesPieChart(dom) {
  if (salesPieChart) salesPieChart.dispose()
  const byCategory = statsData.value?.byCategory || {}
  const data = Object.entries(byCategory).map(([name, value]) => ({ name, value: Number(value) }))

  salesPieChart = echarts.init(dom)
  if (data.length === 0) {
    salesPieChart.setOption({
      title: { text: '暂无成交数据', left: 'center', top: 'center', textStyle: { color: '#9CA3AF', fontSize: 14 } }
    })
    return
  }
  salesPieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
    legend: { bottom: 0 },
    color: ['#10B981', '#059669', '#34D399', '#6EE7B7', '#F59E0B', '#3B82F6', '#8B5CF6', '#A7F3D0'],
    series: [{
      type: 'pie',
      radius: ['45%', '75%'],
      center: ['50%', '45%'],
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 3 },
      label: { formatter: '{b}\n{d}%' },
      data
    }]
  })
}

/** 渲染销售明细柱状图。 */
function initSalesBarChart(dom) {
  if (salesBarChart) salesBarChart.dispose()
  const bySeller = statsData.value?.bySeller || {}
  const data = Object.entries(bySeller)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 10)

  salesBarChart = echarts.init(dom)
  if (data.length === 0) {
    salesBarChart.setOption({
      title: { text: '暂无成交数据', left: 'center', top: 'center', textStyle: { color: '#9CA3AF', fontSize: 14 } }
    })
    return
  }
  salesBarChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: '{b}: {c} 单' },
    grid: { left: 40, right: 20, top: 20, bottom: 40 },
    xAxis: { type: 'category', data: data.map(d => d[0]), axisLabel: { fontSize: 12, rotate: data.length > 5 ? 30 : 0 } },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      type: 'bar',
      data: data.map(d => d[1]),
      barMaxWidth: 48,
      itemStyle: { color: '#10B981', borderRadius: [6, 6, 0, 0] }
    }]
  })
}

// 用户/商品/订单表格导出 Excel，便于离线统计
/** 按类型导出用户、商品或订单 Excel。 */
async function handleExport(kind) {
  const conf = {
    users: [exportUsers, '用户管理'],
    products: [exportProducts, '商品管理'],
    orders: [exportOrders, '订单管理']
  }[kind]
  const blob = await conf[0]()
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${conf[1]}_${new Date().toISOString().slice(0, 10)}.xlsx`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
  ElMessage.success('导出成功')
}

/** 导出销售明细 Excel。 */
async function handleExportSales() {
  exporting.value = true
  try {
    const params = {}
    if (statsRange.value?.length === 2) {
      params.startDate = statsRange.value[0]
      params.endDate = statsRange.value[1]
    }
    const blob = await exportSales(params)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `销售统计_${new Date().toISOString().slice(0, 10)}.xlsx`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } finally { exporting.value = false }
}

/** 打开分类新增/编辑弹窗。 */
function openCategoryDialog(row) {
  if (row) {
    Object.assign(categoryForm, { id: row.id, parentId: row.parentId || null, name: row.name, sort: row.sort })
  } else {
    Object.assign(categoryForm, { id: null, parentId: null, name: '', sort: 0 })
  }
  categoryDialogVisible.value = true
}

/** 保存分类新增或修改。 */
async function saveCategoryForm() {
  if (!categoryForm.name.trim()) {
    ElMessage.warning('请输入分类名称')
    return
  }
  await saveCategory(categoryForm)
  ElMessage.success(categoryForm.parentId ? '子分类保存成功' : '一级分类保存成功')
  categoryDialogVisible.value = false
  loadCategories()
}

/** 删除空分类。 */
async function handleDeleteCategory(id) {
  await ElMessageBox.confirm('确认删除该分类？', '提示', { type: 'warning' })
  await deleteCategory(id)
  ElMessage.success('删除成功')
  loadCategories()
}

/* ---- 状态映射 ---- */
/** 商品状态转中文标签。 */
function productStatusText(s) {
  const map = { ON_SALE: '在售', SOLD: '已售', OFF_SHELF: '已下架' }
  return map[s] || s
}
/** 商品状态转标签颜色类型。 */
function productStatusType(s) {
  const map = { ON_SALE: 'success', SOLD: 'info', OFF_SHELF: 'danger' }
  return map[s] || 'info'
}
/** 订单状态转中文标签。 */
function orderStatusText(s) {
  const map = { PENDING: '待付款', PAID: '已付款', SHIPPED: '已发货', COMPLETED: '已完成', CANCELLED: '已取消' }
  return map[s] || s
}
/** 订单状态转标签颜色类型。 */
function orderStatusType(s) {
  const map = { PENDING: 'warning', PAID: 'primary', SHIPPED: 'success', COMPLETED: 'success', CANCELLED: 'info' }
  return map[s] || 'info'
}

// === 小黑屋 ===
/** 格式化时间展示。 */
function formatTime(t) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 16)
}

/** 分页加载小黑屋用户。 */
async function loadBlacklist() {
  const res = await getBlacklist({ pageNum: 1, pageSize: 50 })
  blacklistUsers.value = res.data.records
  blacklistTotal.value = res.data.total
}

/** 手动触发违规自动扫描。 */
async function triggerScan() {
  scanning.value = true
  try {
    await triggerBlacklistScan()
    ElMessage.success('扫描完成')
    loadBlacklist()
  } finally { scanning.value = false }
}

/** 手动解除用户封禁。 */
async function handleUnblacklist(userId) {
  await ElMessageBox.confirm('确认解封该用户？', '解封确认', { type: 'warning' })
  await unblacklistUser(userId)
  ElMessage.success('已解封')
  loadBlacklist()
  loadAdminNotify()
}

// === 投诉/申诉 ===
/** 投诉/申诉状态转中文标签。 */
function reportStatusText(s) {
  const map = { PENDING: '待处理', RESOLVED: '已通过', DISMISSED: '已驳回', APPROVED: '已通过', REJECTED: '已驳回' }
  return map[s] || s
}
/** 投诉/申诉状态转标签颜色类型。 */
function reportStatusType(s) {
  const map = { PENDING: 'warning', RESOLVED: 'success', DISMISSED: 'info', APPROVED: 'success', REJECTED: 'info' }
  return map[s] || 'info'
}

/** 分页加载用户投诉列表。 */
async function loadComplaints() {
  const res = await getAdminComplaints({ pageNum: 1, pageSize: 50 })
  complaints.value = res.data.records
}

/** 分页加载用户申诉列表。 */
async function loadAppeals() {
  const res = await getAdminAppeals({ pageNum: 1, pageSize: 50 })
  appeals.value = res.data.records
}

/** 处理投诉或申诉结果。 */
async function handleReport(row, type, approve) {
  const action = approve ? '通过' : '驳回'
  await ElMessageBox.confirm(`确认${action}此${type === 'complaint' ? '投诉' : '申诉'}？`, `${action}确认`, { type: approve ? 'primary' : 'warning' })
  if (type === 'complaint') {
    await handleComplaint(row.id, { approve, handlerNote: '' })
    loadComplaints()
    loadBlacklist()
  } else {
    await handleAppeal(row.id, { approve, handlerNote: '' })
    loadAppeals()
    loadBlacklist()
  }
  loadAdminNotify()
  ElMessage.success(`${action}成功`)
}

watch(activeTab, (tab) => {
  if (tab === 'dashboard') loadDashboardData()
  if (tab === 'users') loadUserTabData()
  if (tab === 'products') loadAdminProducts()
  if (tab === 'orders') loadAdminOrders()
  if (tab === 'categories') loadCategories()
  if (tab === 'blacklist') { loadBlacklist(); loadAdminNotify() }
  if (tab === 'reports') { loadComplaints(); loadAppeals(); loadAdminNotify() }
  if (tab === 'sales') loadSalesStats()
})

watch(userSubTab, () => {
  if (activeTab.value === 'users') loadUserTabData()
})

/** 按用户管理当前子分类加载对应数据 */
/** 根据当前后台页签加载对应数据。 */
function loadUserTabData() {
  if (userSubTab.value === 'admins') loadAdmins()
  else loadUsers()
}

onMounted(() => {
  let tab = sessionStorage.getItem('adminTab')
  if (tab) {
    // 管理员管理已并入用户管理子分类，兼容旧标记；watch 会按子分类加载数据
    if (tab === 'admins') { tab = 'users'; userSubTab.value = 'admins' }
    // 从顶栏铃铛“新仲裁”进入：直达订单管理并勾选“仅看待仲裁”
    if (tab === 'ordersArb') { tab = 'orders'; arbitrationOnly.value = true }
    activeTab.value = tab
    sessionStorage.removeItem('adminTab')
  }
  if (activeTab.value === 'dashboard') loadDashboardData()
  else if (activeTab.value === 'blacklist') loadBlacklist()
  else if (activeTab.value === 'reports') { loadComplaints(); loadAppeals() }
  else if (activeTab.value === 'sales') loadSalesStats()
  // 顶栏角标的来源数据同步加载，Tab 标签上直接标红待处理模块
  loadAdminNotify()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  pieChart?.dispose()
  barChart?.dispose()
  salesPieChart?.dispose()
  salesBarChart?.dispose()
})
</script>

<style scoped>
/* ====== 页面标题 ====== */
.page-header { margin-bottom: 24px; }

/* ====== 标签页 ====== */
.admin-tabs { margin-top: 4px; }
.admin-tabs :deep(.el-tabs__item) { display: flex; align-items: center; gap: 6px; }

/* ====== 统计卡片 ====== */
.stat-card {
  position: relative;
  background: #fff;
  border-radius: 16px;
  padding: 24px 20px;
  text-align: center;
  cursor: pointer;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  border: 1px solid #F3F4F6;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
  margin-bottom: 16px;
}
.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(16,185,129,0.08);
}

.stat-icon-bg {
  display: inline-flex; align-items: center; justify-content: center;
  width: 48px; height: 48px; border-radius: 14px;
  margin-bottom: 12px;
}
.stat-users .stat-icon-bg { background: linear-gradient(135deg, #ECFDF5, #D1FAE5); color: #10B981; }
.stat-products .stat-icon-bg { background: linear-gradient(135deg, #EFF6FF, #DBEAFE); color: #3B82F6; }
.stat-orders .stat-icon-bg { background: linear-gradient(135deg, #FFFBEB, #FEF3C7); color: #F59E0B; }
.stat-today .stat-icon-bg { background: linear-gradient(135deg, #F5F3FF, #EDE9FE); color: #8B5CF6; }

.stat-value {
  font-size: 36px; font-weight: 800; color: #1F2937;
  font-variant-numeric: tabular-nums; line-height: 1.1;
}
.stat-label {
  margin-top: 6px; color: #6B7280; font-size: 13px; font-weight: 500;
}

/* ====== 图表卡片 ====== */
.chart-card {
  background: #fff; border-radius: 16px;
  padding: 20px 24px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  border: 1px solid #F3F4F6;
  margin-bottom: 16px;
}
.chart-card-header {
  display: flex; align-items: center; gap: 10px;
  margin-bottom: 12px;
}
.chart-title {
  font-size: 15px; font-weight: 600; color: #374151;
}
.chart-box {
  width: 100%; height: 340px; min-height: 340px;
}

/* ====== 表格卡片 ====== */
.table-card {
  background: #fff; border-radius: 16px;
  padding: 20px 24px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  border: 1px solid #F3F4F6;
}
.table-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 16px; font-size: 14px; color: #6B7280;
  flex-wrap: wrap; gap: 10px;
}
.table-header strong { color: #10B981; font-weight: 700; }
.table-header-actions { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.text-muted { color: #9CA3AF; }

/* Tab 标签待处理角标：与顶栏通知同源，指明消息在哪个模块 */
.tab-badge-dot {
  display: inline-flex; align-items: center; justify-content: center;
  min-width: 18px; height: 18px; padding: 0 5px;
  margin-left: 4px; border-radius: 9px;
  background: #EF4444; color: #fff;
  font-size: 11px; font-weight: 700;
  vertical-align: 2px;
}

/* ====== 销售统计 ====== */
.sales-toolbar {
  display: flex; align-items: center; gap: 12px; flex-wrap: wrap;
  margin-bottom: 16px;
}
.sales-pager {
  display: flex; justify-content: center;
  margin-top: 16px;
}
.contact-hint {
  display: flex; align-items: center; gap: 8px;
  padding: 12px 14px; margin-top: 4px;
  background: #F0FDF4; border-radius: 10px;
  font-size: 13px; color: #059669;
}

/* ====== 响应式 ====== */
@media (max-width: 768px) {
  .stat-card { padding: 16px 12px; }
  .stat-value { font-size: 26px; }
  .stat-icon-bg { width: 40px; height: 40px; border-radius: 10px; }
  .chart-box { height: 260px; min-height: 260px; }
  .table-card { padding: 12px 10px; }
}
/* 手机端：统计卡每行2个，表格横向滚动 */
@media (max-width: 480px) {
  .stat-card { padding: 14px 10px; border-radius: 12px; }
  .stat-value { font-size: 22px; }
  .stat-icon-bg { width: 36px; height: 36px; border-radius: 8px; margin-bottom: 8px; }
  .stat-icon-bg .el-icon { font-size: 18px; }
  .chart-box { height: 220px; min-height: 220px; }
  .chart-card { padding: 14px 16px; }
  .table-card {
    padding: 12px 8px; border-radius: 12px;
    overflow-x: auto;
  }
}
</style>
