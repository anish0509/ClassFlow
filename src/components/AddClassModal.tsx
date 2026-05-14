import React, { useState, useEffect, useCallback } from "react";
import {
  View,
  Text,
  Modal,
  TouchableOpacity,
  TextInput,
  ScrollView,
  StyleSheet,
  KeyboardAvoidingView,
  Platform,
  Alert,
} from "react-native";
import { BlurView } from "expo-blur";
import { LinearGradient } from "expo-linear-gradient";
import { Ionicons } from "@expo/vector-icons";
import { Course } from "../types";
import { TimePickerModal } from "./WheelPicker";
import * as db from "../database";
import {
  ThemePalette,
  useThemedColors,
  useThemedStyles,
} from "../theme/useTheme";

interface AddClassModalProps {
  visible: boolean;
  onClose: () => void;
  onSubmit: (classData: {
    courseId: string;
    dayOfWeek: string;
    startTime: string;
    endTime: string;
    room: string;
    semesterId: string;
  }) => void;
  courses: Course[];
  semesterId: string;
  preselectedDay?: string;
  preselectedCourseId?: string;
}

const DAYS = ["MON", "TUE", "WED", "THU", "FRI", "SAT"];

const AddClassModal: React.FC<AddClassModalProps> = ({
  visible,
  onClose,
  onSubmit,
  courses,
  semesterId,
  preselectedDay,
  preselectedCourseId,
}) => {
  const { colors, isDark } = useThemedColors();
  const styles = useThemedStyles(createStyles);
  const [selectedCourse, setSelectedCourse] = useState<string>("");
  const [selectedDay, setSelectedDay] = useState<string>("MON");
  const [startTime, setStartTime] = useState("09:00");
  const [endTime, setEndTime] = useState("09:50");
  const [room, setRoom] = useState("");
  const [recentRooms, setRecentRooms] = useState<string[]>([]);
  const [showTimePicker, setShowTimePicker] = useState(false);
  const [timePickerType, setTimePickerType] = useState<"start" | "end">(
    "start",
  );

  // Load recent rooms from database
  const loadRecentRooms = useCallback(async () => {
    try {
      const database = await db.getDatabase();
      const rooms = await database.getAllAsync<{ room: string }>(
        `SELECT DISTINCT room FROM classes WHERE room IS NOT NULL AND room != '' ORDER BY id DESC LIMIT 6`,
      );
      const uniqueRooms = [...new Set(rooms.map((r) => r.room))];
      setRecentRooms(uniqueRooms);
    } catch (error) {
      console.error("Failed to load recent rooms:", error);
    }
  }, []);

  useEffect(() => {
    if (visible) {
      loadRecentRooms();
      if (preselectedDay) setSelectedDay(preselectedDay);
      if (preselectedCourseId) {
        setSelectedCourse(preselectedCourseId);
        // Set default room from preselected course
        const course = courses.find((c) => c.id === preselectedCourseId);
        if (course?.room) {
          setRoom(course.room);
        }
      }
    }
  }, [visible, preselectedDay, preselectedCourseId, courses, loadRecentRooms]);

  // Handle course selection - set default room from course
  const handleCourseSelect = (courseId: string) => {
    setSelectedCourse(courseId);
    const course = courses.find((c) => c.id === courseId);
    if (course?.room && !room) {
      // Only set if room is empty or different from current
      setRoom(course.room);
    }
  };

  const handleSubmit = () => {
    if (!selectedCourse) {
      Alert.alert("Select a course", "Please pick a course for this class.");
      return;
    }
    if (!room.trim()) {
      Alert.alert("Add a room", "Please enter the room or venue.");
      return;
    }

    const toMinutes = (time: string) => {
      const [h, m] = time.split(":").map((v) => parseInt(v, 10) || 0);
      return h * 60 + m;
    };
    if (toMinutes(startTime) >= toMinutes(endTime)) {
      Alert.alert("Time conflict", "End time must be after start time.");
      return;
    }

    onSubmit({
      courseId: selectedCourse,
      dayOfWeek: selectedDay,
      startTime,
      endTime,
      room,
      semesterId,
    });
    resetForm();
    onClose();
  };

  const resetForm = () => {
    setSelectedCourse("");
    setSelectedDay(preselectedDay || "MON");
    setStartTime("09:00");
    setEndTime("09:50");
    setRoom("");
  };

  return (
    <Modal
      visible={visible}
      transparent
      animationType="slide"
      onRequestClose={onClose}
    >
      <KeyboardAvoidingView
        behavior={Platform.OS === "ios" ? "padding" : "height"}
        style={styles.modalOverlay}
      >
        <View style={styles.modalContent}>
          <BlurView intensity={90} tint={isDark ? "dark" : "light"} style={styles.blurContainer}>
            <LinearGradient
              colors={
                isDark
                  ? ["rgba(20, 24, 33, 0.65)", "rgba(10, 12, 16, 0.92)"]
                  : ["rgba(255, 255, 255, 0.75)", "rgba(245, 245, 245, 0.96)"]
              }
              style={styles.gradient}
            >
              {/* Header */}
              <View style={styles.header}>
                <Text style={styles.title}>Add Class</Text>
                <TouchableOpacity onPress={onClose} style={styles.closeButton}>
                  <Ionicons
                    name="close"
                    size={24}
                    color={colors.text.secondary}
                  />
                </TouchableOpacity>
              </View>

              <ScrollView showsVerticalScrollIndicator={false}>
                {/* Course Selection */}
                <Text style={styles.label}>Course</Text>
                <ScrollView
                  horizontal
                  showsHorizontalScrollIndicator={false}
                  style={styles.courseScroll}
                >
                  {courses.map((course) => (
                    <TouchableOpacity
                      key={course.id}
                      style={[
                        styles.courseChip,
                        selectedCourse === course.id && {
                          backgroundColor: course.color,
                        },
                      ]}
                      onPress={() => handleCourseSelect(course.id)}
                    >
                      <Text
                        style={[
                          styles.courseChipText,
                          selectedCourse === course.id &&
                            styles.courseChipTextActive,
                        ]}
                      >
                        {course.shortName}
                      </Text>
                    </TouchableOpacity>
                  ))}
                </ScrollView>

                {/* Day Selection */}
                <Text style={styles.label}>Day</Text>
                <View style={styles.dayContainer}>
                  {DAYS.map((day) => (
                    <TouchableOpacity
                      key={day}
                      style={[
                        styles.dayChip,
                        selectedDay === day && styles.dayChipActive,
                      ]}
                      onPress={() => setSelectedDay(day)}
                    >
                      <Text
                        style={[
                          styles.dayChipText,
                          selectedDay === day && styles.dayChipTextActive,
                        ]}
                      >
                        {day}
                      </Text>
                    </TouchableOpacity>
                  ))}
                </View>

                {/* Time Inputs */}
                <View style={styles.timeRow}>
                  <View style={styles.timeInput}>
                    <Text style={styles.label}>Start Time</Text>
                    <TouchableOpacity
                      style={styles.timeDisplay}
                      onPress={() => {
                        setTimePickerType("start");
                        setShowTimePicker(true);
                      }}
                    >
                      <Text style={styles.timeDisplayText}>{startTime}</Text>
                    </TouchableOpacity>
                  </View>
                  <View style={styles.timeInput}>
                    <Text style={styles.label}>End Time</Text>
                    <TouchableOpacity
                      style={styles.timeDisplay}
                      onPress={() => {
                        setTimePickerType("end");
                        setShowTimePicker(true);
                      }}
                    >
                      <Text style={styles.timeDisplayText}>{endTime}</Text>
                    </TouchableOpacity>
                  </View>
                </View>

                {/* Room Input */}
                <Text style={styles.label}>Room / Venue</Text>
                <TextInput
                  style={styles.input}
                  value={room}
                  onChangeText={setRoom}
                  placeholder="e.g., LH-101"
                  placeholderTextColor={colors.text.muted}
                />

                {/* Recent Rooms */}
                {recentRooms.length > 0 && (
                  <View style={styles.recentRoomsContainer}>
                    <Text style={styles.recentRoomsLabel}>Recent:</Text>
                    <ScrollView
                      horizontal
                      showsHorizontalScrollIndicator={false}
                      style={styles.recentRoomsScroll}
                    >
                      {recentRooms.map((recentRoom, index) => (
                        <TouchableOpacity
                          key={`${recentRoom}-${index}`}
                          style={[
                            styles.recentRoomChip,
                            room === recentRoom && styles.recentRoomChipActive,
                          ]}
                          onPress={() => setRoom(recentRoom)}
                        >
                          <Ionicons
                            name="location-outline"
                            size={12}
                            color={
                              room === recentRoom ? "#fff" : colors.text.muted
                            }
                          />
                          <Text
                            style={[
                              styles.recentRoomText,
                              room === recentRoom &&
                                styles.recentRoomTextActive,
                            ]}
                          >
                            {recentRoom}
                          </Text>
                        </TouchableOpacity>
                      ))}
                    </ScrollView>
                  </View>
                )}

                {/* Submit Button */}
                <TouchableOpacity
                  onPress={handleSubmit}
                  style={styles.submitContainer}
                >
                  <LinearGradient
                    colors={[colors.primary.teal, colors.primary.blue]}
                    start={{ x: 0, y: 0 }}
                    end={{ x: 1, y: 1 }}
                    style={styles.submitButton}
                  >
                    <Ionicons
                      name="add-circle-outline"
                      size={20}
                      color="#fff"
                    />
                    <Text style={styles.submitText}>Add Class</Text>
                  </LinearGradient>
                </TouchableOpacity>
              </ScrollView>
            </LinearGradient>
          </BlurView>
        </View>
      </KeyboardAvoidingView>

      {/* Time Picker Modal */}
      <TimePickerModal
        visible={showTimePicker}
        title={
          timePickerType === "start" ? "Select Start Time" : "Select End Time"
        }
        initialTime={timePickerType === "start" ? startTime : endTime}
        onConfirm={(time) => {
          if (timePickerType === "start") {
            setStartTime(time);
          } else {
            setEndTime(time);
          }
          setShowTimePicker(false);
        }}
        onCancel={() => setShowTimePicker(false)}
      />
    </Modal>
  );
};

const createStyles = (colors: ThemePalette) =>
  StyleSheet.create({
    modalOverlay: {
      flex: 1,
      backgroundColor: colors.overlay,
      justifyContent: "flex-end",
    },
    modalContent: {
      borderTopLeftRadius: 24,
      borderTopRightRadius: 24,
      overflow: "hidden",
      maxHeight: "85%",
    },
    blurContainer: {
      borderTopLeftRadius: 24,
      borderTopRightRadius: 24,
    },
    gradient: {
      padding: 20,
      borderTopLeftRadius: 24,
      borderTopRightRadius: 24,
    },
    header: {
      flexDirection: "row",
      justifyContent: "space-between",
      alignItems: "center",
      marginBottom: 20,
    },
    title: {
      color: colors.text.primary,
      fontSize: 24,
      fontWeight: "700",
    },
    closeButton: {
      padding: 8,
      backgroundColor: colors.glass.dark,
      borderRadius: 12,
    },
    label: {
      color: colors.text.secondary,
      fontSize: 14,
      fontWeight: "600",
      marginBottom: 8,
      marginTop: 16,
    },
    courseScroll: {
      marginBottom: 8,
    },
    courseChip: {
      paddingHorizontal: 16,
      paddingVertical: 10,
      borderRadius: 20,
      backgroundColor: colors.glass.light,
      marginRight: 8,
      borderWidth: 1,
      borderColor: colors.glass.border,
    },
    courseChipText: {
      color: colors.text.secondary,
      fontSize: 14,
      fontWeight: "500",
    },
    courseChipTextActive: {
      color: "#fff",
      fontWeight: "600",
    },
    dayContainer: {
      flexDirection: "row",
      flexWrap: "wrap",
      gap: 8,
    },
    dayChip: {
      paddingHorizontal: 16,
      paddingVertical: 10,
      borderRadius: 12,
      backgroundColor: colors.glass.dark,
      borderWidth: 1,
      borderColor: colors.glass.border,
    },
    dayChipActive: {
      backgroundColor: colors.primary.blue,
      borderColor: colors.primary.blue,
    },
    dayChipText: {
      color: colors.text.secondary,
      fontSize: 13,
      fontWeight: "600",
    },
    dayChipTextActive: {
      color: "#fff",
    },
    timeRow: {
      flexDirection: "row",
      gap: 16,
    },
    timeInput: {
      flex: 1,
    },
    timeDisplay: {
      backgroundColor: colors.glass.dark,
      borderRadius: 12,
      padding: 16,
      borderWidth: 1,
      borderColor: colors.glass.border,
      alignItems: "center",
    },
    timeDisplayText: {
      color: colors.text.primary,
      fontSize: 22,
      fontWeight: "600",
      fontVariant: ["tabular-nums"],
    },
    input: {
      backgroundColor: colors.glass.dark,
      borderRadius: 12,
      padding: 14,
      color: colors.text.primary,
      fontSize: 16,
      borderWidth: 1,
      borderColor: colors.glass.border,
    },
    recentRoomsContainer: {
      flexDirection: "row",
      alignItems: "center",
      marginTop: 10,
    },
    recentRoomsLabel: {
      color: colors.text.muted,
      fontSize: 12,
      fontWeight: "500",
      marginRight: 8,
    },
    recentRoomsScroll: {
      flex: 1,
    },
    recentRoomChip: {
      flexDirection: "row",
      alignItems: "center",
      paddingHorizontal: 10,
      paddingVertical: 6,
      borderRadius: 12,
      backgroundColor: colors.glass.dark,
      marginRight: 6,
      borderWidth: 1,
      borderColor: colors.glass.border,
    },
    recentRoomChipActive: {
      backgroundColor: colors.primary.teal,
      borderColor: colors.primary.teal,
    },
    recentRoomText: {
      color: colors.text.muted,
      fontSize: 12,
      fontWeight: "500",
      marginLeft: 4,
    },
    recentRoomTextActive: {
      color: "#fff",
      fontWeight: "600",
    },
    submitContainer: {
      marginTop: 24,
      marginBottom: 20,
    },
    submitButton: {
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "center",
      padding: 16,
      borderRadius: 16,
    },
    submitText: {
      color: "#fff",
      fontSize: 16,
      fontWeight: "700",
      marginLeft: 8,
    },
  });

export default AddClassModal;
