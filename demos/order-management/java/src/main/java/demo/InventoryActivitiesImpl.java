package demo;

import demo.Models.OrderInput;
import java.util.List;

public class InventoryActivitiesImpl implements InventoryActivities {
    @Override
    public List<String> checkStock(OrderInput order) {
        Pace.pause(order.pace(), 2);
        Calls.all.add("check_stock");
        return List.of("table top", "table legs", "keypad");
    }

    @Override
    public void reserveStock(OrderInput order) {
        Pace.pause(order.pace(), 2);
        Calls.all.add("reserve_stock");
    }

    @Override
    public void releaseStock(OrderInput order) {
        Pace.pause(order.pace(), 2);
        Calls.all.add("release_stock");
    }
}
