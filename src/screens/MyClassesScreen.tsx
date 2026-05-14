import React, { useEffect, useMemo, useState } from "react";
import {
  View,
  Text,
  ScrollView,
  StyleSheet,
  TouchableOpacity,
  Alert,
  ActivityIndicator,
} from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import BackgroundMesh from "../components/BackgroundMesh";
import { BlurView } from "expo-blur";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation } from "@react-navigation/native";
import {
  GlassHeader,
  CourseCard,
  AddCourseModal,
  AddClassModal,
  EmptyStateCard,
} from "../components";
import { useTimetableStore } from "../store/timetableStore";
import { useShallow } from "zustand/react/shallow";
import { Course } from "../types";
import {
  ThemePalette,
  getBackgroundGradient,
  useThemedColors,
  useThemedStyles,
} from "../theme/useTheme";

const MyClassesScreen: React.FC = () => {
  const insets = useSafeAreaInsets();
  const navigation = useNavigation<any>();
  const { colors } = useThemedColors();
  const styles = useThemedStyles(createStyles);
  const {
    courses,
    semesters,
    isLoading,
    isInitialized,
    addCourse,
    addClass,
    deleteCourse,
    loadCourses,
  } = useTimetableStore(
    useShallow((state) => ({
      courses: state.courses,
      semesters: state.semesters,
      isLoading: state.isLoading,
      isInitialized: state.isInitialized,
      addCourse: state.addCourse,
      addClass: state.addClass,
      deleteCourse: state.deleteCourse,
      loadCourses: state.loadCourses,
    }))
  );

  const [showAddCourseModal, setShowAddCourseModal] = useState(false);
  const [showAddClassModal, setShowAddClassModal] = useState(false);
  const [selectedCourse, setSelectedCourse] = useState<Course | null>(null);

  const currentSemester = useMemo(
    () => semesters.find((s) => s.isActive) || semesters[0],
    [semesters],
  );

  useEffect(() => {
    if (isInitialized) {
      loadCourses();
    }
  }, [isInitialized, loadCourses, currentSemester?.id]);

  const stats = useMemo(() => {
    const totalCourses = courses.length;
    const totalClasses = courses.reduce(
      (sum, course) => sum + (course.totalClasses || 0),
      0,
    );
    const attended = courses.reduce(
      (sum, course) => sum + (course.attendedClasses || 0),
      0,
    );
    const attendancePct = totalClasses
      ? Math.round((attended / totalClasses) * 100)
      : 0;

    return { totalCourses, totalClasses, attended, attendancePct };
  }, [courses]);

  const backgroundGradient = useMemo(
    () => getBackgroundGradient(colors),
    [colors],
  );

  const handleAddCourse = async (
    course: Parameters<typeof addCourse>[0],
  ) => {
    await addCourse({
      ...course,
      semesterId: course.semesterId || currentSemester?.id,
    });
  };

  const handleAddClass = async (classData: Parameters<typeof addClass>[0]) => {
    try {
      await addClass(classData);
      setShowAddClassModal(false);
      setSelectedCourse(null);
    } catch (error) {
      const message = error instanceof Error ? error.message : "Failed to add class";
      Alert.alert("Add Class", message);
    }
  };

  const handleCoursePress = (course: Course) => {
    navigation.navigate("CourseDetails", { courseId: course.id });
  };

  const handleCourseOptions = (course: Course) => {
    Alert.alert("Course options", course.name, [
      {
        text: "Add class",
        onPress: () => {
          setSelectedCourse(course);
          setShowAddClassModal(true);
        },
      },
      {
        text: "Delete",
        style: "destructive",
        onPress: () => deleteCourse(course.id),
      },
      { text: "Cancel", style: "cancel" },
    ]);
  };

  return (
    <LinearGradient colors={backgroundGradient} style={styles.container}>
      <BackgroundMesh />
      <GlassHeader
        title="My Courses"
        subtitle={`${stats.totalCourses} courses`}
        rightComponent={
          <View style={styles.headerActions}>
            <TouchableOpacity
              style={styles.headerButton}
              onPress={() => setShowAddCourseModal(true)}
            >
              <Ionicons name="add" size={20} color={colors.text.primary} />
            </TouchableOpacity>
            <TouchableOpacity
              style={styles.headerButton}
              onPress={() => setShowAddClassModal(true)}
            >
              <Ionicons name="calendar-outline" size={20} color={colors.text.primary} />
            </TouchableOpacity>
          </View>
        }
      />

      {isLoading ? (
        <View style={styles.loadingContainer}>
          <ActivityIndicator size="large" color={colors.primary.blue} />
        </View>
      ) : (
        <ScrollView
          style={styles.scrollView}
          contentContainerStyle={[
            styles.scrollContent,
            { paddingBottom: insets.bottom + 130 },
          ]}
          showsVerticalScrollIndicator={false}
        >


          <View style={styles.sectionHeader}>
            <Text style={styles.sectionTitle}>Courses</Text>
            <Text style={styles.sectionSubtitle}>
              Tap to view details, hold for options
            </Text>
          </View>

          {courses.length ? (
            courses.map((course) => (
              <CourseCard
                key={course.id}
                course={course}
                onPress={() => handleCoursePress(course)}
                onLongPress={() => handleCourseOptions(course)}
              />
            ))
          ) : (
            <EmptyStateCard
              icon="school-outline"
              title="No courses yet"
              description="Add your first course to get started!"
              buttonText="Add Course"
              onPress={() => setShowAddCourseModal(true)}
            />
          )}
        </ScrollView>
      )}

      <AddCourseModal
        visible={showAddCourseModal}
        onClose={() => setShowAddCourseModal(false)}
        onSubmit={handleAddCourse}
        semesterId={currentSemester?.id || semesters[0]?.id || ""}
      />

      <AddClassModal
        visible={showAddClassModal}
        onClose={() => {
          setShowAddClassModal(false);
          setSelectedCourse(null);
        }}
        onSubmit={handleAddClass}
        courses={courses}
        semesterId={currentSemester?.id || semesters[0]?.id || ""}
        preselectedCourseId={selectedCourse?.id}
      />
    </LinearGradient>
  );
};

const createStyles = (colors: ThemePalette) =>
  StyleSheet.create({
    container: {
      flex: 1,
    },
    headerActions: {
      flexDirection: "row",
      alignItems: "center",
      gap: 10,
    },
    headerButton: {
      width: 44,
      height: 44,
      borderRadius: 22,
      backgroundColor: colors.glass.light,
      alignItems: "center",
      justifyContent: "center",
      borderWidth: 1,
      borderColor: colors.glass.border,
    },
    loadingContainer: {
      flex: 1,
      alignItems: "center",
      justifyContent: "center",
    },
    scrollView: {
      flex: 1,
    },
    scrollContent: {
      paddingTop: 20,
    },
    statsCard: {
      marginHorizontal: 16,
      marginBottom: 20,
      borderRadius: 20,
      overflow: "hidden",
    },
    statsBlur: {
      borderRadius: 20,
    },
    statsGradient: {
      borderRadius: 18,
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: colors.glass.cardBorder,
      padding: 20,
      backgroundColor: colors.glass.cardFill,
    },
    statsContent: {
      flexDirection: "row",
      justifyContent: "space-around",
      alignItems: "center",
      marginBottom: 16,
    },
    statItem: {
      alignItems: "center",
    },
    statValue: {
      color: colors.text.primary,
      fontSize: 28,
      fontWeight: "700",
      marginBottom: 4,
    },
    goodStat: {
      color: colors.primary.green,
    },
    warnStat: {
      color: colors.primary.orange,
    },
    statLabel: {
      color: colors.text.muted,
      fontSize: 12,
      textAlign: "center",
    },
    statDivider: {
      width: StyleSheet.hairlineWidth,
      height: 40,
      backgroundColor: colors.border.subtle,
    },
    progressContainer: {
      marginTop: 8,
    },
    progressBar: {
      height: 4,
      backgroundColor: colors.glass.badge,
      borderRadius: 2,
      overflow: "hidden",
    },
    progressFill: {
      height: "100%",
      borderRadius: 3,
    },
    progressLabel: {
      color: colors.text.muted,
      fontSize: 12,
      textAlign: "center",
      marginTop: 8,
    },
    sectionHeader: {
      paddingHorizontal: 20,
      marginBottom: 12,
    },
    sectionTitle: {
      color: colors.text.primary,
      fontSize: 20,
      fontWeight: "700",
    },
    sectionSubtitle: {
      color: colors.text.muted,
      fontSize: 13,
      marginTop: 2,
    },
    emptyState: {
      alignItems: "center",
      justifyContent: "center",
      paddingVertical: 60,
      paddingHorizontal: 20,
    },
    emptyText: {
      color: colors.text.primary,
      fontSize: 20,
      fontWeight: "600",
      marginTop: 16,
      marginBottom: 8,
    },
    emptySubtext: {
      color: colors.text.muted,
      fontSize: 16,
      marginBottom: 20,
      textAlign: "center",
    },
    emptyButton: {
      backgroundColor: colors.glass.cardFill,
      paddingHorizontal: 24,
      paddingVertical: 12,
      borderRadius: 18,
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: colors.primary.teal + '40',
    },
    emptyButtonText: {
      color: colors.primary.teal,
      fontSize: 16,
      fontWeight: "600",
    },
  });

export default MyClassesScreen;
