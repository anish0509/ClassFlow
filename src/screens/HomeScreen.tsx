import React, { useEffect, useMemo, useState, useRef } from "react";
import {
  ActivityIndicator,
  Alert,
  Modal,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
  Animated,
} from "react-native";
import { GestureHandlerRootView, GestureDetector, Gesture, Directions } from "react-native-gesture-handler";
import { LinearGradient } from "expo-linear-gradient";
import BackgroundMesh from "../components/BackgroundMesh";
import { BlurView } from "expo-blur";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import {
  addDays,
  addWeeks,
  differenceInMinutes,
  format,
  isAfter,
  isToday,
  startOfWeek,
} from "date-fns";
import { Ionicons } from "@expo/vector-icons";
import {
  AddClassModal,
  AddTaskModal,
  DatePickerModal,
  EditClassModal,
  GlassHeader,
  SemesterSelector,
  SwipeableClassCard,
  TimePickerModal,
  WeekView,
  EmptyStateCard,
} from "../components";
import { useTimetableStore } from "../store/timetableStore";
import { useShallow } from "zustand/react/shallow";
import { ThemePalette, useThemedColors } from "../theme/useTheme";

import { AttendanceStatus, ClassSchedule, DayOfWeek, Semester, WeekDay } from "../types";

const HomeScreen: React.FC = () => {
  const scrollY = useRef(new Animated.Value(0)).current;
  const insets = useSafeAreaInsets();
  const { colors, isDark } = useThemedColors();
  const styles = useMemo(() => createStyles(colors), [colors]);

  const {
    todaysClasses,
    selectedDayClasses,
    semesters,
    courses,
    isLoading,
    isInitialized,
    initializeApp,
    setCurrentSemester,
    markAttendance,
    shiftClass,
    addClass,
    addTask,
    updateClass,
    deleteClass,
    unshiftClass,
    loadTodaysClasses,
    loadClassesForDay,
    removeAttendance,
  } = useTimetableStore(
    useShallow((state) => ({
      todaysClasses: state.todaysClasses,
      selectedDayClasses: state.selectedDayClasses,
      semesters: state.semesters,
      courses: state.courses,
      isLoading: state.isLoading,
      isInitialized: state.isInitialized,
      initializeApp: state.initializeApp,
      setCurrentSemester: state.setCurrentSemester,
      markAttendance: state.markAttendance,
      shiftClass: state.shiftClass,
      addClass: state.addClass,
      addTask: state.addTask,
      updateClass: state.updateClass,
      deleteClass: state.deleteClass,
      unshiftClass: state.unshiftClass,
      loadTodaysClasses: state.loadTodaysClasses,
      loadClassesForDay: state.loadClassesForDay,
      removeAttendance: state.removeAttendance,
    }))
  );

  const [currentTime, setCurrentTime] = useState(new Date());
  const [selectedDay, setSelectedDay] = useState<DayOfWeek | "">("");
  const [weekOffset, setWeekOffset] = useState(0);
  const [showAddModal, setShowAddModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [showAttendanceModal, setShowAttendanceModal] = useState(false);
  const [showShiftModal, setShowShiftModal] = useState(false);
  const [selectedClass, setSelectedClass] = useState<
    (ClassSchedule & { attended?: boolean; attendanceStatus?: string; attendanceId?: string }) | null
  >(null);
  const [editingClass, setEditingClass] = useState<ClassSchedule | null>(null);
  const [shiftDate, setShiftDate] = useState("");
  const [shiftStartHours, setShiftStartHours] = useState("08");
  const [shiftStartMinutes, setShiftStartMinutes] = useState("00");
  const [shiftEndHours, setShiftEndHours] = useState("09");
  const [shiftEndMinutes, setShiftEndMinutes] = useState("00");
  const [shiftVenue, setShiftVenue] = useState("");
  const [showTimePickerModal, setShowTimePickerModal] = useState(false);
  const [timePickerType, setTimePickerType] = useState<"start" | "end">("start");
  const [showDatePickerModal, setShowDatePickerModal] = useState(false);
  const [showAddTaskModal, setShowAddTaskModal] = useState(false);

  useEffect(() => {
    if (!isInitialized) {
      initializeApp();
    }
  }, [isInitialized, initializeApp]);

  useEffect(() => {
    // Update every minute (60000ms) to avoid unnecessary React re-renders every second
    const timer = setInterval(() => setCurrentTime(new Date()), 60000);
    return () => clearInterval(timer);
  }, []);

  useEffect(() => {
    if (!isInitialized) return;
    loadTodaysClasses();
  }, [isInitialized, loadTodaysClasses]);

  const getWeekDays = (offset: number = 0): WeekDay[] => {
    const today = new Date();
    const currentWeekStart = startOfWeek(today, { weekStartsOn: 0 });
    const targetWeekStart = addWeeks(currentWeekStart, offset);
    const dayNames: DayOfWeek[] = ["SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"];

    const days: WeekDay[] = [];
    for (let i = 0; i < 7; i++) {
      const date = addDays(targetWeekStart, i);
      days.push({
        day: dayNames[i],
        date: date.getDate(),
        fullDate: format(date, "yyyy-MM-dd"),
        isToday: isToday(date),
      });
    }
    return days;
  };

  const weekDays = useMemo(() => getWeekDays(weekOffset), [weekOffset, currentTime]);
  const todayDayName = useMemo(() => format(currentTime, "EEE").toUpperCase() as DayOfWeek, [currentTime]);

  useEffect(() => {
    if (!selectedDay) {
      setSelectedDay(todayDayName);
    }
  }, [selectedDay, todayDayName]);

  const selectedDateStr = useMemo(() => {
    const match = weekDays.find((d) => d.day === selectedDay);
    return match?.fullDate || weekDays[0]?.fullDate || format(new Date(), "yyyy-MM-dd");
  }, [weekDays, selectedDay]);

  useEffect(() => {
    if (showShiftModal && selectedClass) {
      if (selectedClass.startTime && selectedClass.startTime.includes(":")) {
        const [h, m] = selectedClass.startTime.split(":");
        setShiftStartHours(h);
        setShiftStartMinutes(m);
      }
      if (selectedClass.endTime && selectedClass.endTime.includes(":")) {
        const [h, m] = selectedClass.endTime.split(":");
        setShiftEndHours(h);
        setShiftEndMinutes(m);
      }
      if (selectedClass.room) {
        setShiftVenue(selectedClass.room);
      }
    }
  }, [showShiftModal, selectedClass]);

  useEffect(() => {
    if (!isInitialized) return;
    if (selectedDay && selectedDateStr) {
      loadClassesForDay(selectedDay, selectedDateStr);
    }
  }, [isInitialized, selectedDay, selectedDateStr, loadClassesForDay]);

  const isViewingToday = selectedDay === todayDayName && weekOffset === 0;

  const getWeekLabel = (): string => {
    if (weekOffset === 0) return "This Week";
    if (weekOffset === -1) return "Last Week";
    if (weekOffset === 1) return "Next Week";
    if (weekOffset < 0) return `${Math.abs(weekOffset)} Weeks Ago`;
    return `In ${weekOffset} Weeks`;
  };

  const handleWeekChange = (direction: "prev" | "next") => {

    setWeekOffset((prev) => (direction === "prev" ? prev - 1 : prev + 1));
  };

  const handleGoToThisWeek = () => {
    setWeekOffset(0);
    setSelectedDay(todayDayName);
  };

  const handleDayChange = (direction: "prev" | "next") => {
    const currentIndex = weekDays.findIndex((d) => d.day === selectedDay);
    if (currentIndex === -1) return;

    if (direction === "prev") {
      if (currentIndex > 0) {
        setSelectedDay(weekDays[currentIndex - 1].day as DayOfWeek);
      } else {
        setWeekOffset((prev) => prev - 1);
        setSelectedDay("SAT");
      }
    } else {
      if (currentIndex < weekDays.length - 1) {
        setSelectedDay(weekDays[currentIndex + 1].day as DayOfWeek);
      } else {
        setWeekOffset((prev) => prev + 1);
        setSelectedDay("SUN");
      }
    }
  };

  const swipeGesture = useMemo(() => {
    // Native UX alignment: Swiping Left goes to next day, Swiping Right goes to previous day
    const flingLeft = Gesture.Fling()
      .direction(Directions.LEFT)
      .runOnJS(true)
      .onStart(() => {
        handleDayChange("next");
      });

    const flingRight = Gesture.Fling()
      .direction(Directions.RIGHT)
      .runOnJS(true)
      .onStart(() => {
        handleDayChange("prev");
      });

    return Gesture.Race(flingLeft, flingRight);
  }, [weekDays, selectedDay]);

  const currentSemester = semesters.find((s) => s.isActive) || semesters[0];

  const getNextClass = useMemo(() => {
    if (!isViewingToday || selectedDayClasses.length === 0) return null;

    const now = currentTime;
    for (const classItem of selectedDayClasses) {
      const [hours, minutes] = classItem.startTime.split(":").map(Number);
      const classStartTime = new Date(now);
      classStartTime.setHours(hours, minutes, 0, 0);

      if (isAfter(classStartTime, now)) {
        return classItem;
      }
    }
    return null;
  }, [selectedDayClasses, currentTime, isViewingToday]);

  const displayClasses = selectedDayClasses;

  const handleSemesterSelect = async (semester: Semester) => {
    await setCurrentSemester(semester.id);
  };

  const handleOpenAttendanceModal = (classItem: ClassSchedule) => {
    setSelectedClass(classItem);
    setShowAttendanceModal(true);
  };

  const handleMarkAttendance = async (status: AttendanceStatus) => {
    if (!selectedClass) return;
    await markAttendance(selectedClass.id, selectedClass.courseId, selectedDateStr, status);
    setShowAttendanceModal(false);
    setSelectedClass(null);
    const selectedDayInfo = weekDays.find((d) => d.day === selectedDay);
    if (selectedDayInfo) {
      await loadClassesForDay(selectedDay, selectedDayInfo.fullDate);
    }
  };
  const handleRemoveAttendance = async () => {
    if (!selectedClass || !selectedClass.attendanceId) return;
    await removeAttendance(selectedClass.attendanceId);
    setShowAttendanceModal(false);
    setSelectedClass(null);
    const selectedDayInfo = weekDays.find((d) => d.day === selectedDay);
    if (selectedDayInfo) {
      await loadClassesForDay(selectedDay, selectedDayInfo.fullDate);
    }
  };
  const handleShiftClass = async () => {
    if (!selectedClass || !shiftDate) {
      Alert.alert("Error", "Please select a date to shift to");
      return;
    }

    const shiftStartTime = `${shiftStartHours}:${shiftStartMinutes}`;
    const shiftEndTime = `${shiftEndHours}:${shiftEndMinutes}`;

    try {
      await shiftClass(
        selectedClass.id,
        selectedClass.courseId,
        selectedDateStr,
        shiftDate,
        shiftStartTime,
        shiftEndTime,
        shiftVenue || selectedClass.room,
        selectedClass.isShifted,
        selectedClass.originalClassId,
        selectedClass.originalDate,
      );
      setShowShiftModal(false);
      setSelectedClass(null);
      setShiftDate("");
      setShiftStartHours("08");
      setShiftStartMinutes("00");
      setShiftEndHours("09");
      setShiftEndMinutes("00");
      setShiftVenue("");
      const selectedDayInfo = weekDays.find((d) => d.day === selectedDay);
      if (selectedDayInfo) {
        await loadClassesForDay(selectedDay, selectedDayInfo.fullDate);
      }
      Alert.alert("Success", `Class shifted to ${shiftDate}`);
    } catch (error) {
      Alert.alert("Error", "Failed to shift class");
    }
  };

  const handleAddClass = async (classData: any) => {
    try {
      await addClass(classData);
      setShowAddModal(false);
      const selectedDayInfo = weekDays.find((d) => d.day === selectedDay);
      if (selectedDayInfo) {
        await loadClassesForDay(selectedDay, selectedDayInfo.fullDate);
      }
      await loadTodaysClasses();
    } catch (error) {
      const message = error instanceof Error ? error.message : "Failed to add class";
      Alert.alert("Add Class", message);
    }
  };

  const handleUpdateClass = async (classData: any) => {
    try {
      if (!editingClass) return;
      await updateClass(editingClass.id, classData);
      setShowEditModal(false);
      setEditingClass(null);
      const selectedDayInfo = weekDays.find((d) => d.day === selectedDay);
      if (selectedDayInfo) {
        await loadClassesForDay(selectedDay, selectedDayInfo.fullDate);
      }
      Alert.alert("Success", "Class updated successfully");
    } catch (error) {
      const message = error instanceof Error ? error.message : "Failed to update class";
      Alert.alert("Update Class", message);
    }
  };

  const handleDeleteClass = (classItem: ClassSchedule) => {
    if (classItem.isShifted && classItem.originalClassId && classItem.originalDate) {
      Alert.alert(
        "Manage Shifted Class",
        "Would you like to restore this class to its original schedule or delete it completely?",
        [
          { text: "Cancel", style: "cancel" },
          {
            text: "Restore Original",
            onPress: async () => {
              try {
                await unshiftClass(classItem.id, classItem.originalClassId!, classItem.originalDate!);
                const selectedDayInfo = weekDays.find((d) => d.day === selectedDay);
                if (selectedDayInfo) {
                  await loadClassesForDay(selectedDay, selectedDayInfo.fullDate);
                }
              } catch (error) {
                Alert.alert("Error", "Failed to restore class");
              }
            },
          },
          {
            text: "Delete Entirely",
            style: "destructive",
            onPress: async () => {
              try {
                await unshiftClass(classItem.id, classItem.originalClassId!, classItem.originalDate!);
                await deleteClass(classItem.originalClassId!);
                const selectedDayInfo = weekDays.find((d) => d.day === selectedDay);
                if (selectedDayInfo) {
                  await loadClassesForDay(selectedDay, selectedDayInfo.fullDate);
                }
              } catch (error) {
                Alert.alert("Error", "Failed to delete class");
              }
            },
          },
        ],
      );
      return;
    }

    Alert.alert(
      "Delete Class",
      `Delete ${classItem.courseName || classItem.shortName} on ${classItem.dayOfWeek}?`,
      [
        { text: "Cancel", style: "cancel" },
        {
          text: "Delete",
          style: "destructive",
          onPress: async () => {
            try {
              await deleteClass(classItem.id);
              const selectedDayInfo = weekDays.find((d) => d.day === selectedDay);
              if (selectedDayInfo) {
                await loadClassesForDay(selectedDay, selectedDayInfo.fullDate);
              }
            } catch (error) {
              Alert.alert("Error", "Failed to delete class");
            }
          },
        },
      ],
    );
  };

  const handleEditClass = (classItem: ClassSchedule) => {
    setEditingClass(classItem);
    setShowEditModal(true);
  };

  const handleAddTask = async (taskData: Parameters<typeof addTask>[0]) => {
    try {
      await addTask(taskData);
      setShowAddTaskModal(false);
    } catch (error) {
      const message = error instanceof Error ? error.message : "Failed to add task";
      Alert.alert("Add Task", message);
    }
  };

  if (!isInitialized || isLoading) {
    return (
      <LinearGradient
        colors={[colors.background.start, colors.background.end]}
        style={styles.loadingContainer}
      >
        <ActivityIndicator size="large" color={colors.primary.teal} />
        <Text style={styles.loadingText}>Loading...</Text>
      </LinearGradient>
    );
  }

  return (
    <GestureHandlerRootView style={{ flex: 1 }}>
      <LinearGradient
        colors={[colors.background.start, colors.background.end]}
        style={styles.container}
      >
        <BackgroundMesh />
        <GlassHeader
          title="Today"
          scrollY={scrollY}
          subtitle={format(currentTime, "EEEE, MMMM d")}
          rightComponent={
            currentSemester && (
              <View style={{
                paddingHorizontal: 14,
                paddingVertical: 6,
                borderRadius: 12,
                backgroundColor: colors.glass.cardFill,
                borderWidth: StyleSheet.hairlineWidth,
                borderColor: colors.glass.cardBorder,
                alignItems: 'center',
                justifyContent: 'center',
                minWidth: 60,
              }}>
                <Text style={{ color: colors.text.primary, fontWeight: '600', fontSize: 15 }}>
                  {currentSemester.name}
                </Text>
              </View>
            )
          }
        />

        <GestureDetector gesture={swipeGesture}>
          <Animated.ScrollView
          style={styles.scrollView}
          contentContainerStyle={[
            styles.scrollContent,
            { paddingBottom: insets.bottom + 130 },
          ]}
          showsVerticalScrollIndicator={false}
          scrollEventThrottle={16}
          onScroll={Animated.event(
            [{ nativeEvent: { contentOffset: { y: scrollY } } }],
            { useNativeDriver: true }
          )}
        >
          {/* Quick Actions removed as requested */}
          {/* Week View */}
          <WeekView
            days={weekDays}
            selectedDay={selectedDay || todayDayName}
            onDaySelect={(day) => setSelectedDay(day as DayOfWeek)}
            weekLabel={getWeekLabel()}
            weekOffset={weekOffset}
            onWeekChange={handleWeekChange}
            onGoToThisWeek={handleGoToThisWeek}
          />



          {/* Classes Header */}
          <View style={styles.sectionHeader}>
            <View style={styles.sectionLeft}>
              <Text style={styles.sectionTitle}>
                {isViewingToday ? "Today's Classes" : `${selectedDay}`}
              </Text>
              <View style={styles.classBadge}>
                <Text style={styles.classBadgeText}>{displayClasses.length}</Text>
              </View>
            </View>
            <TouchableOpacity onPress={() => setShowAddModal(true)} style={styles.addButton}>
              <Ionicons name="add" size={22} color={colors.primary.teal} />
            </TouchableOpacity>
          </View>

          {/* Classes List */}
          {semesters.length === 0 ? (
            <EmptyStateCard
              icon="school-outline"
              title="Welcome to UniTimetable!"
              description="Start by adding a semester in Settings"
            />
          ) : courses.length === 0 ? (
            <EmptyStateCard
              icon="book-outline"
              title="No courses yet"
              description="Add your courses in My Classes tab"
            />
          ) : displayClasses.length > 0 ? (
            displayClasses.map((classItem) => (
              <SwipeableClassCard
                key={classItem.id}
                classItem={classItem}
                selectedDateStr={selectedDateStr}
                isNextClass={isViewingToday && getNextClass?.id === classItem.id}
                onMarkAttendance={async (status) => {
                  if (status === "shifted") {
                    const [startH, startM] = classItem.startTime.split(":");
                    const [endH, endM] = classItem.endTime.split(":");
                    setShiftStartHours(startH || "08");
                    setShiftStartMinutes(startM?.substring(0, 2) || "00");
                    setShiftEndHours(endH || "09");
                    setShiftEndMinutes(endM?.substring(0, 2) || "00");
                    setShiftVenue(classItem.room || "");
                    setSelectedClass(classItem);
                    setShowShiftModal(true);
                    return;
                  }

                  await markAttendance(classItem.id, classItem.courseId, selectedDateStr, status);

                  const selectedDayInfo = weekDays.find((d) => d.day === selectedDay);
                  if (selectedDayInfo) {
                    await loadClassesForDay(selectedDay, selectedDayInfo.fullDate);
                  }
                }}
                onOpenModal={() => handleOpenAttendanceModal(classItem)}
                onEdit={() => handleEditClass(classItem)}
              />
            ))
          ) : (
            <EmptyStateCard
              icon="calendar-outline"
              title={isViewingToday ? "No classes today!" : `No classes on ${selectedDay}`}
              description={isViewingToday ? "Enjoy your day off 🎉" : "This day is free"}
              buttonText="Add a class"
              onPress={() => setShowAddModal(true)}
            />
          )}
        </Animated.ScrollView>
        </GestureDetector>

        {/* Add Class Modal */}
        <AddClassModal
          visible={showAddModal}
          onClose={() => setShowAddModal(false)}
          onSubmit={handleAddClass}
          courses={courses}
          semesterId={currentSemester?.id || semesters[0]?.id || ""}
        />

        {/* Edit Class Modal */}
        <EditClassModal
          visible={showEditModal}
          onClose={() => {
            setShowEditModal(false);
            setEditingClass(null);
          }}
          onSubmit={handleUpdateClass}
          onDelete={handleDeleteClass}
          classItem={editingClass}
          courses={courses}
        />

        {/* Add Task Modal */}
        <AddTaskModal
          visible={showAddTaskModal}
          onClose={() => setShowAddTaskModal(false)}
          onSubmit={handleAddTask}
          courses={courses}
        />

        {/* Attendance Options Modal */}
        <Modal
          visible={showAttendanceModal}
          transparent
          animationType="fade"
          onRequestClose={() => setShowAttendanceModal(false)}
        >
          <View style={styles.modalOverlay}>
            <BlurView intensity={60} tint={colors.glass.tint} style={styles.attendanceModalBlur}>
              <LinearGradient
                colors={
                  colors.mode === "dark"
                    ? ["rgba(255, 255, 255, 0.12)", "rgba(255, 255, 255, 0.02)"]
                    : ["rgba(0, 0, 0, 0.07)", "rgba(0, 0, 0, 0.01)"]
                }
                style={styles.attendanceModalContent}
              >
                <View style={styles.attendanceModalHeader}>
                  <View style={{ flex: 1, alignItems: "flex-start" }}>
                    <Text style={styles.attendanceModalTitle}>
                      {selectedClass?.attended ? "Edit Attendance" : "Mark Attendance"}
                    </Text>
                    <Text style={styles.attendanceModalSubtitle}>
                      {selectedClass?.shortName || selectedClass?.courseName}
                    </Text>
                  </View>
                  <TouchableOpacity
                    style={styles.modalDeleteBtn}
                    onPress={() => {
                      if (selectedClass) {
                        setShowAttendanceModal(false);
                        handleDeleteClass(selectedClass);
                      }
                    }}
                  >
                    <Ionicons name="trash-outline" size={20} color={colors.status.danger} />
                  </TouchableOpacity>
                </View>

                <View style={styles.attendanceOptionsGrid}>
                  <TouchableOpacity
                    style={[
                      styles.attendanceOption,
                      selectedClass?.attendanceStatus === "present" && styles.attendanceOptionSelected,
                    ]}
                    onPress={() => handleMarkAttendance("present")}
                  >
                    <LinearGradient colors={["#22c55e", "#16a34a"]} style={styles.attendanceOptionIcon}>
                      <Ionicons name="checkmark" size={24} color="#fff" />
                    </LinearGradient>
                    <Text style={styles.attendanceOptionText}>Present</Text>
                  </TouchableOpacity>

                  <TouchableOpacity
                    style={[
                      styles.attendanceOption,
                      selectedClass?.attendanceStatus === "absent" && styles.attendanceOptionSelected,
                    ]}
                    onPress={() => handleMarkAttendance("absent")}
                  >
                    <LinearGradient colors={["#ef4444", "#dc2626"]} style={styles.attendanceOptionIcon}>
                      <Ionicons name="close" size={24} color="#fff" />
                    </LinearGradient>
                    <Text style={styles.attendanceOptionText}>Absent</Text>
                  </TouchableOpacity>

                  <TouchableOpacity
                    style={[
                      styles.attendanceOption,
                      selectedClass?.attendanceStatus === "canceled" && styles.attendanceOptionSelected,
                    ]}
                    onPress={() => handleMarkAttendance("canceled")}
                  >
                    <LinearGradient colors={["#f59e0b", "#d97706"]} style={styles.attendanceOptionIcon}>
                      <Ionicons name="ban-outline" size={24} color="#fff" />
                    </LinearGradient>
                    <Text style={styles.attendanceOptionText}>Canceled</Text>
                  </TouchableOpacity>

                  <TouchableOpacity
                    style={[
                      styles.attendanceOption,
                      selectedClass?.attendanceStatus === "shifted" && styles.attendanceOptionSelected,
                    ]}
                    onPress={() => {
                      setShowAttendanceModal(false);
                      setShowShiftModal(true);
                    }}
                  >
                    <LinearGradient colors={["#8b5cf6", "#7c3aed"]} style={styles.attendanceOptionIcon}>
                      <Ionicons name="swap-horizontal" size={24} color="#fff" />
                    </LinearGradient>
                    <Text style={styles.attendanceOptionText}>Shift</Text>
                  </TouchableOpacity>
                </View>

                <View style={styles.attendanceActionsRow}>
                  <TouchableOpacity
                    style={[styles.actionBtn, styles.actionBtnCancel]}
                    onPress={() => {
                      setShowAttendanceModal(false);
                      setSelectedClass(null);
                    }}
                  >
                    <Text style={styles.actionBtnCancelText}>Cancel</Text>
                  </TouchableOpacity>

                  {selectedClass?.attended && (
                    <TouchableOpacity
                      style={[styles.actionBtn, styles.actionBtnReset]}
                      onPress={handleRemoveAttendance}
                    >
                      <Ionicons name="trash-outline" size={16} color={colors.status.danger} />
                      <Text style={styles.actionBtnResetText}>Clear Mark</Text>
                    </TouchableOpacity>
                  )}
                </View>
              </LinearGradient>
            </BlurView>
          </View>
        </Modal>

        {/* Shift Class Modal */}
        <Modal
          visible={showShiftModal}
          transparent
          animationType="fade"
          onRequestClose={() => setShowShiftModal(false)}
        >
          <View style={styles.modalOverlay}>
            <BlurView intensity={40} tint={colors.glass.tint} style={styles.shiftModalBlur}>
              <ScrollView
                style={styles.shiftModalScroll}
                contentContainerStyle={styles.shiftModalScrollContent}
                showsVerticalScrollIndicator={false}
                bounces={false}
              >
                <LinearGradient
                  colors={["rgba(255, 255, 255, 0.15)", "rgba(255, 255, 255, 0.05)"]}
                  style={styles.shiftModalContent}
                >
                  <Text style={styles.shiftModalTitle}>Shift Class</Text>
                  <Text style={styles.shiftModalSubtitle}>
                    {selectedClass?.shortName || selectedClass?.courseName}
                  </Text>

                  {/* Date Selection */}
                  <View style={styles.shiftInputContainer}>
                    <Text style={styles.shiftInputLabel}>Shift to date:</Text>
                    <TouchableOpacity style={styles.shiftInput} onPress={() => setShowDatePickerModal(true)}>
                      <Text style={shiftDate ? styles.shiftInputText : styles.shiftInputPlaceholder}>
                        {shiftDate || "Select Date"}
                      </Text>
                      <Ionicons name="calendar-outline" size={20} color={colors.text.muted} />
                    </TouchableOpacity>
                  </View>

                  <View style={styles.shiftQuickDates}>
                    <TouchableOpacity
                      style={[
                        styles.shiftQuickDate,
                        shiftDate === format(addDays(new Date(), 1), "yyyy-MM-dd") &&
                          styles.shiftQuickDateActive,
                      ]}
                      onPress={() => setShiftDate(format(addDays(new Date(), 1), "yyyy-MM-dd"))}
                    >
                      <Text
                        style={[
                          styles.shiftQuickDateText,
                          shiftDate === format(addDays(new Date(), 1), "yyyy-MM-dd") &&
                            styles.shiftQuickDateTextActive,
                        ]}
                      >
                        Tomorrow
                      </Text>
                    </TouchableOpacity>
                    <TouchableOpacity
                      style={[
                        styles.shiftQuickDate,
                        shiftDate === format(addWeeks(new Date(), 1), "yyyy-MM-dd") &&
                          styles.shiftQuickDateActive,
                      ]}
                      onPress={() => setShiftDate(format(addWeeks(new Date(), 1), "yyyy-MM-dd"))}
                    >
                      <Text
                        style={[
                          styles.shiftQuickDateText,
                          shiftDate === format(addWeeks(new Date(), 1), "yyyy-MM-dd") &&
                            styles.shiftQuickDateTextActive,
                        ]}
                      >
                        Next Week
                      </Text>
                    </TouchableOpacity>
                  </View>

                  {/* Time Selection */}
                  <View style={styles.shiftTimeRow}>
                    <View style={styles.shiftTimeInputContainer}>
                      <Text style={styles.shiftInputLabel}>Start Time</Text>
                      <TouchableOpacity
                        style={styles.shiftTimeDisplay}
                        onPress={() => {
                          setTimePickerType("start");
                          setShowTimePickerModal(true);
                        }}
                      >
                        <Text style={styles.shiftTimeDisplayText}>
                          {shiftStartHours}:{shiftStartMinutes}
                        </Text>
                      </TouchableOpacity>
                    </View>
                    <View style={styles.shiftTimeInputContainer}>
                      <Text style={styles.shiftInputLabel}>End Time</Text>
                      <TouchableOpacity
                        style={styles.shiftTimeDisplay}
                        onPress={() => {
                          setTimePickerType("end");
                          setShowTimePickerModal(true);
                        }}
                      >
                        <Text style={styles.shiftTimeDisplayText}>
                          {shiftEndHours}:{shiftEndMinutes}
                        </Text>
                      </TouchableOpacity>
                    </View>
                  </View>

                  {/* Venue Selection */}
                  <View style={styles.shiftInputContainer}>
                    <Text style={styles.shiftInputLabel}>Venue / Room</Text>
                    <TextInput
                      style={styles.shiftInput}
                      placeholder="e.g. LH-101, CS Lab"
                      placeholderTextColor={colors.text.muted}
                      value={shiftVenue}
                      onChangeText={setShiftVenue}
                    />
                  </View>

                  {/* Quick Venue Options */}
                  <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.shiftVenueQuickContainer}>
                    {[
                      "LH-101",
                      "LH-203",
                      "LH-105",
                      "CS-Lab 1",
                      "PH-201",
                      "PH-202",
                      "Library",
                      "Online",
                    ].map((venue, index) => (
                      <TouchableOpacity
                        key={index}
                        style={[styles.shiftVenueQuick, shiftVenue === venue && styles.shiftVenueQuickActive]}
                        onPress={() => setShiftVenue(venue)}
                      >
                        <Text
                          style={[styles.shiftVenueQuickText, shiftVenue === venue && styles.shiftVenueQuickTextActive]}
                        >
                          {venue}
                        </Text>
                      </TouchableOpacity>
                    ))}
                  </ScrollView>

                  {/* Quick time presets */}
                  <View style={styles.shiftTimeQuickContainer}>
                    <ScrollView horizontal showsHorizontalScrollIndicator={false}>
                      {[
                        { label: "08:00-09:00", start: "08", end: "09" },
                        { label: "09:00-10:00", start: "09", end: "10" },
                        { label: "10:00-11:00", start: "10", end: "11" },
                        { label: "11:00-12:00", start: "11", end: "12" },
                        { label: "12:00-13:00", start: "12", end: "13" },
                        { label: "13:00-14:00", start: "13", end: "14" },
                      ].map((slot, index) => (
                        <TouchableOpacity
                          key={index}
                          style={[
                            styles.shiftTimeQuick,
                            shiftStartHours === slot.start && shiftEndHours === slot.end && styles.shiftTimeQuickActive,
                          ]}
                          onPress={() => {
                            setShiftStartHours(slot.start);
                            setShiftStartMinutes("00");
                            setShiftEndHours(slot.end);
                            setShiftEndMinutes("00");
                          }}
                        >
                          <Text
                            style={[
                              styles.shiftTimeQuickText,
                              shiftStartHours === slot.start &&
                                shiftEndHours === slot.end &&
                                styles.shiftTimeQuickTextActive,
                            ]}
                          >
                            {slot.label}
                          </Text>
                        </TouchableOpacity>
                      ))}
                    </ScrollView>
                  </View>

                  <View style={styles.shiftModalButtons}>
                    <TouchableOpacity
                      style={styles.shiftModalCancelBtn}
                      onPress={() => {
                        setShowShiftModal(false);
                        setShiftDate("");
                        setShiftVenue("");
                      }}
                    >
                      <Text style={styles.shiftModalCancelText}>Cancel</Text>
                    </TouchableOpacity>

                    <TouchableOpacity
                      style={styles.shiftModalConfirmBtn}
                      onPress={handleShiftClass}
                      disabled={!shiftDate}
                    >
                      <LinearGradient
                        colors={[colors.primary.teal, `${colors.primary.teal}cc`]}
                        style={{ paddingHorizontal: 18, paddingVertical: 10, borderRadius: 16 }}
                      >
                        <Text style={styles.shiftModalConfirmText}>Confirm Shift</Text>
                      </LinearGradient>
                    </TouchableOpacity>
                  </View>
                </LinearGradient>
              </ScrollView>
            </BlurView>
          </View>
        </Modal>

        {/* Time Picker Modal */}
        <TimePickerModal
          visible={showTimePickerModal}
          title={timePickerType === "start" ? "Select Start Time" : "Select End Time"}
          initialTime={
            timePickerType === "start"
              ? `${shiftStartHours}:${shiftStartMinutes}`
              : `${shiftEndHours}:${shiftEndMinutes}`
          }
          onConfirm={(time) => {
            const [h, m] = time.split(":");
            if (timePickerType === "start") {
              setShiftStartHours(h);
              setShiftStartMinutes(m);
            } else {
              setShiftEndHours(h);
              setShiftEndMinutes(m);
            }
            setShowTimePickerModal(false);
          }}
          onCancel={() => setShowTimePickerModal(false)}
        />

        {/* Date Picker Modal */}
        <DatePickerModal
          visible={showDatePickerModal}
          title="Select Date"
          initialDate={shiftDate || format(new Date(), "yyyy-MM-dd")}
          onConfirm={(date) => {
            setShiftDate(date);
            setShowDatePickerModal(false);
          }}
          onCancel={() => setShowDatePickerModal(false)}
          minDate={new Date()}
        />

        {/* Floating Today Button */}
        {!isViewingToday && (
          <View style={[styles.floatingButtonContainer, { bottom: insets.bottom + 100 }]}>
            <TouchableOpacity onPress={handleGoToThisWeek} activeOpacity={0.8} style={styles.floatingButtonWrapper}>
              <View style={styles.floatingButtonBlur}>
                <BlurView intensity={isDark ? 50 : 80} tint={isDark ? "dark" : "light"} style={StyleSheet.absoluteFill} />
                <LinearGradient
                  colors={
                    isDark
                      ? ["rgba(255, 255, 255, 0.2)", "rgba(255, 255, 255, 0.05)"]
                      : ["rgba(0, 0, 0, 0.1)", "rgba(0, 0, 0, 0.02)"]
                  }
                  start={{ x: 0, y: 0 }}
                  end={{ x: 1, y: 1 }}
                  style={styles.floatingButtonGradientBorder}
                >
                  <View style={[styles.floatingButtonInner, { backgroundColor: isDark ? "rgba(255, 255, 255, 0.05)" : "rgba(255, 255, 255, 0.5)" }]}>
                    <Ionicons name="refresh" size={14} color={colors.text.primary} style={{ marginRight: 4 }} />
                    <Text style={[styles.floatingButtonText, { color: colors.text.primary }]}>Today</Text>
                  </View>
                </LinearGradient>
              </View>
            </TouchableOpacity>
          </View>
        )}
      </LinearGradient>
    </GestureHandlerRootView>
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
  loadingText: {
    color: colors.text.secondary,
    marginTop: 16,
    fontSize: 16,
  },
  scrollView: {
    flex: 1,
  },
  scrollContent: {
    paddingTop: 12,
  },
  quickRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    paddingHorizontal: 16,
    marginBottom: 12,
    gap: 10,
  },
  quickCard: {
    flex: 1,
    borderRadius: 14,
    padding: 14,
    borderWidth: 1,
    backgroundColor: colors.glass.light,
  },
  quickIcon: {
    width: 36,
    height: 36,
    borderRadius: 12,
    alignItems: "center",
    justifyContent: "center",
    marginBottom: 8,
  },
  quickTitle: {
    color: colors.text.primary,
    fontSize: 14,
    fontWeight: "700",
  },
  quickSubtitle: {
    color: colors.text.muted,
    fontSize: 12,
    marginTop: 2,
  },
  sectionHeader: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    paddingHorizontal: 20,
    marginTop: 18,
    marginBottom: 10,
  },
  sectionLeft: {
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
  },
  sectionTitle: {
    color: colors.text.primary,
    fontSize: 18,
    fontWeight: "600",
    letterSpacing: -0.2,
  },
  classBadge: {
    backgroundColor: colors.glass.cardFill,
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 8,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: colors.glass.cardBorder,
  },
  classBadgeText: {
    color: colors.primary.teal,
    fontSize: 13,
    fontWeight: "700",
  },
  classCard: {
    marginHorizontal: 16,
    marginVertical: 8,
    borderRadius: 16,
    overflow: "hidden",
    shadowColor: colors.shadow,
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.2,
    shadowRadius: 8,
    // Removed elevation to fix transparent glass on Android
  },
  cardBlur: {
    borderRadius: 16,
  },
  cardGradient: {
    padding: 16,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: colors.glass.border,
  },
  attendedCard: {
    opacity: 0.95,
  },
  cardContent: {
    flexDirection: "row",
    alignItems: "center",
  },
  colorBar: {
    width: 6,
    height: "100%",
    borderRadius: 6,
    marginRight: 12,
  },
  cardMain: {
    flex: 1,
  },
  cardTop: {
    flexDirection: "row",
    justifyContent: "space-between",
    marginBottom: 8,
  },
  timeContainer: {
    flexDirection: "row",
    alignItems: "center",
    gap: 6,
  },
  roomContainer: {
    flexDirection: "row",
    alignItems: "center",
    gap: 6,
  },
  timeText: {
    color: colors.text.secondary,
    fontSize: 13,
    fontWeight: "600",
  },
  roomText: {
    color: colors.text.secondary,
    fontSize: 13,
    fontWeight: "600",
  },
  className: {
    color: colors.text.primary,
    fontSize: 18,
    fontWeight: "700",
    marginBottom: 6,
  },
  professorRow: {
    flexDirection: "row",
    alignItems: "center",
    gap: 6,
    marginBottom: 8,
  },
  professorText: {
    color: colors.text.muted,
    fontSize: 13,
  },
  cardBottom: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
  },
  attendedBadge: {
    flexDirection: "row",
    alignItems: "center",
    gap: 6,
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 12,
  },
  attendedText: {
    color: colors.text.primary,
    fontWeight: "700",
    fontSize: 13,
  },
  attendButton: {
    backgroundColor: colors.glass.light,
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: colors.glass.border,
  },
  attendButtonText: {
    color: colors.text.primary,
    fontWeight: "700",
    fontSize: 13,
  },
  upcomingBadge: {
    backgroundColor: colors.glass.light,
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 12,
  },
  upcomingText: {
    color: colors.text.secondary,
    fontWeight: "600",
    fontSize: 12,
  },

  addButton: {
    width: 38,
    height: 38,
    borderRadius: 12,
    backgroundColor: colors.glass.cardFill,
    alignItems: "center",
    justifyContent: "center",
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: colors.glass.cardBorder,
  },
  emptyState: {
    alignItems: "center",
    paddingVertical: 48,
    paddingHorizontal: 24,
  },
  emptyIcon: {
    marginBottom: 16,
    opacity: 0.35,
  },
  emptyText: {
    color: colors.text.primary,
    fontSize: 18,
    fontWeight: "600",
    marginBottom: 6,
    letterSpacing: 0.1,
  },
  emptySubtext: {
    color: colors.text.muted,
    fontSize: 14,
    marginBottom: 20,
    textAlign: "center",
  },
  emptyButton: {
    backgroundColor: colors.glass.cardFill,
    paddingHorizontal: 22,
    paddingVertical: 11,
    borderRadius: 14,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: colors.primary.teal + '40',
  },
  emptyButtonText: {
    color: colors.primary.teal,
    fontSize: 14,
    fontWeight: "600",
  },
  // Attendance Modal Styles
  modalOverlay: {
    flex: 1,
    backgroundColor: "rgba(0, 0, 0, 0.65)",
    justifyContent: "center",
    alignItems: "center",
    padding: 20,
  },
  attendanceModalBlur: {
    borderRadius: 22,
    overflow: "hidden",
    width: "100%",
    maxWidth: 340,
  },
  attendanceModalContent: {
    padding: 24,
    borderRadius: 22,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: colors.glass.elevatedBorder,
    backgroundColor: colors.glass.elevatedFill,
  },
  attendanceModalHeader: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    marginBottom: 22,
    width: "100%",
  },
  modalDeleteBtn: {
    width: 38,
    height: 38,
    borderRadius: 12,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: colors.mode === "dark" ? "rgba(255, 69, 58, 0.12)" : "rgba(255, 59, 48, 0.08)",
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: colors.mode === "dark" ? "rgba(255, 69, 58, 0.3)" : "rgba(255, 59, 48, 0.2)",
  },
  attendanceModalTitle: {
    color: colors.text.primary,
    fontSize: 20,
    fontWeight: "700",
    letterSpacing: 0.1,
    marginBottom: 2,
  },
  attendanceModalSubtitle: {
    color: colors.text.secondary,
    fontSize: 14,
    fontWeight: "500",
  },
  attendanceOptionsGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    justifyContent: "space-between",
    marginBottom: 18,
  },
  attendanceOption: {
    width: "48%",
    alignItems: "center",
    padding: 14,
    marginBottom: 10,
    backgroundColor: colors.mode === "dark" ? "rgba(255, 255, 255, 0.03)" : "rgba(0, 0, 0, 0.02)",
    borderRadius: 16,
    borderWidth: 1,
    borderColor: colors.glass.cardBorder,
  },
  attendanceOptionSelected: {
    backgroundColor: colors.mode === "dark" ? "rgba(255, 255, 255, 0.08)" : "rgba(0, 0, 0, 0.04)",
    borderColor: colors.primary.teal,
    borderWidth: 1.5,
  },
  attendanceOptionIcon: {
    width: 44,
    height: 44,
    borderRadius: 16,
    alignItems: "center",
    justifyContent: "center",
    marginBottom: 8,
  },
  attendanceOptionText: {
    color: colors.text.primary,
    fontSize: 13,
    fontWeight: "600",
  },
  attendanceActionsRow: {
    flexDirection: "row",
    gap: 12,
    width: "100%",
    marginTop: 8,
  },
  actionBtn: {
    flex: 1,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    paddingVertical: 12,
    borderRadius: 12,
    borderWidth: 1,
    gap: 6,
  },
  actionBtnCancel: {
    backgroundColor: "transparent",
    borderColor: colors.glass.cardBorder,
  },
  actionBtnCancelText: {
    color: colors.text.secondary,
    fontSize: 14,
    fontWeight: "600",
  },
  actionBtnReset: {
    backgroundColor: colors.mode === "dark" ? "rgba(255, 69, 58, 0.1)" : "rgba(255, 59, 48, 0.05)",
    borderColor: colors.mode === "dark" ? "rgba(255, 69, 58, 0.3)" : "rgba(255, 59, 48, 0.15)",
  },
  actionBtnResetText: {
    color: colors.status.danger,
    fontSize: 14,
    fontWeight: "700",
  },
  // Shift Modal Styles
  shiftModalBlur: {
    borderRadius: 24,
    overflow: "hidden",
    width: "100%",
    maxWidth: 360,
    maxHeight: "85%",
  },
  shiftModalScroll: {
    maxHeight: "100%",
  },
  shiftModalScrollContent: {
    flexGrow: 1,
  },
  shiftModalContent: {
    padding: 24,
    borderRadius: 24,
    borderWidth: 1,
    borderColor: colors.glass.border,
  },
  shiftModalTitle: {
    color: colors.text.primary,
    fontSize: 22,
    fontWeight: "700",
    textAlign: "center",
    marginBottom: 4,
  },
  shiftModalSubtitle: {
    color: colors.text.secondary,
    fontSize: 16,
    textAlign: "center",
    marginBottom: 24,
  },
  shiftInputContainer: {
    marginBottom: 16,
  },
  shiftInputLabel: {
    color: colors.text.secondary,
    fontSize: 14,
    marginBottom: 8,
  },
  shiftInput: {
    backgroundColor: colors.glass.dark,
    borderRadius: 12,
    padding: 14,
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    borderWidth: 1,
    borderColor: colors.glass.border,
  },
  shiftInputText: {
    color: colors.text.primary,
    fontSize: 16,
  },
  shiftInputPlaceholder: {
    color: colors.text.muted,
    fontSize: 16,
  },
  wheelPickerRow: {
    alignItems: "center",
    marginBottom: 16,
  },
  shiftQuickDates: {
    flexDirection: "row",
    justifyContent: "space-between",
    marginBottom: 16,
  },
  shiftQuickDate: {
    flex: 1,
    backgroundColor: colors.glass.light,
    padding: 12,
    borderRadius: 12,
    marginHorizontal: 4,
    alignItems: "center",
    borderWidth: 1,
    borderColor: colors.glass.border,
  },
  shiftQuickDateActive: {
    backgroundColor: "rgba(139, 92, 246, 0.2)",
    borderColor: "#8b5cf6",
  },
  shiftQuickDateText: {
    color: colors.text.secondary,
    fontSize: 14,
    fontWeight: "500",
  },
  shiftQuickDateTextActive: {
    color: "#8b5cf6",
  },
  shiftTimeRow: {
    flexDirection: "row",
    alignItems: "flex-end",
    marginBottom: 16,
    gap: 12,
  },
  shiftTimeInputContainer: {
    flex: 1,
  },
  shiftTimeDisplay: {
    backgroundColor: colors.glass.dark,
    borderRadius: 12,
    padding: 16,
    borderWidth: 1,
    borderColor: colors.glass.border,
    alignItems: "center",
  },
  shiftTimeDisplayText: {
    color: colors.text.primary,
    fontSize: 24,
    fontWeight: "600",
    fontVariant: ["tabular-nums"],
  },
  shiftTimeInput: {
    backgroundColor: colors.glass.dark,
    borderRadius: 12,
    padding: 14,
    color: colors.text.primary,
    fontSize: 16,
    borderWidth: 1,
    borderColor: colors.glass.border,
    textAlign: "center",
  },
  shiftTimeSeparator: {
    paddingHorizontal: 12,
    paddingBottom: 14,
  },
  shiftTimeSeparatorText: {
    color: colors.text.muted,
    fontSize: 14,
  },
  shiftTimeQuickContainer: {
    marginBottom: 16,
  },
  shiftTimeQuick: {
    backgroundColor: colors.glass.light,
    paddingHorizontal: 14,
    paddingVertical: 10,
    borderRadius: 20,
    marginRight: 8,
    borderWidth: 1,
    borderColor: colors.glass.border,
  },
  shiftTimeQuickActive: {
    backgroundColor: "rgba(139, 92, 246, 0.2)",
    borderColor: "#8b5cf6",
  },
  shiftTimeQuickText: {
    color: colors.text.secondary,
    fontSize: 13,
    fontWeight: "500",
  },
  shiftTimeQuickTextActive: {
    color: "#8b5cf6",
  },
  shiftVenueQuickContainer: {
    marginBottom: 20,
  },
  shiftVenueQuick: {
    backgroundColor: colors.glass.light,
    paddingHorizontal: 14,
    paddingVertical: 10,
    borderRadius: 20,
    marginRight: 8,
    borderWidth: 1,
    borderColor: colors.glass.border,
  },
  shiftVenueQuickActive: {
    backgroundColor: "rgba(20, 184, 166, 0.2)",
    borderColor: "#14b8a6",
  },
  shiftVenueQuickText: {
    color: colors.text.secondary,
    fontSize: 13,
    fontWeight: "500",
  },
  shiftVenueQuickTextActive: {
    color: "#14b8a6",
  },
  shiftModalButtons: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
  },
  shiftModalCancelBtn: {
    paddingVertical: 12,
    paddingHorizontal: 20,
  },
  shiftModalCancelText: {
    color: colors.text.muted,
    fontSize: 16,
    fontWeight: "500",
  },
  shiftModalConfirmBtn: {
    paddingVertical: 12,
    paddingHorizontal: 24,
    borderRadius: 20,
  },
  shiftModalConfirmText: {
    color: "#fff",
    fontSize: 16,
    fontWeight: "600",
  },
  floatingButtonContainer: {
    position: "absolute",
    left: 0,
    right: 0,
    alignItems: "center",
    zIndex: 100,
  },
  floatingButtonWrapper: {
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 6 },
    shadowOpacity: 0.3,
    shadowRadius: 12,
    elevation: 8,
  },
  floatingButtonBlur: {
    borderRadius: 24,
    overflow: "hidden",
  },
  floatingButtonGradientBorder: {
    borderRadius: 24,
    padding: 1, // Creates the gradient border effect
  },
  floatingButtonInner: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 23,
  },
  floatingButtonText: {
    fontSize: 13,
    fontWeight: "700",
    letterSpacing: 0.3,
  },
  // Premium Active HUD Styling matrix
  activeHudContainer: {
    marginHorizontal: 16,
    marginBottom: 20,
    borderRadius: 22,
    overflow: "hidden",
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.15,
    shadowRadius: 12,
    elevation: 8,
  },
  activeHudBlur: {
    borderRadius: 22,
  },
  activeHudGradient: {
    padding: 18,
    borderRadius: 22,
  },
  activeHudHeader: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: 10,
  },
  pulseContainer: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: "rgba(239, 68, 68, 0.15)",
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 20,
    gap: 6,
  },
  pulseDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
  },
  activeHudLabel: {
    fontSize: 9,
    fontWeight: "900",
    letterSpacing: 1,
    color: "#ef4444",
  },
  activeHudTimer: {
    fontSize: 12,
    color: colors.text.secondary,
    fontWeight: "500",
  },
  activeHudTitle: {
    fontSize: 21,
    fontWeight: "800",
    color: colors.text.primary,
    marginBottom: 6,
    letterSpacing: -0.4,
  },
  activeHudSubRow: {
    flexDirection: "row",
    gap: 14,
    alignItems: "center",
    marginBottom: 14,
  },
  hudMetaItem: {
    flexDirection: "row",
    alignItems: "center",
    gap: 5,
  },
  hudMetaText: {
    fontSize: 12.5,
    color: colors.text.secondary,
    fontWeight: "600",
  },
  hudProgressBarBg: {
    height: 5,
    borderRadius: 3,
    width: "100%",
    overflow: "hidden",
    marginBottom: 16,
  },
  hudProgressBarFill: {
    height: "100%",
    borderRadius: 3,
  },
  hudActionRow: {
    flexDirection: "row",
    gap: 10,
  },
  hudBtn: {
    flex: 1,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 6,
    paddingVertical: 11,
    borderRadius: 14,
  },
  hudBtnText: {
    fontSize: 12.5,
    fontWeight: "800",
    letterSpacing: -0.1,
  },
});

export default HomeScreen;
