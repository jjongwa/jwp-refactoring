package kitchenpos.tablegroup.application;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import kitchenpos.common.OrderStatus;
import kitchenpos.order.repository.OrderRepository;
import kitchenpos.table.domain.OrderTable;
import kitchenpos.table.domain.OrderTables;
import kitchenpos.table.repository.OrderTableRepository;
import kitchenpos.tablegroup.domain.TableGroup;
import kitchenpos.tablegroup.dto.TableGroupCreateRequest;
import kitchenpos.tablegroup.repository.TableGroupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TableGroupService {
    private final OrderRepository orderRepository;
    private final OrderTableRepository orderTableRepository;
    private final TableGroupRepository tableGroupRepository;

    public TableGroupService(
            final OrderRepository orderRepository,
            final OrderTableRepository orderTableRepository,
            final TableGroupRepository tableGroupRepository
    ) {
        this.orderRepository = orderRepository;
        this.orderTableRepository = orderTableRepository;
        this.tableGroupRepository = tableGroupRepository;
    }

    @Transactional
    public Long create(final TableGroupCreateRequest request) {
        final List<Long> orderTableIds = request.getTableIds();

        final OrderTables orderTables = new OrderTables(orderTableRepository.findAllByIdIn(orderTableIds));
        orderTables.checkTableSizeIsEqual(orderTableIds.size());
        orderTables.checkEmptyAndTableGroups();

        final TableGroup savedTableGroup = tableGroupRepository.save(new TableGroup());

        for (final OrderTable orderTable : orderTables.getTables()) {
            orderTable.addGroup(savedTableGroup.getId());
            orderTableRepository.save(orderTable);
        }

        return savedTableGroup.getId();
    }

    @Transactional
    public void ungroup(final Long tableGroupId) {
        final OrderTables orderTables = new OrderTables(orderTableRepository.findAllByGroupId(tableGroupId));

        final List<Long> orderTableIds = orderTables.getTables().stream()
                .map(OrderTable::getId)
                .collect(Collectors.toList());

        if (orderRepository.existsByOrderTableIdInAndOrderStatusIn(
                orderTableIds, Arrays.asList(OrderStatus.COOKING.name(), OrderStatus.MEAL.name()))) {
            throw new IllegalArgumentException("주문 상태가 완료되지 않았습니다.");
        }

        for (final OrderTable orderTable : orderTables.getTables()) {
            orderTable.unGroup();
            orderTableRepository.save(orderTable);
        }
    }
}
