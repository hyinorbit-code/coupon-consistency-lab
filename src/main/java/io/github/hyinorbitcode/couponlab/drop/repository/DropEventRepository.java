package io.github.hyinorbitcode.couponlab.drop.repository;

import io.github.hyinorbitcode.couponlab.drop.entity.DropEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DropEventRepository extends JpaRepository<DropEvent, Long> {
}
