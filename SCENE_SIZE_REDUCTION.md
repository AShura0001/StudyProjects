# Cookie Monster Hint Position Fix

## Change Summary

**Problem:** The hint was appearing in a separate area below the image instead of overlaying on top of it.

**Solution:** Restructured the FXML to wrap the image and hint together in a StackPane.

---

## Layout Structure

### Before (Incorrect):
```
VBox
├── ImageView (Cookie Monster image)
└── StackPane
    ├── aimTrainingGame
    └── defendantHintBox (wrong position!)
```

The hint was a sibling to the game in the lower StackPane, not overlaying the image.

---

### After (Correct): ✅
```
VBox
├── StackPane (image + hint overlay)
│   ├── ImageView (Cookie Monster image)
│   └── defendantHintBox (overlays on image!)
└── StackPane
    └── aimTrainingGame
```

Now the hint is a direct sibling to the ImageView inside the same StackPane, so it overlays on top of the image.

---

## Key Changes

1. **Wrapped image in StackPane**:
   ```xml
   <StackPane prefHeight="316.0" prefWidth="692.0">
     <ImageView fitHeight="316.0" fitWidth="692.0">
       <image>
         <Image url="@/images/memoryscene3-cookieMonster.png" />
       </image>
     </ImageView>
     
     <!-- Hint overlays here -->
     <VBox fx:id="defendantHintBox" ... StackPane.alignment="CENTER">
       ...
     </VBox>
   </StackPane>
   ```

2. **Added `StackPane.alignment="CENTER"`** to center the hint on the image

3. **Adjusted maxWidth to 650** and **prefWidth to 620** to fit nicely on the image

---

## Visual Result

When the hint appears after completing the aim training game:

```
┌────────────────────────────────────┐
│                                    │
│    Cookie Monster Image            │
│                                    │
│   ┌──────────────────────────┐    │
│   │  💡 Verdict Hint:        │    │  ← Green hint box
│   │  Cookie Monster's...     │    │     overlays on image
│   │  Consider: Can an AI...  │    │
│   └──────────────────────────┘    │
│                                    │
└────────────────────────────────────┘
```

---

## Testing

Run the game and:
1. ✅ Enter Cookie Monster's memory scene
2. ✅ Complete the aim training game (5 targets)
3. ✅ **Verify the green hint appears centered on top of the Cookie Monster image**
4. ✅ Text wraps properly within the hint box
5. ✅ Typewriter animation plays smoothly

The hint now overlays directly on the image, matching the Human Witness layout! 🎉
