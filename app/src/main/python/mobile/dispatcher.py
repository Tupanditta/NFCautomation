# Este módulo se encarga de coordinar la ejecución de un workflow a partir de un UID NFC.

import logging
from mobile.exceptions import (
    UnknownTagError, 
    WorkflowNotFoundError,
    MobileBaseError
)

logger = logging.getLogger(__name__)

class Dispatcher:
    """
    Coordina la resolución y ejecución de workflows de forma resiliente.
    """

    def __init__(self, tag_registry, workflow_registry, action_registry):
        self.tag_registry = tag_registry
        self.workflow_registry = workflow_registry
        self.action_registry = action_registry

    def execute_uid(self, uid):
        """
        Ejecuta el workflow asociado a un UID.
        Si una acción falla, registra el error y continúa con las demás.
        """
        logger.info(f"Iniciando ejecución para tag: {uid}")
        
        workflow_id = self.tag_registry.get_workflow(uid)
        if workflow_id is None:
            raise UnknownTagError(f"UID no registrado: {uid}")

        logger.info(f"Workflow detectado: {workflow_id}")

        workflow = self.workflow_registry.get_workflow(workflow_id)
        if workflow is None:
            raise WorkflowNotFoundError(f"Workflow no encontrado: {workflow_id}")

        laptop_task = workflow.get("laptop_task")
        if laptop_task:
            logger.info(f"Laptop task pendiente: {laptop_task}\n")

        action_definitions = workflow.get("mobile_actions", [])
        errors = []

        for action_def in action_definitions:
            action_name = action_def.get("action")
            params = action_def.get("params", {})
            
            try:
                action_class = self.action_registry.get_action(action_name)

                if action_class is None:
                    errors.append(f"Acción no registrada: {action_name}")
                    continue

                action = action_class(**params)
                action.execute()
                
            except MobileBaseError as e:
                # Capturamos errores específicos del motor y los registramos
                logger.warning(f"Fallo controlado en acción '{action_name}': {e}")
                errors.append(e)
            except Exception as e:
                # Capturamos fallos inesperados para que no detengan el motor
                logger.error(f"Error inesperado en acción '{action_name}': {e}")
                errors.append(e)

        return errors
