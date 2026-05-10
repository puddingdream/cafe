package com.cafe.domain.point.support;

import com.cafe.common.error.PointErrorCode;
import com.cafe.common.error.PointException;
import com.cafe.domain.point.entity.PointWallet;
import com.cafe.domain.point.repository.PointWalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointWalletReader {
    // 포인트 지갑 조회를 다른 도메인에서 재사용할 수 있게 모은 reader다.
    private final PointWalletRepository pointWalletRepository;

    public PointWallet findByMemberId(Long memberId) {
        // 지갑이 없으면 회원 생성 흐름이 깨진 상태로 보고 도메인 예외를 던진다.
        return pointWalletRepository.findByMemberId(memberId)
                .orElseThrow(() -> new PointException(PointErrorCode.POINT_WALLET_NOT_FOUND));
    }

    public PointWallet findByMemberIdForUpdate(Long memberId) {
        return pointWalletRepository.findWithLockByMemberId(memberId)
                .orElseThrow(() -> new PointException(PointErrorCode.POINT_WALLET_NOT_FOUND));
    }
}
