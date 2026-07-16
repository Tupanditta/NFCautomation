#Implemento la acción de...

from mobile.actions.actions import Action
import logging

logger = logging.getLogger(__name__)

class OpenApp(Action): 

  def __init__(self, package_name):
    self.package_name = package_name

  def execute(self):
    logger.info(
      f"Abriendo aplicación: {self.package_name}"
    )