#Contiene la clase ShowNotification

from mobile.actions.actions import Action
import logging

logger = logging.getLogger(__name__)

class ShowNotification(Action):
  
  def __init__(self, title, message):
    self.title = title
    self.message = message

  def execute(self):
    logger.info(f"{self.title} - {self.message}")