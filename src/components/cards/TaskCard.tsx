import React, { useRef, useEffect } from "react";
import {
  View,
  Text,
  StyleSheet,
  Pressable,
  Animated,
  Easing,
  Dimensions,
} from "react-native";
import { BlurView } from "expo-blur";
import { LinearGradient } from "expo-linear-gradient";
import { Ionicons } from "@expo/vector-icons";
import { Task } from "../types";
import {
  ThemePalette,
  useThemedColors,
  useThemedStyles,
} from "../theme/useTheme";


const { width: SCREEN_WIDTH } = Dimensions.get("window");

interface TaskCardProps {
  task: Task;
  onComplete: () => void;
  onUncomplete?: () => void;
  onDelete: () => void;
}

const TaskCard: React.FC<TaskCardProps> = ({
  task,
  onComplete,
  onUncomplete,
  onDelete,
}) => {
  const { colors, isDark } = useThemedColors();
  const styles = useThemedStyles(createStyles);
  const isCompleted = task.status === "completed";

  // Animated values
  const scale = useRef(new Animated.Value(1)).current;
  const checkScale = useRef(new Animated.Value(isCompleted ? 1 : 0)).current;

  // Checkbox animation
  useEffect(() => {
    Animated.spring(checkScale, {
      toValue: isCompleted ? 1 : 0,
      friction: 6,
      tension: 180,
      useNativeDriver: true,
    }).start();
  }, [isCompleted]);

  const handlePress = () => {
    if (isCompleted && onUncomplete) {

      onUncomplete();
    } else {

      onComplete();
    }
  };

  const handlePressIn = () => {
    Animated.spring(scale, {
      toValue: 0.97,
      friction: 8,
      tension: 400,
      useNativeDriver: true,
    }).start();
  };

  const handlePressOut = () => {
    Animated.spring(scale, {
      toValue: 1,
      friction: 6,
      tension: 300,
      useNativeDriver: true,
    }).start();
  };

  const formatDateTime = (dateStr?: string) => {
    if (!dateStr) return null;
    const date = new Date(dateStr);
    const dayNames = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
    const monthNames = [
      "Jan", "Feb", "Mar", "Apr", "May", "Jun",
      "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
    ];
    const dateLabel = `${dayNames[date.getDay()]}, ${monthNames[date.getMonth()]} ${date.getDate()}`;
    if (dateStr.includes("T")) {
      const hours = date.getHours();
      const minutes = date.getMinutes();
      const ampm = hours >= 12 ? "PM" : "AM";
      const displayHours = hours % 12 || 12;
      const displayMinutes = minutes < 10 ? `0${minutes}` : minutes;
      return `${dateLabel} at ${displayHours}:${displayMinutes} ${ampm}`;
    }
    return dateLabel;
  };

  return (
    <Animated.View style={[styles.container, { transform: [{ scale }] }]}>
      <View style={styles.cardBlur}>
        <BlurView intensity={isDark ? 50 : 80} tint={isDark ? "dark" : "light"} style={StyleSheet.absoluteFill} />
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
          <View style={styles.cardInner}>
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

            {/* Content */}
            <Pressable 
              style={styles.content}
              onLongPress={onDelete}
              delayLongPress={500}
            >
              <View style={styles.textContent}>
                <Text
                  style={[styles.title, isCompleted && styles.titleCompleted]}
                  numberOfLines={2}
                >
                  {task.title}
                </Text>
                {task.dueDate && (
                  <View style={styles.dateRow}>
                    <View style={styles.dateIconWrapper}>
                      <Ionicons
                        name="calendar-outline"
                        size={11}
                        color={isCompleted ? colors.text.muted : colors.text.secondary}
                      />
                    </View>
                    <Text
                      style={[styles.dateText, isCompleted && styles.dateCompleted]}
                    >
                      {formatDateTime(task.dueDate)}
                    </Text>
                  </View>
                )}
              </View>

              <View style={{ flexDirection: "row", alignItems: "center", gap: 16 }}>
                {/* Direct Delete Trash Button */}
                <Pressable
                  onPress={onDelete}
                  hitSlop={10}
                  style={({ pressed }) => [
                    styles.deleteButton,
                    { opacity: pressed ? 0.4 : 0.7 }
                  ]}
                >
                  <Ionicons
                    name="trash-outline"
                    size={20}
                    color={colors.status.danger || "#FF453A"}
                  />
                </Pressable>

                {/* Checkbox */}
                <Pressable
                  onPress={handlePress}
                  onPressIn={handlePressIn}
                  onPressOut={handlePressOut}
                  hitSlop={12}
                >
                  <View
                    style={[
                      styles.checkbox,
                      isCompleted && styles.checkboxCompleted,
                    ]}
                  >
                    {/* Inner shine for glass checkbox */}
                    {!isCompleted && (
                      <LinearGradient
                        colors={[
                          "rgba(255, 255, 255, 0.2)",
                          "rgba(255, 255, 255, 0.05)",
                          "transparent",
                        ]}
                        start={{ x: 0, y: 0 }}
                        end={{ x: 0, y: 1 }}
                        style={styles.checkboxShine}
                      />
                    )}
                    <Animated.View
                      style={{
                        transform: [{ scale: checkScale }],
                        opacity: checkScale,
                      }}
                    >
                      <Ionicons name="checkmark" size={18} color="#fff" />
                    </Animated.View>
                  </View>
                </Pressable>
              </View>
            </Pressable>
          </View>
        </LinearGradient>
      </View>
    </Animated.View>
  );
};

const createStyles = (colors: ThemePalette) =>
  StyleSheet.create({
    container: {
      marginVertical: 6,
      borderRadius: 20,
      shadowColor: "#000",
      shadowOffset: { width: 0, height: 4 },
      shadowOpacity: 0.15,
      shadowRadius: 10,
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
      backgroundColor: colors.glass.cardFill,
      overflow: "hidden",
    },
    content: {
      flexDirection: "row",
      alignItems: "center",
      paddingHorizontal: 18,
      paddingVertical: 18,
    },
    textContent: {
      flex: 1,
      marginRight: 16,
    },
    title: {
      color: colors.text.primary,
      fontSize: 16,
      fontWeight: "600",
      letterSpacing: 0.3,
    },
    titleCompleted: {
      textDecorationLine: "line-through",
      color: colors.text.muted,
    },
    dateRow: {
      flexDirection: "row",
      alignItems: "center",
      marginTop: 6,
    },
    dateIconWrapper: {
      marginRight: 5,
    },
    dateText: {
      color: colors.text.secondary,
      fontSize: 13,
      fontWeight: "500",
    },
    dateCompleted: {
      color: colors.text.muted,
    },
    checkbox: {
      width: 26,
      height: 26,
      borderRadius: 13,
      borderWidth: 1.5,
      borderColor: colors.glass.border,
      backgroundColor: colors.glass.medium,
      alignItems: "center",
      justifyContent: "center",
      overflow: "hidden",
    },
    deleteButton: {
      width: 30,
      height: 30,
      justifyContent: "center",
      alignItems: "center",
      borderRadius: 15,
    },
    checkboxCompleted: {
      backgroundColor: "#30D158",
      borderColor: "#30D158",
    },
    checkboxShine: {
      ...StyleSheet.absoluteFillObject,
      borderRadius: 14,
    },
  });

export default TaskCard;
