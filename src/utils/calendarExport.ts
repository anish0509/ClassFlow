import { ClassSchedule } from "../types";

const dayMap: Record<string, number> = {
  SUN: 0,
  MON: 1,
  TUE: 2,
  WED: 3,
  THU: 4,
  FRI: 5,
  SAT: 6,
};

const pad = (n: number) => n.toString().padStart(2, "0");

const formatDate = (date: Date) => {
  return `${date.getFullYear()}${pad(date.getMonth() + 1)}${pad(date.getDate())}`;
};

const formatDateTime = (date: Date, time: string) => {
  const [h, m] = time.split(":").map((v) => parseInt(v, 10) || 0);
  const dt = new Date(date);
  dt.setHours(h, m, 0, 0);
  return `${formatDate(dt)}T${pad(dt.getHours())}${pad(dt.getMinutes())}00`;
};

const addDays = (date: Date, days: number) => {
  const next = new Date(date);
  next.setDate(next.getDate() + days);
  return next;
};

/**
 * Generate an ICS calendar payload for recurring weekly classes within a semester window.
 */
export const generateICSForSemester = (
  classes: ClassSchedule[],
  semesterStart: string,
  semesterEnd: string,
): string => {
  const start = new Date(semesterStart);
  const end = new Date(semesterEnd);
  const until = `${formatDate(end)}T235959`;
  const dtStamp = formatDateTime(new Date(), "00:00");

  const lines: string[] = [
    "BEGIN:VCALENDAR",
    "VERSION:2.0",
    "PRODID:-//UniTimetable//Calendar Export//EN",
    "CALSCALE:GREGORIAN",
  ];

  classes.forEach((cls) => {
    const weekday = dayMap[cls.dayOfWeek];
    if (weekday === undefined) return;

    const startDay = start.getDay();
    const offset = (weekday - startDay + 7) % 7;
    const firstOccurrence = addDays(start, offset);

    const dtStart = formatDateTime(firstOccurrence, cls.startTime);
    const dtEnd = formatDateTime(firstOccurrence, cls.endTime);

    lines.push(
      "BEGIN:VEVENT",
      `UID:${cls.id}-${dtStart}`,
      `DTSTAMP:${dtStamp}`,
      `SUMMARY:${cls.courseName || cls.shortName || "Class"}`,
      cls.room ? `LOCATION:${cls.room}` : "LOCATION:",
      `DTSTART:${dtStart}`,
      `DTEND:${dtEnd}`,
      `RRULE:FREQ=WEEKLY;UNTIL=${until}`,
      "END:VEVENT",
    );
  });

  lines.push("END:VCALENDAR");
  return lines.join("\n");
};
