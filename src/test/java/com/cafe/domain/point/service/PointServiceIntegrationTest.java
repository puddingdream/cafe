package com.cafe.domain.point.service;

import com.cafe.common.error.PointErrorCode;
import com.cafe.common.error.PointException;
import com.cafe.domain.auth.dto.SignUpRequest;
import com.cafe.domain.member.entity.Member;
import com.cafe.domain.member.service.MemberService;
import com.cafe.domain.point.dto.PointChargeRequest;
import com.cafe.domain.point.entity.PointWallet;
import com.cafe.domain.point.repository.PointWalletRepository;
import com.cafe.infrastructure.security.dto.LoginUserInfoDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "app.dummy-data.enabled=false")
class PointServiceIntegrationTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private PointService pointService;

    @Autowired
    private PointWalletRepository pointWalletRepository;

    @Test
    void chargeUseAndRefundPoint() {
        Member member = createMember();
        LoginUserInfoDto loginUser = LoginUserInfoDto.builder()
                .id(member.getId())
                .build();

        pointService.charge(new PointChargeRequest(10_000L), loginUser);
        long afterUsePoint = pointService.usePoint(member.getId(), 4_000L);
        long afterRefundPoint = pointService.refundPoint(member.getId(), 1_500L);

        PointWallet pointWallet = pointWalletRepository.findByMemberId(member.getId()).orElseThrow();
        assertThat(afterUsePoint).isEqualTo(6_000L);
        assertThat(afterRefundPoint).isEqualTo(7_500L);
        assertThat(pointWallet.getPoint()).isEqualTo(7_500L);
    }

    @Test
    void usePointFailsWhenPointIsInsufficient() {
        Member member = createMember();

        assertThatThrownBy(() -> pointService.usePoint(member.getId(), 1_000L))
                .isInstanceOf(PointException.class)
                .extracting("errorCode")
                .isEqualTo(PointErrorCode.INSUFFICIENT_POINT);
    }

    private Member createMember() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return memberService.createMember(new SignUpRequest(
                "point-" + suffix + "@cafe.test",
                "password123",
                "Point Test User",
                "010-point-" + suffix
        ));
    }
}
