#Implemento la acción de...

from mobile.actions.actions import Action
import logging

logger = logging.getLogger(__name__)

class OpenUrl(Action): 

  def __init__(self, url):
    self.url = url

  def execute(self):
    logger.info(
      f"Abriendo URL: {self.url}"
    )