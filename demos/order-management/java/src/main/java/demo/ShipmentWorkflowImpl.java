package demo;

import io.temporal.workflow.Async;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import java.util.List;

public class ShipmentWorkflowImpl implements ShipmentWorkflow {
    @Override
    public String run(String orderId, String address, List<String> items, String scenario, double pace) {
        ShippingActivities shipping = Workflow.newActivityStub(ShippingActivities.class, Pace.options("shipping", pace));
        List<Promise<Void>> shipments = items.stream()
            .map(item -> Async.procedure(shipping::shipItem, orderId, item, scenario, pace))
            .toList();
        Promise.allOf(shipments).get();
        return "TRK-" + orderId;
    }
}
