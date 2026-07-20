from com.chaquo.python import Python

def get_context():
    """
    Obtiene el contexto de la aplicación Android a través de Chaquopy.
    """
    return Python.getPlatform().getApplication()

def get_system_service(service_name):
    """
    Obtiene un servicio del sistema Android por su nombre.
    """
    context = get_context()
    return context.getSystemService(service_name)
