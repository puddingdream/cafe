package com.cafe.domain.order.support;

import com.cafe.domain.menu.entity.Menu;
import com.cafe.domain.menu.enums.MenuCategory;
import com.cafe.domain.menu.repository.MenuRepository;
import com.cafe.domain.order.entity.Order;
import com.cafe.domain.order.entity.OrderItem;
import com.cafe.domain.order.repository.OrderItemRepository;
import com.cafe.domain.order.repository.OrderRepository;
import com.cafe.domain.order.repository.projection.PopularMenuProjection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "app.dummy-data.enabled=false")
class OrderStatisticsReaderIntegrationTest {

    @Autowired
    private OrderStatisticsReader orderStatisticsReader;

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Test
    void findPopularMenusAggregatesOnlyPaidOrders() {
        LocalDateTime orderedFrom = LocalDateTime.now().minusSeconds(1);
        Menu americano = createMenu("Popular Americano", 4_000, MenuCategory.COFFEE);
        Menu latte = createMenu("Popular Latte", 5_000, MenuCategory.LATTE);
        Menu tea = createMenu("Canceled Tea", 4_500, MenuCategory.TEA);

        Order paidOrder1 = createPaidOrder(1L, 17_000L);
        Order paidOrder2 = createPaidOrder(2L, 30_000L);
        Order canceledOrder = createPaidOrder(3L, 450_000L);
        canceledOrder.cancel();
        orderRepository.save(canceledOrder);

        orderItemRepository.saveAll(List.of(
                orderItem(paidOrder1.getId(), americano, 3),
                orderItem(paidOrder1.getId(), latte, 1),
                orderItem(paidOrder2.getId(), latte, 5),
                orderItem(canceledOrder.getId(), tea, 100)
        ));

        List<PopularMenuProjection> popularMenus = orderStatisticsReader.findPopularMenus(orderedFrom);

        assertThat(popularMenus).hasSizeGreaterThanOrEqualTo(2);
        assertThat(popularMenus.get(0).menu().getId()).isEqualTo(latte.getId());
        assertThat(popularMenus.get(0).orderCount()).isEqualTo(6L);
        assertThat(popularMenus.get(1).menu().getId()).isEqualTo(americano.getId());
        assertThat(popularMenus.get(1).orderCount()).isEqualTo(3L);
        assertThat(popularMenus)
                .extracting(projection -> projection.menu().getId())
                .doesNotContain(tea.getId());
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

    private Order createPaidOrder(Long memberId, long totalAmount) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return orderRepository.save(Order.builder()
                .orderNumber("TEST-" + suffix)
                .memberId(memberId)
                .totalAmount(totalAmount)
                .build());
    }

    private OrderItem orderItem(Long orderId, Menu menu, int quantity) {
        return OrderItem.builder()
                .orderId(orderId)
                .menuId(menu.getId())
                .menuName(menu.getName())
                .menuPrice(menu.getPrice())
                .quantity(quantity)
                .build();
    }
}
