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
import com.cafe.domain.order.repository.OrderItemRepository;
import com.cafe.domain.order.repository.OrderRepository;
import com.cafe.domain.order.support.OrderNumberGenerator;
import com.cafe.domain.point.service.PointService;
import com.cafe.infrastructure.redis.CacheNames;
import com.cafe.infrastructure.redis.lock.RedisLock;
import com.cafe.infrastructure.security.dto.LoginUserInfoDto;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
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

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MenuReader menuReader;
    private final PointService pointService;
    private final OrderNumberGenerator orderNumberGenerator;
    private final MemberReader memberReader;

    @Transactional
    @RedisLock(key = "'lock:order:member:' + #loginUser.id()", waitTime = 3, leaseTime = 5)
    @CacheEvict(cacheNames = CacheNames.POPULAR_MENUS, allEntries = true)
    public OrderCreateResponse createOrder(OrderCreateRequest request, LoginUserInfoDto loginUser) {
        Member member = memberReader.findById(loginUser.id());
        Map<Long, Integer> quantityByMenuId = getQuantityByMenuId(request);
        List<OrderLine> orderLines = getOrderLines(quantityByMenuId);
        long totalAmount = orderLines.stream()
                .mapToLong(OrderLine::totalPrice)
                .sum();

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

        return OrderCreateResponse.of(order, afterPoint, orderItems);
    }

    public OrderGetResponse getOrder(String orderNumber, LoginUserInfoDto loginUser) {
        Member member = memberReader.findById(loginUser.id());
        Order order = findOrderByOrderNumber(orderNumber);
        validateOrderOwner(order, member.getId());

        List<OrderItem> orderItems = orderItemRepository.findAllByOrderIdOrderByIdAsc(order.getId());
        return OrderGetResponse.of(order, orderItems);
    }

    public OrderSliceResponse getMyOrders(LoginUserInfoDto loginUser, Pageable pageable) {
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
        Member member = memberReader.findById(loginUser.id());
        Order order = findOrderByOrderNumberForUpdate(orderNumber);
        validateOrderOwner(order, member.getId());
        validateCancelable(order);

        long afterPoint = pointService.refundPoint(member.getId(), order.getTotalAmount());

        order.cancel();

        List<OrderItem> orderItems = orderItemRepository.findAllByOrderIdOrderByIdAsc(order.getId());
        return OrderCancelResponse.of(order, afterPoint, orderItems);
    }

    private Map<Long, Integer> getQuantityByMenuId(OrderCreateRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new OrderException(OrderErrorCode.EMPTY_ORDER_ITEMS);
        }

        Map<Long, Integer> quantityByMenuId = new LinkedHashMap<>();
        for (OrderCreateRequest.Item item : request.items()) {
            validateOrderItem(item);
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
    }
}
