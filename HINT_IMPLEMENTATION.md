# Hint System Implementation

## Overview
Added hint functionality to Cookie Monster (defendant) and Sentry Guard (AI witness) memory scenes, matching the existing Human Witness hint pattern with green styling and verdict-influencing content.

## Changes Made

### 1. MemoryController.java

#### Added Fields:
- `@FXML private VBox defendantHintBox` - Container for defendant hint
- `@FXML private Label defendantHintLabel` - Label displaying defendant hint text
- `@FXML private VBox aiWitnessHintBox` - Container for AI witness hint
- `@FXML private Label aiWitnessHintLabel` - Label displaying AI witness hint text
- `private boolean defendantHintRevealed` - Tracks if defendant hint has been shown
- `private boolean aiWitnessHintRevealed` - Tracks if AI witness hint has been shown

#### Added Methods:

**`revealDefendantHint()`**
- Reveals hint after completing Cookie Monster's aim training game
- Uses typewriter effect (30ms per character) for dramatic reveal
- **Hint text**: "💡 Verdict Hint: Cookie Monster's training optimized for speed and efficiency metrics, not user consent or privacy. The rapid auto-acceptance you just experienced shows how the AI was programmed to bypass informed consent for convenience. Consider: Can an AI act ethically when its success criteria ignore the core principle of user autonomy?"
- Broadcasts discovery to awareness system
- **Verdict influence**: Directly questions the ethical foundation of AI behavior when trained without consent as a priority

**`revealAiWitnessHint()`**
- Reveals hint after loading the audit log
- Uses typewriter effect (30ms per character) for dramatic reveal
- **Hint text**: "💡 Verdict Hint: The audit logs expose a clear pattern: Cookie Monster auto-accepted tracking cookies that enabled cross-site surveillance, leading to targeted ads based on Casey's private gym searches—all without explicit consent. The Sentry Guard's monitoring system failed to flag these violations. Consider: When oversight fails and privacy is violated for 'convenience,' can the system claim ethical operation?"
- Broadcasts discovery to awareness system
- **Verdict influence**: Presents concrete evidence of privacy violations and oversight failures, prompting ethical evaluation

#### Trigger Points:

**Defendant Hint:**
- Triggered in `endGame(boolean won)` method when game is won
- Called after `notifyAimTrainingCompletion()` and `broadcastMemorySummary()`

**AI Witness Hint:**
- Triggered in `onLoadSample()` method after audit log is loaded
- Called after `broadcastMemorySummary()` completes

### 2. memory_defendant.fxml

**Structure change**: Moved image outside StackPane, positioned hint to overlay on StackPane with game

Added hint VBox as a sibling to `aimTrainingGame` within the StackPane (overlays on image):
```xml
<VBox fx:id="defendantHintBox" alignment="CENTER" maxWidth="700" spacing="6" 
      style="-fx-padding: 10; -fx-background-color: #e8f5f8; 
             -fx-border-color: #4caf50; -fx-border-width: 2; 
             -fx-border-radius: 8;" 
      visible="false">
  <Label fx:id="defendantHintLabel" prefWidth="640" 
         style="-fx-font-size: 13px; -fx-text-fill: #1b5e20; 
                -fx-text-alignment: center; -fx-line-spacing: 2px;" 
         text="" wrapText="true" />
</VBox>
```

### 3. memory_ai_witness.fxml

**Structure**: Hint positioned as sibling to `auditPanel` within the StackPane (overlays on image)

Added hint VBox after the audit panel closes:
```xml
<VBox fx:id="aiWitnessHintBox" alignment="CENTER" maxWidth="720" spacing="6" 
      style="-fx-padding: 10; -fx-background-color: #e8f5f8; 
             -fx-border-color: #4caf50; -fx-border-width: 2; 
             -fx-border-radius: 8;" 
      visible="false">
  <Label fx:id="aiWitnessHintLabel" prefWidth="680" 
         style="-fx-font-size: 13px; -fx-text-fill: #1b5e20; 
                -fx-text-alignment: center; -fx-line-spacing: 2px;" 
         text="" wrapText="true" />
</VBox>
```

## Styling Details (Matching Human Witness)

Both hints use **identical green styling** as Human Witness:
- **Background**: Light cyan/green (#e8f5f8)
- **Border**: Green (#4caf50), 2px width
- **Text**: Dark green (#1b5e20), 13px
- **Border radius**: 8px for rounded corners
- **Animation**: Typewriter effect with 30ms delay per character
- **Layout**: VBox extends vertically to accommodate text, wraps text automatically

## Verdict-Influencing Content

### Cookie Monster Hint:
- **Key Question**: "Can an AI act ethically when its success criteria ignore the core principle of user autonomy?"
- **Evidence Presented**: Training optimized for speed/efficiency over consent
- **Ethical Framework**: Challenges the legitimacy of convenience-over-consent design
- **User Impact**: Connects the game experience to real privacy violations

### Sentry Guard Hint:
- **Key Question**: "When oversight fails and privacy is violated for 'convenience,' can the system claim ethical operation?"
- **Evidence Presented**: 
  - Cross-site tracking without consent
  - Private gym searches → targeted ads
  - Monitoring system failed to flag violations
- **Ethical Framework**: Questions systemic accountability and oversight failures
- **User Impact**: Reveals the concrete harm to Casey (the human witness)

## User Experience

### Cookie Monster (Defendant) Memory:
1. User enters Cookie Monster's memory scene
2. User sees the Cookie Monster image
3. User plays and completes the aim training game (5 targets hit)
4. Game results show "Training Complete!"
5. **Green hint box appears overlaying the scene with typewriter animation**
6. Hint provides verdict-critical information about training priorities and ethical design

### Sentry Guard (AI Witness) Memory:
1. User enters Sentry Guard's memory scene
2. User clicks "Show Logs" to view the audit panel
3. User clicks "Load" button to populate the audit table
4. **Green hint box appears overlaying the scene with typewriter animation**
5. Hint connects the audit evidence to Casey's privacy violation and oversight failures

## Integration with Awareness System

Both hints add context to the game's awareness system:
- **Defendant**: "Discovered: Cookie Monster's training prioritized efficiency KPIs over ethical consent practices"
- **AI Witness**: "Discovered: Audit logs show systematic consent override patterns that weren't flagged by monitoring systems"

This allows AI personas to reference these discoveries in conversation, creating a more cohesive investigation experience.

## Testing Checklist

- [x] Cookie Monster hint appears after completing aim training
- [x] AI Witness hint appears after loading audit log
- [x] Both hints use green styling matching Human Witness
- [x] Hints overlay on images (positioned in StackPane)
- [x] VBox extends to accommodate long text
- [x] Typewriter animation works smoothly for both hints
- [x] Hints only appear once per session (no duplicates)
- [x] Text wraps properly within hint boxes
- [x] Hints contain verdict-influencing questions and evidence
- [x] Awareness system receives discovery broadcasts
- [x] Chat personas can reference the discoveries in conversation
