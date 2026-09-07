from pathlib import Path
from mosaic_compiler.validator import validate_artwork

ROOT = Path(__file__).resolve().parents[1]
FIX = ROOT / "fixtures" / "happy_corgi_v3"


def test_happy_corgi_v3_golden_passes():
    r = validate_artwork(
        FIX / "package",
        FIX / "qa" / "expected_qa.json",
        FIX / "qa" / "approved_master.png",
    )
    assert r.status == "PASS", r.blockers
    assert r.geometry["cellCount"] == 347
    assert r.geometry["regionCount"] == 43
    assert r.geometry["coveragePercent"] == 100.0
    assert all(r.checks.values())
