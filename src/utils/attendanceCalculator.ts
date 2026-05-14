/**
 * Attendance Intelligence System
 * Calculates attendance predictions, risk levels, and recovery plans
 */

export interface AttendanceAnalysis {
  // Current stats
  totalClasses: number;
  attendedClasses: number;
  missedClasses: number;
  canceledClasses: number;
  currentPercentage: number;

  // Risk assessment
  riskLevel: "safe" | "borderline" | "critical";
  riskColor: string;
  riskIcon: string;
  riskMessage: string;

  // Predictions
  classesCanMiss: number;
  classesToRecover: number;
  predictedPercentage: number;

  // Recommendations
  recommendation: string;
  urgency: "low" | "medium" | "high";
}

export interface AttendancePrediction {
  date: string;
  predictedAttendance: number;
  predictedPercentage: number;
  riskLevel: "safe" | "borderline" | "critical";
}

/**
 * Calculate comprehensive attendance analysis for a course
 */
export const calculateAttendanceAnalysis = (
  attended: number,
  total: number,
  canceled: number = 0,
  requiredPercentage: number = 75,
  remainingClasses: number = 0,
): AttendanceAnalysis => {
  const effectiveTotal = total - canceled;
  const missed = effectiveTotal - attended;
  const currentPercentage =
    effectiveTotal > 0 ? Math.round((attended / effectiveTotal) * 100) : 100;

  // Calculate classes that can be missed while maintaining required percentage
  // Formula: (attended / (effectiveTotal + x)) >= requiredPercentage/100
  // Solving for x: x <= (attended * 100 / requiredPercentage) - effectiveTotal
  const classesCanMiss = Math.max(
    0,
    Math.floor((attended * 100) / requiredPercentage - effectiveTotal),
  );

  // Calculate classes needed to recover if below requirement
  // Formula: ((attended + y) / (effectiveTotal + y)) >= requiredPercentage/100
  // Solving for y: y >= (requiredPercentage * effectiveTotal - 100 * attended) / (100 - requiredPercentage)
  let classesToRecover = 0;
  if (currentPercentage < requiredPercentage) {
    classesToRecover = Math.ceil(
      (requiredPercentage * effectiveTotal - 100 * attended) /
        (100 - requiredPercentage),
    );
  }

  // Determine risk level
  let riskLevel: "safe" | "borderline" | "critical";
  let riskColor: string;
  let riskIcon: string;
  let riskMessage: string;
  let recommendation: string;
  let urgency: "low" | "medium" | "high";

  if (currentPercentage >= 85) {
    riskLevel = "safe";
    riskColor = "#22c55e";
    riskIcon = "shield-checkmark";
    riskMessage = "Excellent attendance!";
    recommendation =
      classesCanMiss > 0
        ? `You can safely miss ${classesCanMiss} more class${classesCanMiss > 1 ? "es" : ""}`
        : "Keep up the good work!";
    urgency = "low";
  } else if (currentPercentage >= requiredPercentage) {
    riskLevel = "borderline";
    riskColor = "#f59e0b";
    riskIcon = "alert-circle";
    riskMessage = "On the edge";
    recommendation =
      classesCanMiss > 0
        ? `You can miss only ${classesCanMiss} more class${classesCanMiss > 1 ? "es" : ""}`
        : "Attend all remaining classes to stay safe";
    urgency = "medium";
  } else {
    riskLevel = "critical";
    riskColor = "#ef4444";
    riskIcon = "warning";
    riskMessage = "Below requirement!";
    recommendation =
      classesToRecover > 0
        ? `Attend ${classesToRecover} consecutive class${classesToRecover > 1 ? "es" : ""} to recover`
        : "Immediate attention needed";
    urgency = "high";
  }

  // Calculate predicted percentage after remaining classes (assuming attendance)
  const futureTotal = effectiveTotal + remainingClasses;
  const futureAttended = attended + remainingClasses;
  const predictedPercentage =
    futureTotal > 0
      ? Math.round((futureAttended / futureTotal) * 100)
      : currentPercentage;

  return {
    totalClasses: total,
    attendedClasses: attended,
    missedClasses: missed,
    canceledClasses: canceled,
    currentPercentage,
    riskLevel,
    riskColor,
    riskIcon,
    riskMessage,
    classesCanMiss,
    classesToRecover,
    predictedPercentage,
    recommendation,
    urgency,
  };
};

/**
 * Calculate how attendance would change with different scenarios
 */
export const simulateAttendance = (
  currentAttended: number,
  currentTotal: number,
  additionalClasses: number,
  attendAll: boolean = true,
): { percentage: number; riskLevel: "safe" | "borderline" | "critical" } => {
  const newTotal = currentTotal + additionalClasses;
  const newAttended = attendAll
    ? currentAttended + additionalClasses
    : currentAttended;

  const percentage =
    newTotal > 0 ? Math.round((newAttended / newTotal) * 100) : 100;

  let riskLevel: "safe" | "borderline" | "critical";
  if (percentage >= 85) {
    riskLevel = "safe";
  } else if (percentage >= 75) {
    riskLevel = "borderline";
  } else {
    riskLevel = "critical";
  }

  return { percentage, riskLevel };
};

/**
 * Get color based on attendance percentage
 */
export const getAttendanceColor = (percentage: number): string => {
  if (percentage >= 85) return "#22c55e";
  if (percentage >= 75) return "#f59e0b";
  return "#ef4444";
};

/**
 * Get gradient colors based on attendance percentage
 */
export const getAttendanceGradient = (percentage: number): [string, string] => {
  if (percentage >= 85) return ["#22c55e", "#16a34a"];
  if (percentage >= 75) return ["#f59e0b", "#d97706"];
  return ["#ef4444", "#dc2626"];
};

/**
 * Format attendance message for display
 */
export const getAttendanceMessage = (analysis: AttendanceAnalysis): string => {
  const { currentPercentage, riskLevel, classesCanMiss, classesToRecover } =
    analysis;

  if (riskLevel === "critical") {
    return `⚠️ ${currentPercentage}% - Attend next ${classesToRecover} classes!`;
  }
  if (riskLevel === "borderline") {
    return `⚡ ${currentPercentage}% - Can miss ${classesCanMiss} more`;
  }
  return `✅ ${currentPercentage}% - ${classesCanMiss} classes buffer`;
};
