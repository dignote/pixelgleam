# Subject + Void Mask QA Prompt

Audit the supplied artwork, subject mask and void mask.

Check every visible negative-space gap, especially between overlapping body parts, accessories, hair, limbs, petals, leaves, handles and loops.

Rules:
- any background-connected gap that is visibly open must remain non-playable
- no playable bridge may appear only because of dilation, closing or silhouette smoothing
- void always overrides subject
- flag every location where the mask would produce gems in visible empty space

Return PASS only if the playable domain matches the intended subject topology.
