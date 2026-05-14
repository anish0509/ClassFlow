import { Course } from "./index";

export type RootStackParamList = {
  MainTabs: undefined;
  CourseDetails: { courseId?: string; course?: Course };
  NotificationSettings: undefined;
};
