import json
import os
from datetime import datetime, timedelta

def get_calendar_config(config_dir):
    """Carga la configuración del calendario académico."""
    path = os.path.join(config_dir, "calendar_config.json")
    if not os.path.exists(path):
        # Configuración por defecto basada en los datos del usuario
        return {
            "semesters": {
                "q1": {"start": "2026-09-02", "end": "2026-12-11"},
                "q2": {"start": "2027-01-25", "end": "2027-05-14"}
            },
            "holidays": [
                "2026-10-12", "2026-11-02", "2026-11-30", "2026-12-03", "2026-12-08",
                {"start": "2026-12-14", "end": "2027-01-24", "label": "Vacaciones/Exámenes"},
                "2027-03-19",
                {"start": "2027-03-25", "end": "2027-04-02", "label": "Semana Santa"},
                "2027-04-19"
            ]
        }
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)

def is_holiday(date_obj, config):
    """Comprueba si una fecha es festiva según la configuración."""
    date_str = date_obj.strftime("%Y-%m-%d")
    for h in config.get("holidays", []):
        if isinstance(h, str):
            if h == date_str: return True
        elif isinstance(h, dict):
            start = datetime.strptime(h["start"], "%Y-%m-%d")
            end = datetime.strptime(h["end"], "%Y-%m-%d")
            if start <= date_obj <= end: return True
    return False

def get_semester_for_date(date_obj, config):
    """Determina en qué cuatrimestre cae una fecha."""
    date_str = date_obj.strftime("%Y-%m-%d")
    for q_id, bounds in config.get("semesters", {}).items():
        start = datetime.strptime(bounds["start"], "%Y-%m-%d")
        end = datetime.strptime(bounds["end"], "%Y-%m-%d")
        if start <= date_obj <= end:
            return q_id
    return None

def get_schedule(config_dir, semester="q1"):
    """Lee el archivo de horario base del cuatrimestre seleccionado."""
    filename = f"schedule_{semester.lower()}.json"
    path = os.path.join(config_dir, filename)
    if not os.path.exists(path):
        old_path = os.path.join(config_dir, "schedule.json")
        if os.path.exists(old_path) and semester == "q1":
            with open(old_path, "r", encoding="utf-8") as f:
                data = json.load(f)
            save_schedule(config_dir, data, "q1")
            return data
        return {}
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)

def save_schedule(config_dir, schedule_data, semester="q1"):
    filename = f"schedule_{semester.lower()}.json"
    path = os.path.join(config_dir, filename)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(schedule_data, f, indent=4, ensure_ascii=False)
    return True

def get_exceptions(config_dir):
    path = os.path.join(config_dir, "schedule_exceptions.json")
    if not os.path.exists(path):
        return {}
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)

def save_exception(config_dir, date_str, daily_schedule):
    exceptions = get_exceptions(config_dir)
    if daily_schedule is None:
        if date_str in exceptions: del exceptions[date_str]
    else:
        exceptions[date_str] = daily_schedule
    path = os.path.join(config_dir, "schedule_exceptions.json")
    with open(path, "w", encoding="utf-8") as f:
        json.dump(exceptions, f, indent=4, ensure_ascii=False)
    return True

def get_day_schedule(config_dir, date_str, semester=None):
    """
    Obtiene el horario efectivo para un día concreto (Base o Excepción).
    """
    calendar = get_calendar_config(config_dir)
    date_obj = datetime.strptime(date_str, "%Y-%m-%d")
    
    if semester is None or semester == "AUTO":
        semester = get_semester_for_date(date_obj, calendar) or "q1"
        
    exceptions = get_exceptions(config_dir)
    if date_str in exceptions:
        return exceptions[date_str], True # (schedule, is_exception)
    
    day_name_en = date_obj.strftime("%A").lower()
    days_map = {"monday": "lunes", "tuesday": "martes", "wednesday": "miercoles", "thursday": "jueves", "friday": "viernes", "saturday": "sabado", "sunday": "domingo"}
    day_key = days_map.get(day_name_en, day_name_en)
    
    base_schedule = get_schedule(config_dir, semester)
    # Aseguramos que tengan session_type por defecto si no existe
    day_list = base_schedule.get(day_key, [])
    for item in day_list:
        if "type" not in item: item["type"] = "THEORY"
        
    return day_list, False

def get_semester_subjects(config_dir, semester="q1"):
    """Retorna la lista única de asignaturas de un cuatrimestre."""
    sched = get_schedule(config_dir, semester)
    subjects = set()
    for day in sched.values():
        for item in day:
            subjects.add(item["subject"])
    return sorted(list(subjects))

def is_academic_day(config_dir, date_str):
    """Verifica si un día es lectivo (L-V y no festivo)."""
    calendar = get_calendar_config(config_dir)
    date_obj = datetime.strptime(date_str, "%Y-%m-%d")
    if date_obj.weekday() >= 5: return False # Fin de semana
    if is_holiday(date_obj, calendar): return False
    # También debe estar dentro de algún semestre
    if get_semester_for_date(date_obj, calendar) is None: return False
    return True

def toggle_manual_attendance(config_dir, date_str, subject, current_status):
    """
    Cambia manualmente el estado de asistencia de una clase.
    Si era ATTENDED, elimina el log. Si era MISSED, crea uno manual.
    """
    logs_path = os.path.join(config_dir, "time_logs.json")
    logs = []
    if os.path.exists(logs_path):
        with open(logs_path, "r", encoding="utf-8") as f:
            try:
                logs = json.load(f)
            except:
                logs = []

    if current_status == "ATTENDED":
        # Eliminar el log correspondiente
        new_logs = []
        found = False
        for log in logs:
            # Comprobación estricta de fecha y asignatura
            if not found and log.get("mode") == "CLASS" and log.get("details") == subject:
                try:
                    log_time = datetime.strptime(log["timestamp"], "%Y-%m-%d %H:%M:%S")
                    if log_time.strftime("%Y-%m-%d") == date_str:
                        found = True
                        continue
                except:
                    pass
            new_logs.append(log)
        logs = new_logs
    else:
        # Crear un log manual (MISSED -> ATTENDED)
        # Usamos la fecha indicada y la hora actual del sistema para el registro
        # (Esto asegura que se vea como un evento ocurrido en el día solicitado)
        timestamp = f"{date_str} {datetime.now().strftime('%H:%M:%S')}"
        
        logs.append({
            "timestamp": timestamp,
            "mode": "CLASS",
            "event": "CHECKIN",
            "details": subject,
            "source": "MANUAL"
        })

    with open(logs_path, "w", encoding="utf-8") as f:
        json.dump(logs, f, indent=4, ensure_ascii=False)
    return True

def get_attendance_summary(config_dir, filter_type="DAY", semester=None, state_filter="ALL", base_date_str=None):
    """
    Cruza los logs con el horario, calendario y festivos.
    base_date_str: Fecha de referencia para el rango (YYYY-MM-DD).
    """
    calendar = get_calendar_config(config_dir)
    
    # SIMULACIÓN: Nuestra fecha "actual" (el punto de corte para UPCOMING)
    simulated_now = datetime(2027, 4, 12, 10, 0, 0) 
    
    # Fecha de referencia para el filtro (si no viene, usamos la simulación)
    if base_date_str:
        ref_date = datetime.strptime(base_date_str, "%Y-%m-%d")
    else:
        ref_date = simulated_now

    # Si no se especifica semestre, detectamos el de la fecha de referencia
    if semester is None or semester == "AUTO":
        semester = get_semester_for_date(ref_date, calendar) or "q1"

    base_schedule = get_schedule(config_dir, semester)
    exceptions = get_exceptions(config_dir)
    
    logs_path = os.path.join(config_dir, "time_logs.json")
    logs = []
    if os.path.exists(logs_path):
        with open(logs_path, "r", encoding="utf-8") as f:
            logs = json.load(f)

    class_logs = [log for log in logs if log.get("mode") == "CLASS"]
    
    # Definir rango de visualización relativo a ref_date
    if filter_type == "DAY":
        start_date = ref_date.replace(hour=0, minute=0, second=0, microsecond=0)
        end_view = start_date
    elif filter_type == "WEEK":
        # Inicio de la semana de la fecha de referencia
        start_date = (ref_date - timedelta(days=ref_date.weekday())).replace(hour=0, minute=0, second=0, microsecond=0)
        end_view = start_date + timedelta(days=6)
    elif filter_type == "MONTH":
        # Mes completo de la fecha de referencia
        start_date = ref_date.replace(day=1, hour=0, minute=0, second=0, microsecond=0)
        if ref_date.month == 12: 
            end_view = ref_date.replace(day=31, hour=23, minute=59)
        else: 
            end_view = (ref_date.replace(month=ref_date.month+1, day=1) - timedelta(days=1)).replace(hour=23, minute=59)
    elif filter_type == "SEMESTER":
        bounds = calendar.get("semesters", {}).get(semester, {})
        if not bounds and semester == "AUTO":
             semester = get_semester_for_date(ref_date, calendar) or "q1"
             bounds = calendar.get("semesters", {}).get(semester, {})
        start_date = datetime.strptime(bounds.get("start", "2026-01-01"), "%Y-%m-%d")
        end_view = datetime.strptime(bounds.get("end", "2026-12-31"), "%Y-%m-%d")
    elif filter_type == "YEAR":
        q1_bounds = calendar.get("semesters", {}).get("q1", {})
        q2_bounds = calendar.get("semesters", {}).get("q2", {})
        start_date = datetime.strptime(q1_bounds.get("start", "2026-01-01"), "%Y-%m-%d")
        end_view = datetime.strptime(q2_bounds.get("end", "2027-12-31"), "%Y-%m-%d")
    else:
        start_date = ref_date
        end_view = ref_date

    summary = []
    total_attended = 0
    total_missed = 0
    total_upcoming = 0

    current_date = start_date
    while current_date <= end_view:
        date_str = current_date.strftime("%Y-%m-%d")
        
        # REGLA 1: ¿Es festivo?
        holiday = is_holiday(current_date, calendar)
        
        # REGLA 2: ¿Está fuera del semestre seleccionado?
        # Solo aplicamos esta restricción si estamos generando el horario base.
        in_semester = get_semester_for_date(current_date, calendar) == semester
        
        if not holiday and in_semester:
            day_name_en = current_date.strftime("%A").lower()
            days_map = {"monday": "lunes", "tuesday": "martes", "wednesday": "miercoles", "thursday": "jueves", "friday": "viernes", "saturday": "sabado", "sunday": "domingo"}
            day_key = days_map.get(day_name_en, day_name_en)
            
            # Cargamos el base para comparar
            day_base = base_schedule.get(day_key, [])
            
            if date_str in exceptions:
                day_schedule = exceptions[date_str]
            else:
                day_schedule = day_base

            for item in day_schedule:
                subject = item.get("subject")
                start_time = item.get("start")
                end_time = item.get("end")
                
                # DETERMINAR SI ES EXCEPCIÓN REAL (Comparación profunda)
                is_manual_exception = False
                if date_str in exceptions:
                    # Buscamos si existe una clase IDÉNTICA en el horario base
                    is_manual_exception = True
                    for base_item in day_base:
                        # Si todos los campos clave coinciden, no es una excepción visual
                        if (base_item.get("subject") == subject and 
                            base_item.get("start") == start_time and 
                            base_item.get("end") == end_time and 
                            base_item.get("room") == item.get("room") and 
                            base_item.get("floor") == item.get("floor") and 
                            base_item.get("building") == item.get("building") and 
                            base_item.get("type", "THEORY") == item.get("type", "THEORY")):
                            is_manual_exception = False
                            break

                # Verificación de asistencia
                attended = False
                for log in class_logs:
                    log_time = datetime.strptime(log["timestamp"], "%Y-%m-%d %H:%M:%S")
                    if log_time.strftime("%Y-%m-%d") == date_str:
                        if log.get("details") == subject:
                            attended = True
                            break
                        log_hour = log_time.strftime("%H:%M")
                        if start_time <= log_hour <= end_time:
                            attended = True
                            break

                class_start_dt = datetime.strptime(f"{date_str} {start_time}", "%Y-%m-%d %H:%M")
                class_end_dt = datetime.strptime(f"{date_str} {end_time}", "%Y-%m-%d %H:%M")
                
                status = "MISSED"
                if attended: status = "ATTENDED"
                elif class_start_dt > simulated_now: status = "UPCOMING"
                elif class_start_dt <= simulated_now <= class_end_dt and not attended: status = "UPCOMING"

                if state_filter == "ALL" or state_filter == status:
                    summary.append({
                        "date": date_str,
                        "day": current_date.strftime("%A"),
                        "subject": subject,
                        "start": start_time,
                        "end": end_time,
                        "status": status,
                        "is_exception": is_manual_exception,
                        "type": item.get("type", "THEORY"),
                        "room": item.get("room", "-"),
                        "floor": item.get("floor", "-"),
                        "building": item.get("building", "-")
                    })
                
                if status == "ATTENDED": total_attended += 1
                elif status == "MISSED": total_missed += 1
                elif status == "UPCOMING": total_upcoming += 1
        elif holiday:
            # Podríamos añadir una entrada tipo "FESTIVO" si es vista de DÍA
            if filter_type == "DAY":
                summary.append({"date": date_str, "status": "HOLIDAY", "subject": "Festivo / Vacaciones"})

        current_date += timedelta(days=1)

    summary.sort(key=lambda x: (x.get("date", ""), x.get("start", "")), reverse=True)

    return {
        "records": summary,
        "stats": {
            "total_attended": total_attended,
            "total_missed": total_missed,
            "total_upcoming": total_upcoming,
            "percentage": round((total_attended / (total_attended + total_missed) * 100), 1) if (total_attended + total_missed) > 0 else 0
        },
        "detected_semester": semester
    }
