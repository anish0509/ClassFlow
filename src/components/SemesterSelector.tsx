import React, { useState } from "react";
import {
  View,
  Text,
  TouchableOpacity,
  Modal,
  FlatList,
} from "react-native";
import { BlurView } from "expo-blur";
import { Ionicons } from "@expo/vector-icons";
import { Semester } from "../types";
import {
  ThemePalette,
  useThemedColors,
  useThemedStyles,
} from "../theme/useTheme";

interface SemesterSelectorProps {
  semesters: Semester[];
  currentSemester: Semester;
  onSelect: (semester: Semester) => void;
}

const SemesterSelector: React.FC<SemesterSelectorProps> = ({
  semesters,
  currentSemester,
  onSelect,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const { colors, isDark } = useThemedColors();
  const styles = useThemedStyles(createStyles);

  const handleSelect = (semester: Semester) => {
    onSelect(semester);
    setIsOpen(false);
  };

  return (
    <>
      <TouchableOpacity
        style={styles.selector}
        onPress={() => setIsOpen(true)}
        activeOpacity={0.7}
      >
        <Text style={styles.selectorText}>{currentSemester.name}</Text>
        <Ionicons name="chevron-down" size={18} color={colors.text.secondary} />
      </TouchableOpacity>

      <Modal
        visible={isOpen}
        transparent
        animationType="fade"
        onRequestClose={() => setIsOpen(false)}
      >
        <TouchableOpacity
          style={styles.modalOverlay}
          activeOpacity={1}
          onPress={() => setIsOpen(false)}
        >
          <View style={styles.modalContent}>
            <BlurView intensity={90} tint={isDark ? "dark" : "light"} style={styles.blur}>
              <Text style={styles.modalTitle}>Select Semester</Text>
              <FlatList
                data={semesters}
                keyExtractor={(item) => item.id}
                renderItem={({ item }) => (
                  <TouchableOpacity
                    style={[
                      styles.optionItem,
                      item.id === currentSemester.id && styles.optionItemActive,
                    ]}
                    onPress={() => handleSelect(item)}
                  >
                    <Text
                      style={[
                        styles.optionText,
                        item.id === currentSemester.id &&
                          styles.optionTextActive,
                      ]}
                    >
                      {item.name}
                    </Text>
                    {item.id === currentSemester.id && (
                      <Ionicons
                        name="checkmark-circle"
                        size={20}
                        color={colors.primary.teal}
                      />
                    )}
                  </TouchableOpacity>
                )}
              />
            </BlurView>
          </View>
        </TouchableOpacity>
      </Modal>
    </>
  );
};

const createStyles = (colors: ThemePalette) => ({
  selector: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: colors.glass.dark,
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: 14,
    borderWidth: 0.5,
    borderColor: colors.border.subtle,
  },
  selectorText: {
    color: colors.text.primary,
    fontSize: 13,
    fontWeight: "600",
    marginRight: 5,
    letterSpacing: 0.2,
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: colors.overlay,
    justifyContent: "center",
    alignItems: "center",
    padding: 20,
  },
  modalContent: {
    width: "90%",
    maxWidth: 340,
    borderRadius: 24,
    overflow: "hidden",
  },
  blur: {
    padding: 20,
  },
  modalTitle: {
    color: colors.text.primary,
    fontSize: 20,
    fontWeight: "700",
    marginBottom: 16,
    textAlign: "center",
  },
  optionItem: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    paddingVertical: 14,
    paddingHorizontal: 16,
    borderRadius: 12,
    marginBottom: 8,
    backgroundColor: colors.glass.dark,
  },
  optionItemActive: {
    backgroundColor: colors.glass.medium,
    borderWidth: 1,
    borderColor: colors.primary.teal,
  },
  optionText: {
    color: colors.text.secondary,
    fontSize: 16,
    fontWeight: "500",
  },
  optionTextActive: {
    color: colors.text.primary,
    fontWeight: "600",
  },
});

export default SemesterSelector;
