import React, {
  useState,
  useEffect,
  useMemo,
  useCallback,
  memo,
  useRef,
} from "react";
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  Dimensions,
  Modal,
  ActivityIndicator,
  Alert,
  ScrollView,
  Animated,
} from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import BackgroundMesh from "../components/BackgroundMesh";
import { BlurView } from "expo-blur";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";
import { format, addWeeks, startOfWeek, addDays } from "date-fns";
import { AddClassModal, EditClassModal, GlassHeader } from "../components";
import { useTimetableStore } from "../store/timetableStore";
import { useShallow } from "zustand/react/shallow";
import { ClassSchedule, AttendanceStatus, Task } from "../types";
import { captureRef } from "react-native-view-shot";
import * as Sharing from "expo-sharing";
import { GestureDetector, Gesture, Directions, GestureHandlerRootView } from "react-native-gesture-handler";

import {
  ThemePalette,
  getBackgroundGradient,
  useThemedColors,
  useThemedStyles,
} from "../theme/useTheme";

const { width: SCREEN_WIDTH, height: SCREEN_HEIGHT } = Dimensions.get("window");

// Time grid configuration
const START_HOUR = 0; // 12 AM (midnight)
const END_HOUR = 24; // 12 AM next day (24 hours total)
const TOTAL_HOURS = END_HOUR - START_HOUR;
const HOUR_HEIGHT = 60; // Fixed height per hour for scrollable view
const TIME_COLUMN_WIDTH = 50; // Wider for better time labels
const HEADER_HEIGHT = 50; // Compact header
const DAY_HEADER_HEIGHT = 48; // Day labels row
const TAB_BAR_HEIGHT = 85; // Bottom tab bar
const GRID_HEIGHT = TOTAL_HOURS * HOUR_HEIGHT; // Total scrollable height

// Interface for Task Pills
interface TaskPillProps {
  task: Task;
  top: number;
  dayIndex: number;
  dayWidth: number;
  theme: any;
  isDark: boolean;
  onPress: () => void;
}

// Gorgeous floating glass deadline pill
const TaskPill: React.FC<TaskPillProps> = memo(({
  task,
  top,
  dayIndex,
  dayWidth,
  theme,
  isDark,
  onPress
}) => {
  const size = 22;
  // Align exactly 3px inside the right-hand column boundary to prevent border bleeding
  const leftOffset = (dayIndex + 1) * dayWidth - (size + 3);

  // Unified, clean brand-teal theme for all tasks (priority removed permanently)
  const taskColor = theme.primary.teal;
  const iconName = "checkmark";

  return (
    <TouchableOpacity
      onPress={onPress}
      activeOpacity={0.8}
      style={{
        position: "absolute",
        top: top - size / 2,
        left: leftOffset,
        width: size,
        height: size,
        zIndex: 60, // Higher than classes to float on top
        shadowColor: taskColor,
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.3,
        shadowRadius: 2,
        elevation: 4,
      }}
    >
      <View style={{
        flex: 1,
        alignItems: "center",
        justifyContent: "center",
        backgroundColor: isDark ? "rgba(255, 255, 255, 0.1)" : "rgba(255, 255, 255, 0.85)",
        borderColor: taskColor + "B0", // Brighter glow border for visibility
        borderWidth: 1.2,
        borderRadius: size / 2,
        overflow: "hidden",
      }}>
        <BlurView intensity={15} tint={isDark ? "dark" : "light"} style={StyleSheet.absoluteFill} />
        
        <Ionicons 
          name={iconName as any} 
          size={11} 
          color={taskColor} 
          style={{ 
            textShadowColor: taskColor, 
            textShadowRadius: 1 
          }} 
        />
      </View>
    </TouchableOpacity>
  );
});

// Days to show (full week Monday to Saturday)
const WEEKDAYS = ["MON", "TUE", "WED", "THU", "FRI", "SAT"];

// Memoized class block component
const ClassBlock = memo(
  ({
    classItem,
    dayIndex,
    onPress,
    onLongPress,
    hourHeight,
    dayWidth,
    theme,
    styles,
  }: {
    classItem: ClassSchedule & {
      attended?: boolean;
      attendanceStatus?: string;
    };
    dayIndex: number;
    onPress: () => void;
    onLongPress: () => void;
    hourHeight: number;
    dayWidth: number;
    theme: ThemePalette;
    styles: any;
  }) => {
    const [startHour, startMin] = classItem.startTime.split(":").map(Number);
    const [endHour, endMin] = classItem.endTime.split(":").map(Number);

    // Calculate position (no clamping needed with 24-hour view)
    const startOffset =
      (startHour - START_HOUR) * hourHeight + (startMin / 60) * hourHeight;
    const duration = (endHour - startHour) * 60 + (endMin - startMin);
    const height = Math.max((duration / 60) * hourHeight, 28);

    // Desaturated color for calm look
    const baseColor = classItem.color || theme.primary.teal;

    const getStatusIndicator = () => {
      if (!classItem.attendanceStatus) return null;
      switch (classItem.attendanceStatus) {
        case "present":
          return { icon: "checkmark-circle", color: "#22c55e" };
        case "absent":
          return { icon: "close-circle", color: "#ef4444" };
        case "canceled":
          return { icon: "ban", color: "#64748b" };
        default:
          return null;
      }
    };

    const status = getStatusIndicator();

    return (
      <TouchableOpacity
        style={[
          styles.classBlock,
          {
            top: startOffset,
            left: dayIndex * dayWidth + 1,
            width: dayWidth - 2,
            height: height,
            borderColor: `${baseColor}40`,
            borderWidth: 1,
            borderLeftWidth: 4,
            borderLeftColor: baseColor,
            backgroundColor: "transparent",
          } as const,
        ]}
        onPress={onPress}
        onLongPress={onLongPress}
        activeOpacity={0.8}
        delayLongPress={400}
      >
        <LinearGradient
          colors={[`${baseColor}35`, `${baseColor}12`]}
          start={{ x: 0, y: 0 }}
          end={{ x: 1, y: 1 }}
          style={StyleSheet.absoluteFill}
        />
        
        <View style={{ flex: 1, padding: height > 35 ? 4 : 2, justifyContent: 'center' }}>
          <Text
            style={[
              styles.classBlockText,
              { fontSize: height > 35 ? 10.5 : 8.5, fontWeight: "700" as const } as const,
            ]}
            numberOfLines={height > 35 ? 2 : 1}
          >
            {classItem.shortName || classItem.courseName}
          </Text>
          {status && height > 35 && (
            <View style={[
              styles.statusDot,
              { backgroundColor: status.color, marginTop: 2 } as const,
            ]} />
          )}
        </View>
      </TouchableOpacity>
    );
  },
);

// Current time indicator component
const NowIndicator = memo(
  ({ currentTime, hourHeight }: { currentTime: Date; hourHeight: number }) => {
    const hours = currentTime.getHours();
    const minutes = currentTime.getMinutes();

    const offset =
      (hours - START_HOUR) * hourHeight + (minutes / 60) * hourHeight;

    // Use styles from parent scope
    const { colors } = useThemedColors();
    const styles = useThemedStyles(createStyles);

    return (
      <View style={[styles.nowIndicator, { top: offset }]}> 
        <View style={styles.nowDot} />
        <View style={styles.nowLine} />
      </View>
    );
  },
);

const WeekViewScreen: React.FC = () => {
  const insets = useSafeAreaInsets();
  const scrollViewRef = useRef<ScrollView>(null);
  const { colors, isDark } = useThemedColors();
  const styles = useThemedStyles(createStyles);
  const slideAnim = useRef(new Animated.Value(0)).current;
  const backgroundGradient = useMemo(
    () => getBackgroundGradient(colors),
    [colors],
  );

  // Calculate dynamic dimensions - fixed hour height for scrollable view
  const hourHeight = HOUR_HEIGHT;
  const dayWidth = (SCREEN_WIDTH - TIME_COLUMN_WIDTH) / WEEKDAYS.length;

  const {
    courses,
    semesters,
    weekClasses,
    currentWeek,
    isLoading,
    isInitialized,
    tasks,
    setCurrentWeek,
    loadWeekClassesWithAttendance,
    loadTasks,
    markAttendance,
    addClass,
    updateClass,
    deleteClass,
  } = useTimetableStore(
    useShallow((state) => ({
      courses: state.courses,
      semesters: state.semesters,
      weekClasses: state.weekClasses,
      currentWeek: state.currentWeek,
      isLoading: state.isLoading,
      isInitialized: state.isInitialized,
      tasks: state.tasks,
      setCurrentWeek: state.setCurrentWeek,
      loadWeekClassesWithAttendance: state.loadWeekClassesWithAttendance,
      loadTasks: state.loadTasks,
      markAttendance: state.markAttendance,
      addClass: state.addClass,
      updateClass: state.updateClass,
      deleteClass: state.deleteClass,
    }))
  );

  const [currentTime, setCurrentTime] = useState(new Date());
  const [selectedClass, setSelectedClass] = useState<ClassSchedule | null>(
    null,
  );
  const [showDetailsSheet, setShowDetailsSheet] = useState(false);
  const [showAddModal, setShowAddModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [selectedTask, setSelectedTask] = useState<Task | null>(null);
  const [showTaskSheet, setShowTaskSheet] = useState(false);
  const [isCapturing, setIsCapturing] = useState(false);
  const timetableRef = useRef<View>(null);
  const [isTransitioning, setIsTransitioning] = useState<{ direction: 'prev' | 'next' } | null>(null);
  const cachedWeekClasses = useRef<Record<string, ClassSchedule[]>>({});
  const cachedWeekDates = useRef<Record<string, any>>({});
  const cachedWeekTasks = useRef<Record<string, Task[]>>({});

  const handleShareGrid = async () => {
    try {
      setIsCapturing(true);
      
      // Give the native UI thread time to flush layout buffers to avoid blank snaps
      await new Promise((resolve) => setTimeout(resolve, 250));
      
      const uri = await captureRef(timetableRef, {
        format: "png",
        quality: 0.95, // Slightly lowered for faster share operations without loss
        result: "tmpfile", // Force physical file writing on disk
      });
      
      const isAvailable = await Sharing.isAvailableAsync();
      if (isAvailable) {
        await Sharing.shareAsync(uri, {
          dialogTitle: "Share your weekly timetable",
          mimeType: "image/png",
        });
      } else {
        Alert.alert("Share Error", "Sharing is not available on this device.");
      }
    } catch (error) {
      console.error("Capture Error:", error);
      Alert.alert("Capture Error", "Failed to generate an image of your timetable.");
    } finally {
      setIsCapturing(false);
    }
  };
  const [selectedDay, setSelectedDay] = useState<string>("MON");

  const currentSemester = semesters.find((s) => s.isActive) || semesters[0];

  // Update current time every minute
  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentTime(new Date());
    }, 60000);
    return () => clearInterval(timer);
  }, []);

  // Auto-scroll to current time on mount
  useEffect(() => {
    const scrollToCurrentTime = () => {
      const now = new Date();
      const hours = now.getHours();
      // Scroll to 2 hours before current time for context, but not before start
      const scrollToHour = Math.max(0, hours - 2);
      const scrollOffset = scrollToHour * HOUR_HEIGHT;
      scrollViewRef.current?.scrollTo({ y: scrollOffset, animated: false });
    };

    // Small delay to ensure ScrollView is mounted
    const timer = setTimeout(scrollToCurrentTime, 100);
    return () => clearTimeout(timer);
  }, []);

  // Calculate week dates
  const weekDates = useMemo(() => {
    const semesterStart = currentSemester?.startDate
      ? new Date(currentSemester.startDate)
      : new Date();
    const weekStart = addWeeks(
      startOfWeek(semesterStart, { weekStartsOn: 1 }),
      currentWeek - 1,
    );

    const dates: Record<
      string,
      { date: number; fullDate: string; month: string; isToday: boolean }
    > = {};
    const today = new Date();

    WEEKDAYS.forEach((day, index) => {
      const date = addDays(weekStart, index);
      dates[day] = {
        date: date.getDate(),
        fullDate: format(date, "yyyy-MM-dd"),
        month: format(date, "MMM"),
        isToday: format(date, "yyyy-MM-dd") === format(today, "yyyy-MM-dd"),
      };
    });

    return dates;
  }, [currentWeek, currentSemester?.startDate]);

  // Get week date range for header
  const weekDateRange = useMemo(() => {
    const firstDay = weekDates["MON"];
    const lastDay = weekDates["SAT"];
    if (!firstDay || !lastDay) return "";
    return `${firstDay.month} ${firstDay.date} – ${lastDay.date}`;
  }, [weekDates]);

  // Find today column index (moved before useEffect that uses it)
  const todayIndex = useMemo(() => {
    return WEEKDAYS.findIndex((day) => weekDates[day]?.isToday);
  }, [weekDates]);

  // Check if viewing current week
  const isCurrentWeek = todayIndex >= 0;

  // Build a flat map of day -> dateStr for the store action
  const weekDateMap = useMemo(() => {
    const map: Record<string, string> = {};
    WEEKDAYS.forEach((day) => {
      if (weekDates[day]) {
        map[day] = weekDates[day].fullDate;
      }
    });
    return map;
  }, [weekDates]);

  // Load classes for all weekdays using the dedicated store action
  useEffect(() => {
    if (!isInitialized || !currentSemester) return;
    loadWeekClassesWithAttendance(weekDateMap);
    loadTasks(); // Synchronously keep tasks fresh on week flips
  }, [isInitialized, currentWeek, currentSemester?.id, weekDateMap]);

  // Extract pending tasks for this week to layout inside the grid
  const weekTasks = useMemo(() => {
    const map: Record<string, Task[]> = {};
    WEEKDAYS.forEach((day) => {
      map[day] = [];
    });

    if (!tasks || tasks.length === 0) return map;

    tasks.forEach((task) => {
      if (!task.dueDate || task.status === "completed") return;
      
      // Format ISO date string down to local date comparison string
      const taskDateStr = task.dueDate.split("T")[0];
      
      WEEKDAYS.forEach((day) => {
        if (weekDates[day]?.fullDate === taskDateStr) {
          map[day].push(task);
        }
      });
    });

    return map;
  }, [tasks, weekDates]);

  const animateWeekTransition = useCallback((direction: "prev" | "next", callback: () => void) => {
    // 1. Capture current state snapshots
    cachedWeekClasses.current = { ...weekClasses };
    cachedWeekDates.current = { ...weekDates };
    cachedWeekTasks.current = { ...weekTasks };

    // 2. Activate transition layout wrapper
    setIsTransitioning({ direction });

    // 3. Animate from center (0) to horizontal target
    const targetValue = direction === "next" ? -SCREEN_WIDTH : SCREEN_WIDTH;
    slideAnim.setValue(0);

    // 4. Update core data model (asynchronously populates off-screen incoming view)
    callback();

    // 5. Kick off native slide animation
    Animated.timing(slideAnim, {
      toValue: targetValue,
      duration: 300,
      useNativeDriver: true,
    }).start(() => {
      // 6. Unmount overlay and reset
      slideAnim.setValue(0);
      setIsTransitioning(null);
    });
  }, [slideAnim, weekClasses, weekDates, weekTasks]);

  const handlePreviousWeek = useCallback(() => {
    if (currentWeek > 1) {
      animateWeekTransition("prev", () => {
        setCurrentWeek(currentWeek - 1);
      });
    }
  }, [currentWeek, setCurrentWeek, animateWeekTransition]);

  const handleNextWeek = useCallback(() => {
    const maxWeeks = currentSemester?.totalWeeks || 20;
    if (currentWeek < maxWeeks) {
      animateWeekTransition("next", () => {
        setCurrentWeek(currentWeek + 1);
      });
    }
  }, [currentWeek, currentSemester?.totalWeeks, setCurrentWeek, animateWeekTransition]);

  const handleJumpToToday = useCallback(() => {
    if (!currentSemester?.startDate) return;

    const semesterStart = new Date(currentSemester.startDate);
    const today = new Date();
    const diffWeeks = Math.floor(
      (today.getTime() - semesterStart.getTime()) / (7 * 24 * 60 * 60 * 1000),
    );
    const targetWeek = Math.max(1, diffWeeks + 1);

    if (targetWeek !== currentWeek) {
      const direction = targetWeek > currentWeek ? "next" : "prev";
      animateWeekTransition(direction, () => {
        setCurrentWeek(targetWeek);
      });
    }
  }, [currentSemester?.startDate, currentWeek, setCurrentWeek, animateWeekTransition]);

  const swipeGesture = useMemo(() => {
    const flingLeft = Gesture.Fling()
      .direction(Directions.LEFT)
      .runOnJS(true)
      .onStart(() => {
        handleNextWeek();
      });

    const flingRight = Gesture.Fling()
      .direction(Directions.RIGHT)
      .runOnJS(true)
      .onStart(() => {
        handlePreviousWeek();
      });

    return Gesture.Race(flingLeft, flingRight);
  }, [handleNextWeek, handlePreviousWeek]);

  const handleClassPress = useCallback((classItem: ClassSchedule) => {

    setSelectedClass(classItem);
    setShowDetailsSheet(true);
  }, []);

  const handleClassLongPress = useCallback((classItem: ClassSchedule) => {

    setSelectedClass(classItem);
    setShowEditModal(true);
  }, []);

  const handleTaskPress = useCallback((taskItem: Task) => {
    setSelectedTask(taskItem);
    setShowTaskSheet(true);
  }, []);

  const handleMarkAttendance = async (status: AttendanceStatus) => {
    if (!selectedClass) return;

    const dateInfo = weekDates[selectedClass.dayOfWeek];
    if (!dateInfo) return;

    try {


      await markAttendance(
        selectedClass.id,
        selectedClass.courseId,
        dateInfo.fullDate,
        status,
      );

      setShowDetailsSheet(false);
      setSelectedClass(null);

      // Reload all week classes with attendance
      await loadWeekClassesWithAttendance(weekDateMap);
    } catch (error) {
      Alert.alert("Error", "Failed to mark attendance");
    }
  };

  const handleUpdateClass = async (
    classId: string,
    updates: Partial<ClassSchedule>,
  ) => {
    try {
      await updateClass(classId, updates);
      setShowEditModal(false);
      setSelectedClass(null);
      // Reload all week classes
      await loadWeekClassesWithAttendance(weekDateMap);
    } catch (error) {
      const message = error instanceof Error ? error.message : "Failed to update class";
      Alert.alert("Update Class", message);
    }
  };

  const handleDeleteClass = async (classItem: ClassSchedule) => {

    try {
      await deleteClass(classItem.id);
      setShowEditModal(false);
      setSelectedClass(null);
    } catch (error) {
      Alert.alert("Error", "Failed to delete class");
    }
  };

  const handleAddClass = async (classData: any) => {
    try {
      await addClass(classData);
      setShowAddModal(false);
    } catch (error) {
      const message = error instanceof Error ? error.message : "Failed to add class";
      Alert.alert("Add Class", message);
    }
  };

  // Render time labels
  const renderTimeLabels = () => {
    const hours = [];
    for (let h = START_HOUR; h < END_HOUR; h++) {
      const displayHour = h > 12 ? h - 12 : h === 0 ? 12 : h;
      const meridiem = h >= 12 ? "PM" : "AM";
      hours.push(
        <View key={h} style={[styles.timeLabel, { height: hourHeight }]}>
          <Text style={styles.timeLabelText}>
            {displayHour} {meridiem}
          </Text>
        </View>,
      );
    }
    return hours;
  };

  // Render horizontal grid lines
  const renderGridLines = () => {
    const lines = [];
    for (let h = START_HOUR; h < END_HOUR; h++) {
      lines.push(
        <View
          key={h}
          style={[styles.gridLine, { top: (h - START_HOUR) * hourHeight }]}
        />,
      );
    }
    return lines;
  };

  const renderGrid = (
    gridClasses: Record<string, ClassSchedule[]>,
    gridDates: Record<string, any>,
    gridTasks: Record<string, Task[]>,
    activeTodayIndex: number,
    activeIsCurrentWeek: boolean,
    refToUse?: React.RefObject<ScrollView | null>
  ) => {
    return (
      <View style={{ flex: 1 }}>
        {/* Day Headers Row - Fixed */}
        <View style={styles.dayHeadersRow}>
          <View style={[styles.timeColumnSpacer, { width: TIME_COLUMN_WIDTH }]} />
          {WEEKDAYS.map((day) => {
            const dateInfo = gridDates[day];
            const isToday = dateInfo?.isToday;
            return (
              <View
                key={day}
                style={[
                  styles.dayHeader,
                  { width: dayWidth },
                  isToday && styles.dayHeaderToday,
                ]}
              >
                <Text
                  style={[
                    styles.dayHeaderText,
                    isToday && styles.dayHeaderTextToday,
                  ]}
                >
                  {day}
                </Text>
                <Text style={[styles.dateText, isToday && styles.dateTextToday]}>
                  {dateInfo?.date || "--"}
                </Text>
              </View>
            );
          })}
        </View>

        {/* Scrollable Grid Body */}
        <ScrollView
          ref={refToUse}
          style={styles.scrollContainer}
          contentContainerStyle={styles.scrollContent}
          showsVerticalScrollIndicator={true}
          bounces={true}
        >
          <View style={[styles.gridBody, { height: GRID_HEIGHT }]}>
            <View style={[styles.timeColumn, { width: TIME_COLUMN_WIDTH }]}>
              {renderTimeLabels()}
            </View>
            <View style={styles.gridArea}>
              {renderGridLines()}
              {activeTodayIndex >= 0 && (
                <View
                  style={[
                    styles.todayColumn,
                    {
                      left: activeTodayIndex * dayWidth,
                      width: dayWidth,
                    },
                  ]}
                />
              )}
              {activeIsCurrentWeek && (
                <NowIndicator
                  currentTime={currentTime}
                  hourHeight={hourHeight}
                />
              )}
              {WEEKDAYS.map((day, dayIndex) =>
                (gridClasses[day] || []).map((classItem) => (
                  <ClassBlock
                    key={classItem.id}
                    classItem={classItem}
                    dayIndex={dayIndex}
                    hourHeight={hourHeight}
                    dayWidth={dayWidth}
                    onPress={() => handleClassPress(classItem)}
                    onLongPress={() => handleClassLongPress(classItem)}
                    theme={colors}
                    styles={styles}
                  />
                ))
              )}
              {/* Tasks Floating Capsules Overlay */}
              {WEEKDAYS.map((day, dayIndex) =>
                (gridTasks[day] || []).map((taskItem) => {
                  if (!taskItem.dueDate) return null;
                  const date = new Date(taskItem.dueDate);
                  const hours = date.getHours();
                  const minutes = date.getMinutes();
                  const top = ((hours + minutes / 60) - START_HOUR) * hourHeight;
                  
                  return (
                    <TaskPill
                      key={taskItem.id}
                      task={taskItem}
                      top={top}
                      dayIndex={dayIndex}
                      dayWidth={dayWidth}
                      theme={colors}
                      isDark={isDark}
                      onPress={() => handleTaskPress(taskItem)}
                    />
                  );
                })
              )}
            </View>
          </View>
        </ScrollView>
      </View>
    );
  };

  if (isLoading || !isInitialized) {
    return (
      <LinearGradient
        colors={backgroundGradient}
        style={styles.loadingContainer}
      >
        <ActivityIndicator size="large" color={colors.primary.teal} />
      </LinearGradient>
    );
  }

  return (
    <GestureHandlerRootView style={{ flex: 1 }}>
      <View style={styles.container}>
      <LinearGradient colors={backgroundGradient} style={StyleSheet.absoluteFill} />
      <BackgroundMesh />

      <GlassHeader
        title="Week Schedule"
        subtitle={`Week ${currentWeek} • ${weekDateRange} • ${currentSemester?.name || ""}`}
        rightComponent={
          <View style={{ flexDirection: 'row', gap: 8, alignItems: 'center' }}>
            <TouchableOpacity
              style={styles.todayButton}
              onPress={handleShareGrid}
              disabled={isCapturing}
            >
              <Ionicons name="share-outline" size={18} color={colors.text.primary} />
            </TouchableOpacity>
          </View>
        }
      />

      {/* Full-Page Grid - Scrollable */}
      <GestureDetector gesture={swipeGesture}>
        <View 
          ref={timetableRef} 
          collapsable={false}
          style={styles.gridContainer}
        >
        {/* Local background layers embedded to feed full ambient aesthetic into shared snapshot image */}
        <LinearGradient colors={backgroundGradient} style={StyleSheet.absoluteFill} />
        <BackgroundMesh />
        
        <Animated.View style={{ 
          flex: 1, 
          transform: isTransitioning ? [{ translateX: slideAnim }] : []
        }}>
          {!isTransitioning ? (
            // Bulletproof Default State - 100% identical to the original working architecture
            renderGrid(weekClasses, weekDates, weekTasks, todayIndex, isCurrentWeek, scrollViewRef)
          ) : (
            // Bulletproof Overlay State - Absolute overlays completely eliminate flex squashing
            <>
              {/* Page A: Outgoing Old Week (Strictly at 0,0) */}
              <View style={{ position: 'absolute', top: 0, bottom: 0, left: 0, right: 0 }}>
                {renderGrid(cachedWeekClasses.current, cachedWeekDates.current, cachedWeekTasks.current, -1, false)}
              </View>
              
              {/* Page B: Incoming New Week (Strictly at offset, out of grid flow) */}
              <View style={{ 
                position: 'absolute', 
                top: 0, 
                bottom: 0, 
                left: isTransitioning.direction === 'next' ? SCREEN_WIDTH : -SCREEN_WIDTH,
                width: SCREEN_WIDTH
              }}>
                {renderGrid(weekClasses, weekDates, weekTasks, todayIndex, isCurrentWeek)}
              </View>
            </>
          )}
        </Animated.View>
      </View>
      </GestureDetector>

      {/* Class Details Bottom Sheet */}
      <Modal
        visible={showDetailsSheet}
        transparent
        animationType="slide"
        onRequestClose={() => setShowDetailsSheet(false)}
      >
        <TouchableOpacity
          style={styles.sheetOverlay}
          activeOpacity={1}
          onPress={() => setShowDetailsSheet(false)}
        >
          <View
            style={styles.sheetContainer}
            onStartShouldSetResponder={() => true}
          >
            <BlurView intensity={80} tint={colors.glass.tint} style={styles.sheetBlur}>
              <LinearGradient
                colors={[colors.background.surface, colors.background.sunken]}
                style={styles.sheetContent}
              >
                {/* Handle */}
                <View style={styles.sheetHandle} />

                {selectedClass && (
                  <>
                    {/* Class Info */}
                    <View style={styles.sheetHeader}>
                      <View
                        style={[
                          styles.sheetColorBar,
                          { backgroundColor: selectedClass.color },
                        ]}
                      />
                      <View style={styles.sheetInfo}>
                        <Text style={styles.sheetTitle}>
                          {selectedClass.courseName}
                        </Text>
                        <Text style={styles.sheetSubtitle}>
                          {selectedClass.shortName}
                        </Text>
                      </View>
                    </View>

                    {/* Details */}
                    <View style={styles.sheetDetails}>
                      <View style={styles.sheetDetailRow}>
                        <Ionicons
                          name="time-outline"
                          size={18}
                          color={colors.text.muted}
                        />
                        <Text style={styles.sheetDetailText}>
                          {selectedClass.startTime} – {selectedClass.endTime}
                        </Text>
                      </View>
                      <View style={styles.sheetDetailRow}>
                        <Ionicons
                          name="location-outline"
                          size={18}
                          color={colors.text.muted}
                        />
                        <Text style={styles.sheetDetailText}>
                          {selectedClass.room}
                        </Text>
                      </View>
                      <View style={styles.sheetDetailRow}>
                        <Ionicons
                          name="person-outline"
                          size={18}
                          color={colors.text.muted}
                        />
                        <Text style={styles.sheetDetailText}>
                          {selectedClass.professor}
                        </Text>
                      </View>
                    </View>

                    {/* Attendance Actions */}
                    <Text style={styles.sheetSectionTitle}>
                      Mark Attendance
                    </Text>
                    <View style={styles.attendanceActions}>
                      <TouchableOpacity
                        style={[styles.attendanceButton, styles.attendedButton]}
                        onPress={() => handleMarkAttendance("present")}
                      >
                        <Ionicons
                          name="checkmark-circle"
                          size={24}
                          color={colors.status.success}
                        />
                        <Text
                          style={[
                            styles.attendanceButtonText,
                            { color: colors.status.success },
                          ]}
                        >
                          Attended
                        </Text>
                      </TouchableOpacity>

                      <TouchableOpacity
                        style={[styles.attendanceButton, styles.absentButton]}
                        onPress={() => handleMarkAttendance("absent")}
                      >
                        <Ionicons
                          name="close-circle"
                          size={24}
                          color={colors.status.danger}
                        />
                        <Text
                          style={[
                            styles.attendanceButtonText,
                            { color: colors.status.danger },
                          ]}
                        >
                          Absent
                        </Text>
                      </TouchableOpacity>

                      <TouchableOpacity
                        style={[styles.attendanceButton, styles.canceledButton]}
                        onPress={() => handleMarkAttendance("canceled")}
                      >
                        <Ionicons name="ban" size={24} color={colors.text.muted} />
                        <Text
                          style={[
                            styles.attendanceButtonText,
                            { color: colors.text.muted },
                          ]}
                        >
                          No Class
                        </Text>
                      </TouchableOpacity>
                    </View>

                    {/* Edit Button */}
                    <TouchableOpacity
                      style={styles.editButton}
                      onPress={() => {
                        setShowDetailsSheet(false);
                        setShowEditModal(true);
                      }}
                    >
                      <Ionicons
                        name="create-outline"
                        size={18}
                        color={colors.primary.teal}
                      />
                      <Text style={styles.editButtonText}>Edit Class</Text>
                    </TouchableOpacity>
                  </>
                )}
              </LinearGradient>
            </BlurView>
          </View>
        </TouchableOpacity>
      </Modal>

      {/* Add Class Modal */}
      <AddClassModal
        visible={showAddModal}
        onClose={() => setShowAddModal(false)}
        onSubmit={handleAddClass}
        courses={courses}
        semesterId={currentSemester?.id || ""}
        preselectedDay={selectedDay}
      />

      {/* Edit Class Modal */}
      <EditClassModal
        visible={showEditModal}
        onClose={() => {
          setShowEditModal(false);
          setSelectedClass(null);
        }}
        onSubmit={handleUpdateClass}
        onDelete={handleDeleteClass}
        classItem={selectedClass}
        courses={courses}
      />

      {/* Task Details Bottom Sheet */}
      <Modal
        visible={showTaskSheet}
        transparent
        animationType="slide"
        onRequestClose={() => setShowTaskSheet(false)}
      >
        <TouchableOpacity
          style={styles.sheetOverlay}
          activeOpacity={1}
          onPress={() => setShowTaskSheet(false)}
        >
          <View
            style={styles.sheetContainer}
            onStartShouldSetResponder={() => true}
          >
            <BlurView intensity={80} tint={colors.glass.tint} style={styles.sheetBlur}>
              <LinearGradient
                colors={[colors.background.surface, colors.background.sunken]}
                style={styles.sheetContent}
              >
                {/* Handle */}
                <View style={styles.sheetHandle} />

                {selectedTask && (
                  <>
                    {/* Task Header */}
                    <View style={styles.sheetHeader}>
                      <View
                        style={[
                          styles.sheetColorBar,
                          { backgroundColor: colors.primary.teal },
                        ]}
                      />
                      <View style={styles.sheetInfo}>
                        <Text style={styles.sheetTitle}>
                          {selectedTask.title}
                        </Text>
                        <Text style={styles.sheetSubtitle}>
                          Pending Task
                        </Text>
                      </View>
                    </View>

                    {/* Details */}
                    <View style={styles.sheetDetails}>
                      <View style={styles.sheetDetailRow}>
                        <Ionicons
                          name="time-outline"
                          size={18}
                          color={colors.text.muted}
                        />
                        <Text style={styles.sheetDetailText}>
                          Due {selectedTask.dueDate ? format(new Date(selectedTask.dueDate), "EEEE, MMM d 'at' h:mm a") : "No due date"}
                        </Text>
                      </View>

                      {/* Description Section */}
                      <View style={{ marginTop: 16, paddingHorizontal: 4 }}>
                        <Text style={[styles.sheetSectionTitle, { marginBottom: 6, paddingLeft: 0 }]}>
                          Description
                        </Text>
                        <Text style={{ 
                          fontSize: 14, 
                          color: colors.text.secondary, 
                          lineHeight: 20 
                        }}>
                          {selectedTask.description || "No additional details provided for this task."}
                        </Text>
                      </View>
                    </View>
                    
                    {/* Safe area cushion */}
                    <View style={{ height: 20 }} />
                  </>
                )}
              </LinearGradient>
            </BlurView>
          </View>
        </TouchableOpacity>
      </Modal>

      {/* Floating Today Button */}
      {!isCurrentWeek && (
        <View style={[styles.floatingButtonContainer, { bottom: insets.bottom + 100 }]}>
          <TouchableOpacity onPress={handleJumpToToday} activeOpacity={0.8} style={styles.floatingButtonWrapper}>
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
      </View>
    </GestureHandlerRootView>
  );
};

const createStyles = (colors: ThemePalette) => ({
  container: {
    flex: 1,
    backgroundColor: colors.background.start,
  },
  loadingContainer: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
  },

  // Header
  header: {
    zIndex: 10,
  },
  headerBlur: {
    overflow: "hidden",
  },
  headerGradient: {
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: colors.border.subtle,
    backgroundColor: colors.glass.cardFill,
  },
  headerTop: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
  },
  headerLeft: {},
  headerTitle: {
    fontSize: 22,
    fontWeight: "700",
    color: colors.text.primary,
  },
  headerSubtitle: {
    fontSize: 13,
    color: colors.text.muted,
    marginTop: 2,
  },
  todayButton: {
    paddingHorizontal: 14,
    paddingVertical: 6,
    borderRadius: 14,
    backgroundColor: colors.glass.cardFill,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: colors.primary.teal + '40',
  },
  todayButtonText: {
    fontSize: 13,
    fontWeight: "600",
    color: colors.primary.teal,
  },
  headerActions: {
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
  },
  addButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: colors.glass.cardFill,
    alignItems: "center",
    justifyContent: "center",
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: colors.glass.cardBorder,
  },
  condensedToggle: {
    width: 34,
    height: 34,
    borderRadius: 17,
    backgroundColor: colors.glass.cardFill,
    alignItems: "center",
    justifyContent: "center",
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: colors.glass.cardBorder,
  },

  // Week Navigation
  weekNav: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  weekNavButton: {
    width: 34,
    height: 34,
    borderRadius: 17,
    backgroundColor: colors.glass.cardFill,
    alignItems: "center",
    justifyContent: "center",
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: colors.glass.cardBorder,
  },
  weekNavCenter: {
    flex: 1,
    alignItems: "center",
  },
  weekNumber: {
    fontSize: 15,
    fontWeight: "600",
    color: colors.text.primary,
  },
  weekRange: {
    fontSize: 12,
    color: colors.text.muted,
    marginTop: 2,
  },
  jumpToday: {
    fontSize: 10,
    color: colors.primary.cyan,
    marginTop: 2,
  },

  // Grid Container - Full Page
  gridContainer: {
    flex: 1,
  },
  scrollContainer: {
    flex: 1,
  },
  scrollContent: {
    flexGrow: 1,
  },
  dayHeadersRow: {
    flexDirection: "row",
    height: DAY_HEADER_HEIGHT,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: colors.border.subtle,
    backgroundColor: colors.glass.cardFill,
  },
  timeColumnSpacer: {
    borderRightWidth: 1,
    borderRightColor: colors.border.subtle,
  },
  dayHeader: {
    alignItems: "center",
    justifyContent: "center",
    borderRightWidth: 1,
    borderRightColor: colors.border.subtle,
  },
  dayHeaderToday: {
    backgroundColor: `${colors.primary.teal}15`,
  },
  dayHeaderText: {
    fontSize: 11,
    fontWeight: "700",
    color: colors.text.secondary,
    letterSpacing: 0.5,
  },
  dayHeaderTextToday: {
    color: colors.primary.teal,
  },
  dateText: {
    fontSize: 14,
    fontWeight: "700",
    color: colors.text.primary,
    marginTop: 2,
  },
  dateTextToday: {
    color: colors.primary.teal,
  },
  gridBody: {
    flexDirection: "row",
    flex: 1,
  },
  timeColumn: {
    backgroundColor: colors.background.surface,
    borderRightWidth: 1,
    borderRightColor: colors.border.subtle,
  },
  timeLabel: {
    justifyContent: "flex-start",
    paddingLeft: 6,
    paddingTop: 0,
  },
  timeLabelText: {
    fontSize: 10,
    color: colors.text.secondary,
    fontWeight: "600",
  },

  // Grid Area
  gridArea: {
    flex: 1,
    position: "relative",
  },
  gridLine: {
    position: "absolute",
    left: 0,
    right: 0,
    height: StyleSheet.hairlineWidth,
    backgroundColor: colors.border.strong,
  },
  todayColumn: {
    position: "absolute",
    top: 0,
    bottom: 0,
    backgroundColor: `${colors.primary.teal}10`,
  },

  // Now Indicator
  nowIndicator: {
    position: "absolute",
    left: 0,
    right: 0,
    flexDirection: "row",
    alignItems: "center",
    zIndex: 100,
  },
  nowDot: {
    width: 10,
    height: 10,
    borderRadius: 5,
    backgroundColor: colors.status.danger,
    marginLeft: -5,
  },
  nowLine: {
    flex: 1,
    height: 2,
    backgroundColor: colors.status.danger,
  },
  nowLabel: {
    backgroundColor: colors.status.danger,
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 4,
  },
  nowLabelText: {
    fontSize: 9,
    fontWeight: "700",
    color: "#fff",
    letterSpacing: 0.5,
  },

  // Class Block
  classBlock: {
    position: "absolute" as const,
    borderRadius: 8,
    borderLeftWidth: 3,
    paddingHorizontal: 6,
    paddingVertical: 4,
    overflow: "hidden" as const,
    backgroundColor: colors.glass.cardFill,
  },
  classBlockText: {
    fontSize: 11,
    fontWeight: "600" as const,
    color: colors.text.primary,
    lineHeight: 14,
  },
  statusDot: {
    position: "absolute" as const,
    top: 4,
    right: 4,
    width: 7,
    height: 7,
    borderRadius: 4,
  },

  // Bottom Sheet
  sheetOverlay: {
    flex: 1,
    backgroundColor: "transparent", // Zero dimming, shows timetable fully
    justifyContent: "flex-end",
  },
  sheetContainer: {
    maxHeight: SCREEN_HEIGHT * 0.55,
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    borderWidth: 1.2,
    borderBottomWidth: 0,
    borderColor: colors.glass.stroke, // iOS premium glass separator
    overflow: "hidden",
    shadowColor: "#000",
    shadowOffset: { width: 0, height: -4 },
    shadowOpacity: 0.16,
    shadowRadius: 16,
    elevation: 24,
  },
  sheetBlur: {
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
  },
  sheetContent: {
    padding: 20,
    paddingBottom: 40,
    backgroundColor: colors.glass.elevatedFill,
  },
  sheetHandle: {
    width: 40,
    height: 4,
    backgroundColor: colors.border.subtle,
    borderRadius: 2,
    alignSelf: "center",
    marginBottom: 20,
  },
  sheetHeader: {
    flexDirection: "row",
    marginBottom: 20,
  },
  sheetColorBar: {
    width: 4,
    borderRadius: 2,
    marginRight: 12,
  },
  sheetInfo: {
    flex: 1,
  },
  sheetTitle: {
    fontSize: 20,
    fontWeight: "700",
    color: colors.text.primary,
  },
  sheetSubtitle: {
    fontSize: 14,
    color: colors.text.muted,
    marginTop: 4,
  },
  sheetDetails: {
    backgroundColor: colors.glass.cardFill,
    borderRadius: 12,
    padding: 16,
    marginBottom: 20,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: colors.glass.cardBorder,
  },
  sheetDetailRow: {
    flexDirection: "row",
    alignItems: "center",
    marginBottom: 12,
  },
  sheetDetailText: {
    fontSize: 14,
    color: colors.text.secondary,
    marginLeft: 12,
  },
  sheetSectionTitle: {
    fontSize: 12,
    fontWeight: "600",
    color: colors.text.muted,
    marginBottom: 12,
    textTransform: "uppercase",
    letterSpacing: 0.5,
  },
  attendanceActions: {
    flexDirection: "row",
    gap: 10,
    marginBottom: 20,
  },
  attendanceButton: {
    flex: 1,
    flexDirection: "column",
    alignItems: "center",
    paddingVertical: 14,
    borderRadius: 12,
    backgroundColor: colors.glass.cardFill,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: colors.glass.cardBorder,
  },
  attendedButton: {
    borderColor: `${colors.status.success}4d`,
    backgroundColor: `${colors.status.success}14`,
  },
  absentButton: {
    borderColor: `${colors.status.danger}4d`,
    backgroundColor: `${colors.status.danger}14`,
  },
  canceledButton: {
    borderColor: colors.text.muted + '40',
    backgroundColor: colors.glass.cardFill,
  },
  attendanceButtonText: {
    fontSize: 11,
    fontWeight: "600",
    marginTop: 6,
    color: colors.text.primary,
  },
  editButton: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    paddingVertical: 14,
    borderRadius: 12,
    backgroundColor: colors.glass.cardFill,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: colors.primary.teal + '40',
  },
  editButtonText: {
    fontSize: 14,
    fontWeight: "600",
    color: colors.primary.teal,
    marginLeft: 8,
  },

  // Floating Today Button
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
});

export default WeekViewScreen;
