#Contiene la clase DesactivateVibration

from mobile.actions.actions import Action
import logging

logger = logging.getLogger(__name__)

class DesactivateVibration(Action): 
  def execute(self):
    logger.info(
      "Desactivando vibración continua"
    )