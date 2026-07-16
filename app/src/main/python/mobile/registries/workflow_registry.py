#Este módulo tiene el único trabajo de declarar e implementar la clase WorkFlowRegistry

class WorkFlowRegistry:
  """
  Esta clase relaciona cada workflow con 
  su respectiva lista de acciones
  """
  def __init__(self, workflows_dict):
    """
    Guardo el diccionario del workflow.json
    en una variable de la clade
    """
    self.workflows = workflows_dict

  def get_workflow(self, workflow_id): #devuelvo el workflow (el diccionario principal)
    """
    Devuelve el diccionario correspondiente
    al workflow_id que se le proporciona
    este diccionario contiene dos apartados, 
    mobile_actions(con sus respectivas acciones) 
    y laptop_task
    """
    return self.workflows.get(workflow_id)