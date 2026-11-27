import asyncio
from datetime import timedelta

from temporalio import workflow
from temporalio.common import RetryPolicy
from temporalio.exceptions import ActivityError, ChildWorkflowError

with workflow.unsafe.imports_passed_through():
    from activities import charge, check_stock, refund_charge, release_stock, reserve_stock, ship_item
    from models import OrderInput, OrderOutput, OrderStatus

def opts(pace: float) -> dict:   # slower demo pace needs a longer timeout and a longer wait between retries
    return dict(
        start_to_close_timeout=timedelta(seconds=5 + 4 * pace),
        retry_policy=RetryPolicy(initial_interval=timedelta(seconds=1 + pace), maximum_attempts=5),
    )


@workflow.defn
class ShipmentWorkflow:
    @workflow.run
    async def run(self, order_id: str, address: str, items: list[str], scenario: str, pace: float = 0.0) -> str:
        await asyncio.gather(*[
            workflow.execute_activity(ship_item, args=[order_id, item, scenario, pace], task_queue="shipping", **opts(pace))
            for item in items
        ])
        return f"TRK-{order_id}"


@workflow.defn
class OrderWorkflow:
    def __init__(self) -> None:
        self.step = "starting"
        self.address = ""
        self.approved = False

    @workflow.run
    async def run(self, order: OrderInput) -> OrderOutput:
        self.address = order.address
        o = opts(order.pace)
        self.step = "checking stock"
        items = await workflow.execute_activity(check_stock, order, task_queue="inventory", **o)

        self.step = "reserving stock"
        undo = [(release_stock, "inventory")]  # registered before the step, as in exercise 10
        await workflow.execute_activity(reserve_stock, order, task_queue="inventory", **o)

        if order.scenario in ("approval", "approval_timeout"):
            self.step = "waiting for approval"
            patience = timedelta(hours=1) if order.scenario == "approval" else timedelta(seconds=5 + 15 * order.pace)
            try:
                await workflow.wait_condition(lambda: self.approved, timeout=patience)
            except asyncio.TimeoutError:
                return await self.cancel(order, undo, "approval timed out", o)

        self.step = "charging card"
        undo.append((refund_charge, "payments"))  # registered before the step: a charge can land even if the call fails
        try:
            await workflow.execute_activity(charge, order, task_queue="payments", **o)
        except ActivityError:
            return await self.cancel(order, undo, "card declined", o)

        self.step = "shipping"
        try:
            tracking_id = await workflow.execute_child_workflow(
                ShipmentWorkflow.run,
                args=[order.order_id, self.address, items, order.scenario, order.pace],
                id=f"ship-{order.order_id}",
            )
        except ChildWorkflowError:
            return await self.cancel(order, undo, "shipping failed", o)
        self.step = "done"
        return OrderOutput("shipped", tracking_id, self.address)

    async def cancel(self, order: OrderInput, undo: list, reason: str, o: dict) -> OrderOutput:
        self.step = "cancelling"
        for step, queue in reversed(undo):
            await workflow.execute_activity(step, order, task_queue=queue, **o)
        self.step = "cancelled"
        return OrderOutput(f"cancelled: {reason}", "", self.address)

    @workflow.signal
    def approve(self) -> None:
        self.approved = True

    @workflow.update
    def change_address(self, address: str) -> str:
        self.address = address
        return address

    @change_address.validator
    def check_address(self, address: str) -> None:
        if len(address.strip()) < 5:
            raise ValueError("address is too short")

    @workflow.query
    def status(self) -> OrderStatus:
        return OrderStatus(self.step, self.address)
