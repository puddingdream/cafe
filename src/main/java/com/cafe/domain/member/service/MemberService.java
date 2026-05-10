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
    private final MemberRepository memberRepository;
    private final PointService pointService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Member createMember(SignUpRequest request) {
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
        Long pointWalletId = pointService.createWalletForMember(savedMember.getId());
        savedMember.linkPointWallet(pointWalletId);

        return savedMember;
    }
}
