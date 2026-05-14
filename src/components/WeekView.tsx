import React, { useMemo, useRef } from "react";
import {
  View,
  StyleSheet,
  Text,
  TouchableOpacity,
  Animated,
  PanResponder,
  Dimensions,
} from "react-native";
import { BlurView } from "expo-blur";
import { LinearGradient } from "expo-linear-gradient";
import { Ionicons } from "@expo/vector-icons";
import DayPill from "./DayPill";
import { WeekDay } from "../types";
import { ThemePalette, useThemedColors } from "../theme/useTheme";

const { width: SCREEN_WIDTH } = Dimensions.get("window");
const SWIPE_THRESHOLD = 50;

interface WeekViewProps {
  days: WeekDay[];
  selectedDay: string;
  onDaySelect: (day: string) => void;
  weekLabel?: string;
  weekOffset?: number;
  onWeekChange?: (direction: "prev" | "next") => void;
  onGoToThisWeek?: () => void;
}

const WeekView: React.FC<WeekViewProps> = ({
  days,
  selectedDay,
  onDaySelect,
  weekLabel = "This Week",
  weekOffset = 0,
  onWeekChange,
  onGoToThisWeek,
}) => {
  const { colors, isDark } = useThemedColors();
  const styles = useMemo(() => createStyles(colors), [colors]);

  const getDateRange = () => {
    if (days.length === 0) return "";
    const firstDay = days[0];
    const lastDay = days[days.length - 1];
    const [, firstMonth, firstDate] = firstDay.fullDate.split("-");
    const [, lastMonth, lastDate] = lastDay.fullDate.split("-");
    
    const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
    const firstMonthName = months[parseInt(firstMonth) - 1];
    const lastMonthName = months[parseInt(lastMonth) - 1];
    
    if (firstMonth === lastMonth) {
      return `${firstMonthName} ${parseInt(firstDate)} – ${parseInt(lastDate)}`;
    }
    return `${firstMonthName} ${parseInt(firstDate)} – ${lastMonthName} ${parseInt(lastDate)}`;
  };

  return (
    <View style={styles.container}>
      {/* Week Header */}
      <View style={styles.weekHeader}>
        <TouchableOpacity
          onPress={() => onWeekChange?.("prev")}
          style={styles.navButton}
        >
          <BlurView intensity={45} tint={isDark ? "dark" : "light"} style={styles.navBlur}>
            <LinearGradient
              colors={
                isDark
                  ? ["rgba(255,255,255,0.12)", "rgba(255,255,255,0.01)"]
                  : ["rgba(255,255,255,0.40)", "rgba(255,255,255,0.02)"]
              }
              start={{ x: 0, y: 0 }}
              end={{ x: 1, y: 1 }}
              style={styles.navGradientBorder}
            >
              <View style={styles.navInner}>
                <Ionicons name="chevron-back" size={18} color={colors.text.secondary} />
              </View>
            </LinearGradient>
          </BlurView>
        </TouchableOpacity>
        
        <TouchableOpacity
          onPress={onGoToThisWeek}
          style={styles.weekLabelContainer}
          disabled={weekOffset === 0}
        >
          <Text style={[
            styles.weekLabel,
            weekOffset !== 0 && styles.weekLabelActive,
          ]}>
            {weekLabel}
          </Text>
          <Text style={styles.dateRange}>
            {getDateRange()}
          </Text>
        </TouchableOpacity>
        
        <TouchableOpacity
          onPress={() => onWeekChange?.("next")}
          style={styles.navButton}
        >
          <BlurView intensity={45} tint={isDark ? "dark" : "light"} style={styles.navBlur}>
            <LinearGradient
              colors={
                isDark
                  ? ["rgba(255,255,255,0.12)", "rgba(255,255,255,0.01)"]
                  : ["rgba(255,255,255,0.40)", "rgba(255,255,255,0.02)"]
              }
              start={{ x: 0, y: 0 }}
              end={{ x: 1, y: 1 }}
              style={styles.navGradientBorder}
            >
              <View style={styles.navInner}>
                <Ionicons name="chevron-forward" size={18} color={colors.text.secondary} />
              </View>
            </LinearGradient>
          </BlurView>
        </TouchableOpacity>
      </View>

      {/* Days Row */}
      <View style={styles.daysContainer}>
        <View style={styles.daysRow}>
          {days.map((day) => (
            <DayPill
              key={day.day}
              day={day}
              isSelected={selectedDay === day.day}
              onPress={() => onDaySelect(day.day)}
            />
          ))}
        </View>
      </View>
    </View>
  );
};

const createStyles = (colors: ThemePalette) =>
  StyleSheet.create({
    container: {
      marginVertical: 8,
      marginHorizontal: 16,
    },
    weekHeader: {
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "space-between",
      marginBottom: 10,
      paddingHorizontal: 2,
    },
    navButton: {
      borderRadius: 12,
      overflow: "hidden",
    },
    navBlur: {
      width: 36,
      height: 36,
      borderRadius: 12,
      overflow: "hidden",
    },
    navGradientBorder: {
      flex: 1,
      borderRadius: 12,
      padding: 1,
    },
    navInner: {
      flex: 1,
      borderRadius: 11,
      alignItems: "center",
      justifyContent: "center",
      backgroundColor: colors.glass.cardFill,
    },
    weekLabelContainer: {
      flex: 1,
      alignItems: "center",
    },
    weekLabel: {
      fontSize: 15,
      fontWeight: "500",
      color: colors.text.primary,
      letterSpacing: -0.4,
    },
    weekLabelActive: {
      color: colors.primary.teal,
    },
    dateRange: {
      fontSize: 11,
      color: colors.text.muted,
      marginTop: 1,
      fontWeight: "400",
    },
    daysContainer: {
      overflow: "visible",
      paddingVertical: 4,
    },
    daysRow: {
      flexDirection: "row",
      justifyContent: "space-between",
      alignItems: "center",
    },
  });

export default WeekView;
