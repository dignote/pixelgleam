# Google AI Studio Handoff

Google AI Studio should receive only frozen artwork packages that have passed the Visual Asset Factory.

AI Studio responsibilities:
- load artwork package
- render numbered/uncompleted state
- perform Cell-ID hit lookup
- resolve Region completion
- animate G02/transient effects
- reveal settled_texture by completed Cell/Region mask
- persist completed Region IDs

AI Studio must NOT:
- regenerate artwork
- infer semantic regions from final pixels
- flood-fill the settled texture
- create a new square grid
- fill negative-space gaps
- reinterpret topology
