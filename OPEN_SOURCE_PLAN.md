# Open Source Preparation Plan for Sage Mobile (formerly Simon Mobile)

## Overview
This document outlines the plan to prepare Sage Mobile for open-sourcing under the Apache 2.0 license.

## Phase 1: Configuration & Security Verification

### 1.1 Verify Security Best Practices
- [x] Confirm `.gitignore` properly excludes config.properties
- [x] Double-check no API keys or secrets in committed code
- [x] Verify all sensitive files are gitignored
- [x] Scan codebase for any hardcoded credentials

### 1.2 Make Key Settings Configurable
- [ ] **Update config.properties.example**
  - [ ] Add configuration options for voice and personality
  - [ ] Keep numeric values and technical settings hardcoded for simplicity

- [ ] **Voice Selection**
  - [ ] Move voice selection from hardcoded "ballad" to config.properties
  - [ ] Point to OpenAI documentation for available voices

- [ ] **Personality Customization**
  - [ ] Create base prompt with hardcoded opinionated sections in code
  - [ ] Allow additional personality customization via config.properties
  - [ ] Move from assets/simon_prompt.md to assets/sage_prompt.md
  - [ ] Support custom prompt additions/modifications through config

- [ ] **Model Configuration**
  - [ ] Make model version configurable (currently "gpt-realtime")
  - [ ] Allow for future model updates without code changes

- [ ] **Documentation**
  - [ ] Update README with configuration section
  - [ ] Document which aspects are configurable vs opinionated

### 1.3 Claude Code Settings
- [x] Keep `.claude/settings.local.json` in git (contains project-specific Claude Code permissions)

### 1.4 Rename Project to Sage Mobile
- [ ] Update package name from `com.simon.app` to `com.sage.app` throughout codebase
- [ ] Rename main app module and directories
- [ ] Update `AndroidManifest.xml` with new package name
- [ ] Update `settings.gradle` project name
- [ ] Update all import statements
- [ ] Update all string resources referencing "Simon"
- [ ] Update test packages and imports
- [ ] Update documentation (README, docs, comments)
- [ ] Rename repository from simon-mobile to sage-mobile

## Phase 2: Essential Open-Source Components

### 2.1 Add Apache 2.0 LICENSE
- [ ] Use LICENSE_DRAFT as the base (standard Apache 2.0 text already prepared)
- [ ] Rename LICENSE_DRAFT to LICENSE
- [ ] Update copyright year to 2025 (already set)
- [ ] Add actual copyright owner name in place of [Your Name]

### 2.2 Update README.md
- [ ] Use README_DRAFT.md as the base (already created with all required sections)
- [ ] Update all references from "Simon" to "Sage"
- [ ] Update repository URLs from yourusername/simon-mobile to actual repository
- [ ] Update package references from com.simon.app to com.sage.app
- [ ] Add actual copyright owner name
- [ ] Remove references to CONTRIBUTING.md and SECURITY.md
- [ ] Update voice configuration section with new options from config.properties.example.enhanced
- [ ] Create a demo to put at top of README


## Phase 3: Final Preparations

### 3.1 Move Development to Mac
- [ ] Set up development environment on Mac
- [ ] Remove Windows-specific configurations or workarounds
- [ ] Test that project builds on a clean Mac with fresh checkout
- [ ] Update documentation to reflect Mac-only development
- [ ] Remove any WSL-specific instructions or configurations

### 3.2 Update Project Metadata
- [ ] Add GitHub repository description
- [ ] Add relevant topics (android, kotlin, voice-assistant, openai, webrtc)
- [ ] Configure issue labels
- [ ] Update all placeholder values in docs (emails, URLs, copyright owner)
- [ ] Set repository to public (when ready)

### 3.3 Update CLAUDE.md
- [ ] Update project name from Simon to Sage throughout
- [ ] Update package names and architecture details
- [ ] Add new configuration options documentation
- [ ] Update build and testing instructions if changed
- [ ] Add note about voice customization and configurability
- [ ] Review and update development gotchas
- [ ] Ensure it accurately reflects the final project state

## Files Checklist

### Drafts Already Created (Use as Base):
- **README_DRAFT.md** - Professional README ready for customization
- **LICENSE_DRAFT** - Standard Apache 2.0 license text

### To Create (From Drafts):
- [ ] LICENSE (use LICENSE_DRAFT)
- [ ] README.md (use README_DRAFT.md)

### To Update:
- [ ] .gitignore (verify completeness)
- [ ] Package names from com.simon.app to com.sage.app
- [ ] All references from "Simon" to "Sage"
- [ ] CLAUDE.md (update with new name and final project state)
- [ ] config.properties.example (add new configuration options from section 1.2)

## Success Criteria
- [ ] No sensitive data in repository
- [ ] Clear installation instructions
- [ ] Professional README
- [ ] Legal compliance (LICENSE)

## Notes
- Priority: Minimal viable release to get secure, compliant open-source quickly
- API Key Configuration: Keep build-time config method as chosen
- License: Apache 2.0 as selected