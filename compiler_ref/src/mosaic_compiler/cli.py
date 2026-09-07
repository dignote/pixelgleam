from __future__ import annotations
import argparse
import json
from pathlib import Path
from .validator import validate_artwork


def main() -> None:
    p = argparse.ArgumentParser(prog="pixelgleam-mosaic")
    sub = p.add_subparsers(dest="cmd", required=True)
    v = sub.add_parser("validate", help="Validate a frozen artwork package")
    v.add_argument("package", type=Path)
    v.add_argument("--expected-qa", type=Path)
    v.add_argument("--master", type=Path)
    v.add_argument("--report", type=Path)
    args = p.parse_args()

    if args.cmd == "validate":
        result = validate_artwork(args.package, args.expected_qa, args.master)
        payload = result.to_dict()
        text = json.dumps(payload, indent=2, ensure_ascii=False)
        print(text)
        if args.report:
            args.report.parent.mkdir(parents=True, exist_ok=True)
            args.report.write_text(text + "\n", encoding="utf-8")
        raise SystemExit(0 if result.status == "PASS" else 2)

if __name__ == "__main__":
    main()
