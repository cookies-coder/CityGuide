package com.city.guide.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.city.guide.dto.Result;
import com.city.guide.dto.UserDTO;
import com.city.guide.entity.LimitedPerformanceTicket;
import com.city.guide.entity.TicketOrder;
import com.city.guide.mapper.TicketOrderMapper;
import com.city.guide.service.ILimitedPerformanceTicketService;
import com.city.guide.service.ITicketOrderService;
import com.city.guide.utils.RedisIdWorker;
import com.city.guide.utils.TravelerContext;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.city.guide.utils.RedisConstants.SECKILL_ORDER_RESULT_KEY;
import static com.city.guide.utils.RedisConstants.SECKILL_ORDER_RESULT_TTL;
import static com.city.guide.utils.RedisConstants.STREAM_KEY;
import static com.city.guide.utils.RedisConstants.CONSUMER_GROUP;
import static com.city.guide.utils.RedisConstants.CONSUMER_NAME;

/**
 * <p>
 * 门票订单服务实现类
 * </p>
 *
 * @Cookie-coder
 * 
 */
@Slf4j
@Service
public class TicketOrderServiceImpl extends ServiceImpl<TicketOrderMapper, TicketOrder> implements ITicketOrderService {
    private static final DefaultRedisScript<Long> TICKET_SCRIPT;
    static {
        TICKET_SCRIPT = new DefaultRedisScript<>();
        TICKET_SCRIPT.setLocation(new ClassPathResource("Ticket.lua"));
        TICKET_SCRIPT.setResultType(Long.class);
    }

    @Resource
    private ILimitedPerformanceTicketService limitedPerformanceTicketService;
    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedissonClient redissonClient;

    /**
     * 消费者线程池（单线程），用于异步处理订单
     */
    private final ExecutorService orderConsumerExecutor = Executors.newSingleThreadExecutor();

    /**
     * 项目启动后自动开启消费者线程，通过 Consumer Group 消费 Stream 消息
     */
    @PostConstruct
    public void startOrderConsumer() {
        // 1. 初始化消费者组（如果不存在则创建，从最新位置开始消费）
        boolean groupReady = initConsumerGroup();

        // 2. 启动消费者线程
        orderConsumerExecutor.submit(() -> {
            Consumer consumer = Consumer.from(CONSUMER_GROUP, CONSUMER_NAME);

            // ====== Phase 1: 补偿 Pending 消息（消费者崩溃重启后未 ACK 的历史消息） ======
            if (groupReady) {
                StreamReadOptions noBlock = StreamReadOptions.empty().count(10);
                StreamOffset<String> pendingOffset = StreamOffset.create(STREAM_KEY, ReadOffset.from("0"));
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        List<MapRecord<String, Object, Object>> pendingRecords = stringRedisTemplate.opsForStream()
                                .read(consumer, noBlock, pendingOffset);
                        if (pendingRecords == null || pendingRecords.isEmpty()) {
                            log.info("Pending 消息已全部处理完毕，切换到消费新消息");
                            break;
                        }
                        for (MapRecord<String, Object, Object> record : pendingRecords) {
                            processMessage(record);
                        }
                    } catch (Exception e) {
                        if (Thread.currentThread().isInterrupted())
                            break;
                        log.debug("补偿 Pending 消息跳过: {}", e.getMessage());
                        break;
                    }
                }
            } else {
                log.info("消费者组未就绪，跳过 Pending 补偿阶段");
            }

            // ====== Phase 2: 消费新消息 ======
            StreamReadOptions blockOptions = StreamReadOptions.empty().block(Duration.ofSeconds(2)).count(1);
            StreamOffset<String> newOffset = StreamOffset.create(STREAM_KEY, ReadOffset.latest());
            boolean phase2GroupReady = groupReady;

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // 如果消费者组未就绪，尝试懒创建
                    if (!phase2GroupReady) {
                        phase2GroupReady = initConsumerGroup();
                        if (!phase2GroupReady) {
                            Thread.sleep(5000);
                            continue;
                        }
                    }
                    List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream()
                            .read(consumer, blockOptions, newOffset);

                    if (records == null || records.isEmpty()) {
                        continue;
                    }

                    for (MapRecord<String, Object, Object> record : records) {
                        processMessage(record);
                    }
                } catch (Exception e) {
                    if (Thread.currentThread().isInterrupted()) {
                        break;
                    }
                    // NOGROUP 异常时重置标志，下次循环重试创建
                    if (e.getMessage() != null && e.getMessage().contains("NOGROUP")) {
                        phase2GroupReady = false;
                    }
                    log.debug("消费者读取 Stream 异常: {}", e.getMessage());
                }
            }
        });
        log.info("门票订单消费者线程已启动（Stream Consumer Group 模式）");
    }

    /**
     * 初始化消费者组，Stream 不存在时返回 false
     */
    private boolean initConsumerGroup() {
        try {
            // Stream 不存在时直接返回 false
            if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(STREAM_KEY))) {
                log.info("Stream {} 不存在，等待首次抢票时自动创建", STREAM_KEY);
                return false;
            }
            // Stream 存在，检查消费者组是否存在
            boolean exists = stringRedisTemplate.opsForStream().groups(STREAM_KEY)
                    .stream().anyMatch(g -> g.groupName().equals(CONSUMER_GROUP));
            if (!exists) {
                stringRedisTemplate.opsForStream().createGroup(STREAM_KEY, ReadOffset.latest(), CONSUMER_GROUP);
                log.info("消费者组 {} 创建成功", CONSUMER_GROUP);
            }
            return true;
        } catch (Exception e) {
            log.warn("初始化消费者组失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 处理单条 Stream 消息：创建订单、确认消息、失败重试
     */
    private void processMessage(MapRecord<String, Object, Object> record) {
        Map<Object, Object> body = record.getValue();
        Long ticketId = Long.parseLong(body.get("ticketId").toString());
        Long userId = Long.parseLong(body.get("userId").toString());
        int retryCount = body.containsKey("retryCount") ? Integer.parseInt(body.get("retryCount").toString()) : 0;

        log.info("消费者取到消息，开始创建订单，用户ID: {}, 门票ID: {}, 重试次数: {}", userId, ticketId, retryCount);

        try {
            Long orderId = handleTicketOrder(userId, ticketId);
            // 成功，XACK 确认消息
            stringRedisTemplate.opsForStream().acknowledge(STREAM_KEY, CONSUMER_GROUP, record.getId());
            // 写入结果供前端轮询
            String resultKey = SECKILL_ORDER_RESULT_KEY + userId + ":" + ticketId;
            stringRedisTemplate.opsForValue().set(resultKey, orderId.toString(), SECKILL_ORDER_RESULT_TTL,
                    TimeUnit.MINUTES);
            log.info("订单创建成功，用户ID: {}, 门票ID: {}, 订单ID: {}", userId, ticketId, orderId);
        } catch (Exception e) {
            log.error("创建订单失败，用户ID: {}, 门票ID: {}", userId, ticketId, e);
            // 失败，XACK 当前消息（避免 PEL 堆积），然后决定是否重试
            stringRedisTemplate.opsForStream().acknowledge(STREAM_KEY, CONSUMER_GROUP, record.getId());
            if (retryCount < 3) {
                // 重新 XADD 入队，retryCount + 1
                Map<String, String> retryMsg = new HashMap<>();
                retryMsg.put("ticketId", ticketId.toString());
                retryMsg.put("userId", userId.toString());
                retryMsg.put("retryCount", String.valueOf(retryCount + 1));
                stringRedisTemplate.opsForStream().add(STREAM_KEY, retryMsg);
                log.warn("消息重新入队，重试次数: {}", retryCount + 1);
            } else {
                log.error("消息重试超过3次，放弃处理，用户ID: {}, 门票ID: {}", userId, ticketId);
                String resultKey = SECKILL_ORDER_RESULT_KEY + userId + ":" + ticketId;
                stringRedisTemplate.opsForValue().set(resultKey, "FAIL", SECKILL_ORDER_RESULT_TTL, TimeUnit.MINUTES);
            }
        }
    }

    /**
     * 项目关闭时优雅关闭消费者线程
     */
    @PreDestroy
    public void stopOrderConsumer() {
        orderConsumerExecutor.shutdownNow();
        log.info("门票订单消费者线程已关闭");
    }

    @Override
    public Result seckillTicket(Long ticketId) {
        // 1. 查询限定表演票信息
        LimitedPerformanceTicket limitedPerformanceTicket = limitedPerformanceTicketService.getById(ticketId);
        if (limitedPerformanceTicket == null) {
            return Result.fail("门票不存在");
        }

        // 2. 判断秒杀是否开始
        LocalDateTime beginTime = limitedPerformanceTicket.getBeginTime();
        if (beginTime != null && beginTime.isAfter(LocalDateTime.now())) {
            return Result.fail("抢票尚未开始");
        }

        // 3. 判断秒杀是否结束
        LocalDateTime endTime = limitedPerformanceTicket.getEndTime();
        if (endTime != null && endTime.isBefore(LocalDateTime.now())) {
            return Result.fail("抢票已经结束");
        }

        // 4. 判断库存是否充足
        if (limitedPerformanceTicket.getStock() <= 0) {
            return Result.fail("库存不足");
        }

        // 5. 一人一单判断
        Long userId = TravelerContext.getTraveler().getId();
        // 创建锁对象
        RLock lock = redissonClient.getLock("cg:user:order:" + userId);
        // 获取锁
        boolean isLocked = lock.tryLock();
        if (!isLocked) {
            return Result.fail("请勿重复提交");
        }
        try {
            // 查库校验一人一单
            Integer count = lambdaQuery()
                    .eq(TicketOrder::getUserId, userId)
                    .eq(TicketOrder::getTicketId, ticketId)
                    .count();
            if (count > 0) {
                return Result.fail("用户已经购买过该门票");
            }

            // 1. 执行lua脚本
            Long result;
            try {
                result = stringRedisTemplate.execute(
                        TICKET_SCRIPT,
                        Collections.emptyList(),
                        ticketId.toString(),
                        userId.toString());
            } catch (Exception e) {
                log.error("Lua脚本执行异常，用户ID: {}, 门票ID: {}", userId, ticketId, e);
                return Result.fail("系统繁忙，请稍后重试");
            }

            // 2. 判断结果是否为0
            if (result != 0) {
                // 2.1. 不为0，代表没有购买资格
                if (result == 1L) {
                    return Result.fail("库存不足");
                } else if (result == 2L) {
                    return Result.fail("用户已经购买过该门票");
                } else {
                    return Result.fail("抢票失败");
                }
            }

            // 2.2. 为0，有购买资格，Lua脚本中已通过XADD写入Stream，无需再入队
            log.info("抢票成功，消息已通过Lua写入Stream，用户ID: {}, 门票ID: {}", userId, ticketId);
            return Result.ok("抢票成功，订单处理中");
        } finally {
            // 释放锁
            lock.unlock();
        }
    }

    /**
     * 异步消费消息时调用：创建订单 + 扣减数据库库存（事务保证原子性）
     */
    @Transactional
    public Long handleTicketOrder(Long userId, Long ticketId) {
        // 1. 创建订单
        TicketOrder ticketOrder = createTicketOrder(userId, ticketId);

        // 2. 扣减数据库库存（gt防止超卖）
        boolean updated = limitedPerformanceTicketService.lambdaUpdate()
                .setSql("stock = stock - 1")
                .eq(LimitedPerformanceTicket::getTicketId, ticketId)
                .gt(LimitedPerformanceTicket::getStock, 0)
                .update();

        if (!updated) {
            // 库存扣减失败，回滚事务
            throw new RuntimeException("数据库库存扣减失败，门票ID: " + ticketId);
        }

        return ticketOrder.getId();
    }

    @Transactional
    public TicketOrder createTicketOrder(Long userId, Long ticketId) {
        // 生成订单ID
        Long orderId = redisIdWorker.nextId("ticket_order");

        // 创建订单对象
        TicketOrder ticketOrder = new TicketOrder();
        ticketOrder.setId(orderId);
        ticketOrder.setUserId(userId);
        ticketOrder.setTicketId(ticketId);
        ticketOrder.setStatus(1); // 1：未支付
        ticketOrder.setCreateTime(LocalDateTime.now());
        ticketOrder.setUpdateTime(LocalDateTime.now());

        // 保存订单
        save(ticketOrder);

        return ticketOrder;
    }

    /**
     * 前端轮询查询秒杀下单结果
     */
    @Override
    public Result querySeckillResult(Long ticketId) {
        Long userId = TravelerContext.getTraveler().getId();
        String resultKey = SECKILL_ORDER_RESULT_KEY + userId + ":" + ticketId;
        String result = stringRedisTemplate.opsForValue().get(resultKey);

        if (result == null) {
            // Redis中没有结果，可能还在处理中
            return Result.ok("处理中");
        }
        if ("FAIL".equals(result)) {
            // 下单失败
            return Result.fail("抢票失败，请重试");
        }
        // 下单成功，返回订单ID
        return Result.ok(Long.parseLong(result));
    }
}
