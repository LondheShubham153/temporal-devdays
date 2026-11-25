.PHONY: setup check check-live

setup:
	cd python && uv sync

check:
	./scripts/check.sh

check-live:
	./scripts/check.sh --live
