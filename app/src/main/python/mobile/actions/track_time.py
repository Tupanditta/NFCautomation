import json
import os
import logging
from datetime import datetime
from mobile.actions.actions import Action
from mobile.utils.android_utils import get_context

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
                schedule_path = os.path.join(config_dir, "schedule.json")
                self.detected_class = self._get_current_class(schedule_path, now)
                if self.detected_class:
                    entry["details"] = self.detected_class
                else:
                    # Si no hay clase en la ventana, no registramos el evento como CLASE asistida
                    # o lo registramos como un check-in genérico si se prefiere.
                    # El usuario dice "quiero que las clases asistidas se registren si..."
                    # Así que si no cumple, no se registra como asistida.
                    return 

            # Guardar el log
            self._save_log(logs_path, entry)
            
            # Notificar en el log del Dispatcher
            log_msg = f"Registro {self.event_type} en {self.mode} completado"
            if self.detected_class:
                log_msg += f": {self.detected_class}"
            
            logger.info(log_msg)

        except Exception as e:
            logger.error(f"Error en TrackTime: {str(e)}")

    def _get_current_class(self, schedule_path, now):
        """Busca la clase correspondiente en el archivo de horario con ventana de +/- 10 min."""
        if not os.path.exists(schedule_path):
            return None
        
        try:
            with open(schedule_path, "r", encoding="utf-8") as f:
                schedule = json.load(f)
            
            day_name = now.strftime("%A").lower()
            days_map = {
                "monday": "lunes", "tuesday": "martes", "wednesday": "miercoles",
                "thursday": "jueves", "friday": "viernes", "saturday": "sabado", "sunday": "domingo"
            }
            day_key = days_map.get(day_name, day_name)
            day_schedule = schedule.get(day_key, [])
            
            for item in day_schedule:
                start_str = item.get("start")
                start_dt = datetime.strptime(f"{now.strftime('%Y-%m-%d')} {start_str}", "%Y-%m-%d %H:%M")
                
                # Ventana de +/- 10 minutos
                diff = (now - start_dt).total_seconds() / 60.0
                if -10 <= diff <= 10:
                    return item.get("subject")
            
            return None
        except Exception as e:
            logger.error(f"Error leyendo horario: {str(e)}")
            return None

    def _save_log(self, logs_path, entry):
        """Persiste el registro en el archivo JSON de logs."""
        logs = []
        if os.path.exists(logs_path):
            try:
                with open(logs_path, "r", encoding="utf-8") as f:
                    logs = json.load(f)
            except:
                logs = []
        
        logs.append(entry)
        
        with open(logs_path, "w", encoding="utf-8") as f:
            json.dump(logs, f, indent=4, ensure_ascii=False)

    def get_summary(self):
        return f"Registro {self.mode}"
