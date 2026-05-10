package com.cafe.concurrency;

import com.cafe.domain.auth.dto.SignUpRequest;
import com.cafe.domain.member.entity.Member;
import com.cafe.domain.member.service.MemberService;
import com.cafe.domain.menu.entity.Menu;
import com.cafe.domain.menu.enums.MenuCategory;
import com.cafe.domain.menu.repository.MenuRepository;
import com.cafe.domain.order.dto.OrderCreateRequest;
import com.cafe.domain.order.dto.OrderCreateResponse;
import com.cafe.domain.order.enums.OrderStatus;
import com.cafe.domain.order.repository.OrderRepository;
import com.cafe.domain.order.service.OrderService;
import com.cafe.domain.point.dto.PointChargeRequest;
import com.cafe.domain.point.entity.PointWallet;
import com.cafe.domain.point.repository.PointWalletRepository;
import com.cafe.domain.point.service.PointService;
import com.cafe.infrastructure.security.dto.LoginUserInfoDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

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
class OrderConcurrencyIntegrationTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private PointService pointService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PointWalletRepository pointWalletRepository;

    @Test
    void concurrentOrdersForSameMemberDoNotOverSpendPoint() throws Exception {
        int threadCount = 8;
        int menuPrice = 10_000;
        Member member = createMember();
        LoginUserInfoDto loginUser = LoginUserInfoDto.builder()
                .id(member.getId())
                .build();
        pointService.charge(new PointChargeRequest(menuPrice), loginUser);
        Menu menu = createMenu(menuPrice);
        OrderCreateRequest request = new OrderCreateRequest(List.of(
                new OrderCreateRequest.Item(menu.getId(), 1)
        ));

        List<Result> results = runConcurrently(threadCount, () -> {
            OrderCreateResponse response = orderService.createOrder(request, loginUser);
            return Result.ok(response);
        });

        long successCount = results.stream()
                .filter(Result::succeeded)
                .count();
        PointWallet pointWallet = pointWalletRepository.findByMemberId(member.getId()).orElseThrow();
        long paidOrderCount = orderRepository.findAllByMemberIdOrderByOrderedAtDesc(
                        member.getId(),
                        PageRequest.of(0, threadCount)
                )
                .stream()
                .filter(order -> order.getStatus() == OrderStatus.PAID)
                .count();

        assertThat(successCount).isOne();
        assertThat(results).hasSize(threadCount);
        assertThat(pointWallet.getPoint()).isZero();
        assertThat(paidOrderCount).isOne();
    }

    @Test
    void concurrentOrdersForDifferentMembersAllSucceed() throws Exception {
        int threadCount = 8;
        int menuPrice = 10_000;
        Menu menu = createMenu(menuPrice);
        List<Member> members = new ArrayList<>();
        List<LoginUserInfoDto> loginUsers = new ArrayList<>();
        for (int count = 0; count < threadCount; count++) {
            Member member = createMember();
            LoginUserInfoDto loginUser = LoginUserInfoDto.builder()
                    .id(member.getId())
                    .build();
            pointService.charge(new PointChargeRequest(menuPrice), loginUser);
            members.add(member);
            loginUsers.add(loginUser);
        }
        OrderCreateRequest request = new OrderCreateRequest(List.of(
                new OrderCreateRequest.Item(menu.getId(), 1)
        ));

        List<Callable<Result>> tasks = new ArrayList<>();
        for (LoginUserInfoDto loginUser : loginUsers) {
            tasks.add(() -> {
                OrderCreateResponse response = orderService.createOrder(request, loginUser);
                return Result.ok(response);
            });
        }

        List<Result> results = runConcurrently(tasks);

        long successCount = results.stream()
                .filter(Result::succeeded)
                .count();
        assertThat(successCount).isEqualTo(threadCount);

        for (Member member : members) {
            PointWallet pointWallet = pointWalletRepository.findByMemberId(member.getId()).orElseThrow();
            long paidOrderCount = orderRepository.findAllByMemberIdOrderByOrderedAtDesc(
                            member.getId(),
                            PageRequest.of(0, 1)
                    )
                    .stream()
                    .filter(order -> order.getStatus() == OrderStatus.PAID)
                    .count();

            assertThat(pointWallet.getPoint()).isZero();
            assertThat(paidOrderCount).isOne();
        }
    }

    private List<Result> runConcurrently(int threadCount, Callable<Result> task) throws Exception {
        return runConcurrently(new ArrayList<>(java.util.Collections.nCopies(threadCount, task)));
    }

    private List<Result> runConcurrently(List<Callable<Result>> tasks) throws Exception {
        int threadCount = tasks.size();
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        try {
            List<Future<Result>> futures = new ArrayList<>();
            for (Callable<Result> task : tasks) {
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
                "order-concurrency-" + suffix + "@cafe.test",
                "password123",
                "Order Concurrency User",
                "010-order-con-" + suffix
        ));
    }

    private Menu createMenu(int price) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return menuRepository.save(Menu.builder()
                .name("Concurrency Menu " + suffix)
                .description("Concurrency test menu")
                .price(price)
                .category(MenuCategory.COFFEE)
                .imageUrl("https://example.com/concurrency-" + suffix + ".png")
                .build());
    }

    private record Result(boolean succeeded, OrderCreateResponse response, Exception exception) {
        static Result ok(OrderCreateResponse response) {
            return new Result(true, response, null);
        }

        static Result failed(Exception exception) {
            return new Result(false, null, exception);
        }
    }
}
