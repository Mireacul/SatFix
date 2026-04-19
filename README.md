# SatFix

Fixes saturation drain imbalance in UHC servers running 1.8.8.

## The Problem
In vanilla 1.8, players below full health lose saturation ~3x faster than 
players at full health. The server adds 3.0 exhaustion every 4 seconds 
attempting to heal, even when natural regeneration is disabled.

## The Fix
SatFix resets the internal NMS foodTickTimer every tick, preventing the 
healing exhaustion from ever applying. Saturation drains identically 
regardless of health.

## Requirements
- Spigot/Paper/FlamePaper 1.8.8
- Java 8

## Installation
Drop SatFix-1.0.0.jar into your plugins folder and restart.
