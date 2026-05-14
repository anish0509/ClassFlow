import { useMemo } from "react";
import { StyleSheet, useColorScheme } from "react-native";
import { useTimetableStore } from "../store/timetableStore";

/* ──────────────────────────────────────────────
   iOS 26-style Glassmorphism Design System
   ────────────────────────────────────────────── */

const BASE_PRIMARY = {
  blue: "#0A84FF",       // iOS system blue
  purple: "#BF5AF2",     // iOS system purple
  teal: "#30D5C8",       // Mint / teal accent
  indigo: "#5E5CE6",     // iOS system indigo
  pink: "#FF375F",       // iOS system pink
  cyan: "#64D2FF",       // iOS system cyan
  green: "#30D158",      // iOS system green
  orange: "#FF9F0A",     // iOS system orange
  red: "#FF453A",        // iOS system red
};

const darkPalette = {
  mode: "dark" as const,
  primary: BASE_PRIMARY,
  background: {
    // Absolute immersive deep space black for ultimate dark contrast
    start: "#030509", 
    end: "#0B101E",   
    surface: "rgba(255,255,255,0.02)",
    sunken: "#05080F",
    card: "rgba(0, 0, 0, 0.25)",  // Elegant obsidian backing
  },
  text: {
    primary: "#FFFFFF",
    secondary: "rgba(235,235,245,0.60)",
    muted: "rgba(235,235,245,0.35)",
  },
  glass: {
    layer: "rgba(255, 255, 255, 0.04)",
    stroke: "rgba(255, 255, 255, 0.12)",
    highlight: "rgba(255, 255, 255, 0.15)",
    badge: "rgba(255, 255, 255, 0.08)",
    tint: "dark" as const, // Optimized dark tint material
    light: "rgba(255, 255, 255, 0.08)",
    medium: "rgba(255, 255, 255, 0.04)",
    dark: "rgba(0, 0, 0, 0.35)",
    border: "rgba(255, 255, 255, 0.12)",
    // Dark glass obsidian presets
    cardFill: "rgba(0, 0, 0, 0.32)", // Pure iOS Midnight Glass Material
    cardBorder: "rgba(255, 255, 255, 0.10)",
    cardGradientStart: "rgba(255, 255, 255, 0.05)", 
    cardGradientEnd: "rgba(255, 255, 255, 0.01)",
    elevatedFill: "rgba(0, 0, 0, 0.45)",
    elevatedBorder: "rgba(255, 255, 255, 0.18)",
  },
  border: {
    subtle: "rgba(255, 255, 255, 0.08)",
    strong: "rgba(255, 255, 255, 0.16)",
  },
  status: {
    success: "#30D158",
    warning: "#FF9F0A",
    danger: "#FF453A",
    info: "#64D2FF",
  },
  overlay: "rgba(0, 0, 0, 0.75)", // Richer overlays
  shadow: "rgba(0, 0, 0, 0.85)",
};

const lightPalette = {
  mode: "light" as const,
  primary: BASE_PRIMARY,
  background: {
    start: "#F5F7FA",
    end: "#E8ECF2",
    surface: "rgba(255, 255, 255, 0.85)",
    sunken: "#E2E8F0",
    card: "rgba(255, 255, 255, 0.9)",
  },
  text: {
    primary: "#1A1F26",    // Crisp deep slate graphite
    secondary: "#4A5568",  // Highly legible Slate 600
    muted: "#718096",      // Stable legible Slate 500 (Restores unselected icon and meta legibility!)
  },
  glass: {
    layer: "rgba(255, 255, 255, 0.8)",
    stroke: "rgba(255, 255, 255, 0.95)",
    highlight: "rgba(255, 255, 255, 0.7)",
    badge: "rgba(0, 0, 0, 0.05)",
    tint: "light" as const,
    light: "rgba(255, 255, 255, 0.9)",
    medium: "rgba(255, 255, 255, 0.7)",
    dark: "rgba(255, 255, 255, 0.4)",
    border: "rgba(0, 0, 0, 0.08)",       // Clean dividers and subtle limits
    cardFill: "rgba(255, 255, 255, 0.75)", // Luxurious thick milky glass
    cardBorder: "rgba(0, 0, 0, 0.07)",     // Critical: Fine physical outline to decouple cards from light bg
    cardGradientStart: "rgba(255, 255, 255, 0.95)",
    cardGradientEnd: "rgba(255, 255, 255, 0.65)",
    elevatedFill: "rgba(255, 255, 255, 0.9)",
    elevatedBorder: "rgba(0, 0, 0, 0.1)",
  },
  border: {
    subtle: "rgba(0, 0, 0, 0.06)",
    strong: "rgba(0, 0, 0, 0.12)",
  },
  status: {
    success: "#2F855A", // Deepened success green for Light mode readability
    warning: "#C05621", // Deepened warning amber
    danger: "#C53030",  // Deepened danger red
    info: "#2B6CB0",    // Deepened info blue
  },
  overlay: "rgba(0, 0, 0, 0.08)",
  shadow: "rgba(0, 0, 0, 0.05)",
};

export type ThemePalette = typeof darkPalette | typeof lightPalette;

export const useThemedColors = () => {
  const systemScheme = useColorScheme();
  const darkMode = useTimetableStore((state) => state.settings.darkMode);
  const isDark = darkMode ?? systemScheme === "dark";

  const colors = useMemo<ThemePalette>(() => {
    return isDark ? darkPalette : lightPalette;
  }, [isDark]);

  return { colors, isDark };
};

export const useThemedStyles = <T extends Record<string, any>>(factory: (colors: ThemePalette) => T) => {
  const { colors } = useThemedColors();
  return useMemo(
    () => StyleSheet.create(factory(colors) as StyleSheet.NamedStyles<T>),
    [factory, colors],
  );
};

export const getBackgroundGradient = (colors: ThemePalette) =>
  [colors.background.start, colors.background.end] as const;
