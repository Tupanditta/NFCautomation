import json
import os
from datetime import datetime

def calculate_attendance_stats(records):
    """
    Procesa la lista de registros y devuelve diccionarios listos para graficar.
    """
    # 1. Por Día de la Semana
    days_stats = {
        "Lunes": {"total": 0, "missed": 0},
        "Martes": {"total": 0, "missed": 0},
        "Miércoles": {"total": 0, "missed": 0},
        "Jueves": {"total": 0, "missed": 0},
        "Viernes": {"total": 0, "missed": 0}
    }
    
    # Mapeo de nombres que vienen de Python (datetime/attendance)
    day_mapping = {
        "Monday": "Lunes", "Tuesday": "Martes", "Wednesday": "Miércoles", 
        "Thursday": "Jueves", "Friday": "Viernes"
    }

    # 2. Por Asignatura
    subject_stats = {}

    # 3. Por Intervalos Horarios
    # Pedido: 15-17, 17-19, 19-21
    intervals = {
        "15:00-17:00": {"total": 0, "missed": 0},
        "17:00-19:00": {"total": 0, "missed": 0},
        "19:00-21:00": {"total": 0, "missed": 0}
    }

    for r in records:
        if r.get("status") == "HOLIDAY":
            continue
            
        status = r.get("status")
        if status == "UPCOMING":
            continue

        # Procesar Día
        raw_day = r.get("day")
        day_es = day_mapping.get(raw_day, raw_day)
        if day_es in days_stats:
            days_stats[day_es]["total"] += 1
            if status == "MISSED":
                days_stats[day_es]["missed"] += 1

        # Procesar Asignatura
        subj = r.get("subject", "Unknown")
        if subj not in subject_stats:
            subject_stats[subj] = {"total": 0, "missed": 0}
        subject_stats[subj]["total"] += 1
        if status == "MISSED":
            subject_stats[subj]["missed"] += 1

        # Procesar Intervalo
        start_time = r.get("start", "00:00")
        try:
            if "15:00" <= start_time < "17:00":
                intervals["15:00-17:00"]["total"] += 1
                if status == "MISSED": intervals["15:00-17:00"]["missed"] += 1
            elif "17:00" <= start_time < "19:00":
                intervals["17:00-19:00"]["total"] += 1
                if status == "MISSED": intervals["17:00-19:00"]["missed"] += 1
            elif "19:00" <= start_time <= "21:00":
                intervals["19:00-21:00"]["total"] += 1
                if status == "MISSED": intervals["19:00-21:00"]["missed"] += 1
        except:
            pass

    return {
        "days": days_stats,
        "subjects": subject_stats,
        "intervals": intervals
    }
