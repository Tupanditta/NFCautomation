#Contiene la clase Vibrate

from mobile.actions.actions import Action
import logging

logger = logging.getLogger(__name__)

class Vibrate(Action):

  def __init__(self, duration):
    self.duration = duration

  def execute(self):
    logger.info(
      f"Vibrando durante {self.duration} ms"
    )