#Modulo con la clase SetVolume

from mobile.actions.actions import Action
import logging

logger = logging.getLogger(__name__)

class SetVolume(Action):
  def __init__(self, level):
    self.level = level

  def execute(self):
    logger.info(
      f"Volumen establecido al {self.level}%"
    )