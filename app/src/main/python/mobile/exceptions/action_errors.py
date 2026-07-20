from .base_error import MobileBaseError

class ActionError(MobileBaseError):
    """Clase base para errores en las acciones del móvil."""
    pass

class AppNotFoundError(ActionError):
    """Se lanza cuando una aplicación no está instalada o su alias no existe."""
    pass

class PermissionDeniedError(ActionError):
    """Se lanza cuando falta un permiso de sistema (normal o runtime)."""
    pass

class PermissionRequiredError(ActionError):
    """Se lanza cuando se requiere que el usuario active un permiso manual en ajustes."""
    pass

class ExecutionError(ActionError):
    """Error genérico durante la ejecución de una acción."""
    pass
