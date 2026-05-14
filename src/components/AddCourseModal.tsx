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
import {
  ThemePalette,
  useThemedColors,
  useThemedStyles,
} from "../theme/useTheme";

interface AddCourseModalProps {
  visible: boolean;
  onClose: () => void;
  onSubmit: (course: {
    name: string;
    shortName: string;
    professor: string;
    credits: number;
    room: string;
    color: string;
    semesterId?: string;
  }) => void;
  semesterId?: string;
}

const COURSE_COLORS = [
  "#3b82f6", // blue
  "#8b5cf6", // purple
  "#14b8a6", // teal
  "#6366f1", // indigo
  "#ec4899", // pink
  "#06b6d4", // cyan
  "#f59e0b", // amber
  "#22c55e", // green
  "#ef4444", // red
  "#a855f7", // violet
];

const AddCourseModal: React.FC<AddCourseModalProps> = ({
  visible,
  onClose,
  onSubmit,
  semesterId,
}) => {
  const { colors, isDark } = useThemedColors();
  const styles = useThemedStyles(createStyles);
  const [name, setName] = useState("");
  const [shortName, setShortName] = useState("");
  const [professor, setProfessor] = useState("");
  const [credits, setCredits] = useState("3");
  const [room, setRoom] = useState("");
  const [selectedColor, setSelectedColor] = useState(COURSE_COLORS[0]);

  const handleSubmit = () => {
    if (
      !name.trim() ||
      !shortName.trim() ||
      !professor.trim() ||
      !room.trim()
    ) {
      return;
    }
    onSubmit({
      name: name.trim(),
      shortName: shortName.trim(),
      professor: professor.trim(),
      credits: parseInt(credits) || 3,
      room: room.trim(),
      color: selectedColor,
      semesterId,
    });
    resetForm();
    onClose();
  };

  const resetForm = () => {
    setName("");
    setShortName("");
    setProfessor("");
    setCredits("3");
    setRoom("");
    setSelectedColor(COURSE_COLORS[0]);
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
                <Text style={styles.title}>Add Course</Text>
                <TouchableOpacity onPress={onClose} style={styles.closeButton}>
                  <Ionicons
                    name="close"
                    size={24}
                    color={colors.text.secondary}
                  />
                </TouchableOpacity>
              </View>

              <ScrollView showsVerticalScrollIndicator={false}>
                {/* Course Name */}
                <Text style={styles.label}>Course Name *</Text>
                <TextInput
                  style={styles.input}
                  value={name}
                  onChangeText={setName}
                  placeholder="e.g., Introduction to Programming"
                  placeholderTextColor={colors.text.muted}
                />

                {/* Short Name */}
                <Text style={styles.label}>Short Name *</Text>
                <TextInput
                  style={styles.input}
                  value={shortName}
                  onChangeText={setShortName}
                  placeholder="e.g., Intro Prog"
                  placeholderTextColor={colors.text.muted}
                />

                {/* Professor */}
                <Text style={styles.label}>Professor *</Text>
                <TextInput
                  style={styles.input}
                  value={professor}
                  onChangeText={setProfessor}
                  placeholder="e.g., Dr. Smith"
                  placeholderTextColor={colors.text.muted}
                />

                {/* Credits & Room */}
                <View style={styles.row}>
                  <View style={styles.halfInput}>
                    <Text style={styles.label}>Credits</Text>
                    <TextInput
                      style={styles.input}
                      value={credits}
                      onChangeText={setCredits}
                      placeholder="3"
                      placeholderTextColor={colors.text.muted}
                      keyboardType="numeric"
                    />
                  </View>
                  <View style={styles.halfInput}>
                    <Text style={styles.label}>Default Room *</Text>
                    <TextInput
                      style={styles.input}
                      value={room}
                      onChangeText={setRoom}
                      placeholder="e.g., LH-101"
                      placeholderTextColor={colors.text.muted}
                    />
                  </View>
                </View>

                {/* Color Selection */}
                <Text style={styles.label}>Course Color</Text>
                <View style={styles.colorContainer}>
                  {COURSE_COLORS.map((color) => (
                    <TouchableOpacity
                      key={color}
                      style={[
                        styles.colorOption,
                        { backgroundColor: color },
                        selectedColor === color && styles.colorOptionSelected,
                      ]}
                      onPress={() => setSelectedColor(color)}
                    >
                      {selectedColor === color && (
                        <Ionicons name="checkmark" size={16} color="#fff" />
                      )}
                    </TouchableOpacity>
                  ))}
                </View>

                {/* Preview */}
                <Text style={styles.label}>Preview</Text>
                <View
                  style={[styles.preview, { borderLeftColor: selectedColor }]}
                >
                  <Text style={styles.previewName}>
                    {name || "Course Name"}
                  </Text>
                  <Text style={styles.previewDetails}>
                    {shortName || "Short"} • {professor || "Professor"} •{" "}
                    {room || "Room"}
                  </Text>
                </View>

                {/* Submit Button */}
                <TouchableOpacity
                  onPress={handleSubmit}
                  style={styles.submitContainer}
                >
                  <LinearGradient
                    colors={[selectedColor, `${selectedColor}CC`]}
                    start={{ x: 0, y: 0 }}
                    end={{ x: 1, y: 1 }}
                    style={styles.submitButton}
                  >
                    <Ionicons
                      name="add-circle-outline"
                      size={20}
                      color="#fff"
                    />
                    <Text style={styles.submitText}>Add Course</Text>
                  </LinearGradient>
                </TouchableOpacity>
              </ScrollView>
            </LinearGradient>
          </BlurView>
        </View>
      </KeyboardAvoidingView>
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
    row: {
      flexDirection: "row",
      gap: 16,
    },
    halfInput: {
      flex: 1,
    },
    colorContainer: {
      flexDirection: "row",
      flexWrap: "wrap",
      gap: 12,
    },
    colorOption: {
      width: 40,
      height: 40,
      borderRadius: 20,
      alignItems: "center",
      justifyContent: "center",
    },
    colorOptionSelected: {
      borderWidth: 3,
      borderColor: "#fff",
    },
    preview: {
      backgroundColor: colors.glass.dark,
      borderRadius: 12,
      padding: 16,
      borderLeftWidth: 4,
    },
    previewName: {
      color: colors.text.primary,
      fontSize: 16,
      fontWeight: "600",
      marginBottom: 4,
    },
    previewDetails: {
      color: colors.text.muted,
      fontSize: 13,
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

export default AddCourseModal;
