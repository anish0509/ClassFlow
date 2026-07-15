import React, { useEffect, useRef } from "react";
import { View, Text, StyleSheet, Animated, TouchableOpacity, Easing, ViewStyle } from "react-native";
import { BlurView } from "expo-blur";
import { LinearGradient } from "expo-linear-gradient";
import { Ionicons } from "@expo/vector-icons";
import { useThemedColors } from "../theme/useTheme";

interface EmptyStateCardProps {
  icon: keyof typeof Ionicons.glyphMap;
  title: string;
  description: string;
  buttonText?: string;
  onPress?: () => void;
  style?: ViewStyle;
}

export const EmptyStateCard: React.FC<EmptyStateCardProps> = ({
  icon,
  title,
  description,
  buttonText,
  onPress,
  style,
}) => {
  const { colors, isDark } = useThemedColors();
  
  const floatAnim = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    Animated.loop(
      Animated.sequence([
        Animated.timing(floatAnim, {
          toValue: 1,
          duration: 3000,
          easing: Easing.inOut(Easing.ease),
          useNativeDriver: true,
        }),
        Animated.timing(floatAnim, {
          toValue: 0,
          duration: 3000,
          easing: Easing.inOut(Easing.ease),
          useNativeDriver: true,
        }),
      ])
    ).start();
  }, [floatAnim]);

  const floatY = floatAnim.interpolate({
    inputRange: [0, 1],
    outputRange: [0, -10],
  });

  return (
    <View style={[styles.container, style]}>
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
          <View style={[styles.cardInner, { backgroundColor: colors.glass.cardFill }]}>
            {isDark && (
              <>
                <LinearGradient
                  colors={["rgba(255,255,255,0)", "rgba(255,255,255,0.4)", "rgba(255,255,255,0)"]}
                  start={{ x: 0, y: 0 }}
                  end={{ x: 1, y: 0 }}
                  style={{ position: 'absolute', top: 0, left: 0, right: 0, height: 1, zIndex: 10 }}
                />
                <LinearGradient
                  colors={["rgba(255,255,255,0.4)", "rgba(255,255,255,0)", "rgba(255,255,255,0.1)"]}
                  start={{ x: 0, y: 0 }}
                  end={{ x: 0, y: 1 }}
                  style={{ position: 'absolute', top: 0, left: 0, bottom: 0, width: 1, zIndex: 10 }}
                />
              </>
            )}

            <View style={styles.content}>
              <Animated.View style={[styles.iconContainer, { transform: [{ translateY: floatY }] }]}>
                <Ionicons name={icon} size={56} color={colors.primary.teal} />
              </Animated.View>
              
              <Text style={[styles.title, { color: colors.text.primary }]}>{title}</Text>
              <Text style={[styles.description, { color: colors.text.secondary }]}>{description}</Text>

              {buttonText && onPress && (
                <TouchableOpacity onPress={onPress} activeOpacity={0.7} style={[styles.button, { backgroundColor: colors.primary.teal }]}>
                  <Text style={styles.buttonText}>{buttonText}</Text>
                </TouchableOpacity>
              )}
            </View>
          </View>
        </LinearGradient>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    marginVertical: 12,
    marginHorizontal: 16,
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
    overflow: "hidden",
  },
  content: {
    alignItems: "center",
    paddingVertical: 36,
    paddingHorizontal: 24,
  },
  iconContainer: {
    marginBottom: 20,
  },
  title: {
    fontSize: 20,
    fontWeight: "700",
    marginBottom: 8,
    textAlign: "center",
    letterSpacing: 0.3,
  },
  description: {
    fontSize: 14,
    textAlign: "center",
    marginBottom: 24,
    lineHeight: 20,
  },
  button: {
    paddingHorizontal: 24,
    paddingVertical: 12,
    borderRadius: 20,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.2,
    shadowRadius: 8,
  },
  buttonText: {
    color: "#FFFFFF",
    fontSize: 15,
    fontWeight: "700",
    letterSpacing: 0.5,
  },
});
