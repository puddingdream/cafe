package com.cafe.infrastructure.bootstrap;

import com.cafe.domain.member.entity.Member;
import com.cafe.domain.member.enums.MemberRole;
import com.cafe.domain.member.repository.MemberRepository;
import com.cafe.domain.menu.entity.Menu;
import com.cafe.domain.menu.enums.MenuCategory;
import com.cafe.domain.menu.repository.MenuRepository;
import com.cafe.domain.point.entity.PointHistory;
import com.cafe.domain.point.entity.PointWallet;
import com.cafe.domain.point.repository.PointHistoryRepository;
import com.cafe.domain.point.repository.PointWalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.dummy-data", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DummyDataInitializer implements ApplicationRunner {
    // 로컬 수동 테스트를 빠르게 할 수 있도록 기본 계정과 메뉴를 시딩한다.

    private static final String TEST_PASSWORD = "password123";

    private final MemberRepository memberRepository;
    private final PointWalletRepository pointWalletRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final MenuRepository menuRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // 이미 존재하는 데이터는 건너뛰므로 애플리케이션을 여러 번 실행해도 중복 생성되지 않는다.
        createMemberIfAbsent(
                "admin@cafe.test",
                "Cafe Admin",
                "010-1000-0001",
                MemberRole.ADMIN,
                200_000L
        );
        createMemberIfAbsent(
                "user@cafe.test",
                "Cafe User",
                "010-1000-0002",
                MemberRole.USER,
                100_000L
        );

        createMenuIfAbsent("Americano", "Clean espresso with hot water.", 4_500, MenuCategory.COFFEE, "https://placehold.co/600x400?text=Americano");
        createMenuIfAbsent("Cold Brew", "Slow extracted coffee with a smooth finish.", 5_000, MenuCategory.COFFEE, "https://placehold.co/600x400?text=Cold+Brew");
        createMenuIfAbsent("Cafe Latte", "Espresso with steamed milk.", 5_200, MenuCategory.LATTE, "https://placehold.co/600x400?text=Cafe+Latte");
        createMenuIfAbsent("Vanilla Latte", "Latte with vanilla syrup.", 5_800, MenuCategory.LATTE, "https://placehold.co/600x400?text=Vanilla+Latte");
        createMenuIfAbsent("Earl Grey Tea", "Black tea with bergamot aroma.", 4_800, MenuCategory.TEA, "https://placehold.co/600x400?text=Earl+Grey");
        createMenuIfAbsent("Chamomile Tea", "Caffeine-free herbal tea.", 4_800, MenuCategory.TEA, "https://placehold.co/600x400?text=Chamomile");
        createMenuIfAbsent("Lemon Ade", "Fresh lemon ade with sparkling water.", 5_500, MenuCategory.ADE, "https://placehold.co/600x400?text=Lemon+Ade");
        createMenuIfAbsent("Strawberry Smoothie", "Blended strawberry smoothie.", 6_500, MenuCategory.SMOOTHIE, "https://placehold.co/600x400?text=Strawberry+Smoothie");
        createMenuIfAbsent("Decaf Americano", "Decaffeinated espresso with hot water.", 5_000, MenuCategory.DECAFFEINATED, "https://placehold.co/600x400?text=Decaf+Americano");
        createMenuIfAbsent("Basque Cheesecake", "Rich burnt cheesecake.", 7_000, MenuCategory.DESSERT, "https://placehold.co/600x400?text=Cheesecake");

        log.info("Dummy data initialized. admin={}, user={}", "admin@cafe.test", "user@cafe.test");
    }

    private void createMemberIfAbsent(String email, String name, String phoneNumber, MemberRole role, long initialPoint) {
        // 테스트 계정은 권한과 초기 포인트 지갑까지 함께 준비한다.
        Member member = memberRepository.findByEmail(email)
                .orElseGet(() -> memberRepository.save(Member.builder()
                        .email(email)
                        .password(passwordEncoder.encode(TEST_PASSWORD))
                        .name(name)
                        .phoneNumber(phoneNumber)
                        .role(role)
                        .build()));

        if (member.getRole() != role) {
            member.changeRole(role);
        }

        pointWalletRepository.findByMemberId(member.getId())
                .ifPresentOrElse(
                        pointWallet -> member.linkPointWallet(pointWallet.getId()),
                        () -> createWallet(member, initialPoint)
                );
    }

    private void createWallet(Member member, long initialPoint) {
        // 지갑 생성과 초기 충전 이력을 함께 남겨 포인트 흐름을 추적할 수 있게 한다.
        PointWallet pointWallet = PointWallet.builder()
                .memberId(member.getId())
                .build();
        long afterPoint = pointWallet.charge(initialPoint);
        PointWallet savedPointWallet = pointWalletRepository.save(pointWallet);

        member.linkPointWallet(savedPointWallet.getId());
        pointHistoryRepository.save(PointHistory.charge(
                member.getId(),
                savedPointWallet.getId(),
                initialPoint,
                afterPoint
        ));
    }

    private void createMenuIfAbsent(String name, String description, int price, MenuCategory category, String imageUrl) {
        // 더미 메뉴는 S3 업로드 없이 placeholder 이미지 URL로 만든다.
        if (menuRepository.existsByName(name)) {
            return;
        }

        menuRepository.save(Menu.builder()
                .name(name)
                .description(description)
                .price(price)
                .category(category)
                .imageUrl(imageUrl)
                .build());
    }
}
