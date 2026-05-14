import AsyncStorage from "@react-native-async-storage/async-storage";

export type PendingActionStatus = "pending" | "synced" | "failed";

export interface PendingAction {
  id: string;
  type: string;
  payload: any;
  createdAt: string;
  status: PendingActionStatus;
}

const STORAGE_KEY = "pendingActions";

export const loadQueue = async (): Promise<PendingAction[]> => {
  try {
    const raw = await AsyncStorage.getItem(STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : [];
  } catch (error) {
    console.error("Failed to load queue", error);
    return [];
  }
};

export const saveQueue = async (queue: PendingAction[]): Promise<void> => {
  try {
    await AsyncStorage.setItem(STORAGE_KEY, JSON.stringify(queue));
  } catch (error) {
    console.error("Failed to save queue", error);
  }
};

export const enqueueAction = async (
  queue: PendingAction[],
  action: PendingAction,
): Promise<PendingAction[]> => {
  const next = [...queue, action];
  await saveQueue(next);
  return next;
};

export const clearSynced = async (
  queue: PendingAction[],
): Promise<PendingAction[]> => {
  const pending = queue.filter((item) => item.status !== "synced");
  await saveQueue(pending);
  return pending;
};
