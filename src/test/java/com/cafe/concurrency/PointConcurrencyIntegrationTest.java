package com.cafe.concurrency;

import com.cafe.domain.auth.dto.SignUpRequest;
import com.cafe.domain.member.entity.Member;
import com.cafe.domain.member.service.MemberService;
import com.cafe.domain.point.dto.PointChargeRequest;
import com.cafe.domain.point.entity.PointWallet;
import com.cafe.domain.point.repository.PointWalletRepository;
import com.cafe.domain.point.service.PointService;
import com.cafe.infrastructure.security.dto.LoginUserInfoDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "app.dummy-data.enabled=false")
class PointConcurrencyIntegrationTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private PointService pointService;

    @Autowired
    private PointWalletRepository pointWalletRepository;

    @Test
    void concurrentPointUseKeepsWalletBalanceConsistent() throws Exception {
        int threadCount = 20;
        long usePoint = 1_000L;
        long initialPoint = threadCount * usePoint;
        Member member = createMember();
        LoginUserInfoDto loginUser = LoginUserInfoDto.builder()
                .id(member.getId())
                .build();
        pointService.charge(new PointChargeRequest(initialPoint), loginUser);

        List<Result> results = runConcurrently(threadCount, () -> {
            pointService.usePoint(member.getId(), usePoint);
            return Result.ok();
        });

        PointWallet pointWallet = pointWalletRepository.findByMemberId(member.getId()).orElseThrow();
        assertThat(results).allMatch(Result::succeeded);
        assertThat(pointWallet.getPoint()).isZero();
    }

    @Test
    void concurrentPointChargeAccumulatesEveryCharge() throws Exception {
        int threadCount = 10;
        long chargePoint = 1_000L;
        Member member = createMember();
        LoginUserInfoDto loginUser = LoginUserInfoDto.builder()
                .id(member.getId())
                .build();

        List<Result> results = runConcurrently(threadCount, () -> {
            pointService.charge(new PointChargeRequest(chargePoint), loginUser);
            return Result.ok();
        });

        PointWallet pointWallet = pointWalletRepository.findByMemberId(member.getId()).orElseThrow();
        assertThat(results).allMatch(Result::succeeded);
        assertThat(pointWallet.getPoint()).isEqualTo(threadCount * chargePoint);
    }

    private List<Result> runConcurrently(int threadCount, Callable<Result> task) throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        try {
            List<Future<Result>> futures = new ArrayList<>();
            for (int count = 0; count < threadCount; count++) {
                futures.add(executorService.submit(() -> {
                    readyLatch.countDown();
                    startLatch.await();
                    try {
                        return task.call();
                    } catch (Exception exception) {
                        return Result.failed(exception);
                    }
                }));
            }

            readyLatch.await();
            startLatch.countDown();

            List<Result> results = new ArrayList<>();
            for (Future<Result> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            executorService.shutdownNow();
        }
    }

    private Member createMember() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return memberService.createMember(new SignUpRequest(
                "point-concurrency-" + suffix + "@cafe.test",
                "password123",
                "Point Concurrency User",
                "010-point-con-" + suffix
        ));
    }

    private record Result(boolean succeeded, Exception exception) {
        static Result ok() {
            return new Result(true, null);
        }

        static Result failed(Exception exception) {
            return new Result(false, exception);
        }
    }
}
