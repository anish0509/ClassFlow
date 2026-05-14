import { Platform } from "react-native";
import * as Notifications from "expo-notifications";
import { ClassSchedule, Task } from "../types";

// Basic channel configuration for Android to ensure alerts fire
const ANDROID_CHANNEL_ID = "reminders";

let handlerConfigured = false;

export const initializeNotifications = async (): Promise<boolean> => {
  if (!handlerConfigured) {
    Notifications.setNotificationHandler({
      handleNotification: async () => ({
        shouldShowAlert: true,
        shouldPlaySound: true,
        shouldSetBadge: true,
        shouldShowBanner: true,
        shouldShowList: true,
      }),
    });
    handlerConfigured = true;
  }

  const settings = await Notifications.getPermissionsAsync();
  if (settings.status !== "granted") {
    const request = await Notifications.requestPermissionsAsync();
    if (request.status !== "granted") {
      return false;
    }
  }

  if (Platform.OS === "android") {
    await Notifications.setNotificationChannelAsync(ANDROID_CHANNEL_ID, {
      name: "Reminders",
      importance: Notifications.AndroidImportance.HIGH,
      sound: undefined,
      vibrationPattern: [0, 250, 250, 250],
      lockscreenVisibility: Notifications.AndroidNotificationVisibility.PUBLIC,
    });
  }

  return true;
};

const withinHolidays = (dateISO: string, holidays: string[] = []): boolean => {
  return holidays.includes(dateISO);
};

const buildDateFromTime = (dateISO: string, time: string): Date => {
  // time format: HH:mm or HH:mm:ss
  const normalized = time.length === 5 ? `${time}:00` : time;
  return new Date(`${dateISO}T${normalized}`);
};

export const scheduleClassReminder = async (
  classItem: ClassSchedule,
  dateISO: string,
  bufferMinutes: number,
  holidays: string[],
): Promise<string | null> => {
  if (withinHolidays(dateISO, holidays)) return null;

  const triggerDate = buildDateFromTime(dateISO, classItem.startTime);
  triggerDate.setMinutes(triggerDate.getMinutes() - bufferMinutes);

  if (triggerDate <= new Date()) return null;

  const trigger: Notifications.DateTriggerInput = {
    type: Notifications.SchedulableTriggerInputTypes.DATE,
    date: triggerDate,
    channelId: ANDROID_CHANNEL_ID,
  };

  const id = await Notifications.scheduleNotificationAsync({
    content: {
      title: `${classItem.shortName || classItem.courseName || "Class"} starts soon`,
      body: `${classItem.startTime} in ${classItem.room || "room"}`,
      data: { classId: classItem.id, courseId: classItem.courseId },
    },
    trigger,
  });
  return id;
};

const dayMap: Record<string, number> = {
  SUN: 1, MON: 2, TUE: 3, WED: 4, THU: 5, FRI: 6, SAT: 7
};

export const scheduleRecurringClassReminder = async (
  classItem: ClassSchedule,
  bufferMinutes: number,
): Promise<string | null> => {
  try {
    const dayOfWeekStr = classItem.dayOfWeek.toUpperCase().trim();
    let weekday = dayMap[dayOfWeekStr];
    if (!weekday) return null;

    // Parse HH:MM
    const [hourStr, minStr] = classItem.startTime.split(":");
    let triggerHour = parseInt(hourStr, 10);
    let triggerMin = parseInt(minStr, 10);

    // Subtract buffer
    triggerMin -= bufferMinutes;
    while (triggerMin < 0) {
      triggerMin += 60;
      triggerHour -= 1;
    }
    while (triggerHour < 0) {
      triggerHour += 24;
      weekday -= 1;
      if (weekday < 1) {
        weekday = 7; // Wrap back to Saturday if went past Sunday
      }
    }

    const trigger: Notifications.WeeklyTriggerInput = {
      type: Notifications.SchedulableTriggerInputTypes.WEEKLY,
      weekday: weekday,
      hour: triggerHour,
      minute: triggerMin,
      channelId: ANDROID_CHANNEL_ID,
    };

    const id = await Notifications.scheduleNotificationAsync({
      content: {
        title: `${classItem.shortName || classItem.courseName || "Class"} begins soon`,
        body: `${classItem.startTime} in ${classItem.room || "room"}`,
        data: { classId: classItem.id, type: 'recurring' },
      },
      trigger,
    });
    return id;
  } catch (e) {
    console.error("Failed to schedule recurring notification:", e);
    return null;
  }
};

export const scheduleDailySummaryReminder = async (
  timeStr: string // "HH:mm"
): Promise<string | null> => {
  try {
    const [hourStr, minStr] = timeStr.split(":");
    const hour = parseInt(hourStr, 10);
    const minute = parseInt(minStr, 10);

    const trigger: Notifications.DailyTriggerInput = {
      type: Notifications.SchedulableTriggerInputTypes.DAILY,
      hour,
      minute,
      channelId: ANDROID_CHANNEL_ID,
    };

    const id = await Notifications.scheduleNotificationAsync({
      content: {
        title: "📋 Good Morning! Today's Schedule",
        body: "Tap to view today's classes and planned activities.",
        data: { type: 'daily_summary' },
      },
      trigger,
    });
    return id;
  } catch (error) {
    console.error("Failed to schedule daily summary:", error);
    return null;
  }
};

export const scheduleTaskReminder = async (
  task: Task,
  minutesBefore: number,
  holidays: string[],
): Promise<string | null> => {
  if (!task.dueDate) return null;
  if (withinHolidays(task.dueDate, holidays)) return null;

  // If no time is stored, default to 09:00 local
  const dueDateTime = buildDateFromTime(task.dueDate, "09:00");
  const triggerDate = new Date(dueDateTime.getTime() - minutesBefore * 60000);

  if (triggerDate <= new Date()) return null;

  const trigger: Notifications.DateTriggerInput = {
    type: Notifications.SchedulableTriggerInputTypes.DATE,
    date: triggerDate,
    channelId: ANDROID_CHANNEL_ID,
  };

  const id = await Notifications.scheduleNotificationAsync({
    content: {
      title: task.title,
      body: task.courseName
        ? `${task.courseName} due ${task.dueDate}`
        : `Due ${task.dueDate}`,
      data: { taskId: task.id, courseId: task.courseId },
    },
    trigger,
  });

  return id;
};

export const cancelAllReminders = async () => {
  await Notifications.cancelAllScheduledNotificationsAsync();
};
