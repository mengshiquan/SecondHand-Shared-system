# 第一批功能增强实施计划（交易基础补全）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为校园二手交易平台补全交易闭环：校园身份认证、地址管理、购物车、订单退款流程、超级管理员体系、销售统计导出、各模块 CRUD 补全及配套前端页面。

**Architecture:** 沿用现有 Spring Boot 3.3 + MyBatis-Plus 分层（Controller → Service → Mapper），新模块按同样模式落地；退款流程作为订单的独立维度（refund_status）不侵入主状态机；角色体系扩展为 USER/ADMIN/SUPER_ADMIN，通过 UserContext 统一判断。

**Tech Stack:** Spring Boot 3.3.5、MyBatis-Plus 3.5.9、MySQL、Redis、EasyExcel 3.3.x（新增，用于导出）、Vue 3 + Element Plus + ECharts。

**设计文档:** `docs/superpowers/specs/2026-08-28-batch1-transaction-foundation-design.md`

**验证方式说明:** 本项目无单元测试基建，每个任务以 `mvn compile` 通过为准；全部后端完成后统一启动应用做接口冒烟验证（Task 9）。

**后端环境:** 编译前设置 `JAVA_HOME=D:\java\jdk17`，编译命令在 `backend/` 目录下执行 `mvn compile -q`。

---

### Task 0: 数据库迁移脚本

**Files:**
- Create: `sql/migration_batch1.sql`
- Modify: `sql/schema.sql`（同步更新建表语句，保持新库可一键初始化）

- [x] **Step 1: 创建迁移脚本**

```sql
-- 第一批功能增强迁移脚本
USE secondhand_db;

-- 收货地址表
CREATE TABLE IF NOT EXISTS t_address (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '地址ID',
    user_id       BIGINT       NOT NULL COMMENT '所属用户ID',
    receiver_name VARCHAR(50)  NOT NULL COMMENT '收货人姓名',
    phone         VARCHAR(20)  NOT NULL COMMENT '手机号',
    address       VARCHAR(255) NOT NULL COMMENT '详细地址',
    is_default    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否默认 0-否 1-是',
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货地址表';

-- 购物车表
CREATE TABLE IF NOT EXISTS t_cart (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '购物车项ID',
    user_id     BIGINT   NOT NULL COMMENT '用户ID',
    product_id  BIGINT   NOT NULL COMMENT '商品ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_user_product (user_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

-- 用户表：校园认证字段
ALTER TABLE t_user ADD COLUMN student_id    VARCHAR(20)  DEFAULT NULL COMMENT '学号';
ALTER TABLE t_user ADD COLUMN school_name   VARCHAR(100) DEFAULT NULL COMMENT '学校名称';
ALTER TABLE t_user ADD COLUMN verify_status VARCHAR(20)  DEFAULT NULL COMMENT '认证状态 PENDING/APPROVED/REJECTED';
-- 存量用户默认已认证
UPDATE t_user SET verify_status = 'APPROVED' WHERE verify_status IS NULL;
-- admin 账号升级为超级管理员
UPDATE t_user SET role = 'SUPER_ADMIN' WHERE username = 'admin';

-- 订单表：地址 + 退款 + 预留支付字段
ALTER TABLE t_order ADD COLUMN address_id    BIGINT       DEFAULT NULL COMMENT '收货地址ID';
ALTER TABLE t_order ADD COLUMN refund_status VARCHAR(20)  DEFAULT NULL COMMENT '退款状态';
ALTER TABLE t_order ADD COLUMN refund_reason VARCHAR(255) DEFAULT NULL COMMENT '退款原因';
ALTER TABLE t_order ADD COLUMN refund_time   DATETIME     DEFAULT NULL COMMENT '退款申请时间';
ALTER TABLE t_order ADD COLUMN payment_time  DATETIME     DEFAULT NULL COMMENT '付款时间（预留模拟支付）';
```

- [x] **Step 2: 同步更新 `sql/schema.sql`**

在 `t_user` 建表语句的 `deleted` 字段前追加三列（与迁移脚本一致）；`t_order` 建表语句追加五列；文件末尾追加 `t_address` 和 `t_cart` 两张表的 CREATE 语句；初始化数据处将 `('admin', ..., 'ADMIN', 1)` 改为 `'SUPER_ADMIN'`。

- [x] **Step 3: 执行迁移并验证**

```bash
mysql -uroot -p secondhand_db < sql/migration_batch1.sql
mysql -uroot -p -e "SHOW COLUMNS FROM t_user LIKE '%student%'; SELECT username,role FROM secondhand_db.t_user WHERE username='admin';"
```

预期：输出 `student_id` 列；admin 的 role 为 `SUPER_ADMIN`。

- [x] **Step 4: Commit**

```bash
git add sql/migration_batch1.sql sql/schema.sql
git commit -m "feat(db): batch1 migration - address, cart, verify, refund columns"
```

---

### Task 1: 校园身份认证（后端）

**Files:**
- Modify: `backend/src/main/java/com/campus/secondhand/entity/User.java`
- Modify: `backend/src/main/java/com/campus/secondhand/dto/RegisterDTO.java`
- Modify: `backend/src/main/java/com/campus/secondhand/service/impl/UserServiceImpl.java`
- Create: `backend/src/main/java/com/campus/secondhand/common/VerifyGuard.java`
- Modify: `backend/src/main/java/com/campus/secondhand/service/impl/ProductServiceImpl.java`
- Modify: `backend/src/main/java/com/campus/secondhand/service/impl/FavoriteServiceImpl.java`
- Modify: `backend/src/main/java/com/campus/secondhand/service/impl/CommentServiceImpl.java`
- Modify: `backend/src/main/java/com/campus/secondhand/service/impl/OrderServiceImpl.java`
- Modify: `backend/src/main/java/com/campus/secondhand/service/AdminService.java` + `impl/AdminServiceImpl.java`
- Modify: `backend/src/main/java/com/campus/secondhand/controller/AdminController.java`

- [ ] **Step 1: User 实体新增字段**

在 `User.java` 的 `blacklistCount` 字段后追加：

```java
    /** 学号（12位纯数字） */
    private String studentId;

    /** 学校名称 */
    private String schoolName;

    /** 认证状态：PENDING/APPROVED/REJECTED */
    private String verifyStatus;
```

- [ ] **Step 2: 认证校验工具方法**

新建 `backend/src/main/java/com/campus/secondhand/common/VerifyGuard.java`：

```java
package com.campus.secondhand.common;

import com.campus.secondhand.entity.User;
import com.campus.secondhand.service.UserService;
import com.campus.secondhand.util.UserContext;

/**
 * 校园身份认证守卫：未通过认证的用户禁止执行写操作
 */
public final class VerifyGuard {

    private VerifyGuard() {}

    public static void requireVerified(UserService userService) {
        Long userId = UserContext.getUserId();
        User user = userService.getById(userId);
        if (user == null || !"APPROVED".equals(user.getVerifyStatus())) {
            throw new BusinessException("请先完成校园身份认证");
        }
    }
}
```

- [ ] **Step 3: RegisterDTO 新增字段**

在 `RegisterDTO.java` 的 `email` 字段后追加：

```java
    @NotBlank(message = "学号不能为空")
    @Pattern(regexp = "^\\d{12}$", message = "学号格式不正确")
    private String studentId;

    @NotBlank(message = "学校名称不能为空")
    @Size(max = 100, message = "学校名称过长")
    private String schoolName;
```

并导入 `import jakarta.validation.constraints.Pattern;`。

- [ ] **Step 4: UserServiceImpl.register 写入认证字段**

将 `register` 方法中的 `user.setStatus(1); save(user);` 改为：

```java
        user.setStatus(1);
        user.setVerifyStatus("APPROVED");
        save(user);
```

（DTO 的 studentId/schoolName 已由 BeanUtils.copyProperties 拷贝）

- [ ] **Step 5: 写操作入口加认证守卫**

在以下四个 Service 的入口方法首行调用 `VerifyGuard.requireVerified(userService)`（需 `@Autowired UserService userService`）：

- `ProductServiceImpl.publish(...)`
- `FavoriteServiceImpl.toggle(...)`
- `CommentServiceImpl.add(...)`
- `OrderServiceImpl.createOrder(...)`

示例（FavoriteServiceImpl）：

```java
    @Autowired
    private UserService userService;

    @Override
    public void toggle(Long productId) {
        VerifyGuard.requireVerified(userService);
        // ... 原有逻辑
    }
```

注意：`UserServiceImpl` 内部不加守卫（避免循环依赖），`getCurrentUser`/资料修改不受限。

- [ ] **Step 6: 管理员批量审核接口**

`AdminService.java` 追加：

```java
    void verifyUsers(java.util.List<Long> userIds, String action);
```

`AdminServiceImpl.java` 追加实现：

```java
    @Override
    public void verifyUsers(List<Long> userIds, String action) {
        checkAdmin();
        if (userIds == null || userIds.isEmpty()) {
            throw new BusinessException("请选择要审核的用户");
        }
        if (!"APPROVE".equals(action) && !"REJECT".equals(action)) {
            throw new BusinessException("无效的审核动作");
        }
        String target = "APPROVE".equals(action) ? "APPROVED" : "REJECTED";
        for (Long uid : userIds) {
            User user = userService.getById(uid);
            if (user != null && "PENDING".equals(user.getVerifyStatus())) {
                user.setVerifyStatus(target);
                userService.updateById(user);
            }
        }
    }
```

`AdminController.java` 追加：

```java
    /** 批量审核身份认证 */
    @PutMapping("/users/verify")
    public Result<Void> verifyUsers(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<Long> userIds = ((List<Number>) params.get("userIds"))
                .stream().map(Number::longValue).collect(java.util.stream.Collectors.toList());
        adminService.verifyUsers(userIds, (String) params.get("action"));
        return Result.success();
    }
```

`userPage` 支持按 `verifyStatus` 过滤：`AdminController.users` 增加参数 `@RequestParam(required = false) String verifyStatus` 并透传给 `adminService.userPage`；`AdminServiceImpl.userPage` 中在 wrapper 追加：

```java
        if (verifyStatus != null && !verifyStatus.isEmpty()) {
            wrapper.eq(User::getVerifyStatus, verifyStatus);
        }
```

- [ ] **Step 7: 编译验证**

```bash
cd backend
mvn compile -q
```

预期：无错误输出。

- [ ] **Step 8: Commit**

```bash
git add backend/src
git commit -m "feat: campus identity verification (12-digit student ID) with write-op guards"
```

---

### Task 2: 地址管理模块（后端）

**Files:**
- Create: `backend/src/main/java/com/campus/secondhand/entity/Address.java`
- Create: `backend/src/main/java/com/campus/secondhand/mapper/AddressMapper.java`
- Create: `backend/src/main/java/com/campus/secondhand/service/AddressService.java`
- Create: `backend/src/main/java/com/campus/secondhand/service/impl/AddressServiceImpl.java`
- Create: `backend/src/main/java/com/campus/secondhand/controller/AddressController.java`

- [ ] **Step 1: Address 实体**

```java
package com.campus.secondhand.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 收货地址实体
 */
@Data
@TableName("t_address")
public class Address {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 收货人姓名 */
    private String receiverName;

    /** 手机号 */
    private String phone;

    /** 详细地址 */
    private String address;

    /** 是否默认 0-否 1-是 */
    private Integer isDefault;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
```

- [ ] **Step 2: AddressMapper**

```java
package com.campus.secondhand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.secondhand.entity.Address;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AddressMapper extends BaseMapper<Address> {
}
```

- [ ] **Step 3: AddressService 接口**

```java
package com.campus.secondhand.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.secondhand.entity.Address;

import java.util.List;

public interface AddressService extends IService<Address> {

    List<Address> listMine();

    void add(Address address);

    void update(Long id, Address address);

    void delete(Long id);

    void setDefault(Long id);
}
```

- [ ] **Step 4: AddressServiceImpl**

```java
package com.campus.secondhand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.secondhand.common.BusinessException;
import com.campus.secondhand.entity.Address;
import com.campus.secondhand.mapper.AddressMapper;
import com.campus.secondhand.service.AddressService;
import com.campus.secondhand.util.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 收货地址服务实现
 */
@Service
public class AddressServiceImpl extends ServiceImpl<AddressMapper, Address> implements AddressService {

    private static final int MAX_PER_USER = 10;

    @Override
    public List<Address> listMine() {
        return list(new LambdaQueryWrapper<Address>()
                .eq(Address::getUserId, UserContext.getUserId())
                .orderByDesc(Address::getIsDefault)
                .orderByDesc(Address::getUpdateTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(Address address) {
        Long userId = UserContext.getUserId();
        long count = count(new LambdaQueryWrapper<Address>().eq(Address::getUserId, userId));
        if (count >= MAX_PER_USER) {
            throw new BusinessException("最多保存 " + MAX_PER_USER + " 个收货地址");
        }
        address.setId(null);
        address.setUserId(userId);
        // 第一个地址自动设为默认
        if (count == 0) {
            address.setIsDefault(1);
        } else if (Integer.valueOf(1).equals(address.getIsDefault())) {
            clearDefault(userId);
        } else {
            address.setIsDefault(0);
        }
        save(address);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, Address address) {
        Address exist = getOwned(id);
        exist.setReceiverName(address.getReceiverName());
        exist.setPhone(address.getPhone());
        exist.setAddress(address.getAddress());
        if (Integer.valueOf(1).equals(address.getIsDefault())
                && !Integer.valueOf(1).equals(exist.getIsDefault())) {
            clearDefault(exist.getUserId());
            exist.setIsDefault(1);
        }
        updateById(exist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Address exist = getOwned(id);
        removeById(id);
        // 删除默认地址后，自动把最近一个设为默认
        if (Integer.valueOf(1).equals(exist.getIsDefault())) {
            Address latest = getOne(new LambdaQueryWrapper<Address>()
                    .eq(Address::getUserId, exist.getUserId())
                    .orderByDesc(Address::getUpdateTime)
                    .last("LIMIT 1"));
            if (latest != null) {
                latest.setIsDefault(1);
                updateById(latest);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long id) {
        Address exist = getOwned(id);
        clearDefault(exist.getUserId());
        exist.setIsDefault(1);
        updateById(exist);
    }

    private Address getOwned(Long id) {
        Address exist = getById(id);
        if (exist == null || !exist.getUserId().equals(UserContext.getUserId())) {
            throw new BusinessException("地址不存在");
        }
        return exist;
    }

    private void clearDefault(Long userId) {
        update(new LambdaUpdateWrapper<Address>()
                .eq(Address::getUserId, userId)
                .set(Address::getIsDefault, 0));
    }
}
```

- [ ] **Step 5: AddressController**

```java
package com.campus.secondhand.controller;

import com.campus.secondhand.common.Result;
import com.campus.secondhand.entity.Address;
import com.campus.secondhand.service.AddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 收货地址接口
 */
@RestController
@RequestMapping("/address")
public class AddressController {

    @Autowired
    private AddressService addressService;

    /** 地址列表 */
    @GetMapping("/list")
    public Result<List<Address>> list() {
        return Result.success(addressService.listMine());
    }

    /** 新增地址 */
    @PostMapping
    public Result<Void> add(@RequestBody Address address) {
        addressService.add(address);
        return Result.success();
    }

    /** 修改地址 */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Address address) {
        addressService.update(id, address);
        return Result.success();
    }

    /** 删除地址 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        addressService.delete(id);
        return Result.success();
    }

    /** 设为默认 */
    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id) {
        addressService.setDefault(id);
        return Result.success();
    }
}
```

- [ ] **Step 6: 编译验证 + Commit**

```bash
mvn compile -q
git add backend/src
git commit -m "feat: address management module (CRUD + default address)"
```

---

### Task 3: 购物车模块（后端）

**Files:**
- Create: `backend/src/main/java/com/campus/secondhand/entity/Cart.java`
- Create: `backend/src/main/java/com/campus/secondhand/mapper/CartMapper.java`
- Create: `backend/src/main/java/com/campus/secondhand/service/CartService.java`
- Create: `backend/src/main/java/com/campus/secondhand/service/impl/CartServiceImpl.java`
- Create: `backend/src/main/java/com/campus/secondhand/dto/CartCheckoutDTO.java`
- Create: `backend/src/main/java/com/campus/secondhand/controller/CartController.java`
- Modify: `backend/src/main/java/com/campus/secondhand/dto/OrderDTO.java`（新增 addressId）
- Modify: `backend/src/main/java/com/campus/secondhand/service/impl/OrderServiceImpl.java`（下单关联地址）
- Modify: `backend/src/main/java/com/campus/secondhand/entity/Order.java`（新增字段）

- [ ] **Step 1: Order 实体补字段**（为购物车结算做准备）

在 `Order.java` 的 `remark` 字段后追加：

```java
    /** 收货地址ID */
    private Long addressId;

    /** 退款状态 */
    private String refundStatus;

    /** 退款原因 */
    private String refundReason;

    /** 退款申请时间 */
    private LocalDateTime refundTime;

    /** 付款时间（预留模拟支付） */
    private LocalDateTime paymentTime;
```

同时给 `payOrder` 里补付款时间：`OrderServiceImpl.payOrder` 中 `order.setStatus("PAID");` 前加 `order.setPaymentTime(LocalDateTime.now());`。

- [ ] **Step 2: Cart 实体**

```java
package com.campus.secondhand.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 购物车项实体
 */
@Data
@TableName("t_cart")
public class Cart {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long productId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
```

- [ ] **Step 3: CartMapper + CartCheckoutDTO**

`CartMapper.java`：

```java
package com.campus.secondhand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.secondhand.entity.Cart;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CartMapper extends BaseMapper<Cart> {
}
```

`CartCheckoutDTO.java`：

```java
package com.campus.secondhand.dto;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class CartCheckoutDTO {

    @NotEmpty(message = "请选择要结算的商品")
    private List<Long> cartItemIds;

    @NotNull(message = "请选择收货地址")
    private Long addressId;
}
```

- [ ] **Step 4: CartService 接口**

```java
package com.campus.secondhand.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.secondhand.dto.CartCheckoutDTO;
import com.campus.secondhand.entity.Cart;
import com.campus.secondhand.vo.CartItemVO;
import com.campus.secondhand.vo.OrderVO;

import java.util.List;

public interface CartService extends IService<Cart> {

    List<CartItemVO> listMine();

    void addToCart(Long productId);

    void remove(Long id);

    void removeBatch(List<Long> ids);

    void clear();

    List<OrderVO> checkout(CartCheckoutDTO dto);
}
```

- [ ] **Step 5: CartItemVO**

新建 `backend/src/main/java/com/campus/secondhand/vo/CartItemVO.java`：

```java
package com.campus.secondhand.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购物车列表项（含商品摘要与失效标记）
 */
@Data
public class CartItemVO {

    private Long id;

    private Long productId;

    private String title;

    private BigDecimal price;

    private java.util.List<String> images;

    private String categoryName;

    private String sellerNickname;

    /** 是否失效（商品已下架/已售出） */
    private Boolean invalid;

    private LocalDateTime createTime;
}
```

- [ ] **Step 6: CartServiceImpl**

```java
package com.campus.secondhand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.secondhand.common.BusinessException;
import com.campus.secondhand.common.VerifyGuard;
import com.campus.secondhand.dto.CartCheckoutDTO;
import com.campus.secondhand.dto.OrderDTO;
import com.campus.secondhand.entity.Address;
import com.campus.secondhand.entity.Cart;
import com.campus.secondhand.entity.Product;
import com.campus.secondhand.mapper.CartMapper;
import com.campus.secondhand.service.AddressService;
import com.campus.secondhand.service.CartService;
import com.campus.secondhand.service.OrderService;
import com.campus.secondhand.service.ProductService;
import com.campus.secondhand.service.UserService;
import com.campus.secondhand.util.UserContext;
import com.campus.secondhand.vo.CartItemVO;
import com.campus.secondhand.vo.OrderVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 购物车服务实现
 */
@Service
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart> implements CartService {

    @Autowired
    private ProductService productService;
    @Autowired
    private UserService userService;
    @Autowired
    private AddressService addressService;
    @Autowired
    private OrderService orderService;

    @Override
    public List<CartItemVO> listMine() {
        Long userId = UserContext.getUserId();
        List<Cart> items = list(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .orderByDesc(Cart::getCreateTime));
        List<CartItemVO> result = new ArrayList<>();
        for (Cart item : items) {
            Product p = productService.getById(item.getProductId());
            CartItemVO vo = new CartItemVO();
            vo.setId(item.getId());
            vo.setProductId(item.getProductId());
            vo.setCreateTime(item.getCreateTime());
            if (p == null) {
                vo.setTitle("商品已删除");
                vo.setInvalid(true);
            } else {
                vo.setTitle(p.getTitle());
                vo.setPrice(p.getPrice());
                vo.setImages(parseImages(p.getImages()));
                vo.setInvalid(!"ON_SALE".equals(p.getStatus()));
            }
            result.add(vo);
        }
        return result;
    }

    /** 解析商品图片 JSON 数组，失败时返回空列表 */
    private List<String> parseImages(String imagesJson) {
        if (imagesJson == null || imagesJson.isBlank()) return new ArrayList<>();
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(imagesJson, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Override
    public void addToCart(Long productId) {
        VerifyGuard.requireVerified(userService);
        Long userId = UserContext.getUserId();
        Product product = productService.getById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (!"ON_SALE".equals(product.getStatus())) {
            throw new BusinessException("商品不在售，无法加入购物车");
        }
        if (product.getUserId().equals(userId)) {
            throw new BusinessException("不能将自己发布的商品加入购物车");
        }
        long exists = count(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .eq(Cart::getProductId, productId));
        if (exists > 0) {
            throw new BusinessException("该商品已在购物车中");
        }
        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setProductId(productId);
        save(cart);
    }

    @Override
    public void remove(Long id) {
        Cart cart = getOwned(id);
        removeById(cart.getId());
    }

    @Override
    public void removeBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        for (Long id : ids) {
            remove(id);
        }
    }

    @Override
    public void clear() {
        Long userId = UserContext.getUserId();
        remove(new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<OrderVO> checkout(CartCheckoutDTO dto) {
        VerifyGuard.requireVerified(userService);
        Long userId = UserContext.getUserId();

        Address address = addressService.getById(dto.getAddressId());
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException("收货地址不存在");
        }

        List<OrderVO> orders = new ArrayList<>();
        for (Long cartItemId : dto.getCartItemIds()) {
            Cart item = getOwned(cartItemId);
            OrderDTO orderDTO = new OrderDTO();
            orderDTO.setProductId(item.getProductId());
            orderDTO.setAddressId(dto.getAddressId());
            OrderVO vo = orderService.createOrder(orderDTO);
            removeById(item.getId());
            orders.add(vo);
        }
        return orders;
    }

    private Cart getOwned(Long id) {
        Cart cart = getById(id);
        if (cart == null || !cart.getUserId().equals(UserContext.getUserId())) {
            throw new BusinessException("购物车项不存在");
        }
        return cart;
    }
}
```

- [ ] **Step 7: OrderDTO 新增 addressId**

在 `OrderDTO.java` 追加：

```java
    /** 收货地址ID（购物车结算时必传） */
    private Long addressId;
```

`OrderServiceImpl.doCreateOrder` 中 `order.setRemark(dto.getRemark());` 后追加地址关联：

```java
        order.setAddressId(dto.getAddressId());
        if (dto.getAddressId() != null) {
            com.campus.secondhand.entity.Address address = addressService.getById(dto.getAddressId());
            if (address != null && address.getUserId().equals(buyerId)) {
                order.setBuyerName(address.getReceiverName());
                order.setBuyerPhone(address.getPhone());
                order.setBuyerAddress(address.getAddress());
            }
        }
```

并在 `OrderServiceImpl` 中注入 `@Autowired private AddressService addressService;`。

- [ ] **Step 8: CartController**

```java
package com.campus.secondhand.controller;

import com.campus.secondhand.common.Result;
import com.campus.secondhand.dto.CartCheckoutDTO;
import com.campus.secondhand.service.CartService;
import com.campus.secondhand.vo.CartItemVO;
import com.campus.secondhand.vo.OrderVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 购物车接口
 */
@RestController
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    /** 购物车列表 */
    @GetMapping("/list")
    public Result<List<CartItemVO>> list() {
        return Result.success(cartService.listMine());
    }

    /** 加入购物车 */
    @PostMapping("/{productId}")
    public Result<Void> add(@PathVariable Long productId) {
        cartService.addToCart(productId);
        return Result.success();
    }

    /** 移除 */
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        cartService.remove(id);
        return Result.success();
    }

    /** 批量移除 */
    @DeleteMapping("/batch")
    public Result<Void> removeBatch(@RequestBody Map<String, List<Long>> params) {
        cartService.removeBatch(params.get("ids"));
        return Result.success();
    }

    /** 清空 */
    @DeleteMapping("/clear")
    public Result<Void> clear() {
        cartService.clear();
        return Result.success();
    }

    /** 结算 */
    @PostMapping("/checkout")
    public Result<List<OrderVO>> checkout(@Validated @RequestBody CartCheckoutDTO dto) {
        return Result.success(cartService.checkout(dto));
    }
}
```

- [ ] **Step 9: 编译验证 + Commit**

```bash
mvn compile -q
git add backend/src
git commit -m "feat: shopping cart module with single/batch checkout"
```

---

### Task 4: 订单增强 — 退款流程 + 取消 + 删除（后端）

**Files:**
- Modify: `backend/src/main/java/com/campus/secondhand/service/OrderService.java`
- Modify: `backend/src/main/java/com/campus/secondhand/service/impl/OrderServiceImpl.java`
- Modify: `backend/src/main/java/com/campus/secondhand/controller/OrderController.java`
- Modify: `backend/src/main/java/com/campus/secondhand/service/AdminService.java` + `impl/AdminServiceImpl.java`
- Modify: `backend/src/main/java/com/campus/secondhand/controller/AdminController.java`

- [ ] **Step 1: OrderService 接口新增方法**

```java
    void cancelOrder(Long id);

    void applyRefund(Long id, String reason);

    void handleRefund(Long id, boolean agree);

    void applyArbitration(Long id);

    void adminArbitrate(Long id, boolean refund);

    void updateAddress(Long id, Long addressId);

    void deleteOrder(Long id);
```

- [ ] **Step 2: OrderServiceImpl 退款相关实现**

在 `OrderServiceImpl` 中追加（置于 `detail` 方法之后）：

```java
    @Autowired
    private AddressService addressService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long id) {
        Order order = getById(id);
        if (order == null) throw new BusinessException("订单不存在");
        if (!"PENDING".equals(order.getStatus())) throw new BusinessException("仅待付款订单可取消");
        if (!order.getBuyerId().equals(UserContext.getUserId())) throw new BusinessException("仅买家可取消");
        order.setStatus("CANCELLED");
        updateById(order);
        restoreProduct(order.getProductId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyRefund(Long id, String reason) {
        Order order = getById(id);
        if (order == null) throw new BusinessException("订单不存在");
        if (!order.getBuyerId().equals(UserContext.getUserId())) throw new BusinessException("仅买家可申请退款");
        if (!"PAID".equals(order.getStatus()) && !"SHIPPED".equals(order.getStatus())) {
            throw new BusinessException("当前订单状态不支持退款");
        }
        if (order.getRefundStatus() != null && !"NONE".equals(order.getRefundStatus())) {
            throw new BusinessException("退款流程已在进行中");
        }
        order.setRefundStatus("REQUESTED");
        order.setRefundReason(reason);
        order.setRefundTime(LocalDateTime.now());
        updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleRefund(Long id, boolean agree) {
        Order order = getById(id);
        if (order == null) throw new BusinessException("订单不存在");
        if (!order.getSellerId().equals(UserContext.getUserId())) throw new BusinessException("仅卖家可处理退款");
        if (!"REQUESTED".equals(order.getRefundStatus())) throw new BusinessException("无待处理的退款申请");
        if (agree) {
            order.setRefundStatus("SELLER_AGREED");
            order.setStatus("CANCELLED");
            updateById(order);
            restoreProduct(order.getProductId());
        } else {
            order.setRefundStatus("SELLER_REJECTED");
            updateById(order);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyArbitration(Long id) {
        Order order = getById(id);
        if (order == null) throw new BusinessException("订单不存在");
        if (!order.getBuyerId().equals(UserContext.getUserId())) throw new BusinessException("仅买家可申请仲裁");
        if (!"SELLER_REJECTED".equals(order.getRefundStatus())) throw new BusinessException("仅卖家拒绝后可申请仲裁");
        // 72小时内有效（以退款申请时间起算）
        if (order.getRefundTime() != null
                && order.getRefundTime().plusHours(72).isBefore(LocalDateTime.now())) {
            throw new BusinessException("仲裁申请已超过72小时时限");
        }
        order.setRefundStatus("ARBITRATION");
        updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminArbitrate(Long id, boolean refund) {
        Order order = getById(id);
        if (order == null) throw new BusinessException("订单不存在");
        if (!"ARBITRATION".equals(order.getRefundStatus())) throw new BusinessException("无待仲裁的退款申请");
        if (refund) {
            order.setRefundStatus("ARBITRATION_REFUND");
            order.setStatus("CANCELLED");
            updateById(order);
            restoreProduct(order.getProductId());
        } else {
            order.setRefundStatus("ARBITRATION_MAINTAIN");
            updateById(order);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAddress(Long id, Long addressId) {
        Order order = getById(id);
        if (order == null) throw new BusinessException("订单不存在");
        if (!order.getBuyerId().equals(UserContext.getUserId())) throw new BusinessException("仅买家可修改地址");
        if (!"PENDING".equals(order.getStatus()) && !"PAID".equals(order.getStatus())) {
            throw new BusinessException("仅未发货订单可修改地址");
        }
        Address address = addressService.getById(addressId);
        if (address == null || !address.getUserId().equals(UserContext.getUserId())) {
            throw new BusinessException("收货地址不存在");
        }
        order.setAddressId(addressId);
        order.setBuyerName(address.getReceiverName());
        order.setBuyerPhone(address.getPhone());
        order.setBuyerAddress(address.getAddress());
        updateById(order);
    }

    @Override
    public void deleteOrder(Long id) {
        Order order = getById(id);
        if (order == null) throw new BusinessException("订单不存在");
        if (!order.getBuyerId().equals(UserContext.getUserId())
                && !order.getSellerId().equals(UserContext.getUserId())) {
            throw new BusinessException("无权删除此订单");
        }
        if (!"COMPLETED".equals(order.getStatus()) && !"CANCELLED".equals(order.getStatus())) {
            throw new BusinessException("仅已完成或已取消的订单可删除");
        }
        removeById(id);
    }
```

注意：`restoreProduct` 目前只恢复 `OFF_SHELF` 商品。退款场景下商品可能已是 `SOLD`（PAID 后），需将其改为同时处理 `SOLD`：

```java
    private void restoreProduct(Long productId) {
        Product product = productService.getById(productId);
        if (product != null
                && ("OFF_SHELF".equals(product.getStatus()) || "SOLD".equals(product.getStatus()))) {
            product.setStatus("ON_SALE");
            productService.updateById(product);
        }
    }
```

- [ ] **Step 3: 超时自动退款定时任务**

在 `OrderServiceImpl` 中 `cancelExpiredOrders` 之后追加：

```java
    /** 卖家48小时未处理退款申请，自动同意退款（每10分钟扫描） */
    @Scheduled(fixedRate = 600000)
    @Transactional(rollbackFor = Exception.class)
    public void autoRefundTimeout() {
        List<Order> pending = list(new LambdaQueryWrapper<Order>()
                .eq(Order::getRefundStatus, "REQUESTED")
                .lt(Order::getRefundTime, LocalDateTime.now().minusHours(48)));
        for (Order order : pending) {
            order.setRefundStatus("SELLER_AGREED");
            order.setStatus("CANCELLED");
            updateById(order);
            restoreProduct(order.getProductId());
        }
    }
```

- [ ] **Step 4: OrderController 新增接口**

```java
    /** 买家取消订单 */
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        orderService.cancelOrder(id);
        return Result.success();
    }

    /** 买家申请退款 */
    @PostMapping("/{id}/refund")
    public Result<Void> applyRefund(@PathVariable Long id, @RequestBody Map<String, String> params) {
        orderService.applyRefund(id, params.get("reason"));
        return Result.success();
    }

    /** 卖家处理退款 */
    @PutMapping("/{id}/refund/handle")
    public Result<Void> handleRefund(@PathVariable Long id, @RequestBody Map<String, Boolean> params) {
        orderService.handleRefund(id, Boolean.TRUE.equals(params.get("agree")));
        return Result.success();
    }

    /** 买家申请仲裁 */
    @PostMapping("/{id}/arbitration")
    public Result<Void> arbitration(@PathVariable Long id) {
        orderService.applyArbitration(id);
        return Result.success();
    }

    /** 修改收货地址 */
    @PutMapping("/{id}/address")
    public Result<Void> updateAddress(@PathVariable Long id, @RequestBody Map<String, Long> params) {
        orderService.updateAddress(id, params.get("addressId"));
        return Result.success();
    }

    /** 删除已完成/已取消订单 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return Result.success();
    }
```

- [ ] **Step 5: 管理员仲裁 + 订单/商品管理补全**

`AdminService.java` 追加：

```java
    void arbitrate(Long orderId, boolean refund);

    void updateOrderStatus(Long orderId, String status);

    void deleteOrder(Long orderId);

    void updateProductStatus(Long productId, String status);

    void deleteProduct(Long productId);
```

`AdminServiceImpl.java` 追加：

```java
    @Override
    public void arbitrate(Long orderId, boolean refund) {
        checkAdmin();
        orderService.adminArbitrate(orderId, refund);
    }

    @Override
    public void updateOrderStatus(Long orderId, String status) {
        checkAdmin();
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        order.setStatus(status);
        orderMapper.updateById(order);
    }

    @Override
    public void deleteOrder(Long orderId) {
        checkAdmin();
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        if (!"COMPLETED".equals(order.getStatus()) && !"CANCELLED".equals(order.getStatus())) {
            throw new BusinessException("仅已完成或已取消的订单可删除");
        }
        orderMapper.deleteById(orderId);
    }

    @Override
    public void updateProductStatus(Long productId, String status) {
        checkAdmin();
        Product product = productMapper.selectById(productId);
        if (product == null) throw new BusinessException("商品不存在");
        product.setStatus(status);
        productMapper.updateById(product);
    }

    @Override
    public void deleteProduct(Long productId) {
        checkAdmin();
        productMapper.deleteById(productId);
    }
```

（需确认 `AdminServiceImpl` 已注入 `orderService`，未注入则补充 `@Autowired private OrderService orderService;`）

`AdminController.java` 追加：

```java
    /** 管理员仲裁退款 */
    @PutMapping("/order/{id}/arbitration")
    public Result<Void> arbitrate(@PathVariable Long id, @RequestBody Map<String, Boolean> params) {
        adminService.arbitrate(id, Boolean.TRUE.equals(params.get("refund")));
        return Result.success();
    }

    /** 管理员修改订单状态 */
    @PutMapping("/orders/{id}/status")
    public Result<Void> updateOrderStatus(@PathVariable Long id, @RequestBody Map<String, String> params) {
        adminService.updateOrderStatus(id, params.get("status"));
        return Result.success();
    }

    /** 管理员删除订单 */
    @DeleteMapping("/orders/{id}")
    public Result<Void> deleteOrder(@PathVariable Long id) {
        adminService.deleteOrder(id);
        return Result.success();
    }

    /** 管理员修改商品状态 */
    @PutMapping("/products/{id}/status")
    public Result<Void> updateProductStatus(@PathVariable Long id, @RequestBody Map<String, String> params) {
        adminService.updateProductStatus(id, params.get("status"));
        return Result.success();
    }

    /** 管理员删除商品 */
    @DeleteMapping("/products/{id}")
    public Result<Void> deleteProduct(@PathVariable Long id) {
        adminService.deleteProduct(id);
        return Result.success();
    }
```

- [ ] **Step 6: 编译验证 + Commit**

```bash
mvn compile -q
git add backend/src
git commit -m "feat: order refund flow, cancel, arbitration, auto-refund scheduler, admin order/product ops"
```

---

### Task 5: 超级管理员角色体系 + 管理员管理（后端）

**Files:**
- Modify: `backend/src/main/java/com/campus/secondhand/util/UserContext.java`
- Modify: `backend/src/main/java/com/campus/secondhand/service/AdminService.java`
- Modify: `backend/src/main/java/com/campus/secondhand/service/impl/AdminServiceImpl.java`
- Modify: `backend/src/main/java/com/campus/secondhand/controller/AdminController.java`

- [ ] **Step 1: UserContext 角色判断升级**

将 `UserContext.java` 的角色方法改为：

```java
    public static boolean isAdmin() {
        String role = getRole();
        return "ADMIN".equals(role) || "SUPER_ADMIN".equals(role);
    }

    public static boolean isSuperAdmin() {
        return "SUPER_ADMIN".equals(getRole());
    }
```

**注意**：角色存储在 JWT Token 中。admin 账号角色已改为 `SUPER_ADMIN`，需重新登录才会生效（旧 Token 里仍是 ADMIN，`isAdmin()` 仍返回 true，无兼容问题）。

- [ ] **Step 2: AdminService 新增方法**

```java
    // ===== 管理员管理（仅 SUPER_ADMIN） =====
    IPage<User> adminPage(Integer pageNum, Integer pageSize);

    String createAdmin(String username, String nickname);

    void updateAdmin(Long id, String nickname);

    void deleteAdmin(Long id);

    void updateAdminStatus(Long id, Integer status);

    // ===== 用户管理补全 =====
    void createUser(String username, String password, String nickname, String role);

    void updateUser(Long id, String nickname, String phone, String email);

    void deleteUser(Long id);

    void resetUserPassword(Long id);
```

- [ ] **Step 3: AdminServiceImpl — 管理员管理实现**

先在类中注入 `@Autowired private BCryptPasswordEncoder passwordEncoder;`，然后追加：

```java
    private void checkSuperAdmin() {
        if (!UserContext.isSuperAdmin()) {
            throw new BusinessException("仅超级管理员可执行此操作");
        }
    }

    @Override
    public IPage<User> adminPage(Integer pageNum, Integer pageSize) {
        checkSuperAdmin();
        return userService.page(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<User>()
                        .in(User::getRole, "ADMIN", "SUPER_ADMIN")
                        .orderByDesc(User::getCreateTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createAdmin(String username, String nickname) {
        checkSuperAdmin();
        long exists = userService.count(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (exists > 0) throw new BusinessException("用户名已存在");
        String password = cn.hutool.core.util.RandomUtil.randomString(8);
        User user = new User();
        user.setUsername(username);
        user.setNickname(nickname);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("ADMIN");
        user.setStatus(1);
        user.setVerifyStatus("APPROVED");
        userService.save(user);
        return password;
    }

    @Override
    public void updateAdmin(Long id, String nickname) {
        checkSuperAdmin();
        User user = userService.getById(id);
        if (user == null || !"ADMIN".equals(user.getRole())) {
            throw new BusinessException("管理员不存在");
        }
        user.setNickname(nickname);
        userService.updateById(user);
    }

    @Override
    public void deleteAdmin(Long id) {
        checkSuperAdmin();
        User user = userService.getById(id);
        if (user == null) throw new BusinessException("管理员不存在");
        if ("SUPER_ADMIN".equals(user.getRole())) throw new BusinessException("不能删除超级管理员");
        if (user.getId().equals(UserContext.getUserId())) throw new BusinessException("不能删除自己");
        userService.removeById(id);
    }

    @Override
    public void updateAdminStatus(Long id, Integer status) {
        checkSuperAdmin();
        User user = userService.getById(id);
        if (user == null) throw new BusinessException("管理员不存在");
        if ("SUPER_ADMIN".equals(user.getRole())) throw new BusinessException("不能禁用超级管理员");
        if (user.getId().equals(UserContext.getUserId())) throw new BusinessException("不能禁用自己");
        user.setStatus(status);
        userService.updateById(user);
    }
```

- [ ] **Step 4: AdminServiceImpl — 用户管理补全实现**

先注入 `favoriteMapper`、`addressMapper`、`cartMapper`：

```java
    @Autowired
    private FavoriteMapper favoriteMapper;
    @Autowired
    private AddressMapper addressMapper;
    @Autowired
    private CartMapper cartMapper;
```

追加实现：

```java
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createUser(String username, String password, String nickname, String role) {
        checkAdmin();
        if ("SUPER_ADMIN".equals(role)) throw new BusinessException("不能创建超级管理员");
        long exists = userService.count(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (exists > 0) throw new BusinessException("用户名已存在");
        User user = new User();
        user.setUsername(username);
        user.setNickname(nickname);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role == null ? "USER" : role);
        user.setStatus(1);
        user.setVerifyStatus("APPROVED");
        userService.save(user);
    }

    @Override
    public void updateUser(Long id, String nickname, String phone, String email) {
        checkAdmin();
        User user = userService.getById(id);
        if (user == null) throw new BusinessException("用户不存在");
        user.setNickname(nickname);
        user.setPhone(phone);
        user.setEmail(email);
        userService.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        checkAdmin();
        User user = userService.getById(id);
        if (user == null) throw new BusinessException("用户不存在");
        if ("SUPER_ADMIN".equals(user.getRole())) throw new BusinessException("不能删除超级管理员");
        if (user.getId().equals(UserContext.getUserId())) throw new BusinessException("不能删除自己");

        // 级联清理：商品下架、未完成订单取消、收藏/地址/购物车清除
        productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getUserId, id)
                .set(Product::getStatus, "OFF_SHELF"));
        orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .in(Order::getStatus, "PENDING", "PAID", "SHIPPED")
                .and(w -> w.eq(Order::getBuyerId, id).or().eq(Order::getSellerId, id))
                .set(Order::getStatus, "CANCELLED"));
        favoriteMapper.delete(new LambdaQueryWrapper<Favorite>().eq(Favorite::getUserId, id));
        addressMapper.delete(new LambdaQueryWrapper<Address>().eq(Address::getUserId, id));
        cartMapper.delete(new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, id));

        user.setStatus(2); // 已注销，防止再次登录
        userService.updateById(user);
        userService.removeById(id);
    }

    @Override
    public void resetUserPassword(Long id) {
        checkAdmin();
        User user = userService.getById(id);
        if (user == null) throw new BusinessException("用户不存在");
        if ("SUPER_ADMIN".equals(user.getRole())) throw new BusinessException("不能重置超级管理员密码");
        user.setPassword(passwordEncoder.encode(cn.hutool.core.util.RandomUtil.randomString(8)));
        userService.updateById(user);
    }
```

- [ ] **Step 5: updateUserStatus 保护超级管理员**

`AdminServiceImpl.updateUserStatus` 在取出 user 后、状态修改前追加：

```java
        if ("SUPER_ADMIN".equals(user.getRole())) {
            throw new BusinessException("不能修改超级管理员状态");
        }
```

- [ ] **Step 6: AdminController 新增端点**

```java
    // ===== 管理员管理（仅超级管理员） =====

    @GetMapping("/admins")
    public Result<IPage<User>> admins(@RequestParam(defaultValue = "1") Integer pageNum,
                                      @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(adminService.adminPage(pageNum, pageSize));
    }

    @PostMapping("/admins")
    public Result<Map<String, String>> createAdmin(@RequestBody Map<String, String> params) {
        String password = adminService.createAdmin(params.get("username"), params.get("nickname"));
        Map<String, String> result = new java.util.HashMap<>();
        result.put("password", password);
        return Result.success(result);
    }

    @PutMapping("/admins/{id}")
    public Result<Void> updateAdmin(@PathVariable Long id, @RequestBody Map<String, String> params) {
        adminService.updateAdmin(id, params.get("nickname"));
        return Result.success();
    }

    @DeleteMapping("/admins/{id}")
    public Result<Void> deleteAdmin(@PathVariable Long id) {
        adminService.deleteAdmin(id);
        return Result.success();
    }

    @PutMapping("/admins/{id}/status")
    public Result<Void> updateAdminStatus(@PathVariable Long id, @RequestBody Map<String, Integer> params) {
        adminService.updateAdminStatus(id, params.get("status"));
        return Result.success();
    }

    // ===== 用户管理补全 =====

    @PostMapping("/users")
    public Result<Void> createUser(@RequestBody Map<String, String> params) {
        adminService.createUser(params.get("username"), params.get("password"),
                params.get("nickname"), params.get("role"));
        return Result.success();
    }

    @PutMapping("/users/{id}")
    public Result<Void> updateUser(@PathVariable Long id, @RequestBody Map<String, String> params) {
        adminService.updateUser(id, params.get("nickname"), params.get("phone"), params.get("email"));
        return Result.success();
    }

    @DeleteMapping("/users/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return Result.success();
    }

    @PutMapping("/users/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id) {
        adminService.resetUserPassword(id);
        return Result.success();
    }
```

- [ ] **Step 7: 编译验证 + Commit**

```bash
mvn compile -q
git add backend/src
git commit -m "feat: SUPER_ADMIN role + admin management + admin user CRUD"
```

---

### Task 6: 销售统计报表 + Excel 导出（后端）

**Files:**
- Modify: `backend/pom.xml`
- Modify: `backend/src/main/resources/mapper/OrderMapper.xml`
- Modify: `backend/src/main/java/com/campus/secondhand/mapper/OrderMapper.java`
- Create: `backend/src/main/java/com/campus/secondhand/vo/SalesRowVO.java`
- Create: `backend/src/main/java/com/campus/secondhand/service/SalesStatsService.java`
- Create: `backend/src/main/java/com/campus/secondhand/service/impl/SalesStatsServiceImpl.java`
- Create: `backend/src/main/java/com/campus/secondhand/controller/AdminStatsController.java`

- [ ] **Step 1: pom.xml 新增 EasyExcel 依赖**

在 `hutool-all` 依赖后追加：

```xml
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>easyexcel</artifactId>
            <version>3.3.4</version>
        </dependency>
```

- [ ] **Step 2: SalesRowVO**

```java
package com.campus.secondhand.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 销售统计明细行（页面表格与 Excel 导出共用）
 */
@Data
public class SalesRowVO {

    @ExcelProperty("成交时间")
    private LocalDateTime dealTime;

    @ExcelProperty("订单编号")
    private String orderNo;

    @ExcelProperty("商品标题")
    private String productTitle;

    @ExcelProperty("分类")
    private String categoryName;

    @ExcelProperty("卖家")
    private String sellerNickname;

    @ExcelProperty("买家")
    private String buyerNickname;

    @ExcelProperty("成交价")
    private BigDecimal price;
}
```

- [ ] **Step 3: OrderMapper.xml 统计查询**

在 `OrderMapper.xml` 的 `</mapper>` 前追加：

```xml
    <!-- 销售明细统计：已完成订单，按时间范围过滤 -->
    <select id="selectSalesRows" resultType="com.campus.secondhand.vo.SalesRowVO">
        SELECT o.update_time AS dealTime,
               o.order_no AS orderNo,
               p.title AS productTitle,
               c.name AS categoryName,
               su.nickname AS sellerNickname,
               bu.nickname AS buyerNickname,
               o.price AS price
        FROM t_order o
        JOIN t_product p ON o.product_id = p.id
        LEFT JOIN t_category c ON p.category_id = c.id
        JOIN t_user su ON o.seller_id = su.id
        JOIN t_user bu ON o.buyer_id = bu.id
        WHERE o.deleted = 0
          AND o.status = 'COMPLETED'
          <if test="startTime != null"> AND o.update_time &gt;= #{startTime} </if>
          <if test="endTime != null"> AND o.update_time &lt;= #{endTime} </if>
        ORDER BY o.update_time DESC
    </select>
```

`OrderMapper.java` 追加（需导入 `org.apache.ibatis.annotations.Param`）：

```java
    List<SalesRowVO> selectSalesRows(@Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime);
```

- [ ] **Step 4: SalesStatsService + Impl**

```java
package com.campus.secondhand.service;

import com.campus.secondhand.vo.SalesRowVO;

import java.util.List;
import java.util.Map;

public interface SalesStatsService {

    /** 销售统计：汇总 + 明细。period: day/week/month（影响默认时间范围） */
    Map<String, Object> salesStats(String period, String startDate, String endDate);

    List<SalesRowVO> salesRows(String startDate, String endDate);
}
```

```java
package com.campus.secondhand.service.impl;

import com.campus.secondhand.common.BusinessException;
import com.campus.secondhand.mapper.OrderMapper;
import com.campus.secondhand.service.SalesStatsService;
import com.campus.secondhand.util.UserContext;
import com.campus.secondhand.vo.SalesRowVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 销售统计服务：基于已完成订单（status=COMPLETED，以 update_time 为成交时间）
 */
@Service
public class SalesStatsServiceImpl implements SalesStatsService {

    @Autowired
    private OrderMapper orderMapper;

    @Override
    public Map<String, Object> salesStats(String period, String startDate, String endDate) {
        checkAdmin();
        List<SalesRowVO> rows = orderMapper.selectSalesRows(parseStart(startDate, period), parseEnd(endDate));

        BigDecimal totalAmount = BigDecimal.ZERO;
        Map<String, BigDecimal> byCategory = new LinkedHashMap<>();
        Map<String, Integer> bySeller = new LinkedHashMap<>();
        for (SalesRowVO row : rows) {
            totalAmount = totalAmount.add(row.getPrice());
            String cat = row.getCategoryName() != null ? row.getCategoryName() : "未分类";
            byCategory.merge(cat, row.getPrice(), BigDecimal::add);
            bySeller.merge(row.getSellerNickname(), 1, Integer::sum);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalCount", rows.size());
        result.put("totalAmount", totalAmount);
        result.put("byCategory", byCategory);
        result.put("bySeller", bySeller);
        result.put("rows", rows);
        return result;
    }

    @Override
    public List<SalesRowVO> salesRows(String startDate, String endDate) {
        checkAdmin();
        return orderMapper.selectSalesRows(parseStart(startDate, null), parseEnd(endDate));
    }

    private void checkAdmin() {
        if (!UserContext.isAdmin()) {
            throw new BusinessException("无管理员权限");
        }
    }

    private LocalDateTime parseStart(String date, String period) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        if (date == null || date.isEmpty()) {
            LocalDate today = LocalDate.now();
            if ("week".equals(period)) return today.minusDays(7).atStartOfDay();
            if ("month".equals(period)) return today.minusDays(30).atStartOfDay();
            return today.atStartOfDay();
        }
        return LocalDate.parse(date, fmt).atStartOfDay();
    }

    private LocalDateTime parseEnd(String date) {
        if (date == null || date.isEmpty()) return LocalDateTime.now();
        return LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd")).atTime(LocalTime.MAX);
    }
}
```

- [ ] **Step 5: AdminStatsController**

```java
package com.campus.secondhand.controller;

import com.alibaba.excel.EasyExcel;
import com.campus.secondhand.common.Result;
import com.campus.secondhand.service.SalesStatsService;
import com.campus.secondhand.vo.SalesRowVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 销售统计接口（管理员）
 */
@RestController
@RequestMapping("/admin/stats")
public class AdminStatsController {

    @Autowired
    private SalesStatsService salesStatsService;

    /** 销售统计（汇总+明细） */
    @GetMapping("/sales")
    public Result<Map<String, Object>> sales(@RequestParam(defaultValue = "day") String period,
                                             @RequestParam(required = false) String startDate,
                                             @RequestParam(required = false) String endDate) {
        return Result.success(salesStatsService.salesStats(period, startDate, endDate));
    }

    /** 导出销售明细 Excel */
    @GetMapping("/sales/export")
    public void export(HttpServletResponse response,
                       @RequestParam(required = false) String startDate,
                       @RequestParam(required = false) String endDate) throws Exception {
        List<SalesRowVO> rows = salesStatsService.salesRows(startDate, endDate);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String filename = URLEncoder.encode("销售统计_" + System.currentTimeMillis(), StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment;filename=" + filename + ".xlsx");
        EasyExcel.write(response.getOutputStream(), SalesRowVO.class).sheet("销售明细").doWrite(rows);
    }
}
```

导出接口需携带 Token 下载：前端用 fetch/axios 拿 blob，再拼 Authorization 头（见 Task 8）。

- [ ] **Step 6: 编译验证 + Commit**

```bash
mvn compile -q
git add backend
git commit -m "feat: sales statistics with day/week/month aggregation and Excel export"
```

---

### Task 7: 收藏模块补全（后端）

**Files:**
- Modify: `backend/src/main/java/com/campus/secondhand/service/FavoriteService.java`
- Modify: `backend/src/main/java/com/campus/secondhand/service/impl/FavoriteServiceImpl.java`
- Modify: `backend/src/main/java/com/campus/secondhand/controller/FavoriteController.java`
- Modify: `frontend/src/api/favorite.js`（Task 8 一并处理）

- [ ] **Step 1: FavoriteService 接口变更**

```java
    void removeBatch(java.util.List<Long> productIds);
```

`pageList` 签名改为：

```java
    IPage<ProductVO> pageList(Integer pageNum, Integer pageSize, String keyword);
```

- [ ] **Step 2: FavoriteServiceImpl 实现**

`removeBatch`：

```java
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeBatch(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return;
        Long userId = UserContext.getUserId();
        remove(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .in(Favorite::getProductId, productIds));
    }
```

`pageList`：先阅读现有实现，在其构造商品查询条件处追加标题关键词过滤：

```java
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Product::getTitle, keyword);
        }
```

- [ ] **Step 3: FavoriteController 更新**

```java
    /** 批量取消收藏 */
    @DeleteMapping("/batch")
    public Result<Void> removeBatch(@RequestBody java.util.Map<String, java.util.List<Long>> params) {
        favoriteService.removeBatch(params.get("productIds"));
        return Result.success();
    }
```

`list` 方法增加关键词参数并透传：

```java
    @GetMapping("/list")
    public Result<IPage<ProductVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        return Result.success(favoriteService.pageList(pageNum, pageSize, keyword));
    }
```

- [ ] **Step 4: 编译验证 + Commit**

```bash
mvn compile -q
git add backend/src
git commit -m "feat: favorite batch remove and keyword search"
```

---

### Task 8: 前端适配（分 8 个子步骤）

**前端环境**：在 `frontend/` 目录下 `npm run build` 验证，主题色 `#10B981`。

- [ ] **Step 1: API 模块**

新建 `frontend/src/api/address.js`：

```javascript
import request from '@/utils/request'

export function getAddressList() {
  return request.get('/address/list')
}
export function addAddress(data) {
  return request.post('/address', data)
}
export function updateAddress(id, data) {
  return request.put(`/address/${id}`, data)
}
export function deleteAddress(id) {
  return request.delete(`/address/${id}`)
}
export function setDefaultAddress(id) {
  return request.put(`/address/${id}/default`)
}
```

新建 `frontend/src/api/cart.js`：

```javascript
import request from '@/utils/request'

export function getCartList() {
  return request.get('/cart/list')
}
export function addToCart(productId) {
  return request.post(`/cart/${productId}`)
}
export function removeFromCart(id) {
  return request.delete(`/cart/${id}`)
}
export function removeBatchFromCart(ids) {
  return request.delete('/cart/batch', { data: { ids } })
}
export function clearCart() {
  return request.delete('/cart/clear')
}
export function checkoutCart(data) {
  return request.post('/cart/checkout', data)
}
```

新建 `frontend/src/api/stats.js`：

```javascript
import request from '@/utils/request'

export function getSalesStats(params) {
  return request.get('/admin/stats/sales', { params })
}
// Excel 导出：需要携带 Token，返回 blob 流，自行触发下载；
// 若项目的 request 实例支持 responseType，可改为 request.get(..., { responseType: 'blob' })
export function exportSales(params) {
  return request.get('/admin/stats/sales/export', { params, responseType: 'blob' })
}
```

在 `frontend/src/api/order.js` 追加：

```javascript
export function cancelOrder(id) {
  return request.post(`/order/${id}/cancel`)
}
export function applyRefund(id, reason) {
  return request.post(`/order/${id}/refund`, { reason })
}
export function handleRefund(id, agree) {
  return request.put(`/order/${id}/refund/handle`, { agree })
}
export function applyArbitration(id) {
  return request.post(`/order/${id}/arbitration`)
}
export function updateOrderAddress(id, addressId) {
  return request.put(`/order/${id}/address`, { addressId })
}
export function deleteOrder(id) {
  return request.delete(`/order/${id}`)
}
```

在 `frontend/src/api/favorite.js` 追加：

```javascript
export function removeFavoriteBatch(productIds) {
  return request.delete('/favorite/batch', { data: { productIds } })
}
```

- [ ] **Step 2: 注册页改造（`frontend/src/views/Register.vue`）**

在表单中密码字段后追加两个字段（不提示格式规则）：

```html
<el-form-item label="学号" prop="studentId">
  <el-input v-model="form.studentId" placeholder="请输入学号" maxlength="12" />
</el-form-item>
<el-form-item label="学校" prop="schoolName">
  <el-input v-model="form.schoolName" placeholder="请输入学校名称" maxlength="100" />
</el-form-item>
```

在 `form` 响应式对象中增加 `studentId: ''`、`schoolName: ''`；提交时一并传入后端（后端 `@Pattern(^\d{12}$)` 校验，错误时后端返回"学号格式不正确"，`request.js` 已统一提示）。

- [ ] **Step 3: 地址管理（`frontend/src/views/Profile.vue` 新 tab）**

1. `navItems` 数组追加 `{ key: 'address', label: '收货地址', icon: 'Location' }`（放在"我的收藏"之后）。
2. 在"我的收藏"内容块之后、"修改密码"之前插入地址列表卡片：地址表格（收货人/手机号/地址/默认标记）+ 每行操作（编辑/删除/设默认）+ 顶部"新增地址"按钮。
3. 新增/编辑弹窗 `el-dialog`：表单字段 receiverName/phone/address/isDefault（开关）。
4. 逻辑：新增后刷新列表；删除/设默认后 `ElMessage.success` 并刷新；错误由 `request.js` 统一提示。
5. 导入 `import { getAddressList, addAddress, updateAddress, deleteAddress, setDefaultAddress } from '@/api/address'`。

- [ ] **Step 4: 购物车页（新建 `frontend/src/views/Cart.vue`）**

页面结构：
- 顶部标题 + "清空购物车"按钮。
- `el-table`：多选列（selection）、商品信息（图片/标题/价格/卖家）、状态列（失效商品红色标记"已失效"并禁用勾选）、操作列（移除）。
- 底部结算栏：已选数量 + 合计金额 + "去结算"按钮。
- 点击"去结算"：弹出地址选择 `el-dialog`（单选列表，默认选中 isDefault=1 的地址，无地址时提示去个人中心添加并附跳转按钮）→ 确认后调用 `checkoutCart({ cartItemIds, addressId })` → 成功提示"已创建 N 笔订单"并跳转 `/orders`。
- 加载时调用 `getCartList()`。

- [ ] **Step 5: 商品详情页加购（`frontend/src/views/ProductDetail.vue`）**

在购买按钮旁增加"加入购物车"按钮（`el-button` plain 样式，icon=ShoppingCart）：

```javascript
import { addToCart } from '@/api/cart'

async function handleAddCart() {
  await addToCart(product.value.id)
  ElMessage.success('已加入购物车')
}
```

卖家视角（商品是自己发布的）隐藏该按钮。

- [ ] **Step 6: 我的订单页退款操作（`frontend/src/views/MyOrders.vue`）**

1. 买家视角：
   - `PENDING` 状态增加"取消订单"按钮 → `cancelOrder(id)`。
   - `PAID`/`SHIPPED` 状态增加"申请退款"按钮 → `ElMessageBox.prompt` 输入原因 → `applyRefund(id, reason)`。
   - `refundStatus === 'SELLER_REJECTED'` 时显示"申请仲裁"按钮 → `applyArbitration(id)`。
2. 卖家视角：`refundStatus === 'REQUESTED'` 时显示"同意退款/拒绝退款"两个按钮 → `handleRefund(id, true/false)`。
3. 订单卡片增加退款状态标签（`el-tag` type=warning/danger 映射 REQUESTED/ARBITRATION 等）。
4. `COMPLETED`/`CANCELLED` 状态增加"删除订单"按钮 → `ElMessageBox.confirm` 后 `deleteOrder(id)`。
5. 操作完成后刷新列表。

- [ ] **Step 7: 后台管理页增强（`frontend/src/views/Admin.vue`）**

1. **管理员管理 tab**（仅 role === 'SUPER_ADMIN' 显示）：管理员表格（用户名/昵称/状态/创建时间）+ 新增弹窗（用户名+昵称，成功后弹窗展示初始密码）+ 编辑/删除/启用禁用按钮。
2. **用户管理增强**：表格增加"新增用户"按钮（用户名/密码/昵称/角色下拉 USER/ADMIN）、每行"编辑/删除/重置密码"按钮；认证列显示 verify_status 标签；"待审核"筛选按钮 + 批量通过/拒绝（多选）。
3. **商品管理增强**：每行增加"上架/下架"切换与"删除"按钮。
4. **订单管理增强**：每行增加"强制完成/强制取消"与"删除"（仅终态）；仲裁列表：筛选 refundStatus=ARBITRATION 的订单，提供"退款/维持"按钮。
5. **销售统计 tab**：
   - 顶部周期切换（日/周/月 `el-radio-group`）+ 日期范围选择器 + 查询按钮 + "导出 Excel"按钮。
   - 汇总卡片：成交笔数、成交总额。
   - ECharts：饼图（分类销售额占比，数据源 byCategory）、柱状图（卖家销量排行，数据源 bySeller）。
   - 明细表格（rows，分页前端切片即可）。
   - 导出：`exportSales({ startDate, endDate })` 拿 blob 后 `URL.createObjectURL` + `<a download>` 触发下载。
6. 收藏列表搜索：个人中心"我的收藏"增加搜索框，`getFavoriteList({ pageNum, pageSize, keyword })`。

- [ ] **Step 8: 路由 + 头部导航**

`frontend/src/router/index.js` 主布局 children 中（`profile` 路由后）追加：

```javascript
{ path: 'cart', name: 'Cart', component: () => import('@/views/Cart.vue'), meta: { requiresAuth: true } },
```

结算逻辑在 Cart.vue 内弹窗完成，无需独立 /checkout 路由（简化实现，与规格一致：核心是"选地址→创建订单"）。
`AppHeader.vue` 导航栏（已登录时）增加购物车图标入口（icon=ShoppingCart，跳转 `/cart`）。
`stores/user.js` 的 `isAdmin` 计算属性需同时兼容 `SUPER_ADMIN`：`role === 'ADMIN' || role === 'SUPER_ADMIN'`。

- [ ] **Step 9: 构建验证 + Commit**

```bash
cd frontend
npm run build
```

预期：无 error（chunk size warning 可忽略）。

```bash
git add frontend
git commit -m "feat(frontend): cart page, address management, refund ops, admin CRUD and sales stats"
```

---

### Task 9: 全链路冒烟验证（人工）

- [ ] **Step 1: 启动后端与前端**

按 `/run-dev` 技能启动（后端 `JAVA_HOME=D:\java\jdk17`，前端 `npm run dev`）。

- [ ] **Step 2: 核心链路检查清单**

| # | 验证项 | 预期 |
|---|--------|------|
| 1 | admin/admin 登录 | 角色显示超级管理员，后台可见管理员管理+销售统计 tab |
| 2 | 注册新账号，学号输 11 位 | 提示学号格式不正确，注册失败 |
| 3 | 注册新账号，学号 12 位数字 | 注册成功，可直接发布/购买 |
| 4 | 个人中心添加 2 个地址，第二个设默认 | 第一个自动取消默认 |
| 5 | 删除默认地址 | 另一个自动成为默认 |
| 6 | 商品详情页加购物车（他人商品） | 成功；重复加购提示已在购物车 |
| 7 | 购物车批量结算（选地址） | 生成多笔订单，商品下架，购物车项清除 |
| 8 | 买家付款后申请退款，卖家拒绝，买家仲裁 | 状态依次流转；管理员后台可退款/维持 |
| 9 | 卖家 48h 超时场景无法即时验证 → 直接查库构造：将某订单 refund_status=REQUESTED、refund_time 改为 50 小时前，等 10 分钟观察自动退款 | 订单自动变 CANCELLED，商品恢复在售 |
| 10 | 管理员创建新用户/重置密码/删除用户 | 删除后该用户商品下架、无法登录 |
| 11 | SUPER_ADMIN 新建管理员，用新管理员登录 | 看不到管理员管理 tab；尝试删除 SUPER_ADMIN 被拒 |
| 12 | 销售统计：完成一笔订单后查看日统计 | 笔数/金额/分类饼图正确；导出 xlsx 可打开 |
| 13 | 收藏批量取消 + 关键词搜索 | 生效 |
| 14 | 删除已完成订单 | 列表消失（逻辑删除） |
| 15 | 未认证用户（手工改库 verify_status=PENDING）尝试发布商品 | 提示"请先完成校园身份认证" |
| 16 | 前端 `npm run build`、后端 `mvn compile` 均无错误 | 通过 |

- [ ] **Step 3: 最终 Commit**

```bash
git add -A
git commit -m "chore: batch1 feature enhancement complete (auth, address, cart, refund, super admin, stats)"
```

---

## 自审记录（已执行）

1. **规格覆盖**：校园认证→Task1；地址→Task2；购物车→Task3；订单退款/取消/仲裁/超时/地址变更/删除→Task4；超管+管理员管理→Task5；销售统计+导出→Task6；CRUD补全（用户/商品/订单/收藏）→Task4/5/7；前端→Task8；实施顺序与规格一致。
2. **类型一致性**：`CartCheckoutDTO.cartItemIds`/`addressId` 在 Task3 定义并在 Task8 前端调用一致；`refundStatus` 枚举值在 Task0 注释、Task4 实现、Task8 前端展示三处一致；`SalesRowVO` 字段与 Excel 导出、统计接口返回一致。
3. **已知修正**：Task3 的 `CartItemVO.images` 采用 `List<String>` 与 `ProductVO` 一致，后端用 Jackson 解析 Product.images 的 JSON 字符串，前端直接取 `images[0]` 展示。
