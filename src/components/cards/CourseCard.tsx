import React from "react";
import { View, Text, TouchableOpacity, StyleSheet } from "react-native";
import { BlurView } from "expo-blur";
import { LinearGradient } from "expo-linear-gradient";
import { Ionicons } from "@expo/vector-icons";
import { Course } from "../types";

import {
  ThemePalette,
  useThemedColors,
  useThemedStyles,
} from "../theme/useTheme";

interface CourseCardProps {
  course: Course;
  onPress: () => void;
  onLongPress?: () => void;
}

const CourseCard: React.FC<CourseCardProps> = ({
  course,
  onPress,
  onLongPress,
}) => {
  const attendancePercentage =
    course.totalClasses && course.totalClasses > 0
      ? Math.round(((course.attendedClasses || 0) / course.totalClasses) * 100)
      : 0;

  const handlePress = () => {
    onPress();
  };

  const { colors, isDark } = useThemedColors();
  const styles = useThemedStyles(createStyles);

  return (
    <TouchableOpacity
      onPress={handlePress}
      onLongPress={onLongPress}
      activeOpacity={0.8}
    >
      <View style={styles.container}>
        <BlurView intensity={45} tint={isDark ? "dark" : "light"} style={styles.cardBlur}>
          <LinearGradient
            colors={
              isDark
                ? ["rgba(255, 255, 255, 0.15)", "rgba(255, 255, 255, 0.02)"]
                : ["rgba(0, 0, 0, 0.08)", "rgba(0, 0, 0, 0.02)"]
            }
            start={{ x: 0, y: 0 }}
            end={{ x: 1, y: 1 }}
            style={styles.gradientBorder}
          >
            <View style={[styles.cardInner, { borderLeftWidth: 4, borderLeftColor: course.color }]}>
              {isDark && (
                <>
                  {/* Top Edge Gradient Highlight */}
                  <LinearGradient
                    colors={["rgba(255,255,255,0)", "rgba(255,255,255,0.4)", "rgba(255,255,255,0)"]}
                    start={{ x: 0, y: 0 }}
                    end={{ x: 1, y: 0 }}
                    style={{ position: 'absolute', top: 0, left: 0, right: 0, height: 1, zIndex: 10 }}
                  />
                  {/* Left Edge Gradient Highlight */}
                  <LinearGradient
                    colors={["rgba(255,255,255,0.4)", "rgba(255,255,255,0)", "rgba(255,255,255,0.1)"]}
                    start={{ x: 0, y: 0 }}
                    end={{ x: 0, y: 1 }}
                    style={{ position: 'absolute', top: 0, left: 0, bottom: 0, width: 1, zIndex: 10 }}
                  />
                </>
              )}



              <View style={styles.content}>
                <View style={styles.mainContent}>
                  {/* Course name */}
                  <Text style={styles.courseName} numberOfLines={2}>
                    {course.name}
                  </Text>

                  {/* Professor and Room chips */}
                  <View style={styles.chipsRow}>
                    <View style={[styles.chip, { backgroundColor: colors.glass.badge, borderColor: colors.border.subtle }]}>
                      <Ionicons
                        name="person-outline"
                        size={12}
                        color={colors.text.secondary}
                      />
                      <Text style={[styles.chipText, { color: colors.text.secondary }]} numberOfLines={1} ellipsizeMode="tail">
                        {course.professor}
                      </Text>
                    </View>
                    <View style={[styles.chip, { backgroundColor: colors.glass.badge, borderColor: colors.border.subtle }]}>
                      <Ionicons
                        name="location-outline"
                        size={12}
                        color={colors.text.secondary}
                      />
                      <Text style={[styles.chipText, { color: colors.text.secondary }]} numberOfLines={1} ellipsizeMode="tail">
                        {course.room}
                      </Text>
                    </View>
                  </View>

                  {/* Glowing capsule progress metrics */}
                  <View style={styles.progressContainer}>
                    <View style={styles.progressHeader}>
                      <Text style={styles.progressLabel}>
                        {course.attendedClasses || 0}/{course.totalClasses || 0} classes completed
                      </Text>
                      <View style={[styles.percentageBadge, { backgroundColor: `${course.color}15` }]}>
                        <Text style={[styles.percentageText, { color: course.color }]}>
                          {attendancePercentage}%
                        </Text>
                      </View>
                    </View>

                    <View style={styles.progressBar}>
                      <View
                        style={[
                          styles.progressFill,
                          {
                            width: `${attendancePercentage}%`,
                            backgroundColor: course.color,
                          },
                        ]}
                      />
                    </View>
                  </View>
                </View>

                <Ionicons
                  name="chevron-forward"
                  size={18}
                  color={colors.text.muted}
                />
              </View>
            </View>
          </LinearGradient>
        </BlurView>
      </View>
    </TouchableOpacity>
  );
};

const createStyles = (colors: ThemePalette) => ({
  container: {
    marginHorizontal: 16,
    marginVertical: 4,
    borderRadius: 12,
    overflow: "hidden" as const,
  },
  cardBlur: {
    borderRadius: 12,
    overflow: "hidden" as const,
  },
  gradientBorder: {
    borderRadius: 12,
    padding: 1,
  },
  cardInner: {
    borderRadius: 11,
    backgroundColor: colors.glass.cardFill,
    flexDirection: "row" as const,
    overflow: "hidden" as const,
  },

  content: {
    flex: 1,
    flexDirection: "row" as const,
    alignItems: "center" as const,
    padding: 12,
    paddingLeft: 8, // Perfect balance to achieve symmetrical 12px margin alongside the 4px border!
  },

  mainContent: {
    flex: 1,
  },
  courseName: {
    color: colors.text.primary,
    fontSize: 14,
    fontWeight: "600" as const,
    marginBottom: 5,
    lineHeight: 18,
    letterSpacing: -0.2,
  },
  chipsRow: {
    flexDirection: "row" as const,
    flexWrap: "nowrap" as const,
    gap: 6,
    marginBottom: 6,
  },
  chip: {
    flexDirection: "row" as const,
    alignItems: "center" as const,
    paddingHorizontal: 6,
    paddingVertical: 3,
    borderRadius: 8,
    gap: 4,
    borderWidth: StyleSheet.hairlineWidth,
    flexShrink: 1,
  },
  chipText: {
    fontSize: 11,
    fontWeight: "500" as const,
  },
  progressContainer: {
    marginTop: 4,
  },
  progressHeader: {
    flexDirection: "row" as const,
    justifyContent: "space-between" as const,
    alignItems: "center" as const,
    marginBottom: 4,
  },
  progressLabel: {
    color: colors.text.secondary,
    fontSize: 12,
    fontWeight: "500" as const,
  },
  percentageBadge: {
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 6,
  },
  percentageText: {
    fontSize: 11,
    fontWeight: "700" as const,
  },
  progressBar: {
    height: 4,
    backgroundColor: colors.mode === "dark" ? "rgba(255, 255, 255, 0.06)" : "rgba(0, 0, 0, 0.09)",
    borderRadius: 2,
    overflow: "hidden" as const,
  },
  progressFill: {
    height: "100%" as const,
    borderRadius: 3,
  },
});

export default CourseCard;
