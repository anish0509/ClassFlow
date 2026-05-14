import React from "react";
import { View, Text } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import {
  ThemePalette,
  useThemedColors,
  useThemedStyles,
} from "../theme/useTheme";

interface CompletionBadgeProps {
  completedAt: string;
  color?: string;
}

const CompletionBadge: React.FC<CompletionBadgeProps> = ({
  completedAt,
  color,
}) => {
  const { colors } = useThemedColors();
  const styles = useThemedStyles(createStyles);
  const badgeColor = color || colors.primary.teal;

  const formatDateTime = (dateStr: string) => {
    const date = new Date(dateStr);
    const time = date.toLocaleTimeString("en-US", {
      hour: "2-digit",
      minute: "2-digit",
      hour12: false,
    });
    const dateFormatted = date.toLocaleDateString("en-US", {
      month: "short",
      day: "numeric",
    });
    const dayName = date.toLocaleDateString("en-US", { weekday: "short" });

    return { time, date: dateFormatted, day: dayName };
  };

  const { time, date, day } = formatDateTime(completedAt);

  return (
    <View style={styles.container}>
      <View
        style={[styles.iconContainer, { backgroundColor: `${badgeColor}20` }]}
      >
        <Ionicons name="checkmark-circle" size={20} color={badgeColor} />
      </View>
      <View style={styles.textContainer}>
        <Text style={styles.dateText}>
          {day}, {date}
        </Text>
        <Text style={styles.timeText}>{time}</Text>
      </View>
    </View>
  );
};

const createStyles = (colors: ThemePalette) => ({
  container: {
    flexDirection: "row",
    alignItems: "center",
  },
  iconContainer: {
    padding: 6,
    borderRadius: 10,
    marginRight: 10,
  },
  textContainer: {
    flex: 1,
  },
  dateText: {
    color: colors.text.secondary,
    fontSize: 13,
    fontWeight: "500",
  },
  timeText: {
    color: colors.text.muted,
    fontSize: 12,
    fontFamily: "monospace",
    marginTop: 2,
  },
});

export default CompletionBadge;
