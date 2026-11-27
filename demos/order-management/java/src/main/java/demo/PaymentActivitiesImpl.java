package demo;

import demo.Models.OrderInput;
import io.temporal.activity.Activity;
import io.temporal.failure.ApplicationFailure;

public class PaymentActivitiesImpl implements PaymentActivities {
    @Override
    public void charge(OrderInput order) {
        Calls.all.add("charge");
        int attempt = Activity.getExecutionContext().getInfo().getAttempt();
        boolean flaky = order.scenario().equals("flaky_card") && attempt < 3;
        Pace.pause(order.pace(), flaky || order.scenario().equals("bad_card") ? 1 : 3);
        if (order.scenario().equals("bad_card")) {
            throw ApplicationFailure.newNonRetryableFailure("card declined", "CardDeclined");
        }
        if (flaky) {
            throw new RuntimeException("payment API unavailable");
        }
        Calls.charged.add(order.orderId());
    }

    @Override
    public void refundCharge(OrderInput order) {
        Pace.pause(order.pace(), 2);
        if (Calls.charged.remove(order.orderId())) {   // safe to run when nothing was charged, or twice
            Calls.all.add("refund_charge");
        } else {
            Calls.all.add("refund_skipped");
        }
    }
}
