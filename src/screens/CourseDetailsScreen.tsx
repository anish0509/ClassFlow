import React, { useEffect, useMemo, useState, useCallback } from "react";
import {
  View,
  Text,
  ScrollView,
  StyleSheet,
  TouchableOpacity,
  Dimensions,
  ActivityIndicator,
  TextInput,
  Modal,
  Alert,
  Linking,
} from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import { BlurView } from "expo-blur";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";
import { NativeStackScreenProps } from "@react-navigation/native-stack";
import {
  format,
  startOfMonth,
  endOfMonth,
  getDay,
  addMonths,
  subMonths,
  isSameDay,
  parseISO,
  eachDayOfInterval,
  isAfter,
  startOfDay,
} from "date-fns";
import { useTimetableStore } from "../store/timetableStore";
import { Course, ClassSchedule, Attendance, Attachment } from "../types";
import { calculateAttendanceAnalysis } from "../utils/attendanceCalculator";
import * as db from "../database";
import { RootStackParamList } from "../types/navigation";
import {
  ThemePalette,
  getBackgroundGradient,
  useThemedColors,
  useThemedStyles,
} from "../theme/useTheme";

const { width: SCREEN_WIDTH } = Dimensions.get("window");

const generateUUID = () =>
  "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === "x" ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });

type CourseDetailsScreenProps = NativeStackScreenProps<RootStackParamList, "CourseDetails">;

const CourseDetailsScreen: React.FC<CourseDetailsScreenProps> = ({
  route,
  navigation,
}) => {
  const insets = useSafeAreaInsets();
  const { colors, isDark } = useThemedColors();
  const styles = useThemedStyles(createStyles);
  const backgroundGradient = getBackgroundGradient(colors);

  const { 
    courses, 
    tasks, 
    loadTasks, 
    toggleTaskStatus,
    markAttendance,
    removeAttendance
  } = useTimetableStore();
  
  const courseId = route.params?.courseId || route.params?.course?.id || "";
  const course = useMemo(
    () =>
      courses.find((c) => c.id === courseId) || route.params?.course,
    [courses, courseId, route.params?.course],
  );

  const [courseClasses, setCourseClasses] = useState<ClassSchedule[]>([]);
  const [courseAttendance, setCourseAttendance] = useState<Attendance[]>([]);
  const [currentMonth, setCurrentMonth] = useState(new Date());
  const [isLoading, setIsLoading] = useState(true);
  const [attendanceStats, setAttendanceStats] = useState({
    total: 0,
    present: 0,
    absent: 0,
    canceled: 0,
  });

  // Retroactive Attendance States
  const [selectedCalendarDay, setSelectedCalendarDay] = useState<Date | null>(null);
  const [matchingClassesForDay, setMatchingClassesForDay] = useState<ClassSchedule[]>([]);
  const [showRetroactiveSheet, setShowRetroactiveSheet] = useState(false);

  const reloadCourseData = useCallback(async () => {
    if (!courseId) return;
    setIsLoading(true);
    try {
      const database = await db.getDatabase();
      const [classes, attendance, stats] =
        await Promise.all([
          database.getAllAsync<ClassSchedule>(
            `SELECT * FROM classes WHERE courseId = ?`,
            [courseId],
          ),
          database.getAllAsync<Attendance>(
            `SELECT * FROM attendance WHERE courseId = ? ORDER BY date DESC`,
            [courseId],
          ),
          db.getAttendanceStats(courseId),
        ]);

      setCourseClasses(classes);
      setCourseAttendance(attendance);
      setAttendanceStats(
        stats || { total: 0, present: 0, absent: 0, canceled: 0 },
      );
    } catch (error) {
      console.error("Failed to load course data:", error);
    } finally {
      setIsLoading(false);
    }
  }, [courseId]);

  useEffect(() => {
    reloadCourseData();
    loadTasks();
  }, [reloadCourseData, loadTasks]);

  const targetPercentage = 75;

  // Bunk Smart Predictive Assist Logic
  const bunkAssist = useMemo(() => {
    const { present, total } = attendanceStats;
    if (total === 0) return null;
    
    const currentPct = (present / total) * 100;
    
    if (currentPct >= targetPercentage) {
      const maxMissable = Math.floor((100 * present - targetPercentage * total) / targetPercentage);
      return {
        status: "safe",
        value: Math.max(0, maxMissable),
        message: maxMissable > 0 
          ? `You can safely miss ${maxMissable} upcoming class${maxMissable > 1 ? 'es' : ''} without dropping below 75%.`
          : `Critical: Missing any more classes will pull you below 75% target.`
      };
    } else {
      const factor = 100 - targetPercentage;
      const minRequired = Math.ceil((targetPercentage * total - 100 * present) / factor);
      return {
        status: "critical",
        value: minRequired,
        message: `You must attend ${minRequired} consecutive class${minRequired > 1 ? 'es' : ''} to recover back to 75%.`
      };
    }
  }, [attendanceStats, targetPercentage]);

  // Course-Specific Task Filtering
  const courseTasks = useMemo(() => {
    return tasks.filter((t) => t.courseId === courseId && t.status === "pending");
  }, [tasks, courseId]);

  const handleToggleTask = useCallback(async (taskId: string) => {
    try {
      await toggleTaskStatus(taskId);
    } catch (error) {
      console.error("Failed to complete task:", error);
    }
  }, [toggleTaskStatus]);

  // Quick-Tap Retroactive Calendar Trigger
  const handleCalendarDayPress = useCallback((day: Date) => {
    const dayName = format(day, "EEE").toUpperCase();
    const matching = courseClasses.filter(c => c.dayOfWeek === dayName);
    
    setSelectedCalendarDay(day);
    setMatchingClassesForDay(matching);
    setShowRetroactiveSheet(true); // Open custom glass UI modal in all cases (even if 0 classes)!
  }, [courseClasses]);

  const handleSaveRetroactiveAttendance = useCallback(async (
    classId: string, 
    status: "present" | "absent" | "canceled" | "clear"
  ) => {
    if (!selectedCalendarDay || !courseId) return;
    const dateStr = format(selectedCalendarDay, "yyyy-MM-dd");
    const existing = courseAttendance.find(a => a.date === dateStr && a.classId === classId);
    
    try {
      if (status === "clear") {
        if (existing) {
          await removeAttendance(existing.id);
        }
      } else {
        await markAttendance(classId, courseId, dateStr, status);
      }
      await reloadCourseData(); // Update UI elements instantly!
    } catch (error) {
      console.error("Failed updating retroactive attendance:", error);
      Alert.alert("Sync Failed", "Could not update your attendance.");
    }
  }, [selectedCalendarDay, courseId, courseAttendance, removeAttendance, markAttendance, reloadCourseData]);

  const getAttendanceForClassAndDate = (classId: string, date: Date) => {
    const dateStr = format(date, "yyyy-MM-dd");
    return courseAttendance.find(a => a.date === dateStr && a.classId === classId);
  };

  const attendancePercentage =
    attendanceStats.total > 0
      ? Math.round((attendanceStats.present / attendanceStats.total) * 100)
      : 0;

  const attendanceAnalysis = calculateAttendanceAnalysis(
    attendanceStats.present,
    attendanceStats.total,
    attendanceStats.canceled,
    75,
    0,
  );

  const getAttendanceForDate = (date: Date) => {
    const dateStr = format(date, "yyyy-MM-dd");
    return courseAttendance.find((a) => a.date === dateStr);
  };

  const renderCalendar = () => {
    const monthStart = startOfMonth(currentMonth);
    const monthEnd = endOfMonth(currentMonth);
    const days = eachDayOfInterval({ start: monthStart, end: monthEnd });
    const startDay = getDay(monthStart);
    const weekDays = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];

    const paddedDays: (Date | null)[] = [];
    for (let i = 0; i < startDay; i++) paddedDays.push(null);
    paddedDays.push(...days);
    while (paddedDays.length % 7 !== 0) paddedDays.push(null);

    const weeks: (Date | null)[][] = [];
    for (let i = 0; i < paddedDays.length; i += 7) {
      weeks.push(paddedDays.slice(i, i + 7));
    }

    return (
      <View style={styles.calendarCard}>
        <BlurView intensity={20} tint={colors.glass.tint} style={styles.calendarBlur}>
          <LinearGradient
            colors={[colors.glass.dark, colors.glass.light]}
            style={styles.calendarGradient}
          >
            <View style={styles.calendarHeader}>
              <TouchableOpacity
                style={styles.calendarNavBtn}
                onPress={() => setCurrentMonth(subMonths(currentMonth, 1))}
              >
                <Ionicons name="chevron-back" size={20} color={course?.color || colors.text.primary} />
              </TouchableOpacity>
              <Text style={styles.calendarTitle}>
                {format(currentMonth, "MMMM")}{" "}
                <Text style={{ color: course?.color || colors.text.primary }}>
                  {format(currentMonth, "yyyy")}
                </Text>
              </Text>
              <TouchableOpacity
                style={styles.calendarNavBtn}
                onPress={() => setCurrentMonth(addMonths(currentMonth, 1))}
              >
                <Ionicons name="chevron-forward" size={20} color={course?.color || colors.text.primary} />
              </TouchableOpacity>
            </View>

            <View style={styles.weekDaysRow}>
              {weekDays.map((day) => (
                <Text key={day} style={styles.weekDayText}>
                  {day}
                </Text>
              ))}
            </View>

            {weeks.map((week, weekIndex) => (
              <View key={weekIndex} style={styles.calendarWeek}>
                {week.map((day, dayIndex) => {
                  if (!day) {
                    return <View key={dayIndex} style={styles.calendarDay} />;
                  }

                  const attendance = getAttendanceForDate(day);
                  const isToday = isSameDay(day, new Date());

                  let bgColor = "transparent";
                  let statusIcon: string | null = null;
                  let iconColor = colors.text.muted;

                  if (attendance) {
                    switch (attendance.status) {
                      case "present":
                        bgColor = "#22c55e30";
                        statusIcon = "checkmark";
                        iconColor = "#22c55e";
                        break;
                      case "absent":
                        bgColor = "#ef444430";
                        statusIcon = "close";
                        iconColor = "#ef4444";
                        break;
                      case "canceled":
                        bgColor = "#64748b30";
                        statusIcon = "ban-outline";
                        iconColor = "#64748b";
                        break;
                      case "shifted":
                        bgColor = "#8b5cf630";
                        statusIcon = "swap-horizontal";
                        iconColor = "#8b5cf6";
                        break;
                    }
                  }

                  const isFuture = day && isAfter(startOfDay(day), startOfDay(new Date()));

                  return (
                    <TouchableOpacity 
                      key={dayIndex} 
                      style={styles.calendarDay}
                      activeOpacity={0.6}
                      disabled={!day || isFuture}
                      onPress={() => day && handleCalendarDayPress(day)}
                    >
                      <View
                        style={[
                          styles.calendarDayInner,
                          attendance && {
                            backgroundColor: bgColor,
                            borderColor: `${iconColor}40`,
                            borderWidth: 1,
                          },
                          isToday && {
                            borderWidth: 1.5,
                            borderColor: course?.color || colors.primary.blue,
                          },
                        ]}
                      >
                        <Text
                          style={[
                            styles.calendarDayText,
                            isToday && { color: course?.color || colors.primary.blue, fontWeight: "700" },
                            attendance && { color: colors.text.primary, fontWeight: "600" },
                            isFuture && { color: colors.text.muted, opacity: 0.4 }
                          ]}
                        >
                          {format(day, "d")}
                        </Text>
                        {statusIcon && (
                          <View style={[styles.calendarStatusDot, { backgroundColor: iconColor }]} />
                        )}
                      </View>
                    </TouchableOpacity>
                  );
                })}
              </View>
            ))}
          </LinearGradient>
        </BlurView>
      </View>
    );
  };

  if (!course) {
    return (
      <LinearGradient colors={backgroundGradient} style={styles.container}>
        <View style={[styles.loadingContainer, { paddingTop: insets.top + 40 }]}>
          <ActivityIndicator size="large" color={colors.primary.teal} />
          <Text style={styles.loadingText}>Loading course...</Text>
        </View>
      </LinearGradient>
    );
  }

  if (isLoading) {
    return (
      <LinearGradient colors={backgroundGradient} style={styles.container}>
        <View style={[styles.loadingContainer, { paddingTop: insets.top + 40 }]}>
          <ActivityIndicator size="large" color={course.color} />
        </View>
      </LinearGradient>
    );
  }

  return (
    <LinearGradient colors={backgroundGradient} style={styles.container}>
      <ScrollView
        style={styles.content}
        contentContainerStyle={{ paddingBottom: insets.bottom + 32 }}
        showsVerticalScrollIndicator={false}
      >
        {/* Hero Glass Card */}
        <View style={[styles.heroCard, { paddingTop: insets.top + 24 }]}>
          <BlurView intensity={isDark ? 30 : 50} tint={isDark ? "dark" : "light"} style={styles.heroBlur}>
            <LinearGradient
              colors={[
                `${course.color}33`, // 20% opacity tint
                `${course.color}12`, // 7% opacity tint
              ]}
              start={{ x: 0, y: 0 }}
              end={{ x: 1, y: 1 }}
              style={[styles.heroGradient, { borderColor: `${course.color}50`, borderWidth: 1.5 }]}
            >
              <TouchableOpacity style={styles.closeButton} onPress={() => navigation.goBack()}>
                <Ionicons name="close" size={22} color={colors.text.primary} />
              </TouchableOpacity>

              <View style={{ flexDirection: 'row', alignItems: 'center', marginTop: -12, marginBottom: 4 }}>
                <View style={{ width: 12, height: 12, borderRadius: 6, backgroundColor: course.color, marginRight: 8 }} />
                <Text style={[styles.courseName, { color: colors.text.primary, marginTop: 0 }]}>{course.name}</Text>
              </View>
              <Text style={[styles.professorName, { color: colors.text.secondary }]}>{course.professor}</Text>

              {/* Timetable Events as modern horizontal pills */}
              <View style={styles.scheduleInfo}>
                {courseClasses.length ? (
                  courseClasses.map((cls) => (
                    <View key={cls.id} style={styles.schedulePill}>
                      <View style={[styles.dayPillBadge, { backgroundColor: `${course.color}30` }]}>
                        <Text style={[styles.dayPillText, { color: course.color }]}>
                          {cls.dayOfWeek.toUpperCase()}
                        </Text>
                      </View>
                      
                      <Ionicons name="time-outline" size={12} color={colors.text.secondary} style={{ marginLeft: 8, marginRight: 3 }} />
                      <Text style={[styles.scheduleText, { color: colors.text.secondary }]}>
                        {cls.startTime}-{cls.endTime}
                      </Text>
                      
                      <Ionicons name="location-outline" size={12} color={colors.text.secondary} style={{ marginLeft: 8, marginRight: 3 }} />
                      <Text style={[styles.scheduleText, { color: colors.text.secondary }]}>
                        {cls.room || course.room || "N/A"}
                      </Text>
                    </View>
                  ))
                ) : (
                  <Text style={[styles.scheduleText, { color: colors.text.muted }]}>No classes scheduled</Text>
                )}
              </View>
            </LinearGradient>
          </BlurView>
        </View>

        {/* Unified Premium Attendance Dashboard */}
        {attendanceStats.total > 0 ? (
          <View style={styles.dashboardCard}>
            <BlurView intensity={isDark ? 25 : 60} tint={colors.glass.tint} style={styles.dashboardBlur}>
              <LinearGradient
                colors={[`${attendanceAnalysis.riskColor}16`, `${colors.background.surface}`]}
                style={[styles.dashboardGradient, { borderColor: `${attendanceAnalysis.riskColor}30`, borderWidth: 1 }]}
              >
                <View style={styles.dashboardMainRow}>
                  {/* Glass Concentric Rings Percent Module */}
                  <View style={[styles.radialProgressOuter, { borderColor: `${attendanceAnalysis.riskColor}20` }]}>
                    <View style={[styles.radialProgressInner, { borderColor: `${attendanceAnalysis.riskColor}45`, shadowColor: attendanceAnalysis.riskColor }]}>
                      <View style={{ flexDirection: 'row', alignItems: 'baseline' }}>
                        <Text style={[styles.radialProgressText, { color: colors.text.primary }]}>
                          {attendanceAnalysis.currentPercentage}
                        </Text>
                        <Text style={{ fontSize: 9, color: attendanceAnalysis.riskColor, fontWeight: '900', marginLeft: 1 }}>%</Text>
                      </View>
                    </View>
                  </View>

                  {/* Detailed Statistics Insights Column */}
                  <View style={styles.dashboardInsightColumn}>
                    <View style={{ flexDirection: 'row', alignItems: 'center', marginBottom: 4 }}>
                      <View style={[styles.riskBadgeUnified, { backgroundColor: `${attendanceAnalysis.riskColor}20` }]}>
                        <Ionicons
                          name={attendanceAnalysis.riskIcon as any}
                          size={12}
                          color={attendanceAnalysis.riskColor}
                          style={{ marginRight: 4 }}
                        />
                        <Text style={[styles.riskTextUnified, { color: attendanceAnalysis.riskColor }]}>
                          {attendanceAnalysis.riskLevel.toUpperCase()}
                        </Text>
                      </View>
                      <Text style={styles.classRatioText}>
                        {attendanceStats.present}/{attendanceStats.total} classes
                      </Text>
                    </View>

                    <Text style={styles.intelligenceMessageUnified}>
                      {attendanceAnalysis.riskMessage}
                    </Text>

                    {/* Bunk Assistant Predictive Inline Insights */}
                    {bunkAssist && (
                      <Text style={{ fontSize: 12, color: colors.text.secondary, marginTop: 3, fontWeight: "600", letterSpacing: -0.1 }}>
                        💡 {bunkAssist.message}
                      </Text>
                    )}
                  </View>
                </View>

                {/* Actionable Recommendation Ribbon */}
                <View style={[styles.recommendationRibbon, { borderTopColor: colors.glass.stroke }]}>
                  <Ionicons name="sparkles-outline" size={13} color={attendanceAnalysis.riskColor} style={{ marginRight: 6 }} />
                  <Text style={styles.recommendationText}>
                    {attendanceAnalysis.recommendation}
                  </Text>
                </View>
              </LinearGradient>
            </BlurView>
          </View>
        ) : (
          <View style={styles.emptyDashboardCard}>
            <Ionicons name="bar-chart-outline" size={24} color={colors.text.muted} style={{ marginBottom: 8 }} />
            <Text style={{ color: colors.text.muted, fontSize: 14, fontWeight: "500" }}>
              No classes recorded for attendance yet.
            </Text>
          </View>
        )}

        {/* Course Specific Active Deadlines List */}
        <View style={styles.tasksSection}>
          <View style={styles.tasksHeader}>
            <Text style={styles.sectionTitle}>Deadlines</Text>
            {courseTasks.length > 0 && (
              <View style={[styles.tasksCountBadge, { backgroundColor: `${course.color}25` }]}>
                <Text style={[styles.tasksCountText, { color: course.color }]}>{courseTasks.length}</Text>
              </View>
            )}
          </View>
          
          {courseTasks.length > 0 ? (
            courseTasks.map(task => (
              <View key={task.id} style={styles.taskRow}>
                <TouchableOpacity 
                  style={styles.checkboxContainer} 
                  onPress={() => handleToggleTask(task.id)}
                >
                  <View style={[styles.checkbox, { borderColor: course.color }]}>
                    <View style={styles.checkboxDot} />
                  </View>
                </TouchableOpacity>
                <View style={styles.taskBody}>
                  <Text style={styles.taskTitleText}>{task.title}</Text>
                  <Text style={styles.taskDueText}>
                    {task.dueDate ? `Due ${format(new Date(task.dueDate), "MMM d 'at' h:mm a")}` : "No due date"}
                  </Text>
                </View>
              </View>
            ))
          ) : (
            <View style={styles.emptyTasksBox}>
              <Ionicons name="sparkles-outline" size={16} color={colors.text.muted} style={{ marginRight: 6 }} />
              <Text style={styles.emptyTasksText}>All caught up! No pending deadlines.</Text>
            </View>
          )}
        </View>





        {/* Calendar */}
        {renderCalendar()}

        {/* Recent Attendance */}
        {courseAttendance.length > 0 && (
          <View style={styles.recentCard}>
            <BlurView intensity={20} tint={colors.glass.tint} style={styles.recentBlur}>
              <LinearGradient
                colors={[colors.glass.dark, colors.glass.light]}
                style={styles.recentGradient}
              >
                <Text style={styles.sectionTitle}>Recent Attendance</Text>
                {courseAttendance.slice(0, 5).map((attendance, index) => (
                  <View
                    key={attendance.id || index}
                    style={[
                      styles.attendanceItem,
                      index === Math.min(4, courseAttendance.length - 1) && {
                        borderBottomWidth: 0,
                      },
                    ]}
                  >
                    <View style={styles.attendanceDate}>
                      <Text style={styles.attendanceDateText}>
                        {format(parseISO(attendance.date), "MMM d")}
                      </Text>
                      <Text style={styles.attendanceDayText}>
                        {format(parseISO(attendance.date), "EEE")}
                      </Text>
                    </View>
                    <View
                      style={[
                        styles.attendanceStatus,
                        {
                          backgroundColor:
                            attendance.status === "present"
                              ? "#22c55e25"
                              : attendance.status === "absent"
                                ? "#ef444425"
                                : attendance.status === "canceled"
                                  ? "#64748b25"
                                  : "#8b5cf625",
                        },
                      ]}
                    >
                      <Text
                        style={[
                          styles.attendanceStatusText,
                          {
                            color:
                              attendance.status === "present"
                                ? "#22c55e"
                                : attendance.status === "absent"
                                  ? "#ef4444"
                                  : attendance.status === "canceled"
                                    ? "#94a3b8"
                                    : "#8b5cf6",
                          },
                        ]}
                      >
                        {attendance.status.charAt(0).toUpperCase() + attendance.status.slice(1)}
                      </Text>
                    </View>
                  </View>
                ))}
              </LinearGradient>
            </BlurView>
          </View>
        )}
      </ScrollView>

      {/* Floating Glass Retroactive Attendance Sheet */}
      <Modal
        transparent
        animationType="fade"
        visible={showRetroactiveSheet}
        onRequestClose={() => setShowRetroactiveSheet(false)}
      >
        {/* Transparent backdrop allows timetable grid to remain perfectly visible as requested! */}
        <View style={[styles.modalBackdrop, { backgroundColor: "transparent" }]}>
          <TouchableOpacity 
            style={styles.modalDismissArea} 
            activeOpacity={1} 
            onPress={() => setShowRetroactiveSheet(false)}
          />
          
          {/* The floating rectangle card - with heavy elevation shadow and exclusive blur */}
          <View style={[styles.retroactiveCard, { shadowColor: "#000", elevation: 24 }]}>
            <BlurView intensity={100} tint={isDark ? "dark" : "light"} style={styles.retroactiveBlur}>
              <LinearGradient
                colors={[
                  isDark ? 'rgba(23, 27, 35, 0.86)' : 'rgba(255, 255, 255, 0.86)',
                  isDark ? 'rgba(15, 18, 24, 0.94)' : 'rgba(245, 245, 250, 0.94)'
                ]}
                style={[styles.retroactiveGradient, { borderColor: colors.glass.stroke, borderWidth: 1.2 }]}
              >
                <Text style={styles.retroTitle}>Log Attendance</Text>
                <Text style={styles.retroSubtitle}>
                  {selectedCalendarDay ? format(selectedCalendarDay, "EEEE, MMM d, yyyy") : ""}
                </Text>

                <ScrollView style={{ maxHeight: 280, marginTop: 18 }} showsVerticalScrollIndicator={false}>
                  {matchingClassesForDay.length > 0 ? (
                    matchingClassesForDay.map((cls) => {
                      const existing = selectedCalendarDay ? getAttendanceForClassAndDate(cls.id, selectedCalendarDay) : null;
                      return (
                        <View key={cls.id} style={[styles.retroSlotRow, { borderColor: colors.glass.stroke }]}>
                          <View style={styles.slotInfo}>
                            <Ionicons name="time-outline" size={15} color={colors.text.secondary} style={{ marginRight: 6 }} />
                            <Text style={styles.slotTimeText}>
                              {cls.startTime} - {cls.endTime}
                            </Text>
                            {existing && (
                              <View style={[styles.miniStatusBadge, { backgroundColor: `${existing.status === 'present' ? '#22c55e' : existing.status === 'absent' ? '#ef4444' : '#64748b'}25` }]}>
                                <Text style={[styles.miniStatusText, { color: existing.status === 'present' ? '#22c55e' : existing.status === 'absent' ? '#ef4444' : '#64748b' }]}>
                                  {existing.status.toUpperCase()}
                                </Text>
                              </View>
                            )}
                          </View>
                          
                          {/* Generously sized status button row */}
                          <View style={styles.retroButtonGroup}>
                            <TouchableOpacity 
                              onPress={() => handleSaveRetroactiveAttendance(cls.id, "present")}
                              style={[styles.retroBtn, { backgroundColor: '#22c55e15' }, existing?.status === 'present' && { borderWidth: 1.5, borderColor: '#22c55e' }]}
                            >
                              <Ionicons name="checkmark" size={18} color="#22c55e" />
                            </TouchableOpacity>
                            
                            <TouchableOpacity 
                              onPress={() => handleSaveRetroactiveAttendance(cls.id, "absent")}
                              style={[styles.retroBtn, { backgroundColor: '#ef444415' }, existing?.status === 'absent' && { borderWidth: 1.5, borderColor: '#ef4444' }]}
                            >
                              <Ionicons name="close" size={18} color="#ef4444" />
                            </TouchableOpacity>
                            
                            <TouchableOpacity 
                              onPress={() => handleSaveRetroactiveAttendance(cls.id, "canceled")}
                              style={[styles.retroBtn, { backgroundColor: '#64748b15' }, existing?.status === 'canceled' && { borderWidth: 1.5, borderColor: '#64748b' }]}
                            >
                              <Ionicons name="ban" size={16} color="#64748b" />
                            </TouchableOpacity>
                            
                            {existing && (
                              <TouchableOpacity 
                                onPress={() => handleSaveRetroactiveAttendance(cls.id, "clear")}
                                style={[styles.retroBtn, { backgroundColor: colors.glass.light, borderWidth: 0.8, borderColor: colors.glass.stroke }]}
                              >
                                <Ionicons name="trash-outline" size={16} color={colors.text.muted} />
                              </TouchableOpacity>
                            )}
                          </View>
                        </View>
                      );
                    })
                  ) : (
                    /* Elegant custom replacement for system No Scheduled Slot Alert */
                    <View style={styles.retroNoSlotBox}>
                      <View style={[styles.retroNoSlotIconBg, { backgroundColor: colors.glass.light }]}>
                        <Ionicons name="calendar-outline" size={26} color={colors.text.muted} />
                      </View>
                      <Text style={styles.retroNoSlotTitle}>No Scheduled Slot</Text>
                      <Text style={styles.retroNoSlotDesc}>
                        No regular classes are scheduled for {course?.shortName || 'this course'} on {selectedCalendarDay ? format(selectedCalendarDay, "EEEE") + "s" : "this weekday"}.
                      </Text>
                    </View>
                  )}
                </ScrollView>

                <TouchableOpacity 
                  style={[styles.retroCloseBtn, { backgroundColor: colors.glass.light, borderColor: colors.glass.stroke, borderWidth: 0.8 }]} 
                  onPress={() => setShowRetroactiveSheet(false)}
                >
                  <Text style={styles.retroCloseBtnText}>Close</Text>
                </TouchableOpacity>
              </LinearGradient>
            </BlurView>
          </View>
        </View>
      </Modal>
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
      justifyContent: "center",
      alignItems: "center",
    },
    loadingText: {
      color: colors.text.secondary,
      marginTop: 12,
    },
    content: {
      flex: 1,
    },
    heroCard: {
      margin: 16,
      borderRadius: 24,
      overflow: "hidden",
      shadowColor: colors.shadow,
      shadowOffset: { width: 0, height: 10 },
      shadowOpacity: 0.22,
      shadowRadius: 18,
      elevation: 12,
    },
    heroBlur: {
      borderRadius: 24,
      overflow: "hidden",
    },
    heroGradient: {
      padding: 20,
      paddingTop: 24,
      borderRadius: 24,
    },
    closeButton: {
      alignSelf: "flex-end",
      width: 32,
      height: 32,
      borderRadius: 16,
      backgroundColor: colors.glass.light,
      alignItems: "center",
      justifyContent: "center",
      zIndex: 10,
    },
    courseName: {
      fontSize: 24,
      fontWeight: "800",
      color: colors.text.primary,
      letterSpacing: -0.5,
    },
    professorName: {
      fontSize: 15,
      fontWeight: "500",
      color: colors.text.secondary,
      marginBottom: 16,
      paddingLeft: 20,
    },
    scheduleInfo: {
      gap: 6,
    },
    schedulePill: {
      flexDirection: "row",
      alignItems: "center",
      backgroundColor: colors.glass.light,
      paddingRight: 12,
      borderRadius: 12,
      borderWidth: 0.8,
      borderColor: colors.glass.stroke,
      alignSelf: "flex-start",
      overflow: "hidden",
    },
    dayPillBadge: {
      paddingHorizontal: 10,
      paddingVertical: 6,
      borderRadius: 10,
      justifyContent: "center",
      alignItems: "center",
    },
    dayPillText: {
      fontSize: 11,
      fontWeight: "800",
      letterSpacing: 0.5,
    },
    scheduleText: {
      color: colors.text.secondary,
      fontSize: 13,
      fontWeight: "600",
    },
    
    // Premium Unified Dashboard
    dashboardCard: {
      marginHorizontal: 16,
      marginBottom: 16,
      borderRadius: 20,
      overflow: "hidden",
      shadowColor: colors.shadow,
      shadowOffset: { width: 0, height: 6 },
      shadowOpacity: 0.14,
      shadowRadius: 10,
      elevation: 8,
    },
    dashboardBlur: {
      borderRadius: 20,
    },
    dashboardGradient: {
      padding: 18,
      borderRadius: 20,
    },
    dashboardMainRow: {
      flexDirection: "row",
      alignItems: "center",
    },
    radialProgressOuter: {
      width: 72,
      height: 72,
      borderRadius: 36,
      borderWidth: 3,
      alignItems: "center",
      justifyContent: "center",
      backgroundColor: "rgba(255, 255, 255, 0.02)",
    },
    radialProgressInner: {
      width: 60,
      height: 60,
      borderRadius: 30,
      borderWidth: 3.5,
      alignItems: "center",
      justifyContent: "center",
      backgroundColor: "rgba(255, 255, 255, 0.03)",
      shadowOffset: { width: 0, height: 0 },
      shadowOpacity: 0.3,
      shadowRadius: 3,
    },
    radialProgressText: {
      fontSize: 19,
      fontWeight: "900",
      letterSpacing: -0.5,
    },
    dashboardInsightColumn: {
      flex: 1,
      marginLeft: 16,
    },
    riskBadgeUnified: {
      flexDirection: "row",
      alignItems: "center",
      paddingHorizontal: 8,
      paddingVertical: 4,
      borderRadius: 10,
    },
    riskTextUnified: {
      fontWeight: "800",
      fontSize: 10,
      letterSpacing: 0.5,
    },
    classRatioText: {
      fontSize: 12,
      fontWeight: "600",
      color: colors.text.muted,
      marginLeft: 8,
    },
    intelligenceMessageUnified: {
      fontSize: 17,
      fontWeight: "700",
      color: colors.text.primary,
      letterSpacing: -0.2,
    },
    recommendationRibbon: {
      flexDirection: "row",
      alignItems: "center",
      marginTop: 14,
      paddingTop: 12,
      borderTopWidth: 0.8,
    },
    recommendationText: {
      flex: 1,
      fontSize: 13,
      color: colors.text.secondary,
      lineHeight: 18,
      fontWeight: "500",
    },
    emptyDashboardCard: {
      marginHorizontal: 16,
      marginBottom: 16,
      paddingVertical: 24,
      alignItems: "center",
      justifyContent: "center",
      backgroundColor: colors.glass.light,
      borderColor: colors.glass.stroke,
      borderWidth: 1,
      borderStyle: "dashed",
      borderRadius: 20,
    },

    // Calendar Card
    calendarCard: {
      marginHorizontal: 16,
      marginBottom: 16,
      borderRadius: 20,
      overflow: "hidden",
    },
    calendarBlur: {
      borderRadius: 20,
    },
    calendarGradient: {
      padding: 16,
      borderRadius: 20,
      borderWidth: 1,
      borderColor: colors.glass.border,
    },
    calendarDayInner: {
      width: 32,
      height: 32,
      borderRadius: 16,
      alignItems: "center",
      justifyContent: "center",
      position: "relative",
    },
    calendarStatusDot: {
      width: 4,
      height: 4,
      borderRadius: 2,
      position: "absolute",
      bottom: 3,
    },
    calendarHeader: {
      flexDirection: "row",
      justifyContent: "space-between",
      alignItems: "center",
      marginBottom: 12,
    },
    calendarNavBtn: {
      width: 36,
      height: 36,
      borderRadius: 18,
      backgroundColor: colors.glass.light,
      justifyContent: "center",
      alignItems: "center",
    },
    calendarTitle: {
      fontSize: 18,
      fontWeight: "700",
      color: colors.text.primary,
    },
    weekDaysRow: {
      flexDirection: "row",
      marginBottom: 8,
      paddingHorizontal: 4,
    },
    weekDayText: {
      flex: 1,
      textAlign: "center",
      fontSize: 12,
      color: colors.text.muted,
      fontWeight: "500",
    },
    calendarWeek: {
      flexDirection: "row",
      paddingHorizontal: 4,
    },
    calendarDay: {
      flex: 1,
      height: 44,
      justifyContent: "center",
      alignItems: "center",
      borderRadius: 10,
      margin: 2,
    },
    calendarDayText: {
      fontSize: 14,
      color: colors.text.primary,
    },
    recentCard: {
      marginHorizontal: 16,
      marginBottom: 24,
      borderRadius: 20,
      overflow: "hidden",
    },
    recentBlur: {
      borderRadius: 20,
    },
    recentGradient: {
      padding: 16,
      borderRadius: 20,
      borderWidth: 1,
      borderColor: colors.glass.border,
    },
    sectionTitle: {
      fontSize: 18,
      fontWeight: "700",
      color: colors.text.primary,
      marginBottom: 14,
    },
    attendanceItem: {
      flexDirection: "row",
      justifyContent: "space-between",
      alignItems: "center",
      paddingVertical: 12,
      borderBottomWidth: 1,
      borderBottomColor: colors.glass.border,
    },
    attendanceDate: {
      flexDirection: "row",
      alignItems: "center",
      gap: 8,
    },
    attendanceDateText: {
      fontSize: 16,
      fontWeight: "600",
      color: colors.text.primary,
    },
    attendanceDayText: {
      fontSize: 14,
      color: colors.text.muted,
    },
    attendanceStatus: {
      paddingHorizontal: 14,
      paddingVertical: 6,
      borderRadius: 20,
    },
    attendanceStatusText: {
      fontSize: 14,
      fontWeight: "700",
    },
    attachmentsCard: {
      marginHorizontal: 16,
      marginBottom: 16,
      borderRadius: 20,
      overflow: "hidden",
    },
    attachmentsBlur: {
      borderRadius: 20,
    },
    attachmentsGradient: {
      padding: 16,
      borderRadius: 20,
      borderWidth: 1,
      borderColor: colors.glass.border,
    },
    attachmentsHeader: {
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "space-between",
      marginBottom: 12,
    },
    addAttachmentBtn: {
      flexDirection: "row",
      alignItems: "center",
      gap: 6,
      paddingHorizontal: 12,
      paddingVertical: 8,
      borderRadius: 12,
      borderWidth: 1,
      borderColor: colors.glass.border,
      backgroundColor: colors.glass.light,
    },
    addAttachmentText: {
      color: colors.text.primary,
      fontWeight: "700",
    },
    emptyAttachmentsText: {
      color: colors.text.muted,
      marginTop: 4,
    },
    attachmentItem: {
      flexDirection: "row",
      alignItems: "center",
      paddingVertical: 10,
      borderBottomWidth: 1,
      borderBottomColor: colors.glass.border,
      gap: 12,
    },
    attachmentIcon: {
      width: 36,
      height: 36,
      borderRadius: 12,
      alignItems: "center",
      justifyContent: "center",
    },
    attachmentMeta: {
      flex: 1,
      gap: 2,
    },
    attachmentTitle: {
      color: colors.text.primary,
      fontWeight: "700",
      fontSize: 14,
    },
    attachmentSub: {
      color: colors.text.muted,
      fontSize: 12,
    },
    attachmentDelete: {
      padding: 8,
    },
    // Task list styling definitions
    tasksSection: {
      marginHorizontal: 16,
      marginBottom: 16,
    },
    tasksHeader: {
      flexDirection: "row",
      alignItems: "center",
      marginBottom: 12,
    },
    tasksCountBadge: {
      paddingHorizontal: 8,
      paddingVertical: 3,
      borderRadius: 8,
      marginLeft: 8,
    },
    tasksCountText: {
      fontSize: 11,
      fontWeight: "800",
    },
    taskRow: {
      flexDirection: "row",
      alignItems: "center",
      backgroundColor: colors.glass.light,
      borderColor: colors.glass.border,
      borderWidth: 0.8,
      padding: 14,
      borderRadius: 16,
      marginBottom: 8,
    },
    checkboxContainer: {
      marginRight: 14,
    },
    checkbox: {
      width: 20,
      height: 20,
      borderRadius: 10,
      borderWidth: 2,
      alignItems: "center",
      justifyContent: "center",
    },
    checkboxDot: {
      width: 8,
      height: 8,
      borderRadius: 4,
      backgroundColor: "transparent",
    },
    taskBody: {
      flex: 1,
    },
    taskTitleText: {
      fontSize: 15,
      fontWeight: "700",
      color: colors.text.primary,
      letterSpacing: -0.2,
    },
    taskDueText: {
      fontSize: 12,
      color: colors.text.muted,
      marginTop: 2,
      fontWeight: "500",
    },
    emptyTasksBox: {
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "center",
      backgroundColor: colors.glass.light,
      borderColor: colors.glass.stroke,
      borderWidth: 0.8,
      paddingVertical: 14,
      borderRadius: 16,
      borderStyle: "dashed",
    },
    emptyTasksText: {
      color: colors.text.muted,
      fontSize: 13,
      fontWeight: "500",
    },

    // Modal Backdrop & Float Styling
    modalBackdrop: {
      flex: 1,
      justifyContent: "center",
      alignItems: "center",
    },
    modalDismissArea: {
      ...StyleSheet.absoluteFillObject,
    },
    retroactiveCard: {
      width: "85%",
      borderRadius: 24,
      overflow: "hidden",
      shadowColor: "#000",
      shadowOffset: { width: 0, height: 8 },
      shadowOpacity: 0.2,
      shadowRadius: 12,
      elevation: 16,
    },
    retroactiveBlur: {
      borderRadius: 24,
    },
    retroactiveGradient: {
      padding: 20,
      borderRadius: 24,
    },
    retroTitle: {
      fontSize: 18,
      fontWeight: "800",
      color: colors.text.primary,
    },
    retroSubtitle: {
      fontSize: 13,
      color: colors.text.secondary,
      marginTop: 2,
      fontWeight: "500",
    },
    retroSlotRow: {
      borderTopWidth: 0.8,
      paddingVertical: 14,
    },
    slotInfo: {
      flexDirection: "row",
      alignItems: "center",
      marginBottom: 10,
    },
    slotTimeText: {
      fontSize: 13,
      fontWeight: "700",
      color: colors.text.primary,
    },
    miniStatusBadge: {
      paddingHorizontal: 6,
      paddingVertical: 2,
      borderRadius: 6,
      marginLeft: 8,
    },
    miniStatusText: {
      fontSize: 9,
      fontWeight: "800",
      letterSpacing: 0.5,
    },
    retroButtonGroup: {
      flexDirection: "row",
      gap: 8,
    },
    retroBtn: {
      width: 44,
      height: 44,
      borderRadius: 12,
      alignItems: "center",
      justifyContent: "center",
    },
    retroCloseBtn: {
      marginTop: 16,
      paddingVertical: 12,
      borderRadius: 14,
      alignItems: "center",
    },
    retroCloseBtnText: {
      fontSize: 14,
      fontWeight: "700",
      color: colors.text.primary,
    },
    // Custom Empty State Placeholder styles
    retroNoSlotBox: {
      alignItems: "center",
      paddingVertical: 24,
    },
    retroNoSlotIconBg: {
      width: 52,
      height: 52,
      borderRadius: 26,
      alignItems: "center",
      justifyContent: "center",
      marginBottom: 12,
      borderWidth: 0.8,
      borderColor: colors.glass.stroke,
    },
    retroNoSlotTitle: {
      fontSize: 16,
      fontWeight: "800",
      color: colors.text.primary,
      letterSpacing: -0.2,
    },
    retroNoSlotDesc: {
      fontSize: 13,
      color: colors.text.secondary,
      textAlign: "center",
      marginTop: 6,
      paddingHorizontal: 16,
      lineHeight: 18,
      fontWeight: "500",
    },
  });

export default CourseDetailsScreen;
