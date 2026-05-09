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
    private final PointWalletRepository pointWalletRepository;

    public PointWallet findByMemberId(Long memberId) {
        return pointWalletRepository.findByMemberId(memberId)
                .orElseThrow(() -> new PointException(PointErrorCode.POINT_WALLET_NOT_FOUND));
    }

    public PointWallet findByMemberIdForUpdate(Long memberId) {
        return pointWalletRepository.findWithLockByMemberId(memberId)
                .orElseThrow(() -> new PointException(PointErrorCode.POINT_WALLET_NOT_FOUND));
    }
}
