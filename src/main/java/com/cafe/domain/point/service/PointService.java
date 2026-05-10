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
        PointWallet pointWallet = findWalletForUpdate(member.getId());

        long afterPoint = pointWallet.charge(request.chargePoint());
        pointHistoryRepository.save(PointHistory.charge(
                member.getId(),
                pointWallet.getId(),
                request.chargePoint(),
                afterPoint
        ));

        return new PointChargeResponse(member.getId(), request.chargePoint(), afterPoint);
    }

    @Transactional
    public Long createWalletForMember(Long memberId) {
        PointWallet pointWallet = pointWalletRepository.findByMemberId(memberId)
                .orElseGet(() -> pointWalletRepository.save(PointWallet.builder()
                        .memberId(memberId)
                        .build()));

        return pointWallet.getId();
    }

    @Transactional
    public long usePoint(Long memberId, long usedPoint) {
        PointWallet pointWallet = pointWalletRepository.findWithLockByMemberId(memberId)
                .orElseThrow(() -> new PointException(PointErrorCode.POINT_WALLET_NOT_FOUND));

        long afterPoint = pointWallet.use(usedPoint);
        pointHistoryRepository.save(PointHistory.use(
                memberId,
                pointWallet.getId(),
                usedPoint,
                afterPoint
        ));

        return afterPoint;
    }

    @Transactional
    public long refundPoint(Long memberId, long refundPoint) {
        PointWallet pointWallet = pointWalletRepository.findWithLockByMemberId(memberId)
                .orElseThrow(() -> new PointException(PointErrorCode.POINT_WALLET_NOT_FOUND));

        long afterPoint = pointWallet.refund(refundPoint);
        pointHistoryRepository.save(PointHistory.refund(
                memberId,
                pointWallet.getId(),
                refundPoint,
                afterPoint
        ));

        return afterPoint;
    }

    private PointWallet findWalletForUpdate(Long memberId) {
        return pointWalletRepository.findWithLockByMemberId(memberId)
                .orElseThrow(() -> new PointException(PointErrorCode.POINT_WALLET_NOT_FOUND));
    }

    private void validateChargePoint(long chargePoint) {
        if (chargePoint <= 0) {
            throw new PointException(PointErrorCode.INVALID_CHARGE_POINT);
        }
    }
}
