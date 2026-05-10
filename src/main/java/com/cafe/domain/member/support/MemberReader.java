package com.cafe.domain.member.support;

import com.cafe.common.error.MemberErrorCode;
import com.cafe.common.error.MemberException;
import com.cafe.domain.member.entity.Member;
import com.cafe.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberReader {
    // 다른 도메인이 MemberRepository를 직접 뒤지지 않도록 회원 조회 책임을 모은다.
    private final MemberRepository memberRepository;

    public Member findByEmail(String email) {
        // 로그인에서는 이메일 존재 여부를 자세히 노출하지 않고 인증 실패로 처리한다.
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new MemberException(MemberErrorCode.INVALID_CREDENTIALS));
    }

    public Member findById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    public Member findByIdForUpdate(Long memberId) {
        // 포인트 충전처럼 회원 row까지 직렬화하고 싶을 때 비관락으로 조회한다.
        return memberRepository.findWithLockById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
    }
}
