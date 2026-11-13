package io.github.hyinorbitcode.couponlab.user;

import java.util.Collection;
import java.util.Map;

/** 다른 모듈이 사용자 닉네임을 조회할 때 쓰는 공개 API. */
public interface UserNicknames {

    /** 사용자 ID별 닉네임. 없는 ID는 결과에 들어가지 않는다. */
    Map<Long, String> findNicknames(Collection<Long> userIds);
}
