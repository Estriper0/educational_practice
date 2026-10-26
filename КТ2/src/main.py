"""Процедурный прототип регистрации результатов испытаний кабеля."""
import argparse
import json
import math
from pathlib import Path


def valid_measurement(row):
    """Проверка одной записи; единица и пределы проверяются по плану отдельно."""
    return (isinstance(row, dict)
            and all(row.get(k) is not None for k in
                    ("id", "sample_id", "requirement_id", "employee_id", "measured_at", "unit"))
            and isinstance(row.get("value"), (int, float))
            and not isinstance(row.get("value"), bool)
            and math.isfinite(row["value"]))


def validate_measurements(rows):
    """Алгоритм A: линейная проверка формата результатов, O(r)."""
    errors = []
    for index, row in enumerate(rows):
        if not valid_measurement(row):
            errors.append(index)
    return errors


def evaluate(samples, rules, measurements):
    """Алгоритм B: сверка каждого образца с каждой обязательной проверкой.

    Вход предварительно проверен validate_data. Правила уже выбраны для
    версии плана партии, образцы и результаты относятся только к этой партии.
    Уникальная пара образец/требование допускает одну запись в попытке.
    """
    issues = []
    for sample in samples:
        for rule in rules:
            found = []
            for row in measurements:
                if (row["sample_id"] == sample["id"]
                        and row["requirement_id"] == rule["requirement_id"]):
                    found.append(row)
            if len(found) != 1:
                issues.append([sample["id"], rule["requirement_id"], "missing_or_duplicate"])
            else:
                row = found[0]
                if (row["unit"] != rule["unit"]
                        or not rule["lower"] <= row["value"] <= rule["upper"]):
                    issues.append([sample["id"], rule["requirement_id"], "nonconformity"])
    return issues


def validate_data(data):
    """Проверить ключи, ссылки и целостность нормализованных коллекций."""
    tables = ("products", "plans", "requirements", "plan_requirements", "batches",
              "samples", "measurements", "employees", "protocols")
    for table in tables:
        if not isinstance(data.get(table), list):
            raise ValueError("Ожидается список: " + table)
    indexes = {}
    fields = {
        "products": ("id", "name"), "plans": ("id", "name", "revision", "document"),
        "requirements": ("id", "name"),
        "batches": ("id", "product_id", "plan_id", "number"),
        "samples": ("id", "batch_id", "number"),
        "employees": ("id", "name", "role"),
        "measurements": ("id", "sample_id", "requirement_id", "employee_id", "value", "unit", "measured_at"),
        "protocols": ("id", "batch_id", "employee_id", "decision", "approved_at")}
    for table, keys in fields.items():
        indexes[table] = {}
        for row in data[table]:
            if not isinstance(row, dict) or any(row.get(k) is None for k in keys):
                raise ValueError("Неполная запись: " + table)
            if not isinstance(row["id"], str) or not row["id"] or row["id"] in indexes[table]:
                raise ValueError("Пустой или повторный ключ: " + table)
            indexes[table][row["id"]] = row
    for table, field, parent in (
        ("batches", "product_id", "products"), ("batches", "plan_id", "plans"),
        ("samples", "batch_id", "batches"), ("measurements", "sample_id", "samples"),
        ("measurements", "requirement_id", "requirements"),
        ("measurements", "employee_id", "employees"),
        ("protocols", "batch_id", "batches"), ("protocols", "employee_id", "employees")):
        for row in data[table]:
            if row[field] not in indexes[parent]:
                raise ValueError("Нарушена ссылка: " + table + "." + field)
    pairs = set()
    for rule in data["plan_requirements"]:
        if not isinstance(rule, dict) or any(rule.get(k) is None for k in
                ("plan_id", "requirement_id", "lower", "upper", "unit")):
            raise ValueError("Неполное правило плана")
        pair = (rule["plan_id"], rule["requirement_id"])
        if pair in pairs or pair[0] not in indexes["plans"] or pair[1] not in indexes["requirements"]:
            raise ValueError("Неверный ключ правила плана")
        pairs.add(pair)
        if (any(isinstance(rule[k], bool) or not isinstance(rule[k], (int, float))
                or not math.isfinite(rule[k]) for k in ("lower", "upper"))
                or rule["lower"] > rule["upper"]):
            raise ValueError("Некорректные границы")
    errors = validate_measurements(data["measurements"])
    if errors:
        raise ValueError("Ошибки формата измерений: " + str(errors))
    for row in data["measurements"]:
        batch = indexes["batches"][indexes["samples"][row["sample_id"]]["batch_id"]]
        if (batch["plan_id"], row["requirement_id"]) not in pairs:
            raise ValueError("Измерение отсутствует в плане партии")
    if len({p["batch_id"] for p in data["protocols"]}) != len(data["protocols"]):
        raise ValueError("Более одного решения на попытку приёмки")
    for protocol in data["protocols"]:
        if indexes["employees"][protocol["employee_id"]]["role"] != "approver":
            raise ValueError("Решение подписал сотрудник без полномочий")
        if protocol["decision"] not in ("accepted", "rejected") or not protocol["approved_at"]:
            raise ValueError("Некорректное решение")


def make_protocol(data, batch_id):
    """Сформировать проект протокола, сохранив отдельное утверждение человеком."""
    validate_data(data)
    batch = next((b for b in data["batches"] if b["id"] == batch_id), None)
    if batch is None:
        raise ValueError("Партия не найдена")
    samples = [s for s in data["samples"] if s["batch_id"] == batch_id]
    rules = [p for p in data["plan_requirements"] if p["plan_id"] == batch["plan_id"]]
    sample_ids = {s["id"] for s in samples}
    rows = [r for r in data["measurements"] if r["sample_id"] in sample_ids]
    issues = evaluate(samples, rules, rows)
    complete = bool(samples and rules) and not any(i[2] == "missing_or_duplicate" for i in issues)
    proposed = "incomplete" if not complete else ("rejected" if issues else "accepted")
    approval = next((p for p in data["protocols"] if p["batch_id"] == batch_id), None)
    if approval is not None and (proposed == "incomplete" or approval["decision"] != proposed):
        raise ValueError("Решение не соответствует результатам контроля")
    return {"batch_id": batch_id, "proposal": proposed, "issues": issues,
            "status": approval["decision"] if approval else "awaiting_approval",
            "approval": approval}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("input", type=Path)
    parser.add_argument("--batch", default="B1")
    args = parser.parse_args()
    try:
        data = json.loads(args.input.read_text(encoding="utf-8"))
        result = make_protocol(data, args.batch)
    except (OSError, ValueError, KeyError, TypeError) as error:
        parser.exit(2, "Ошибка: " + str(error) + "\n")
    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
