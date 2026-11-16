import argparse
import json
import os
import sys

INTENTS_FILE = os.path.join(os.path.dirname(__file__), "golden_intents.jsonl")
FEYNMAN_FILE = os.path.join(os.path.dirname(__file__), "golden_feynman_rubrics.jsonl")


def load_jsonl(path):
    if not os.path.exists(path):
        return []
    with open(path, "r", encoding="utf-8") as f:
        return [json.loads(line) for line in f if line.strip()]


def print_summary():
    intents = load_jsonl(INTENTS_FILE)
    feynman = load_jsonl(FEYNMAN_FILE)

    print("\n" + "=" * 60)
    print(" CADENCE EVALUATION SUITE AUDIT")
    print("=" * 60)
    print(f"Total Golden Intent Cases   : {len(intents)}")

    cats = {}
    for item in intents:
        c = item.get("category", "unknown")
        cats[c] = cats.get(c, 0) + 1
    for cat, count in cats.items():
        print(f"  • {cat:<24}: {count}")

    print(f"\nTotal Golden Feynman Cases  : {len(feynman)}")
    grades = {}
    for item in feynman:
        g = item.get("expected_grade", 0)
        grades[g] = grades.get(g, 0) + 1
    for g in sorted(grades.keys()):
        print(f"  • Grade {g:<22}: {grades[g]}")
    print("=" * 60 + "\n")


def list_intents(category=None):
    intents = load_jsonl(INTENTS_FILE)
    if category:
        intents = [i for i in intents if i.get("category") == category]

    print(f"\n--- Golden Intents ({len(intents)} items) ---")
    for item in intents:
        verified = "✓" if item.get("verified_by_human") else "✗"
        print(f"[{verified}] {item['id']} ({item.get('category')}): \"{item['user_prompt']}\"")


def list_feynman(grade=None):
    feynman = load_jsonl(FEYNMAN_FILE)
    if grade:
        feynman = [f for f in feynman if f.get("expected_grade") == grade]

    print(f"\n--- Golden Feynman Rubrics ({len(feynman)} items) ---")
    for item in feynman:
        verified = "✓" if item.get("verified_by_human") else "✗"
        print(f"[{verified}] {item['id']} [Grade {item['expected_grade']}]: \"{item['card_title']}\"")
        print(f"      Transcript: \"{item['transcript'][:70]}...\"")


def main():
    parser = argparse.ArgumentParser(description="Cadence Evaluation Suite Verification CLI")
    parser.add_argument("--summary", action="store_true", help="Print summary of evaluation datasets")
    parser.add_argument("--list-intents", action="store_true", help="List all golden intent cases")
    parser.add_argument("--category", type=str, help="Filter intents by category")
    parser.add_argument("--list-feynman", action="store_true", help="List all golden Feynman cases")
    parser.add_argument("--grade", type=int, help="Filter Feynman cases by expected grade (1..4)")

    args = parser.parse_args()

    if args.list_intents:
        list_intents(args.category)
    elif args.list_feynman:
        list_feynman(args.grade)
    else:
        print_summary()


if __name__ == "__main__":
    main()
