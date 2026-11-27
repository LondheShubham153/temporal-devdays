package demo;

import io.temporal.failure.ApplicationFailure;
import java.util.Map;

public class ShippingActivitiesImpl implements ShippingActivities {
    // the three items ship in parallel and finish at different times
    static final Map<String, Integer> SHIP_SECONDS = Map.of("table top", 3, "table legs", 4, "keypad", 5);

    @Override
    public void shipItem(String orderId, String item, String scenario, double pace) {
        Pace.pause(pace, SHIP_SECONDS.getOrDefault(item, 3));
        if (scenario.equals("ship_fails") && item.equals("keypad")) {
            Calls.all.add("ship_failed:" + item);
            throw ApplicationFailure.newNonRetryableFailure("no courier available", "NoCourier");
        }
        Calls.all.add("ship:" + item);
    }
}
