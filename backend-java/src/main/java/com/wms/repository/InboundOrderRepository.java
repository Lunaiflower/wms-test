package com.wms.repository;

import com.wms.entity.InboundOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 入库单 Repository — 候选人需要实现
 */
@Repository
public interface InboundOrderRepository extends JpaRepository<InboundOrder, Long> {

    Optional<InboundOrder> findTopByOrderNoStartingWithOrderByOrderNoDesc(String prefix);
}
