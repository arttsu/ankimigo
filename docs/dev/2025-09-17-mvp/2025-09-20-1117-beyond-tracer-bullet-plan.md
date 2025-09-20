# AnkiMigo MVP: Beyond Tracer Bullet Implementation Plan

**Date:** 2025-09-20
**Status:** Planning Document
**Prerequisites:** Tracer bullet complete (all 6 stages implemented)

## Executive Summary

The AnkiMigo tracer bullet phase is **complete** ✅. We now have a working end-to-end application that validates the core technical stack and user workflow. This document outlines the implementation plan for evolving the tracer bullet into the full MVP as defined in the [original MVP outline](2025-09-17-2059-mvp-outline.org).

## Current State Assessment

### ✅ Completed Tracer Bullet Features
- Three-column desktop UI with responsive layout
- Concept input and real-time prompt generation
- System clipboard integration for prompt copying
- Modal dialog for LLM response pasting
- Tolerant JSON parser with card validation and visual error feedback
- AnkiConnect integration (deck fetching and card pushing)
- Complete end-to-end workflow: Concept → Prompt → LLM Response → Parsed Cards → Push to Anki
- Card state management with Anki note ID tracking
- Comprehensive error handling and user feedback

### Technical Foundation
- **Language:** Clojure 1.12.0
- **UI Framework:** cljfx 1.9.5
- **HTTP Client:** hato 1.0.0
- **JSON Parsing:** jsonista 0.3.13
- **Architecture:** Single state atom with pure event handlers
- **Code Size:** ~465 lines in main.clj, ~50 lines in prompt.clj

## Technical Debt Analysis

### Critical Issues to Address

#### Code Quality
- **State validation needed**: No clojure.spec validation for state shape - brittle map structures
- **Function size**: Some UI functions are becoming large and complex (root function approaching 100+ lines)
- **Component organization**: UI components mixed with business logic in single namespace
- **Error message improvements**: Some error messages could be more actionable

#### Architecture Decisions Needing Refinement
- **Hardcoded model type**: AnkiConnect requests use hardcoded "Basic" model - should be configurable
- **Single template system**: Only supports one prompt template - MVP outline mentions this as "secret sauce" needing first-class treatment
- **State atom granularity**: Single large atom - consider breaking into logical domains
- **Event handler organization**: All events in one large case statement (450+ lines)

#### Development Practices Gaps
- **No test suite exists**: Critical gap - need unit tests for core functions
- **Integration testing**: Need tests for AnkiConnect integration with mock responses
- **Documentation gaps**: Functions lack comprehensive docstrings
- **Configuration management**: Extract hardcoded values to config

## MVP Feature Gap Analysis

### Features Included in MVP Scope But Not Yet Implemented

#### Enhanced Prompt System
- **Real-time Final Prompt assembly with "Re-roll" themes button**: Current implementation only has concept interpolation
- **Multiple prompt input fields**: Goal, Persona, Themes (multi-line), Colloquial Usage checkbox, Additional Instructions
- **System Message collapsible section**: Template visibility toggle
- **Themes randomization**: "Re-roll" button for variety generation

#### Configuration & Persistence
- **Field mapping and persistence**: Save input fields + last deck + field mappings in `~/.ankimigo/config.edn`
- **Multiple note types**: Support beyond "Basic" AnkiConnect model
- **User preferences**: Deck selection, field mappings, template customization

#### User Experience Enhancements
- **Setup wizard**: First-run experience for AnkiConnect configuration
- **Keyboard shortcuts**: No keyboard navigation support
- **Progress indicators**: Long operations lack visual feedback
- **Card editing**: No ability to modify cards before/after pushing

## Implementation Plan

### Phase 1: Technical Foundation (1-2 sessions)

#### 1.1 Namespace Reorganization
**Objective:** Extract business logic from UI layer for better maintainability

**Tasks:**
- Create `ankimigo.state` namespace for state management and validation
- Create `ankimigo.anki` namespace for AnkiConnect operations
- Create `ankimigo.ui.components` namespace for reusable UI components
- Create `ankimigo.config` namespace for configuration management
- Refactor main.clj to use new namespace organization

**Benefits:**
- Cleaner architecture enables easier testing
- Separation of concerns improves maintainability
- Enables parallel development of different features

#### 1.2 State Validation with clojure.spec
**Objective:** Add runtime state validation to prevent bugs

**Tasks:**
- Define specs for all state map structures
- Add validation on state transitions
- Implement helpful error messages for invalid states
- Add development-time state inspection tools

**Benefits:**
- Prevents bugs from invalid state mutations
- Provides better error messages during development
- Foundation for safe state migrations

#### 1.3 Configuration Persistence System
**Objective:** Save user preferences between sessions

**Implementation:**
- Create `~/.ankimigo/config.edn` for persistent storage
- Save: input fields, last selected deck, field mappings, window state
- Load configuration on startup with fallback defaults
- Auto-save configuration on changes

**User Impact:**
- Eliminates repetitive setup between sessions
- Remembers user preferences and workflow state
- Foundation for all advanced configuration features

#### 1.4 Basic Test Suite
**Objective:** Establish testing foundation for confident refactoring

**Coverage:**
- Unit tests for card validation logic
- Unit tests for JSON parsing with various LLM outputs
- Unit tests for prompt rendering and template interpolation
- Mock tests for AnkiConnect integration
- Property-based tests for state transitions

### Phase 2: Core MVP Features (2-3 sessions)

#### 2.1 Enhanced Prompt Input System
**Objective:** Implement the full prompt template system from MVP outline

**New Input Fields:**
- **Goal**: High-level learning objective (e.g., "Reach Spanish A2")
- **Persona**: User description (hobbies, job, etc.)
- **Themes**: Multi-line text for example contexts
- **Colloquial Usage**: Checkbox toggle
- **Additional Instructions**: Optional one-time guidance
- **System Message**: Collapsible template section

**Features:**
- Real-time prompt assembly from all inputs
- Template interpolation with proper escaping
- Input validation and error feedback

#### 2.2 Themes Randomization System
**Objective:** Implement the core "variety" mechanism

**Implementation:**
- Parse themes into individual items (one per line)
- "Re-roll" button randomly selects subset of themes
- Inject selected themes into prompt template
- Visual indication of which themes are currently active
- Configurable number of themes to select

**User Impact:**
- Ensures fresh, personalized examples every time
- Core differentiator that makes cards memorable
- Addresses repetition fatigue in language learning

#### 2.3 Field Mapping and Note Type Selection
**Objective:** Support multiple Anki note types beyond "Basic"

**Features:**
- Fetch available note types from AnkiConnect
- UI for mapping card fields to note type fields
- Save field mappings per note type in configuration
- Validation of field mapping completeness
- Support for common note types (Basic, Cloze, etc.)

#### 2.4 Settings Dialog
**Objective:** Centralized configuration management

**Settings Categories:**
- **Prompt Templates**: Edit and manage prompt templates
- **Anki Connection**: AnkiConnect URL, timeout settings
- **Field Mappings**: Configure mappings per note type
- **UI Preferences**: Default deck, auto-save settings
- **Import/Export**: Backup and restore configuration

### Phase 3: Polish & User Experience (1-2 sessions)

#### 3.1 Keyboard Shortcuts
**Objective:** Improve workflow efficiency for power users

**Shortcuts:**
- `Ctrl+C`: Copy prompt (when concept is focused)
- `Ctrl+V`: Open paste dialog
- `Ctrl+Enter`: Push to Anki (when cards are ready)
- `F5`: Fetch decks / refresh
- `Ctrl+R`: Re-roll themes
- `Ctrl+,`: Open settings dialog

#### 3.2 Card Editing Capabilities
**Objective:** Allow card modification before and after push

**Features:**
- Edit card fields in-place within the card list
- Add/remove cards from the set
- Duplicate cards for variations
- Bulk operations (delete selected, tag all)
- Undo/redo for editing operations

#### 3.3 Enhanced Error Handling
**Objective:** Provide actionable guidance for common issues

**Improvements:**
- Specific AnkiConnect setup guidance
- Network connectivity troubleshooting
- JSON parsing error location highlighting
- Recovery suggestions for common failures
- Link to documentation for complex issues

## Success Criteria

### Phase 1 Complete When:
- [ ] Codebase is organized into logical namespaces
- [ ] State validation prevents common bugs
- [ ] User preferences persist between sessions
- [ ] Core functions have test coverage >80%

### Phase 2 Complete When:
- [ ] All MVP prompt inputs are implemented and functional
- [ ] Themes randomization provides variety in card generation
- [ ] Multiple Anki note types are supported
- [ ] Settings dialog allows full configuration management

### Phase 3 Complete When:
- [ ] Keyboard shortcuts improve workflow efficiency
- [ ] Users can edit cards before and after pushing
- [ ] Error messages provide actionable guidance
- [ ] User experience feels polished and professional

### MVP Complete When:
- [ ] All features from the [MVP outline](2025-09-17-2059-mvp-outline.org) are implemented
- [ ] Application feels reliable and professional
- [ ] Workflow is efficient for daily use
- [ ] Technical debt is manageable for future development

## Risk Mitigation

### Technical Risks
- **State management complexity**: Mitigated by spec validation and namespace organization
- **UI performance**: Monitor for slowdowns as complexity increases
- **AnkiConnect reliability**: Implement robust retry and error handling
- **Configuration corruption**: Add validation and backup mechanisms

### Feature Risks
- **Scope creep**: Stick to MVP feature list, defer advanced features
- **User workflow disruption**: Preserve existing tracer bullet functionality
- **Template complexity**: Keep prompt system simple and discoverable
- **Configuration overwhelming**: Use progressive disclosure in settings

## Next Steps

### Immediate Actions
1. **Start Phase 1.3: Configuration Persistence** - Highest impact, foundational for other features
2. **Create development branch** - Preserve working tracer bullet while developing
3. **Set up development environment** - Ensure Anki + AnkiConnect are available for testing

### Development Approach
- **Incremental implementation** with explicit checkpoints (proven successful in tracer bullet)
- **Preserve working state** at each step - never break the end-to-end workflow
- **Test with real usage** - validate each feature with actual Anki workflows
- **Document as you go** - Update session notes with discoveries and decisions

### Quality Gates
- All existing tracer bullet functionality must continue working
- Each phase should be shippable as an incremental improvement
- Configuration changes should be backward compatible
- Performance should not degrade with added features

---

## References

### Project Documentation
- [MVP Outline](2025-09-17-2059-mvp-outline.org) - Original vision and scope
- [Tracer Bullet Requirements](2025-09-18-0832-tracer-bullet-requirements.md) - Completed foundation
- [Implementation Spec](2025-09-18-0900-tracer-bullet-implementation-spec.md) - Technical approach
- [Session 7 Notes](2025-09-20-1100-tracer-bullet-session-7.org) - Current state and lessons learned

### Code Files
- `src/ankimigo/main.clj` - Main application logic (465 lines)
- `src/ankimigo/prompt.clj` - Template system (50 lines)
- `resources/prompts/default_prompt_v1.md` - Current prompt template
- `deps.edn` - Project dependencies

### External Dependencies
- Anki desktop application with AnkiConnect add-on
- Clojure development environment
- Testing framework (to be selected in Phase 1.4)
