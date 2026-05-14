import React, { useRef, useEffect, useMemo } from "react";
import {
  View,
  Text,
  StyleSheet,
  Animated,
  TouchableOpacity,
  Dimensions,
} from "react-native";
import { Swipeable } from "react-native-gesture-handler";
import { BlurView } from "expo-blur";
import { LinearGradient } from "expo-linear-gradient";
import { Ionicons } from "@expo/vector-icons";
import { format } from "date-fns";
import { ClassSchedule, AttendanceStatus } from "../types";

import { useThemedColors } from "../theme/useTheme";

const { width: SCREEN_WIDTH } = Dimensions.get("window");

interface SwipeableClassCardProps {
  classItem: ClassSchedule & {
    attended?: boolean;
    attendanceStatus?: string;
  };
  selectedDateStr: string;
  isNextClass?: boolean;
  onMarkAttendance: (status: AttendanceStatus) => void;
  onEdit: () => void;
  onOpenModal: () => void;
}

const SwipeableClassCard: React.FC<SwipeableClassCardProps> = ({
  classItem,
  selectedDateStr,
  isNextClass = false,
  onMarkAttendance,
  onEdit,
  onOpenModal,
}) => {
  const { colors, isDark } = useThemedColors();
  const styles = useMemo(() => createStyles(colors, isDark), [colors, isDark]);
  const swipeableRef = useRef<Swipeable>(null);
  const fadeAnim = useRef(new Animated.Value(0)).current;
  const slideAnim = useRef(new Animated.Value(20)).current;

  const isAttended = classItem.attended;
  const status = classItem.attendanceStatus as AttendanceStatus | undefined;

  useEffect(() => {
    Animated.parallel([
      Animated.timing(fadeAnim, {
        toValue: 1,
        duration: 400,
        useNativeDriver: true,
      }),
      Animated.spring(slideAnim, {
        toValue: 0,
        tension: 80,
        friction: 12,
        useNativeDriver: true,
      }),
    ]).start();
  }, []);

  const today = format(new Date(), "yyyy-MM-dd");
  const isFutureDate = selectedDateStr > today;
  let canMarkAttendance = true; // Unlock marking for all classes (past/future/ongoing) for advanced planning!
  let isClassOver = false;
  let isOngoing = false;

  if (!isFutureDate && selectedDateStr === today) {
    const now = new Date();
    const [startHours, startMins] = classItem.startTime.split(":").map(Number);
    const [endHours, endMins] = classItem.endTime.split(":").map(Number);
    const classStart = new Date(now);
    classStart.setHours(startHours, startMins, 0, 0);
    const classEnd = new Date(now);
    classEnd.setHours(endHours, endMins, 0, 0);
    isClassOver = now >= classEnd;
    isOngoing = now >= classStart && now < classEnd;
  } else if (!isFutureDate) {
    isClassOver = true;
  }

  const getStatusConfig = () => {
    switch (status) {
      case "present":
        return {
          color: colors.status.success,
          bgColor: colors.status.success + "1F",
          icon: "checkmark-circle" as const,
          text: "Present",
        };
      case "absent":
        return {
          color: colors.status.danger,
          bgColor: colors.status.danger + "1F",
          icon: "close-circle" as const,
          text: "Absent",
        };
      case "canceled":
        return {
          color: colors.status.warning,
          bgColor: colors.status.warning + "1F",
          icon: "ban" as const,
          text: "Canceled",
        };
      case "shifted":
        return {
          color: colors.primary.purple,
          bgColor: colors.primary.purple + "1F",
          icon: "swap-horizontal" as const,
          text: "Shifted",
        };
      default:
        return null;
    }
  };

  const statusConfig = getStatusConfig();

  const getClassBadge = () => {
    if (statusConfig) {
      return { text: statusConfig.text, color: statusConfig.color, bgColor: statusConfig.bgColor };
    }
    if (isOngoing) {
      return { text: "LIVE", color: colors.status.success, bgColor: colors.status.success + "26" };
    }
    if (isNextClass) {
      return { text: "UP NEXT", color: colors.primary.cyan, bgColor: colors.primary.cyan + "26" };
    }
    if (!canMarkAttendance) {
      return { text: "SCHEDULED", color: colors.text.muted, bgColor: colors.glass.badge };
    }
    if (isClassOver && !isAttended) {
      return { text: "UNMARKED", color: colors.status.warning, bgColor: colors.status.warning + "26" };
    }
    return { text: "SCHEDULED", color: colors.text.muted, bgColor: colors.glass.badge };
  };

  const classBadge = getClassBadge();
  const accentColor = classItem.color || colors.primary.teal;

  // Safe color utility to handle opacity fades robustly across any hex variation
  const getAlphaColor = (hex: string, opacity: number) => {
    if (!hex || typeof hex !== "string") return "rgba(0,0,0,0)";
    let cleanHex = hex.replace("#", "");
    if (cleanHex.length === 3) {
      cleanHex = cleanHex.split("").map(c => c + c).join("");
    }
    const r = parseInt(cleanHex.substring(0, 2), 16) || 0;
    const g = parseInt(cleanHex.substring(2, 4), 16) || 0;
    const b = parseInt(cleanHex.substring(4, 6), 16) || 0;
    return `rgba(${r}, ${g}, ${b}, ${opacity})`;
  };

  const getCardBorderColor = () => {
    if (isOngoing && !status) return accentColor + "50";
    if (isNextClass && !status) return colors.primary.cyan + "40";
    if (status === "present") return colors.status.success + "30";
    if (status === "absent") return colors.status.danger + "30";
    if (status === "canceled") return colors.status.warning + "30";
    if (status === "shifted") return colors.primary.purple + "30";
    return colors.glass.cardBorder;
  };

  // Swipe actions
  const renderRightActions = (
    progress: Animated.AnimatedInterpolation<number>,
    dragX: Animated.AnimatedInterpolation<number>,
  ) => {
    if (!canMarkAttendance) return null;
    const scale = dragX.interpolate({ inputRange: [-100, 0], outputRange: [1, 0.5], extrapolate: "clamp" });
    const opacity = dragX.interpolate({ inputRange: [-100, -50, 0], outputRange: [1, 0.8, 0], extrapolate: "clamp" });

    return (
      <Animated.View style={[styles.swipeAction, styles.absentAction, { opacity }]}>
        <Animated.View style={{ transform: [{ scale }] }}>
          <View style={styles.swipeActionContent}>
            <Ionicons name="close-circle" size={28} color="#fff" />
            <Text style={styles.swipeActionText}>Absent</Text>
          </View>
        </Animated.View>
      </Animated.View>
    );
  };

  const renderLeftActions = (
    progress: Animated.AnimatedInterpolation<number>,
    dragX: Animated.AnimatedInterpolation<number>,
  ) => {
    if (!canMarkAttendance) return null;
    const scale = dragX.interpolate({ inputRange: [0, 100], outputRange: [0.5, 1], extrapolate: "clamp" });
    const opacity = dragX.interpolate({ inputRange: [0, 50, 100], outputRange: [0, 0.8, 1], extrapolate: "clamp" });

    return (
      <Animated.View style={[styles.swipeAction, styles.presentAction, { opacity }]}>
        <Animated.View style={{ transform: [{ scale }] }}>
          <View style={styles.swipeActionContent}>
            <Ionicons name="checkmark-circle" size={28} color="#fff" />
            <Text style={styles.swipeActionText}>Present</Text>
          </View>
        </Animated.View>
      </Animated.View>
    );
  };

  const handleSwipeLeft = () => {
    if (canMarkAttendance) {

      onMarkAttendance("present");
      swipeableRef.current?.close();
    }
  };

  const handleSwipeRight = () => {
    if (canMarkAttendance) {

      onMarkAttendance("absent");
      swipeableRef.current?.close();
    }
  };

  return (
    <Animated.View style={{ opacity: fadeAnim, transform: [{ translateY: slideAnim }] }}>
      <Swipeable
        ref={swipeableRef}
        renderRightActions={renderRightActions}
        renderLeftActions={renderLeftActions}
        onSwipeableOpen={(direction: "left" | "right") => {
          if (direction === "left") handleSwipeLeft();
          else if (direction === "right") handleSwipeRight();
        }}
        friction={2}
        overshootLeft={false}
        overshootRight={false}
        enabled={canMarkAttendance && !isAttended}
      >
        <TouchableOpacity
          style={styles.classCard}
          onLongPress={onEdit}
          onPress={onOpenModal}
          activeOpacity={0.85}
          delayLongPress={300}
        >
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
              <View style={[styles.cardInner, { borderLeftWidth: 4, borderLeftColor: accentColor }]}>
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
                <View style={styles.cardMain}>
                {/* Header Row */}
                <View style={styles.headerRow}>
                  <View style={styles.titleSection}>
                    <Text style={[styles.courseName, { color: colors.text.primary }]} numberOfLines={1}>
                      {classItem.courseName || classItem.shortName}
                    </Text>
                  </View>
                  <View style={[styles.statusBadge, { backgroundColor: classBadge.bgColor }]}>
                    {isOngoing && !status && <View style={[styles.liveDot, { backgroundColor: colors.status.success }]} />}
                    <Text style={[styles.statusBadgeText, { color: classBadge.color }]}>
                      {classBadge.text}
                    </Text>
                  </View>
                </View>

                {/* Info chips */}
                <View style={styles.chipsRow}>
                  <View style={[styles.chip, { backgroundColor: colors.glass.badge, borderColor: colors.border.subtle }]}>
                    <Ionicons name="time-outline" size={13} color={colors.text.secondary} />
                    <Text style={[styles.chipText, { color: colors.text.secondary }]} numberOfLines={1} ellipsizeMode="tail">
                      {classItem.startTime} – {classItem.endTime}
                    </Text>
                  </View>
                  <View style={[styles.chip, { backgroundColor: colors.glass.badge, borderColor: colors.border.subtle }]}>
                    <Ionicons name="location-outline" size={13} color={colors.text.secondary} />
                    <Text style={[styles.chipText, { color: colors.text.secondary }]} numberOfLines={1} ellipsizeMode="tail">{classItem.room}</Text>
                  </View>
                  <View style={[styles.chip, { backgroundColor: colors.glass.badge, borderColor: colors.border.subtle }]}>
                    <Ionicons name="person-outline" size={13} color={colors.text.secondary} />
                    <Text style={[styles.chipText, { color: colors.text.secondary }]} numberOfLines={1} ellipsizeMode="tail">{classItem.professor}</Text>
                  </View>
                </View>

                {/* Bottom */}
                <View style={styles.cardBottom}>
                  {isAttended && statusConfig ? (
                    <TouchableOpacity
                      onPress={onOpenModal}
                      style={[styles.attendedBadge, { backgroundColor: statusConfig.bgColor }]}
                    >
                      <Ionicons name={statusConfig.icon} size={16} color={statusConfig.color} />
                      <Text style={[styles.attendedText, { color: statusConfig.color }]}>
                        {statusConfig.text}
                      </Text>
                      <Ionicons name="pencil-outline" size={12} color={statusConfig.color} style={{ marginLeft: 4, opacity: 0.7 }} />
                    </TouchableOpacity>
                  ) : !canMarkAttendance ? (
                    <View style={[styles.upcomingBadge, { backgroundColor: colors.glass.badge }]}>
                      <Ionicons name="time-outline" size={14} color={colors.text.muted} />
                      <Text style={[styles.upcomingText, { color: colors.text.muted }]}>Upcoming</Text>
                    </View>
                  ) : canMarkAttendance && !isAttended ? (
                    <View style={styles.swipeHint}>
                      <Ionicons name="swap-horizontal-outline" size={14} color={colors.text.muted} />
                      <Text style={[styles.swipeHintText, { color: colors.text.muted }]}>Swipe or tap to mark</Text>
                    </View>
                  ) : null}
                </View>
              </View>
              </View>
            </LinearGradient>
          </BlurView>
        </TouchableOpacity>
      </Swipeable>
    </Animated.View>
  );
};

const createStyles = (colors: any, isDark: boolean) => StyleSheet.create({
  classCard: {
    marginHorizontal: 16,
    marginVertical: 4, // Condensed gap
    borderRadius: 16, // Sleeker sharpness
    overflow: "hidden",
    shadowColor: isDark ? "#000" : colors.shadow,
    shadowOffset: { width: 0, height: 3 },
    shadowOpacity: isDark ? 0.25 : 0.12,
    shadowRadius: 8,
  },
  cardBlur: {
    borderRadius: 16,
    overflow: "hidden",
  },
  gradientBorder: {
    borderRadius: 16,
    padding: 1, 
  },
  cardInner: {
    borderRadius: 15,
    flexDirection: "row",
    backgroundColor: colors.glass.cardFill,
    overflow: "hidden",
  },
  cardMain: {
    flex: 1,
    paddingVertical: 9, // Tighter padding
    paddingHorizontal: 12,
    paddingLeft: 8, // Perfectly balances the 4px border to achieve 12px symmetrical margin!
  },
  headerRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "flex-start",
    marginBottom: 5, // Condensed gap
  },
  titleSection: {
    flex: 1,
    marginRight: 10,
  },
  courseName: {
    fontSize: 16,
    fontWeight: "700", // Thicker for strong visual hierarchy
    letterSpacing: -0.3, 
    lineHeight: 20,
    color: colors.text.primary,
  },
  shortName: {
    fontSize: 12,
    fontWeight: "500",
    marginTop: 1.5,
    letterSpacing: -0.1,
    color: colors.text.secondary,
  },
  statusBadge: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 8,
    gap: 4,
  },
  statusBadgeText: {
    fontSize: 10,
    fontWeight: "700",
    textTransform: "uppercase",
    letterSpacing: 0.6,
  },
  liveDot: {
    width: 6,
    height: 6,
    borderRadius: 3,
  },
  chipsRow: {
    flexDirection: "row",
    flexWrap: "nowrap",
    gap: 4,
    marginBottom: 6, // Condensed gap
  },
  chip: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 6,
    paddingVertical: 3, // Tighter chips
    borderRadius: 6,
    gap: 3,
    borderWidth: StyleSheet.hairlineWidth,
    flexShrink: 1,
  },
  chipText: {
    fontSize: 11,
    fontWeight: "500",
  },
  cardBottom: {
    flexDirection: "row",
    justifyContent: "flex-end",
    alignItems: "center",
    minHeight: 24, // Systematically compressed lock height!
  },
  attendedBadge: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 8,
    paddingVertical: 4, // Condensed pill
    borderRadius: 8,
    gap: 4,
  },
  attendedText: {
    fontSize: 12, // Micro text
    fontWeight: "600",
  },
  swipeHint: {
    flexDirection: "row",
    alignItems: "center",
    gap: 5,
    opacity: 0.5,
  },
  swipeHintText: {
    fontSize: 10,
    fontWeight: "500",
  },
  upcomingBadge: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 8,
    paddingVertical: 4, // Condensed pill
    borderRadius: 8,
    gap: 4,
  },
  upcomingText: {
    fontSize: 11, // Micro text
    fontWeight: "500",
  },
  swipeAction: {
    justifyContent: "center",
    alignItems: "center",
    width: 90,
    marginVertical: 5,
  },
  presentAction: {
    backgroundColor: "#30D158",
    borderTopLeftRadius: 18,
    borderBottomLeftRadius: 18,
    marginLeft: 16,
  },
  absentAction: {
    backgroundColor: "#FF453A",
    borderTopRightRadius: 18,
    borderBottomRightRadius: 18,
    marginRight: 16,
  },
  swipeActionContent: {
    alignItems: "center",
    justifyContent: "center",
  },
  swipeActionText: {
    color: "#fff",
    fontSize: 11,
    fontWeight: "600",
    marginTop: 3,
  },
});

export default SwipeableClassCard;
