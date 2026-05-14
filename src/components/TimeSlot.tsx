import React from "react";
import { View, Text } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import {
  ThemePalette,
  useThemedColors,
  useThemedStyles,
} from "../theme/useTheme";

interface TimeSlotProps {
  start: string;
  end: string;
}

const TimeSlot: React.FC<TimeSlotProps> = ({ start, end }) => {
  const { colors } = useThemedColors();
  const styles = useThemedStyles(createStyles);

  return (
    <View style={styles.container}>
      <Ionicons name="time-outline" size={14} color={colors.text.secondary} />
      <Text style={styles.timeText}>
        {start} - {end}
      </Text>
    </View>
  );
};

const createStyles = (colors: ThemePalette) => ({
  container: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: colors.glass.dark,
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 12,
  },
  timeText: {
    color: colors.text.secondary,
    fontSize: 12,
    marginLeft: 4,
    fontFamily: "monospace",
    fontWeight: "500",
  },
});

export default TimeSlot;
