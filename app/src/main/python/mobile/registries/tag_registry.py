#Este módulo tiene el único trabajo de declarar e implementar la clase TagRegistry

class TagRegistry:
  """
  Esta clase se encarga de relacionar el uid del 
  NFC leído con su respectivo workflow
  """

  def __init__(self, tags_dict):
    """
    Función que guarda el diccionario 
    con los tags
    """
    self.tags = tags_dict

  def get_workflow(self, uid):
    """
    Devuelve el workflow correspondiente
    al uid que se le proporciona
    """
    return self.tags.get(uid) #devuelve el workflow