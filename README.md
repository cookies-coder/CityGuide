# CityGuide - 城市文旅导览平台

基于黑马点评重构的城市文旅导览平台，作为 Java 实习求职的核心简历项目。相比原黑马点评，延伸了布隆过滤器、景点打卡、Top5 点赞排行榜、Lua 秒杀 + Stream 消息队列等自研模块。

> **技术栈：** Spring Boot 2.3.12 + Java 8 + MyBatis-Plus 3.4.3 + MySQL 8.0 + Redis (Lettuce + Redisson) + Hutool + Lombok
>
> **端口：** 8081 | **数据库：** city_guide | **前端：** Vue 3 + Vite（D:\CityGuidevue3）

---

## 功能模块总览

### 1. 🏛️ 用户登录系统

- **手机号验证码登录**：正则校验 + Redis 存储验证码（2min TTL），验证码直接打印控制台模拟发送
- **双拦截器体系**：
  - `AuthInterceptor` — 权限校验，无 Token 返回 401
  - `RefreshTokenInterceptor` — 自动刷新 Token 过期时间
- **Redis Hash 存储用户状态**（30min TTL），`TravelerContext` ThreadLocal 上下文透传
- **登出功能**：删除 Redis 中 Token 对应的用户信息，Token 立即失效
- **自动注册**：新用户首次登录自动创建账号

### 2. 🏔️ 景点模块

- **分类展示**：按 `SpotType` 筛选景点
- **详情查询**：Cache-Aside 缓存策略（30min TTL）+ **Redis 布隆过滤器**防缓存穿透
- **坐标转换**：GCJ-02 → BD-09，查询时自动转换
- **搜索**：按景点名称关键字模糊搜索

### 3. ❤️ 收藏功能

- MySQL + Redis Set **双写一致性**，O(1) 收藏状态判断
- `@Transactional` 事务保障，MyBatis-Plus 分页多表关联查询

### 4. 💬 景点评论

- 缓存优先（10min TTL），发布新评论时自动失效缓存
- `idx_spot_time` 复合索引（spot_id, create_time DESC）
- 最新 5 条展示

### 5. 📝 游记笔记 + 点赞

- **发布/查询笔记**（分页），支持图片上传
- **Top5 点赞排行榜**：Redis ZSet 实现（score = 毫秒时间戳），倒序排列最新点赞用户
- **点赞状态联动**：查询笔记时自动填充 `isLike` 字段
- **热门笔记**：按点赞数排行

### 6. 📍 景点打卡

- Redisson 分布式锁防重复打卡
- 缓存优先（30min TTL），连续打卡天数计算
- `idx_user_spot` 复合索引

### 7. 🎫 限定表演票秒杀（亮点模块）

- **Lua 脚本原子操作**：`Ticket.lua` 一次性完成库存检查、扣减、下单
- **Redis Stream 消息队列**：Lua 脚本执行成功后通过 `XADD` 将订单消息推入 Stream
- **异步消费者**：`@PostConstruct` 启动独立线程消费 Stream 消息，创建数据库订单
- **Pending 消息补偿**：消费者重启后先处理未 ACK 的历史消息
- **重试机制**：失败自动重试（最多 3 次），超限标记为 FAIL
- **一人一单**：数据库唯一索引 + Redis Set 双重保障
- **Redis 预扣库存** + 数据库最终扣减（`stock > 0` 乐观锁）
- **前端轮询**：秒杀结果通过 Redis String 返回（成功=orderId，失败=FAIL）

### 8. 👥 社交互动

- **关注/取关**：FollowController + FollowService 完整实现
- **笔记评论**：NoteCommentController + NoteCommentService 完整实现

### 9. 🖼️ 文件上传 + 用户信息

- UploadController 文件上传
- UserInfo 实体/Mapper/Service 完整实现

---

## 🔧 技术亮点

| 技术 | 应用场景 |
|:----|:--------|
| **布隆过滤器** | 景点查询防缓存穿透，启动时加载所有景点 ID |
| **Redisson 分布式锁** | 打卡防重复、秒杀防重复下单 |
| **Lua 脚本** | 秒杀原子操作，避免竞态条件 |
| **Redis Stream** | 异步订单处理消息队列，支持 Pending 补偿 |
| **Redis ZSet** | 点赞排行榜（毫秒时间戳 score） |
| **Redis Set** | 收藏状态 / 秒杀已购记录 O(1) 判断 |
| **Cache-Aside 模式** | 景点缓存，更新数据库后删除缓存 |
| **双拦截器架构** | 权限校验 + Token 自动续期分离 |
| **MyBatis-Plus 分页** | 景点/评论/收藏/笔记列表 |

---

## 📊 数据库表（13张）

```
cg_user          cg_user_info        cg_spot          cg_spot_type
cg_spot_comment  cg_user_favorite    cg_guide_note    cg_note_comment
cg_check_in      cg_ticket           cg_limited_performance_ticket
cg_ticket_order  cg_follow
```

---

## 🗃️ Redis 键设计

```
cg:login:code:{phone}              — 验证码 String (2min)
cg:login:token:{token}             — 登录态 Hash (30min)
cg:spot:cache:{id}                 — 景点缓存 String (30min)
cg:use:favorite:{userId}           — 收藏 Set
cg:like:note:{noteId}              — 点赞 ZSet
cg:checkin:{userId}:{spotId}       — 打卡缓存
cg:limited_performance:stock:{id}  — 秒杀库存 String
cg:ticket:order:stream             — 订单 Stream
cg:ticket:order:result:{u}:{t}     — 秒杀结果 String
cg:follows:{userId}                — 关注 Set
spot-id-filter                     — 布隆过滤器
```

---

## 📐 架构示意

```
前端 (Vue 3) → /api 代理 → 后端 (8081)
                              ├─ AuthInterceptor (Token 校验)
                              ├─ Service 层
                              │   ├─ Redis (缓存 / 布隆 / ZSet / Set)
                              │   ├─ Lua (秒杀原子操作)
                              │   └─ Stream → 异步消费者 → MySQL
                              └─ MySQL (13张表)
```
