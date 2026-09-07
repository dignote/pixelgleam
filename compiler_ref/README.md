# PixelGleam Mosaic Topology Compiler V1 — M0/M1

Current scope: golden-fixture validation only. Automatic segmentation/tessellation is intentionally not implemented yet.

## Run Happy Corgi V3 golden validation

```bash
PYTHONPATH=src python -m mosaic_compiler.cli validate \
  <happy_corgi_v3_package> \
  --expected-qa <HAPPY_CORGI_V3_MASTER_DERIVED_QA.json> \
  --master <HAPPY_CORGI_MASTER_CROP.png> \
  --report reports/happy_corgi_v3_validation.json
```

Exit code `0` = PASS, `2` = REWORK.

## M2-M8 execution additions

Reconstruct from the approved V3 semantic/color raster:

```bash
PYTHONPATH=src python -m mosaic_compiler.reconstruct \
  --source-v3 <happy_corgi_v3_dir> \
  --master <HAPPY_CORGI_MASTER_CROP.png> \
  --out outputs/happy_corgi_reconstructed_v1 \
  --target-cells 347
```

The `--auto-color-mask` option is experimental M6 only and is not production-approved.
