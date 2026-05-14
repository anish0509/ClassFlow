import { Class, Course, Semester } from "../types";

export const COLORS = {
  primary: {
    blue: "#0A84FF",
    purple: "#BF5AF2",
    teal: "#30D5C8",
    indigo: "#5E5CE6",
    pink: "#FF375F",
    cyan: "#64D2FF",
    green: "#30D158",
    orange: "#FF9F0A",
    red: "#FF453A",
  },
  background: {
    start: "#0C0C0E",
    end: "#0C0C0E",
  },
  glass: {
    light: "rgba(255, 255, 255, 0.08)",
    medium: "rgba(255, 255, 255, 0.12)",
    dark: "rgba(255, 255, 255, 0.04)",
    border: "rgba(255, 255, 255, 0.10)",
    cardFill: "rgba(44, 44, 46, 0.55)",
    cardBorder: "rgba(255, 255, 255, 0.10)",
  },
  text: {
    primary: "#ffffff",
    secondary: "rgba(235, 235, 245, 0.60)",
    muted: "rgba(235, 235, 245, 0.30)",
  },
};

export const COURSES: Course[] = [
  {
    id: "1",
    name: "The Indian Constitution",
    shortName: "Indian Const.",
    professor: "Dr. Sharma",
    credits: 3,
    room: "LH-101",
    color: COLORS.primary.blue,
    totalClasses: 45,
    attendedClasses: 32,
  },
  {
    id: "2",
    name: "Mathematical Foundations of Computer Science",
    shortName: "MFCS",
    professor: "Dr. Patel",
    credits: 4,
    room: "LH-203",
    color: COLORS.primary.purple,
    totalClasses: 60,
    attendedClasses: 48,
  },
  {
    id: "3",
    name: "Machine Learning",
    shortName: "ML",
    professor: "Dr. Kumar",
    credits: 4,
    room: "CS-Lab 1",
    color: COLORS.primary.teal,
    totalClasses: 60,
    attendedClasses: 55,
  },
  {
    id: "4",
    name: "Engineering Mathematics - 2",
    shortName: "EM-2",
    professor: "Dr. Singh",
    credits: 4,
    room: "LH-105",
    color: COLORS.primary.indigo,
    totalClasses: 60,
    attendedClasses: 42,
  },
  {
    id: "5",
    name: "Foundations of Electrodynamics",
    shortName: "Electrodynamics",
    professor: "Dr. Verma",
    credits: 3,
    room: "PH-201",
    color: COLORS.primary.pink,
    totalClasses: 45,
    attendedClasses: 38,
  },
  {
    id: "6",
    name: "Quantum Mechanics",
    shortName: "QM",
    professor: "Dr. Gupta",
    credits: 4,
    room: "PH-202",
    color: COLORS.primary.cyan,
    totalClasses: 60,
    attendedClasses: 51,
  },
];

export const SEMESTERS: Semester[] = [
  {
    id: "1",
    name: "Semester 1",
    startDate: "2025-01-15",
    endDate: "2025-05-15",
    isActive: false,
  },
  {
    id: "2",
    name: "Semester 2",
    startDate: "2025-07-15",
    endDate: "2025-11-30",
    isActive: false,
  },
  {
    id: "3",
    name: "Semester 3",
    startDate: "2026-01-15",
    endDate: "2026-05-15",
    isActive: true,
  },
];

export const generateTodaysClasses = (): Class[] => {
  const today = new Date();
  const dayNames = ["SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"];
  const currentDay = dayNames[today.getDay()];
  const dateStr = today.toISOString().split("T")[0];

  return [
    {
      id: "c1",
      name: "The Indian Constitution",
      shortName: "Indian Const.",
      professor: "Dr. Sharma",
      room: "LH-101",
      time: { start: "08:00", end: "08:50" },
      day: currentDay,
      date: dateStr,
      semester: "3",
      completed: false,
      color: COLORS.primary.blue,
    },
    {
      id: "c2",
      name: "Mathematical Foundations of Computer Science",
      shortName: "MFCS",
      professor: "Dr. Patel",
      room: "LH-203",
      time: { start: "09:00", end: "09:50" },
      day: currentDay,
      date: dateStr,
      semester: "3",
      completed: false,
      color: COLORS.primary.purple,
    },
    {
      id: "c3",
      name: "Machine Learning",
      shortName: "ML",
      professor: "Dr. Kumar",
      room: "CS-Lab 1",
      time: { start: "10:00", end: "10:50" },
      day: currentDay,
      date: dateStr,
      semester: "3",
      completed: false,
      color: COLORS.primary.teal,
    },
    {
      id: "c4",
      name: "Engineering Mathematics - 2",
      shortName: "EM-2",
      professor: "Dr. Singh",
      room: "LH-105",
      time: { start: "11:00", end: "11:50" },
      day: currentDay,
      date: dateStr,
      semester: "3",
      completed: false,
      color: COLORS.primary.indigo,
    },
    {
      id: "c5",
      name: "Foundations of Electrodynamics",
      shortName: "Electrodynamics",
      professor: "Dr. Verma",
      room: "PH-201",
      time: { start: "14:00", end: "14:50" },
      day: currentDay,
      date: dateStr,
      semester: "3",
      completed: false,
      color: COLORS.primary.pink,
    },
    {
      id: "c6",
      name: "Quantum Mechanics",
      shortName: "QM",
      professor: "Dr. Gupta",
      room: "PH-202",
      time: { start: "15:00", end: "15:50" },
      day: currentDay,
      date: dateStr,
      semester: "3",
      completed: false,
      color: COLORS.primary.cyan,
    },
  ];
};

export const generateWeekClasses = (): Record<string, Class[]> => {
  const weekClasses: Record<string, Class[]> = {
    MON: [],
    TUE: [],
    WED: [],
    THU: [],
    FRI: [],
  };

  const courses = COURSES;
  const schedules: Record<
    string,
    { courseIdx: number; time: { start: string; end: string } }[]
  > = {
    MON: [
      { courseIdx: 0, time: { start: "08:00", end: "08:50" } },
      { courseIdx: 1, time: { start: "09:00", end: "09:50" } },
      { courseIdx: 2, time: { start: "10:00", end: "10:50" } },
      { courseIdx: 3, time: { start: "14:00", end: "14:50" } },
    ],
    TUE: [
      { courseIdx: 4, time: { start: "08:00", end: "08:50" } },
      { courseIdx: 5, time: { start: "09:00", end: "09:50" } },
      { courseIdx: 0, time: { start: "10:00", end: "10:50" } },
      { courseIdx: 1, time: { start: "11:00", end: "11:50" } },
    ],
    WED: [
      { courseIdx: 2, time: { start: "08:00", end: "08:50" } },
      { courseIdx: 3, time: { start: "09:00", end: "09:50" } },
      { courseIdx: 4, time: { start: "14:00", end: "14:50" } },
      { courseIdx: 5, time: { start: "15:00", end: "15:50" } },
    ],
    THU: [
      { courseIdx: 0, time: { start: "08:00", end: "08:50" } },
      { courseIdx: 1, time: { start: "09:00", end: "09:50" } },
      { courseIdx: 2, time: { start: "10:00", end: "10:50" } },
      { courseIdx: 3, time: { start: "11:00", end: "11:50" } },
    ],
    FRI: [
      { courseIdx: 4, time: { start: "08:00", end: "08:50" } },
      { courseIdx: 5, time: { start: "09:00", end: "09:50" } },
      { courseIdx: 0, time: { start: "14:00", end: "14:50" } },
      { courseIdx: 2, time: { start: "15:00", end: "15:50" } },
    ],
  };

  Object.keys(schedules).forEach((day) => {
    schedules[day].forEach((schedule, idx) => {
      const course = courses[schedule.courseIdx];
      weekClasses[day].push({
        id: `${day}-${idx}`,
        name: course.name,
        shortName: course.shortName,
        professor: course.professor,
        room: course.room,
        time: schedule.time,
        day: day,
        date: "",
        semester: "3",
        completed: false,
        color: course.color,
      });
    });
  });

  return weekClasses;
};

export const COMPLETED_CLASSES: Class[] = [
  {
    id: "comp1",
    name: "Machine Learning",
    shortName: "ML",
    professor: "Dr. Kumar",
    room: "CS-Lab 1",
    time: { start: "10:00", end: "10:50" },
    day: "MON",
    date: "2026-02-02",
    semester: "3",
    completed: true,
    completedAt: "2026-02-02T10:52:00",
    color: COLORS.primary.teal,
  },
  {
    id: "comp2",
    name: "Engineering Mathematics - 2",
    shortName: "EM-2",
    professor: "Dr. Singh",
    room: "LH-105",
    time: { start: "11:00", end: "11:50" },
    day: "MON",
    date: "2026-02-02",
    semester: "3",
    completed: true,
    completedAt: "2026-02-02T11:53:00",
    color: COLORS.primary.indigo,
  },
  {
    id: "comp3",
    name: "The Indian Constitution",
    shortName: "Indian Const.",
    professor: "Dr. Sharma",
    room: "LH-101",
    time: { start: "08:00", end: "08:50" },
    day: "FRI",
    date: "2026-01-30",
    semester: "3",
    completed: true,
    completedAt: "2026-01-30T08:51:00",
    color: COLORS.primary.blue,
  },
  {
    id: "comp4",
    name: "Quantum Mechanics",
    shortName: "QM",
    professor: "Dr. Gupta",
    room: "PH-202",
    time: { start: "15:00", end: "15:50" },
    day: "THU",
    date: "2026-01-29",
    semester: "3",
    completed: true,
    completedAt: "2026-01-29T15:52:00",
    color: COLORS.primary.cyan,
  },
];
