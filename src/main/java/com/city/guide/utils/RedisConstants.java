package com.city.guide.utils;

public class RedisConstants {
    public static final String LOGIN_CODE_KEY = "cg:login:code:";
    public static final Long LOGIN_CODE_TTL = 2L;
    public static final String LOGIN_USER_KEY = "cg:login:token:";
    public static final Long LOGIN_USER_TTL = 30L;

    public static final Long CACHE_SPOT_TTL = 30L;
    public static final String CACHE_SPOT_KEY = "cg:spot:cache:";

    public static final String LIMITED_PERFORMANCE_STOCK_KEY = "cg:limited_performance:stock:";

    public static final String USE_FAVORITE_KEY = "cg:use:favorite:";

    public static final String SPOT_COMMENT_CACHE_KEY = "cg:spot:comment:";
    public static final Long SPOT_COMMENT_CACHE_TTL = 10L;

    /**
     * 秒杀订单结果 key，格式：cg:ticket:order:result:{userId}:{ticketId}
     * 值：成功时为 orderId，失败时为 "FAIL"
     */
    public static final String SECKILL_ORDER_RESULT_KEY = "cg:ticket:order:result:";
    public static final Long SECKILL_ORDER_RESULT_TTL = 15L;

    /**
     * Stream 消息队列相关常量
     */
    public static final String STREAM_KEY = "cg:ticket:order:stream";
    public static final String CONSUMER_GROUP = "orderConsumerGroup";
    public static final String CONSUMER_NAME = "consumer-1";

    /**
     * 关注关系 Set key，格式：cg:follows:{userId}
     * 值：该用户关注的所有用户 ID 集合
     */
    public static final String FOLLOW_KEY = "cg:follows:";
}
