import React, { useMemo } from "react";
import { View, Text, Switch, StyleSheet } from "react-native";
import { BlurView } from "expo-blur";
import { ThemePalette, useThemedColors } from "../theme/useTheme";

interface NotificationToggleProps {
  label: string;
  description?: string;
  value: boolean;
  onToggle: () => void;
}

const NotificationToggle: React.FC<NotificationToggleProps> = ({
  label,
  description,
  value,
  onToggle,
}) => {
  const { colors } = useThemedColors();
  const styles = useMemo(() => createStyles(colors), [colors]);
  return (
    <View style={styles.wrapper}>
      <BlurView intensity={12} tint="light" style={styles.blurContainer}>
        <View style={styles.container}>
          <View style={styles.textContainer}>
            <Text style={styles.label}>{label}</Text>
            {description && <Text style={styles.description}>{description}</Text>}
          </View>
          <Switch
            value={value}
            onValueChange={onToggle}
            trackColor={{
              false: "rgba(255, 255, 255, 0.15)",
              true: `${colors.primary.teal}80`,
            }}
            thumbColor={value ? colors.primary.teal : "#b0b0b0"}
            ios_backgroundColor="rgba(0,0,0,0.3)"
          />
        </View>
      </BlurView>
    </View>
  );
};

const createStyles = (colors: ThemePalette) =>
  StyleSheet.create({
    wrapper: {
      borderRadius: 24,
      overflow: "hidden",
      marginBottom: 16,
    },
    blurContainer: {
      borderRadius: 24,
    },
    container: {
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "space-between",
      backgroundColor: colors.glass.cardFill,
      paddingVertical: 18,
      paddingHorizontal: 20,
      borderRadius: 24,
      borderWidth: 1,
      borderColor: colors.glass.cardBorder,
    },
    textContainer: {
      flex: 1,
      marginRight: 16,
    },
    label: {
      color: colors.text.primary,
      fontSize: 16,
      fontWeight: "600",
      letterSpacing: -0.3,
    },
    description: {
      color: "rgba(235, 235, 245, 0.6)",
      fontSize: 13,
      marginTop: 4,
    },
  });

export default NotificationToggle;
