# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

NBT Void is a client-side Minecraft Fabric mod for Minecraft 1.20.4 that automatically saves every item with NBT data to a searchable void collection. The mod provides a custom creative tab interface for retrieving stored items.

**Note: This version has been successfully ported to Minecraft 1.20.4 (v2.1.0) from the original 1.20.4 version, maintaining full functionality while updating dependencies.**

## Build Commands

- **Build the mod**: `./gradlew build` (Windows: `gradlew.bat build`)
- **Clean build**: `./gradlew clean build`
- **Generate sources jar**: `./gradlew genSources`
- **Run in development**: `./gradlew runClient`

## Architecture

### Core Components

- **NbtVoid.java**: Main mod entry point and client initializer
  - Manages the global VOID collection instance
  - Handles save/load operations for void.nbt file in game directory
  - Registers custom item group for the void tab
  
- **Config.java**: Configuration management with YACL integration
  - JSON-based config stored in `.minecraft/config/nbtvoid.json`
  - Configurable search behavior, sorting, NBT filtering, and storage limits
  - Supports NBT path patterns for ignore/remove lists

- **VoidCollection.java**: Core data structure for item storage
  - Maintains both ordered list and unique set for efficient operations
  - Automatic deduplication based on configurable NBT ignore patterns
  - Size-limited with FIFO eviction when max capacity reached

### Mixin Integration

- **ItemStackMixin**: Intercepts item operations to automatically add NBT items to void
- **CreativeInventoryScreenMixin**: Integrates void collection with creative inventory UI

### Key Dependencies

- Fabric Loader 0.15.3+
- Fabric API 0.92.0+
- Yet Another Config Lib (YACL) 3.3.1+ for config UI
- ModMenu integration for settings access

## Development Notes

- Target Java 17
- Uses access wideners (`nbtvoid.accesswidener`) for Minecraft internals
- Client-side only mod (no server-side components)
- Automatic NBT item detection and storage happens via mixins
- Config supports NBT path syntax for precise filtering (e.g., "BlockEntityTag.id")