import React, { useMemo } from "react";
import { View, Text, StyleSheet } from "react-native";
import { BlurView } from "expo-blur";
const AnimatedBlurView = Animated.createAnimatedComponent(BlurView);
import { LinearGradient } from "expo-linear-gradient";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { ThemePalette, useThemedColors } from "../theme/useTheme";

import { TouchableOpacity, Animated } from "react-native";
import { Ionicons } from "@expo/vector-icons";

interface GlassHeaderProps {
  title: string;
  subtitle?: string;
  rightComponent?: React.ReactNode;
  onBack?: () => void;
  scrollY?: Animated.Value;
}

const GlassHeader: React.FC<GlassHeaderProps> = ({
  title,
  subtitle,
  rightComponent,
  onBack,
  scrollY,
}) => {
  const insets = useSafeAreaInsets();
  const { colors, isDark } = useThemedColors();
  const styles = useMemo(() => createStyles(colors), [colors]);

  // Animation Values setup
  const scroll = scrollY || new Animated.Value(0);

  const headerTranslateY = scroll.interpolate({
    inputRange: [-100, 0, 100],
    outputRange: [0, 0, -10], // Minimal nudge up on scroll
    extrapolate: 'clamp',
  });

  const titleScale = scroll.interpolate({
    inputRange: [-100, 0, 100],
    outputRange: [1.1, 1, 0.85], // Overscroll expand, Upward scroll shrink
    extrapolate: 'clamp',
  });

  const titleTranslateY = scroll.interpolate({
    inputRange: [-100, 0, 100],
    outputRange: [10, 0, 0], 
    extrapolate: 'clamp',
  });

  const opacity = scroll.interpolate({
    inputRange: [-100, 0, 40],
    outputRange: [1, 1, 0.9],
    extrapolate: 'clamp',
  });

  return (
    <AnimatedBlurView
      intensity={40}
      tint={isDark ? "dark" : "light"}
      style={[
        styles.container, 
        { paddingTop: insets.top + 8 },
        { transform: [{ translateY: headerTranslateY }] }
      ]}
    >
      <LinearGradient
        colors={[colors.glass.dark, colors.glass.medium]}
        start={{ x: 0, y: 0 }}
        end={{ x: 0, y: 1 }}
        style={styles.gradientBorder}
      >
        <View style={styles.contentContainer}>
          {/* Top Edge Glow */}
          <View style={[StyleSheet.absoluteFill, styles.edgeGlow]} pointerEvents="none" />
          <Animated.View style={[styles.content, onBack && { paddingLeft: 60 }, { opacity }]}>
            {onBack && (
              <TouchableOpacity
                onPress={onBack}
                style={styles.backBtn}
              >
                <Ionicons name="chevron-back" size={24} color={colors.text.primary} />
              </TouchableOpacity>
            )}
            <Animated.View style={[
              styles.textContainer, 
              { 
                transform: [
                  { scale: titleScale },
                  { translateY: titleTranslateY }
                ] 
              }
            ]}>
              <Text style={styles.title}>{title}</Text>
              {subtitle && <Text style={styles.subtitle}>{subtitle}</Text>}
            </Animated.View>
            {rightComponent && (
              <Animated.View style={[styles.rightContainer, { transform: [{ scale: titleScale }] }]}>
                {rightComponent}
              </Animated.View>
            )}
          </Animated.View>
        </View>
      </LinearGradient>
    </AnimatedBlurView>
  );
};

const createStyles = (colors: ThemePalette) =>
  StyleSheet.create({
    container: {
      backgroundColor: "transparent",
      overflow: "hidden",
    },
    gradientBorder: {
      borderBottomWidth: 1,
      borderBottomColor: colors.glass.border,
    },
    contentContainer: {
      // Removed flex:1 to fix collapsing header
    },
    edgeGlow: {
      borderTopWidth: 1,
      borderColor: colors.mode === "dark" ? "rgba(255,255,255,0.15)" : "rgba(255,255,255,0.6)",
      opacity: 0.8,
    },
    content: {
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "space-between",
      paddingHorizontal: 20,
      paddingBottom: 14,
    },
    textContainer: {
      flex: 1,
    },
    title: {
      color: colors.text.primary,
      fontSize: 32,
      fontWeight: "600",
      letterSpacing: -0.8,
    },
    subtitle: {
      color: colors.text.secondary,
      fontSize: 14,
      marginTop: 2,
      fontWeight: "400",
      letterSpacing: -0.08,
    },
    backBtn: {
      position: "absolute",
      left: 16,
      bottom: 16, // center relative to content row visually
      width: 36,
      height: 36,
      alignItems: "center",
      justifyContent: "center",
      borderRadius: 18,
      backgroundColor: colors.glass.cardFill,
      borderWidth: 1,
      borderColor: colors.glass.cardBorder,
      zIndex: 10,
    },
    rightContainer: {
      marginLeft: 16,
    },
  });

export default GlassHeader;
