package com.cafe.domain.member.repository;

import com.cafe.domain.member.entity.Member;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    // 회원가입 중복 이메일 검증에 사용한다.
    boolean existsByEmail(String email);

    // 회원가입 중복 전화번호 검증에 사용한다.
    boolean existsByPhoneNumber(String phoneNumber);

    // 로그인 시 이메일로 회원을 찾는다.
    Optional<Member> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    // 포인트 충전 등 회원 기준 작업을 직렬화해야 할 때 사용한다.
    Optional<Member> findWithLockById(Long memberId);
}
