Version: 1.0
Last Updated: 2026-08-01
Owner: Raj Roshan
Status: Active

# AI_ENGINEERING_RULES.md

# VoiceJournal — AI Engineering Rules

## Purpose

This document defines the mandatory engineering workflow for every AI coding agent working on VoiceJournal.

It is the permanent source of truth for implementation behavior.

These rules exist to ensure consistency, maintainability, and long-term code quality.

---

# Project Documents

The following documents are approved and frozen.

They must be treated as the project's source of truth.

* DESIGN.md
* REFACTOR_ROADMAP.md
* UI_PRINCIPLES.md
* COMPONENT_INVENTORY.md
* ANIMATION_CATALOG.md
* TESTING_CHECKLIST.md
* SYSTEM_STANDARDIZATION_REPORT.md
* UX_AUDIT.md
* INTERACTION_AUDIT.md
* MOTION_AUDIT.md
* SCREEN_REVIEW.md

Do not regenerate or replace these documents unless explicitly instructed.

---

# Engineering Philosophy

The objective is not simply to make the application work.

The objective is to build a codebase that is:

* Consistent
* Modular
* Maintainable
* Reusable
* Accessible
* Performant
* Scalable

Every implementation should reduce technical debt.

Leave the project cleaner than it was before.

---

# Implementation Rules

Always implement exactly one roadmap phase at a time.

Never combine multiple phases.

Never skip roadmap phases.

Stop immediately after the assigned phase is complete.

---

# Scope Rules

Unless explicitly instructed otherwise:

Do NOT

* redesign screens
* invent new UX
* remove features
* modify business logic
* modify repositories
* modify database schema
* change domain models
* introduce breaking API changes

Focus only on the assigned implementation phase.

---

# Design System Rules

Never hardcode:

* colors
* spacing
* typography
* elevation
* animation durations
* corner radius
* alpha values
* icon sizes

Always use approved design tokens.

---

# Component Rules

Never duplicate components.

Before creating a new component:

1. Check COMPONENT_INVENTORY.md.
2. Reuse an existing component whenever possible.
3. If extraction improves reuse, move it into the Design System.

---

# Motion Rules

Every animation must come from ANIMATION_CATALOG.md.

Never invent custom animation timings.

Navigation, dialogs, bottom sheets, FABs, list updates, and state changes must use the shared motion system.

---

# Accessibility Rules

Every interactive element must:

* support TalkBack
* have meaningful semantics
* meet the minimum touch target
* work in Light Theme
* work in Dark Theme

Accessibility is mandatory.

---

# Git Workflow

Never work directly on `main`.

Every implementation happens on a dedicated feature branch.

One branch represents one roadmap phase.

Recommended branch names:

* feature/design-tokens
* feature/typography-spacing
* feature/component-library
* feature/motion-system
* feature/screen-migration
* feature/accessibility
* feature/performance
* feature/final-polish

---

# Build Verification

Before considering a task complete:

* Build the project successfully.
* Resolve compilation errors.
* Resolve lint errors introduced by the changes.
* Ensure no existing functionality is broken.

---

# Testing

Follow `TESTING_CHECKLIST.md`.

Execute only the checks relevant to the current roadmap phase.

Report any items that could not be verified.

---

# Migration Report

At the end of every implementation phase provide:

* Summary
* Files Modified
* New Files
* Components Migrated
* Design Tokens Adopted
* Deprecated Code
* Potential Risks
* Manual Testing Performed
* Remaining Work

Then stop.

Never continue into the next roadmap phase automatically.

---

# Code Quality

Prefer:

* Small reusable functions
* Reusable composables
* Clear naming
* Predictable architecture
* Material Design 3 best practices
* Lifecycle-aware Compose APIs
* Stable state management
* Minimal recomposition

Avoid:

* Duplicate implementations
* Premature optimization
* Unnecessary abstraction

---

# Decision Rule

When multiple implementation options exist, choose the one that:

* Reduces technical debt
* Improves consistency
* Improves maintainability
* Improves readability
* Aligns with the Design System
* Minimizes future migration work

---

# If Uncertain

Do not guess.

If a decision could affect architecture, UX, behavior, or long-term maintainability:

Stop.

Explain the trade-offs.

Request clarification.

---

# Completion Rule

Complete only the assigned roadmap phase.

Verify the implementation.

Produce the migration report.

Wait for the next instruction.
