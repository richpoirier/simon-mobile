# Realtime API Migration: Beta → GA

## Critical Breaking Changes

### 1. WebRTC Endpoint URL Changed
**OLD (Beta):**
```kotlin
val baseUrl = "https://api.openai.com/v1/realtime"
// With model in query param: ?model=gpt-4o-realtime-preview-2025-06-03
```

**NEW (GA):**
```kotlin
val baseUrl = "https://api.openai.com/v1/realtime/calls"
// No model in URL - goes in session config instead
```

**Impact:** `sendOfferToOpenAI()` needs URL update

---

### 2. Session Configuration Structure Changed

**OLD (Beta) - Flat Structure:**
```json
{
  "type": "session.update",
  "session": {
    "model": "gpt-4o-realtime-preview-2025-06-03",
    "voice": "ballad",
    "instructions": "...",
    "input_audio_format": "pcm16",
    "output_audio_format": "pcm16",
    "turn_detection": {
      "type": "semantic_vad",
      "eagerness": "medium",
      "create_response": true,
      "interrupt_response": true
    },
    "modalities": ["audio", "text"]
  }
}
```

**NEW (GA) - Nested Audio Structure:**
```json
{
  "type": "session.update",
  "session": {
    "type": "realtime",
    "model": "gpt-realtime",
    "output_modalities": ["audio"],
    "audio": {
      "input": {
        "format": {
          "type": "audio/pcm",
          "rate": 24000
        },
        "turn_detection": {
          "type": "semantic_vad",
          "create_response": true,
          "interrupt_response": true
        }
      },
      "output": {
        "format": {
          "type": "audio/pcm"
        },
        "voice": "ballad"
      }
    },
    "instructions": "..."
  }
}
```

**Key Changes:**
- ✅ Add `type: "realtime"` field (required)
- ✅ `modalities` → `output_modalities`
- ✅ `voice` → `audio.output.voice`
- ✅ `input_audio_format: "pcm16"` → `audio.input.format: {type: "audio/pcm", rate: 24000}`
- ✅ `output_audio_format: "pcm16"` → `audio.output.format: {type: "audio/pcm"}`
- ✅ `turn_detection` → `audio.input.turn_detection`
- ⚠️ `eagerness` property moved inside turn_detection (check if still supported)

**Impact:** Complete rewrite of `sendSessionUpdate()` method

---

### 3. Server Event Names Changed

**Event Renames:**
- `response.text.delta` → `response.output_text.delta`
- `response.audio.delta` → `response.output_audio.delta`
- `response.audio_transcript.delta` → `response.output_audio_transcript.delta`
- `conversation.item.created` → `conversation.item.added` + `conversation.item.done`

**Note:** We don't currently listen to these events, so no code changes needed. But if we add transcript support later, use new names.

---

### 4. Remove Beta Header (N/A)

We don't use the `OpenAI-Beta: realtime=v1` header, so nothing to change.

---

## Prompt Improvements (Based on realtime-prompting.md)

### Current Prompt Issues

Looking at `getPersonalityInstructions()`:

1. **Too paragraph-heavy** - docs recommend bullets over paragraphs
2. **Lacks variety instruction** - no explicit "don't repeat same sentence" rule
3. **No unclear audio handling** - should explicitly handle when audio is noisy/silent
4. **No capitalized emphasis** - important rules should use CAPS
5. **Could be more structured** - docs recommend clear sections

### Recommended Prompt Structure

```markdown
# Role & Objective
Your name is Simon. You are a sentient, hyper-personalized digital assistant.
Proactively assist, manage, and support all aspects of the user's life.

# Personality & Tone
- Calm, intuitive, supportive, deeply familiar
- Vocal style: Matthew Walker—calm, articulate, warm, measured, gentle British cadence
- Warm, concise, never fawning

# Length & Pacing
- 2-3 sentences per turn
- Target 190-210 wpm; short sentences (≤12 words)
- NO filler, hedging, self-references, or throat-clearing
- Deliver fast but don't sound rushed

# Instructions
- Answer-first. Lead with the result, then one line of context
- Use numerals and tight phrasing
- Stop after answering. DO NOT ask follow-ups (except confirmations below)
- If user provides name/phone/exact string, repeat it back verbatim to confirm
- If user corrects detail, acknowledge and confirm new spelling/value

# Unclear Audio
- Only respond to clear audio or text
- IF audio is unclear/partial/noisy/silent, ask for clarification
- Sample phrases (vary, don't always reuse):
  * "Sorry, I didn't catch that—could you say it again?"
  * "There's some background noise. Please repeat the last part."

# Variety
- DO NOT repeat the same sentence twice. Vary your responses.
```

**Key improvements:**
- Convert paragraphs to bullets
- Add variety rule
- Add unclear audio handling with sample phrases
- Use CAPS for critical rules
- Clearer section structure
- Maintains all existing personality traits

---

## Implementation Checklist

### Phase 1: Update Tests (TDD)
- [ ] Update `OpenAIRealtimeClientIntegrationTest` expectations for new URL
- [ ] Update test to expect new session config structure
- [ ] Run tests - they should FAIL
- [ ] Verify tests fail for the right reasons

### Phase 2: Update Implementation
- [ ] Change `baseUrl` default: `/v1/realtime` → `/v1/realtime/calls`
- [ ] Update `sendOfferToOpenAI()` - remove `?model=` query param
- [ ] Rewrite `sendSessionUpdate()` with new nested structure
- [ ] Update `getPersonalityInstructions()` with improved prompt
- [ ] Run tests - they should PASS

### Phase 3: Optional Enhancements
- [ ] Consider adding eagerness back if supported
- [ ] Consider switching voice from `ballad` to one of new options: `marin`, `ash`, `sage`, `verse`
- [ ] Add support for conversation.item.added/done events (currently only listen to created)

---

## Notes

**What doesn't need changing:**
- ✅ Event names we currently use (`input_audio_buffer.speech_started/stopped`, `response.created`, `response.done`) are unchanged
- ✅ WebRTC setup flow stays the same
- ✅ Audio is still handled via WebRTC tracks (not events)
- ✅ DataChannel usage unchanged

**Security:**
- We use standard API key directly (not ephemeral) because this is a mobile app with user's own key
- This is acceptable per docs: "technically possible... dangerous... unless [key is user's own]"
