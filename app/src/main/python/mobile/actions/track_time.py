import json
import os
import logging
from datetime import datetime, timedelta
from mobile.actions.actions import Action
from mobile.utils.android_utils import get_context
from mobile.utils.translator import translate
from mobile.attendance import get_day_schedule

logger = logging.getLogger(__name__)

class TrackTime(Action):
    """
    Registra eventos de tiempo (Check-in/Check-out) para Trabajo y Clase.
    En el caso de Clase, detecta automáticamente la asignatura según el horario y una ventana de +/- 10 min.
    """
    def __init__(self, event_type, mode):
        self.event_type = event_type  # "CHECKIN" o "CHECKOUT"
        self.mode = mode.upper()      # "WORK" o "CLASS"
        self.detected_class = None
        self.detected_start_time = None
        self.skip_registration = False

    def execute(self):
        try:
            # Obtener ruta de configuración
            context = get_context()
            config_dir = os.path.join(context.getFilesDir().getAbsolutePath(), "config")
            logs_path = os.path.join(config_dir, "time_logs.json")
            
            now = datetime.now()
            timestamp = now.strftime("%Y-%m-%d %H:%M:%S")
            
            entry = {
                "timestamp": timestamp,
                "mode": self.mode,
                "event": self.event_type
            }

            # Lógica específica para CLASE
            if self.mode == "CLASS" and self.event_type == "CHECKIN":
                logger.info("Iniciando detección de clase...")
                self.detected_class = self._detect_current_class(config_dir, now)
                if self.detected_class is not None:
                    entry["details"] = self.detected_class
                    logger.info(translate("attendance_registered", "logs", self.detected_class))
                else:
                    # Informamos y marcamos para saltar el guardado del log.
                    logger.info(translate("outside_hours", "logs"))
                    self.skip_registration = True
                    return 

            # Guardar el log si no se ha marcado para saltar
            self._save_log(logs_path, entry)
            
            # Notificar en el log si no es una detección automática (para WORK u otros)
            if self.mode != "CLASS":
                logger.info(f"Registro {self.event_type} en {self.mode} completado")

        except Exception as e:
            logger.error(f"Error en TrackTime: {str(e)}")

    def _detect_current_class(self, config_dir, now):
        """Detecta la clase usando la lógica central de asistencia con una ventana flexible."""
        try:
            date_str = now.strftime("%Y-%m-%d")
            logger.info(f"Detectando clase para {date_str} a las {now.strftime('%H:%M:%S')}")
            
            # Obtenemos el horario efectivo (Base o Excepciones) para hoy
            day_schedule, is_exception = get_day_schedule(config_dir, date_str)
            logger.info(f"Horario obtenido: {len(day_schedule)} items. Es excepción: {is_exception}")
            
            candidates = []
            for item in day_schedule:
                subject = item.get("subject", "Evento sin nombre")
                # Omitir si la clase está marcada como borrada/cancelada
                if item.get("deleted", False):
                    logger.info(f"Omitiendo {subject} (marcada como borrada)")
                    continue
                    
                start_str = item.get("start")
                end_str = item.get("end")
                if not start_str or not end_str: 
                    logger.info(f"Omitiendo {subject} (sin horas de inicio/fin)")
                    continue
                
                start_dt = datetime.strptime(f"{date_str} {start_str}", "%Y-%m-%d %H:%M")
                end_dt = datetime.strptime(f"{date_str} {end_str}", "%Y-%m-%d %H:%M")
                
                # VENTANA FLEXIBLE: 
                # - Desde 15 minutos antes del inicio.
                # - Hasta el final de la clase (para permitir registros tardíos).
                window_start = start_dt - timedelta(minutes=15)
                if window_start <= now <= end_dt:
                    diff = abs((now - start_dt).total_seconds())
                    candidates.append((diff, item))
                    logger.info(f"Candidato detectado: {subject} ({start_str}-{end_str}) - Dif: {diff}s")
                else:
                    logger.info(f"Fuera de ventana: {subject} ({start_str}-{end_str})")
            
            if not candidates:
                logger.info("No se encontraron candidatos en la ventana horaria")
                return None
            
            # Ordenar por cercanía al inicio programado
            candidates.sort(key=lambda x: x[0])
            best_match = candidates[0][1]
            
            self.detected_start_time = best_match.get("start")
            detected_subject = best_match.get("subject")
            
            # Si el subject es None o vacío, usamos un valor por defecto para que el check is not None pase
            if detected_subject is None:
                detected_subject = ""
                
            logger.info(f"Mejor coincidencia: '{detected_subject}' empezando a las {self.detected_start_time}")
            return detected_subject
        except Exception as e:
            logger.error(f"Error detectando clase: {str(e)}")
            return None

    def _save_log(self, logs_path, entry):
        """Persiste el registro en el archivo JSON de logs, evitando duplicados en la misma ventana de tiempo."""
        logs = []
        if os.path.exists(logs_path):
            try:
                with open(logs_path, "r", encoding="utf-8") as f:
                    logs = json.load(f)
            except:
                logs = []
        
        # Evitar duplicados (mismo modo, mismo event, mismo subject en los últimos 2 minutos)
        now_dt = datetime.strptime(entry["timestamp"], "%Y-%m-%d %H:%M:%S")
        for existing in reversed(logs[-10:]): # Revisar los últimos 10 logs
            try:
                ex_dt = datetime.strptime(existing["timestamp"], "%Y-%m-%d %H:%M:%S")
                if (existing.get("mode") == entry["mode"] and 
                    existing.get("event") == entry["event"] and 
                    existing.get("details") == entry.get("details") and
                    abs((now_dt - ex_dt).total_seconds()) < 120): # Menos de 2 minutos
                    logger.info("Registro duplicado ignorado")
                    return
            except:
                continue

        logs.append(entry)
        
        with open(logs_path, "w", encoding="utf-8") as f:
            json.dump(logs, f, indent=4, ensure_ascii=False)

    def get_summary(self):
        if self.skip_registration:
            return translate("outside_hours", "logs")
        if self.detected_class:
            return translate("subject_summary", "logs", self.detected_class)
        return f"Registro {self.mode}"
