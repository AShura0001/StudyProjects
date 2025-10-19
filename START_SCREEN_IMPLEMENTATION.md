# Start Screen Implementation - Summary

## What Was Added ✅

A custom **TRIAL AI** start screen with Matrix-style binary rain background that shows when the game launches.

---

## Files Modified

### 1. **`room_frame.fxml`**
**Location:** `src/main/resources/fxml/room_frame.fxml`

**Added a new StackPane layer** at the end (so it appears on top):

```xml
<!-- Start Screen Layer -->
<StackPane fx:id="startScreen" style="-fx-background-color: #000000;">
  <!-- Background image with binary rain effect -->
  <ImageView fitHeight="800.0" fitWidth="1200.0" preserveRatio="false">
    <image>
      <Image url="@../images/start_view.png" />
    </image>
  </ImageView>
  
  <!-- Center content with logo and button -->
  <VBox alignment="CENTER" spacing="40">
    <!-- Logo container (white rounded rectangle) -->
    <StackPane style="white rounded background with shadow">
      <Label text="TRIAL AI" with blue glitchy effect />
    </StackPane>
    
    <!-- Start Game Button -->
    <Button fx:id="btnStartGame" text="Start Game" 
      onAction="#onStartScreenButton" />
  </VBox>
</StackPane>
```

**Design Features:**
- ✅ Uses your `start_view.png` as background (binary rain Matrix effect)
- ✅ White rounded rectangle container for the logo
- ✅ "TRIAL AI" text in blue (#1E90FF) with glitch effect styling
- ✅ Drop shadow effects for depth
- ✅ Blue "Start Game" button (250×60px) below the logo
- ✅ Hover-ready with cursor styling

---

### 2. **`RoomFrameController.java`**
**Location:** `src/main/java/nz/ac/auckland/se206/controllers/RoomFrameController.java`

#### Added Fields:
```java
@FXML private StackPane startScreen;
@FXML private Button btnStartGame;
```

#### Updated `initialize()` method:
```java
// Show start screen at the beginning
if (startScreen != null) {
  startScreen.setVisible(true);
  startScreen.setManaged(true);
  startScreen.toFront();
}
```

#### Added Button Handler:
```java
@FXML
private void onStartScreenButton() {
  // Hide the start screen
  if (startScreen != null) {
    startScreen.setVisible(false);
    startScreen.setManaged(false);
  }
  
  // Start the game
  onStartGame();
}
```

---

## How It Works

### At Startup:
1. ✅ Game launches with `room_frame.fxml`
2. ✅ Start screen is shown on top (visible + managed)
3. ✅ Main room content is hidden underneath
4. ✅ Player sees: Binary rain background + TRIAL AI logo + Start Game button

### When "Start Game" Clicked:
1. ✅ Start screen is hidden (invisible + unmanaged)
2. ✅ `onStartGame()` is called
3. ✅ Main room content becomes visible
4. ✅ Timer starts, game begins

---

## Visual Design

### Background:
- Black base color (#000000)
- Matrix-style binary rain (`start_view.png`)
- Full screen (800×1200px)

### Logo Container:
- White rounded rectangle (30px radius)
- Large padding (40px top/bottom, 80px left/right)
- Drop shadow for elevation

### TRIAL AI Text:
- Font: 72px, bold
- Color: Dodger Blue (#1E90FF)
- Glitchy effect with text shadow
- Blue glow effect

### Start Game Button:
- Size: 250×60px
- Blue background (#1E90FF)
- White text, 24px bold
- 15px border radius
- Blue glow shadow on hover
- Hand cursor

---

## Testing

### Test 1: Start Screen Appears ✅
1. Run the application
2. Should see start screen with:
   - Binary rain background
   - White rounded box with "TRIAL AI"
   - Blue "Start Game" button below

### Test 2: Start Game Button Works ✅
1. Click "Start Game" button
2. Start screen should disappear
3. Main courtroom should appear
4. Timer should start
5. Game elements should be enabled

### Test 3: No Duplicate Start ✅
1. Start screen is only shown once at launch
2. Regular "Start" button in game still works
3. No conflicts between the two

---

## Customization Options

### Want to adjust the design?

**In Scene Builder:**
- Open `room_frame.fxml`
- Find `startScreen` StackPane at the bottom
- Adjust:
  - Logo text size/color
  - Button size/position
  - Spacing between elements
  - Container styling

**In Code:**
- Colors: Change `#1E90FF` (blue) to your brand color
- Font: Modify `fx-font-size` and `fx-font-family`
- Animation: Add fade-in/fade-out transitions
- Sound: Add click sound in `onStartScreenButton()`

---

## Image Requirements

✅ **Required file:** `src/main/resources/images/start_view.png`
- Your Matrix-style binary rain background
- Size: 1200×800px (or aspect ratio 3:2)
- Already saved in your project!

---

## Architecture

### Layering (StackPane children order):
```
1. BorderPane (frameRoot) - Main game frame
2. StackPane (overlayRoot) - Verdict/warning overlays
3. StackPane (startScreen) - Start screen (added last = on top)
```

### Why this works:
- ✅ No new scene file needed
- ✅ Minimal code changes
- ✅ Easy to show/hide with visibility
- ✅ Maintains all existing functionality
- ✅ StackPane naturally layers elements

---

## Summary

🎮 **Start screen:** Shows at launch with your design  
🖱️ **One click:** "Start Game" button hides it and starts the game  
🎨 **Fully editable:** Modify design in Scene Builder  
🔧 **Clean code:** Just show/hide logic, no complex routing  

Your **TRIAL AI** start screen is now live! 🚀
