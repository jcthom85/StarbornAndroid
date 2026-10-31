# Unseen Charlie: puffball exploration interaction

October 31, 2026. Implements the approved unseen-playmate direction in Overgrown Glade, on Android and desktop. Residents/Megan remain unchanged in this pass.

## Player experience

A velvety puffball rolls into the exploration scene, bounces and settles. The ball is drawn directly over the room background. Tapping offers a gentle roll/toss or **Leave it be**. Leaving never advances trust and does not trap movement. There is no Charlie NPC chip, portrait or conventional conversation panel.

The first return is hesitant. Nova tries the name Charlie and a chirp answers from the ferns. Later returns are quicker, with a little overshoot. On the third trust beat, the ball returns with a smooth stone nudged out of the moss. Subsequent play cycles through three short reactions without more items or XP. Rustles, chirps and purrs are described in the inline text; no new audio recordings were added.

The animation is finite, with no idle loop or reaction-time challenge. Screen-shake-disabled settings provide a still, immediately usable ball. Choices flow onto additional lines on narrow layouts. The touch target is 64 dp and the ball and reactions have accessibility semantics. Leaving the room cancels a pending presentation before its reward callback.

## Runtime and save behavior

The interaction uses existing `ms_charlie_trust_1/2/3` milestones and preserves the original 25 + 35 + 60 XP and single comfort stone, with unchanged equipment stats. New guarded events grant each beat once. Old partial saves resume from their established trust level; old completed saves retain their reward without a second payout. The item description now reflects discovering it during play.

Direct legacy Charlie NPC interactions redirect to the ball interaction while in the glade, without opening a portrait. Legacy dialogue records remain for compatibility/testing but the glade no longer exposes Charlie as an NPC. Other room interactions remain available. Playing is rejected outside the glade or while exploration interaction is blocked/loading.

## Verification

- **35 hosted tests passed**, including data integrity, legacy dialogue compatibility and a real-controller regression covering all four existing trust states, repeat play, save/reload, portrait-free legacy redirection and rejection outside the glade.
- **Two desktop Compose tests passed**: leaving without advancing, gentle play, reduced motion, and committing only after the animated return. Initial test attempts needed the production exploration-unblocked lifecycle and matching per-character XP in old-save fixtures; those were corrected. Animation checks explicitly advance the Compose clock.
- Android debug APK assembled; desktop compiled; `git diff --check` passed.
- [Evidence/source hashes](evidence/CHARLIE_PUFFBALL_IMPLEMENTATION_2026-10-31.json) and [isolated interaction screenshot](evidence/charlie_2026-10-31/puffball.png).

The walkthrough is updated and the local guide website rebuilt. No publishing, commit, push or physical Pixel installation was performed. Device acceptance remains: see the ball over the actual glade background, try every trust beat, leave during an animation, return/reload and confirm one reward, then check reduced motion and large text. The screenshot verifies the shared component in isolation; it is not a full Android scene capture.
