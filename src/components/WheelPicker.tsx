import React, { useRef, useEffect, useState } from "react";
import {
  View,
  Text,
  ScrollView,
  StyleSheet,
  NativeSyntheticEvent,
  NativeScrollEvent,
  Modal,
  TouchableOpacity,
} from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import { BlurView } from "expo-blur";
import { COLORS } from "../constants/data";

interface WheelPickerProps {
  items: string[];
  selectedValue: string;
  onValueChange: (value: string) => void;
  itemHeight?: number;
  visibleItems?: number;
}

const WheelPicker: React.FC<WheelPickerProps> = ({
  items,
  selectedValue,
  onValueChange,
  itemHeight = 50,
  visibleItems = 3,
}) => {
  const scrollViewRef = useRef<ScrollView>(null);
  const [isScrolling, setIsScrolling] = useState(false);

  const containerHeight = itemHeight * visibleItems;
  const paddingVertical = itemHeight * Math.floor(visibleItems / 2);

  useEffect(() => {
    if (!isScrolling) {
      const index = items.indexOf(selectedValue);
      if (index !== -1 && scrollViewRef.current) {
        scrollViewRef.current.scrollTo({
          y: index * itemHeight,
          animated: false,
        });
      }
    }
  }, [selectedValue, items, itemHeight, isScrolling]);

  const handleScrollEnd = (event: NativeSyntheticEvent<NativeScrollEvent>) => {
    const offsetY = event.nativeEvent.contentOffset.y;
    const index = Math.round(offsetY / itemHeight);
    const clampedIndex = Math.max(0, Math.min(index, items.length - 1));

    if (items[clampedIndex] !== selectedValue) {
      onValueChange(items[clampedIndex]);
    }

    setIsScrolling(false);
  };

  const handleScrollBegin = () => {
    setIsScrolling(true);
  };

  return (
    <View style={[styles.container, { height: containerHeight }]}>
      {/* Selection indicator */}
      <View
        style={[
          styles.selectionIndicator,
          {
            top: paddingVertical,
            height: itemHeight,
          },
        ]}
      />

      <ScrollView
        ref={scrollViewRef}
        showsVerticalScrollIndicator={false}
        snapToInterval={itemHeight}
        decelerationRate="fast"
        onScrollBeginDrag={handleScrollBegin}
        onMomentumScrollEnd={handleScrollEnd}
        onScrollEndDrag={(e) => {
          // Handle case where user stops without momentum
          if (e.nativeEvent.velocity?.y === 0) {
            handleScrollEnd(e);
          }
        }}
        contentContainerStyle={{
          paddingVertical,
        }}
      >
        {items.map((item, index) => {
          const isSelected = item === selectedValue;
          return (
            <View
              key={`${item}-${index}`}
              style={[styles.item, { height: itemHeight }]}
            >
              <Text
                style={[styles.itemText, isSelected && styles.selectedItemText]}
              >
                {item}
              </Text>
            </View>
          );
        })}
      </ScrollView>
    </View>
  );
};

interface TimeWheelPickerProps {
  hours: string;
  minutes: string;
  onHoursChange: (hours: string) => void;
  onMinutesChange: (minutes: string) => void;
}

export const TimeWheelPicker: React.FC<TimeWheelPickerProps> = ({
  hours,
  minutes,
  onHoursChange,
  onMinutesChange,
}) => {
  // Generate hours (00-23) and minutes (00-59 in 5-min increments)
  const hoursArray = Array.from({ length: 24 }, (_, i) =>
    i.toString().padStart(2, "0"),
  );
  const minutesArray = Array.from({ length: 12 }, (_, i) =>
    (i * 5).toString().padStart(2, "0"),
  );

  return (
    <View style={styles.timePickerContainer}>
      <WheelPicker
        items={hoursArray}
        selectedValue={hours}
        onValueChange={onHoursChange}
      />
      <Text style={styles.timeSeparator}>:</Text>
      <WheelPicker
        items={minutesArray}
        selectedValue={minutes}
        onValueChange={onMinutesChange}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    width: 70,
    overflow: "hidden",
    backgroundColor: "rgba(255, 255, 255, 0.05)",
    borderRadius: 12,
  },
  selectionIndicator: {
    position: "absolute",
    left: 0,
    right: 0,
    backgroundColor: "rgba(139, 92, 246, 0.15)",
    borderTopWidth: 1,
    borderBottomWidth: 1,
    borderColor: "rgba(139, 92, 246, 0.3)",
    zIndex: 1,
    pointerEvents: "none",
  },
  item: {
    justifyContent: "center",
    alignItems: "center",
  },
  itemText: {
    fontSize: 22,
    color: COLORS.text.muted,
    fontWeight: "400",
  },
  selectedItemText: {
    color: COLORS.text.primary,
    fontWeight: "600",
    fontSize: 24,
  },
  timePickerContainer: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
  },
  timeSeparator: {
    fontSize: 28,
    fontWeight: "600",
    color: COLORS.text.primary,
    marginHorizontal: 8,
  },
  datePickerContainer: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 8,
  },
  // Time Picker Modal Styles
  modalOverlay: {
    flex: 1,
    backgroundColor: "rgba(0, 0, 0, 0.7)",
    justifyContent: "center",
    alignItems: "center",
    padding: 20,
  },
  modalBlur: {
    borderRadius: 24,
    overflow: "hidden",
    width: "100%",
    maxWidth: 320,
  },
  modalContent: {
    padding: 24,
    borderRadius: 24,
    borderWidth: 1,
    borderColor: "rgba(255, 255, 255, 0.1)",
  },
  modalTitle: {
    color: COLORS.text.primary,
    fontSize: 20,
    fontWeight: "700",
    textAlign: "center",
    marginBottom: 24,
  },
  modalButtons: {
    flexDirection: "row",
    justifyContent: "space-between",
    marginTop: 24,
  },
  cancelBtn: {
    paddingVertical: 12,
    paddingHorizontal: 24,
  },
  cancelText: {
    color: COLORS.text.muted,
    fontSize: 16,
    fontWeight: "500",
  },
  confirmBtn: {
    paddingVertical: 12,
    paddingHorizontal: 28,
    borderRadius: 20,
  },
  confirmText: {
    color: "#fff",
    fontSize: 16,
    fontWeight: "600",
  },
});

// Time Picker Modal Component
interface TimePickerModalProps {
  visible: boolean;
  title: string;
  initialTime: string; // Format: "HH:MM"
  onConfirm: (time: string) => void;
  onCancel: () => void;
}

export const TimePickerModal: React.FC<TimePickerModalProps> = ({
  visible,
  title,
  initialTime,
  onConfirm,
  onCancel,
}) => {
  const [hours, minutes] = initialTime.split(":");
  const [selectedHours, setSelectedHours] = useState(hours || "08");
  const [selectedMinutes, setSelectedMinutes] = useState(
    minutes?.substring(0, 2) || "00",
  );

  // Reset when modal opens with new initial time
  useEffect(() => {
    if (visible) {
      const [h, m] = initialTime.split(":");
      setSelectedHours(h || "08");
      setSelectedMinutes(m?.substring(0, 2) || "00");
    }
  }, [visible, initialTime]);

  const handleConfirm = () => {
    onConfirm(`${selectedHours}:${selectedMinutes}`);
  };

  return (
    <Modal
      visible={visible}
      transparent
      animationType="fade"
      onRequestClose={onCancel}
    >
      <View style={styles.modalOverlay}>
        <BlurView intensity={40} tint="dark" style={styles.modalBlur}>
          <LinearGradient
            colors={["rgba(255, 255, 255, 0.15)", "rgba(255, 255, 255, 0.05)"]}
            style={styles.modalContent}
          >
            <Text style={styles.modalTitle}>{title}</Text>

            <TimeWheelPicker
              hours={selectedHours}
              minutes={selectedMinutes}
              onHoursChange={setSelectedHours}
              onMinutesChange={setSelectedMinutes}
            />

            <View style={styles.modalButtons}>
              <TouchableOpacity style={styles.cancelBtn} onPress={onCancel}>
                <Text style={styles.cancelText}>Cancel</Text>
              </TouchableOpacity>

              <TouchableOpacity onPress={handleConfirm}>
                <LinearGradient
                  colors={["#8b5cf6", "#7c3aed"]}
                  style={styles.confirmBtn}
                >
                  <Text style={styles.confirmText}>Confirm</Text>
                </LinearGradient>
              </TouchableOpacity>
            </View>
          </LinearGradient>
        </BlurView>
      </View>
    </Modal>
  );
};

// Date Picker Modal Component
interface DatePickerModalProps {
  visible: boolean;
  title: string;
  initialDate: string; // Format: "YYYY-MM-DD"
  onConfirm: (date: string) => void;
  onCancel: () => void;
  minDate?: Date;
  maxDate?: Date;
}

const DateWheelPicker: React.FC<{
  year: string;
  month: string;
  day: string;
  onYearChange: (year: string) => void;
  onMonthChange: (month: string) => void;
  onDayChange: (day: string) => void;
  minDate?: Date;
  maxDate?: Date;
}> = ({
  year,
  month,
  day,
  onYearChange,
  onMonthChange,
  onDayChange,
  minDate,
  maxDate,
}) => {
  // Generate years (current year - 1 to current year + 5)
  const currentYear = new Date().getFullYear();
  const minYear = minDate ? minDate.getFullYear() : currentYear - 1;
  const maxYear = maxDate ? maxDate.getFullYear() : currentYear + 5;
  const yearsArray = Array.from({ length: maxYear - minYear + 1 }, (_, i) =>
    (minYear + i).toString(),
  );

  // Month names
  const monthNames = [
    "Jan",
    "Feb",
    "Mar",
    "Apr",
    "May",
    "Jun",
    "Jul",
    "Aug",
    "Sep",
    "Oct",
    "Nov",
    "Dec",
  ];
  const monthsArray = monthNames;

  // Calculate days in selected month
  const selectedYear = parseInt(year);
  const selectedMonth = monthNames.indexOf(month);
  const daysInMonth = new Date(selectedYear, selectedMonth + 1, 0).getDate();
  const daysArray = Array.from({ length: daysInMonth }, (_, i) =>
    (i + 1).toString().padStart(2, "0"),
  );

  // Adjust day if it exceeds days in month
  useEffect(() => {
    const dayNum = parseInt(day);
    if (dayNum > daysInMonth) {
      onDayChange(daysInMonth.toString().padStart(2, "0"));
    }
  }, [month, year, daysInMonth]);

  return (
    <View style={styles.datePickerContainer}>
      <WheelPicker
        items={monthsArray}
        selectedValue={month}
        onValueChange={onMonthChange}
        itemHeight={45}
      />
      <WheelPicker
        items={daysArray}
        selectedValue={day}
        onValueChange={onDayChange}
        itemHeight={45}
      />
      <WheelPicker
        items={yearsArray}
        selectedValue={year}
        onValueChange={onYearChange}
        itemHeight={45}
      />
    </View>
  );
};

export const DatePickerModal: React.FC<DatePickerModalProps> = ({
  visible,
  title,
  initialDate,
  onConfirm,
  onCancel,
  minDate,
  maxDate,
}) => {
  const monthNames = [
    "Jan",
    "Feb",
    "Mar",
    "Apr",
    "May",
    "Jun",
    "Jul",
    "Aug",
    "Sep",
    "Oct",
    "Nov",
    "Dec",
  ];

  const parseInitialDate = () => {
    if (initialDate && initialDate.match(/^\d{4}-\d{2}-\d{2}$/)) {
      const [y, m, d] = initialDate.split("-");
      return {
        year: y,
        month: monthNames[parseInt(m) - 1],
        day: d,
      };
    }
    const today = new Date();
    return {
      year: today.getFullYear().toString(),
      month: monthNames[today.getMonth()],
      day: today.getDate().toString().padStart(2, "0"),
    };
  };

  const initial = parseInitialDate();
  const [selectedYear, setSelectedYear] = useState(initial.year);
  const [selectedMonth, setSelectedMonth] = useState(initial.month);
  const [selectedDay, setSelectedDay] = useState(initial.day);

  // Reset when modal opens
  useEffect(() => {
    if (visible) {
      const parsed = parseInitialDate();
      setSelectedYear(parsed.year);
      setSelectedMonth(parsed.month);
      setSelectedDay(parsed.day);
    }
  }, [visible, initialDate]);

  const handleConfirm = () => {
    const monthIndex = monthNames.indexOf(selectedMonth) + 1;
    const formattedMonth = monthIndex.toString().padStart(2, "0");
    onConfirm(`${selectedYear}-${formattedMonth}-${selectedDay}`);
  };

  return (
    <Modal
      visible={visible}
      transparent
      animationType="fade"
      onRequestClose={onCancel}
    >
      <View style={styles.modalOverlay}>
        <BlurView intensity={40} tint="dark" style={styles.modalBlur}>
          <LinearGradient
            colors={["rgba(255, 255, 255, 0.15)", "rgba(255, 255, 255, 0.05)"]}
            style={styles.modalContent}
          >
            <Text style={styles.modalTitle}>{title}</Text>

            <DateWheelPicker
              year={selectedYear}
              month={selectedMonth}
              day={selectedDay}
              onYearChange={setSelectedYear}
              onMonthChange={setSelectedMonth}
              onDayChange={setSelectedDay}
              minDate={minDate}
              maxDate={maxDate}
            />

            <View style={styles.modalButtons}>
              <TouchableOpacity style={styles.cancelBtn} onPress={onCancel}>
                <Text style={styles.cancelText}>Cancel</Text>
              </TouchableOpacity>

              <TouchableOpacity onPress={handleConfirm}>
                <LinearGradient
                  colors={["#8b5cf6", "#7c3aed"]}
                  style={styles.confirmBtn}
                >
                  <Text style={styles.confirmText}>Confirm</Text>
                </LinearGradient>
              </TouchableOpacity>
            </View>
          </LinearGradient>
        </BlurView>
      </View>
    </Modal>
  );
};

export default WheelPicker;
