package demo;

import demo.Models.OrderInput;
import demo.Models.OrderOutput;
import demo.Models.OrderStatus;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.ChildWorkflowFailure;
import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class OrderWorkflowImpl implements OrderWorkflow {
    private String step = "starting";
    private String address = "";
    private boolean approved = false;

    @Override
    public OrderOutput run(OrderInput order) {
        address = order.address();
        InventoryActivities inventory = Workflow.newActivityStub(InventoryActivities.class, Pace.options("inventory", order.pace()));
        PaymentActivities payments = Workflow.newActivityStub(PaymentActivities.class, Pace.options("payments", order.pace()));
        step = "checking stock";
        List<String> items = inventory.checkStock(order);

        step = "reserving stock";
        List<Runnable> undo = new ArrayList<>(List.of(() -> inventory.releaseStock(order)));  // registered before the step
        inventory.reserveStock(order);

        if (order.scenario().equals("approval") || order.scenario().equals("approval_timeout")) {
            step = "waiting for approval";
            Duration patience = order.scenario().equals("approval") ? Duration.ofHours(1) : Pace.seconds(5 + 15 * order.pace());
            if (!Workflow.await(patience, () -> approved)) {
                return cancel(order, undo, "approval timed out");
            }
        }

        step = "charging card";
        undo.add(() -> payments.refundCharge(order));  // registered before the step: a charge can land even if the call fails
        try {
            payments.charge(order);
        } catch (ActivityFailure e) {
            return cancel(order, undo, "card declined");
        }

        step = "shipping";
        ShipmentWorkflow shipment = Workflow.newChildWorkflowStub(ShipmentWorkflow.class,
            ChildWorkflowOptions.newBuilder().setWorkflowId("ship-" + order.orderId()).build());
        String trackingId;
        try {
            trackingId = shipment.run(order.orderId(), address, items, order.scenario(), order.pace());
        } catch (ChildWorkflowFailure e) {
            return cancel(order, undo, "shipping failed");
        }
        step = "done";
        return new OrderOutput("shipped", trackingId, address);
    }

    private OrderOutput cancel(OrderInput order, List<Runnable> undo, String reason) {
        step = "cancelling";
        for (int i = undo.size() - 1; i >= 0; i--) {
            undo.get(i).run();
        }
        step = "cancelled";
        return new OrderOutput("cancelled: " + reason, "", address);
    }

    @Override
    public void approve() {
        approved = true;
    }

    @Override
    public String changeAddress(String address) {
        this.address = address;
        return address;
    }

    @Override
    public void checkAddress(String address) {
        if (address.strip().length() < 5) {
            throw new IllegalArgumentException("address is too short");
        }
    }

    @Override
    public OrderStatus status() {
        return new OrderStatus(step, address);
    }
}
