package co.edu.udea.alamano.moderacion_reportes.dominio;

import co.edu.udea.alamano.autenticacion_seguridad.dominio.RolUsuario;

import java.util.Arrays;
import java.util.List;

public enum MotivoReporte {
    // Motivos de Cliente -> Profesional (HU15)
    NO_SE_PRESENTO("El profesional no se presentó", RolUsuario.CLIENTE),
    MAL_COMPORTAMIENTO("Mal comportamiento o trato inadecuado", RolUsuario.CLIENTE),
    COBRO_INDEBIDO("Cobro indebido o por fuera de la plataforma", RolUsuario.CLIENTE),
    MALA_CALIDAD("Mala calidad del trabajo realizado", RolUsuario.CLIENTE),

    // Motivos de Profesional -> Cliente (HU16)
    CANCELACION_INJUSTIFICADA("Cancelación injustificada", RolUsuario.PROFESIONAL),
    MALTRATO("Maltrato u ofensas hacia el profesional", RolUsuario.PROFESIONAL),
    NO_PAGO("Negativa o incumplimiento de pago", RolUsuario.PROFESIONAL),
    INFORMACION_FALSA("Información falsa sobre el servicio o dirección", RolUsuario.PROFESIONAL);

    private final String etiqueta;
    private final RolUsuario rolReportantePermitido;

    MotivoReporte(String etiqueta, RolUsuario rolReportantePermitido) {
        this.etiqueta = etiqueta;
        this.rolReportantePermitido = rolReportantePermitido;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public RolUsuario getRolReportantePermitido() {
        return rolReportantePermitido;
    }

    public boolean aplicaParaRolReportante(RolUsuario rol) {
        return this.rolReportantePermitido == rol;
    }

    public static List<MotivoReporte> porRolReportante(RolUsuario rol) {
        return Arrays.stream(values())
                .filter(m -> m.aplicaParaRolReportante(rol))
                .toList();
    }
}
