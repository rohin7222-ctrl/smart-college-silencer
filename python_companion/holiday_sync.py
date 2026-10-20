"""
Smart Campus Silencer - Academic Holiday & Leave Manager (Python Component)
Prevents unwanted muting on college holidays, weekends, semester breaks, and personal leaves.
"""

from datetime import datetime, date
from typing import Dict, Optional

# Standard Academic & State Holidays (Tamil Nadu / Central College Calendar)
ACADEMIC_HOLIDAYS: Dict[str, str] = {
    "2026-01-01": "New Year's Day",
    "2026-01-14": "Pongal Festival",
    "2026-01-15": "Thiruvalluvar Day",
    "2026-01-16": "Uzhavar Thirunal",
    "2026-01-26": "Republic Day",
    "2026-04-14": "Tamil New Year / Dr. Ambedkar Jayanti",
    "2026-05-01": "May Day",
    "2026-08-15": "Independence Day",
    "2026-10-02": "Gandhi Jayanti",
    "2026-10-20": "Ayutha Pooja / Special Academic Leave",
    "2026-11-08": "Deepavali Festival",
    "2026-12-25": "Christmas Day"
}

# Personal leave registry (can be marked dynamically via app or terminal)
PERSONAL_LEAVES = set()

def is_today_holiday(check_date: Optional[date] = None) -> Dict[str, any]:
    """
    Checks if the given date (default today) is a college holiday or weekend.
    Returns:
        dict: {'is_holiday': bool, 'reason': str, 'day_name': str}
    """
    target = check_date or date.today()
    date_str = target.strftime("%Y-%m-%d")
    day_name = target.strftime("%A")

    # 1. Weekend Check (Sundays are always closed)
    if day_name == "Sunday":
        return {
            "is_holiday": True,
            "reason": "Sunday (College Closed)",
            "day_name": day_name,
            "date": date_str
        }

    # 2. Government / Academic Calendar Check
    if date_str in ACADEMIC_HOLIDAYS:
        return {
            "is_holiday": True,
            "reason": f"Official Holiday: {ACADEMIC_HOLIDAYS[date_str]}",
            "day_name": day_name,
            "date": date_str
        }

    # 3. Personal / Sick Leave Check
    if date_str in PERSONAL_LEAVES:
        return {
            "is_holiday": True,
            "reason": "Personal Leave Marked by Student",
            "day_name": day_name,
            "date": date_str
        }

    return {
        "is_holiday": False,
        "reason": "Regular College Working Day",
        "day_name": day_name,
        "date": date_str
    }

def mark_personal_leave(target_date: Optional[date] = None) -> str:
    target = target_date or date.today()
    date_str = target.strftime("%Y-%m-%d")
    PERSONAL_LEAVES.add(date_str)
    return f"Marked {date_str} ({target.strftime('%A')}) as Personal Leave. Phone will NOT mute."

if __name__ == "__main__":
    status = is_today_holiday()
    print(f"Date: {status['date']} ({status['day_name']})")
    print(f"Is College Holiday: {status['is_holiday']}")
    print(f"Reason: {status['reason']}")
