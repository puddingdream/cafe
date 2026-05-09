package com.cafe.domain.point.service;

import com.cafe.common.error.PointErrorCode;
import com.cafe.common.error.PointException;
import com.cafe.domain.member.entity.Member;
import com.cafe.domain.member.support.MemberReader;
import com.cafe.domain.point.dto.PointChargeRequest;
import com.cafe.domain.point.dto.PointChargeResponse;
import com.cafe.domain.point.entity.PointHistory;
import com.cafe.domain.point.entity.PointWallet;
import com.cafe.domain.point.repository.PointHistoryRepository;
import com.cafe.domain.point.repository.PointWalletRepository;
import com.cafe.infrastructure.security.dto.LoginUserInfoDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointService {
    private final PointWalletRepository pointWalletRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final MemberReader memberReader;

    @Transactional
    public PointChargeResponse charge(PointChargeRequest request, LoginUserInfoDto loginUser) {
        validateChargePoint(request.chargePoint());

        Member member = memberReader.findByIdForUpdate(loginUser.id());
        PointWallet pointWallet = findOrCreateWallet(member);

        long afterPoint = pointWallet.charge(request.chargePoint());
        pointHistoryRepository.save(PointHistory.charge(
                member.getId(),
                pointWallet.getId(),
                request.chargePoint(),
                afterPoint
        ));

        return new PointChargeResponse(member.getId(), request.chargePoint(), afterPoint);
    }

    private PointWallet findOrCreateWallet(Member member) {
        return pointWalletRepository.findWithLockByMemberId(member.getId())
                .orElseGet(() -> {
                    PointWallet pointWallet = pointWalletRepository.save(PointWallet.builder()
                            .memberId(member.getId())
                            .build());
                    member.linkPointWallet(pointWallet.getId());
                    return pointWallet;
                });
    }

    private void validateChargePoint(long chargePoint) {
        if (chargePoint <= 0) {
            throw new PointException(PointErrorCode.INVALID_CHARGE_POINT);
        }
    }
}
