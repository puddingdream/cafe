package com.cafe.domain.order.service;

import com.cafe.common.error.OrderErrorCode;
import com.cafe.common.error.OrderException;
import com.cafe.domain.member.entity.Member;
import com.cafe.domain.member.support.MemberReader;
import com.cafe.domain.menu.entity.Menu;
import com.cafe.domain.menu.enums.MenuStatus;
import com.cafe.domain.menu.support.MenuReader;
import com.cafe.domain.order.dto.*;
import com.cafe.domain.order.entity.Order;
import com.cafe.domain.order.entity.OrderItem;
import com.cafe.domain.order.event.OrderCanceledEvent;
import com.cafe.domain.order.event.OrderPaidEvent;
import com.cafe.domain.order.repository.OrderItemRepository;
import com.cafe.domain.order.repository.OrderRepository;
import com.cafe.domain.order.support.OrderNumberGenerator;
import com.cafe.domain.point.service.PointService;
import com.cafe.infrastructure.redis.CacheNames;
import com.cafe.infrastructure.redis.lock.RedisLock;
import com.cafe.infrastructure.security.dto.LoginUserInfoDto;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {
    // 주문 생성/조회/취소를 담당한다. 포인트 변경과 주문 저장은 하나의 트랜잭션으로 묶는다.

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MenuReader menuReader;
    private final PointService pointService;
    private final OrderNumberGenerator orderNumberGenerator;
    private final MemberReader memberReader;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    @RedisLock(key = "'lock:order:member:' + #loginUser.id()", waitTime = 3, leaseTime = 5)
    @CacheEvict(cacheNames = CacheNames.POPULAR_MENUS, allEntries = true)
    public OrderCreateResponse createOrder(OrderCreateRequest request, LoginUserInfoDto loginUser) {
        // 같은 사용자의 동시 주문은 Redis 분산락으로 먼저 직렬화하고, 포인트는 DB row lock으로 한 번 더 보호한다.
        Member member = memberReader.findById(loginUser.id());
        Map<Long, Integer> quantityByMenuId = getQuantityByMenuId(request);
        List<OrderLine> orderLines = getOrderLines(quantityByMenuId);
        long totalAmount = orderLines.stream()
                .mapToLong(OrderLine::totalPrice)
                .sum();

        // 포인트 차감이 실패하면 주문 저장까지 진행하지 않는다.
        long afterPoint = pointService.usePoint(member.getId(), totalAmount);

        Order order = orderRepository.save(Order.builder()
                .orderNumber(generateOrderNumber())
                .memberId(member.getId())
                .totalAmount(totalAmount)
                .build());

        List<OrderItem> orderItems = orderItemRepository.saveAll(orderLines.stream()
                .map(orderLine -> OrderItem.builder()
                        .orderId(order.getId())
                        .menuId(orderLine.menu().getId())
                        .menuName(orderLine.menu().getName())
                        .menuPrice(orderLine.menu().getPrice())
                        .quantity(orderLine.quantity())
                        .build())
                .toList());

        // 트랜잭션 커밋 이후 Kafka로 전달될 Spring 이벤트다.
        eventPublisher.publishEvent(OrderPaidEvent.of(order, orderItems));

        return OrderCreateResponse.of(order, afterPoint, orderItems);
    }

    public OrderGetResponse getOrder(String orderNumber, LoginUserInfoDto loginUser) {
        // 주문번호로 단건 조회하되, 자신의 주문만 볼 수 있게 소유자를 검증한다.
        Member member = memberReader.findById(loginUser.id());
        Order order = findOrderByOrderNumber(orderNumber);
        validateOrderOwner(order, member.getId());

        List<OrderItem> orderItems = orderItemRepository.findAllByOrderIdOrderByIdAsc(order.getId());
        return OrderGetResponse.of(order, orderItems);
    }

    public OrderSliceResponse getMyOrders(LoginUserInfoDto loginUser, Pageable pageable) {
        // 주문 목록은 Slice로 조회하고, 주문 상세는 orderId IN 쿼리로 한 번에 가져와 N+1을 피한다.
        Member member = memberReader.findById(loginUser.id());
        Slice<Order> orders = orderRepository.findAllByMemberIdOrderByOrderedAtDesc(member.getId(), pageable);
        List<Long> orderIds = orders.getContent().stream()
                .map(Order::getId)
                .toList();
        Map<Long, List<OrderItem>> orderItemsByOrderId = orderItemRepository.findAllByOrderIdInOrderByOrderIdAscIdAsc(orderIds)
                .stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId, LinkedHashMap::new, Collectors.toList()));

        Slice<OrderGetResponse> responses = orders.map(order -> OrderGetResponse.of(
                order,
                orderItemsByOrderId.getOrDefault(order.getId(), List.of())
        ));
        return OrderSliceResponse.from(responses);
    }

    @Transactional
    @RedisLock(key = "'lock:order:member:' + #loginUser.id()", waitTime = 3, leaseTime = 5)
    @CacheEvict(cacheNames = CacheNames.POPULAR_MENUS, allEntries = true)
    public OrderCancelResponse cancelOrder(String orderNumber, LoginUserInfoDto loginUser) {
        // 취소는 주문 row를 잠근 뒤 상태를 확인하고 포인트를 환불한다.
        Member member = memberReader.findById(loginUser.id());
        Order order = findOrderByOrderNumberForUpdate(orderNumber);
        validateOrderOwner(order, member.getId());
        validateCancelable(order);

        long afterPoint = pointService.refundPoint(member.getId(), order.getTotalAmount());

        order.cancel();

        List<OrderItem> orderItems = orderItemRepository.findAllByOrderIdOrderByIdAsc(order.getId());
        eventPublisher.publishEvent(OrderCanceledEvent.of(order, orderItems));

        return OrderCancelResponse.of(order, afterPoint, orderItems);
    }

    private Map<Long, Integer> getQuantityByMenuId(OrderCreateRequest request) {
        // 같은 메뉴가 여러 번 들어오면 수량을 합산해 하나의 주문 라인으로 만든다.
        if (request.items() == null || request.items().isEmpty()) {
            throw new OrderException(OrderErrorCode.EMPTY_ORDER_ITEMS);
        }

        Map<Long, Integer> quantityByMenuId = new LinkedHashMap<>();
        for (OrderCreateRequest.Item item : request.items()) {
            validateOrderItem(item);
            // Map.merge는 같은 menuId가 이미 있으면 기존 수량에 새 수량을 더한다.
            quantityByMenuId.merge(item.menuId(), item.quantity(), Integer::sum);
        }

        return quantityByMenuId;
    }

    private void validateOrderItem(OrderCreateRequest.Item item) {
        if (item == null || item.menuId() == null) {
            throw new OrderException(OrderErrorCode.ORDER_MENU_NOT_FOUND);
        }
        if (item.quantity() <= 0) {
            throw new OrderException(OrderErrorCode.INVALID_ORDER_QUANTITY);
        }
    }

    private List<OrderLine> getOrderLines(Map<Long, Integer> quantityByMenuId) {
        // 서버가 메뉴 가격을 직접 조회해 총액을 계산한다.
        List<Menu> menus = menuReader.findAllByIds(quantityByMenuId.keySet());
        if (menus.size() != quantityByMenuId.size()) {
            throw new OrderException(OrderErrorCode.ORDER_MENU_NOT_FOUND);
        }

        List<OrderLine> orderLines = new ArrayList<>();

        for (Menu menu : menus) {
            int quantity = quantityByMenuId.get(menu.getId());
            validateOrderableMenu(menu);
            orderLines.add(new OrderLine(
                    menu,
                    quantity,
                    (long) menu.getPrice() * quantity
            ));
        }

        return orderLines;
    }

    private void validateOrderableMenu(Menu menu) {
        if (menu.getStatus() != MenuStatus.ACTIVE) {
            throw new OrderException(OrderErrorCode.NOT_ORDERABLE_MENU);
        }
    }

    private Order findOrderByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderException(OrderErrorCode.ORDER_NOT_FOUND));
    }

    private Order findOrderByOrderNumberForUpdate(String orderNumber) {
        return orderRepository.findWithLockByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderException(OrderErrorCode.ORDER_NOT_FOUND));
    }

    private String generateOrderNumber() {
        // 랜덤 6자리 충돌 가능성이 낮지만, 생성 직후 몇 번은 DB 중복 여부를 확인한다.
        for (int count = 0; count < 5; count++) {
            String orderNumber = orderNumberGenerator.generate();
            if (!orderRepository.existsByOrderNumber(orderNumber)) {
                return orderNumber;
            }
        }
        return orderNumberGenerator.generate();
    }

    private void validateOrderOwner(Order order, Long memberId) {
        if (!order.getMemberId().equals(memberId)) {
            throw new OrderException(OrderErrorCode.FORBIDDEN_ORDER_ACCESS);
        }
    }

    private void validateCancelable(Order order) {
        if (!order.isPaid()) {
            throw new OrderException(OrderErrorCode.NOT_CANCELABLE_ORDER);
        }
    }



    private record OrderLine(Menu menu, int quantity, long totalPrice) {
        // OrderItem을 저장하기 전, 메뉴 객체와 수량/금액을 묶어두는 내부 계산 모델이다.
    }
}
