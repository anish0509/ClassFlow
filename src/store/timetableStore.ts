import { create } from "zustand";
import {
  Class,
  Course,
  Semester,
  Settings,
  Task,
  Attendance,
  ClassSchedule,
} from "../types";
import * as db from "../database";
import {
  cancelAllReminders,
  initializeNotifications,
  scheduleClassReminder,
  scheduleRecurringClassReminder,
  scheduleDailySummaryReminder,
  scheduleTaskReminder,
} from "../utils/notifications";
import NetInfo from "@react-native-community/netinfo";
import {
  PendingAction,
  clearSynced,
  enqueueAction,
  loadQueue,
} from "../utils/offlineQueue";

interface TimetableState {
  // Data
  courses: Course[];
  semesters: Semester[];
  todaysClasses: ClassSchedule[];
  weekClasses: Record<string, ClassSchedule[]>;
  completedAttendance: Attendance[];
  tasks: Task[];
  settings: Settings;
  currentWeek: number;
  isLoading: boolean;
  isInitialized: boolean;
  scheduledNotificationIds: string[];
  pendingActions: PendingAction[];
  isOffline: boolean;

  // Initialize
  initializeApp: () => Promise<void>;

  // Course actions
  loadCourses: () => Promise<void>;
  addCourse: (
    course: Omit<Course, "totalClasses" | "attendedClasses" | "id"> & {
      id?: string;
    },
  ) => Promise<void>;
  updateCourse: (id: string, updates: Partial<Course>) => Promise<void>;
  deleteCourse: (id: string) => Promise<void>;

  // Class schedule actions
  loadTodaysClasses: () => Promise<void>;
  loadWeekClasses: () => Promise<void>;
  loadWeekClassesWithAttendance: (weekDates: Record<string, string>) => Promise<void>;
  loadClassesForDay: (dayOfWeek: string, dateStr: string) => Promise<void>;
  selectedDayClasses: ClassSchedule[];
  addClass: (
    classData: Omit<
      ClassSchedule,
      "courseName" | "shortName" | "professor" | "color" | "id"
    > & { id?: string },
  ) => Promise<void>;
  updateClass: (id: string, updates: Partial<ClassSchedule>) => Promise<void>;
  deleteClass: (id: string) => Promise<void>;

  // Attendance actions
  markAttendance: (
    classId: string,
    courseId: string,
    date: string,
    status: "present" | "absent" | "canceled" | "shifted",
    notes?: string,
    shiftedToDate?: string,
  ) => Promise<void>;
  shiftClass: (
    classId: string,
    courseId: string,
    originalDate: string,
    shiftedToDate: string,
    startTime: string,
    endTime: string,
    room?: string,
    isShiftedInstance?: boolean,
    realOriginalClassId?: string,
    realOriginalDate?: string,
  ) => Promise<void>;
  removeAttendance: (id: string) => Promise<void>;
  unshiftClass: (shiftedClassId: string, originalClassId: string, originalDate: string) => Promise<void>;
  loadAttendance: () => Promise<void>;
  getAttendanceStats: (courseId: string) => Promise<{
    total: number;
    present: number;
    absent: number;
    canceled: number;
  }>;
  getShiftedClassesForDate: (date: string) => Promise<any[]>;

  // Task actions
  loadTasks: () => Promise<void>;
  addTask: (
    task: Omit<
      Task,
      | "id"
      | "createdAt"
      | "status"
      | "completedAt"
      | "courseName"
      | "shortName"
      | "color"
    >,
  ) => Promise<void>;
  updateTask: (id: string, updates: Partial<Task>) => Promise<void>;
  completeTask: (id: string) => Promise<void>;
  uncompleteTask: (id: string) => Promise<void>;
  deleteTask: (id: string) => Promise<void>;

  // Semester actions
  loadSemesters: () => Promise<void>;
  setCurrentSemester: (semesterId: string) => Promise<void>;
  addSemester: (semester: Omit<Semester, "isActive">) => Promise<void>;

  // Settings actions
  toggleNotifications: () => Promise<void>;
  toggleDarkMode: () => Promise<void>;
  setTodayScheduleEnabled: (enabled: boolean) => Promise<void>;
  setTodayScheduleTime: (time: string) => Promise<void>;
  setCurrentWeek: (week: number) => void;
  updateProfile: (name: string, email: string) => Promise<void>;
  setTravelBufferMinutes: (minutes: number) => Promise<void>;
  setTaskReminderMinutes: (minutes: number) => Promise<void>;
  loadPendingActions: () => Promise<void>;
  enqueuePendingAction: (type: string, payload: any) => Promise<void>;
  flushPendingActions: () => Promise<void>;
  startNetworkWatcher: () => Promise<void>;
  scheduleTodayReminders: () => Promise<void>;

  // Utility
  refreshAll: () => Promise<void>;
  clearAllData: () => Promise<void>;
  exportData: () => Promise<{
    semesters: any[];
    courses: any[];
    classes: any[];
    tasks: any[];
    attendance: any[];
    attachments: any[];
  }>;
  importData: (data: any) => Promise<void>;
}

const generateUUID = () => {
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === "x" ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
};

const timeToMinutes = (time: string) => {
  const [h, m] = time.split(":").map((v) => parseInt(v, 10) || 0);
  return h * 60 + m;
};

const formatClashLabel = (cls: ClassSchedule) => {
  const name = cls.shortName || cls.courseName || "Class";
  return `${name} (${cls.startTime}-${cls.endTime}${cls.room ? ` - ${cls.room}` : ""})`;
};

const checkClassConflicts = async (
  candidate: {
    id?: string;
    dayOfWeek: string;
    startTime: string;
    endTime: string;
    room?: string;
    semesterId?: string;
  },
): Promise<{ overlaps: ClassSchedule[]; roomClashes: ClassSchedule[] }> => {
  const semesterId = candidate.semesterId || (await db.getActiveSemester())?.id;
  if (!semesterId) return { overlaps: [], roomClashes: [] };

  const existing = (await db.getClassesForDay(
    candidate.dayOfWeek,
    semesterId,
  )) as ClassSchedule[];

  const start = timeToMinutes(candidate.startTime);
  const end = timeToMinutes(candidate.endTime);
  const overlaps: ClassSchedule[] = [];
  const roomClashes: ClassSchedule[] = [];

  existing.forEach((cls) => {
    if (candidate.id && cls.id === candidate.id) return;
    const clsStart = timeToMinutes(cls.startTime);
    const clsEnd = timeToMinutes(cls.endTime);
    const isOverlap = start < clsEnd && end > clsStart;
    if (isOverlap) {
      overlaps.push(cls);
      if (
        candidate.room &&
        cls.room &&
        candidate.room.trim().toLowerCase() === cls.room.trim().toLowerCase()
      ) {
        roomClashes.push(cls);
      }
    }
  });

  return { overlaps, roomClashes };
};

const buildClashMessage = (
  overlaps: ClassSchedule[],
  roomClashes: ClassSchedule[],
) => {
  if (overlaps.length === 0) return "";
  const list = overlaps.map(formatClashLabel).join("\n");
  if (roomClashes.length > 0) {
    return `Clash detected:\n${list}\n\nRoom conflict with ${roomClashes
      .map((c) => c.room)
      .filter(Boolean)
      .join(", ")}`;
  }
  return `Clash detected:\n${list}`;
};

const findClassById = async (
  classId: string,
): Promise<ClassSchedule | undefined> => {
  const all = (await db.getAllClasses()) as ClassSchedule[];
  return all.find((cls) => cls.id === classId);
};

const getDayOfWeek = (date: Date): string => {
  const days = ["SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"];
  return days[date.getDay()];
};

let netInfoUnsubscribe: (() => void) | null = null;

const queueIfOffline = async (type: string, payload: any, enqueue: (t: string, p: any) => Promise<void>, isOffline: boolean) => {
  if (!isOffline) return;
  await enqueue(type, payload);
};

export const useTimetableStore = create<TimetableState>((set, get) => ({
  // Initial state
  courses: [],
  semesters: [],
  todaysClasses: [],
  selectedDayClasses: [],
  weekClasses: { SUN: [], MON: [], TUE: [], WED: [], THU: [], FRI: [], SAT: [] },
  completedAttendance: [],
  tasks: [],
  currentWeek: 1,
  isLoading: false,
  isInitialized: false,
  scheduledNotificationIds: [],
  pendingActions: [],
  isOffline: false,
  settings: {
    notifications: true,
    darkMode: true,
    currentSemester: "",
    studentName: "Student Name",
    studentEmail: "student@university.edu",
    travelBufferMinutes: 15,
    taskReminderMinutes: 60,
    holidayDates: [],
  },

  // Initialize app
  initializeApp: async () => {
    try {
      set({ isLoading: true });
      await db.initDatabase();

      // Load semesters first (needed for semester-scoped queries)
      await get().loadSemesters();

      // Load all data + settings in parallel for fast startup
      const [
        ,, // loadCourses, loadTodaysClasses results (set in store)
        ,, // loadWeekClasses, loadAttendance
        ,  // loadTasks
        notifications, darkMode, currentSemester,
        studentName, studentEmail, travelBufferMinutes,
        taskReminderMinutes, holidayDates,
        todayScheduleEnabled, todayScheduleTime,
        activeSemester,
      ] = await Promise.all([
        get().loadCourses(),
        get().loadTodaysClasses(),
        get().loadWeekClasses(),
        get().loadAttendance(),
        get().loadTasks(),
        db.getSetting("notifications"),
        db.getSetting("darkMode"),
        db.getSetting("currentSemester"),
        db.getSetting("studentName"),
        db.getSetting("studentEmail"),
        db.getSetting("travelBufferMinutes"),
        db.getSetting("taskReminderMinutes"),
        db.getSetting("holidayDates"),
        db.getSetting("todayScheduleEnabled"),
        db.getSetting("todayScheduleTime"),
        db.getActiveSemester(),
      ]);

      // Auto-calculate current week from semester start date
      let calculatedWeek = 1;
      if (activeSemester?.startDate) {
        const todayStr = new Date().toISOString().split("T")[0];
        const semStartStr = activeSemester.startDate.split("T")[0];
        const todayMs = new Date(todayStr + "T00:00:00Z").getTime();
        const semStartMs = new Date(semStartStr + "T00:00:00Z").getTime();
        const diffDays = Math.floor((todayMs - semStartMs) / (24 * 60 * 60 * 1000));
        calculatedWeek = Math.max(1, Math.floor(diffDays / 7) + 1);
      }

      set({
        currentWeek: calculatedWeek,
        settings: {
          notifications: notifications === "true",
          darkMode: darkMode !== "false",
          currentSemester: currentSemester || "",
          studentName: studentName || "Student Name",
          studentEmail: studentEmail || "student@university.edu",
          travelBufferMinutes: travelBufferMinutes
            ? Number(travelBufferMinutes)
            : 15,
          taskReminderMinutes: taskReminderMinutes
            ? Number(taskReminderMinutes)
            : 60,
          holidayDates: holidayDates ? JSON.parse(holidayDates) : [],
          todayScheduleEnabled: todayScheduleEnabled === "true",
          todayScheduleTime: todayScheduleTime || "08:00",
        },
        isInitialized: true,
        isLoading: false,
      });

      // Non-critical tasks — don't block UI
      get().loadPendingActions();
      get().startNetworkWatcher();
      get().flushPendingActions();
      get().scheduleTodayReminders();
    } catch (error) {
      console.error("Failed to initialize app:", error);
      set({ isLoading: false });
    }
  },

  scheduleTodayReminders: async () => {
    try {
      const { settings, weekClasses, tasks } = get();
      if (!settings.notifications) {
        await cancelAllReminders();
        return;
      }

      const permissionGranted = await initializeNotifications();
      if (!permissionGranted) return;

      // Clear all to rebuild clean schedule
      await cancelAllReminders();

      const buffer = settings.travelBufferMinutes ?? 15;
      const holidays = settings.holidayDates ?? [];
      const notificationIds: string[] = [];

      // 1. Schedule Daily Summary if enabled
      if (settings.todayScheduleEnabled && settings.todayScheduleTime) {
        const summaryId = await scheduleDailySummaryReminder(settings.todayScheduleTime);
        if (summaryId) notificationIds.push(summaryId);
      }

      // 2. Schedule ALL Recurring Classes for the whole week
      const allScheduledClasses = Object.values(weekClasses).flat();
      for (const classItem of allScheduledClasses) {
        const id = await scheduleRecurringClassReminder(classItem, buffer);
        if (id) notificationIds.push(id);
      }

      // 3. Schedule specific discrete upcoming tasks
      const taskLead = settings.taskReminderMinutes ?? 60;
      for (const task of tasks) {
        if (task.status === "completed") continue;
        const id = await scheduleTaskReminder(task, taskLead, holidays);
        if (id) notificationIds.push(id);
      }

      set({ scheduledNotificationIds: notificationIds });
    } catch (error) {
      console.error("Failed to schedule robust recurring reminders:", error);
    }
  },

  // Course actions
  loadCourses: async () => {
    try {
      const activeSemester = await db.getActiveSemester();
      const courses = (await db.getAllCourses(activeSemester?.id)) as Course[];
      set({ courses });
    } catch (error) {
      console.error("Failed to load courses:", error);
    }
  },

  addCourse: async (course) => {
    try {
      const activeSemester = await db.getActiveSemester();
      if (!activeSemester) {
        console.error("No active semester found");
        return;
      }
      const newCourse = {
        ...course,
        id: generateUUID(),
        semesterId: activeSemester.id,
      };
      await db.addCourse(newCourse);
      await queueIfOffline("addCourse", newCourse, get().enqueuePendingAction, get().isOffline);
      await get().loadCourses();
    } catch (error) {
      console.error("Failed to add course:", error);
    }
  },

  updateCourse: async (id, updates) => {
    try {
      await db.updateCourse(id, updates);
      await queueIfOffline(
        "updateCourse",
        { id, updates },
        get().enqueuePendingAction,
        get().isOffline,
      );
      await get().loadCourses();
    } catch (error) {
      console.error("Failed to update course:", error);
    }
  },

  deleteCourse: async (id) => {
    try {
      await db.deleteCourse(id);
      await queueIfOffline(
        "deleteCourse",
        { id },
        get().enqueuePendingAction,
        get().isOffline,
      );
      await get().refreshAll();
    } catch (error) {
      console.error("Failed to delete course:", error);
    }
  },

  // Class schedule actions
  loadTodaysClasses: async () => {
    try {
      const today = new Date();
      const dayOfWeek = getDayOfWeek(today);
      const todayStr = today.toISOString().split("T")[0];
      const activeSemester = await db.getActiveSemester();
      
      // Check if today falls within the active semester (timezone-safe date-string comparison)
      if (activeSemester) {
        const semStartStr = activeSemester.startDate.split("T")[0];
        const semEndStr = activeSemester.endDate.split("T")[0];
        
        if (todayStr < semStartStr || todayStr > semEndStr) {
          set({ todaysClasses: [] });
          return;
        }
      }
      
      // Fire all 4 DB queries in parallel
      const [classes, todayAttendance, shiftedClasses, classesShiftedAway] = await Promise.all([
        db.getClassesForDay(dayOfWeek, activeSemester?.id) as Promise<ClassSchedule[]>,
        db.getAttendanceForDate(todayStr) as Promise<Attendance[]>,
        db.getShiftedClassesForDate(todayStr),
        db.getClassesShiftedFromDate(todayStr),
      ]);

      const shiftedAwayClassIds = new Set(
        classesShiftedAway.map((sc: any) => sc.originalClassId),
      );

      // Filter out classes that have been shifted away from today
      const filteredClasses = classes.filter(
        (c) => !shiftedAwayClassIds.has(c.id),
      );

      const classesWithAttendance = filteredClasses.map((c) => ({
        ...c,
        attended: todayAttendance.some((a) => a.classId === c.id),
        attendanceStatus: todayAttendance.find((a) => a.classId === c.id)?.status,
        attendanceId: todayAttendance.find((a) => a.classId === c.id)?.id,
      }));

      // Add shifted classes that were moved TO today
      const shiftedClassesFormatted = shiftedClasses.map((sc: any) => ({
        id: sc.id,
        courseId: sc.courseId,
        courseName: sc.courseName,
        shortName: sc.shortName,
        professor: sc.professor,
        color: sc.color,
        dayOfWeek: dayOfWeek,
        startTime: sc.startTime,
        endTime: sc.endTime,
        room: sc.room,
        semesterId: activeSemester?.id || "",
        isShifted: true,
        originalClassId: sc.originalClassId,
        originalDate: sc.originalDate,
        attended: todayAttendance.some((a) => a.classId === sc.id),
        attendanceStatus: todayAttendance.find((a) => a.classId === sc.id)?.status,
        attendanceId: todayAttendance.find((a) => a.classId === sc.id)?.id,
      }));

      set({
        todaysClasses: [
          ...classesWithAttendance,
          ...shiftedClassesFormatted,
        ] as ClassSchedule[],
      });
      // Don't await — reminders are non-blocking
      get().scheduleTodayReminders();
    } catch (error) {
      console.error("Failed to load today's classes:", error);
    }
  },

  loadClassesForDay: async (dayOfWeek, dateStr) => {
    try {
      if (!dayOfWeek || !dateStr) {
        set({ selectedDayClasses: [] });
        return;
      }

      const activeSemester = await db.getActiveSemester();
      
      // Check if the date falls within the active semester (timezone-safe date-string comparison)
      if (activeSemester) {
        const selectedDateStr = dateStr.split("T")[0];
        const semStartStr = activeSemester.startDate.split("T")[0];
        const semEndStr = activeSemester.endDate.split("T")[0];
        
        // If date is outside semester range, return empty
        if (selectedDateStr < semStartStr || selectedDateStr > semEndStr) {
          set({ selectedDayClasses: [] });
          return;
        }
      }
      
      // If no active semester, try to get classes without semester filter
      const classes = (await db.getClassesForDay(
        dayOfWeek,
        activeSemester?.id || undefined,
      )) as ClassSchedule[];

      // Check attendance for the selected date
      const dayAttendance = (await db.getAttendanceForDate(
        dateStr,
      )) as Attendance[];

      // Get shifted classes TO this date (to show them)
      const shiftedClasses = await db.getShiftedClassesForDate(dateStr);

      // Get classes shifted FROM this date (to hide them)
      const classesShiftedAway = await db.getClassesShiftedFromDate(dateStr);
      const shiftedAwayClassIds = new Set(
        classesShiftedAway.map((sc: any) => sc.originalClassId),
      );

      // Filter out classes that have been shifted away from this date
      const filteredClasses = classes.filter(
        (c) => !shiftedAwayClassIds.has(c.id),
      );

      const classesWithAttendance = filteredClasses.map((c) => ({
        ...c,
        attended: dayAttendance.some((a) => a.classId === c.id),
        attendanceStatus: dayAttendance.find((a) => a.classId === c.id)?.status,
        attendanceId: dayAttendance.find((a) => a.classId === c.id)?.id,
      }));

      // Add shifted classes to the list
      const shiftedClassesFormatted = shiftedClasses.map((sc: any) => ({
        id: sc.id,
        courseId: sc.courseId,
        courseName: sc.courseName,
        shortName: sc.shortName,
        professor: sc.professor,
        color: sc.color,
        dayOfWeek: dayOfWeek,
        startTime: sc.startTime,
        endTime: sc.endTime,
        room: sc.room,
        semesterId: activeSemester?.id || "",
        isShifted: true,
        originalClassId: sc.originalClassId,
        originalDate: sc.originalDate,
        attended: dayAttendance.some((a) => a.classId === sc.id),
        attendanceStatus: dayAttendance.find((a) => a.classId === sc.id)?.status,
        attendanceId: dayAttendance.find((a) => a.classId === sc.id)?.id,
      }));

      set({
        selectedDayClasses: [
          ...classesWithAttendance,
          ...shiftedClassesFormatted,
        ] as ClassSchedule[],
      });
    } catch (error) {
      console.error("Failed to load classes for day:", error);
    }
  },

  loadWeekClasses: async () => {
    try {
      const activeSemester = await db.getActiveSemester();
      const allClasses = (await db.getAllClasses(
        activeSemester?.id,
      )) as ClassSchedule[];

      const weekClasses: Record<string, ClassSchedule[]> = {
        SUN: [],
        MON: [],
        TUE: [],
        WED: [],
        THU: [],
        FRI: [],
        SAT: [],
      };

      allClasses.forEach((c) => {
        if (weekClasses[c.dayOfWeek]) {
          weekClasses[c.dayOfWeek].push(c);
        }
      });

      set({ weekClasses });
    } catch (error) {
      console.error("Failed to load week classes:", error);
    }
  },

  loadWeekClassesWithAttendance: async (weekDates) => {
    try {
      const activeSemester = await db.getActiveSemester();
      const allClasses = (await db.getAllClasses(
        activeSemester?.id,
      )) as ClassSchedule[];

      // Pre-compute semester bounds once
      const semStartStr = activeSemester?.startDate?.split("T")[0] || "";
      const semEndStr = activeSemester?.endDate?.split("T")[0] || "9999-12-31";

      // Group classes by day (in-memory, instant)
      const classesByDay: Record<string, ClassSchedule[]> = {};
      allClasses.forEach((c) => {
        if (!classesByDay[c.dayOfWeek]) classesByDay[c.dayOfWeek] = [];
        classesByDay[c.dayOfWeek].push(c);
      });

      // Fire ALL per-day DB queries in parallel (3 queries × N days = all at once)
      const entries = Object.entries(weekDates);
      const dayResults = await Promise.all(
        entries.map(async ([day, dateStr]) => {
          const dayClasses = classesByDay[day] || [];

          // Check semester bounds
          if (activeSemester && (dateStr < semStartStr || dateStr > semEndStr)) {
            return { day, classes: [] as any[] };
          }

          // 3 queries per day, all in parallel
          const [dayAttendance, shiftedClasses, classesShiftedAway] = await Promise.all([
            db.getAttendanceForDate(dateStr) as Promise<Attendance[]>,
            db.getShiftedClassesForDate(dateStr),
            db.getClassesShiftedFromDate(dateStr),
          ]);

          const shiftedAwayClassIds = new Set(
            classesShiftedAway.map((sc: any) => sc.originalClassId),
          );

          const filteredClasses = dayClasses.filter(
            (c) => !shiftedAwayClassIds.has(c.id),
          );

          const classesWithAttendance = filteredClasses.map((c) => ({
            ...c,
            attended: dayAttendance.some((a) => a.classId === c.id),
            attendanceStatus: dayAttendance.find((a) => a.classId === c.id)?.status,
            attendanceId: dayAttendance.find((a) => a.classId === c.id)?.id,
          }));

          const shiftedClassesFormatted = shiftedClasses.map((sc: any) => ({
            id: sc.id,
            courseId: sc.courseId,
            courseName: sc.courseName,
            shortName: sc.shortName,
            professor: sc.professor,
            color: sc.color,
            dayOfWeek: day,
            startTime: sc.startTime,
            endTime: sc.endTime,
            room: sc.room,
            semesterId: activeSemester?.id || "",
            isShifted: true,
            originalClassId: sc.originalClassId,
            originalDate: sc.originalDate,
            attended: dayAttendance.some((a) => a.classId === sc.id),
            attendanceStatus: dayAttendance.find((a) => a.classId === sc.id)?.status,
            attendanceId: dayAttendance.find((a) => a.classId === sc.id)?.id,
          }));

          return { day, classes: [...classesWithAttendance, ...shiftedClassesFormatted] };
        }),
      );

      // Assemble result from parallel outputs
      const result: Record<string, ClassSchedule[]> = {};
      dayResults.forEach(({ day, classes }) => {
        result[day] = classes as ClassSchedule[];
      });

      set({ weekClasses: result });
    } catch (error) {
      console.error("Failed to load week classes with attendance:", error);
    }
  },

  addClass: async (classData) => {
    try {
      const activeSemester = classData.semesterId
        ? { id: classData.semesterId }
        : await db.getActiveSemester();

      if (!activeSemester?.id) {
        throw new Error("No active semester found. Add or activate a semester first.");
      }

      const start = timeToMinutes(classData.startTime);
      const end = timeToMinutes(classData.endTime);
      if (start >= end) {
        throw new Error("End time must be after start time.");
      }

      const candidate = {
        ...classData,
        semesterId: activeSemester.id,
      };
      const { overlaps, roomClashes } = await checkClassConflicts(candidate);
      if (overlaps.length > 0) {
        throw new Error(buildClashMessage(overlaps, roomClashes));
      }

      const newClass = {
        ...candidate,
        id: generateUUID(),
      };

      await db.addClass(newClass);
      await queueIfOffline("addClass", newClass, get().enqueuePendingAction, get().isOffline);
      await get().loadTodaysClasses();
      await get().loadWeekClasses();
      await get().scheduleTodayReminders();
    } catch (error) {
      console.error("Failed to add class:", error);
      throw error;
    }
  },

  updateClass: async (id, updates) => {
    try {
      const existing = await findClassById(id);
      if (!existing) {
        throw new Error("Class not found");
      }

      const merged = { ...existing, ...updates };
      const start = timeToMinutes(merged.startTime);
      const end = timeToMinutes(merged.endTime);
      if (start >= end) {
        throw new Error("End time must be after start time.");
      }

      const { overlaps, roomClashes } = await checkClassConflicts({
        ...merged,
        id,
      });
      if (overlaps.length > 0) {
        throw new Error(buildClashMessage(overlaps, roomClashes));
      }

      await db.updateClass(id, updates);
      await queueIfOffline(
        "updateClass",
        { id, updates },
        get().enqueuePendingAction,
        get().isOffline,
      );
      await get().loadTodaysClasses();
      await get().loadWeekClasses();
      await get().scheduleTodayReminders();
    } catch (error) {
      console.error("Failed to update class:", error);
      throw error;
    }
  },

  deleteClass: async (id) => {
    try {
      await db.deleteClass(id);
      await queueIfOffline(
        "deleteClass",
        { id },
        get().enqueuePendingAction,
        get().isOffline,
      );
      await get().loadTodaysClasses();
      await get().loadWeekClasses();
      await get().scheduleTodayReminders();
    } catch (error) {
      console.error("Failed to delete class:", error);
    }
  },

  unshiftClass: async (shiftedClassId, originalClassId, originalDate) => {
    try {
      await db.removeShiftedClass(shiftedClassId);
      await db.removeShiftTombstone(originalClassId, originalDate);
      await queueIfOffline(
        "unshiftClass",
        { shiftedClassId, originalClassId, originalDate },
        get().enqueuePendingAction,
        get().isOffline,
      );
      await get().loadTodaysClasses();
      await get().loadWeekClasses();
      await get().loadAttendance();
    } catch (error) {
      console.error("Failed to unshift class:", error);
    }
  },

  // Attendance actions
  markAttendance: async (
    classId,
    courseId,
    date,
    status,
    notes,
    shiftedToDate,
  ) => {
    try {
      await db.markAttendance({
        id: generateUUID(),
        classId,
        courseId,
        date,
        status,
        notes,
        shiftedToDate,
      });
      await queueIfOffline(
        "markAttendance",
        { classId, courseId, date, status, notes, shiftedToDate },
        get().enqueuePendingAction,
        get().isOffline,
      );
      await get().loadAttendance();
      await get().loadTodaysClasses();
      await get().loadCourses();
    } catch (error) {
      console.error("Failed to mark attendance:", error);
    }
  },

  shiftClass: async (
    classId,
    courseId,
    originalDate,
    shiftedToDate,
    startTime,
    endTime,
    room,
    isShiftedInstance,
    realOriginalClassId,
    realOriginalDate,
  ) => {
    try {
      if (isShiftedInstance && realOriginalClassId && realOriginalDate) {
        if (shiftedToDate === realOriginalDate) {
          // CASE C: Shift Back to Baseline
          await db.removeShiftedClass(classId);
          await db.removeShiftTombstone(realOriginalClassId, realOriginalDate);
        } else {
          // CASE B: Reshift to Another Day (Update Existing)
          await db.updateShiftedClass(classId, shiftedToDate, startTime, endTime, room);
          await db.updateShiftTombstoneDestination(realOriginalClassId, realOriginalDate, shiftedToDate);
        }
      } else {
        // CASE A: Initial Shift
        await db.markAttendance({
          id: generateUUID(),
          classId,
          courseId,
          date: originalDate,
          status: "shifted",
          notes: `Shifted to ${shiftedToDate}`,
          shiftedToDate,
        });

        await db.shiftClass({
          id: generateUUID(),
          originalClassId: classId,
          courseId,
          originalDate,
          shiftedToDate,
          startTime,
          endTime,
          room,
        });
      }

      await queueIfOffline(
        "shiftClass",
        {
          classId,
          courseId,
          originalDate,
          shiftedToDate,
          startTime,
          endTime,
          room,
          isShiftedInstance,
          realOriginalClassId,
          realOriginalDate,
        },
        get().enqueuePendingAction,
        get().isOffline,
      );

      await get().loadAttendance();
      await get().loadTodaysClasses();
      await get().loadWeekClasses();
    } catch (error) {
      console.error("Failed to shift class:", error);
    }
  },

  getShiftedClassesForDate: async (date) => {
    try {
      return await db.getShiftedClassesForDate(date);
    } catch (error) {
      console.error("Failed to get shifted classes:", error);
      return [];
    }
  },

  removeAttendance: async (id) => {
    try {
      await db.removeAttendance(id);
      await get().loadAttendance();
      await get().loadTodaysClasses();
      await get().loadCourses();
    } catch (error) {
      console.error("Failed to remove attendance:", error);
    }
  },

  loadAttendance: async () => {
    try {
      const activeSemester = await db.getActiveSemester();
      const attendance = (await db.getAllAttendance(
        undefined,
        activeSemester?.id,
      )) as Attendance[];
      set({ completedAttendance: attendance });
    } catch (error) {
      console.error("Failed to load attendance:", error);
    }
  },

  getAttendanceStats: async (courseId) => {
    try {
      const stats = await db.getAttendanceStats(courseId);
      return stats || { total: 0, present: 0, absent: 0, canceled: 0 };
    } catch (error) {
      console.error("Failed to get attendance stats:", error);
      return { total: 0, present: 0, absent: 0, canceled: 0 };
    }
  },

  // Task actions
  loadTasks: async () => {
    try {
      const activeSemester = await db.getActiveSemester();
      const tasks = (await db.getAllTasks(
        undefined,
        activeSemester?.id,
      )) as Task[];
      set({ tasks });
      await get().scheduleTodayReminders();
    } catch (error) {
      console.error("Failed to load tasks:", error);
    }
  },

  addTask: async (task) => {
    try {
      const courseMeta = task.courseId
        ? get().courses.find((c) => c.id === task.courseId)
        : undefined;
      const newTask = {
        ...task,
        id: generateUUID(),
        courseName: courseMeta?.name,
        shortName: courseMeta?.shortName,
        color: courseMeta?.color,
        createdAt: new Date().toISOString(),
        status: "pending",
      };
      await db.addTask(newTask);
      await queueIfOffline("addTask", newTask, get().enqueuePendingAction, get().isOffline);
      await get().loadTasks();
      await get().scheduleTodayReminders();
    } catch (error) {
      console.error("Failed to add task:", error);
    }
  },

  updateTask: async (id, updates) => {
    try {
      await db.updateTask(id, updates);
      await queueIfOffline(
        "updateTask",
        { id, updates },
        get().enqueuePendingAction,
        get().isOffline,
      );
      await get().loadTasks();
      await get().scheduleTodayReminders();
    } catch (error) {
      console.error("Failed to update task:", error);
    }
  },

  completeTask: async (id) => {
    try {
      await db.completeTask(id);
      await queueIfOffline(
        "completeTask",
        { id },
        get().enqueuePendingAction,
        get().isOffline,
      );
      await get().loadTasks();
      await get().scheduleTodayReminders();
    } catch (error) {
      console.error("Failed to complete task:", error);
    }
  },

  uncompleteTask: async (id) => {
    try {
      await db.uncompleteTask(id);
      await queueIfOffline(
        "uncompleteTask",
        { id },
        get().enqueuePendingAction,
        get().isOffline,
      );
      await get().loadTasks();
      await get().scheduleTodayReminders();
    } catch (error) {
      console.error("Failed to uncomplete task:", error);
    }
  },

  deleteTask: async (id) => {
    try {
      await db.deleteTask(id);
      await queueIfOffline(
        "deleteTask",
        { id },
        get().enqueuePendingAction,
        get().isOffline,
      );
      await get().loadTasks();
      await get().scheduleTodayReminders();
    } catch (error) {
      console.error("Failed to delete task:", error);
    }
  },

  // Semester actions
  loadSemesters: async () => {
    try {
      const semesters = (await db.getAllSemesters()) as Semester[];
      set({ semesters });
    } catch (error) {
      console.error("Failed to load semesters:", error);
    }
  },

  setCurrentSemester: async (semesterId) => {
    try {
      await db.setActiveSemester(semesterId);
      await db.setSetting("currentSemester", semesterId);
      set((state) => ({
        settings: { ...state.settings, currentSemester: semesterId },
      }));
      await get().refreshAll();
    } catch (error) {
      console.error("Failed to set semester:", error);
    }
  },

  addSemester: async (semester) => {
    try {
      await db.addSemester({
        ...semester,
        id: generateUUID(),
      });
      await get().loadSemesters();
    } catch (error) {
      console.error("Failed to add semester:", error);
    }
  },

  // Settings actions
  toggleNotifications: async () => {
    const newValue = !get().settings.notifications;
    await db.setSetting("notifications", String(newValue));
    set((state) => ({
      settings: { ...state.settings, notifications: newValue },
    }));
  },

  toggleDarkMode: async () => {
    const newValue = !get().settings.darkMode;
    await db.setSetting("darkMode", String(newValue));
    set((state) => ({
      settings: { ...state.settings, darkMode: newValue },
    }));
  },

  setTodayScheduleEnabled: async (enabled: boolean) => {
    await db.setSetting("todayScheduleEnabled", String(enabled));
    set((state) => ({
      settings: { ...state.settings, todayScheduleEnabled: enabled },
    }));
    await get().scheduleTodayReminders();
  },

  setTodayScheduleTime: async (time: string) => {
    await db.setSetting("todayScheduleTime", time);
    set((state) => ({
      settings: { ...state.settings, todayScheduleTime: time },
    }));
    await get().scheduleTodayReminders();
  },

  setTravelBufferMinutes: async (minutes: number) => {
    const clamped = Math.max(5, Math.min(120, minutes));
    await db.setSetting("travelBufferMinutes", String(clamped));
    set((state) => ({
      settings: { ...state.settings, travelBufferMinutes: clamped },
    }));
    await get().scheduleTodayReminders();
  },

  setTaskReminderMinutes: async (minutes: number) => {
    const clamped = Math.max(10, Math.min(240, minutes));
    await db.setSetting("taskReminderMinutes", String(clamped));
    set((state) => ({
      settings: { ...state.settings, taskReminderMinutes: clamped },
    }));
    await get().scheduleTodayReminders();
  },

  setCurrentWeek: (week) => {
    set({ currentWeek: week });
  },

  updateProfile: async (name, email) => {
    await db.setSetting("studentName", name);
    await db.setSetting("studentEmail", email);
    set((state) => ({
      settings: { ...state.settings, studentName: name, studentEmail: email },
    }));
  },

  loadPendingActions: async () => {
    const queue = await loadQueue();
    set({ pendingActions: queue });
  },

  enqueuePendingAction: async (type, payload) => {
    const entry: PendingAction = {
      id: generateUUID(),
      type,
      payload,
      createdAt: new Date().toISOString(),
      status: "pending",
    };
    const next = await enqueueAction(get().pendingActions, entry);
    set({ pendingActions: next });
  },

  flushPendingActions: async () => {
    const { isOffline, pendingActions } = get();
    if (isOffline || pendingActions.length === 0) return;

    // Placeholder sync success - replace with real API sync when available
    const synced = pendingActions.map((action) => ({
      ...action,
      status: "synced" as const,
    }));

    const remaining = await clearSynced(synced);
    set({ pendingActions: remaining });
  },

  startNetworkWatcher: async () => {
    if (netInfoUnsubscribe) return;
    netInfoUnsubscribe = NetInfo.addEventListener((state) => {
      const offline = !(state.isConnected && state.isInternetReachable !== false);
      set({ isOffline: offline });
      if (!offline) {
        get().flushPendingActions();
      }
    });
  },

  // Utility
  refreshAll: async () => {
    await get().loadSemesters();
    // All other loads are independent — run in parallel
    await Promise.all([
      get().loadCourses(),
      get().loadTodaysClasses(),
      get().loadWeekClasses(),
      get().loadAttendance(),
      get().loadTasks(),
    ]);
  },

  clearAllData: async () => {
    try {
      await db.clearAllData();
      await clearSynced([]);
      set({
        semesters: [],
        courses: [],
        todaysClasses: [],
        selectedDayClasses: [],
        weekClasses: { SUN: [], MON: [], TUE: [], WED: [], THU: [], FRI: [], SAT: [] },
        completedAttendance: [],
        tasks: [],
        pendingActions: [],
      });
    } catch (error) {
      console.error("Failed to clear all data:", error);
      throw error;
    }
  },

  exportData: async () => {
    try {
      return await db.exportBackupData();
    } catch (error) {
      console.error("Failed to export data:", error);
      throw error;
    }
  },

  importData: async (data: any) => {
    try {
      await db.restoreBackupData(data);
      // Force reset current semester logic locally just in case
      const active = await db.getActiveSemester();
      if (active) {
        await db.setSetting("currentSemester", active.id);
        set((state) => ({ settings: { ...state.settings, currentSemester: active.id } }));
      }
      await get().refreshAll();
      await get().scheduleTodayReminders();
    } catch (error) {
      console.error("Failed to import data:", error);
      throw error;
    }
  },
}));
