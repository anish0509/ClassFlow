import React, { useEffect, useRef } from "react";
import { View, Text, StyleSheet, Animated } from "react-native";
import { BlurView } from "expo-blur";
import { LinearGradient } from "expo-linear-gradient";
import { Ionicons } from "@expo/vector-icons";
import { useThemedColors } from "../theme/useTheme";
import { ClassSchedule, Course } from "../types";

interface UpNextBannerProps {
  classItem: ClassSchedule;
  course: Course;
  minutesUntil: number;
  hoursUntil: number;
  minsRemaining: number;
}

export const UpNextBanner: React.FC<UpNextBannerProps> = ({
  classItem,
  course,
  minutesUntil,
  hoursUntil,
  minsRemaining,
}) => {
  const { colors, isDark } = useThemedColors();
  const pulseAnim = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    Animated.loop(
      Animated.sequence([
        Animated.timing(pulseAnim, {
          toValue: 1,
          duration: 1500,
          useNativeDriver: true,
        }),
        Animated.timing(pulseAnim, {
          toValue: 0,
          duration: 1500,
          useNativeDriver: true,
        }),
      ])
    ).start();
  }, [pulseAnim]);

  const glowOpacity = pulseAnim.interpolate({
    inputRange: [0, 1],
    outputRange: [0.1, 0.4],
  });

  const glowScale = pulseAnim.interpolate({
    inputRange: [0, 1],
    outputRange: [0.98, 1.02],
  });

  let timeString = "";
  if (hoursUntil > 0) {
    timeString = `${hoursUntil}h ${minsRemaining}m`;
  } else {
    timeString = `${minutesUntil}m`;
  }

  // Determine urgency
  const isUrgent = minutesUntil <= 15;
  const accentColor = isUrgent ? (colors.status.danger || "#FF453A") : course.color || colors.primary.teal;

  return (
    <View style={styles.container}>
      <Animated.View 
        style={[
          styles.glowLayer, 
          { 
            backgroundColor: accentColor,
            opacity: glowOpacity,
            transform: [{ scale: glowScale }]
          }
        ]} 
      />
      <View style={styles.cardBlur}>
        <BlurView intensity={isDark ? 60 : 90} tint={isDark ? "dark" : "light"} style={StyleSheet.absoluteFill} />
        
        <LinearGradient
          colors={
            isDark
              ? ["rgba(255, 255, 255, 0.2)", "rgba(255, 255, 255, 0.05)"]
              : ["rgba(0, 0, 0, 0.1)", "rgba(0, 0, 0, 0.02)"]
          }
          start={{ x: 0, y: 0 }}
          end={{ x: 1, y: 1 }}
          style={styles.gradientBorder}
        >
          <View style={[styles.cardInner, { backgroundColor: colors.glass.cardFill }]}>
            <View style={[styles.colorBar, { backgroundColor: accentColor }]} />
            
            <View style={styles.content}>
              <View style={styles.headerRow}>
                <Ionicons name="time-outline" size={16} color={accentColor} />
                <Text style={[styles.upNextText, { color: accentColor }]}>UP NEXT IN {timeString}</Text>
              </View>
              
              <Text style={[styles.courseName, { color: colors.text.primary }]} numberOfLines={1}>
                {course.name}
              </Text>
              
              <View style={styles.detailsRow}>
                <View style={styles.detailItem}>
                  <Ionicons name="location-outline" size={14} color={colors.text.secondary} />
                  <Text style={[styles.detailText, { color: colors.text.secondary }]}>
                    {classItem.room || "TBA"}
                  </Text>
                </View>
                <View style={styles.detailItem}>
                  <Ionicons name="time" size={14} color={colors.text.secondary} />
                  <Text style={[styles.detailText, { color: colors.text.secondary }]}>
                    {classItem.startTime} - {classItem.endTime}
                  </Text>
                </View>
              </View>
            </View>
          </View>
        </LinearGradient>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    marginHorizontal: 16,
    marginVertical: 12,
    position: "relative",
  },
  glowLayer: {
    ...StyleSheet.absoluteFillObject,
    borderRadius: 24,
    filter: "blur(10px)",
  },
  cardBlur: {
    borderRadius: 20,
    overflow: "hidden",
  },
  gradientBorder: {
    borderRadius: 20,
    padding: 1,
  },
  cardInner: {
    borderRadius: 19,
    flexDirection: "row",
    overflow: "hidden",
  },
  colorBar: {
    width: 6,
    height: "100%",
  },
  content: {
    flex: 1,
    padding: 16,
  },
  headerRow: {
    flexDirection: "row",
    alignItems: "center",
    marginBottom: 6,
    gap: 6,
  },
  upNextText: {
    fontSize: 12,
    fontWeight: "800",
    letterSpacing: 1,
  },
  courseName: {
    fontSize: 18,
    fontWeight: "700",
    marginBottom: 8,
  },
  detailsRow: {
    flexDirection: "row",
    alignItems: "center",
    gap: 16,
  },
  detailItem: {
    flexDirection: "row",
    alignItems: "center",
    gap: 4,
  },
  detailText: {
    fontSize: 13,
    fontWeight: "500",
  },
});
