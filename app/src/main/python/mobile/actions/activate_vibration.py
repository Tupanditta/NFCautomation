# Implementación de la acción de Vibración Continua con cálculo de ciclos preciso
from mobile.actions.actions import Action
from mobile.utils.android_utils import get_system_service
from android.content import Context
from android.os import VibrationEffect, VibrationAttributes
from java import jarray, jlong
import logging

logger = logging.getLogger(__name__)

class ActivateVibration(Action): 
    def __init__(self, total_duration=0, on_duration=500, off_duration=500):
        """
        :param total_duration: Duración total del aviso en milisegundos. 0 para infinito.
        :param on_duration: Cuánto tiempo vibra (ms) en cada ciclo.
        :param off_duration: Cuánto tiempo para (ms) en cada ciclo.
        """
        try:
            self.total_duration = int(total_duration)
            self.on_duration = int(on_duration)
            self.off_duration = int(off_duration)
        except (ValueError, TypeError):
            self.total_duration = 0
            self.on_duration = 500
            self.off_duration = 500

    def execute(self):
        msg = f"Iniciando vibración (Patrón: {self.on_duration}ms ON / {self.off_duration}ms OFF)"
        if self.total_duration > 0:
            msg += f" durante {self.total_duration}ms"
        else:
            msg += " (Infinita)"
        
        logger.info(msg)
        
        try:
            vibrator = get_system_service(Context.VIBRATOR_SERVICE)
            if not vibrator.hasVibrator():
                logger.warning("El dispositivo no tiene hardware de vibración.")
                return

            attrs = VibrationAttributes.Builder() \
                .setUsage(VibrationAttributes.USAGE_ALARM) \
                .build()

            if self.total_duration > 0:
                # Nuevo algoritmo de consumo de tiempo para máxima precisión
                timings_list = [0] # Delay inicial (Java espera esto como primer elemento)
                remaining = self.total_duration
                
                # Intentamos meter tantas vibraciones (ON) como quepan en el tiempo total
                while remaining >= self.on_duration:
                    timings_list.append(self.on_duration) # Añadimos vibración
                    remaining -= self.on_duration
                    
                    # ¿Queda tiempo suficiente para añadir también la pausa?
                    if remaining >= self.off_duration:
                        timings_list.append(self.off_duration) # Añadimos pausa
                        remaining -= self.off_duration
                    else:
                        # Si no cabe una pausa completa, paramos para no exceder total_duration
                        break
                
                timings = jarray(jlong)(timings_list)
                effect = VibrationEffect.createWaveform(timings, -1)
            else:
                # Patrón infinito: vibra, pausa, repite desde el índice 0
                timings = jarray(jlong)([0, self.on_duration, self.off_duration])
                effect = VibrationEffect.createWaveform(timings, 0)

            vibrator.vibrate(effect, attrs)
            
        except Exception as e:
            logger.error(f"Fallo al activar la vibración: {e}")
            raise e
