# PIXELGLEAM — CODEX / VS CODE STRICT INTEGRATION PROMPT
## Integrate: PIXELGLEAM_ALL_7_PRODUCTION_DROPIN_PACK.zip
## Mode: Inspect → Plan → Approval → Integrate → Verify

You are working locally in **Codex + VS Code** on the PixelGleam Android project.

The artwork production pack to integrate is:

```text
PIXELGLEAM_ALL_7_PRODUCTION_DROPIN_PACK.zip
```

This pack has already passed the external artwork/topology QA workflow and must be treated as **authoritative content input**.

Your task is **integration only**.

Do NOT regenerate artwork.
Do NOT regenerate topology.
Do NOT alter Cell IDs.
Do NOT alter Region IDs.
Do NOT rename artwork IDs.
Do NOT rebuild subject masks.
Do NOT replace settled textures.
Do NOT "improve" the artwork package unless a concrete compatibility defect is proven.

---

# 0. FIRST ACTION — INSPECT REPOSITORY BEFORE EDITING

Before changing any file:

1. Read the project documentation relevant to PixelGleam.
2. Inspect the current Git state:

```bash
git status --short
git diff --stat
```

3. Inspect the current artwork assets structure.
4. Inspect the current catalog implementation.
5. Inspect whether runtime uses:
   - `catalog.json`
   - build-time generated catalog
   - `ArtworkRepository`
   - dynamic scanner fallback
   - static catalog fallback
6. Inspect current Gallery category/title metadata flow.
7. Inspect current Room/progress identity usage.
8. Inspect Daily Challenge artwork assignment flow.
9. Inspect existing tests related to artwork/catalog/gallery.
10. Inspect available Gradle wrapper/tasks before choosing commands.

Do not modify anything yet.

---

# 1. SOURCE OF TRUTH

Use this priority:

```text
1. current repository code
2. current repository configuration
3. production artwork pack
4. approved PixelGleam naming/category contract
5. previous chat/history only as secondary context
```

If repository assumptions conflict with the pack contract, report the conflict before editing.

---

# 2. ARTWORK PACK CONTRACT

The production pack contains 7 new artworks:

```text
sakura_cat_window
rainy_bookshop
matcha_tea_set
forest_witch
hummingbird_hibiscus
butterfly_bloom
aurora_mandala
```

Expected canonical categories:

```text
sakura_cat_window
→ animals_friends
→ Animals & Friends

rainy_bookshop
→ cozy_scenes
→ Cozy Scenes

matcha_tea_set
→ food_treats
→ Food & Treats

forest_witch
→ characters
→ Characters

hummingbird_hibiscus
→ flowers_nature
→ Flowers & Nature

butterfly_bloom
→ ornamental_patterns
→ Ornamental Patterns

aurora_mandala
→ geometric_mandalas
→ Geometric Mandalas
```

These are **new semantic persistent IDs**.

Do not prefix them with A/B/C batch codes.
Do not append version suffixes.

---

# 3. REQUIRED RUNTIME FILES PER ARTWORK

Expected required runtime files:

```text
artwork.json
cell_id_map.png
subject_mask.png
safe_anchors.json
thumbnail.webp
settled_texture.png
```

Optional debug/reference:

```text
region_id_map.png
master.png
```

Do not make optional debug files mandatory unless current runtime explicitly requires them.

---

# 4. LOCKED CONTENT RULES

The pack follows:

```text
100% full-canvas playable
hidden topology
4-connected Cells
4-connected Regions
maximal same-color connected Regions
safe anchors
<= 15 gameplay colors
```

Do not convert full-white `subject_mask.png` back to subject-only masks.

Do not split or merge Regions during integration.

---

# 5. PERSISTENCE SAFETY

Existing shipped artwork IDs must remain unchanged.

New artwork IDs above are new identities.

Never:
- reuse an existing artwork ID;
- rename old artwork IDs;
- change Room progress keys;
- rewrite old completedRegionIds;
- map a new artwork onto an old artwork ID.

Verify that progress remains keyed only by stable `artwork.id`.

---

# 6. GALLERY METADATA SAFETY

These 7 artworks should be visible as normal production content.

Expected metadata:

```text
lifecycle = active
galleryVisible = true
```

Gallery categories must derive from canonical metadata.

Do not add another permanent hardcoded category mapping if the repository already has canonical metadata support.

If a legacy mapper remains, treat it only as migration fallback.

---

# 7. CATALOG INTEGRATION

Preferred release architecture:

```text
artwork packages
→ build-time catalog generator
→ catalog.json
→ release runtime reads catalog.json
```

Release startup must NOT re-run full topology validation across the entire artwork library.

If the repo already has a catalog generator:
- use it;
- do not manually maintain duplicate catalog metadata.

If the repo does not yet have a working generator:
- report that clearly in the plan;
- integrate using the safest current path;
- do not silently invent a second source of truth.

After adding the pack, verify:

```text
actual artwork packages
==
catalog entries
```

for:
- id
- title
- categoryId
- categoryLabel
- cell count
- region count
- thumbnail path
- asset folder
- visibility state

---

# 8. DO NOT TOUCH LEGACY CONTENT UNLESS REQUIRED

This integration task is not a general cleanup task.

Do not modify existing production artwork unless required for compatibility.

Do not delete:
- legacy IDs
- benchmark assets
- old progress

unless the current approved code already defines their retirement behavior.

---

# 9. DAILY CHALLENGE SAFETY

Adding these 7 artworks must not break historical/current Daily assignments.

Expected architecture:

```text
HRW / rendezvous hashing
+
persisted assignment lock
```

Verify:
- persisted Daily artwork ID remains unchanged;
- newly added artwork does not replace an already persisted date assignment;
- new artworks are eligible only according to the current Daily eligibility rules.

Do not reintroduce:

```text
seed % catalog.size
```

or any index-based selection logic.

---

# 10. FILE COPY RULE

Before copying:

1. determine the exact current assets root;
2. determine naming/folder conventions;
3. check for ID collision;
4. check for duplicate folder;
5. check whether current build expects generated or checked-in assets.

Then copy the 7 artwork folders **without modifying their internal IDs**.

Do not overwrite an existing folder with the same ID without first reporting it.

---

# 11. PLAN-FIRST RESPONSE REQUIRED

Before implementation, return:

```text
STATUS: PLAN READY

CURRENT ARTWORK ASSET ROOT:
...

CURRENT CATALOG SOURCE:
...

CURRENT GALLERY METADATA SOURCE:
...

CURRENT RELEASE STARTUP PATH:
...

CURRENT PERSISTENCE KEY:
...

CURRENT DAILY ASSIGNMENT PATH:
...

PACK COMPATIBILITY:
PASS / REWORK / BLOCKED
details:

ID COLLISIONS:
...

FILES/FOLDERS EXPECTED TO ADD:
...

FILES EXPECTED TO MODIFY:
...

CATALOG UPDATE METHOD:
...

TEST PLAN:
...

RISKS:
...

IMPLEMENTATION PHASES:
1.
2.
3.
...

STATUS: WAITING FOR APPROVAL
```

Then STOP.

Do not edit until approved.

---

# 12. IMPLEMENTATION PHASES AFTER APPROVAL

## Phase 1 — Add artwork assets only

Copy the 7 folders to the correct Android assets location.

Do not change topology data.

Then run:

```bash
git status --short
git diff --stat
```

Report and stop if unexpected files changed.

---

## Phase 2 — Regenerate / update catalog

Use the existing repository-approved generator.

Verify all 7 new IDs appear exactly once.

Verify actual `cells.size` / `regions.size` are used.

Do not hardcode counts manually.

---

## Phase 3 — Gallery integration verification

Verify the 7 artworks:
- appear in Gallery;
- use correct titles;
- appear in correct canonical categories;
- have thumbnails;
- do not create internal/legacy category chips.

---

## Phase 4 — Gameplay smoke verification

Open at least one representative new artwork from:
- illustrative scene
- character
- ornamental/geometric

Recommended:

```text
sakura_cat_window
forest_witch
aurora_mandala
```

Verify:
- image loads
- grayscale unfinished state loads
- palette works
- correct tap completes Region
- wrong color does not mutate
- pan/zoom works
- hint works
- save/reload works
- completion state persists

---

## Phase 5 — Targeted tests

Use only actual Gradle tasks present in repo.

Prefer targeted tests first, for example:

```bash
./gradlew :app:testDebugUnitTest --tests "*Catalog*"
./gradlew :app:testDebugUnitTest --tests "*Gallery*"
```

Use exact existing test names.

Do not invent task names.

---

## Phase 6 — Final build verification

Only after targeted tests PASS:

```bash
./gradlew :app:testDebugUnitTest
```

Then separately:

```bash
./gradlew :app:assembleDebug
```

If the full suite is long, let it complete locally and inspect the actual exit code/report.

Do not claim PASS without current evidence.

---

# 13. REQUIRED ACCEPTANCE GATES

Integration is complete only when:

```text
7/7 ARTWORK FOLDERS ADDED           PASS
7/7 IDS UNIQUE                      PASS
CATALOG PARITY                      PASS
CANONICAL TITLES                    PASS
CANONICAL CATEGORIES                PASS
THUMBNAILS LOAD                     PASS
GALLERY VISIBILITY                  PASS
LEGACY/INTERNAL FILTERING           PASS
PERSISTENCE COMPATIBILITY           PASS
DAILY STABILITY                     PASS
TARGETED TESTS                      PASS
UNIT TEST SUITE                     PASS
ASSEMBLE DEBUG                      PASS
```

---

# 14. DO NOT SILENTLY FIX CONTENT

If any new artwork fails current runtime validation:

Do NOT:
- regenerate IDs;
- remap Regions;
- rewrite topology;
- substitute a different PNG;
- silently reduce validation strictness.

Instead report:

```text
STATUS: REWORK

ARTWORK:
...

COMPATIBILITY FAILURE:
...

EXPECTED CONTRACT:
...

CURRENT CODE EXPECTATION:
...

MINIMAL INTEGRATION-SAFE FIX:
...
```

Then stop.

---

# 15. FINAL REPORT

Create:

```text
PIXELGLEAM_7_ARTWORK_INTEGRATION_REPORT.md
```

Include:

```text
1. repository state before integration
2. assets added
3. IDs added
4. catalog changes
5. category/title mapping
6. gallery verification
7. persistence verification
8. Daily verification
9. gameplay smoke test
10. targeted tests
11. full test result
12. assembleDebug result
13. remaining risks
14. final PASS / REWORK / BLOCKED
```

---

# 16. MOST IMPORTANT RULE

Treat the production artwork pack as immutable content.

Your job is to integrate it safely into the Android project, not to redesign it.
