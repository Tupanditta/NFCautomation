import os
import logging
import io
import json
from datetime import datetime
from mobile.storage.json_storage import load_tags, load_workflows, save_tags, save_workflows
from mobile.attendance import (
    get_attendance_summary as _get_summary, 
    get_schedule as _get_sched, 
    save_schedule as _save_sched,
    save_exception as _save_exception,
    toggle_manual_attendance as _toggle_attendance,
    get_day_schedule as _get_day_sched,
    is_academic_day as _is_acad,
    get_semester_subjects as _get_subjects
)
from mobile.exporter import generate_attendance_report as _gen_report
from mobile.pdf_exporter import generate_attendance_pdf as _gen_pdf_report
from mobile.utils.android_utils import get_context
from mobile.registries.tag_registry import TagRegistry
from mobile.utils.translator import load_translations, set_current_language, translate
from mobile.registries.workflow_registry import WorkFlowRegistry
from mobile.registries.action_registry import ActionRegistry
from mobile.dispatcher import Dispatcher
from mobile.validators import tag_validator, workflow_validator
from mobile.errors import analize_errors
from mobile.exceptions import (
    MobileBaseError,
    ConfigurationError,
    UnknownTagError,
    WorkflowNotFoundError
)

# Variable global para mantener el dispatcher vivo y evitar recargas constantes
_dispatcher_instance = None
_BASE_CONFIG_PATH = None

def setup_base_path(path: str):
    """Establece la ruta base para los archivos de configuración."""
    global _BASE_CONFIG_PATH, _dispatcher_instance
    _BASE_CONFIG_PATH = path
    # Cargamos traducciones inmediatamente
    load_translations(path)
    # Forzamos la recreación del dispatcher con la nueva ruta
    _dispatcher_instance = None
    get_dispatcher()

def set_language(lang: str):
    """Cambia el idioma de las traducciones de Python."""
    set_current_language(lang)
    return True

def translate_workflow(wf_id: str):
    """Traduce el nombre de un workflow para la UI."""
    return translate(wf_id, "workflows")

def get_dispatcher():
    global _dispatcher_instance
    try:
        if _dispatcher_instance is None:
            if _BASE_CONFIG_PATH is None:
                # Fallback a la lógica anterior si no se ha configurado (para desarrollo/test)
                current_dir = os.path.dirname(os.path.abspath(__file__))
                config_dir = os.path.join(current_dir, "mobile", "config")
            else:
                config_dir = _BASE_CONFIG_PATH

            logging.info(f"Cargando configuración desde: {config_dir}")
            tags_path = os.path.join(config_dir, "tags.json")
            workflows_path = os.path.join(config_dir, "workflows.json")

            if not os.path.exists(tags_path):
                logging.error(f"No existe tags.json en {tags_path}")
            if not os.path.exists(workflows_path):
                logging.error(f"No existe workflows.json en {workflows_path}")

            tags_dict = load_tags(tags_path)
            workflows_dict = load_workflows(workflows_path)

            _dispatcher_instance = Dispatcher(
                TagRegistry(tags_dict),
                WorkFlowRegistry(workflows_dict),
                ActionRegistry(),
                config_dir # Pasamos la ruta al dispatcher
            )
        return _dispatcher_instance
    except Exception as e:
        logging.error(f"Error crítico en get_dispatcher: {str(e)}")
        import traceback
        logging.error(traceback.format_exc())
        return None

def is_tag_registered(tag_id: str):
    """Verifica si un tag está en el registro."""
    try:
        dispatcher = get_dispatcher()
        return tag_id in dispatcher.tag_registry.tags
    except Exception:
        return False

def initialize_system():
    """Fuerza la inicialización del sistema mediante un modo toggle oculto."""
    try:
        # Esto estabiliza el I/O y la conexión con Java al arrancar
        # Usamos el nombre base para que pase por _handle_toggle (mismo flujo que un tag)
        execute_manual("app_init")
        return True
    except Exception:
        return False

def get_available_workflows():
    """
    Devuelve la lista de nombres base de workflows para la UI de forma robusta.
    Filtra los workflows marcados como 'system' y claves corruptas.
    """
    try:
        dispatcher = get_dispatcher()
        workflows_dict = dispatcher.workflow_registry.workflows
        
        unique_bases = []
        for wf_id, wf_data in workflows_dict.items():
            # Ignoramos si es un workflow de sistema o basura
            if wf_data.get("system", False) or str(wf_id).startswith("toggle:"):
                continue
                
            base = str(wf_id).replace("_ON", "").replace("_OFF", "")
            if base not in unique_bases:
                unique_bases.append(base)
            
        unique_bases.sort()
        return unique_bases
    except Exception:
        return []

def get_active_mode():
    """
    Devuelve el nombre del modo actualmente en ON.
    Ignora los modos de sistema para que la UI no los muestre.
    """
    try:
        dispatcher = get_dispatcher()
        states_path = dispatcher.states_path
        if not os.path.exists(states_path):
            return None
        with open(states_path, "r", encoding="utf-8") as f:
            states = json.load(f)
            
        dispatcher = get_dispatcher()
        for mode, state in states.items():
            if state == "ON":
                # Verificamos si es de sistema
                wf_on = dispatcher.workflow_registry.get_workflow(f"{mode}_ON")
                if wf_on and wf_on.get("system", False):
                    continue
                return mode
        return None
    except Exception:
        return None

def sync_ui_state():
    """Sincroniza la UI de Android con el estado actual."""
    dispatcher = get_dispatcher()
    dispatcher.sync_android_state()

def force_work_mode_off():
    """
    Apaga el modo activo desde la interfaz.
    Retorna el resultado en JSON para actualizar la UI.
    """
    log_capture_string = io.StringIO()
    class InfoFilter(logging.Filter):
        def filter(self, record): return record.levelno == logging.INFO

    ch = logging.StreamHandler(log_capture_string)
    ch.setLevel(logging.INFO)
    ch.addFilter(InfoFilter())
    formatter = logging.Formatter('• %(message)s')
    ch.setFormatter(formatter)
    
    logger = logging.getLogger("mobile")
    logger.setLevel(logging.INFO)
    logger.addHandler(ch)

    try:
        dispatcher = get_dispatcher()
        # Identificamos qué modo estamos apagando para que la UI pueda reactivarlo si quiere
        active_mode_before = get_active_mode()
        
        errors, summaries, attendance_info = dispatcher.force_off_active_mode()
        captured_logs = log_capture_string.getvalue()
        
        # Si había un modo, devolvemos su nombre técnico _OFF
        target_name = f"{active_mode_before}_OFF" if active_mode_before else "FORZAR APAGADO"
        
        is_toggle = True
        if errors:
            is_toggle = False
            
        result_map = {
            "target": target_name,
            "details": captured_logs,
            "errors": [str(e) for e in errors],
            "summarized_actions": summaries,
            "attendance_info": attendance_info,
            "active_mode": None,
            "is_toggle": is_toggle,
            "toggle_type": "OFF"
        }
        return json.dumps(result_map)
    except Exception as e:
        return json.dumps({"critical_error": str(e)})
    finally:
        logger.removeHandler(ch)
        log_capture_string.close()

def _get_workflow_type_info(workflow_id: str):
    is_toggle = workflow_id.endswith("_ON") or workflow_id.endswith("_OFF")
    toggle_type = None
    if workflow_id.endswith("_ON"): toggle_type = "ON"
    elif workflow_id.endswith("_OFF"): toggle_type = "OFF"
    return is_toggle, toggle_type

def execute(tag_id: str):
    """Ejecuta el workflow para un tag ID."""
    log_capture_string = io.StringIO()
    class InfoFilter(logging.Filter):
        def filter(self, record): return record.levelno == logging.INFO

    ch = logging.StreamHandler(log_capture_string)
    ch.setLevel(logging.INFO)
    ch.addFilter(InfoFilter())
    formatter = logging.Formatter('• %(message)s')
    ch.setFormatter(formatter)
    
    logger = logging.getLogger("mobile")
    logger.setLevel(logging.INFO)
    logger.addHandler(ch)

    try:
        dispatcher = get_dispatcher()
        # Obtenemos el raw target desde el registro del dispatcher
        target_raw = dispatcher.tag_registry.get_workflow(tag_id) or tag_id

        errors, summaries, attendance_info = dispatcher.execute_uid(tag_id)
        analize_errors(errors)

        captured_logs = log_capture_string.getvalue()
        active_mode = get_active_mode()
        
        executed_workflow = target_raw.replace("toggle:", "")
        if target_raw.startswith("toggle:"):
            mode_base = target_raw.split(":")[1]
            is_on = active_mode == mode_base
            executed_workflow = f"{mode_base}_{'ON' if is_on else 'OFF'}"

        is_toggle, toggle_type = _get_workflow_type_info(executed_workflow)
        
        # Si hay errores (ej: bloqueo por exclusividad), no permitimos botones de toggle en la UI
        if errors:
            is_toggle = False

        result_map = {
            "target": executed_workflow,
            "details": captured_logs,
            "errors": [str(e) for e in errors],
            "summarized_actions": summaries,
            "attendance_info": attendance_info,
            "active_mode": active_mode,
            "is_toggle": is_toggle,
            "toggle_type": toggle_type
        }
        return json.dumps(result_map)
    except Exception as e:
        return json.dumps({"critical_error": str(e)})
    finally:
        logger.removeHandler(ch)
        log_capture_string.close()

def execute_manual(workflow_id: str):
    """Ejecución manual desde el menú."""
    log_capture_string = io.StringIO()
    class InfoFilter(logging.Filter):
        def filter(self, record): return record.levelno == logging.INFO

    ch = logging.StreamHandler(log_capture_string)
    ch.setLevel(logging.INFO)
    ch.addFilter(InfoFilter())
    formatter = logging.Formatter('• %(message)s')
    ch.setFormatter(formatter)
    
    logger = logging.getLogger("mobile")
    logger.setLevel(logging.INFO)
    logger.addHandler(ch)

    try:
        dispatcher = get_dispatcher()
        all_workflows = dispatcher.workflow_registry.workflows

        executed_wf = workflow_id
        errors = []
        summaries = []
        attendance_info = None

        if workflow_id in all_workflows:
            errors, summaries, attendance_info = dispatcher._execute_workflow(workflow_id)
            if workflow_id.endswith("_ON") or workflow_id.endswith("_OFF"):
                mode_name = workflow_id.replace("_ON", "").replace("_OFF", "")
                new_state = "ON" if workflow_id.endswith("_ON") else "OFF"
                from mobile.storage.state_storage import load_states, save_states
                states_path = dispatcher.states_path
                states = load_states(states_path)
                states[mode_name] = new_state
                save_states(states_path, states)
                dispatcher.sync_android_state()
        else:
            errors, summaries, attendance_info = dispatcher._handle_toggle(workflow_id)
            active_mode = get_active_mode()
            is_on = active_mode == workflow_id
            executed_wf = f"{workflow_id}_{'ON' if is_on else 'OFF'}"

        analize_errors(errors)
        captured_logs = log_capture_string.getvalue()
        is_toggle, toggle_type = _get_workflow_type_info(executed_wf)
        
        # Si hay errores (ej: bloqueo por exclusividad), no permitimos botones de toggle en la UI
        if errors:
            is_toggle = False

        result_map = {
            "target": executed_wf,
            "details": captured_logs,
            "errors": [str(e) for e in errors],
            "summarized_actions": summaries,
            "attendance_info": attendance_info,
            "active_mode": get_active_mode(),
            "is_toggle": is_toggle,
            "toggle_type": toggle_type
        }
        return json.dumps(result_map)
    except Exception as e:
        return json.dumps({"critical_error": str(e)})
    finally:
        logger.removeHandler(ch)
        log_capture_string.close()

# --- Funciones de Gestión para la UI ---

def get_app_settings():
    """Devuelve los ajustes generales de la aplicación."""
    try:
        config_dir = _BASE_CONFIG_PATH or os.path.join(os.path.dirname(__file__), "mobile", "config")
        settings_path = os.path.join(config_dir, "settings.json")
        if os.path.exists(settings_path):
            with open(settings_path, "r", encoding="utf-8") as f:
                return json.dumps(json.load(f))
        return json.dumps({"notification_enabled": True, "dark_mode": True})
    except Exception:
        return json.dumps({})

def get_management_data():
    """Devuelve toda la configuración necesaria para el editor visual."""
    try:
        dispatcher = get_dispatcher()
        # Ruta base
        config_dir = _BASE_CONFIG_PATH or os.path.join(os.path.dirname(__file__), "mobile", "config")
        
        # Plantillas de acciones
        template_path = os.path.join(config_dir, "actions_template.json")
        with open(template_path, "r", encoding="utf-8") as f:
            templates = json.load(f)

        # Ajustes de la app
        settings_path = os.path.join(config_dir, "settings.json")
        settings = {}
        if os.path.exists(settings_path):
            with open(settings_path, "r", encoding="utf-8") as f:
                settings = json.load(f)

        data = {
            "tags": dispatcher.tag_registry.tags,
            "workflows": dispatcher.workflow_registry.workflows,
            "templates": templates.get("plantillas", []),
            "settings": settings,
            "action_translations": _get_action_translations()
        }
        return json.dumps(data)
    except Exception as e:
        return json.dumps({"error": str(e)})

def _get_action_translations():
    """Devuelve un mapeo de nombres técnicos a nombres amigables en el idioma actual."""
    from mobile.utils.translator import _translations, _current_lang
    return _translations.get(_current_lang, {}).get("actions", {})

def save_management_data(tags_json: str, workflows_json: str, settings_json: str = "{}"):
    """Guarda los cambios realizados en el editor visual y ajustes."""
    try:
        new_tags = json.loads(tags_json)
        new_workflows = json.loads(workflows_json)
        new_settings = json.loads(settings_json)
        
        config_dir = _BASE_CONFIG_PATH or os.path.join(os.path.dirname(__file__), "mobile", "config")
        
        # Guardar en disco
        save_tags(os.path.join(config_dir, "tags.json"), new_tags)
        save_workflows(os.path.join(config_dir, "workflows.json"), new_workflows)
        
        if new_settings:
            settings_path = os.path.join(config_dir, "settings.json")
            with open(settings_path, "w", encoding="utf-8") as f:
                json.dump(new_settings, f, indent=4)
        
        # Forzar recarga del dispatcher
        global _dispatcher_instance
        _dispatcher_instance = None
        get_dispatcher()
        
        return json.dumps({"success": True})
    except Exception as e:
        return json.dumps({"error": str(e)})

# --- Funciones de Asistencia y Horario ---

def get_attendance_data(filter_type: str, semester: str = "AUTO", state_filter: str = "ALL", base_date: str = None):
    """Obtiene el resumen de asistencia filtrado con detección de calendario y fecha base."""
    try:
        config_dir = _BASE_CONFIG_PATH or os.path.join(os.path.dirname(__file__), "mobile", "config")
        data = _get_summary(config_dir, filter_type, semester, state_filter, base_date)
        return json.dumps(data)
    except Exception as e:
        return json.dumps({"error": str(e)})

def get_schedule_data(semester: str = "q1"):
    """Obtiene el horario completo de un cuatrimestre."""
    try:
        config_dir = _BASE_CONFIG_PATH or os.path.join(os.path.dirname(__file__), "mobile", "config")
        return json.dumps(_get_sched(config_dir, semester))
    except Exception as e:
        return json.dumps({"error": str(e)})

def save_schedule_data(schedule_json: str, semester: str = "q1"):
    """Guarda el nuevo horario para un cuatrimestre."""
    try:
        config_dir = _BASE_CONFIG_PATH or os.path.join(os.path.dirname(__file__), "mobile", "config")
        data = json.loads(schedule_json)
        _save_sched(config_dir, data, semester)
        return json.dumps({"success": True})
    except Exception as e:
        return json.dumps({"error": str(e)})

def save_daily_exception(date_str: str, schedule_json: str):
    """Guarda una excepción horaria para un día específico."""
    try:
        config_dir = _BASE_CONFIG_PATH or os.path.join(os.path.dirname(__file__), "mobile", "config")
        data = json.loads(schedule_json) if schedule_json else None
        _save_exception(config_dir, date_str, data)
        return json.dumps({"success": True})
    except Exception as e:
        return json.dumps({"error": str(e)})

def get_day_schedule_data(date_str: str, semester: str = "AUTO"):
    """Retorna el horario (y si es excepción) de un día específico."""
    try:
        config_dir = _BASE_CONFIG_PATH or os.path.join(os.path.dirname(__file__), "mobile", "config")
        sched, is_exc = _get_day_sched(config_dir, date_str, semester)
        return json.dumps({"schedule": sched, "is_exception": is_exc})
    except Exception as e:
        return json.dumps({"error": str(e)})

def get_subjects_for_semester(semester: str = "AUTO"):
    """Retorna la lista de asignaturas del cuatrimestre."""
    try:
        config_dir = _BASE_CONFIG_PATH or os.path.join(os.path.dirname(__file__), "mobile", "config")
        # Resolvemos AUTO si es necesario
        if semester == "AUTO":
             from mobile.attendance import get_calendar_config, get_semester_for_date
             calendar = get_calendar_config(config_dir)
             semester = get_semester_for_date(datetime.now(), calendar) or "q1"
             
        subjects = _get_subjects(config_dir, semester)
        return json.dumps({"subjects": subjects})
    except Exception as e:
        return json.dumps({"error": str(e)})

def check_academic_day(date_str: str):
    """Retorna si el día es lectivo."""
    try:
        config_dir = _BASE_CONFIG_PATH or os.path.join(os.path.dirname(__file__), "mobile", "config")
        res = _is_acad(config_dir, date_str)
        return res
    except Exception:
        return False

def toggle_attendance(date_str: str, subject: str, current_status: str):
    """Cambia el estado de asistencia manualmente."""
    try:
        config_dir = _BASE_CONFIG_PATH or os.path.join(os.path.dirname(__file__), "mobile", "config")
        _toggle_attendance(config_dir, date_str, subject, current_status)
        return json.dumps({"success": True})
    except Exception as e:
        return json.dumps({"error": str(e)})

def export_attendance_excel(period_type: str, semester: str = "q1", format: str = "EXCEL"):
    """Genera y retorna la ruta del archivo exportado (Excel o PDF)."""
    try:
        config_dir = _BASE_CONFIG_PATH or os.path.join(os.path.dirname(__file__), "mobile", "config")
        context = get_context()
        cache_dir = context.getCacheDir().getAbsolutePath()
        
        if format == "PDF":
            file_path = _gen_pdf_report(config_dir, cache_dir, period_type, semester)
        else:
            file_path = _gen_report(config_dir, cache_dir, period_type, semester)
            
        return json.dumps({"file_path": file_path})
    except Exception as e:
        import traceback
        logging.error(traceback.format_exc())
        return json.dumps({"error": str(e)})
