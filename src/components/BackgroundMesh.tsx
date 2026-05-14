import React from 'react';
import { View, StyleSheet } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { useThemedColors } from '../theme/useTheme';

const BackgroundMesh = () => {
  const { isDark } = useThemedColors();

  // Dynamically adjust ambient light intensities based on theme darkness
  const topOrb = isDark 
    ? ['rgba(100, 180, 255, 0.12)', 'rgba(100, 180, 255, 0.04)', 'transparent'] as const
    : ['rgba(100, 180, 255, 0.22)', 'rgba(100, 180, 255, 0.08)', 'transparent'] as const;

  const bottomOrb = isDark
    ? ['rgba(160, 100, 255, 0.10)', 'rgba(160, 100, 255, 0.03)', 'transparent'] as const
    : ['rgba(160, 100, 255, 0.18)', 'rgba(160, 100, 255, 0.06)', 'transparent'] as const;

  const rightOrb = isDark
    ? ['rgba(255, 255, 255, 0.06)', 'transparent'] as const
    : ['rgba(255, 255, 255, 0.12)', 'transparent'] as const;

  return (
    <View style={[StyleSheet.absoluteFill, { backgroundColor: "transparent" }]} pointerEvents="none">
      
      {/* Unified Mesh Orb 1 - Top Atmospheric Light */}
      <LinearGradient
        colors={topOrb}
        start={{ x: 1, y: 0 }} // Anchored to top right corner
        end={{ x: 0, y: 0.6 }}
        style={StyleSheet.absoluteFill}
      />

      {/* Unified Mesh Orb 2 - Bottom-Left Subtle Warmth */}
      <LinearGradient
        colors={bottomOrb}
        start={{ x: 0, y: 1 }} // Anchored to bottom left corner
        end={{ x: 0.8, y: 0.2 }}
        style={StyleSheet.absoluteFill}
      />

      {/* Unified Mesh Orb 3 - Right Depth Light */}
      <LinearGradient
        colors={rightOrb}
        start={{ x: 1, y: 0.8 }} // Anchored bottom right
        end={{ x: 0, y: 0.5 }}
        style={StyleSheet.absoluteFill}
      />
      
    </View>
  );
};

export default BackgroundMesh;
