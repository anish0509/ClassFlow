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
import { COLORS } from "../constants/data";
import { Course, ClassSchedule } from "../types";
import { TimePickerModal } from "./WheelPicker";
import * as db from "../database";

interface EditClassModalProps {
  visible: boolean;
  onClose: () => void;
  onSubmit: (
    classId: string,
    updates: {
      dayOfWeek: string;
      startTime: string;
      endTime: string;
      room: string;
    },
  ) => void;
  onDelete: (classItem: ClassSchedule) => void;
  classItem: ClassSchedule | null;
  courses: Course[];
}

const DAYS = ["MON", "TUE", "WED", "THU", "FRI", "SAT"];

const EditClassModal: React.FC<EditClassModalProps> = ({
  visible,
  onClose,
  onSubmit,
  onDelete,
  classItem,
  courses,
}) => {
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

  // Populate form with existing class data
  useEffect(() => {
    if (visible && classItem) {
      loadRecentRooms();
      setSelectedDay(classItem.dayOfWeek);
      setStartTime(classItem.startTime);
      setEndTime(classItem.endTime);
      setRoom(classItem.room || "");
    }
  }, [visible, classItem, loadRecentRooms]);

  const handleSubmit = () => {
    if (!room) {
      Alert.alert("Missing Info", "Please enter a room/venue");
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
    if (!classItem) return;

    onSubmit(classItem.id, {
      dayOfWeek: selectedDay,
      startTime,
      endTime,
      room,
    });
    onClose();
  };

  const handleDelete = () => {
    if (!classItem) return;
    onDelete(classItem);
    onClose();
  };

  const course = courses.find((c) => c.id === classItem?.courseId);

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
          <BlurView intensity={80} tint="dark" style={styles.blurContainer}>
            <LinearGradient
              colors={[
                "rgba(255, 255, 255, 0.15)",
                "rgba(255, 255, 255, 0.05)",
              ]}
              style={styles.gradient}
            >
              {/* Header */}
              <View style={styles.header}>
                <View>
                  <Text style={styles.title}>Edit Class</Text>
                  {course && (
                    <View style={styles.courseInfo}>
                      <View
                        style={[
                          styles.courseColorDot,
                          { backgroundColor: course.color },
                        ]}
                      />
                      <Text style={styles.courseName}>
                        {course.shortName || course.name}
                      </Text>
                    </View>
                  )}
                </View>
                <TouchableOpacity onPress={onClose} style={styles.closeButton}>
                  <Ionicons
                    name="close"
                    size={24}
                    color={COLORS.text.secondary}
                  />
                </TouchableOpacity>
              </View>

              <ScrollView showsVerticalScrollIndicator={false}>
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
                  placeholderTextColor={COLORS.text.muted}
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
                              room === recentRoom ? "#fff" : COLORS.text.muted
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

                {/* Action Buttons */}
                <View style={styles.buttonRow}>
                  {/* Delete Button */}
                  <TouchableOpacity
                    onPress={handleDelete}
                    style={styles.deleteButton}
                  >
                    <Ionicons name="trash-outline" size={20} color="#ef4444" />
                    <Text style={styles.deleteText}>Delete</Text>
                  </TouchableOpacity>

                  {/* Save Button */}
                  <TouchableOpacity
                    onPress={handleSubmit}
                    style={styles.submitContainer}
                  >
                    <LinearGradient
                      colors={[COLORS.primary.teal, COLORS.primary.blue]}
                      start={{ x: 0, y: 0 }}
                      end={{ x: 1, y: 1 }}
                      style={styles.submitButton}
                    >
                      <Ionicons
                        name="checkmark-circle-outline"
                        size={20}
                        color="#fff"
                      />
                      <Text style={styles.submitText}>Save Changes</Text>
                    </LinearGradient>
                  </TouchableOpacity>
                </View>
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

const styles = StyleSheet.create({
  modalOverlay: {
    flex: 1,
    backgroundColor: "rgba(0, 0, 0, 0.6)",
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
    alignItems: "flex-start",
    marginBottom: 20,
  },
  title: {
    color: COLORS.text.primary,
    fontSize: 24,
    fontWeight: "700",
  },
  courseInfo: {
    flexDirection: "row",
    alignItems: "center",
    marginTop: 6,
  },
  courseColorDot: {
    width: 10,
    height: 10,
    borderRadius: 5,
    marginRight: 8,
  },
  courseName: {
    color: COLORS.text.secondary,
    fontSize: 14,
    fontWeight: "500",
  },
  closeButton: {
    padding: 8,
    backgroundColor: COLORS.glass.dark,
    borderRadius: 12,
  },
  label: {
    color: COLORS.text.secondary,
    fontSize: 14,
    fontWeight: "600",
    marginBottom: 8,
    marginTop: 16,
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
    backgroundColor: COLORS.glass.dark,
    borderWidth: 1,
    borderColor: COLORS.glass.border,
  },
  dayChipActive: {
    backgroundColor: COLORS.primary.blue,
    borderColor: COLORS.primary.blue,
  },
  dayChipText: {
    color: COLORS.text.secondary,
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
    backgroundColor: COLORS.glass.dark,
    borderRadius: 12,
    padding: 16,
    borderWidth: 1,
    borderColor: COLORS.glass.border,
    alignItems: "center",
  },
  timeDisplayText: {
    color: COLORS.text.primary,
    fontSize: 22,
    fontWeight: "600",
    fontVariant: ["tabular-nums"],
  },
  input: {
    backgroundColor: COLORS.glass.dark,
    borderRadius: 12,
    padding: 14,
    color: COLORS.text.primary,
    fontSize: 16,
    borderWidth: 1,
    borderColor: COLORS.glass.border,
  },
  recentRoomsContainer: {
    flexDirection: "row",
    alignItems: "center",
    marginTop: 10,
  },
  recentRoomsLabel: {
    color: COLORS.text.muted,
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
    backgroundColor: COLORS.glass.dark,
    marginRight: 6,
    borderWidth: 1,
    borderColor: COLORS.glass.border,
  },
  recentRoomChipActive: {
    backgroundColor: COLORS.primary.teal,
    borderColor: COLORS.primary.teal,
  },
  recentRoomText: {
    color: COLORS.text.muted,
    fontSize: 12,
    fontWeight: "500",
    marginLeft: 4,
  },
  recentRoomTextActive: {
    color: "#fff",
    fontWeight: "600",
  },
  buttonRow: {
    flexDirection: "row",
    alignItems: "center",
    gap: 12,
    marginTop: 24,
    marginBottom: 20,
  },
  deleteButton: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    paddingVertical: 16,
    paddingHorizontal: 20,
    borderRadius: 16,
    backgroundColor: "rgba(239, 68, 68, 0.15)",
    borderWidth: 1,
    borderColor: "rgba(239, 68, 68, 0.3)",
  },
  deleteText: {
    color: "#ef4444",
    fontSize: 16,
    fontWeight: "600",
    marginLeft: 6,
  },
  submitContainer: {
    flex: 1,
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

export default EditClassModal;
