import os
import logging
import io
from mobile.storage.json_storage import load_tags, load_workflows
from mobile.registries.tag_registry import TagRegistry
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

def is_tag_registered(tag_id: str) -> bool:
    """
    Función ligera para verificar si un tag está en el registro.
    Se utiliza como Gatekeeper para evitar abrir la app con tags desconocidos.
    """
    try:
        base_dir = os.path.dirname(__file__)
        tags_path = os.path.join(base_dir, "mobile", "config", "tags.json")
        tags_dict = load_tags(tags_path)
        return tag_id in tags_dict
    except Exception:
        return False

def execute(tag_id: str):
    # Captura de logs para la UI de Android
    log_capture_string = io.StringIO()
    ch = logging.StreamHandler(log_capture_string)
    ch.setLevel(logging.INFO)
    formatter = logging.Formatter('• %(message)s')
    ch.setFormatter(formatter)
    
    logger = logging.getLogger("mobile")
    logger.setLevel(logging.INFO)
    logger.addHandler(ch)

    try:
        base_dir = os.path.dirname(__file__)
        tags_path = os.path.join(base_dir, "mobile", "config", "tags.json")
        workflows_path = os.path.join(base_dir, "mobile", "config", "workflows.json")

        tags_dict = load_tags(tags_path)
        workflows_dict = load_workflows(workflows_path)

        tag_validator.validate_tags_dict(tags_dict)
        workflow_validator.validate_workflows_dict(workflows_dict)

        tag_registry = TagRegistry(tags_dict)
        workflow_registry = WorkFlowRegistry(workflows_dict)
        action_registry = ActionRegistry()

        workflow_name = tag_registry.get_workflow(tag_id)

        dispatcher = Dispatcher(
            tag_registry,
            workflow_registry,
            action_registry
        )

        # Ejecución resiliente
        errors = dispatcher.execute_uid(tag_id)
        analize_errors(errors)

        captured_logs = log_capture_string.getvalue()
        
        if workflow_name:
            response = f"¡Workflow '{workflow_name}' ejecutado con éxito!\n"
        else:
            response = "¡Procesamiento del tag finalizado con éxito!\n"
        
        if captured_logs:
            response += f"\nDetalles de ejecución:\n{captured_logs}"

        if errors:
            err_msgs = [f"[{type(e).__name__}] {str(e)}" for e in errors]
            response += f"\n\nWarning: Errores detectados:\n• " + "\n• ".join(err_msgs)
        
        return response

    except MobileBaseError as e:
        # Errores controlados del motor
        return f"Error en el motor: [{type(e).__name__}] {str(e)}"
    except Exception as e:
        # Fallos críticos inesperados
        return f"Error inesperado: {str(e)}"
    finally:
        logger.removeHandler(ch)
        log_capture_string.close()
