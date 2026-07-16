#Este módulo es quien dirige 

from mobile.exceptions import UnknownTagError, WorkflowNotFoundError
import logging

logger = logging.getLogger(__name__)

class Dispatcher:

  def __init__(
    self,
    tag_registry,
    workflow_registry,
    action_registry
):
    self.tag_registry = tag_registry
    self.workflow_registry = workflow_registry
    self.action_registry = action_registry

  def execute_uid(self, uid):

    workflow_id = self.tag_registry.get_workflow(uid) #consigo el workflow_id a traves de la clave del NFC
    errors = [] #lista de acciones que no existen (inicializar)

    if workflow_id is None:
      raise UnknownTagError(f"UID no registrado: {uid}")

    workflow = self.workflow_registry.get_workflow(workflow_id) #obtengo el workflow asociado al NFC

    if workflow is None:
      raise WorkflowNotFoundError(f"Workflow no encontrado: {workflow_id}")
    
    laptop_task = workflow.get("laptop_task")
    if laptop_task:
      logger.info(f"Laptop task pendiente: {laptop_task}")

    action_definitions = workflow["mobile_actions"] #solo me interesan las acciones del móbil

    for action_definition in action_definitions: #recorro todos los diccionarios de acciones

      action_name = action_definition["action"]
      params = action_definition.get("params", {}) #uso get porque hay acciones sin parámetros
      
      action_class = self.action_registry.get_action(action_name) #obtengo la clase atribuida a cada acción

      if action_class is None:
        errors.append(f"Acción no registrada: {action_name}")
        continue

      action = action_class(**params) #obtengo el objeto de cada clase de cada acción
      action.execute() #ejecuto el objeto 

    return errors #devuelvo la lista de acciones que no se han encontrado en la carpeta actions
