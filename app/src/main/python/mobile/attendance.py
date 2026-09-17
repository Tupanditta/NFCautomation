import json
import os
from datetime import datetime, timedelta
from mobile.utils.translator import translate

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
            if start <= date_obj <= end: return h
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
        data = json.load(f)
        # Ordenar cada día por hora de inicio
        for day in data:
            if isinstance(data[day], list):
                data[day].sort(key=lambda x: x.get("start", "00:00"))
        return data

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
    try:
        with open(path, "r", encoding="utf-8") as f:
            return json.load(f)
    except:
        return {}

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
    has_daily_exception = date_str in exceptions
    
    day_name_en = date_obj.strftime("%A").lower()
    days_map = {"monday": "lunes", "tuesday": "martes", "wednesday": "miercoles", "thursday": "jueves", "friday": "viernes", "saturday": "sabado", "sunday": "domingo"}
    day_key = days_map.get(day_name_en, day_name_en)
    
    base_schedule = get_schedule(config_dir, semester)
    day_base = base_schedule.get(day_key, [])
    
    # Aseguramos que tengan session_type y subject por defecto si no existe e is_manual=False
    for item in day_base:
        if "type" not in item: item["type"] = "THEORY"
        if not item.get("subject"): 
            item["subject"] = item.get("label", "")
        item["is_manual"] = False
        
    if not has_daily_exception:
        day_base.sort(key=lambda x: x.get("start", "00:00"))
        return day_base, False
    
    # Si hay excepción, calculamos qué items son manuales (no estaban en el base)
    enriched_schedule = []
    day_schedule = exceptions[date_str]
    
    for item in day_schedule:
        is_manual = True
        for base_item in day_base:
            base_subj = base_item.get("subject") or base_item.get("label", "")
            item_subj = item.get("subject") or item.get("label", "")
            
            if (base_subj == item_subj and 
                base_item.get("start") == item.get("start") and 
                base_item.get("end") == item.get("end") and 
                base_item.get("room") == item.get("room") and 
                base_item.get("floor") == item.get("floor") and 
                base_item.get("building") == item.get("building") and 
                base_item.get("type", "THEORY") == item.get("type", "THEORY")):
                is_manual = False
                break
        
        item_copy = dict(item)
        item_copy["is_manual"] = is_manual
        if "type" not in item_copy: item_copy["type"] = "THEORY"
        if not item_copy.get("subject"):
            item_copy["subject"] = item_copy.get("label", "")
        enriched_schedule.append(item_copy)
    
    # Ordenar por hora de inicio
    enriched_schedule.sort(key=lambda x: x.get("start", "00:00"))
    return enriched_schedule, True

def get_semester_subjects(config_dir, semester="q1"):
    """Retorna la lista única de asignaturas de un cuatrimestre."""
    sched = get_schedule(config_dir, semester)
    subjects = set()
    for day in sched.values():
        for item in day:
            subjects.add(item["subject"])
    return sorted(list(subjects))

def is_academic_day(config_dir, date_str):
    """Verifica si un día está dentro del periodo lectivo (dentro de semestre y no festivo)."""
    calendar = get_calendar_config(config_dir)
    date_obj = datetime.strptime(date_str, "%Y-%m-%d")
    
    # Eliminamos la restricción de fin de semana (L-V) para permitir eventos en S-D
    if is_holiday(date_obj, calendar): return False
    
    # Debe estar dentro de algún semestre para cargar el horario base
    if get_semester_for_date(date_obj, calendar) is None: return False
    return True

def toggle_manual_attendance(config_dir, date_str, subject, start_time, current_status):
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
            # Comprobación estricta de fecha, asignatura y ventana horaria
            if not found and log.get("mode") == "CLASS" and log.get("details") == subject:
                try:
                    log_time = datetime.strptime(log["timestamp"], "%Y-%m-%d %H:%M:%S")
                    log_date = log_time.strftime("%Y-%m-%d")
                    log_hour = log_time.strftime("%H:%M")
                    
                    # Si coincide la fecha Y (es el mismo subject Y está en el rango de esa clase)
                    if log_date == date_str:
                        # Buscamos si el log cae en la clase que intentamos desmarcar
                        # (O si es un registro manual sin hora específica pero misma fecha)
                        if log.get("source") == "MANUAL" or (start_time and log_hour >= start_time):
                             found = True
                             continue
                except:
                    pass
            new_logs.append(log)
        logs = new_logs
    else:
        # Crear un log manual (MISSED -> ATTENDED)
        # Usamos la fecha indicada y, si tenemos start_time, lo usamos como referencia 
        # para que el summary lo detecte correctamente.
        time_part = datetime.now().strftime('%H:%M:%S')
        if start_time:
            time_part = f"{start_time}:00"
            
        timestamp = f"{date_str} {time_part}"
        
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
    
    # Fecha de referencia para el filtro (si no viene, usamos la del sistema)
    if base_date_str:
        ref_date = datetime.strptime(base_date_str, "%Y-%m-%d")
    else:
        ref_date = datetime.now()

    # Si no se especifica semestre, detectamos el de la fecha de referencia
    if semester is None or semester == "AUTO":
        detected = get_semester_for_date(ref_date, calendar)
        if detected:
            semester = detected
        else:
            # Si no estamos en un semestre (ej: verano), buscamos si estamos cerca de uno
            # o simplemente usamos q1 por defecto para cargar el horario base.
            semester = "q1"

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
        
        # REGLA 1: Obtener estado del día
        holiday = is_holiday(current_date, calendar)
        has_exception = date_str in exceptions
        is_lectivo = is_academic_day(config_dir, date_str)
        
        # Obtenemos el horario (base o excepción)
        day_schedule, _ = get_day_schedule(config_dir, date_str, semester if filter_type != "YEAR" else get_semester_for_date(current_date, calendar))
        
        # DECISIÓN CRÍTICA: ¿Qué mostramos?
        # Si hay excepción manual, manda la excepción (el usuario manda sobre el calendario).
        # Si es festivo y NO hay excepción, bloqueamos el horario base.
        should_show_classes = (day_schedule and not holiday) or has_exception
        
        if should_show_classes:
            for item in day_schedule:
                # ... (resto de la lógica de procesamiento de items sigue igual)
                subject = item.get("subject")
                start_time = item.get("start")
                end_time = item.get("end")
                
                # DETERMINAR SI ES EXCEPCIÓN REAL
                is_manual_exception = False
                if has_exception:
                    day_name_en = current_date.strftime("%A").lower()
                    days_map = {"monday": "lunes", "tuesday": "martes", "wednesday": "miercoles", "thursday": "jueves", "friday": "viernes", "saturday": "sabado", "sunday": "domingo"}
                    day_key = days_map.get(day_name_en, day_name_en)
                    base_schedule = get_schedule(config_dir, semester if filter_type != "YEAR" else get_semester_for_date(current_date, calendar))
                    day_base = base_schedule.get(day_key, [])
                    
                    is_manual_exception = True
                    for base_item in day_base:
                        if (base_item.get("subject") == subject and 
                            base_item.get("start") == start_time and 
                            base_item.get("end") == end_time and 
                            base_item.get("room") == item.get("room") and 
                            base_item.get("type", "THEORY") == item.get("type", "THEORY")):
                            is_manual_exception = False
                            break

                # Verificación de asistencia precisa
                attended = False
                for log in class_logs:
                    try:
                        log_time = datetime.strptime(log["timestamp"], "%Y-%m-%d %H:%M:%S")
                        if log_time.strftime("%Y-%m-%d") == date_str:
                            log_hour = log_time.strftime("%H:%M")
                            if log.get("details") == subject:
                                margin_start = (datetime.strptime(start_time, "%H:%M") - timedelta(minutes=15)).strftime("%H:%M")
                                if margin_start <= log_hour <= end_time:
                                    attended = True
                                    break
                    except: continue

                class_start_dt = datetime.strptime(f"{date_str} {start_time}", "%Y-%m-%d %H:%M")
                class_end_dt = datetime.strptime(f"{date_str} {end_time}", "%Y-%m-%d %H:%M")
                
                status = "MISSED"
                if attended: status = "ATTENDED"
                elif class_start_dt > datetime.now(): status = "UPCOMING"
                elif class_start_dt <= datetime.now() <= class_end_dt and not attended: status = "UPCOMING"

                is_deleted = item.get("deleted", False)
                if is_manual_exception and is_deleted: continue

                if state_filter == "ALL" or state_filter == status:
                    summary.append({
                        "date": date_str,
                        "day": current_date.strftime("%A"),
                        "subject": subject,
                        "start": start_time,
                        "end": end_time,
                        "status": status,
                        "is_exception": is_manual_exception,
                        "is_deleted": is_deleted,
                        "type": item.get("type", "THEORY"),
                        "room": item.get("room", "-"),
                        "floor": item.get("floor", "-"),
                        "building": item.get("building", "-")
                    })
                
                if not is_deleted:
                    if status == "ATTENDED": total_attended += 1
                    elif status == "MISSED": total_missed += 1
                    elif status == "UPCOMING": total_upcoming += 1
        
        # REGLA 2: Si no hay clases pero es un día especial (Festivo/Finde) en vista DAY, mostrar etiqueta
        elif filter_type == "DAY":
            is_weekend = current_date.weekday() >= 5
            holiday = is_holiday(current_date, calendar)
            
            if holiday:
                label = holiday["label"] if isinstance(holiday, dict) else "Festivo / Vacaciones"
                summary.append({"date": date_str, "status": "HOLIDAY", "subject": label})
            elif is_weekend:
                summary.append({"date": date_str, "status": "WEEKEND", "subject": translate("status_weekend", "logs")})
            elif is_academic_day(config_dir, date_str):
                summary.append({"date": date_str, "status": "FREE", "subject": "Sin clases programadas"})

        current_date += timedelta(days=1)

    # Ordenar por fecha (DESC) y luego por hora de inicio (ASC) para cada día
    summary.sort(key=lambda x: (x.get("date", ""), x.get("start", ""))) # Primero ASC global
    
    # Agrupar por fecha y asegurar que dentro de la fecha sea ASC, pero las fechas sean DESC
    # Al ser sort estable, si ordenamos por start ASC y luego por date DESC funciona
    summary.sort(key=lambda x: x.get("start", "00:00"))
    summary.sort(key=lambda x: x.get("date", ""), reverse=True)

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
