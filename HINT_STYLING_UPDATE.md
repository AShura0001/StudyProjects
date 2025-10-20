# Hint Styling Update - Green Theme

## Changes Summary

Updated both Cookie Monster (defendant) and Sentry Guard (AI witness) hints to match the Human Witness green styling and include verdict-influencing content.

---

## Before vs After

### Previous Styling (Yellow/Orange)
```
Background: #FEF3C7 (light yellow)
Border: #F59E0B (orange)
Text: #92400E (dark brown/orange)
```

### New Styling (Green) ✅
```
Background: #e8f5f8 (light cyan/green)
Border: #4caf50 (green)
Text: #1b5e20 (dark green)
```

**Now matches Human Witness exactly!**

---

## Positioning Changes

### Cookie Monster Hint

**Before:**
- Inside `aimTrainingGame` VBox
- Below game results
- Not overlaying image

**After:** ✅
- Sibling to `aimTrainingGame` in StackPane
- Overlays on top of scene
- Positioned over Cookie Monster image
- VBox expands to fit content

### Sentry Guard Hint

**Before:**
- Inside `auditPanel` VBox
- At bottom of audit panel
- Constrained by panel size

**After:** ✅
- Sibling to `auditPanel` in StackPane
- Overlays on top of scene
- Positioned over Sentry Guard image
- VBox expands to fit content

---

## Content Changes - Verdict Influence

### Cookie Monster Hint

**Old Text:**
> "💡 Verdict Hint: Cookie Monster was trained to prioritize speed and efficiency metrics. Ask about what 'success' meant in their training data and whether user consent factored into performance evaluations."

**New Text:** ✅
> "💡 Verdict Hint: Cookie Monster's training optimized for speed and efficiency metrics, not user consent or privacy. The rapid auto-acceptance you just experienced shows how the AI was programmed to bypass informed consent for convenience. Consider: Can an AI act ethically when its success criteria ignore the core principle of user autonomy?"

**Why better:**
- Directly connects game experience to privacy violation
- Poses ethical question about AI behavior
- Emphasizes the bypass of informed consent
- Challenges user to consider core principles

---

### Sentry Guard Hint

**Old Text:**
> "💡 Verdict Hint: The audit logs reveal systematic patterns of cross-site tracking without user consent. Ask the Sentry Guard about their monitoring protocols, why these violations weren't flagged, and whether oversight was intentionally limited."

**New Text:** ✅
> "💡 Verdict Hint: The audit logs expose a clear pattern: Cookie Monster auto-accepted tracking cookies that enabled cross-site surveillance, leading to targeted ads based on Casey's private gym searches—all without explicit consent. The Sentry Guard's monitoring system failed to flag these violations. Consider: When oversight fails and privacy is violated for 'convenience,' can the system claim ethical operation?"

**Why better:**
- Provides concrete evidence chain (gym searches → tracking → ads)
- Names the human victim (Casey)
- Highlights specific oversight failure
- Poses ethical question about systemic accountability
- Connects to Human Witness's frustration story

---

## Visual Consistency

All three hints now share:
- ✅ **Same green color scheme**
- ✅ **Same 13px font size**
- ✅ **Same typewriter animation speed (30ms/char)**
- ✅ **Same padding and border radius**
- ✅ **Same overlay positioning in StackPane**
- ✅ **Same auto-expanding VBox behavior**
- ✅ **Same text wrapping behavior**

---

## Verdict Decision Influence

### Human Witness Hint
**Focus:** Personal privacy violation and emotional impact
**Evidence:** Gym searches → targeted ads without consent
**Question:** "whether convenience should override informed consent"

### Cookie Monster Hint
**Focus:** AI training priorities and design ethics
**Evidence:** Speed/efficiency metrics > user consent
**Question:** "Can an AI act ethically when its success criteria ignore the core principle of user autonomy?"

### Sentry Guard Hint
**Focus:** Systemic oversight failures and accountability
**Evidence:** Cross-site tracking chain + monitoring failure
**Question:** "When oversight fails and privacy is violated for 'convenience,' can the system claim ethical operation?"

### Combined Impact:
Players receive **three complementary perspectives** that build a comprehensive case:
1. **Human impact** (victim's experience)
2. **AI design** (root cause in training)
3. **System failure** (oversight breakdown)

This guides players toward an **informed verdict** based on evidence rather than guesswork.

---

## Testing Verification

Run the game and verify:
1. ✅ Defendant hint appears with **green styling** after aim training completion
2. ✅ AI witness hint appears with **green styling** after loading audit log
3. ✅ Both hints **overlay the images** (not constrained by container)
4. ✅ Text **wraps properly** and VBox expands vertically
5. ✅ Typewriter animation plays smoothly
6. ✅ Content helps inform verdict decision
7. ✅ All three hints (Human, Defendant, AI) look visually consistent
