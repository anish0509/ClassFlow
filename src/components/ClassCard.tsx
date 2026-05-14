import React from "react";
import { View, Text, TouchableOpacity } from "react-native";
import { BlurView } from "expo-blur";
import { LinearGradient } from "expo-linear-gradient";
import { Ionicons } from "@expo/vector-icons";
import { Class } from "../types";
import AttendButton from "./AttendButton";
import TimeSlot from "./TimeSlot";
import {
  ThemePalette,
  useThemedColors,
  useThemedStyles,
} from "../theme/useTheme";

interface ClassCardProps {
  classItem: Class;
  onAttend: () => void;
  showAttendButton?: boolean;
}

const ClassCard: React.FC<ClassCardProps> = ({
  classItem,
  onAttend,
  showAttendButton = true,
}) => {
  const { colors, isDark } = useThemedColors();
  const styles = useThemedStyles(createStyles);

  return (
    <View style={styles.outerShadow}>
      <BlurView intensity={80} tint={isDark ? "dark" : "light"} style={styles.blurContainer}>
        <LinearGradient
          colors={
            isDark 
              ? ['rgba(255,255,255,0.03)', 'rgba(255,255,255,0.01)']
              : ['rgba(255,255,255,0.45)', 'rgba(255,255,255,0.10)']
          }
          start={{ x: 0.05, y: 0 }}
          end={{ x: 1, y: 1 }}
          style={styles.gradient}
        >
          <View style={styles.glowBorder}>
            <View style={styles.innerGlow}>
              <View style={styles.content}>
                {/* Color accent bar */}
                <View
                  style={[styles.accentBar, { backgroundColor: classItem.color }]}
                />

                <View style={styles.mainContent}>
                  {/* Top row - Time and Room */}
                  <View style={styles.topRow}>
                    <TimeSlot
                      start={classItem.time.start}
                      end={classItem.time.end}
                    />
                    <View style={styles.roomContainer}>
                      <Ionicons
                        name="location-outline"
                        size={14}
                        color={colors.text.secondary}
                      />
                      <Text style={styles.roomText}>{classItem.room}</Text>
                    </View>
                  </View>

                  {/* Class name */}
                  <Text style={styles.className} numberOfLines={2}>
                    {classItem.name}
                  </Text>

                  {/* Professor */}
                  <View style={styles.professorRow}>
                    <Ionicons
                      name="person-outline"
                      size={14}
                      color={colors.text.muted}
                    />
                    <Text style={styles.professorText}>{classItem.professor}</Text>
                  </View>

                  {/* Bottom row - Attend button */}
                  {showAttendButton && (
                    <View style={styles.bottomRow}>
                      <AttendButton
                        onPress={onAttend}
                        isCompleted={classItem.completed}
                        color={classItem.color}
                      />
                    </View>
                  )}
                </View>
              </View>
            </View>
          </View>
        </LinearGradient>
      </BlurView>
    </View>
  );
};

const createStyles = (colors: ThemePalette) => ({
  outerShadow: {
    marginHorizontal: 16,
    marginVertical: 14,
    borderRadius: 32,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 10 },
    shadowOpacity: 0.15,
    shadowRadius: 20,
    elevation: 8,
    backgroundColor: 'transparent',
  },
  blurContainer: {
    borderRadius: 32,
    overflow: 'hidden',
  },
  gradient: {
    borderRadius: 32,
    borderWidth: 1.5,
    borderColor: colors.glass.border,
    padding: 0,
  },
  glowBorder: {
    borderRadius: 32,
    backgroundColor: colors.glass.medium,
  },
  innerGlow: {
    borderRadius: 32,
  },
  content: {
    flexDirection: 'row',
    padding: 0,
  },
  accentBar: {
    width: 7,
    borderTopLeftRadius: 32,
    borderBottomLeftRadius: 32,
    marginRight: 0,
  },
  mainContent: {
    flex: 1,
    padding: 24,
    justifyContent: 'center',
  },
  topRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  roomContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.glass.badge,
    paddingHorizontal: 14,
    paddingVertical: 6,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: colors.glass.border,
  },
  roomText: {
    color: colors.text.secondary,
    fontSize: 14,
    marginLeft: 6,
    fontWeight: '700',
    letterSpacing: 0.12,
  },
  className: {
    color: colors.text.primary,
    fontSize: 24,
    fontWeight: '900',
    marginBottom: 12,
    lineHeight: 30,
    letterSpacing: 0.22,
    textShadowColor: 'rgba(255,255,255,0.22)',
    textShadowOffset: { width: 0, height: 2 },
    textShadowRadius: 6,
  },
  professorRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 20,
  },
  professorText: {
    color: colors.text.muted,
    fontSize: 16,
    marginLeft: 8,
    fontWeight: '600',
    letterSpacing: 0.12,
  },
  bottomRow: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    marginTop: 8,
  },
});

export default ClassCard;
