import React, { useRef, useEffect, useMemo } from "react";
import { StyleSheet, View, Animated, TouchableOpacity, Dimensions } from "react-native";

const originalCreate = StyleSheet.create;
const injectTypography = (styles: any) => {
  if (!styles || typeof styles !== "object") return styles;
  const processed: any = {};
  
  for (const key in styles) {
    const style = styles[key];
    if (style && typeof style === "object" && !Array.isArray(style)) {
      const cloned = { ...style };
      
      const hasTextProps = 
        "fontSize" in cloned || 
        "fontWeight" in cloned || 
        "color" in cloned || 
        "lineHeight" in cloned || 
        "letterSpacing" in cloned ||
        "textAlign" in cloned;

      if (hasTextProps) {
        const weight = String(cloned.fontWeight || "400");
        
        // Ultimate Cohesion: Map all text node weights to Plus Jakarta Sans family
        if (weight === "700" || weight === "800" || weight === "900" || weight === "bold") {
          cloned.fontFamily = "PlusJakarta-Bold";
        } else if (weight === "600") {
          cloned.fontFamily = "PlusJakarta-SemiBold";
        } else if (weight === "500") {
          cloned.fontFamily = "PlusJakarta-Medium";
        } else {
          cloned.fontFamily = "PlusJakarta-Regular";
        }
        
        // Sanitize synthetic weighting to avoid blurry/double-bold rendering
        delete cloned.fontWeight;
      }
      processed[key] = cloned;
    } else {
      processed[key] = style;
    }
  }
  return processed;
};

// Overwrite the react-native StyleSheet static constructor factory
StyleSheet.create = (styles: any) => originalCreate(injectTypography(styles) as any);
import { useFonts } from "expo-font";
import { 
  PlusJakartaSans_400Regular, 
  PlusJakartaSans_500Medium, 
  PlusJakartaSans_600SemiBold, 
  PlusJakartaSans_700Bold 
} from "@expo-google-fonts/plus-jakarta-sans";
import { NavigationContainer, DefaultTheme, DarkTheme, Theme } from "@react-navigation/native";
import { createBottomTabNavigator } from "@react-navigation/bottom-tabs";
import { createNativeStackNavigator } from "@react-navigation/native-stack";
import { SafeAreaProvider } from "react-native-safe-area-context";
import { BlurView } from "expo-blur";
import { LinearGradient } from "expo-linear-gradient";
import { Ionicons } from "@expo/vector-icons";
import {
  HomeScreen,
  WeekViewScreen,
  MyClassesScreen,
  CompletedScreen,
  SettingsScreen,
  CourseDetailsScreen,
  NotificationSettingsScreen,
} from "./src/screens";
import { ThemePalette, useThemedColors } from "./src/theme/useTheme";
import { RootStackParamList } from "./src/types/navigation";

const Tab = createBottomTabNavigator();
const Stack = createNativeStackNavigator<RootStackParamList>();
const { width: SCREEN_WIDTH } = Dimensions.get("window");
const TAB_BAR_WIDTH = SCREEN_WIDTH - 40; // 20px margin each side
const TAB_COUNT = 5;
const TAB_WIDTH = TAB_BAR_WIDTH / TAB_COUNT;

type TabStyles = ReturnType<typeof createStyles>;

interface TabIconProps {
  name: keyof typeof Ionicons.glyphMap;
  isFocused: boolean;
  onPress: () => void;
  color: string;
  scale: Animated.Value;
  styles: TabStyles;
}

const AnimatedTabIcon: React.FC<TabIconProps> = ({
  name,
  isFocused,
  onPress,
  color,
  scale,
  styles,
}) => {
  return (
    <TouchableOpacity
      onPress={onPress}
      activeOpacity={0.7}
      style={styles.tabTouchable}
    >
      <Animated.View style={[styles.iconContainer, { transform: [{ scale }] }]}>
        <Ionicons name={name} size={24} color={color} />
      </Animated.View>
    </TouchableOpacity>
  );
};

const GlassTabBar = ({ state, descriptors, navigation }: any) => {
  const { colors, isDark } = useThemedColors();
  const styles = useMemo(() => createStyles(colors, isDark), [colors, isDark]);
  const slideAnim = useRef(new Animated.Value(state.index * TAB_WIDTH)).current;
  const scaleAnims = useRef(
    state.routes.map(
      (_: any, i: number) => new Animated.Value(state.index === i ? 1.15 : 1),
    ),
  ).current;

  useEffect(() => {
    // Animate the sliding indicator
    Animated.spring(slideAnim, {
      toValue: state.index * TAB_WIDTH,
      useNativeDriver: true,
      tension: 68,
      friction: 10,
    }).start();

    // Animate scale for all tabs
    state.routes.forEach((_: any, index: number) => {
      Animated.spring(scaleAnims[index], {
        toValue: state.index === index ? 1.15 : 1,
        useNativeDriver: true,
        tension: 100,
        friction: 8,
      }).start();
    });
  }, [state.index]);

  return (
    <View style={styles.tabBarContainer}>
      <BlurView
        intensity={45}
        tint={isDark ? "dark" : "light"}
        style={styles.tabBarBlur}
      >
        <LinearGradient
          colors={
            isDark
              ? ["rgba(255,255,255,0.12)", "rgba(255,255,255,0.01)"]
              : ["rgba(0,0,0,0.07)", "rgba(0,0,0,0.01)"]
          }
          start={{ x: 0, y: 0 }}
          end={{ x: 0, y: 1 }}
          style={styles.gradientBorder}
        >
          <View style={styles.tabBarInner}>
          
          {/* Animated sliding indicator - vibrant glass pill */}
          <Animated.View
            style={[
              styles.slideIndicator,
              {
                transform: [{ translateX: slideAnim }],
              },
            ]}
          >
            <View style={styles.pillContainer}>
              <View style={[styles.pillGradient, { backgroundColor: isDark ? "rgba(255, 255, 255, 0.15)" : "rgba(0, 0, 0, 0.06)" }]}>
                {/* Inner border/shine */}
                <View style={{
                  position: 'absolute',
                  top: 0, left: 0, right: 0, bottom: 0,
                  borderRadius: 26,
                  borderWidth: 1,
                  borderColor: isDark ? "rgba(255,255,255,0.2)" : "rgba(0,0,0,0.15)"
                }} />
              </View>
            </View>
          </Animated.View>

          {/* Tab icons */}
          {state.routes.map((route: any, index: number) => {
            const isFocused = state.index === index;

            const onPress = () => {
              const event = navigation.emit({
                type: "tabPress",
                target: route.key,
                canPreventDefault: true,
              });

              if (!isFocused && !event.defaultPrevented) {
                navigation.navigate(route.name);
              }
            };

            let iconName: keyof typeof Ionicons.glyphMap = "home";
            switch (route.name) {
              case "Home":
                iconName = isFocused ? "home" : "home-outline";
                break;
              case "Week":
                iconName = isFocused ? "calendar" : "calendar-outline";
                break;
              case "Classes":
                iconName = isFocused ? "book" : "book-outline";
                break;
              case "Completed":
                iconName = isFocused
                  ? "checkmark-circle"
                  : "checkmark-circle-outline";
                break;
              case "Settings":
                iconName = isFocused ? "settings" : "settings-outline";
                break;
            }

            return (
              <View key={route.key} style={styles.tabItem}>
                <AnimatedTabIcon
                  name={iconName}
                  isFocused={isFocused}
                  onPress={onPress}
                  color={isFocused ? (isDark ? "#fff" : "#000") : colors.text.muted}
                  scale={scaleAnims[index]}
                  styles={styles}
                />
              </View>
            );
          })}
            </View>
          </LinearGradient>
        </BlurView>
    </View>
  );
};

// Tab Navigator Component
const TabNavigator = () => {
  return (
    <Tab.Navigator
      tabBar={(props) => <GlassTabBar {...props} />}
      screenOptions={{
        headerShown: false,
      }}
    >
      <Tab.Screen name="Home" component={HomeScreen} />
      <Tab.Screen name="Week" component={WeekViewScreen} />
      <Tab.Screen name="Classes" component={MyClassesScreen} />
      <Tab.Screen name="Completed" component={CompletedScreen} />
      <Tab.Screen name="Settings" component={SettingsScreen} />
    </Tab.Navigator>
  );
};

export default function App() {
  const { colors, isDark } = useThemedColors();

  const [fontsLoaded] = useFonts({
    "PlusJakarta-Regular": PlusJakartaSans_400Regular,
    "PlusJakarta-Medium": PlusJakartaSans_500Medium,
    "PlusJakarta-SemiBold": PlusJakartaSans_600SemiBold,
    "PlusJakarta-Bold": PlusJakartaSans_700Bold,
  });

  const navigationTheme = useMemo<Theme>(
    () => {
      const base = isDark ? DarkTheme : DefaultTheme;
      return {
        ...base,
        dark: isDark,
        colors: {
          ...base.colors,
          primary: colors.primary.teal,
          background: colors.background.start,
          card: colors.background.surface,
          text: colors.text.primary,
          border: colors.border.subtle,
          notification: colors.primary.orange,
        },
      };
    },
    [colors, isDark],
  );

  if (!fontsLoaded) {
    return null;
  }

  return (
    <SafeAreaProvider>
      <View style={{ flex: 1, backgroundColor: colors.background.start }}>
        <NavigationContainer theme={navigationTheme}>
          <Stack.Navigator 
            screenOptions={{ 
              headerShown: false,
              contentStyle: { backgroundColor: colors.background.start }, 
              // Universally maintain state memory across ALL layer stack cycles to cure pops!
              freezeOnBlur: false,
            }}
          >
            <Stack.Screen name="MainTabs" component={TabNavigator} />
            <Stack.Screen
              name="CourseDetails"
              component={CourseDetailsScreen}
              options={{
                presentation: "transparentModal",
                animation: "fade",
                animationDuration: 250,
                gestureEnabled: true,
                gestureDirection: "vertical",
                contentStyle: { backgroundColor: "transparent" },
              }}
            />
            <Stack.Screen
              name="NotificationSettings"
              component={NotificationSettingsScreen}
              options={{
                animation: "slide_from_right",
                presentation: "card",
              }}
            />
          </Stack.Navigator>
        </NavigationContainer>
      </View>
    </SafeAreaProvider>
  );
}

const createStyles = (colors: ThemePalette, isDark: boolean) =>
  StyleSheet.create({
    tabBarContainer: {
      position: "absolute",
      bottom: 24,
      left: 20,
      right: 20,
      borderRadius: 28,
      overflow: "hidden",
      shadowColor: colors.shadow,
      shadowOffset: { width: 0, height: 10 },
      shadowOpacity: 0.35,
      shadowRadius: 16,
      // Removed elevation to fix transparent glass on Android
    },
    tabBarBlur: {
      borderRadius: 28,
      overflow: "hidden",
    },
    gradientBorder: {
      borderRadius: 28,
      padding: 1, // Inner gradient border
    },
    tabBarInner: {
      flex: 1,
      flexDirection: "row",
      justifyContent: "space-around",
      alignItems: "center",
      paddingHorizontal: 0,
      paddingVertical: 12,
      borderRadius: 27,
      backgroundColor: "rgba(255, 255, 255, 0.06)", // Ultra-thin liquid crystal
    },
    slideIndicator: {
      position: "absolute",
      width: TAB_WIDTH,
      height: 52,
      justifyContent: "center",
      alignItems: "center",
      left: 0,
    },
    pillContainer: {
      width: 48,
      height: 48,
      borderRadius: 24,
      overflow: "hidden",
    },
    pillGradient: {
      width: "100%",
      height: "100%",
      borderRadius: 26,
      alignItems: "center",
      justifyContent: "center",
    },
    pillShine: {
      position: "absolute",
      top: 0,
      left: 0,
      right: 0,
      bottom: 0,
      borderRadius: 26,
    },
    tabItem: {
      flex: 1,
      alignItems: "center",
      justifyContent: "center",
      height: 52,
    },
    tabTouchable: {
      alignItems: "center",
      justifyContent: "center",
      width: 52,
      height: 52,
    },
    iconContainer: {
      alignItems: "center",
      justifyContent: "center",
    },
  });
