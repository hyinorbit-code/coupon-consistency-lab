package io.github.hyinorbitcode.couponlab.drop.repository;

import io.github.hyinorbitcode.couponlab.drop.entity.DropEvent;
import io.github.hyinorbitcode.couponlab.drop.entity.DropEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DropEventRepository extends JpaRepository<DropEvent, Long> {

    List<DropEvent> findByStatusOrderByOpenAtAscIdAsc(DropEventStatus status);
}
