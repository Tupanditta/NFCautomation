# Este módulo se encarga de coordinar la ejecución de un workflow a partir de un UID NFC.

import logging
import os
from datetime import datetime
from mobile.exceptions import (
    UnknownTagError, 
    WorkflowNotFoundError,
    MobileBaseError
)
from mobile.storage.state_storage import load_states, save_states
from mobile.utils.android_utils import get_context
from mobile.utils.translator import translate
from java import jclass

logger = logging.getLogger(__name__)

class Dispatcher:
    """
    Coordina la resolución y ejecución de workflows con lógica de exclusividad.
    """

    def __init__(self, tag_registry, workflow_registry, action_registry, config_dir=None):
        self.tag_registry = tag_registry
        self.workflow_registry = workflow_registry
        self.action_registry = action_registry
        
        # Ruta absoluta de states.json
        if config_dir is None:
            base_dir = os.path.dirname(__file__)
            self.states_path = os.path.join(base_dir, "config", "states.json")
        else:
            self.states_path = os.path.join(config_dir, "states.json")
        
        # Sincronización inmediata para "despertar" el puente Java-Python.
        # Esto estabiliza el entorno desde la creación del motor.
        try:
            self.sync_android_state()
        except Exception:
            pass

    def execute_uid(self, uid):
        """
        Ejecuta el workflow asociado a un UID.
        Retorna (errors, summaries, attendance_info)
        """
        logger.info(translate("start_exec", "logs", uid))
        
        target = self.tag_registry.get_workflow(uid)
        if target is None:
            raise UnknownTagError(translate("msg_unknown_tag", "errors", uid))

        # Lógica de Toggle (Modos Exclusivos)
        if target.startswith("toggle:"):
            mode_name = target.split(":")[1]
            return self._handle_toggle(mode_name)
        
        # Ejecución de Workflow Simple (Acción Libre)
        return self._execute_workflow(target)

    def _handle_toggle(self, mode_name):
        """Gestiona la alternancia de estados con regla de exclusividad."""
        states = load_states(self.states_path)
        current_state = states.get(mode_name, "OFF")
        
        # Obtenemos info del workflow para ver si es de sistema
        wf_on = self.workflow_registry.get_workflow(f"{mode_name}_ON")
        is_system = wf_on.get("system", False) if wf_on else False

        # Regla de Exclusividad: Solo bloqueamos si intentamos encender (OFF -> ON)
        # Y SOLO para modos que NO son de sistema.
        if current_state == "OFF" and not is_system:
            # Verificar si hay algún OTRO modo activo (que no sea de sistema)
            for other_mode, state in states.items():
                if other_mode != mode_name and state == "ON":
                    other_wf = self.workflow_registry.get_workflow(f"{other_mode}_ON")
                    other_is_system = other_wf.get("system", False) if other_wf else False
                    
                    if not other_is_system:
                        msg = translate("lock_msg", "logs", other_mode)
                        return [MobileBaseError(msg)], [], None
            
            new_state = "ON"
            logger.info(translate("mode_on", "logs", mode_name))
        else:
            new_state = "OFF" if current_state == "ON" else "ON"
            mode_desc = translate("mode_system", "logs") if is_system else ""
            logger.info(translate("mode_change", "logs", mode_desc, mode_name, new_state))

        workflow_to_run = f"{mode_name}_{new_state}"
        
        # Ejecutamos el workflow
        errors, summaries, attendance_info = self._execute_workflow(workflow_to_run)
        
        # CAMBIO CRÍTICO: Siempre guardamos el nuevo estado si la ejecución se completó,
        # aunque haya habido errores en acciones individuales (ej: wifi hotspot).
        states[mode_name] = new_state
        save_states(self.states_path, states)
        self.sync_android_state()
            
        return errors, summaries, attendance_info

    def force_off_active_mode(self):
        """Busca qué modo está en ON y lo apaga. Llamado desde Android."""
        states = load_states(self.states_path)
        active_mode = None
        for mode, state in states.items():
            if state == "ON":
                active_mode = mode
                break
        
        if active_mode:
            logger.info(translate("force_off", "logs", active_mode))
            errors, summaries, attendance_info = self._execute_workflow(f"{active_mode}_OFF")
            
            # Siempre guardamos el estado OFF tras forzar
            states[active_mode] = "OFF"
            save_states(self.states_path, states)
            self.sync_android_state()
            return errors, summaries, attendance_info
        return [], [], None

    def sync_android_state(self):
        """Sincroniza la notificación de Android con los estados persistentes."""
        states = load_states(self.states_path)
        
        active_mode = None
        for mode, state in states.items():
            if state == "ON":
                # Verificamos si es un modo de usuario (no system)
                wf_on = self.workflow_registry.get_workflow(f"{mode}_ON")
                if wf_on and wf_on.get("system", False):
                    continue
                active_mode = mode
                break
        
        try:
            context = get_context()
            QuickAccessService = jclass("com.example.nfcautomation.services.QuickAccessService")
            QuickAccessService.updateStatusNotification(context, active_mode)
        except Exception as e:
            # Aquí sí logueamos el error de sistema
            logger.error(translate("sync_err", "logs", str(e)))

    def _execute_workflow(self, workflow_id):
        """Lógica interna para ejecutar una lista de acciones. Retorna (errors, summaries, attendance_info)"""
        logger.info(translate("start_wf", "logs", workflow_id))
        
        workflow = self.workflow_registry.get_workflow(workflow_id)
        if workflow is None:
            raise WorkflowNotFoundError(translate("msg_wf_not_found", "errors", workflow_id))

        laptop_task = workflow.get("laptop_task")
        if laptop_task:
            logger.info(translate("laptop_task", "logs", laptop_task) + "\n")

        action_definitions = workflow.get("mobile_actions", [])
        errors = []
        summaries = []
        attendance_info = None

        for action_def in action_definitions:
            action_name = action_def.get("action")
            params = action_def.get("params", {})
            
            logger.info(f"→ Ejecutando: {action_name}")
            
            try:
                action_class = self.action_registry.get_action(action_name)
                if action_class is None:
                    errors.append(MobileBaseError(translate("unknown_action", "logs", action_name)))
                    continue

                action = action_class(**params)
                action.execute()
                
                # Recopilar resumen para la UI
                summary = action.get_summary()
                if summary:
                    summaries.append(summary)
                
                # Extraer info de asistencia si la acción era TrackTime
                from mobile.actions.track_time import TrackTime
                if isinstance(action, TrackTime) and action.detected_class:
                    attendance_info = {
                        "subject": action.detected_class,
                        "date": datetime.now().strftime("%Y-%m-%d"),
                        "startTime": action.detected_start_time
                    }

            except MobileBaseError as e:
                errors.append(e)
            except Exception as e:
                errors.append(e)

        return errors, summaries, attendance_info
