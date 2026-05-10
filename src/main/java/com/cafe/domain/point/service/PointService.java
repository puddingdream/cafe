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
    // 포인트 지갑의 생성, 충전, 사용, 환불을 담당한다.
    private final PointWalletRepository pointWalletRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final MemberReader memberReader;

    @Transactional
    public PointChargeResponse charge(PointChargeRequest request, LoginUserInfoDto loginUser) {
        // 충전은 지갑 row를 비관락으로 잠근 뒤 잔액과 이력을 함께 저장한다.
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
        // 회원 생성 시 기본 지갑을 만든다. 이미 있으면 기존 지갑을 반환해 중복 생성을 피한다.
        PointWallet pointWallet = pointWalletRepository.findByMemberId(memberId)
                .orElseGet(() -> pointWalletRepository.save(PointWallet.builder()
                        .memberId(memberId)
                        .build()));

        return pointWallet.getId();
    }

    @Transactional
    public long usePoint(Long memberId, long usedPoint) {
        // 주문 결제에서 호출된다. 지갑 row lock으로 동시 차감을 직렬화한다.
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
        // 주문 취소에서 호출된다. 환불 역시 같은 지갑 row lock을 사용한다.
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
