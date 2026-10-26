"""Проверки содержательных сценариев без пользовательских классов."""
from copy import deepcopy
import json
from pathlib import Path
from main import make_protocol, validate_measurements

def run():
    original = json.loads(Path(__file__).with_name("example.json").read_text(encoding="utf-8"))
    assert make_protocol(original, "B1")["proposal"] == "accepted"
    assert make_protocol(original, "B1")["status"] == "awaiting_approval"
    changed = deepcopy(original)
    changed["measurements"][0]["value"] = 20
    assert make_protocol(changed, "B1")["proposal"] == "rejected"
    changed = deepcopy(original)
    changed["measurements"].pop()
    assert make_protocol(changed, "B1")["proposal"] == "incomplete"
    changed = deepcopy(original)
    changed["measurements"].append(dict(changed["measurements"][0], id="M5"))
    assert make_protocol(changed, "B1")["proposal"] == "incomplete"
    changed = deepcopy(original)
    changed["measurements"][0]["unit"] = "V"
    assert make_protocol(changed, "B1")["proposal"] == "rejected"
    changed = deepcopy(original)
    changed["protocols"] = [{"id":"PR1","batch_id":"B1","employee_id":"E2", "decision":"accepted","approved_at":"2026-11-03T12:00:00+03:00"}]
    assert make_protocol(changed, "B1")["status"] == "accepted"
    changed["protocols"][0]["employee_id"] = "E1"
    try:
        make_protocol(changed, "B1")
        raise AssertionError("Нет проверки полномочий")
    except ValueError:
        pass
    assert validate_measurements([dict(original["measurements"][0], value=float("nan"))]) == [0]
    assert validate_measurements([dict(original["measurements"][0], value=True)]) == [0]
    changed = deepcopy(original)
    changed["measurements"][0]["sample_id"] = "missing"
    try:
        make_protocol(changed, "B1")
        raise AssertionError("Нет проверки внешнего ключа")
    except ValueError:
        pass
    changed = deepcopy(original)
    changed["samples"] = []
    changed["measurements"] = []
    assert make_protocol(changed, "B1")["proposal"] == "incomplete"
    print("OK: 11 сценариев, включая отрицательные и граничные")

if __name__ == "__main__":
    run()
