#Contiene la clase LogEvent

from mobile.actions.actions import Action
import logging

logger = logging.getLogger(__name__)

class LogEvent(Action):

  def __init__(self, event_name):
    self.event_name = event_name

  def execute(self):
    logger.info(
      f"Evento registrado: {self.event_name}"
    )