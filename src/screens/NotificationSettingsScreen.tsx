import React, { useState, useMemo } from "react";
import {
  View,
  Text,
  StyleSheet,
  Switch,
  TouchableOpacity,
  ScrollView,
  ActionSheetIOS,
  Platform,
  Alert,
} from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import { BlurView } from "expo-blur";
import { Ionicons } from "@expo/vector-icons";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { useNavigation } from "@react-navigation/native";
import {
  GlassHeader,
  BackgroundMesh,
  TimePickerModal,
} from "../components";
import { useTimetableStore } from "../store/timetableStore";
import { useThemedColors, ThemePalette } from "../theme/useTheme";
import { useShallow } from "zustand/react/shallow";

const BUFFER_OPTIONS = [
  { label: "5 mins before", value: 5 },
  { label: "10 mins before", value: 10 },
  { label: "15 mins before", value: 15 },
  { label: "30 mins before", value: 30 },
  { label: "60 mins before", value: 60 },
];

const NotificationSettingsScreen: React.FC = () => {
  const insets = useSafeAreaInsets();
  const navigation = useNavigation();
  const { colors, isDark } = useThemedColors();
  const styles = useMemo(() => createStyles(colors), [colors]);

  const {
    settings,
    toggleNotifications,
    setTodayScheduleEnabled,
    setTodayScheduleTime,
    setTravelBufferMinutes,
  } = useTimetableStore(
    useShallow((state) => ({
      settings: state.settings,
      toggleNotifications: state.toggleNotifications,
      setTodayScheduleEnabled: state.setTodayScheduleEnabled,
      setTodayScheduleTime: state.setTodayScheduleTime,
      setTravelBufferMinutes: state.setTravelBufferMinutes,
    }))
  );

  const [timePickerVisible, setTimePickerVisible] = useState(false);

  const handleTimeConfirm = async (time: string) => {
    await setTodayScheduleTime(time);
    setTimePickerVisible(false);
  };

  const showBufferSelector = () => {
    const labels = BUFFER_OPTIONS.map(o => o.label);
    
    if (Platform.OS === 'ios') {
      ActionSheetIOS.showActionSheetWithOptions(
        {
          options: [...labels, "Cancel"],
          cancelButtonIndex: labels.length,
          title: "Choose Buffer Time",
        },
        (buttonIndex) => {
          if (buttonIndex < labels.length) {
            setTravelBufferMinutes(BUFFER_OPTIONS[buttonIndex].value);
          }
        }
      );
    } else {
      Alert.alert(
        "Upcoming Class",
        "Minutes before class starts:",
        BUFFER_OPTIONS.map(o => ({
          text: o.label,
          onPress: () => setTravelBufferMinutes(o.value)
        })),
        { cancelable: true }
      );
    }
  };

  const currentBuffer = settings.travelBufferMinutes || 15;
  const currentBufferLabel = BUFFER_OPTIONS.find(o => o.value === currentBuffer)?.label || `${currentBuffer} mins before`;

  return (
    <LinearGradient
      colors={[colors.background.start, colors.background.end]}
      style={styles.container}
    >
      <BackgroundMesh />
      
      <GlassHeader 
        title="Notifications" 
        subtitle="Alert preferences"
        onBack={() => navigation.goBack()} 
      />

      <ScrollView
        style={styles.scroll}
        contentContainerStyle={[styles.content, { paddingBottom: insets.bottom + 40 }]}
        showsVerticalScrollIndicator={false}
      >
        {/* Card 1: Today Schedule */}
        <View style={styles.cardWrapper}>
          <BlurView intensity={12} tint="light" style={styles.blur}>
            <View style={styles.gradientContainer}>
              {/* High-End Rim Highlights */}
              <LinearGradient colors={["rgba(255,255,255,0.5)", "transparent"]} start={{x:0,y:0}} end={{x:1,y:0}} style={styles.rimTop} />
              <LinearGradient colors={["rgba(255,255,255,0.4)", "transparent"]} start={{x:0,y:0}} end={{x:0,y:1}} style={styles.rimLeft} />
              
              <View style={styles.cardHeader}>
                <View style={styles.labelRow}>
                  <View style={[styles.iconRing, { backgroundColor: colors.primary.teal + '1A' }]}>
                    <Ionicons name="time" size={18} color={colors.primary.teal} />
                  </View>
                  <Text style={styles.labelTitle}>Today schedule</Text>
                </View>
                <Switch
                  value={!!settings.todayScheduleEnabled}
                  onValueChange={setTodayScheduleEnabled}
                  trackColor={{ false: "rgba(255,255,255,0.12)", true: colors.primary.teal }}
                  thumbColor={"#fff"}
                  ios_backgroundColor="rgba(0,0,0,0.3)"
                />
              </View>

              <TouchableOpacity 
                style={styles.selectorBtn}
                onPress={() => setTimePickerVisible(true)}
                activeOpacity={0.7}
              >
                <Text style={styles.selectorText}>
                  {settings.todayScheduleTime || "08:00"}
                </Text>
                <Ionicons name="chevron-down" size={18} color={colors.text.muted} />
              </TouchableOpacity>
            </View>
          </BlurView>
        </View>

        {/* Card 2: Upcoming Class */}
        <View style={styles.cardWrapper}>
          <BlurView intensity={12} tint="light" style={styles.blur}>
            <View style={styles.gradientContainer}>
              {/* High-End Rim Highlights */}
              <LinearGradient colors={["rgba(255,255,255,0.5)", "transparent"]} start={{x:0,y:0}} end={{x:1,y:0}} style={styles.rimTop} />
              <LinearGradient colors={["rgba(255,255,255,0.4)", "transparent"]} start={{x:0,y:0}} end={{x:0,y:1}} style={styles.rimLeft} />
              
              <View style={styles.cardHeader}>
                <View style={styles.labelRow}>
                  <View style={[styles.iconRing, { backgroundColor: colors.primary.blue + '1A' }]}>
                    <Ionicons name="notifications" size={18} color={colors.primary.blue} />
                  </View>
                  <Text style={styles.labelTitle}>Upcoming class</Text>
                </View>
                <Switch
                  value={settings.notifications}
                  onValueChange={toggleNotifications}
                  trackColor={{ false: "rgba(255,255,255,0.12)", true: colors.primary.blue }}
                  thumbColor={"#fff"}
                  ios_backgroundColor="rgba(0,0,0,0.3)"
                />
              </View>

              <TouchableOpacity 
                style={styles.selectorBtn}
                onPress={showBufferSelector}
                activeOpacity={0.7}
              >
                <Text style={styles.selectorText}>
                  {currentBufferLabel}
                </Text>
                <Ionicons name="chevron-down" size={18} color={colors.text.muted} />
              </TouchableOpacity>
            </View>
          </BlurView>
        </View>

        {/* Description Note */}
        <Text style={styles.footerNote}>
          "Today schedule" fires once every morning at the chosen time. "Upcoming class" notifies you before each lesson.
        </Text>
      </ScrollView>

      {/* Shared Premium Time Picker for Daily Alert */}
      <TimePickerModal
        visible={timePickerVisible}
        title="Alert Time"
        initialTime={settings.todayScheduleTime || "08:00"}
        onConfirm={handleTimeConfirm}
        onCancel={() => setTimePickerVisible(false)}
      />
    </LinearGradient>
  );
};

const createStyles = (colors: ThemePalette) =>
  StyleSheet.create({
    container: {
      flex: 1,
    },
    scroll: {
      flex: 1,
    },
    content: {
      padding: 20,
      paddingTop: 24,
    },
    cardWrapper: {
      borderRadius: 24,
      overflow: "hidden",
      marginBottom: 16,
    },
    blur: {
      borderRadius: 24,
    },
    gradientContainer: {
      padding: 24,
      backgroundColor: "rgba(255, 255, 255, 0.02)",
      borderRadius: 28,
      borderWidth: 1.5,
      borderColor: "rgba(255,255,255,0.12)",
    },
    rimTop: {
      position: "absolute",
      top: 0,
      left: 0,
      right: 0,
      height: 1,
      opacity: 0.8,
    },
    rimLeft: {
      position: "absolute",
      top: 0,
      left: 0,
      bottom: 0,
      width: 1,
      opacity: 0.8,
    },
    cardHeader: {
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "space-between",
      marginBottom: 20,
    },
    labelRow: {
      flexDirection: "row",
      alignItems: "center",
    },
    iconRing: {
      width: 36,
      height: 36,
      borderRadius: 18,
      alignItems: "center",
      justifyContent: "center",
      marginRight: 12,
      borderWidth: 1,
      borderColor: "rgba(255,255,255,0.08)",
    },
    labelTitle: {
      color: colors.text.primary,
      fontSize: 18,
      fontWeight: "600",
      letterSpacing: -0.4,
    },
    selectorBtn: {
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "space-between",
      backgroundColor: "rgba(255,255,255,0.07)",
      paddingVertical: 16,
      paddingHorizontal: 20,
      borderRadius: 20,
      borderWidth: 1.5,
      borderColor: "rgba(255,255,255,0.1)",
      marginLeft: 48, 
      shadowColor: "#000",
      shadowOffset: { width: 0, height: 4 },
      shadowOpacity: 0.2,
      shadowRadius: 8,
    },
    selectorText: {
      color: colors.text.primary,
      fontSize: 17,
      fontWeight: "600",
      letterSpacing: -0.2,
    },
    footerNote: {
      color: colors.text.muted,
      fontSize: 13,
      lineHeight: 18,
      textAlign: "center",
      paddingHorizontal: 24,
      marginTop: 12,
      opacity: 0.7,
    }
  });

export default NotificationSettingsScreen;
