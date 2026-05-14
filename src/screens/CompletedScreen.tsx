import React, { useState, useEffect, useMemo } from "react";
import {
  View,
  Text,
  ScrollView,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  ActivityIndicator,
  Alert,
  LayoutAnimation,
  Platform,
} from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";
import { BlurView } from "expo-blur";
import { GlassHeader, TaskCard, AddTaskModal, BackgroundMesh, EmptyStateCard } from "../components";
import { useTimetableStore } from "../store/timetableStore";
import { useShallow } from "zustand/react/shallow";
import { ThemePalette, useThemedColors } from "../theme/useTheme";

const CompletedScreen: React.FC = () => {
  const insets = useSafeAreaInsets();
  const { colors, isDark } = useThemedColors();
  const styles = useMemo(() => createStyles(colors), [colors]);
  const {
    tasks,
    courses,
    isLoading,
    isInitialized,
    loadTasks,
    completeTask,
    uncompleteTask,
    deleteTask,
    addTask,
  } = useTimetableStore(
    useShallow((state) => ({
      tasks: state.tasks,
      courses: state.courses,
      isLoading: state.isLoading,
      isInitialized: state.isInitialized,
      loadTasks: state.loadTasks,
      completeTask: state.completeTask,
      uncompleteTask: state.uncompleteTask,
      deleteTask: state.deleteTask,
      addTask: state.addTask,
    }))
  );

  const [showAddTaskModal, setShowAddTaskModal] = useState(false);
  const [showCompleted, setShowCompleted] = useState(true);

  useEffect(() => {
    if (isInitialized) {
      loadTasks();
    }
  }, [isInitialized]);

  const handleCompleteTask = async (taskId: string) => {
    try {
      LayoutAnimation.configureNext(LayoutAnimation.Presets.easeInEaseOut);
      await completeTask(taskId);
    } catch (error) {
      Alert.alert("Error", "Failed to update task");
    }
  };

  const handleUncompleteTask = async (taskId: string) => {
    try {
      LayoutAnimation.configureNext(LayoutAnimation.Presets.easeInEaseOut);
      await uncompleteTask(taskId);
    } catch (error) {
      Alert.alert("Error", "Failed to update task");
    }
  };

  const handleDeleteTask = async (taskId: string) => {
    Alert.alert("Delete Task", "Are you sure you want to delete this task?", [
      { text: "Cancel", style: "cancel" },
      {
        text: "Delete",
        style: "destructive",
        onPress: async () => {
          try {
            await deleteTask(taskId);
          } catch (error) {
            Alert.alert("Error", "Failed to delete task");
          }
        },
      },
    ]);
  };

  const handleAddTask = async (taskData: any) => {
    try {
      await addTask(taskData);
    } catch (error) {
      Alert.alert("Error", "Failed to add task");
    }
  };
  const toggleCompletedSection = () => {
    LayoutAnimation.configureNext(LayoutAnimation.Presets.easeInEaseOut);
    setShowCompleted(!showCompleted);
  };

  // Filter and sort tasks
  const pendingTasks = useMemo(() => {
    return [...tasks]
      .filter((t) => t.status !== "completed")
      .sort((a, b) => {
        const timeA = a.dueDate ? new Date(a.dueDate).getTime() : 9999999999999;
        const timeB = b.dueDate ? new Date(b.dueDate).getTime() : 9999999999999;
        if (timeA !== timeB) return timeA - timeB;
        const createdA = a.createdAt ? new Date(a.createdAt).getTime() : 0;
        const createdB = b.createdAt ? new Date(b.createdAt).getTime() : 0;
        return createdB - createdA; 
      });
  }, [tasks]);

  const completedTasks = useMemo(() => {
    return [...tasks]
      .filter((t) => t.status === "completed")
      .sort((a, b) => {
        const timeA = a.completedAt ? new Date(a.completedAt).getTime() : 0;
        const timeB = b.completedAt ? new Date(b.completedAt).getTime() : 0;
        return timeB - timeA; 
      });
  }, [tasks]);

  if (isLoading) {
    return (
      <LinearGradient
        colors={[colors.background.start, colors.background.end]}
        style={styles.loadingContainer}
      >
        <ActivityIndicator size="large" color={colors.primary.teal} />
      </LinearGradient>
    );
  }

  return (
    <LinearGradient
      colors={[colors.background.start, colors.background.end]}
      style={styles.container}
    >
      <BackgroundMesh />
      <GlassHeader
        title="Tasks"
        subtitle={`${pendingTasks.length} pending, ${completedTasks.length} completed`}
        rightComponent={
          <TouchableOpacity
            onPress={() => setShowAddTaskModal(true)}
            style={styles.headerButton}
          >
            <Ionicons name="add" size={24} color={colors.primary.teal} />
          </TouchableOpacity>
        }
      />

      <ScrollView
        style={styles.scrollView}
        contentContainerStyle={[
          styles.scrollContent,
          { paddingBottom: insets.bottom + 130 },
        ]}
        showsVerticalScrollIndicator={false}
      >
        {/* Pending Tasks Flat List */}
        {pendingTasks.length === 0 ? (
          <EmptyStateCard
            icon="checkbox-outline"
            title="No pending tasks"
            description="Tap the + button to create one"
            buttonText="Create Task"
            onPress={() => setShowAddTaskModal(true)}
          />
        ) : (
          pendingTasks.map((task) => (
            <TaskCard
              key={task.id}
              task={task}
              onComplete={() => handleCompleteTask(task.id)}
              onDelete={() => handleDeleteTask(task.id)}
            />
          ))
        )}

        {/* Completed Section */}
        {completedTasks.length > 0 && (
          <>
            <TouchableOpacity
              style={styles.completedHeader}
              onPress={toggleCompletedSection}
              activeOpacity={0.7}
            >
              <View style={styles.completedHeaderLeft}>
                <Text style={styles.completedHeaderText}>Completed</Text>
                <View style={styles.completedBadge}>
                  <Text style={styles.completedBadgeText}>
                    {completedTasks.length}
                  </Text>
                </View>
              </View>
              <Ionicons
                name={showCompleted ? "chevron-up" : "chevron-down"}
                size={22}
                color={colors.text.muted}
              />
            </TouchableOpacity>

            {showCompleted &&
              completedTasks.map((task) => (
                <TaskCard
                  key={task.id}
                  task={task}
                  onComplete={() => handleCompleteTask(task.id)}
                  onUncomplete={() => handleUncompleteTask(task.id)}
                  onDelete={() => handleDeleteTask(task.id)}
                />
              ))}
          </>
        )}
      </ScrollView>

      <AddTaskModal
        visible={showAddTaskModal}
        onClose={() => setShowAddTaskModal(false)}
        onSubmit={handleAddTask}
        courses={courses}
      />
    </LinearGradient>
  );
};

const createStyles = (colors: ThemePalette) =>
  StyleSheet.create({
    container: {
      flex: 1,
    },
    loadingContainer: {
      flex: 1,
      alignItems: "center",
      justifyContent: "center",
    },
    scrollView: {
      flex: 1,
    },
    scrollContent: {
      padding: 16,
    },
    quickCard: {
      padding: 16,
      borderRadius: 18,
      backgroundColor: colors.glass.cardFill,
      borderWidth: 1,
      borderColor: colors.glass.cardBorder,
      gap: 12,
    },
    quickLabel: {
      color: colors.text.secondary,
      fontWeight: "600",
      marginBottom: 2,
      fontSize: 13,
    },
    quickRow: {
      flexDirection: "row",
      alignItems: "center",
      gap: 10,
    },
    quickInput: {
      flex: 1,
      backgroundColor: colors.mode === "dark" ? "rgba(255, 255, 255, 0.03)" : "rgba(0, 0, 0, 0.08)",
      borderRadius: 12,
      paddingHorizontal: 14,
      paddingVertical: 12,
      color: colors.text.primary,
      borderWidth: 1,
      borderColor: colors.glass.cardBorder,
    },
    quickAddButton: {
      width: 46,
      height: 46,
      borderRadius: 12,
      alignItems: "center",
      justifyContent: "center",
      backgroundColor: colors.primary.teal,
      shadowColor: colors.primary.teal,
      shadowOffset: { width: 0, height: 4 },
      shadowOpacity: 0.25,
      shadowRadius: 8,
    },
    quickChipsRow: {
      flexDirection: "row",
      gap: 8,
      marginTop: 4,
    },
    quickChip: {
      paddingHorizontal: 14,
      paddingVertical: 8,
      borderRadius: 10,
      borderWidth: 1,
      borderColor: colors.glass.cardBorder,
      backgroundColor: colors.glass.cardFill,
    },
    quickChipText: {
      color: colors.text.secondary,
      fontWeight: "600",
      fontSize: 12,
    },
    headerButton: {
      width: 42,
      height: 42,
      borderRadius: 21,
      backgroundColor: colors.glass.cardFill,
      alignItems: "center",
      justifyContent: "center",
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: colors.glass.cardBorder,
    },
    completedHeader: {
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "space-between",
      marginTop: 24,
      marginBottom: 12,
      paddingVertical: 8,
    },
    completedHeaderLeft: {
      flexDirection: "row",
      alignItems: "center",
      gap: 12,
    },
    completedHeaderText: {
      color: colors.text.primary,
      fontSize: 18,
      fontWeight: "600",
      fontFamily: Platform.OS === "ios" ? "Menlo" : "monospace",
    },
    completedBadge: {
      backgroundColor: colors.glass.cardFill,
      paddingHorizontal: 10,
      paddingVertical: 4,
      borderRadius: 10,
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: colors.glass.cardBorder,
    },
    completedBadgeText: {
      color: colors.text.secondary,
      fontSize: 14,
      fontWeight: "600",
    },
    emptyState: {
      alignItems: "center",
      justifyContent: "center",
      paddingVertical: 60,
    },
    emptyText: {
      color: colors.text.secondary,
      fontSize: 18,
      fontWeight: "600",
      marginTop: 16,
    },
    emptySubtext: {
      color: colors.text.muted,
      fontSize: 14,
      marginTop: 8,
    },
    laneSection: {
      marginTop: 10,
    },
    laneHeader: {
      flexDirection: "row",
      alignItems: "center",
      gap: 8,
      marginBottom: 6,
    },
    laneDot: {
      width: 10,
      height: 10,
      borderRadius: 5,
    },
    laneTitle: {
      color: colors.text.primary,
      fontSize: 15,
      fontWeight: "700",
      flex: 1,
    },
    laneBadge: {
      backgroundColor: colors.glass.cardFill,
      paddingHorizontal: 10,
      paddingVertical: 4,
      borderRadius: 10,
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: colors.glass.cardBorder,
    },
    laneBadgeText: {
      color: colors.text.secondary,
      fontWeight: "700",
      fontSize: 12,
    },
    laneEmpty: {
      color: colors.text.muted,
      fontSize: 12,
      marginLeft: 4,
      marginBottom: 4,
    },
  });

export default CompletedScreen;
