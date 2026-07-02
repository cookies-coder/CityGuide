package com.city.guide.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.city.guide.entity.Ticket;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 门票 Mapper 接口
 * </p>
 *
 * @Cookie-coder
 * 
 */
public interface TicketMapper extends BaseMapper<Ticket> {

    List<Ticket> queryTicketOfSpot(@Param("spotId") Long spotId);
}
