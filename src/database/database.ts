import * as FileSystem from "expo-file-system/legacy";
import * as SQLite from "expo-sqlite";
import { Attachment } from "../types";

const DATABASE_NAME = "timetable.db";

// Debug utility: Delete the SQLite DB file and re-initialize
export const resetDatabase = async () => {
  try {
    db = null;
    const dbPath = `${FileSystem.documentDirectory}SQLite/${DATABASE_NAME}`;
    const exists = await FileSystem.getInfoAsync(dbPath);
    if (exists.exists) {
      await FileSystem.deleteAsync(dbPath, { idempotent: true });
    }
    // Re-initialize
    await initDatabase();
    console.log("Database reset and re-initialized.");
  } catch (e) {
    console.error("Failed to reset database:", e);
  }
};

let db: SQLite.SQLiteDatabase | null = null;
let connectionPromise: Promise<SQLite.SQLiteDatabase> | null = null;

export const getDatabase = async (): Promise<SQLite.SQLiteDatabase> => {
  // 1. Reuse healthy cached connection if active
  if (db) {
    try {
      await db.getFirstAsync("SELECT 1"); // Fast health check
      return db;
    } catch {
      db = null; // Connection released/stale (hot reload), reset cache
    }
  }

  // 2. Concurrency Lock: Await existing in-flight opening request
  if (connectionPromise) {
    return await connectionPromise;
  }

  // 3. Open fresh connection natively with a promise lock
  connectionPromise = SQLite.openDatabaseAsync(DATABASE_NAME);
  try {
    db = await connectionPromise;
    return db;
  } finally {
    connectionPromise = null; // Release the lock
  }
};

// Migration helper to check if a column exists
const columnExists = async (
  database: SQLite.SQLiteDatabase,
  tableName: string,
  columnName: string,
): Promise<boolean> => {
  try {
    const result = await database.getAllAsync<{ name: string }>(
      `PRAGMA table_info(${tableName})`,
    );
    return result.some((col) => col.name === columnName);
  } catch {
    return false;
  }
};

// Check if a table needs to be recreated due to missing required columns
const tableNeedsRecreation = async (
  database: SQLite.SQLiteDatabase,
  tableName: string,
  requiredColumns: string[],
): Promise<boolean> => {
  try {
    const result = await database.getAllAsync<{ name: string }>(
      `PRAGMA table_info(${tableName})`,
    );
    const existingColumns = result.map((col) => col.name);
    return requiredColumns.some((col) => !existingColumns.includes(col));
  } catch {
    return false;
  }
};

// Run database migrations for schema updates
const runMigrations = async (
  database: SQLite.SQLiteDatabase,
): Promise<void> => {
  // Check if all critical tables have the required columns
  const requiredSchemas: Record<string, string[]> = {
    semesters: ["id", "name", "startDate", "endDate", "isActive"],
    courses: ["id", "name", "shortName", "professor", "credits", "room", "color", "semesterId"],
    classes: ["id", "courseId", "dayOfWeek", "startTime", "endTime", "room", "semesterId"],
    attendance: ["id", "classId", "courseId", "date", "status", "markedAt"],
    tasks: ["id", "courseId", "title", "priority", "status", "createdAt"],
  };

  let needsRecreation = false;
  for (const [table, columns] of Object.entries(requiredSchemas)) {
    if (await tableNeedsRecreation(database, table, columns)) {
      needsRecreation = true;
      break;
    }
  }

  if (needsRecreation) {
    // Drop all tables and recreate - the schema is too old
    await database.execAsync(`
      DROP TABLE IF EXISTS attendance;
      DROP TABLE IF EXISTS shifted_classes;
      DROP TABLE IF EXISTS tasks;
      DROP TABLE IF EXISTS classes;
      DROP TABLE IF EXISTS courses;
      DROP TABLE IF EXISTS semesters;
      DROP TABLE IF EXISTS settings;
    `);
    return; // initDatabase will recreate tables
  }

  // Migration 2: Remove old demo data (detect by known demo IDs)
  const hasDemoData = await database.getFirstAsync<{ count: number }>(
    "SELECT COUNT(*) as count FROM semesters WHERE id IN ('sem1', 'sem2', 'sem3')",
  );

  if (hasDemoData && hasDemoData.count > 0) {
    // Clear all demo data to give user a fresh start
    await database.execAsync(`
      DELETE FROM attendance;
      DELETE FROM shifted_classes;
      DELETE FROM tasks;
      DELETE FROM classes;
      DELETE FROM courses;
      DELETE FROM semesters WHERE id IN ('sem1', 'sem2', 'sem3');
    `);
  }

  // Migration 1: Add isActive column to semesters table if it doesn't exist
  const hasIsActive = await columnExists(database, "semesters", "isActive");
  if (!hasIsActive) {
    await database.execAsync(
      `ALTER TABLE semesters ADD COLUMN isActive INTEGER DEFAULT 0`,
    );
    // Set the first semester as active if none is set
    const semesters = await database.getAllAsync<{ id: string }>(
      "SELECT id FROM semesters ORDER BY startDate DESC LIMIT 1",
    );
    if (semesters.length > 0) {
      await database.runAsync(
        "UPDATE semesters SET isActive = 1 WHERE id = ?",
        [semesters[0].id],
      );
    }
  }

  // Migration 3: Add shiftedToDate column to attendance table if it doesn't exist
  try {
    const hasAttendanceTable = await database.getFirstAsync<{ name: string }>(
      "SELECT name FROM sqlite_master WHERE type='table' AND name='attendance'",
    );
    if (hasAttendanceTable) {
      const hasShiftedToDate = await columnExists(
        database,
        "attendance",
        "shiftedToDate",
      );
      if (!hasShiftedToDate) {
        await database.execAsync(
          `ALTER TABLE attendance ADD COLUMN shiftedToDate TEXT`,
        );
      }
    }
  } catch (error) {
    console.log("Migration 3 error (likely table doesn't exist yet):", error);
  }
};

export const initDatabase = async (): Promise<void> => {
  const database = await getDatabase();

  // Run migrations FIRST to handle incompatible schemas
  await runMigrations(database);

  // Create tables
  await database.execAsync(`
    PRAGMA journal_mode = WAL;

    CREATE TABLE IF NOT EXISTS semesters (
      id TEXT PRIMARY KEY,
      name TEXT NOT NULL,
      startDate TEXT NOT NULL,
      endDate TEXT NOT NULL,
      isActive INTEGER DEFAULT 0
    );

    CREATE TABLE IF NOT EXISTS courses (
      id TEXT PRIMARY KEY,
      name TEXT NOT NULL,
      shortName TEXT NOT NULL,
      professor TEXT NOT NULL,
      credits INTEGER DEFAULT 3,
      room TEXT NOT NULL,
      color TEXT NOT NULL,
      semesterId TEXT,
      FOREIGN KEY (semesterId) REFERENCES semesters(id)
    );

    CREATE TABLE IF NOT EXISTS classes (
      id TEXT PRIMARY KEY,
      courseId TEXT NOT NULL,
      dayOfWeek TEXT NOT NULL,
      startTime TEXT NOT NULL,
      endTime TEXT NOT NULL,
      room TEXT,
      semesterId TEXT,
      FOREIGN KEY (courseId) REFERENCES courses(id),
      FOREIGN KEY (semesterId) REFERENCES semesters(id)
    );

    CREATE TABLE IF NOT EXISTS attendance (
      id TEXT PRIMARY KEY,
      classId TEXT NOT NULL,
      courseId TEXT NOT NULL,
      date TEXT NOT NULL,
      status TEXT NOT NULL,
      markedAt TEXT NOT NULL,
      notes TEXT,
      shiftedToDate TEXT,
      FOREIGN KEY (classId) REFERENCES classes(id),
      FOREIGN KEY (courseId) REFERENCES courses(id)
    );

    CREATE TABLE IF NOT EXISTS shifted_classes (
      id TEXT PRIMARY KEY,
      originalClassId TEXT NOT NULL,
      courseId TEXT NOT NULL,
      originalDate TEXT NOT NULL,
      shiftedToDate TEXT NOT NULL,
      startTime TEXT NOT NULL,
      endTime TEXT NOT NULL,
      room TEXT,
      createdAt TEXT NOT NULL,
      FOREIGN KEY (originalClassId) REFERENCES classes(id),
      FOREIGN KEY (courseId) REFERENCES courses(id)
    );

    CREATE TABLE IF NOT EXISTS tasks (
      id TEXT PRIMARY KEY,
      courseId TEXT,
      title TEXT NOT NULL,
      description TEXT,
      dueDate TEXT,
      priority TEXT DEFAULT 'medium',
      status TEXT DEFAULT 'pending',
      createdAt TEXT NOT NULL,
      completedAt TEXT,
      FOREIGN KEY (courseId) REFERENCES courses(id)
    );

    CREATE TABLE IF NOT EXISTS settings (
      key TEXT PRIMARY KEY,
      value TEXT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS attachments (
      id TEXT PRIMARY KEY,
      courseId TEXT NOT NULL,
      title TEXT NOT NULL,
      url TEXT NOT NULL,
      type TEXT DEFAULT 'link',
      createdAt TEXT NOT NULL,
      FOREIGN KEY (courseId) REFERENCES courses(id)
    );

    CREATE INDEX IF NOT EXISTS idx_classes_courseId ON classes(courseId);
    CREATE INDEX IF NOT EXISTS idx_classes_semesterId ON classes(semesterId);
    CREATE INDEX IF NOT EXISTS idx_classes_dayOfWeek ON classes(dayOfWeek);
    CREATE INDEX IF NOT EXISTS idx_attendance_classId ON attendance(classId);
    CREATE INDEX IF NOT EXISTS idx_attendance_courseId ON attendance(courseId);
    CREATE INDEX IF NOT EXISTS idx_attendance_date ON attendance(date);
    CREATE INDEX IF NOT EXISTS idx_shifted_classes_originalDate ON shifted_classes(originalDate);
    CREATE INDEX IF NOT EXISTS idx_shifted_classes_shiftedToDate ON shifted_classes(shiftedToDate);
    CREATE INDEX IF NOT EXISTS idx_tasks_courseId ON tasks(courseId);
  `);

  // Initialize default settings if not present
  const settingsCount = await database.getFirstAsync<{ count: number }>(
    "SELECT COUNT(*) as count FROM settings",
  );

  if (settingsCount && settingsCount.count === 0) {
    await database.execAsync(`
      INSERT INTO settings (key, value) VALUES
      ('notifications', 'true'),
      ('darkMode', 'true');
    `);
  }
};

// Course operations
export const getAllCourses = async (semesterId?: string) => {
  const database = await getDatabase();
  const today = new Date().toISOString().split("T")[0];
  const now = new Date();
  
  // Main course query with attendance stats (single query)
  let query = `
    SELECT c.*, 
      (SELECT COUNT(*) FROM attendance a WHERE a.courseId = c.id AND a.status = 'present') as attendedClasses,
      (SELECT COUNT(*) FROM attendance a WHERE a.courseId = c.id AND a.status = 'canceled') as canceledClasses,
      (SELECT COUNT(DISTINCT a.date) FROM attendance a WHERE a.courseId = c.id AND a.date <= '${today}') as markedClasses
    FROM courses c
  `;
  if (semesterId) {
    query += ` WHERE c.semesterId = '${semesterId}'`;
  }
  
  // Batch-load everything in parallel (4 queries total instead of 2N)
  const [courses, allSemesters, allSchedules, allShiftedClasses] = await Promise.all([
    database.getAllAsync<any>(query),
    database.getAllAsync<{ id: string; startDate: string; endDate: string }>(
      `SELECT id, startDate, endDate FROM semesters`
    ),
    database.getAllAsync<{ courseId: string; dayOfWeek: string; startTime: string }>(
      `SELECT courseId, dayOfWeek, startTime FROM classes`
    ),
    database.getAllAsync<{ courseId: string; originalDate: string; shiftedToDate: string; startTime: string }>(
      `SELECT courseId, originalDate, shiftedToDate, startTime FROM shifted_classes`
    ),
  ]);

  // Build lookup maps (in-memory, instant)
  const semesterMap = new Map(allSemesters.map(s => [s.id, s]));
  const scheduleMap = new Map<string, { dayOfWeek: string; startTime: string }[]>();
  allSchedules.forEach(s => {
    if (!scheduleMap.has(s.courseId)) scheduleMap.set(s.courseId, []);
    scheduleMap.get(s.courseId)!.push(s);
  });

  const shiftedMap = new Map<string, { originalDate: string; shiftedToDate: string; startTime: string }[]>();
  allShiftedClasses.forEach(sc => {
    if (!shiftedMap.has(sc.courseId)) shiftedMap.set(sc.courseId, []);
    shiftedMap.get(sc.courseId)!.push(sc);
  });

  // Calculate totalClasses for each course (pure in-memory computation)
  return courses.map((course: any) => {
    const semester = semesterMap.get(course.semesterId);
    const classSchedules = scheduleMap.get(course.id) || [];

    if (!semester || classSchedules.length === 0) {
      return { ...course, totalClasses: course.markedClasses || 0 };
    }

    const semStartStr = semester.startDate.split("T")[0];
    const semEndStr = semester.endDate.split("T")[0];

    if (today < semStartStr) {
      return { ...course, totalClasses: 0 };
    }

    // Build a map of scheduled days for O(1) lookup
    const scheduledDays = new Map<string, string[]>();
    classSchedules.forEach(c => {
      if (!scheduledDays.has(c.dayOfWeek)) scheduledDays.set(c.dayOfWeek, []);
      scheduledDays.get(c.dayOfWeek)!.push(c.startTime);
    });

    const endBoundary = today < semEndStr ? today : semEndStr;
    let totalScheduledClasses = 0;
    const currentDate = new Date(semStartStr + "T00:00:00Z");
    const endDate = new Date(endBoundary + "T00:00:00Z");
    const dayNames = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'];

    while (currentDate <= endDate) {
      const dayOfWeek = dayNames[currentDate.getUTCDay()];
      const startTimes = scheduledDays.get(dayOfWeek);

      if (startTimes) {
        const dateStr = currentDate.toISOString().split('T')[0];
        startTimes.forEach((startTime) => {
          if (dateStr === today) {
            const [hours, minutes] = startTime.split(':').map(Number);
            const classTime = new Date(now);
            classTime.setHours(hours, minutes, 0, 0);
            if (now >= classTime) totalScheduledClasses++;
          } else {
            totalScheduledClasses++;
          }
        });
      }

      currentDate.setUTCDate(currentDate.getUTCDate() + 1);
    }

    // Apply Shift Adjustments to dashboard percentages
    const courseShifted = shiftedMap.get(course.id) || [];
    let shiftedAwayCount = 0;
    let shiftedToCount = 0;

    courseShifted.forEach((sc) => {
      if (sc.originalDate <= endBoundary) {
        shiftedAwayCount++;
      }
      if (sc.shiftedToDate <= endBoundary) {
        if (sc.shiftedToDate === today) {
          const [hours, minutes] = sc.startTime.split(":").map(Number);
          const classTime = new Date(now);
          classTime.setHours(hours, minutes, 0, 0);
          if (now >= classTime) {
            shiftedToCount++;
          }
        } else {
          shiftedToCount++;
        }
      }
    });

    const balancedTotal = totalScheduledClasses - shiftedAwayCount + shiftedToCount - (course.canceledClasses || 0);

    return { ...course, totalClasses: balancedTotal > 0 ? balancedTotal : 0 };
  });
};

export const addCourse = async (course: {
  id: string;
  name: string;
  shortName: string;
  professor: string;
  credits: number;
  room: string;
  color: string;
  semesterId: string;
}) => {
  const database = await getDatabase();
  await database.runAsync(
    `INSERT INTO courses (id, name, shortName, professor, credits, room, color, semesterId)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
    [
      course.id,
      course.name,
      course.shortName,
      course.professor,
      course.credits,
      course.room,
      course.color,
      course.semesterId,
    ],
  );
};

export const updateCourse = async (
  id: string,
  updates: Partial<{
    name: string;
    shortName: string;
    professor: string;
    credits: number;
    room: string;
    color: string;
  }>,
) => {
  const database = await getDatabase();
  const setClause = Object.keys(updates)
    .map((key) => `${key} = ?`)
    .join(", ");
  const values = [...Object.values(updates), id];
  await database.runAsync(
    `UPDATE courses SET ${setClause} WHERE id = ?`,
    values,
  );
};

export const deleteCourse = async (id: string) => {
  const database = await getDatabase();
  await database.runAsync("DELETE FROM classes WHERE courseId = ?", [id]);
  await database.runAsync("DELETE FROM attendance WHERE courseId = ?", [id]);
  await database.runAsync("DELETE FROM tasks WHERE courseId = ?", [id]);
  await database.runAsync("DELETE FROM attachments WHERE courseId = ?", [id]);
  await database.runAsync("DELETE FROM courses WHERE id = ?", [id]);
};

// Attachments
export const addAttachment = async (attachment: {
  id: string;
  courseId: string;
  title: string;
  url: string;
  type: string;
  createdAt: string;
}) => {
  const database = await getDatabase();
  await database.runAsync(
    `INSERT INTO attachments (id, courseId, title, url, type, createdAt)
     VALUES (?, ?, ?, ?, ?, ?)`,
    [
      attachment.id,
      attachment.courseId,
      attachment.title,
      attachment.url,
      attachment.type,
      attachment.createdAt,
    ],
  );
};

export const getAttachmentsForCourse = async (courseId: string) => {
  const database = await getDatabase();
  return database.getAllAsync(
    `SELECT * FROM attachments WHERE courseId = ? ORDER BY createdAt DESC`,
    [courseId],
  );
};

export const deleteAttachment = async (id: string) => {
  const database = await getDatabase();
  await database.runAsync(`DELETE FROM attachments WHERE id = ?`, [id]);
};

export const getAllAttachments = async () => {
  const database = await getDatabase();
  return await database.getAllAsync(`SELECT * FROM attachments`) as Attachment[];
};

// Class schedule operations
export const getClassesForDay = async (
  dayOfWeek: string,
  semesterId?: string,
) => {
  const database = await getDatabase();
  let query = `
    SELECT cl.*, c.name as courseName, c.shortName, c.professor, c.color
    FROM classes cl
    JOIN courses c ON cl.courseId = c.id
    WHERE cl.dayOfWeek = ?
  `;
  const params: string[] = [dayOfWeek];
  if (semesterId) {
    query += ` AND cl.semesterId = ?`;
    params.push(semesterId);
  }
  query += ` ORDER BY cl.startTime`;
  return await database.getAllAsync(query, params);
};

export const getAllClasses = async (semesterId?: string) => {
  const database = await getDatabase();
  let query = `
    SELECT cl.*, c.name as courseName, c.shortName, c.professor, c.color
    FROM classes cl
    JOIN courses c ON cl.courseId = c.id
  `;
  if (semesterId) {
    query += ` WHERE cl.semesterId = '${semesterId}'`;
  }
  query += ` ORDER BY 
    CASE cl.dayOfWeek 
      WHEN 'MON' THEN 1 
      WHEN 'TUE' THEN 2 
      WHEN 'WED' THEN 3 
      WHEN 'THU' THEN 4 
      WHEN 'FRI' THEN 5 
      WHEN 'SAT' THEN 6 
      WHEN 'SUN' THEN 7 
    END, cl.startTime`;
  return await database.getAllAsync(query);
};

export const addClass = async (classData: {
  id: string;
  courseId: string;
  dayOfWeek: string;
  startTime: string;
  endTime: string;
  room: string;
  semesterId: string;
}) => {
  const database = await getDatabase();
  await database.runAsync(
    `INSERT INTO classes (id, courseId, dayOfWeek, startTime, endTime, room, semesterId)
     VALUES (?, ?, ?, ?, ?, ?, ?)`,
    [
      classData.id,
      classData.courseId,
      classData.dayOfWeek,
      classData.startTime,
      classData.endTime,
      classData.room,
      classData.semesterId,
    ],
  );
};

export const updateClass = async (
  id: string,
  updates: Partial<{
    courseId: string;
    dayOfWeek: string;
    startTime: string;
    endTime: string;
    room: string;
  }>,
) => {
  const database = await getDatabase();
  const setClause = Object.keys(updates)
    .map((key) => `${key} = ?`)
    .join(", ");
  const values = [...Object.values(updates), id];
  await database.runAsync(
    `UPDATE classes SET ${setClause} WHERE id = ?`,
    values,
  );
};

export const deleteClass = async (id: string) => {
  const database = await getDatabase();
  await database.runAsync("DELETE FROM attendance WHERE classId = ?", [id]);
  await database.runAsync("DELETE FROM classes WHERE id = ?", [id]);
};

// Attendance operations
export const markAttendance = async (attendance: {
  id: string;
  classId: string;
  courseId: string;
  date: string;
  status: "present" | "absent" | "canceled" | "shifted";
  notes?: string;
  shiftedToDate?: string;
}) => {
  const database = await getDatabase();
  const markedAt = new Date().toISOString();

  // Check if attendance already exists for this class and date
  const existing = await database.getFirstAsync<{ id: string }>(
    "SELECT id FROM attendance WHERE classId = ? AND date = ?",
    [attendance.classId, attendance.date],
  );

  if (existing) {
    await database.runAsync(
      `UPDATE attendance SET status = ?, markedAt = ?, notes = ?, shiftedToDate = ? WHERE id = ?`,
      [
        attendance.status,
        markedAt,
        attendance.notes || null,
        attendance.shiftedToDate || null,
        existing.id,
      ],
    );
    return existing.id;
  } else {
    await database.runAsync(
      `INSERT INTO attendance (id, classId, courseId, date, status, markedAt, notes, shiftedToDate)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        attendance.id,
        attendance.classId,
        attendance.courseId,
        attendance.date,
        attendance.status,
        markedAt,
        attendance.notes || null,
        attendance.shiftedToDate || null,
      ],
    );
    return attendance.id;
  }
};

export const getAttendanceForDate = async (date: string) => {
  const database = await getDatabase();
  return await database.getAllAsync(
    `SELECT a.*, c.name as courseName, c.shortName, c.color, cl.startTime, cl.endTime, cl.room
     FROM attendance a
     JOIN courses c ON a.courseId = c.id
     JOIN classes cl ON a.classId = cl.id
     WHERE a.date = ?
     ORDER BY a.markedAt DESC`,
    [date],
  );
};

export const getAllAttendance = async (
  courseId?: string,
  semesterId?: string,
) => {
  const database = await getDatabase();
  let query = `
    SELECT a.*, c.name as courseName, c.shortName, c.color, cl.startTime, cl.endTime, cl.room, cl.dayOfWeek
    FROM attendance a
    JOIN courses c ON a.courseId = c.id
    JOIN classes cl ON a.classId = cl.id
    WHERE 1=1
  `;
  const conditions: string[] = [];
  if (courseId) {
    conditions.push(`a.courseId = '${courseId}'`);
  }
  if (semesterId) {
    conditions.push(`c.semesterId = '${semesterId}'`);
  }
  if (conditions.length > 0) {
    query += ` AND ${conditions.join(" AND ")}`;
  }
  query += ` ORDER BY a.markedAt DESC`;
  return await database.getAllAsync(query);
};

export const getAttendanceStats = async (courseId: string) => {
  const database = await getDatabase();
  const today = new Date().toISOString().split("T")[0];
  const now = new Date();
  
  // Get marked attendance stats (excluding 'shifted' tombstones to prevent double-counting)
  const markedStats = await database.getFirstAsync<{
    marked: number;
    present: number;
    absent: number;
    canceled: number;
  }>(
    `SELECT 
      COUNT(*) as marked,
      SUM(CASE WHEN status = 'present' THEN 1 ELSE 0 END) as present,
      SUM(CASE WHEN status = 'absent' THEN 1 ELSE 0 END) as absent,
      SUM(CASE WHEN status = 'canceled' THEN 1 ELSE 0 END) as canceled
     FROM attendance WHERE courseId = ? AND status != 'shifted'`,
    [courseId],
  );
  
  // Get course's semester dates
  const course = await database.getFirstAsync<{ semesterId: string }>(
    `SELECT semesterId FROM courses WHERE id = ?`,
    [courseId]
  );
  
  if (!course) {
    return { total: markedStats?.marked || 0, present: markedStats?.present || 0, absent: markedStats?.absent || 0, canceled: markedStats?.canceled || 0 };
  }
  
  const semester = await database.getFirstAsync<{ startDate: string; endDate: string }>(
    `SELECT startDate, endDate FROM semesters WHERE id = ?`,
    [course.semesterId]
  );
  
  if (!semester) {
    return { total: markedStats?.marked || 0, present: markedStats?.present || 0, absent: markedStats?.absent || 0, canceled: markedStats?.canceled || 0 };
  }
  
  // Get class schedules for this course
  const classSchedules = await database.getAllAsync<{ dayOfWeek: string; startTime: string }>(
    `SELECT dayOfWeek, startTime FROM classes WHERE courseId = ?`,
    [courseId]
  );
  
  if (classSchedules.length === 0) {
    return { total: markedStats?.marked || 0, present: markedStats?.present || 0, absent: markedStats?.absent || 0, canceled: markedStats?.canceled || 0 };
  }
  
  // Calculate total scheduled classes up to today (within semester bounds)
  const semesterStart = new Date(semester.startDate);
  const semesterEnd = new Date(semester.endDate);
  const todayDate = new Date(today);
  
  // Don't count beyond semester end date
  const endBoundary = todayDate < semesterEnd ? todayDate : semesterEnd;
  const endBoundaryStr = endBoundary.toISOString().split('T')[0];
  
  // If today is before semester start, no classes to count
  if (todayDate < semesterStart) {
    return { total: 0, present: 0, absent: 0, canceled: 0 };
  }
  
  let totalScheduledClasses = 0;
  const currentDate = new Date(semesterStart);
  
  while (currentDate <= endBoundary) {
    const dayOfWeek = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'][currentDate.getDay()];
    
    // Check if there's a class scheduled for this day
    const scheduledClass = classSchedules.find(c => c.dayOfWeek === dayOfWeek);
    
    if (scheduledClass) {
      // For today, only count if class time has passed
      if (currentDate.toISOString().split('T')[0] === today) {
        const [hours, minutes] = scheduledClass.startTime.split(':').map(Number);
        const classTime = new Date(now);
        classTime.setHours(hours, minutes, 0, 0);
        if (now >= classTime) {
          totalScheduledClasses++;
        }
      } else {
        totalScheduledClasses++;
      }
    }
    
    currentDate.setDate(currentDate.getDate() + 1);
  }

  // Core Shift Correction logic:
  // 1. Query classes shifted AWAY from their original schedule where originalDate <= endBoundary
  const shiftedAway = await database.getFirstAsync<{ count: number }>(
    `SELECT COUNT(*) as count FROM shifted_classes 
     WHERE courseId = ? AND originalDate <= ?`,
    [courseId, endBoundaryStr]
  );

  // 2. Query classes shifted TO a date where shiftedToDate <= endBoundary
  const shiftedTo = await database.getAllAsync<{ shiftedToDate: string; startTime: string }>(
    `SELECT shiftedToDate, startTime FROM shifted_classes 
     WHERE courseId = ? AND shiftedToDate <= ?`,
    [courseId, endBoundaryStr]
  );

  let shiftedToCount = 0;
  shiftedTo.forEach(s => {
    if (s.shiftedToDate === today) {
      // For today's shifted classes, only count if start time has passed
      const [hours, minutes] = s.startTime.split(':').map(Number);
      const classTime = new Date(now);
      classTime.setHours(hours, minutes, 0, 0);
      if (now >= classTime) {
        shiftedToCount++;
      }
    } else {
      shiftedToCount++;
    }
  });

  // Complete mathematical balance (excluding canceled classes to avoid penalizing percentage)
  const finalTotal = totalScheduledClasses - (shiftedAway?.count || 0) + shiftedToCount - (markedStats?.canceled || 0);
  
  return { 
    total: finalTotal > 0 ? finalTotal : 0, 
    present: markedStats?.present || 0, 
    absent: markedStats?.absent || 0, 
    canceled: markedStats?.canceled || 0 
  };
};

export const removeAttendance = async (id: string) => {
  const database = await getDatabase();
  await database.runAsync("DELETE FROM attendance WHERE id = ?", [id]);
};

// Shifted class operations
export const shiftClass = async (shiftData: {
  id: string;
  originalClassId: string;
  courseId: string;
  originalDate: string;
  shiftedToDate: string;
  startTime: string;
  endTime: string;
  room?: string;
}) => {
  const database = await getDatabase();
  const createdAt = new Date().toISOString();

  await database.runAsync(
    `INSERT INTO shifted_classes (id, originalClassId, courseId, originalDate, shiftedToDate, startTime, endTime, room, createdAt)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`,
    [
      shiftData.id,
      shiftData.originalClassId,
      shiftData.courseId,
      shiftData.originalDate,
      shiftData.shiftedToDate,
      shiftData.startTime,
      shiftData.endTime,
      shiftData.room || null,
      createdAt,
    ],
  );
};

export const updateShiftedClass = async (
  id: string,
  shiftedToDate: string,
  startTime: string,
  endTime: string,
  room?: string,
) => {
  const database = await getDatabase();
  await database.runAsync(
    `UPDATE shifted_classes 
     SET shiftedToDate = ?, startTime = ?, endTime = ?, room = ? 
     WHERE id = ?`,
    [shiftedToDate, startTime, endTime, room || null, id]
  );
};

export const removeShiftTombstone = async (classId: string, originalDate: string) => {
  const database = await getDatabase();
  await database.runAsync(
    `DELETE FROM attendance 
     WHERE classId = ? AND date = ? AND status = 'shifted'`,
    [classId, originalDate]
  );
};

export const updateShiftTombstoneDestination = async (
  classId: string,
  originalDate: string,
  newShiftedToDate: string,
) => {
  const database = await getDatabase();
  await database.runAsync(
    `UPDATE attendance 
     SET shiftedToDate = ?, notes = ? 
     WHERE classId = ? AND date = ? AND status = 'shifted'`,
    [newShiftedToDate, `Shifted to ${newShiftedToDate}`, classId, originalDate]
  );
};

export const getShiftedClassesForDate = async (date: string) => {
  const database = await getDatabase();
  return await database.getAllAsync(
    `SELECT sc.*, c.name as courseName, c.shortName, c.professor, c.color
     FROM shifted_classes sc
     JOIN courses c ON sc.courseId = c.id
     WHERE sc.shiftedToDate = ?
     ORDER BY sc.startTime`,
    [date],
  );
};

// Get classes that were shifted away from a specific date
export const getClassesShiftedFromDate = async (date: string) => {
  const database = await getDatabase();
  return await database.getAllAsync(
    `SELECT sc.originalClassId, sc.originalDate
     FROM shifted_classes sc
     WHERE sc.originalDate = ?`,
    [date],
  );
};

export const removeShiftedClass = async (id: string) => {
  const database = await getDatabase();
  await database.runAsync("DELETE FROM shifted_classes WHERE id = ?", [id]);
};

// Task operations
export const getAllTasks = async (status?: string, semesterId?: string) => {
  const database = await getDatabase();
  let query = `
    SELECT t.*, c.name as courseName, c.shortName, c.color
    FROM tasks t
    LEFT JOIN courses c ON t.courseId = c.id
    WHERE 1=1
  `;
  if (status) {
    query += ` AND t.status = '${status}'`;
  }
  if (semesterId) {
    query += ` AND (c.semesterId = '${semesterId}' OR t.courseId IS NULL)`;
  }
  query += ` ORDER BY 
    CASE t.priority WHEN 'high' THEN 1 WHEN 'medium' THEN 2 WHEN 'low' THEN 3 END,
    t.dueDate ASC`;
  return await database.getAllAsync(query);
};

export const addTask = async (task: {
  id: string;
  courseId?: string;
  title: string;
  description?: string;
  dueDate?: string;
  priority: "high" | "medium" | "low";
}) => {
  const database = await getDatabase();
  const createdAt = new Date().toISOString();
  await database.runAsync(
    `INSERT INTO tasks (id, courseId, title, description, dueDate, priority, status, createdAt)
     VALUES (?, ?, ?, ?, ?, ?, 'pending', ?)`,
    [
      task.id,
      task.courseId || null,
      task.title,
      task.description || null,
      task.dueDate || null,
      task.priority,
      createdAt,
    ],
  );
};

export const updateTask = async (
  id: string,
  updates: Partial<{
    courseId: string;
    title: string;
    description: string;
    dueDate: string;
    priority: string;
    status: string;
  }>,
) => {
  const database = await getDatabase();
  const setClause = Object.keys(updates)
    .map((key) => `${key} = ?`)
    .join(", ");
  const values = [...Object.values(updates), id];
  await database.runAsync(`UPDATE tasks SET ${setClause} WHERE id = ?`, values);
};

export const completeTask = async (id: string) => {
  const database = await getDatabase();
  const completedAt = new Date().toISOString();
  await database.runAsync(
    `UPDATE tasks SET status = 'completed', completedAt = ? WHERE id = ?`,
    [completedAt, id],
  );
};

export const uncompleteTask = async (id: string) => {
  const database = await getDatabase();
  await database.runAsync(
    `UPDATE tasks SET status = 'pending', completedAt = NULL WHERE id = ?`,
    [id],
  );
};

export const deleteTask = async (id: string) => {
  const database = await getDatabase();
  await database.runAsync("DELETE FROM tasks WHERE id = ?", [id]);
};

// Semester operations
export const getAllSemesters = async () => {
  const database = await getDatabase();
  return await database.getAllAsync(
    "SELECT * FROM semesters ORDER BY startDate DESC",
  );
};

export const getActiveSemester = async (): Promise<{
  id: string;
  name: string;
  startDate: string;
  endDate: string;
  isActive: number;
} | null> => {
  const database = await getDatabase();
  return await database.getFirstAsync(
    "SELECT * FROM semesters WHERE isActive = 1",
  );
};

export const setActiveSemester = async (id: string) => {
  const database = await getDatabase();
  await database.runAsync("UPDATE semesters SET isActive = 0");
  await database.runAsync("UPDATE semesters SET isActive = 1 WHERE id = ?", [
    id,
  ]);
};

export const addSemester = async (semester: {
  id: string;
  name: string;
  startDate: string;
  endDate: string;
}) => {
  const database = await getDatabase();

  // Check if there are any existing semesters
  const existingCount = await database.getFirstAsync<{ count: number }>(
    "SELECT COUNT(*) as count FROM semesters",
  );

  // If this is the first semester, make it active
  const isActive = existingCount && existingCount.count === 0 ? 1 : 0;

  await database.runAsync(
    `INSERT INTO semesters (id, name, startDate, endDate, isActive) VALUES (?, ?, ?, ?, ?)`,
    [
      semester.id,
      semester.name,
      semester.startDate,
      semester.endDate,
      isActive,
    ],
  );

  // If it's the first semester, set it active
  if (isActive === 1) {
    return;
  }

  // Otherwise set it as active anyway (user just added it, they want to use it)
  await setActiveSemester(semester.id);
};

// Settings operations
export const getSetting = async (key: string): Promise<string | null> => {
  const database = await getDatabase();
  const result = await database.getFirstAsync<{ value: string }>(
    "SELECT value FROM settings WHERE key = ?",
    [key],
  );
  return result?.value || null;
};

export const setSetting = async (key: string, value: string) => {
  const database = await getDatabase();
  await database.runAsync(
    `INSERT OR REPLACE INTO settings (key, value) VALUES (?, ?)`,
    [key, value],
  );
};

// (Removed duplicate resetDatabase function to avoid redeclaration error)

// Clear all user data but keep structure
export const clearAllData = async () => {
  const database = await getDatabase();
  await database.execAsync(`
    DELETE FROM attendance;
    DELETE FROM shifted_classes;
    DELETE FROM tasks;
    DELETE FROM classes;
    DELETE FROM courses;
    DELETE FROM semesters;
    DELETE FROM attachments;
  `);
};

export const exportBackupData = async () => {
  const [semesters, courses, classes, tasks, attendance, attachments] = await Promise.all([
    getAllSemesters(),
    getAllCourses(),
    getAllClasses(),
    getAllTasks(),
    getAllAttendance(),
    getAllAttachments(),
  ]);
  return { semesters, courses, classes, tasks, attendance, attachments };
};

export const restoreBackupData = async (data: any) => {
  const database = await getDatabase();
  
  // Verify minimum data structure
  if (!data || typeof data !== 'object') throw new Error("Invalid backup format");

  // Execute EVERYTHING within an ACID secure transaction loop
  await database.withTransactionAsync(async () => {
    // Clear old data
    await clearAllData();

    // 1. Semesters
    if (Array.isArray(data.semesters)) {
      for (const s of data.semesters) {
        await database.runAsync(
          `INSERT INTO semesters (id, name, startDate, endDate, isActive) VALUES (?, ?, ?, ?, ?)`,
          [s.id, s.name, s.startDate, s.endDate, s.isActive ? 1 : 0]
        );
      }
    }

    // 2. Courses
    if (Array.isArray(data.courses)) {
      for (const c of data.courses) {
        await database.runAsync(
          `INSERT INTO courses (id, name, shortName, professor, credits, room, color, semesterId) VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
          [c.id, c.name, c.shortName, c.professor, c.credits, c.room, c.color, c.semesterId]
        );
      }
    }

    // 3. Classes
    if (Array.isArray(data.classes)) {
      for (const cl of data.classes) {
        await database.runAsync(
          `INSERT INTO classes (id, courseId, dayOfWeek, startTime, endTime, room, semesterId) VALUES (?, ?, ?, ?, ?, ?, ?)`,
          [cl.id, cl.courseId, cl.dayOfWeek, cl.startTime, cl.endTime, cl.room, cl.semesterId]
        );
      }
    }

    // 4. Tasks
    if (Array.isArray(data.tasks)) {
      for (const t of data.tasks) {
        await database.runAsync(
          `INSERT INTO tasks (id, courseId, title, description, dueDate, priority, status, createdAt, completedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`,
          [t.id, t.courseId, t.title, t.description || null, t.dueDate || null, t.priority, t.status, t.createdAt, t.completedAt || null]
        );
      }
    }

    // 5. Attendance
    if (Array.isArray(data.attendance)) {
      for (const a of data.attendance) {
        await database.runAsync(
          `INSERT INTO attendance (id, classId, date, status, semesterId, isLegacy, notes) VALUES (?, ?, ?, ?, ?, ?, ?)`,
          [a.id, a.classId, a.date, a.status, a.semesterId, a.isLegacy ? 1 : 0, a.notes || '']
        );
      }
    }

    // 6. Attachments
    if (Array.isArray(data.attachments)) {
      for (const at of data.attachments) {
        await database.runAsync(
          `INSERT INTO attachments (id, courseId, title, url, type, createdAt) VALUES (?, ?, ?, ?, ?, ?)`,
          [at.id, at.courseId, at.title, at.url, at.type || 'link', at.createdAt]
        );
      }
    }
  });
};
