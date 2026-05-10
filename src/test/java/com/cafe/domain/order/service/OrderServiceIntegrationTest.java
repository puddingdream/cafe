package com.cafe.domain.order.service;

import com.cafe.common.error.OrderErrorCode;
import com.cafe.common.error.OrderException;
import com.cafe.domain.auth.dto.SignUpRequest;
import com.cafe.domain.member.entity.Member;
import com.cafe.domain.member.service.MemberService;
import com.cafe.domain.menu.entity.Menu;
import com.cafe.domain.menu.enums.MenuCategory;
import com.cafe.domain.menu.repository.MenuRepository;
import com.cafe.domain.order.dto.OrderCancelResponse;
import com.cafe.domain.order.dto.OrderCreateRequest;
import com.cafe.domain.order.dto.OrderCreateResponse;
import com.cafe.domain.order.enums.OrderStatus;
import com.cafe.domain.order.repository.OrderItemRepository;
import com.cafe.domain.point.dto.PointChargeRequest;
import com.cafe.domain.point.entity.PointWallet;
import com.cafe.domain.point.repository.PointWalletRepository;
import com.cafe.domain.point.service.PointService;
import com.cafe.infrastructure.security.dto.LoginUserInfoDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "app.dummy-data.enabled=false")
class OrderServiceIntegrationTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private PointService pointService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private PointWalletRepository pointWalletRepository;

    @Test
    void createOrderMergesSameMenuAndCancelRefundsPoint() {
        Member member = createMember();
        LoginUserInfoDto loginUser = loginUser(member);
        pointService.charge(new PointChargeRequest(50_000L), loginUser);
        Menu americano = createMenu("Order Americano", 4_000, MenuCategory.COFFEE);
        Menu latte = createMenu("Order Latte", 5_000, MenuCategory.LATTE);

        OrderCreateResponse createResponse = orderService.createOrder(new OrderCreateRequest(List.of(
                new OrderCreateRequest.Item(americano.getId(), 2),
                new OrderCreateRequest.Item(americano.getId(), 3),
                new OrderCreateRequest.Item(latte.getId(), 1)
        )), loginUser);

        assertThat(createResponse.totalAmount()).isEqualTo(25_000L);
        assertThat(createResponse.afterPoint()).isEqualTo(25_000L);
        assertThat(createResponse.items()).hasSize(2);
        assertThat(orderItemRepository.findAllByOrderIdOrderByIdAsc(createResponse.orderId())).hasSize(2);

        OrderCancelResponse cancelResponse = orderService.cancelOrder(createResponse.orderNumber(), loginUser);

        PointWallet pointWallet = pointWalletRepository.findByMemberId(member.getId()).orElseThrow();
        assertThat(cancelResponse.status()).isEqualTo(OrderStatus.CANCELED);
        assertThat(cancelResponse.afterPoint()).isEqualTo(50_000L);
        assertThat(pointWallet.getPoint()).isEqualTo(50_000L);
    }

    @Test
    void createOrderFailsWhenMenuIsInactive() {
        Member member = createMember();
        LoginUserInfoDto loginUser = loginUser(member);
        pointService.charge(new PointChargeRequest(50_000L), loginUser);
        Menu menu = createMenu("Inactive Menu", 4_500, MenuCategory.COFFEE);
        menu.toggleStatus();
        menuRepository.save(menu);

        assertThatThrownBy(() -> orderService.createOrder(new OrderCreateRequest(List.of(
                new OrderCreateRequest.Item(menu.getId(), 1)
        )), loginUser))
                .isInstanceOf(OrderException.class)
                .extracting("errorCode")
                .isEqualTo(OrderErrorCode.NOT_ORDERABLE_MENU);
    }

    private Member createMember() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return memberService.createMember(new SignUpRequest(
                "order-" + suffix + "@cafe.test",
                "password123",
                "Order Test User",
                "010-order-" + suffix
        ));
    }

    private LoginUserInfoDto loginUser(Member member) {
        return LoginUserInfoDto.builder()
                .id(member.getId())
                .build();
    }

    private Menu createMenu(String prefix, int price, MenuCategory category) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return menuRepository.save(Menu.builder()
                .name(prefix + " " + suffix)
                .description(prefix + " description")
                .price(price)
                .category(category)
                .imageUrl("https://example.com/" + suffix + ".png")
                .build());
    }
}
