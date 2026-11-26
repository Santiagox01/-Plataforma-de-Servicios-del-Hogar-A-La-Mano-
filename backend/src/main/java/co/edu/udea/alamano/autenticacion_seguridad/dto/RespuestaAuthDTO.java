package co.edu.udea.alamano.autenticacion_seguridad.dto;

public record RespuestaAuthDTO(
        String token,
        String tipoToken,
        ResumenUsuarioDTO usuario,
        String aviso
) {
    public RespuestaAuthDTO(String token, ResumenUsuarioDTO usuario, String aviso) {
        this(token, "Bearer", usuario, aviso);
    }
}
