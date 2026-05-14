import React, { useState, useEffect, useMemo } from "react";
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  StyleSheet,
  Linking,
  Alert,
  TextInput,
  Modal,
  Switch,
} from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import { BlurView } from "expo-blur";
import * as FileSystem from "expo-file-system/legacy";
import * as Sharing from "expo-sharing";
import * as DocumentPicker from "expo-document-picker";
import Slider from "@react-native-community/slider";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { useNavigation } from "@react-navigation/native";
import { NativeStackNavigationProp } from "@react-navigation/native-stack";
import { RootStackParamList } from "../types/navigation";
import { Ionicons } from "@expo/vector-icons";
import {
  GlassHeader,
  NotificationToggle,
  SemesterSelector,
  DatePickerModal,
  BackgroundMesh,
} from "../components";
import { useTimetableStore } from "../store/timetableStore";
import { useShallow } from "zustand/react/shallow";
import { ThemePalette, useThemedColors } from "../theme/useTheme";
import { ClassSchedule, Semester } from "../types";
import { generateICSForSemester } from "../utils/calendarExport";
import * as db from "../database";
import { format, addMonths } from "date-fns";

const SettingsScreen: React.FC = () => {
  const insets = useSafeAreaInsets();
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const { colors, isDark } = useThemedColors();
  const styles = useMemo(() => createStyles(colors), [colors]);
  const {
    settings,
    semesters,
    toggleNotifications,
    toggleDarkMode,
    setTravelBufferMinutes,
    setTaskReminderMinutes,
    setCurrentSemester,
    clearAllData,
    exportData,
    importData,
    addSemester,
  } = useTimetableStore(
    useShallow((state) => ({
      settings: state.settings,
      semesters: state.semesters,
      toggleNotifications: state.toggleNotifications,
      toggleDarkMode: state.toggleDarkMode,
      setTravelBufferMinutes: state.setTravelBufferMinutes,
      setTaskReminderMinutes: state.setTaskReminderMinutes,
      setCurrentSemester: state.setCurrentSemester,
      clearAllData: state.clearAllData,
      exportData: state.exportData,
      importData: state.importData,
      addSemester: state.addSemester,
    }))
  );

  const [showAddSemesterModal, setShowAddSemesterModal] = useState(false);
  const [newSemesterName, setNewSemesterName] = useState("");
  const [newSemesterStartDate, setNewSemesterStartDate] = useState(
    format(new Date(), "yyyy-MM-dd"),
  );
  const [newSemesterEndDate, setNewSemesterEndDate] = useState(
    format(addMonths(new Date(), 4), "yyyy-MM-dd"),
  );
  const [showDatePicker, setShowDatePicker] = useState(false);
  const [datePickerType, setDatePickerType] = useState<"start" | "end">(
    "start",
  );
  const [isExportingCalendar, setIsExportingCalendar] = useState(false);
  const [travelBuffer, setTravelBuffer] = useState(
    settings.travelBufferMinutes || 15,
  );
  const [taskReminderLead, setTaskReminderLead] = useState(
    settings.taskReminderMinutes || 60,
  );

  // Sync local state when settings load
  useEffect(() => {
    if (settings.travelBufferMinutes !== undefined) {
      setTravelBuffer(settings.travelBufferMinutes);
    }
    if (settings.taskReminderMinutes !== undefined) {
      setTaskReminderLead(settings.taskReminderMinutes);
    }
  }, [
    settings.travelBufferMinutes,
    settings.taskReminderMinutes,
  ]);

  const currentSemester = semesters.find((s) => s.isActive) || semesters[0];

  const handleSemesterSelect = async (semester: Semester) => {
    await setCurrentSemester(semester.id);
    Alert.alert("Semester Changed", `Switched to ${semester.name}`);
  };

  const handleAddSemester = async () => {
    if (!newSemesterName.trim()) {
      Alert.alert("Error", "Please enter a semester name");
      return;
    }
    try {
      await addSemester({
        id: `sem_${Date.now()}`,
        name: newSemesterName.trim(),
        startDate: newSemesterStartDate,
        endDate: newSemesterEndDate,
      });
      setShowAddSemesterModal(false);
      setNewSemesterName("");
      Alert.alert("Success", "Semester added successfully");
    } catch (error) {
      Alert.alert("Error", "Failed to add semester");
    }
  };

  const handleExportData = async () => {
    try {
      const data = await exportData();
      const fileUri = `${FileSystem.cacheDirectory}UniTimetable-Backup.json`;
      await FileSystem.writeAsStringAsync(fileUri, JSON.stringify(data, null, 2));
      
      const canShare = await Sharing.isAvailableAsync();
      if (canShare) {
        await Sharing.shareAsync(fileUri, {
          mimeType: "application/json",
          dialogTitle: "Backup your Timetable",
          UTI: "public.json",
        });
      } else {
        Alert.alert("Error", "Sharing is not available on this device");
      }
    } catch (error) {
      console.error(error);
      Alert.alert("Error", "Failed to export data");
    }
  };

  const handleImportData = async () => {
    Alert.alert(
      "Import Data Backup",
      "WARNING: This will permanently OVERWRITE and replace all your current classes, tasks, and settings. Do you want to continue?",
      [
        { text: "Cancel", style: "cancel" },
        { 
          text: "Restore Backup", 
          style: "destructive",
          onPress: async () => {
            try {
              const result = await DocumentPicker.getDocumentAsync({
                type: "application/json",
                copyToCacheDirectory: true,
              });

              if (!result.canceled && result.assets && result.assets.length > 0) {
                const pickedFile = result.assets[0];
                const rawContent = await FileSystem.readAsStringAsync(pickedFile.uri);
                const parsedData = JSON.parse(rawContent);
                
                // Basic sanity check
                if (!parsedData.courses && !parsedData.semesters) {
                  throw new Error("Missing key data attributes");
                }

                await importData(parsedData);

                Alert.alert("Success", "Database successfully restored from backup!");
              }
            } catch (error) {
              console.error("Import error:", error);
              Alert.alert("Import Failed", "Selected file is invalid or corrupt. Please try a valid JSON backup.");
            }
          }
        }
      ]
    );
  };

  const handleExportCalendar = async () => {
    try {
      setIsExportingCalendar(true);
      const activeSemester = await db.getActiveSemester();
      if (!activeSemester) {
        Alert.alert("No semester", "Set an active semester first.");
        return;
      }

      const classes = (await db.getAllClasses(
        activeSemester.id,
      )) as ClassSchedule[];

      if (!classes.length) {
        Alert.alert("Nothing to export", "No classes found for this semester.");
        return;
      }

      const ics = generateICSForSemester(
        classes,
        activeSemester.startDate,
        activeSemester.endDate,
      );

      const safeName = activeSemester.name.replace(/\s+/g, "-");
      const fileUri = `${FileSystem.cacheDirectory}unittimetable-${safeName}.ics`;
      await FileSystem.writeAsStringAsync(fileUri, ics, {
        encoding: "utf8",
      });

      const sharingAvailable = await Sharing.isAvailableAsync();
      if (sharingAvailable) {
        await Sharing.shareAsync(fileUri, {
          mimeType: "text/calendar",
          dialogTitle: "Export calendar",
        });
      } else {
        Alert.alert("Calendar exported", `Saved to: ${fileUri}`);
      }
    } catch (error) {
      console.error("Failed to export calendar:", error);
      Alert.alert("Error", "Failed to export calendar. Please try again.");
    } finally {
      setIsExportingCalendar(false);
    }
  };

  const handleClearData = () => {
    Alert.alert(
      "Clear All Data",
      "Are you sure you want to delete all data? This cannot be undone.",
      [
        { text: "Cancel", style: "cancel" },
        {
          text: "Delete Everything",
          style: "destructive",
          onPress: async () => {
            try {
              await clearAllData();
              Alert.alert("Success", "All data has been cleared");
            } catch (error) {
              Alert.alert("Error", "Failed to clear data");
            }
          },
        },
      ],
    );
  };

  const renderSettingItem = (
    icon: string,
    label: string,
    value?: string,
    onPress?: () => void,
    destructive?: boolean,
  ) => (
    <TouchableOpacity
      style={styles.settingItem}
      onPress={onPress}
      activeOpacity={onPress ? 0.7 : 1}
    >
      <View style={[styles.settingIcon, destructive && styles.destructiveIcon]}>
        <Ionicons
          name={icon as any}
          size={22}
          color={destructive ? "#ef4444" : colors.primary.teal}
        />
      </View>
      <View style={styles.settingContent}>
        <Text
          style={[styles.settingLabel, destructive && styles.destructiveLabel]}
        >
          {label}
        </Text>
        {value && <Text style={styles.settingValue}>{value}</Text>}
      </View>
      {onPress && (
        <Ionicons name="chevron-forward" size={20} color={colors.text.muted} />
      )}
    </TouchableOpacity>
  );

  const renderSettingToggle = (
    icon: string,
    label: string,
    value: boolean,
    onValueChange: (val: boolean) => void,
    activeColor?: string,
  ) => (
    <View style={styles.settingItem}>
      <View style={styles.settingIcon}>
        <Ionicons
          name={icon as any}
          size={22}
          color={colors.primary.teal}
        />
      </View>
      <View style={styles.settingContent}>
        <Text style={styles.settingLabel}>{label}</Text>
      </View>
      <Switch
        value={value}
        onValueChange={onValueChange}
        trackColor={{
          false: "rgba(255, 255, 255, 0.15)",
          true: (activeColor || colors.primary.teal) + '80',
        }}
        thumbColor={value ? (activeColor || colors.primary.teal) : "#b0b0b0"}
      />
    </View>
  );

  const renderSectionHeader = (title: string) => (
    <Text style={styles.sectionHeader}>{title}</Text>
  );

  return (
    <LinearGradient
      colors={[colors.background.start, colors.background.end]}
      style={styles.container}
    >
      <BackgroundMesh />
      <GlassHeader
        title="Settings"
        subtitle="Customize your experience"
        rightComponent={
          <TouchableOpacity
            onPress={toggleDarkMode}
            style={{
              width: 40,
              height: 40,
              borderRadius: 20,
              backgroundColor: "rgba(255, 255, 255, 0.1)",
              alignItems: "center",
              justifyContent: "center",
              borderWidth: 1,
              borderColor: "rgba(255, 255, 255, 0.15)",
            }}
          >
            <Ionicons
              name={settings.darkMode ? "moon" : "sunny"}
              size={20}
              color={settings.darkMode ? colors.primary.teal : "#FFD700"}
            />
          </TouchableOpacity>
        }
      />

      <ScrollView
        style={styles.scrollView}
        contentContainerStyle={[
          styles.scrollContent,
          { paddingBottom: insets.bottom + 160 },
        ]}
        showsVerticalScrollIndicator={false}
      >


        {/* Notifications Section */}
        {renderSectionHeader("Notifications")}
        <View style={styles.section}>
          <BlurView intensity={12} tint="light" style={styles.blurContainer}>
            <View style={styles.gradient}>
              {/* Dynamic adaptive highlights for seamless spatial edges */}
              <LinearGradient colors={[colors.glass.stroke, "transparent"]} start={{x:0,y:0}} end={{x:1,y:0}} style={{position:"absolute",top:0,left:0,right:0,height:1}} />
              <LinearGradient colors={[colors.glass.stroke, "transparent"]} start={{x:0,y:0}} end={{x:0,y:1}} style={{position:"absolute",top:0,left:0,bottom:0,width:1}} />
              
              {renderSettingItem(
                "notifications-outline",
                "Manage Notifications",
                "Configure alerts and timers",
                () => navigation.navigate("NotificationSettings" as any),
              )}
            </View>
          </BlurView>
        </View>

        {/* Academic Section */}
        {renderSectionHeader("Academic")}
        <View style={styles.section}>
          <BlurView intensity={12} tint="light" style={styles.blurContainer}>
            <View style={styles.gradient}>
              {/* Dynamic adaptive highlights for seamless spatial edges */}
              <LinearGradient colors={[colors.glass.stroke, "transparent"]} start={{x:0,y:0}} end={{x:1,y:0}} style={{position:"absolute",top:0,left:0,right:0,height:1}} />
              <LinearGradient colors={[colors.glass.stroke, "transparent"]} start={{x:0,y:0}} end={{x:0,y:1}} style={{position:"absolute",top:0,left:0,bottom:0,width:1}} />
              
              {semesters.length > 0 ? (
                <TouchableOpacity style={styles.semesterRow}>
                  <View style={styles.settingIcon}>
                    <Ionicons
                      name="calendar-outline"
                      size={22}
                      color={colors.primary.teal}
                    />
                  </View>
                  <View style={styles.settingContent}>
                    <Text style={styles.settingLabel}>Current Semester</Text>
                    <Text style={styles.settingValue}>
                      {currentSemester?.name || "Select semester"}
                    </Text>
                  </View>
                  <SemesterSelector
                    semesters={semesters}
                    currentSemester={currentSemester}
                    onSelect={handleSemesterSelect}
                  />
                </TouchableOpacity>
              ) : (
                <View style={styles.noSemesterContainer}>
                  <Ionicons
                    name="alert-circle-outline"
                    size={24}
                    color={colors.text.muted}
                  />
                  <Text style={styles.noSemesterText}>No semesters yet</Text>
                </View>
              )}
              <View style={styles.divider} />
              <TouchableOpacity
                style={styles.semesterRow}
                onPress={() => setShowAddSemesterModal(true)}
              >
                <View
                  style={[
                    styles.settingIcon,
                    { backgroundColor: colors.status.success + '1A' },
                  ]}
                >
                  <Ionicons
                    name="add-circle-outline"
                    size={22}
                    color={colors.primary.green}
                  />
                </View>
                <View style={styles.settingContent}>
                  <Text style={styles.settingLabel}>Add Semester</Text>
                  <Text style={styles.settingValue}>Create a new semester</Text>
                </View>
                <Ionicons
                  name="chevron-forward"
                  size={20}
                  color={colors.text.muted}
                />
              </TouchableOpacity>
            </View>
          </BlurView>
        </View>


        {/* Data Section */}
        {renderSectionHeader("Data Management")}
        <View style={styles.section}>
          <BlurView intensity={12} tint="light" style={styles.blurContainer}>
            <View style={styles.gradient}>
              {/* Dynamic adaptive highlights for seamless spatial edges */}
              <LinearGradient colors={[colors.glass.stroke, "transparent"]} start={{x:0,y:0}} end={{x:1,y:0}} style={{position:"absolute",top:0,left:0,right:0,height:1}} />
              <LinearGradient colors={[colors.glass.stroke, "transparent"]} start={{x:0,y:0}} end={{x:0,y:1}} style={{position:"absolute",top:0,left:0,bottom:0,width:1}} />
               {renderSettingItem(
                "cloud-download-outline",
                "Import Data",
                "Restore from backup",
                handleImportData,
              )}
              <View style={styles.divider} />
              {renderSettingItem(
                "cloud-upload-outline",
                "Export Data",
                "Backup your timetable",
                handleExportData,
              )}
              <View style={styles.divider} />
              {renderSettingItem(
                "calendar-outline",
                "Export Calendar (.ics)",
                isExportingCalendar ? "Preparing..." : "Sync to calendar apps",
                isExportingCalendar ? undefined : handleExportCalendar,
              )}
              <View style={styles.divider} />
              {renderSettingItem(
                "trash-outline",
                "Clear All Data",
                "Delete everything",
                handleClearData,
                true,
              )}
            </View>
          </BlurView>
        </View>

        {/* About Section */}
        {renderSectionHeader("About")}
        <View style={styles.section}>
          <BlurView intensity={12} tint="light" style={styles.blurContainer}>
            <View style={styles.gradient}>
              {/* Highlight effects */}
              <LinearGradient colors={["rgba(255,255,255,0.4)", "transparent"]} start={{x:0,y:0}} end={{x:1,y:0}} style={{position:"absolute",top:0,left:0,right:0,height:1}} />
              <LinearGradient colors={["rgba(255,255,255,0.4)", "transparent"]} start={{x:0,y:0}} end={{x:0,y:1}} style={{position:"absolute",top:0,left:0,bottom:0,width:1}} />
              {renderSettingItem(
                "information-circle-outline",
                "Version",
                "1.0.0",
              )}
              <View style={styles.divider} />
              {renderSettingItem(
                "document-text-outline",
                "Open Source Licenses",
                undefined,
                () =>
                  Alert.alert(
                    "Licenses",
                    "React Native, Expo, Zustand, and more!",
                  ),
              )}
              <View style={styles.divider} />
              {renderSettingItem(
                "shield-checkmark-outline",
                "Privacy Policy",
                undefined,
                () => Linking.openURL("https://example.com/privacy"),
              )}
              <View style={styles.divider} />
              {renderSettingItem(
                "help-circle-outline",
                "Help & Support",
                undefined,
                () =>
                  Alert.alert("Support", "Contact: support@unitimetable.app"),
              )}
            </View>
          </BlurView>
        </View>


      </ScrollView>

      {/* Add Semester Modal */}
      <Modal
        visible={showAddSemesterModal}
        transparent
        animationType="fade"
        onRequestClose={() => setShowAddSemesterModal(false)}
      >
        <View style={styles.modalOverlay}>
          <BlurView intensity={20} tint="light" style={styles.modalBlur}>
            <LinearGradient
              colors={[
                "rgba(255, 255, 255, 0.15)",
                "rgba(255, 255, 255, 0.05)",
              ]}
              style={styles.modalContent}
            >
              <Text style={styles.modalTitle}>Add Semester</Text>

              <View style={styles.modalInputContainer}>
                <Text style={styles.modalInputLabel}>Semester Name</Text>
                <TextInput
                  style={styles.modalInput}
                  placeholder="e.g. Semester 1"
                  placeholderTextColor={colors.text.muted}
                  value={newSemesterName}
                  onChangeText={setNewSemesterName}
                />
              </View>

              <View style={styles.modalInputContainer}>
                <Text style={styles.modalInputLabel}>Start Date</Text>
                <TouchableOpacity
                  style={styles.modalDateInput}
                  onPress={() => {
                    setDatePickerType("start");
                    setShowDatePicker(true);
                  }}
                >
                  <Text style={styles.modalDateInputText}>
                    {newSemesterStartDate}
                  </Text>
                  <Ionicons
                    name="calendar-outline"
                    size={20}
                    color={colors.text.muted}
                  />
                </TouchableOpacity>
              </View>

              <View style={styles.modalInputContainer}>
                <Text style={styles.modalInputLabel}>End Date</Text>
                <TouchableOpacity
                  style={styles.modalDateInput}
                  onPress={() => {
                    setDatePickerType("end");
                    setShowDatePicker(true);
                  }}
                >
                  <Text style={styles.modalDateInputText}>
                    {newSemesterEndDate}
                  </Text>
                  <Ionicons
                    name="calendar-outline"
                    size={20}
                    color={colors.text.muted}
                  />
                </TouchableOpacity>
              </View>

              <View style={styles.modalButtons}>
                <TouchableOpacity
                  style={styles.modalCancelBtn}
                  onPress={() => {
                    setShowAddSemesterModal(false);
                    setNewSemesterName("");
                  }}
                >
                  <Text style={styles.modalCancelText}>Cancel</Text>
                </TouchableOpacity>

                <TouchableOpacity onPress={handleAddSemester}>
                  <LinearGradient
                    colors={[colors.primary.teal, colors.primary.blue]}
                    style={styles.modalConfirmBtn}
                  >
                    <Text style={styles.modalConfirmText}>Add Semester</Text>
                  </LinearGradient>
                </TouchableOpacity>
              </View>
            </LinearGradient>
          </BlurView>
        </View>
      </Modal>

      {/* Date Picker Modal */}
      <DatePickerModal
        visible={showDatePicker}
        title={
          datePickerType === "start" ? "Select Start Date" : "Select End Date"
        }
        initialDate={
          datePickerType === "start" ? newSemesterStartDate : newSemesterEndDate
        }
        onConfirm={(date) => {
          if (datePickerType === "start") {
            setNewSemesterStartDate(date);
          } else {
            setNewSemesterEndDate(date);
          }
          setShowDatePicker(false);
        }}
        onCancel={() => setShowDatePicker(false)}
      />
    </LinearGradient>
  );
};

const createStyles = (colors: ThemePalette) =>
  StyleSheet.create({
  container: {
    flex: 1,
  },
  scrollView: {
    flex: 1,
  },
  scrollContent: {
    paddingTop: 20,
    paddingHorizontal: 16,
  },
  statsRow: {
    flexDirection: "row",
    gap: 12,
    marginBottom: 8,
  },
  statCard: {
    flex: 1,
    borderRadius: 24,
    overflow: "hidden",
  },
  statBlur: {
    borderRadius: 24,
  },
  statGradient: {
    paddingVertical: 20,
    paddingHorizontal: 10,
    alignItems: "center",
    borderRadius: 24,
    borderWidth: 1,
    borderColor: colors.glass.cardBorder,
    backgroundColor: colors.glass.cardFill,
  },
  statIconWrapper: {
    width: 48,
    height: 48,
    borderRadius: 24,
    alignItems: "center",
    justifyContent: "center",
    marginBottom: 4,
  },
  statNumber: {
    color: colors.text.primary,
    fontSize: 20,
    fontWeight: "700",
    marginTop: 8,
  },
  statLabel: {
    color: colors.text.muted,
    fontSize: 11,
    marginTop: 4,
  },
  sectionHeader: {
    color: colors.text.muted,
    fontSize: 11,
    fontWeight: "800",
    textTransform: "uppercase",
    letterSpacing: 1.2,
    marginBottom: 6,
    marginTop: 20,
    marginLeft: 10,
  },
  section: {
    borderRadius: 20,
    overflow: "hidden",
    marginBottom: 14,
  },
  blurContainer: {
    borderRadius: 20,
  },
  groupedSliderCard: {
    padding: 14,
  },
  unifiedRow: {
    flexDirection: "row",
    alignItems: "center",
    padding: 14,
  },
  sliderCard: {
    marginTop: 12,
    paddingHorizontal: 12,
  },
  sliderHeader: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: 6,
  },
  sliderLabel: {
    color: colors.text.primary,
    fontSize: 14,
    fontWeight: "600",
  },
  sliderSubLabel: {
    color: colors.text.muted,
    fontSize: 11.5,
    marginTop: 1,
  },
  sliderValue: {
    color: colors.text.secondary,
    fontSize: 13,
    fontWeight: "700",
  },
  gradient: {
    borderRadius: 20,
    borderWidth: 1,
    borderColor: colors.glass.cardBorder,
    backgroundColor: colors.glass.cardFill,
  },
  profileCard: {
    flexDirection: "row",
    alignItems: "center",
    padding: 14,
  },
  avatar: {
    width: 60,
    height: 60,
    borderRadius: 30,
    alignItems: "center",
    justifyContent: "center",
    borderWidth: 2,
    borderColor: "rgba(255,255,255,0.2)",
  },
  avatarText: {
    color: "#fff",
    fontSize: 22,
    fontWeight: "700",
    letterSpacing: -0.5,
  },
  floatingEditBtn: {
    position: "absolute",
    bottom: -1,
    right: -1,
    width: 24,
    height: 24,
    borderRadius: 12,
    backgroundColor: "rgba(255,255,255,0.2)",
    alignItems: "center",
    justifyContent: "center",
    borderWidth: 1,
    borderColor: "rgba(255,255,255,0.3)",
  },
  profileInfo: {
    flex: 1,
    marginLeft: 12,
  },
  profileName: {
    color: colors.text.primary,
    fontSize: 17,
    fontWeight: "700",
  },
  profileEmail: {
    color: colors.text.muted,
    fontSize: 12.5,
    marginTop: 1,
  },
  editButton: {
    padding: 8,
    backgroundColor: colors.glass.badge,
    borderRadius: 10,
  },
  editFields: {
    paddingHorizontal: 14,
    paddingBottom: 14,
    gap: 10,
  },
  editInput: {
    backgroundColor: colors.glass.badge,
    borderRadius: 10,
    padding: 12,
    color: colors.text.primary,
    fontSize: 14,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: colors.glass.cardBorder,
  },
  settingItem: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 14,
    paddingVertical: 12, // Tighter vertical squeeze!
  },
  semesterRow: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 14,
    paddingVertical: 12, // Tighter vertical squeeze!
  },
  settingIcon: {
    width: 36, // Squeezed slightly
    height: 36, // Squeezed slightly
    borderRadius: 10, // Apple-style squircle corners
    backgroundColor: `${colors.primary.teal}15`,
    alignItems: "center",
    justifyContent: "center",
    marginRight: 12,
  },
  destructiveIcon: {
    backgroundColor: colors.status.danger + '1A',
  },
  settingContent: {
    flex: 1,
  },
  settingLabel: {
    color: colors.text.primary,
    fontSize: 14.5, // Muted from 16
    fontWeight: "600", // Bolder hierarchy
  },
  destructiveLabel: {
    color: colors.status.danger,
  },
  settingValue: {
    color: colors.text.muted,
    fontSize: 12,
    marginTop: 1.5,
  },
  divider: {
    height: StyleSheet.hairlineWidth,
    backgroundColor: colors.glass.cardBorder,
    marginLeft: 62, // Perfectly matches custom icon padding
    marginRight: 14,
  },
  noSemesterContainer: {
    flexDirection: "row",
    alignItems: "center",
    padding: 16,
    gap: 12,
  },
  noSemesterText: {
    color: colors.text.muted,
    fontSize: 15,
  },
  footer: {
    alignItems: "center",
    paddingVertical: 40,
  },
  footerText: {
    color: colors.text.secondary,
    fontSize: 16,
    fontWeight: "600",
  },
  footerSubtext: {
    color: colors.text.muted,
    fontSize: 13,
    marginTop: 4,
  },
  // Modal Styles
  modalOverlay: {
    flex: 1,
    backgroundColor: "rgba(0, 0, 0, 0.7)",
    justifyContent: "center",
    alignItems: "center",
    padding: 20,
  },
  modalBlur: {
    borderRadius: 24,
    overflow: "hidden",
    width: "100%",
    maxWidth: 340,
  },
  modalContent: {
    padding: 24,
    borderRadius: 24,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: colors.glass.elevatedBorder,
    backgroundColor: colors.glass.elevatedFill,
  },
  modalTitle: {
    color: colors.text.primary,
    fontSize: 22,
    fontWeight: "700",
    textAlign: "center",
    marginBottom: 24,
  },
  modalInputContainer: {
    marginBottom: 16,
  },
  modalInputLabel: {
    color: colors.text.secondary,
    fontSize: 14,
    marginBottom: 8,
  },
  modalInput: {
    backgroundColor: colors.glass.badge,
    borderRadius: 12,
    padding: 14,
    color: colors.text.primary,
    fontSize: 16,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: colors.glass.cardBorder,
  },
  modalDateInput: {
    backgroundColor: colors.glass.badge,
    borderRadius: 12,
    padding: 14,
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: colors.glass.cardBorder,
  },
  modalDateInputText: {
    color: colors.text.primary,
    fontSize: 16,
  },
  modalButtons: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginTop: 8,
  },
  modalCancelBtn: {
    paddingVertical: 12,
    paddingHorizontal: 20,
  },
  modalCancelText: {
    color: colors.text.muted,
    fontSize: 16,
    fontWeight: "500",
  },
  modalConfirmBtn: {
    paddingVertical: 12,
    paddingHorizontal: 24,
    borderRadius: 20,
  },
  modalConfirmText: {
    color: "#fff",
    fontSize: 16,
    fontWeight: "600",
  },
  });

export default SettingsScreen;
