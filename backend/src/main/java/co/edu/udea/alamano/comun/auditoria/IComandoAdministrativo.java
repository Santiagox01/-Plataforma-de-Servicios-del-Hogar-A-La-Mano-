package co.edu.udea.alamano.comun.auditoria;

/**
 * Patron Command para encapsular acciones administrativas auditables.
 */
@FunctionalInterface
public interface IComandoAdministrativo<R> {
    R ejecutar();
}
