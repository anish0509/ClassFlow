export interface ClassTime {
  start: string;
  end: string;
}

export interface Class {
  id: string;
  name: string;
  shortName: string;
  professor: string;
  room: string;
  time: ClassTime;
  day: string;
  date: string;
  semester: string;
  completed: boolean;
  completedAt?: string;
  color: string;
  courseId?: string;
}

export interface ClassSchedule {
  id: string;
  courseId: string;
  courseName?: string;
  shortName?: string;
  professor?: string;
  color?: string;
  dayOfWeek: string;
  startTime: string;
  endTime: string;
  room: string;
  semesterId: string;
  // Runtime injected properties
  attended?: boolean;
  attendanceStatus?: "present" | "absent" | "canceled" | "shifted";
  attendanceId?: string;
  isShifted?: boolean;
  originalClassId?: string;
  originalDate?: string;
}

export interface Course {
  id: string;
  name: string;
  shortName: string;
  professor: string;
  credits: number;
  room: string;
  color: string;
  semesterId?: string;
  totalClasses?: number;
  attendedClasses?: number;
}

export interface Semester {
  id: string;
  name: string;
  startDate: string;
  endDate: string;
  isActive: boolean | number;
  totalWeeks?: number;
}

export interface Attendance {
  id: string;
  classId: string;
  courseId: string;
  courseName?: string;
  shortName?: string;
  color?: string;
  date: string;
  status: "present" | "absent" | "canceled" | "shifted";
  markedAt: string;
  notes?: string;
  startTime?: string;
  endTime?: string;
  room?: string;
  dayOfWeek?: string;
  shiftedToDate?: string; // For shifted classes
}

export interface Task {
  id: string;
  courseId?: string;
  courseName?: string;
  shortName?: string;
  color?: string;
  title: string;
  description?: string;
  dueDate?: string;
  priority: "high" | "medium" | "low";
  status: "pending" | "completed";
  completed?: boolean;
  createdAt: string;
  completedAt?: string;
}

export interface Settings {
  notifications: boolean; // Global or Upcoming enabled
  darkMode: boolean;
  currentSemester: string;
  studentName?: string;
  studentEmail?: string;
  travelBufferMinutes?: number;
  taskReminderMinutes?: number;
  holidayDates?: string[];
  todayScheduleEnabled?: boolean;
  todayScheduleTime?: string; // "HH:MM"
}

export interface Attachment {
  id: string;
  courseId: string;
  title: string;
  url: string;
  type: "link" | "file" | "zoom" | "other";
  createdAt: string;
}

export type DayOfWeek = "SUN" | "MON" | "TUE" | "WED" | "THU" | "FRI" | "SAT";
export type AttendanceStatus =
  | "present"
  | "absent"
  | "canceled"
  | "shifted";

export interface WeekDay {
  day: DayOfWeek;
  date: number;
  fullDate: string;
  isToday: boolean;
}

export interface AttendanceStats {
  total: number;
  present: number;
  absent: number;
  canceled: number;
  percentage: number;
}
