package com.cafe.domain.member.service;

import com.cafe.common.error.MemberErrorCode;
import com.cafe.common.error.MemberException;
import com.cafe.domain.auth.dto.SignUpRequest;
import com.cafe.domain.member.entity.Member;
import com.cafe.domain.member.repository.MemberRepository;
import com.cafe.domain.point.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {
    // 회원 생성과 회원 생성에 필요한 부가 작업을 담당한다.
    private final MemberRepository memberRepository;
    private final PointService pointService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Member createMember(SignUpRequest request) {
        // 이메일과 전화번호는 로그인/사용자 식별에 쓰이므로 중복을 허용하지 않는다.
        if (memberRepository.existsByEmail(request.email())) {
            throw new MemberException(MemberErrorCode.DUPLICATE_EMAIL);
        }
        if (memberRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new MemberException(MemberErrorCode.DUPLICATE_PHONE);
        }

        Member member = Member.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .phoneNumber(request.phoneNumber())
                .build();

        Member savedMember = memberRepository.save(member);
        // 회원 생성 시점에 지갑도 함께 만들어 이후 주문/충전에서 지갑 누락을 방지한다.
        Long pointWalletId = pointService.createWalletForMember(savedMember.getId());
        savedMember.linkPointWallet(pointWalletId);

        return savedMember;
    }
}
