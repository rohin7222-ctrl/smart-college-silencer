"""
Smart Campus Silencer - Student Timetable & Class Schedule Parser
Validates whether class hours are currently active.
"""

from datetime import datetime, time
from typing import Dict, Any

# College timetable configuration
WEEKLY_SCHEDULE: Dict[str, Dict[str, Any]] = {
    "Monday": {"has_classes": True, "start": time(8, 45), "end": time(16, 30)},
    "Tuesday": {"has_classes": True, "start": time(8, 45), "end": time(16, 30)},
    "Wednesday": {"has_classes": True, "start": time(8, 45), "end": time(16, 30)},
    "Thursday": {"has_classes": True, "start": time(8, 45), "end": time(16, 30)},
    "Friday": {"has_classes": True, "start": time(8, 45), "end": time(16, 0)},
    "Saturday": {"has_classes": False, "start": None, "end": None}, # Alternate or holiday
    "Sunday": {"has_classes": False, "start": None, "end": None}
}

def is_class_in_session(now: datetime = None) -> Dict[str, Any]:
    now = now or datetime.now()
    day_name = now.strftime("%A")
    current_time = now.time()

    schedule = WEEKLY_SCHEDULE.get(day_name, {"has_classes": False})

    if not schedule["has_classes"]:
        return {
            "is_in_session": False,
            "reason": f"No scheduled classes on {day_name}.",
            "day": day_name,
            "current_time": current_time.strftime("%H:%M:%S")
        }

    start = schedule["start"]
    end = schedule["end"]

    if start <= current_time <= end:
        return {
            "is_in_session": True,
            "reason": f"Class is currently in session ({start.strftime('%H:%M')} to {end.strftime('%H:%M')}).",
            "day": day_name,
            "current_time": current_time.strftime("%H:%M:%S")
        }
    else:
        return {
            "is_in_session": False,
            "reason": f"Outside class hours ({start.strftime('%H:%M')} - {end.strftime('%H:%M')}).",
            "day": day_name,
            "current_time": current_time.strftime("%H:%M:%S")
        }

if __name__ == "__main__":
    result = is_class_in_session()
    print("Timetable Check:", result)
