---
name: backend-dev
description: Backend API development for SecondHand project. Triggers when adding/modifying backend controllers, services, entities, DTOs, or Mappers.
---

# Backend Development — 校园二手平台

## Tech Stack
- Spring Boot 3.3 + Java 17 + MyBatis-Plus + JWT + MySQL
- Lombok (`@Data`, `@TableName`, `@TableId`, `@TableLogic`)

## Project Layers (bottom-up)

### Entity (`backend/src/main/java/com/campus/secondhand/entity/`)
- `@TableName("t_xxx")` — maps to table `t_` prefix
- `@TableId(type = IdType.AUTO)` — auto-increment PK
- `@TableLogic` — soft delete (`deleted` field, `TINYINT NOT NULL DEFAULT 0`)
- `@TableField(fill = FieldFill.INSERT)` — auto-fill `createTime`
- `@TableField(fill = FieldFill.INSERT_UPDATE)` — auto-fill `updateTime`

### DTO (`backend/src/main/java/com/campus/secondhand/dto/`)
- Input objects passed from frontend to controller
- Use `@Validated` on controller method parameters for validation

### VO (`backend/src/main/java/com/campus/secondhand/vo/`)
- Output objects returned to frontend (may include joined fields like `sellerName`)
- Used in `Result<IPage<ProductVO>>` for paginated lists

### Mapper (`backend/src/main/java/com/campus/secondhand/mapper/`)
- Extends `BaseMapper<Entity>` — provides built-in CRUD
- Custom queries go in `resources/mapper/XxxMapper.xml`

### Service (`backend/src/main/java/com/campus/secondhand/service/`)
- Interface in `service/`, impl in `service/impl/`
- Annotate impl with `@Service`
- Use `@Autowired` for mapper injection

### Controller (`backend/src/main/java/com/campus/secondhand/controller/`)
```java
@RestController
@RequestMapping("/resource-name")
public class XxxController {
    @Autowired
    private XxxService xxxService;

    @GetMapping("/list")
    public Result<IPage<XxxVO>> list(QueryDTO query) {
        return Result.success(xxxService.pageList(query));
    }

    @GetMapping("/detail/{id}")
    public Result<XxxVO> detail(@PathVariable Long id) {
        return Result.success(xxxService.detail(id));
    }

    @PostMapping
    public Result<Void> create(@Validated @RequestBody XxxDTO dto) {
        xxxService.create(dto);
        return Result.success();
    }
}
```

### Result<T> Convention
- `Result.success(data)` — code 200
- `Result.success()` — code 200, no data
- `Result.error("message")` — code 500
- Frontend checks `res.code !== 200` and shows `res.message`
- Code 401 triggers auto-logout on frontend

## JWT Auth
- Token stored in `localStorage` key: `secondhand_token`
- Frontend sends via `Authorization: Bearer <token>`
- Server interceptor: `config/interceptor/JwtInterceptor.java`
- Get current user: `UserContext.getUserId()` (may throw if not logged in)
- Wrap in try/catch for optional-auth endpoints:

```java
Long currentUserId = null;
try {
    currentUserId = UserContext.getUserId();
} catch (Exception ignored) {}
```

## File Upload
- Upload path configured in `application.yml` (`file.upload-path`)
- Static serving via `WebMvcConfig.java` (`/uploads/**` → file system)
- Frontend uploads to `/api/file/upload`, returns URL
- Store URL string in entity (e.g., `images` field as JSON array text)

## MyBatis-Plus Pagination
```java
IPage<XxxVO> pageList(ProductQueryDTO query) {
    Page<Xxx> page = new Page<>(query.getPageNum(), query.getPageSize());
    // ... query wrapper ...
    return mapper.selectPage(page, wrapper);
}
```
- Uses `IPage<T>` return type, `Result<IPage<T>>` at controller
- Frontend receives `{ code: 200, data: { records: [...], total: N } }`

## Quick Reference

| Action | File to create/edit |
|--------|-------------------|
| New API endpoint | Controller + Service interface + Service impl |
| New data table | Entity + Mapper + SQL migration + (optional) XML mapper |
| New request fields | DTO |
| New response fields | VO |
| Auth required | `UserContext.getUserId()` in service |
| Admin only | Check `user.getRole() == "ADMIN"` |
| Business rule error | `throw new BusinessException("message")` |
