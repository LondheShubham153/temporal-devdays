from dataclasses import dataclass


@dataclass
class OrderInput:
    order_id: str
    address: str
    scenario: str = "happy"  # happy | approval | approval_timeout | bad_card | flaky_card | ship_fails
    pace: float = 0.0  # 0 = instant (tests), 1 = demo speed, 2.5 = slow; scales the simulated waits


@dataclass
class OrderOutput:
    status: str
    tracking_id: str
    address: str


@dataclass
class OrderStatus:
    step: str
    address: str
