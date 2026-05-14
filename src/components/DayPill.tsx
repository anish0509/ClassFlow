import React, { useEffect, useMemo, useRef } from "react";
import { Text, TouchableOpacity, StyleSheet, Animated, View } from "react-native";
import { BlurView } from "expo-blur";
import { LinearGradient } from "expo-linear-gradient";
import { WeekDay } from "../types";

import { ThemePalette, useThemedColors } from "../theme/useTheme";

interface DayPillProps {
  day: WeekDay;
  isSelected: boolean;
  onPress: () => void;
}

const DayPill: React.FC<DayPillProps> = ({ day, isSelected, onPress }) => {
  const isToday = day.isToday;
  const { colors, isDark } = useThemedColors();
  const styles = useMemo(() => createStyles(colors), [colors]);

  const scaleAnim = useRef(new Animated.Value(1)).current;

  useEffect(() => {
    Animated.spring(scaleAnim, {
      toValue: isSelected ? 1.05 : 1,
      friction: 8,
      tension: 100,
      useNativeDriver: true,
    }).start();
  }, [isSelected]);

  const handlePress = () => {

    Animated.sequence([
      Animated.timing(scaleAnim, {
        toValue: 0.92,
        duration: 60,
        useNativeDriver: true,
      }),
      Animated.spring(scaleAnim, {
        toValue: isSelected ? 1.05 : 1,
        friction: 5,
        tension: 200,
        useNativeDriver: true,
      }),
    ]).start();
    onPress();
  };

  const isActiveToday = isToday && isSelected;

  return (
    <Animated.View
      style={[styles.container, { transform: [{ scale: scaleAnim }] }]}
    >
      <TouchableOpacity onPress={handlePress} activeOpacity={0.7}>
        <BlurView
          intensity={12}
          tint="light"
          style={styles.blurContainer}
        >
          <LinearGradient
            colors={
              isDark
                ? ["rgba(255,255,255,0.15)", "rgba(255,255,255,0.02)"]
                : ["rgba(0,0,0,0.04)", "rgba(0,0,0,0.01)"]
            }
            start={{ x: 0, y: 0 }}
            end={{ x: 1, y: 1 }}
            style={styles.gradientBorder}
          >
            {isActiveToday || isSelected ? (
              <View
                style={[
                  styles.pill,
                  isActiveToday && styles.pillToday,
                  !isToday && isSelected && styles.pillSelected,
                ]}
              >
              <Text
                style={[
                  styles.dayText,
                  (isActiveToday || isSelected) && styles.dayTextActive,
                ]}
              >
                {day.day}
              </Text>
              <Text
                style={[
                  styles.dateText,
                  isActiveToday && styles.dateTextToday,
                  !isToday && isSelected && styles.dateTextSelected,
                ]}
              >
                {day.date}
              </Text>
            </View>
          ) : (
            <View
              style={[
                styles.pill,
                { borderColor: colors.glass.cardBorder }
              ]}
            >
              <Text
                style={[
                  styles.dayText,
                ]}
              >
                {day.day}
              </Text>
              <Text
                style={[
                  styles.dateText,
                ]}
              >
                {day.date}
              </Text>
              </View>
            )}
          </LinearGradient>
        </BlurView>
      </TouchableOpacity>
    </Animated.View>
  );
};

const createStyles = (colors: ThemePalette) =>
  StyleSheet.create({
    container: {
      flex: 1,
      marginHorizontal: 2,
    },
    blurContainer: {
      borderRadius: 14,
      overflow: "hidden",
    },
    gradientBorder: {
      borderRadius: 14,
      padding: 1, // Creates the gradient inner border
    },
    pill: {
      alignItems: "center",
      justifyContent: "center",
      paddingVertical: 12,
      paddingHorizontal: 4,
      borderRadius: 13,
      minWidth: 42,
      backgroundColor: colors.mode === "dark" ? "rgba(255, 255, 255, 0.03)" : "rgba(255, 255, 255, 0.4)",
      borderWidth: 1,
      borderColor: colors.mode === "dark" ? "rgba(255, 255, 255, 0.02)" : "rgba(0, 0, 0, 0.04)",
    },
    pillToday: {
      borderWidth: 1.5,
      borderColor: colors.mode === "dark" ? "rgba(255, 255, 255, 0.4)" : "#1A1F26",
      backgroundColor: colors.mode === "dark" ? "rgba(255, 255, 255, 0.12)" : "#1A1F26", // Graphite black fill for Light today
    },
    pillSelected: {
      borderWidth: 1.5,
      borderColor: colors.mode === "dark" ? "rgba(255, 255, 255, 0.3)" : "#1A1F26",
      backgroundColor: colors.mode === "dark" ? "rgba(255, 255, 255, 0.08)" : "rgba(26, 31, 38, 0.05)", 
    },
    dayText: {
      color: colors.text.muted,
      fontSize: 10,
      fontWeight: "600",
      marginBottom: 3,
      textTransform: "uppercase",
      letterSpacing: 0.3,
    },
    dayTextActive: {
      color: colors.mode === "dark" ? colors.text.primary : "#FFFFFF", // Force white text if selected on dark graphite pill
      fontWeight: "700",
    },
    dateText: {
      color: colors.text.secondary,
      fontSize: 16,
      fontWeight: "600",
    },
    dateTextToday: {
      color: "#FFFFFF", // Contrasts with dark graphite fill in light, and translucent glow in dark
      fontWeight: "700",
    },
    dateTextSelected: {
      color: colors.mode === "dark" ? colors.text.primary : "#1A1F26",
      fontWeight: "700",
    },
  });

export default DayPill;
