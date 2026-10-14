-- Se quita por completo la función de notificaciones (campana) de los
-- alumnos: la V3/V4 crearon y ajustaron esta tabla, pero esas migraciones
-- ya están aplicadas y no se tocan. Esta es la que deshace lo que hicieron.
DROP TABLE IF EXISTS aviso_leido;
