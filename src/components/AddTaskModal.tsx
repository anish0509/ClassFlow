import React, { useState } from "react";
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
} from "react-native";
import { BlurView } from "expo-blur";
import { LinearGradient } from "expo-linear-gradient";
import { Ionicons } from "@expo/vector-icons";
import { Course } from "../types";
import { DatePickerModal, TimePickerModal } from "./WheelPicker";
import {
  ThemePalette,
  useThemedColors,
  useThemedStyles,
} from "../theme/useTheme";

interface AddTaskModalProps {
  visible: boolean;
  onClose: () => void;
  onSubmit: (task: {
    courseId?: string;
    title: string;
    description?: string;
    dueDate?: string;
    priority: "high" | "medium" | "low";
  }) => void;
  courses: Course[];
  preselectedCourseId?: string;
}

const PRIORITIES: {
  value: "high" | "medium" | "low";
  label: string;
  color: string;
}[] = [
  { value: "high", label: "High", color: "#ef4444" },
  { value: "medium", label: "Medium", color: "#f59e0b" },
  { value: "low", label: "Low", color: "#22c55e" },
];

const AddTaskModal: React.FC<AddTaskModalProps> = ({
  visible,
  onClose,
  onSubmit,
  courses,
  preselectedCourseId,
}) => {
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [selectedCourse, setSelectedCourse] = useState<string>("");
  const [dueDate, setDueDate] = useState("");
  const [dueTime, setDueTime] = useState("");
  const [priority, setPriority] = useState<"high" | "medium" | "low">("medium");
  const [showDatePicker, setShowDatePicker] = useState(false);
  const [showTimePicker, setShowTimePicker] = useState(false);
  const { colors, isDark } = useThemedColors();
  const styles = useThemedStyles(createStyles);

  React.useEffect(() => {
    if (visible && preselectedCourseId) {
      setSelectedCourse(preselectedCourseId);
    }
  }, [visible, preselectedCourseId]);

  const handleSubmit = () => {
    if (!title.trim()) {
      return;
    }
    let finalDueDate = undefined;
    if (dueDate) {
      finalDueDate = dueTime ? `${dueDate}T${dueTime}:00` : dueDate;
    }
    onSubmit({
      courseId: selectedCourse || undefined,
      title: title.trim(),
      description: description.trim() || undefined,
      dueDate: finalDueDate,
      priority,
    });
    resetForm();
    onClose();
  };

  const resetForm = () => {
    setTitle("");
    setDescription("");
    setSelectedCourse("");
    setDueDate("");
    setDueTime("");
    setPriority("medium");
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
                <Text style={styles.title}>Add Task</Text>
                <TouchableOpacity onPress={onClose} style={styles.closeButton}>
                  <Ionicons
                    name="close"
                    size={24}
                    color={colors.text.secondary}
                  />
                </TouchableOpacity>
              </View>

              <ScrollView showsVerticalScrollIndicator={false}>
                {/* Title Input */}
                <Text style={styles.label}>Title *</Text>
                <TextInput
                  style={styles.input}
                  value={title}
                  onChangeText={setTitle}
                  placeholder="e.g., Complete Assignment"
                  placeholderTextColor={colors.text.muted}
                />

                {/* Description Input */}
                <Text style={styles.label}>Description</Text>
                <TextInput
                  style={[styles.input, styles.textArea]}
                  value={description}
                  onChangeText={setDescription}
                  placeholder="Add details about the task..."
                  placeholderTextColor={colors.text.muted}
                  multiline
                  numberOfLines={3}
                />

                {/* Course Selection */}
                <Text style={styles.label}>Related Course (Optional)</Text>
                <ScrollView
                  horizontal
                  showsHorizontalScrollIndicator={false}
                  style={styles.courseScroll}
                >
                  <TouchableOpacity
                    style={[
                      styles.courseChip,
                      !selectedCourse && styles.courseChipActive,
                    ]}
                    onPress={() => setSelectedCourse("")}
                  >
                    <Text
                      style={[
                        styles.courseChipText,
                        !selectedCourse && styles.courseChipTextActive,
                      ]}
                    >
                      None
                    </Text>
                  </TouchableOpacity>
                  {courses.map((course) => (
                    <TouchableOpacity
                      key={course.id}
                      style={[
                        styles.courseChip,
                        selectedCourse === course.id && {
                          backgroundColor: course.color,
                        },
                      ]}
                      onPress={() => setSelectedCourse(course.id)}
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

                {/* Due Date & Time Selection */}
                <View style={{ flexDirection: "row", gap: 12, marginBottom: 4 }}>
                  <View style={{ flex: 1 }}>
                    <Text style={styles.label}>Due Date</Text>
                    <TouchableOpacity
                      style={styles.dateInput}
                      onPress={() => setShowDatePicker(true)}
                    >
                      <Text
                        numberOfLines={1}
                        style={
                          dueDate
                            ? styles.dateInputText
                            : styles.dateInputPlaceholder
                        }
                      >
                        {dueDate || "Select Date"}
                      </Text>
                      <Ionicons
                        name="calendar-outline"
                        size={18}
                        color={colors.text.muted}
                      />
                    </TouchableOpacity>
                  </View>

                  <View style={{ flex: 1 }}>
                    <Text style={styles.label}>Due Time</Text>
                    <TouchableOpacity
                      style={styles.dateInput}
                      onPress={() => setShowTimePicker(true)}
                    >
                      <Text
                        numberOfLines={1}
                        style={
                          dueTime
                            ? styles.dateInputText
                            : styles.dateInputPlaceholder
                        }
                      >
                        {dueTime || "Select Time"}
                      </Text>
                      <Ionicons
                        name="time-outline"
                        size={18}
                        color={colors.text.muted}
                      />
                    </TouchableOpacity>
                  </View>
                </View>



                {/* Submit Button */}
                <TouchableOpacity
                  onPress={handleSubmit}
                  style={styles.submitContainer}
                >
                  <LinearGradient
                    colors={[colors.primary.purple, colors.primary.blue]}
                    start={{ x: 0, y: 0 }}
                    end={{ x: 1, y: 1 }}
                    style={styles.submitButton}
                  >
                    <Ionicons
                      name="add-circle-outline"
                      size={20}
                      color="#fff"
                    />
                    <Text style={styles.submitText}>Add Task</Text>
                  </LinearGradient>
                </TouchableOpacity>
              </ScrollView>
            </LinearGradient>
          </BlurView>
        </View>
      </KeyboardAvoidingView>

      {/* Date Picker Modal */}
      <DatePickerModal
        visible={showDatePicker}
        title="Select Due Date"
        initialDate={dueDate || new Date().toISOString().split("T")[0]}
        onConfirm={(date) => {
          setDueDate(date);
          setShowDatePicker(false);
        }}
        onCancel={() => setShowDatePicker(false)}
      />

      {/* Time Picker Modal */}
      <TimePickerModal
        visible={showTimePicker}
        title="Select Due Time"
        initialTime={dueTime || "12:00"}
        onConfirm={(time) => {
          setDueTime(time);
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
      maxHeight: "90%",
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
    input: {
      backgroundColor: colors.glass.dark,
      borderRadius: 12,
      padding: 14,
      color: colors.text.primary,
      fontSize: 16,
      borderWidth: 1,
      borderColor: colors.glass.border,
    },
    dateInput: {
      backgroundColor: colors.glass.dark,
      borderRadius: 12,
      padding: 14,
      flexDirection: "row",
      justifyContent: "space-between",
      alignItems: "center",
      borderWidth: 1,
      borderColor: colors.glass.border,
    },
    dateInputText: {
      color: colors.text.primary,
      fontSize: 16,
    },
    dateInputPlaceholder: {
      color: colors.text.muted,
      fontSize: 16,
    },
    textArea: {
      minHeight: 80,
      textAlignVertical: "top",
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
    courseChipActive: {
      backgroundColor: colors.glass.medium,
      borderColor: colors.primary.teal,
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
    priorityContainer: {
      flexDirection: "row",
      gap: 12,
    },
    priorityChip: {
      flex: 1,
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "center",
      paddingVertical: 12,
      borderRadius: 12,
      backgroundColor: colors.glass.dark,
      borderWidth: 1,
      borderColor: colors.glass.border,
    },
    priorityDot: {
      width: 8,
      height: 8,
      borderRadius: 4,
      marginRight: 8,
    },
    priorityText: {
      color: colors.text.secondary,
      fontSize: 14,
      fontWeight: "600",
    },
    priorityTextActive: {
      color: "#fff",
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

export default AddTaskModal;
