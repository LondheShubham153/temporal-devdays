import asyncio

from temporalio import activity
from temporalio.exceptions import ApplicationError

from models import OrderInput

calls: list[str] = []  # lets the tests see what ran
charged: set[str] = set()  # orders whose card was charged, so the refund can tell
SHIP_SECONDS = {"table top": 3, "table legs": 4, "keypad": 5}  # the three items ship in parallel and finish at different times


async def pause(pace: float, seconds: float) -> None:
    await asyncio.sleep(seconds * pace)  # simulated latency: zero in tests, a few seconds in the demo


@activity.defn
async def check_stock(order: OrderInput) -> list[str]:
    await pause(order.pace, 2)
    calls.append("check_stock")
    return ["table top", "table legs", "keypad"]


@activity.defn
async def reserve_stock(order: OrderInput) -> None:
    await pause(order.pace, 2)
    calls.append("reserve_stock")


@activity.defn
async def release_stock(order: OrderInput) -> None:
    await pause(order.pace, 2)
    calls.append("release_stock")


@activity.defn
async def charge(order: OrderInput) -> None:
    calls.append("charge")
    flaky = order.scenario == "flaky_card" and activity.info().attempt < 3
    await pause(order.pace, 1 if flaky or order.scenario == "bad_card" else 3)
    if order.scenario == "bad_card":
        raise ApplicationError("card declined", non_retryable=True)
    if flaky:
        raise RuntimeError("payment API unavailable")
    charged.add(order.order_id)


@activity.defn
async def refund_charge(order: OrderInput) -> None:
    await pause(order.pace, 2)
    if order.order_id in charged:   # safe to run when nothing was charged, or twice
        charged.discard(order.order_id)
        calls.append("refund_charge")
    else:
        calls.append("refund_skipped")


@activity.defn
async def ship_item(order_id: str, item: str, scenario: str, pace: float = 0.0) -> None:
    await pause(pace, SHIP_SECONDS.get(item, 3))
    if scenario == "ship_fails" and item == "keypad":
        calls.append(f"ship_failed:{item}")
        raise ApplicationError("no courier available", non_retryable=True)
    calls.append(f"ship:{item}")
